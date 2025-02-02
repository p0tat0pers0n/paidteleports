package SPUD.Industries.paidteleports;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.StringBuilder;
import java.lang.String;
import java.util.*;

public final class PaidTeleports extends JavaPlugin implements Listener {
    String[] teleportCommands = {"tp", "warp", "ewarp", "warps", "ewarps", "essentials:warp", "essentials:ewarp", "essentials:warps", "essentials:ewarps", "tpa", "tp2p", "tpask", "tpahere", "etpa", "etp2p", "etpask", "etpahere", "essentials:tpa", "essentials:tp2p", "essentials:tpask", "essentials:tpahere", "essentials:etpa", "essentials:etp2p", "essentials:etpask", "essentials:etpahere", "home", "homes", "ehome", "ehomes", "essentials:home", "essentials:homes", "essentials:ehome", "essentials:ehomes"};
    String[] tpaTeleport = {"tpa", "tp2p", "tpask", "etpa", "etp2p", "etpask", "essentials:tpa", "essentials:tp2p", "essentials:tpask", "essentials:etpa", "essentials:etp2p", "essentials:etpask"};
    String[] tpaHereTeleport = {"tpahere", "essentials:tpahere", "etpahere", "essentials:etpahere"};

    HashMap<UUID, UUID> tpaHereList = new HashMap<>();
    HashMap<UUID, Integer> teleportingPlayers = new HashMap<>();

    String teleportItem = this.getConfig().getString("payment-item");

