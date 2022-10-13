package com.kwvanderlinde.discordant.core;

public class ConfigurationValidationFailed extends Exception {
    public ConfigurationValidationFailed() {
    }

    public ConfigurationValidationFailed(String message) {
        super(message);
    }

    public ConfigurationValidationFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationValidationFailed(Throwable cause) {
        super(cause);
    }

    public ConfigurationValidationFailed(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
