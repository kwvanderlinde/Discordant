package com.kwvanderlinde.discordant.core.discord.messagehandlers;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import com.kwvanderlinde.discordant.mc.DiscordantModInitializer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MentionMessageHandler implements MessageHandler {
    // TODO Fix mentioning of minecraft names ("!@MyName") so that:
    //  1. The bot can actually receive the literal message. "!@MyName" is turned into !<@someidhere>. We could instead use "@!" or a different prefix altogether, like "+".
    //  2. The message sent to the minecraft client does not include the mention prefix as it does now. Instead it should just contain the coloured username.
    //  Other changes:
    //  1. Color all usernames (sender and mention) according to role color.
    //  2. Do not prefix sender with role, but apply it in hover text.
    //  3. Instead of the prefix "[Discord] XXX >> message", phrase it like "[Discord] user XXX say" and italicize the lead up.

    private final Pattern pattern = Pattern.compile("(?<=!\\+).+?(?=!\\+|$|\\s)");
    private final Pattern pattern2 = Pattern.compile("(?<=<@).+?(?=>)");

    @Override
    public void handleChat(MessageReceivedEvent e, Server server, String msg) {
        System.out.printf("Handling chat message: %s%n", msg);
        Member member = e.getMember();
        final Set<String> playerNamesToNotify = new HashSet<>();
        if (msg.contains("!+")) {
            playerNamesToNotify.addAll(pattern.matcher(msg).results().map(matchResult -> matchResult.group(0).toLowerCase()).collect(Collectors.toSet()));
        }
        if (msg.contains("<@")) {
            List<String> ids = pattern2.matcher(msg).results().map(matchResult -> matchResult.group(0)).toList();
            for (String s : ids) {
                if (s.startsWith("!")) {
                    s = s.substring(1);
                }
                String name = DiscordantModInitializer.core.getLinkedPlayerNameForDiscordId(s);
                if (name != null) {
                    playerNamesToNotify.add(name.toLowerCase());
                    msg = msg.replaceAll("<(@.|@)" + s + ">", "@" + name);
                }
                else {
                    Member m = e.getGuild().getMemberById(s);
                    if (m != null) {
                        msg = msg.replaceAll("<(@.|@)" + s + ">", "@" + m.getEffectiveName());
                    }
                    else {
                        msg = msg.replaceAll("<(@.|@)" + s + ">", "@Unknown");
                    }
                }
            }
        }
        final var msgFinal = msg;

        if (member != null) {
            String role = member.getRoles().isEmpty() ? "" : member.getRoles().get(0).getName();

            final var notifyMessage = new SemanticMessage()
                    .append(SemanticMessage.bot("[Discord] "))
                    .append(getChatComponent(role, member))
                    .append(" >> ");
            final var generalMessage = notifyMessage.copy()
                    .append(msgFinal);

            server.getAllPlayers().forEach(player -> {
                if (!playerNamesToNotify.contains(player.name().toLowerCase())) {
                    player.sendSystemMessage(generalMessage);
                }
                else {
                    final var name = player.name();
                    // TODO Use a semantic message part instead of format codes. And trim the !+.
                    final var replacement = msgFinal.replaceAll("(?i)!\\+" + name, "§a$0§r");
                    player.sendSystemMessage(notifyMessage.copy().append(replacement));
                    player.notifySound();
                }
            });
        }
    }

    private SemanticMessage.Part getChatComponent(String role, Member member) {
        return SemanticMessage.discordUser(
                member.getEffectiveName(),
                member.getUser().getAsTag(),
                role,
                member.getColorRaw()
        );
    }
}
