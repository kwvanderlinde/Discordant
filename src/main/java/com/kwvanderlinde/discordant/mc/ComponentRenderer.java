package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.messages.SemanticMessageRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.stream.Stream;

public class ComponentRenderer implements SemanticMessageRenderer<Component> {
    public static SemanticMessageRenderer<Component> instance() {
        return new ComponentRenderer();
    }

    @Override
    public Component render(Stream<SemanticMessage.Part> messageParts) {
        MutableComponent component = Component.empty();
        messageParts.forEach(part -> {
            if (part.tag() instanceof SemanticMessage.PartTag.None) {
                // No special semantics.
                component.append(part.text());
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.VerificationCode codeTag) {
                component.append(
                        Component.literal(part.text())
                                 .withStyle(Style.EMPTY
                                                 .withColor(codeTag.color())
                                                 .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Language.getInstance().getOrDefault("chat.copy.click")).withStyle(ChatFormatting.GREEN)))
                                                 .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, part.text()))));
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.DiscordSender discordSender) {
                final var roleText = ((discordSender.roleName() == null) ? "" : (discordSender.roleName() + " "))
                        + discordSender.userTag();
                final var hoverComponent = Component.literal(Language.getInstance().getOrDefault("chat.copy.click") + "; ")
                                                    .withStyle(ChatFormatting.GREEN)
                                                    .append(Component.literal(roleText)
                                                                     .setStyle(Style.EMPTY.withColor(discordSender.roleColor())));
                final var textToCopy = "@" + discordSender.userTag();

                component.append(
                        Component.literal(part.text())
                                 .setStyle(Style.EMPTY.withColor(discordSender.roleColor())
                                                      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                                      .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, textToCopy))));
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.DiscordMention discordMention) {
                final var roleText = ((discordMention.roleName() == null) ? "" : (discordMention.roleName() + " "))
                        + discordMention.userTag();
                final var hoverComponent = Component.literal(Language.getInstance().getOrDefault("chat.copy.click") + "; ")
                                                    .withStyle(ChatFormatting.GREEN)
                                                    .append(Component.literal(roleText)
                                                                     .setStyle(Style.EMPTY.withColor(discordMention.roleColor())));
                final var textToCopy = "@" + discordMention.userTag();

                component.append(
                        Component.literal("@" + part.text())
                                .setStyle(Style.EMPTY.withColor(discordMention.roleColor())
                                                  .withItalic(true)
                                                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                                  .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, textToCopy))));
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.DiscordRoleMention discordRoleMention) {
                final var hoverComponent = Component.literal(Language.getInstance().getOrDefault("chat.copy.click"))
                                                    .withStyle(ChatFormatting.GREEN);
                final var textToCopy = "@" + part.text();

                component.append(
                        Component.literal("@" + part.text())
                                 .setStyle(Style.EMPTY.withColor(discordRoleMention.color())
                                                      .withItalic(true)
                                                      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                                      .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, textToCopy))));
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.DiscordChannelMention discordChannelMention) {
                final var hoverComponent = Component.literal(Language.getInstance().getOrDefault("chat.link.open"))
                                                    .withStyle(ChatFormatting.GREEN);

                component.append(
                        Component.literal("#" + part.text())
                                 .setStyle(Style.EMPTY.withUnderlined(true)
                                                      .withColor(0x0083EE)
                                                      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                                      .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordChannelMention.jumpUrl()))));
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.Bot botName) {
                component.append(
                        Component.literal(part.text())
                                 .withStyle(Style.EMPTY.withColor(botName.color())));
            }
        });
        return component;
    }
}
