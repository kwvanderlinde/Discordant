package com.kwvanderlinde.discordant.core;

public class ModLoadFailed extends Exception {
    public ModLoadFailed() {
    }

    public ModLoadFailed(String message) {
        super(message);
    }

    public ModLoadFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public ModLoadFailed(Throwable cause) {
        super(cause);
    }

    public ModLoadFailed(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
