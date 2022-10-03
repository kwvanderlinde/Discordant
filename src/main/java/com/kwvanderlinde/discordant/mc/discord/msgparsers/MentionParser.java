package com.kwvanderlinde.discordant.mc.discord.msgparsers;

import com.kwvanderlinde.discordant.mc.IServerPlayer;
import com.kwvanderlinde.discordant.mc.discord.DiscordantModInitializer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MentionParser implements MsgParser {
    // TODO Fix mentioning of minecraft names ("!@MyName") so that:
    //  1. The bot can actually receive the literal message. "!@MyName" is turned into !<@someidhere>. We could instead use "@!" or a different prefix altogether, like "+".
    //  2. The message sent to the minecraft client does not include the mention prefix as it does now. Instead it should just contain the coloured username.
    //  Other changes:
    //  1. Color all usernames (sender and mention) according to role color.
    //  2. Do not prefix sender with role, but apply it in hover text.
    //  3. Instead of the prefix "[Discord] XXX >> message", phrase it like "[Discord] user XXX say" and italicize the lead up.

    private final Pattern pattern = Pattern.compile("(?<=!\\+).+?(?=!\\+|$|\\s)");
    private final Pattern pattern2 = Pattern.compile("(?<=<@).+?(?=>)");

    public void handleChat(MessageReceivedEvent e, DedicatedServer server, String msg) {
        System.out.printf("Handling chat message: %s%n", msg);
        Member member = e.getMember();
        Set<String> playerNames = new HashSet<>();
        if (msg.contains("!+")) {
            playerNames = pattern.matcher(msg).results().map(matchResult -> matchResult.group(0).toLowerCase()).collect(Collectors.toSet());
        }
        if (msg.contains("<@")) {
            List<String> ids = pattern2.matcher(msg).results().map(matchResult -> matchResult.group(0)).toList();
            for (String s : ids) {
                if (s.startsWith("!")) {
                    s = s.substring(1);
                }
                String name = DiscordantModInitializer.core.getLinkedPlayerNameForDiscordId(s);
                if (name != null) {
                    playerNames.add(name.toLowerCase());
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
        if (member != null) {
            String role = member.getRoles().isEmpty() ? "" : member.getRoles().get(0).getName();
            MutableComponent cp = Component.literal("[Discord] ").withStyle(style -> style.withColor(6955481))
                                           .append(getChatComponent(role, member))
                                           .append(Component.literal(" >> " + msg).withStyle(ChatFormatting.WHITE));
            if (!playerNames.isEmpty()) {
                MutableComponent cp2 = Component.literal("[Discord] ").withStyle(Style.EMPTY.withColor(6955481))
                                                .append(getChatComponent(role, member));
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (((IServerPlayer) player).isAcceptingChatType(ChatType.CHAT)) {
                        if (!playerNames.contains(player.getScoreboardName().toLowerCase())) {
                            player.sendSystemMessage(cp);
                        }
                        else {
                            final var name = player.getScoreboardName();
                            final var replacement = msg.replaceAll("(?i)!\\+" + name, "§a$0§r");
                            player.sendSystemMessage(cp2.append(Component.literal(" >> " + replacement).withStyle(ChatFormatting.WHITE)));
                            if (((IServerPlayer) player).getNotifyState()) {
                                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING, SoundSource.MASTER, 1.0F, 1.0F);
                            }
                        }
                    }
                }
            }
            else {
                server.getPlayerList().getPlayers().forEach(player -> {
                    if (((IServerPlayer) player).isAcceptingChatType(ChatType.CHAT)) {
                        player.sendSystemMessage(cp);
                    }
                });
            }
        }
    }

    private MutableComponent getChatComponent(String role, Member member) {
        String tag = member.getUser().getAsTag();
        return Component.literal(role + " " + member.getEffectiveName()).setStyle(Style.EMPTY.withColor(member.getColorRaw())
                                                                                             .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Language.getInstance().getOrDefault("chat.copy.click") + " " + tag).withStyle(ChatFormatting.GREEN)))
                                                                                             .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tag)));
    }
}
