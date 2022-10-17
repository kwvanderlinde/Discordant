package com.kwvanderlinde.discordant.core.linkedprofiles;

import com.kwvanderlinde.discordant.core.ReloadableComponent;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.utils.Clock;
import com.kwvanderlinde.discordant.core.config.LinkingConfig;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class LinkedProfileManager implements ReloadableComponent {
    private LinkingConfig config;
    private final Clock clock;
    private final LinkedProfileRepository linkedProfileRepository;
    private final HashMap<String, String> linkedPlayersByDiscordId = new HashMap<>();
    private final HashMap<UUID, VerificationData> pendingLinkVerification = new HashMap<>();
    private final Random r = new Random();

    public sealed interface VerificationResult
            permits InvalidCode, IncorrectCode, AlreadyLinked, SuccessfulLink {
    }
    public record InvalidCode(String code) implements VerificationResult {}
    public record IncorrectCode(String code) implements VerificationResult {}
    public record AlreadyLinked(LinkedProfile existingProfile) implements VerificationResult {}
    public record SuccessfulLink(LinkedProfile newProfile) implements VerificationResult {}

    public LinkedProfileManager(LinkingConfig config, Clock clock, LinkedProfileRepository linkedProfileRepository) {
        this.config = config;
        this.clock = clock;
        this.linkedProfileRepository = linkedProfileRepository;
    }

    @Override
    public void reload(DiscordantConfig newConfig) {
        // Not much to be done as we just dynamically read values from the configuration.
        config = newConfig.linking;
    }

    public void clearExpiredVerifications() {
        // Remove any expired pending verifications.
        final var currentTime = clock.getCurrentTime();
        final var iterator = pendingLinkVerification.entrySet().iterator();
        while (iterator.hasNext()) {
            final var e = iterator.next();
            VerificationData data = e.getValue();
            if (currentTime > data.validUntil()) {
                iterator.remove();
            }
        }
    }

    public boolean ensureProfileIsLinked(Profile profile) {
        final var linkedProfile = linkedProfileRepository.getByPlayerId(profile.uuid());

        if (linkedProfile == null) {
            return false;
        }

        // Reverse map so we can look up profile by discord ID.
        // TODO Is it worth handling edge case that there are existing entries? Would
        //  not be correct for them to exist, but bugs or instability may cause it.
        linkedPlayersByDiscordId.put(linkedProfile.discordId(), profile.name());
        return true;
    }

    public void unloadProfile(Profile profile) {
        final var linkedProfile = linkedProfileRepository.getByPlayerId(profile.uuid());
        if (linkedProfile != null) {
            linkedPlayersByDiscordId.remove(linkedProfile.discordId());
        }
    }

    public @Nullable String getDiscordIdForPlayerId(UUID playerId) {
        final var linkedProfile = linkedProfileRepository.getByPlayerId(playerId);
        if (linkedProfile == null) {
            return null;
        }
        return linkedProfile.discordId();
    }

    public String getLinkedPlayerNameForDiscordId(String discordId) {
        return linkedPlayersByDiscordId.get(discordId);
    }

    public String generateLinkCode(UUID uuid, String name) {
        if (pendingLinkVerification.containsKey(uuid)) {
            return pendingLinkVerification.get(uuid).code();
        }

        final var authCode = String.format("%06d", r.nextInt(0, 1_000_000));
        final var expiryTime = clock.getCurrentTime() + config.pendingTimeout;
        final var data = new VerificationData(name, uuid, authCode, expiryTime);
        pendingLinkVerification.put(uuid, data);

        return data.code();
    }

    public boolean removeLinkedProfile(UUID uuid) {
        LinkedProfile profile = linkedProfileRepository.getByPlayerId(uuid);
        if (profile != null) {
            linkedPlayersByDiscordId.remove(profile.discordId());
            linkedProfileRepository.delete(profile);
            return true;
        }
        return false;
    }

    public VerificationResult verifyLinkedProfile(final String authorId,
                                                  final String verificationCode) {
        if (verificationCode.length() != 6 || !verificationCode.matches("[0-9]+")) {
            return new InvalidCode(verificationCode);
        }

        final var optionalData = pendingLinkVerification
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().code().equals(verificationCode))
                .findFirst();
        if (optionalData.isEmpty()) {
            return new IncorrectCode(verificationCode);
        }
        final var uuid = optionalData.get().getKey();
        final var data = optionalData.get().getValue();

        final var existingProfile = linkedProfileRepository.getByPlayerId(uuid);
        if (existingProfile == null) {
            // Profile entry does not exist yet. Create it.
            final var newLinkedProfile = new LinkedProfile(data.name(), uuid, authorId);
            linkedProfileRepository.put(newLinkedProfile);
            pendingLinkVerification.remove(uuid);
            linkedPlayersByDiscordId.put(newLinkedProfile.discordId(), newLinkedProfile.name());

            return new SuccessfulLink(newLinkedProfile);
        }
        else {
            pendingLinkVerification.remove(uuid);
            return new AlreadyLinked(existingProfile);
        }
    }
}
