package com.kwvanderlinde.discordant.core.linkedprofiles;

import java.util.UUID;

public record VerificationData(String name, UUID uuid, String code, long validUntil) {
}
