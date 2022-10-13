package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.modinterfaces.Advancement;

public record AdvancementAdapter(String name, String title, String description) implements Advancement {
}
