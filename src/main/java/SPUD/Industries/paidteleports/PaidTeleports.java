package SPUD.Industries.paidteleports;

import org.bukkit.Bukkit;
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

import java.util.*;

public final class PaidTeleports extends JavaPlugin implements Listener {
    String[] teleportCommands = {"warp", "ewarp", "warps", "ewarps", "essentials:warp", "essentials:ewarp", "essentials:warps", "essentials:ewarps", "tpa", "tp2p", "tpask", "tpahere", "etpa", "etp2p", "etpask", "etpahere", "essentials:tpa", "essentials:tp2p", "essentials:tpask", "essentials:tpahere", "essentials:etpa", "essentials:etp2p", "essentials:etpask", "essentials:etpahere", "home", "homes", "ehome", "ehomes", "essentials:home", "essentials:homes", "essentials:ehome", "essentials:ehomes"};
    String[] tpaTeleport = {"tpa", "tp2p", "tpask", "tpahere", "etpa", "etp2p", "etpask", "etpahere", "essentials:tpa", "essentials:tp2p", "essentials:tpask", "essentials:tpahere", "essentials:etpa", "essentials:etp2p", "essentials:etpask", "essentials:etpahere"};
    String[] tpaHereTeleport = {"tpahere", "essentials:tpahere", "etpahere", "essentials:etpahere"};

    HashMap<UUID, UUID> tpaHereList = new HashMap<UUID, UUID>();
    HashMap<UUID, Integer> teleportingPlayers = new HashMap<UUID, Integer>();

    String teleportItem = this.getConfig().getString("payment-item");

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
                    }
                }
                tpaHereList.remove(player.getUniqueId());
                for (String commands : tpaTeleport) {
                    if (command.equals(commands)) {
                        if (!teleportingPlayers.containsKey(player.getUniqueId())) {
                            teleportingPlayers.put(player.getUniqueId(), 124);
                        }else {
                            teleportingPlayers.replace(player.getUniqueId(), 124);
                        }
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

        for (String item : freeWarps) {
            player.sendMessage(item);
        }

        for (String teleportCommand : teleportCommands) {
            if (command.equals(teleportCommand) && player.hasPermission("essentials.home")) {
                // Player issued actual teleport command and has perms to teleport
                if (command.equals("warp") && freeWarps.contains(commandSplit[2])) {
                    // If the player warps to one of the free warps in the config then make it free by breaking
                    break;
                }
                if (command.equals("warp") && this.getConfig().getBoolean("all-warps-free")) {
                    // If the all warps free config is enabled then skip payment processing
                    break;
                }
                if (commandSplit.length < 3) {
                    player.sendMessage(String.valueOf(commandSplit.length));
                    rawCommand = rawCommand + " NA";
                    commandSplit = rawCommand.split("[/\\s]");
                }
                if (paymentChecker(player, commandSplit[1], commandSplit[2])) {
                    player.sendMessage("You need a " + teleportItem + "§f to teleport.");
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
        // Removes player from teleporting list if they teleport within 3 seconds
        // Otherwise, wait for the refund
        if (String.valueOf(cause).equals("COMMAND") && teleportingPlayers.containsKey(player.getUniqueId())) {
            // If player that is being tp'd was tpahere'd charge the player that did /tpahere
            if (tpaHereList.containsKey(player.getUniqueId())) {
                for (ItemStack item : Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).getInventory()) {
                    if (item != null && item.getItemMeta().hasCustomName() && item.getItemMeta().getDisplayName().equals(teleportItem)) {
                        Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).sendMessage("You have used a " + teleportItem);
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
                    Bukkit.getPlayer(tpaHereList.get(player.getUniqueId())).sendMessage("Where is your " + teleportItem + "§f?");
                }
            }else {
                // Player issued the command themselves
                player.sendMessage("Command issued by themselves");
                for (ItemStack item : player.getInventory()) {
                    if (item != null && item.getItemMeta().hasCustomName() && item.getItemMeta().getDisplayName().equals(teleportItem)) {
                        player.sendMessage("You have used a " + teleportItem + ".");
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
                    player.sendMessage("Where is your " + teleportItem + "§f?");
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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}