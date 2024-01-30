package com.kwvanderlinde.discordant.core.modinterfaces;

import javax.annotation.Nullable;
import java.util.Optional;

public record Advancement(Optional<Display> display) {
    public record Type(String name, @Nullable Integer rgb) {
    }

    public record Display(String title, String description, Type type, boolean announceToChat) {
    }
}
