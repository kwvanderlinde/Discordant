package com.kwvanderlinde.discordant.core.discord.messagehandlers;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DefaultMessageHandler implements MessageHandler {

    @Override
    public void handleChat(MessageReceivedEvent e, Server server, String msg) {
        final var message = new SemanticMessage();
        message.append(SemanticMessage.bot("[Discord] "));

        Member member = e.getMember();
        if (msg.contains("<@")) {
            // TODO Always render discord Ids as users.
            msg = msg.replaceAll("<@.*?>", "");
            if (msg.isEmpty()) {
                return;
            }
        }
        if (member != null) {
            String role = member.getRoles().isEmpty() ? "" : member.getRoles().get(0).getName();
            message.append(SemanticMessage.discordUser(
                    member.getEffectiveName(),
                    member.getUser().getAsTag(),
                    role,
                    member.getColorRaw()
            ));
            message.append(" >> ");
            message.append(msg);

            server.getAllPlayers().forEach(p -> {
                p.sendSystemMessage(message);
            });
        }

    }
}
