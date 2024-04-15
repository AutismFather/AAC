package com.autcraft.aac.events;

import com.autcraft.aac.AAC;
import com.autcraft.aac.objects.InventoryGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class Click implements Listener {
    AAC plugin;

    public Click(AAC plugin){
        this.plugin = plugin;
    }

    /**
     * Tool item (book) to open GUI
     *
     * @param e
     */
    @EventHandler
    public void onClick(PlayerInteractEvent e){
        Player player = (Player) e.getPlayer();
        InventoryGUI inventoryGUI = plugin.getInventoryGUI();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemStack itemOffHand = player.getInventory().getItemInOffHand();

        // The AAC tool has to be in either the main hand or offhand to work
        boolean validItem = inventoryGUI.isItemPanelTool(item) || inventoryGUI.isItemPanelTool(itemOffHand);

        // Check to see if the item in the player's hand is the AAC Tool
        // Make sure it's not a physical action, such as stepping onto a pressure plate
        if (validItem && e.getAction() != Action.PHYSICAL) {
            // If, for whatever reason, the player doesn't have permission to open the gui
            if( !player.hasPermission("aac.open") ){
                player.sendMessage(plugin.getString("error_no_permission"));
                return;
            }

            player.openInventory(inventoryGUI.getGUI(player, 1));

            e.setCancelled(true);
        }
    }

    /**
     * Clicking an item in the GUI inventory screen
     *
     * @param e
     */
    @EventHandler
    public void inventoryClick(InventoryClickEvent e){

        Component inventoryTitle = e.getView().title();
        Component GUITitle = Component.text(plugin.getConfig().getString("settings.title"));

       // If the inventory clicked on is the same as what was created by AAC
       if( inventoryTitle.equals(GUITitle) && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR ){

           // Object vars
           Player player = (Player) e.getWhoClicked();
           ItemStack clickedItem = e.getCurrentItem();
           InventoryGUI inventoryGUI = plugin.getInventoryGUI();

           // If the next button is clicked
           if( inventoryGUI.isNextButton(clickedItem) ){
               // Open GUI for the next page
               player.openInventory(inventoryGUI.getGUI(player, inventoryGUI.getNextPage(clickedItem)));
           }
           // If the previous button is clicked
           else if( inventoryGUI.isPreviousButton(clickedItem) ){
               // Open GUI for the previous page
               player.openInventory(inventoryGUI.getGUI(player, inventoryGUI.getPreviousPage(clickedItem)));
           }
           // Otherwise, output to the chat
           else {

               // If it has persistent data, then it has the string to output
               String output = inventoryGUI.getPersistentDataContainer(clickedItem);

               // So long as the output isn't blank, send it to the chat
               if (output != null) {
                   if( plugin.isInCooldown(player) ){
                       HashMap<String, String> replacements = new HashMap<>();
                       replacements.put("{SECONDS}", "" + plugin.getCooldownRemaining(player));

                       player.sendMessage(plugin.errorMessage("error_player_in_cooldown", replacements));
                   }
                   else {
                       plugin.toConsole(player.getName() + " is using AAC to generate the following text in chat:");
                       player.chat(output);

                       // Message sent successfully, now apply cooldown
                       plugin.addPlayerCooldown(player);
                   }
               }
               else {
                   plugin.toConsole("Error: No output was stored because the plugin could not set the persistent data for the panel option.");
               }
           }

           e.setCancelled(true);
       }
    }

    public boolean compareMaterials(Material material, String icon){
        Material panelIcon = Material.getMaterial(icon.toUpperCase());
        if( panelIcon == null )
            return false;

        return material == panelIcon;
    }
}
