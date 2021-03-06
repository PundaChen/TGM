package network.warzone.tgm.command;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandNumberFormatException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import network.warzone.tgm.TGM;
import network.warzone.tgm.modules.chat.ChatConstant;
import network.warzone.tgm.nickname.NickedUserProfile;
import network.warzone.tgm.util.HashMaps;
import network.warzone.warzoneapi.models.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NickCommands {

    @Command(aliases = {"nicks"}, desc = "View all nicked players", max = 1)
    @CommandPermissions({"tgm.command.whois"})
    public static void nicks(CommandContext cmd, CommandSender sender) {
        HashMap<UUID, String> originalNames = TGM.get().getNickManager().getOriginalNames();
        HashMap<UUID, String> nickNames = TGM.get().getNickManager().getNickNames();

        if (originalNames.size() == 0) {
            sender.sendMessage(ChatColor.RED + "No nicked players found.");
            return;
        }

        if (cmd.argsLength() == 1 && "reset".equals(cmd.getString(0))) {
            if (!sender.hasPermission("tgm.command.nicks.reset")) {
                sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
                return;
            }

            List<UUID> nickedPlayers = new ArrayList<>();

            originalNames.forEach((uuid, originalName) -> nickedPlayers.add(uuid));

            for (UUID nickedPlayer : nickedPlayers) {
                Player player = Bukkit.getPlayer(nickedPlayer);
                if (player != null) {
                    try {
                        TGM.get().getNickManager().reset(player, true);
                        sender.sendMessage(ChatColor.GREEN + "Unnicked: " + ChatColor.DARK_PURPLE + player.getName());
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        sender.sendMessage(ChatColor.RED + "Failed to unnick: " + ChatColor.DARK_PURPLE + player.getName());
                    }
                }
            }

            return;
        }

        StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Nicked Players:");

        originalNames.forEach((uuid, originalName) -> {
            message.append("\n")
                    .append(ChatColor.DARK_PURPLE)
                    .append(originalName)
                    .append(ChatColor.GRAY)
                    .append(" is nicked as ")
                    .append(ChatColor.LIGHT_PURPLE)
                    .append(nickNames.get(uuid));
            if (Bukkit.getPlayer(uuid) == null) message.append(" ")
                    .append(ChatColor.GRAY)
                    .append("[OFFLINE]");
        });

        sender.sendMessage(message.toString());
    }

    @Command(aliases = {"nick"}, desc = "Change nickname", usage = "(name)")
    @CommandPermissions({"tgm.command.nick"})
    public static void nick(CommandContext cmd, CommandSender sender) throws IllegalAccessException, NoSuchFieldException, UnirestException {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatConstant.ERROR_COMMAND_PLAYERS_ONLY.toString());
            return;
        }
        Player p = (Player) sender;
        if (cmd.argsLength() > 0) {
            String option = cmd.getString(0);
            if ("set".equalsIgnoreCase(option) && cmd.argsLength() > 1) {
                String newName = cmd.getString(1);
                if (newName.length() > 16) {
                    sender.sendMessage(ChatColor.RED + "Username must be shorter than 16 characters.");
                    return;
                }
                if (!newName.matches("^[a-z_A-Z0-9]+$")) {
                    sender.sendMessage(ChatColor.RED + "Invalid name.");
                    return;
                }
                if (
                        Bukkit.getOnlinePlayers().stream().map(
                                (Player player) -> TGM.get().getPlayerManager().getPlayerContext(player).getOriginalName()
                        ).anyMatch((String name) -> name.equals(newName))) {
                    sender.sendMessage(ChatColor.RED + "You cannot nick as an online player.");
                    return;
                }
                boolean force = false;
                if (cmd.argsLength() > 2) {
                    String arg2 = cmd.getString(2);
                    force = "force".equals(arg2);
                }
                if (force) {
                    try {
                        TGM.get().getNickManager().setNick(p, newName);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    TGM.get().getNickManager().addQueuedNick(p, newName);
                }

                sender.sendMessage(ChatColor.GREEN + "Updated username to " + ChatColor.YELLOW + newName);
                if (!force) {
                    sender.sendMessage(ChatColor.GOLD + "Reconnect for the changes to apply!");
                }
            } else if ("reset".equalsIgnoreCase(option)) {
                String original = TGM.get().getNickManager().getOriginalNames().get(p.getUniqueId());
                if (original != null) {
                    TGM.get().getNickManager().reset(p, true);
                } else {
                    sender.sendMessage(ChatColor.RED + "You are not nicked!");
                }
            } else if ("skin".equalsIgnoreCase(option) && cmd.argsLength() > 1) {
                String newName = cmd.getString(1);
                if (newName.length() > 16) {
                    sender.sendMessage(ChatColor.RED + "Username must be shorter than 16 characters.");
                    return;
                }

                TGM.get().getNickManager().setSkin(p, newName);
                sender.sendMessage(ChatColor.GREEN + "Updated skin to " + ChatColor.YELLOW + newName);
            } else if ("name".equalsIgnoreCase(option) && cmd.argsLength() > 1) {
                String newName = cmd.getString(1);

                if (!newName.matches("^[a-z_A-Z0-9]+$")) {
                    sender.sendMessage(ChatColor.RED + "Invalid name.");
                    return;
                }
                if (newName.length() > 16) {
                    sender.sendMessage(ChatColor.RED + "New name must be shorter than 16 characters.");
                    return;
                }
                if (
                        Bukkit.getOnlinePlayers().stream().map(
                                (Player player) -> TGM.get().getPlayerManager().getPlayerContext(player).getOriginalName()
                        ).anyMatch((String name) -> name.equals(newName))) {
                    sender.sendMessage(ChatColor.RED + "You cannot nick as an online player.");
                    return;
                }
                try {
                    TGM.get().getNickManager().setName(p, newName);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    p.sendMessage(ChatConstant.ERROR_RATE_LIMITED.toString());
                }
                sender.sendMessage(ChatColor.GREEN + "Updated username to " + ChatColor.YELLOW + newName);
            } else if ("stats".equalsIgnoreCase(option) && cmd.argsLength() > 1) {
                if (cmd.argsLength() > 2) {
                    try {
                        String statToChange = cmd.getString(1);
                        int value = cmd.getInteger(2);

                        try {
                            TGM.get().getNickManager().setStats(p, statToChange, value);
                        } catch (NoSuchFieldException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid stat. /nick stats <kills|deaths|wins|losses|objectives> <value>");
                        }
                        sender.sendMessage(ChatColor.GREEN + "Updated nicked " + ChatColor.YELLOW + statToChange + ChatColor.GREEN + " to " + ChatColor.YELLOW + value + ChatColor.GREEN + ".");
                    } catch (CommandNumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    String type = cmd.getString(1);
                    switch (type) {
                        case "random":
                            TGM.get().getNickManager().setStats(p, generateInt(10, 100), generateInt(45, 90), generateInt(10, 30), generateInt(10, 30), generateInt(10, 20));
                            break;
                        case "good":
                            double goodKDR = generateDouble(1, 2);
                            int goodDeaths = generateInt(35, 75);
                            TGM.get().getNickManager().setStats(p, (int) (goodDeaths * goodKDR), goodDeaths, generateInt(50, 100), generateInt(25, 50), generateInt(50, 100));
                            break;
                        case "bad":
                            double badKDR = generateDouble(0.1, 1);
                            int badDeaths = generateInt(100, 150);
                            TGM.get().getNickManager().setStats(p, (int) (badDeaths * badKDR), badDeaths, generateInt(20, 50), generateInt(50, 150), generateInt(1, 10));
                            break;
                        case "new":
                            TGM.get().getNickManager().setStats(p, 0, 0, 0, 0, 0);
                            TGM.get().getNickManager().setNew(p, true);
                            break;
                        default:
                            sender.sendMessage(ChatColor.RED + "/nick stats <statName|good|random|bad|new> [value]");
                            return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Set stats to preset " + ChatColor.YELLOW + type);
                }
            } else if ("rank".equalsIgnoreCase(option) && cmd.argsLength() > 1) {
                String newRank = cmd.getString(1);

                Bukkit.getScheduler().runTaskAsynchronously(TGM.get(), () -> {
                    Rank rank = null;
                    for (Rank r : TGM.get().getTeamClient().retrieveRanks()) {
                        if (r.getName().equalsIgnoreCase(newRank)) rank = r;
                    }
                    if ("none".equals(newRank)) {
                        NickedUserProfile profile = TGM.get().getNickManager().getUserProfile(p);
                        profile.setRanksLoaded(new ArrayList<>());
                        TGM.get().getNickManager().getStats().put(p.getUniqueId(), profile);
                        sender.sendMessage(ChatColor.GREEN + "Removed nicked rank");
                        return;
                    }
                    if (rank != null) {
                        TGM.get().getNickManager().setRank(p, rank);
                        sender.sendMessage(ChatColor.GREEN + "Updated rank to " + ChatColor.YELLOW + rank.getName());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid rank");
                    }
                });
            } else {
                sender.sendMessage(ChatColor.RED + "/nick <set|reset|name|skin|stats|rank> <option>");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "/nick <set|reset|name|skin|stats|rank> <option>");
        }
    }

    @Command(aliases = {"whois"}, desc = "Reveals a nicked player")
    @CommandPermissions({"tgm.command.whois"})
    public static void whois(CommandContext cmd, CommandSender sender) {
        if (cmd.argsLength() > 0) {
            String username = cmd.getString(0);
            if (TGM.get().getNickManager().getNickNames().containsValue(username)) {
                UUID uuid = HashMaps.reverseGetFirst(username, TGM.get().getNickManager().getNickNames());
                String originalName = TGM.get().getNickManager().getOriginalNames().get(uuid);
                sender.sendMessage(ChatColor.YELLOW + username + ChatColor.GREEN + " is " + ChatColor.YELLOW + originalName);
            } else {
                sender.sendMessage(ChatColor.RED + "That user isn't nicked!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid user.");
        }
    }

    private static double generateDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max + 1);
    }

    private static int generateInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}
