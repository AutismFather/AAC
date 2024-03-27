package com.autcraft.aac.commands;

import com.autcraft.aac.AAC;
import com.autcraft.aac.objects.InventoryGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    AAC plugin;

    public MainCommand(AAC plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        // No arguments provided - get help text
        if( args.length == 0 ){
            // Error: Invalid permission
            if( !commandSender.hasPermission("aac.help") ) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            commandSender.sendMessage(Component.text(plugin.getConfig().getString("settings.helptext")).color(TextColor.color(60, 180 ,180)));
            return true;
        }


        // Reload the config and re-initalize the panel items
        if( args[0].equalsIgnoreCase("reload") ){
            // Error: Invalid permission
            if( !commandSender.hasPermission("aac.reload") ) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            // reload all the things
            plugin.reload();

            commandSender.sendMessage(Component.text(plugin.getConfig().getString("settings.reloadtext")).color(TextColor.color(60, 180, 180)));
            return true;
        }


        // Get the knowledge book!
        if( args[0].equalsIgnoreCase("get") ){
            // Error: Invalid permission
            if( !commandSender.hasPermission("aac.get") ){
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            // If player
            if( commandSender instanceof Player) {
                Player player = (Player) commandSender;
                InventoryGUI inventoryGUI = plugin.getInventoryGUI();

                // Put the item into the player's inventory that will trigger the AAC GUI
                player.getInventory().addItem(inventoryGUI.getTool());
                plugin.debug("Gave AAC tool to " + player.getName());
            }
            else {
                commandSender.sendMessage(plugin.errorMessage("error_no_console"));
                return true;
            }
        }

        // If the player is trying to give the book to another player
        if( args[0].equalsIgnoreCase("give") ){
            // Error: Invalid permission
            if( !commandSender.hasPermission("aac.reload") ) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            // Error: /aac give command ran but no player provided.
            if( args.length == 1 ){
                commandSender.sendMessage(plugin.errorMessage("error_player_not_provided"));
                return true;
            }

            // Error: /aac give <player> command ran but player is not online
            if( plugin.getServer().getPlayer(args[1]) == null ){
                commandSender.sendMessage(plugin.errorMessage("error_player_not_online"));
                return true;
            }

            // Get player based on args[1] given in command
            Player player = plugin.getServer().getPlayer(args[1]);
            InventoryGUI inventoryGUI = plugin.getInventoryGUI();

            player.getInventory().addItem(inventoryGUI.getTool());

            commandSender.sendMessage(Component.text(plugin.getString("success_tool_given_to_player")).color(TextColor.color(60, 180, 180)));
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> options = new ArrayList<>();

        if( args.length == 1 ){
            options.add("get");
            options.add("reload");
        }
        Collections.sort(options);
        if(!options.isEmpty())
            return options;

        return null;
    }
}
