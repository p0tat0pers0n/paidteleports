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
    String[] teleportCommands = {"tp", "warp", "ewarp", "warps", "ewarps", "essentials:warp", "essentials:ewarp", "essentials:warps", "essentials:ewarps", "tpa", "tp2p", "tpask", "tpahere", "etpa", "etp2p", "etpask", "etpahere", "essentials:tpa", "essentials:tp2p", "essentials:tpask", "essentials:tpahere", "essentials:etpa", "essentials:etp2p", "essentials:etpask", "essentials:etpahere", "home", "homes", "ehome", "ehomes", "essentials:home", "essentials:homes", "essentials:ehome", "essentials:ehomes", "call", "ecall", "essentials:call", "essentials:ecall"};
    String[] tpaTeleport = {"tpa", "tp2p", "tpask", "etpa", "etp2p", "etpask", "essentials:tpa", "essentials:tp2p", "essentials:tpask", "essentials:etpa", "essentials:etp2p", "essentials:etpask", "call", "ecall", "essentials:call", "essentials:ecall"};
    String[] tpaHereTeleport = {"tpahere", "essentials:tpahere", "etpahere", "essentials:etpahere"};
    String[] tpacceptCommands = {"tpaccept", "etpaccept", "essentials:tpaccept", "essentials:etpaccept"};

    String[] paidTeleportsArgs0 = {"help", "reload", "setitem", "freewarps"};
    String[] paidTeleportsArgs1 = {"toggle", "add", "remove"};

    HashMap<UUID, UUID> tpaHereList = new HashMap<>();
    HashMap<UUID, UUID> tpaList = new HashMap<>();
    HashMap<UUID, UUID> inverseTpaList = new HashMap<>();
    HashMap<UUID, Integer> teleportingPlayers = new HashMap<>();

    String teleportItem = this.getConfig().getString("payment-item");

    String paidItemMessage = this.getConfig().getString("paid-message");
    String droppedItemMessage = this.getConfig().getString("dropped-item-message");
    String noPaymentItemMessage = this.getConfig().getString("no-payment-item-message");
    List<String> freeWarps = this.getConfig().getStringList("free-warps");

    List<String> commandArguments = new ArrayList<>();

    public boolean reloadConfigFile() {
        try {
            teleportItem = this.getConfig().getString("payment-item");
            paidItemMessage = this.getConfig().getString("paid-message");
            droppedItemMessage = this.getConfig().getString("dropped-item-message");
            noPaymentItemMessage = this.getConfig().getString("no-payment-item-message");
            freeWarps = this.getConfig().getStringList("free-warps");
            return true;
        }catch(Exception e) {
            return false;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String @NotNull [] args) {
        teleportItem = this.getConfig().getString("payment-item");
        if (cmd.getName().equalsIgnoreCase("paidteleports") && args.length > 0) {
            this.getConfig().options().copyDefaults(true);
            if (args[0].equals("help")) {
                // Display help information
                sender.sendMessage("/paidteleports freewarps toggle\n§cToggles if all warps are free.§f\n/paidteleports setitem\n§cSets the teleport item.§f\n/paidteleports freewarps add/remove [WarpName]\n§cAdds or removes a warp from the free-warp list.");
            }
            if (args[0].equals("reload")) {
                // Reloads the config file
                this.reloadConfig();
                if (reloadConfigFile()) {
                    sender.sendMessage("§cReloaded Successfully");
                }else {
                    sender.sendMessage("§cAn error has occured getting new configs");
                }
                return true;
            }

            if (args[0].equals("setitem")) {
                // Sets the item that the player is holding to the teleport item
                try {
                    String heldItem = Objects.requireNonNull(Objects.requireNonNull(Bukkit.getPlayer(sender.getName())).getInventory().getItemInMainHand().getItemMeta().customName()).children().toString();
                    this.getConfig().set("payment-item", heldItem);
                    this.saveConfig();
                    sender.sendMessage("§cItem set successfully");
                    if (!reloadConfigFile()) {
                        sender.sendMessage("§cError getting updated config values. Restarting the server should fix this");
                    }
                }catch(NullPointerException e) {
                    sender.sendMessage("§c§lError occurred when setting item. Are you sure you're holding the right item?");
                }

            }

            if (args[0].equals("freewarps")) {
                freeWarps = this.getConfig().getStringList("free-warps");
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
                                sender.sendMessage("§cMissing warp name argument /paidteleports freewarps add [WarpName] <-- This");
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
                            }else {
                                sender.sendMessage("§cMissing warp name argument /paidteleports freewarps remove [WarpName] <-- This");
                            }
                            return true;
                    }
                }else {
                    // No args [1] therefore list the warps that are free
                    sender.sendMessage("§cAll warps free:§f " + this.getConfig().getBoolean("all-warps-free"));
                    StringBuilder freeWarpsList = new StringBuilder();
                    freeWarpsList.append("§cFree Warps: ");
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
            sender.sendMessage("§cPossible Arguments:§f help, reload, setitem, freewarps [toggle, add, remove]");
            return true;
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList(paidTeleportsArgs0));
        }else if(args.length == 2 && args[0].equals("freewarps")) {
            list.addAll(Arrays.asList(paidTeleportsArgs1));
        }
        return list;
    }

    boolean paymentChecker(Player player, String command, String playerInArgs) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getItemMeta().hasCustomName() && Objects.requireNonNull(item.getItemMeta().customName()).children().toString().equals(teleportItem)) {
                for (String commands : tpaHereTeleport) {
                    // TpaHere processing
                    if (commands.equals(command)) {
                        UUID playerToTpaHereUUID = Objects.requireNonNull(Bukkit.getPlayer(playerInArgs)).getUniqueId();
                        if (!teleportingPlayers.containsKey(playerToTpaHereUUID)) {
                            teleportingPlayers.put(playerToTpaHereUUID, 124);
                        }else {
                            teleportingPlayers.replace(playerToTpaHereUUID, 124);
                        }
                        if (!tpaHereList.containsKey(playerToTpaHereUUID)) {
                            tpaHereList.put(playerToTpaHereUUID, player.getUniqueId());
                        }else {
                            tpaHereList.replace(playerToTpaHereUUID, player.getUniqueId());
                        }
                        return false;
                    }
                }
                tpaHereList.remove(player.getUniqueId());
                for (String commands : tpaTeleport) {
                    // Tpa command processing
                    if (command.equals(commands)) {
                        UUID playerInArgsUUID = Objects.requireNonNull(Bukkit.getPlayer(playerInArgs)).getUniqueId();

                        if (!teleportingPlayers.containsKey(player.getUniqueId())) {
                            teleportingPlayers.put(player.getUniqueId(), 124);
                        }else {
                            teleportingPlayers.replace(player.getUniqueId(), 124);
                        }
                        if (!tpaList.containsKey(playerInArgsUUID)) {
                            tpaList.put(player.getUniqueId(), playerInArgsUUID);
                            inverseTpaList.put(playerInArgsUUID, player.getUniqueId());
                        }else {
                            tpaList.replace(player.getUniqueId(), playerInArgsUUID);
                            inverseTpaList.replace(playerInArgsUUID, player.getUniqueId());
                        }
                        return false;
                    }
                }
                inverseTpaList.remove(tpaList.get(player.getUniqueId()));
                tpaList.remove(player.getUniqueId());
                // Home/Warp processing
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
        freeWarps = this.getConfig().getStringList("free-warps");

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

        for (String tpacceptCommand : tpacceptCommands) {
            if (tpacceptCommand.equals(command)) {
                // If the player issued a tp accept command then check if they are accepting a tpa or tpa here and reduce their time
                if (teleportingPlayers.containsKey(player.getUniqueId())) {
                    // Player is teleporting therefore, is accepting a tpa here
                    teleportingPlayers.replace(player.getUniqueId(), 4);
                }else {
                    // Player is not teleporting therefore they are accepting a tpa request
                    teleportingPlayers.replace(inverseTpaList.get(player.getUniqueId()), 4);
                }
            }
        }
    }

    @EventHandler
    public void playerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        // Removes player from teleporting list if they teleport within the time
        if (String.valueOf(cause).equals("COMMAND") && teleportingPlayers.containsKey(player.getUniqueId()) && teleportingPlayers.get(player.getUniqueId()) < 5) {
            // If player that is being tp'd was tpahere'd charge the player that did /tpahere
            if (tpaHereList.containsKey(player.getUniqueId())) {
                for (ItemStack item : Objects.requireNonNull(Bukkit.getPlayer(tpaHereList.get(player.getUniqueId()))).getInventory()) {
                    if (item != null && item.getItemMeta().hasCustomName() && Objects.requireNonNull(item.getItemMeta().customName()).children().toString().equals(teleportItem)) {
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
                    if (item != null && item.getItemMeta().hasCustomName() && Objects.requireNonNull(item.getItemMeta().customName()).children().toString().equals(teleportItem)) {
                        player.sendMessage(paidItemMessage);
                        item.setAmount(item.getAmount() - 1);
                        player.updateInventory();
                        teleportingPlayers.remove(player.getUniqueId());
                        inverseTpaList.remove(tpaList.get(player.getUniqueId()));
                        tpaList.remove(player.getUniqueId());
                        break;
                    }
                }
                if (teleportingPlayers.containsKey(player.getUniqueId())) {
                    // Cancel teleport because they dropped the crystal
                    event.setCancelled(true);
                    teleportingPlayers.remove(player.getUniqueId());
                    inverseTpaList.remove(tpaList.get(player.getUniqueId()));
                    tpaList.remove(player.getUniqueId());
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

        commandArguments.add("help");
        commandArguments.add("freewarps");
        commandArguments.add("reload");

        tpaHereList.clear();
        tpaList.clear();
        teleportingPlayers.clear();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerUUID : teleportingPlayers.keySet()) {
                    teleportingPlayers.replace(playerUUID, teleportingPlayers.get(playerUUID) - 1);
                    if (teleportingPlayers.get(playerUUID) < 0) {
                        teleportingPlayers.remove(playerUUID);
                        tpaHereList.remove(playerUUID);
                        inverseTpaList.remove(tpaList.get(playerUUID));
                        tpaList.remove(playerUUID);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        teleportingPlayers.remove(event.getPlayer().getUniqueId());
        tpaHereList.remove(event.getPlayer().getUniqueId());
        tpaList.remove(event.getPlayer().getUniqueId());
        inverseTpaList.remove(tpaList.get(event.getPlayer().getUniqueId()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}