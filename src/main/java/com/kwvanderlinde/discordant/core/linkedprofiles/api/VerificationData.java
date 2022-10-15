package com.kwvanderlinde.discordant.core.linkedprofiles.api;

import java.util.UUID;

public record VerificationData(String name, UUID uuid, int code, long validUntil) {
    public String token() {
        return uuid.toString() + "|" + String.valueOf(code);
    }
}
