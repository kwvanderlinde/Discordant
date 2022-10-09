package com.kwvanderlinde.discordant.mc.messages;

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
            else if (part.tag() instanceof SemanticMessage.PartTag.DiscordUser discordUser) {
                // TODO Use hover and copy and click to good effect. Show roles etc that way.
                component.append(
                        Component.literal(discordUser.roleName() + " " + part.text()).setStyle(Style.EMPTY.withColor(discordUser.roleColor())
                                                                                                                 .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Language.getInstance().getOrDefault("chat.copy.click") + " " + discordUser.userTag()).withStyle(ChatFormatting.GREEN)))
                                                                                                                 .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, discordUser.userTag()))));
            }
            else if (part.tag() instanceof SemanticMessage.PartTag.Bot botName) {
                component.append(
                        Component.literal(part.text()).withStyle(
                                Style.EMPTY.withColor(botName.color())
                        )
                );
            }
        });
        return component;
    }
}
