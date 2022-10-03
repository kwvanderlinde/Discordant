package com.kwvanderlinde.discordant.core.discord.linkedprofiles;

import java.util.UUID;

public record VerificationData(String name, UUID uuid, long validUntil) {
}
