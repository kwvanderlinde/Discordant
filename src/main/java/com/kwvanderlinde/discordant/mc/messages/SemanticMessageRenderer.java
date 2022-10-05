package com.kwvanderlinde.discordant.mc.messages;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.stream.Stream;

public class SemanticMessageRenderer {
    public static Component renderMessage(Stream<SemanticMessage.Part> messageParts) {
        MutableComponent component = Component.empty();
        messageParts.forEach(part -> {
            if (part instanceof SemanticMessage.Part.Nil) {
                // Do nothing.
            }
            else if (part instanceof SemanticMessage.Part.Literal literal) {
                component.append(literal.text());
            }
            else if (part instanceof SemanticMessage.Part.VerificationCode verificationCode) {
                component.append(
                        // TODO Can we add copy on click?
                        Component.literal(verificationCode.code()).withStyle(Style.EMPTY.withColor(verificationCode.color()))
                );
            }
            else if (part instanceof SemanticMessage.Part.DiscordUser discordUser) {
                // TODO Use hover and copy and click to good effect. Show roles etc that way.
                component.append(
                        Component.literal(discordUser.roleName() + " " + discordUser.name()).setStyle(Style.EMPTY.withColor(discordUser.color())
                                                                                                                 .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Language.getInstance().getOrDefault("chat.copy.click") + " " + discordUser.tag()).withStyle(ChatFormatting.GREEN)))
                                                                                                                 .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, discordUser.tag()))));
            }
            else if (part instanceof SemanticMessage.Part.BotName botName) {
                component.append(
                        Component.literal(botName.name()).withStyle(
                                Style.EMPTY.withColor(botName.color())
                        )
                );
            }
        });
        return component;
    }
}