    String paidItemMessage = this.getConfig().getString("paid-message");
    String droppedItemMessage = this.getConfig().getString("dropped-item-message");
    String noPaymentItemMessage = this.getConfig().getString("no-payment-item-message");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("paidteleports") && args.length > 0) {
            this.getConfig().options().copyDefaults(true);
            if (args[0].equals("help")) {
                // Display help information
                sender.sendMessage("/paidteleports freewarps toggle\n§cToggles if all warps are free.§f\n/paidteleports freewarps add/remove WarpName\n§cAdds or removes a warp from the free-warp list.");
            }
            if (args[0].equals("reload")) {
                // Reloads the config file
                this.reloadConfig();
                sender.sendMessage("§cReloaded!");
                return true;
            }
            if (args[0].equals("freewarps")) {
                List<String> freeWarps = this.getConfig().getStringList("free-warps");
                if (args.length > 1) {
                    switch (args[1]) {
                        case "toggle":
                            // Toggle the all-warps-free config
                            if (this.getConfig().getBoolean("all-warps-free")) {
                                // All warps free is on so we turn it off
                                this.getConfig().set("all-warps-free", false);
                                this.saveConfig();
                                sender.sendMessage("all-warps-free is now false");
                            } else {
                                // All warps free is off so we turn it on
                                this.getConfig().set("all-warps-free", true);
                                this.saveConfig();
                                sender.sendMessage("all-warps-free is now true");
                            }
                            return true;
                        case "add":
                            // Add a warp by args[2] name
                            if (args.length > 2) {
                                if (!freeWarps.contains(args[2])) {
                                    freeWarps.add(args[2]);
                                    this.getConfig().set("free-warps", freeWarps);
                                    this.saveConfig();
                                    sender.sendMessage(args[2] + "§c has been added");
                                }else {
                                    sender.sendMessage(args[2] + "§c is already on the list");
                                }
                            } else {
                                sender.sendMessage("§cMissing warp name argument /paidteleports freewarps add [Warp Name] <-- This");
                            }
                            return true;
                        case "remove":
                            // Remove a warp by the args[2] name
                            if (args.length > 2) {
                                if (freeWarps.remove(args[2])) {
                                    // Free warps list contains user provided response and is removed
                                    sender.sendMessage(args[2] + "§c has been removed.");
                                    this.getConfig().set("free-warps", freeWarps);
                                    this.saveConfig();
                                } else {
                                    sender.sendMessage("§cNo warp by that name");
                                }
                            } else {
                                sender.sendMessage("§cMissing warp name argument /paidteleports freewarps remove [Warp Name] <-- This");
                            }
                            return true;
                    }
                }else {
                    // No args [1] therefore list the warps that are free
                    sender.sendMessage("§cAll warps free:§f " + this.getConfig().getBoolean("all-warps-free"));
                    StringBuilder freeWarpsList = new StringBuilder();
                    freeWarpsList.append("Free Warps: ");
                    if (!this.getConfig().getStringList("free-warps").isEmpty()) {
                        for (String warps : this.getConfig().getStringList("free-warps")) {
                            freeWarpsList.append(warps);
                            if (!warps.equals(this.getConfig().getStringList("free-warps").getLast())) {
                                freeWarpsList.append(", ");
                            } else {
                                freeWarpsList.append(".");
                                break;
                            }
                        }
                        sender.sendMessage(freeWarpsList.toString());
                    }else {
                        sender.sendMessage("§cNo free warps");
                    }
                    return true;
                }
            }
        }else if (args.length == 0) {
            sender.sendMessage("§cPossible Arguments:§f help, reload, freewarps [toggle, add, remove]");
            return true;
        }
        return true;
    }

    boolean paymentChecker(Player player, String command, String playerToTpaHere) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getItemMeta().hasCustomName() && item.getItemMeta().getDisplayName().equals(teleportItem)) {
                for (String commands : tpaHereTeleport) {
                    if (commands.equals(command)) {
                        UUID playerToTpaHereUUID = Objects.requireNonNull(Bukkit.getPlayer(playerToTpaHere)).getUniqueId();
                        if (!teleportingPlayers.containsKey(playerToTpaHereUUID)) {
                            teleportingPlayers.put(playerToTpaHereUUID, 124);
                            tpaHereList.put(playerToTpaHereUUID, player.getUniqueId());
                        }else {
                            teleportingPlayers.replace(playerToTpaHereUUID, 124);
                            tpaHereList.replace(playerToTpaHereUUID, player.getUniqueId());
                        }
                        return false;
                    }
                }
                tpaHereList.remove(player.getUniqueId());
                for (String commands : tpaTeleport) {
                    if (command.equals(commands)) {
                        tpaHereList.remove(player.getUniqueId());
                        if (!teleportingPlayers.containsKey(player.getUniqueId())) {
                            teleportingPlayers.put(player.getUniqueId(), 124);
                        }else {
                            teleportingPlayers.replace(player.getUniqueId(), 124);
                        }
                        return false;
                    }
                }
                if (!teleportingPlayers.containsKey(player.getUniqueId())) {
                    teleportingPlayers.put(player.getUniqueId(), 4);
                }else {
                    teleportingPlayers.replace(player.getUniqueId(), 4);
                }
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void ReceiveCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String rawCommand = event.getMessage();
        String[] commandSplit = rawCommand.split("[/\\s]");
        String command = commandSplit[1];
        List<String> freeWarps = this.getConfig().getStringList("free-warps");

        for (String teleportCommand : teleportCommands) {
            if (command.equals(teleportCommand)) {
                // Player issued actual teleport command and has perms to teleport
                if (command.equals("tp")) {
                    teleportingPlayers.remove(Objects.requireNonNull(Bukkit.getPlayer(commandSplit[3])).getUniqueId());
                    break;
                }
                if (command.equals("warp") && freeWarps.contains(commandSplit[2])) {
                    // If the player warps to one of the free warps in the config then make it free by breaking
                    break;
                }
                if (command.equals("warp") && this.getConfig().getBoolean("all-warps-free")) {
                    // If the all warps free config is enabled then skip payment processing
                    break;
                }
                if (commandSplit.length < 3) {
                    rawCommand = rawCommand + " NA";
                    commandSplit = rawCommand.split("[/\\s]");
                }
                if (paymentChecker(player, commandSplit[1], commandSplit[2])) {
                    player.sendMessage(noPaymentItemMessage);
                    teleportingPlayers.remove(player.getUniqueId());
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    @EventHandler
    public void playerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        // Removes player from teleporting list if they teleport within the time
        if (String.valueOf(cause).equals("COMMAND") && teleportingPlayers.containsKey(player.getUniqueId())) {
            // If player that is being tp'd was tpahere'd charge the player that did /tpahere
            if (tpaHereList.containsKey(player.getUniqueId())) {
                for (ItemStack item : Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).getInventory()) {
                    if (item != null && item.getItemMeta().hasCustomName() && item.getItemMeta().getDisplayName().equals(teleportItem)) {
                        Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).sendMessage(paidItemMessage);
                        item.setAmount(item.getAmount() - 1);
                        Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).updateInventory();
                        teleportingPlayers.remove(player.getUniqueId());
                        tpaHereList.remove(player.getUniqueId());
                        break;
                    }
                }
                if (teleportingPlayers.containsKey(player.getUniqueId())) {
                    // Cancel teleport because they dropped the crystal
                    event.setCancelled(true);
                    teleportingPlayers.remove(player.getUniqueId());
                    tpaHereList.remove(player.getUniqueId());
                    Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).sendMessage(droppedItemMessage);
                }
            }else {
                // Player issued the command themselves
                for (ItemStack item : player.getInventory()) {
                    if (item != null && item.getItemMeta().hasCustomName() && item.getItemMeta().getDisplayName().equals(teleportItem)) {
                        player.sendMessage(paidItemMessage);
                        item.setAmount(item.getAmount() - 1);
                        player.updateInventory();
                        teleportingPlayers.remove(player.getUniqueId());
                        break;
                    }
                }
                if (teleportingPlayers.containsKey(player.getUniqueId())) {
                    // Cancel teleport because they dropped the crystal
                    event.setCancelled(true);
                    teleportingPlayers.remove(player.getUniqueId());
                    player.sendMessage(droppedItemMessage);
                }
            }
        }
    }

    @EventHandler
    public void onEnable() {
        // Plugin startup logic
        PluginManager pm = this.getServer().getPluginManager();

        /*
         * this registers our listener. The first argument is the listener the second the plugin
         * if you put the listener into a different class you have to put an instance of that class
         * as the first argument
         */
        pm.registerEvents(this, this);

        tpaHereList.clear();
        teleportingPlayers.clear();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerUUID : teleportingPlayers.keySet()) {
                    teleportingPlayers.replace(playerUUID, teleportingPlayers.get(playerUUID) - 1);
                    if (teleportingPlayers.get(playerUUID) < 0) {
                        teleportingPlayers.remove(playerUUID);
                        tpaHereList.remove(playerUUID);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        teleportingPlayers.remove(event.getPlayer().getUniqueId());
        tpaHereList.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}