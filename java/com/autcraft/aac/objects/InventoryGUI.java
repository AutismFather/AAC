package com.autcraft.aac.objects;

import com.autcraft.aac.AAC;
import com.autcraft.aac.CreatePlayerHead;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class InventoryGUI {
    AAC plugin;

    private Map<String, ItemStack> panelOptions = new HashMap<>();
    private Map<String, String> panelTool = new HashMap<>();
    private NamespacedKey namespacedKeyAACTool;
    private NamespacedKey namespacedKey;
    private NamespacedKey namespacedKeyNext;
    private NamespacedKey namespacedKeyPrevious;


    public InventoryGUI(AAC plugin, String namespaceKey) {
        this.plugin = plugin;
        this.namespacedKeyAACTool = new NamespacedKey(plugin, "AAC_Tool");
        this.namespacedKey = new NamespacedKey(plugin, namespaceKey);
        this.namespacedKeyNext = new NamespacedKey(plugin, "next");
        this.namespacedKeyPrevious = new NamespacedKey(plugin, "previous");

        // Dump the config.yml panel items into the hashmap
        initializePanelOptions();
        initializePanelTool();

        plugin.debug("Inventory GUI initialized successfully");
    }

    public void reload() {
        initializePanelOptions();
        initializePanelTool();
    }

    public String getPanelToolIcon() {
        return this.panelTool.get("icon");
    }

    public String getPanelToolName() {
        return this.panelTool.get("name");
    }

    public String getPanelToolDescription() {
        return this.panelTool.get("lore");
    }


    /**
     * Return true or false if the item being checked has the persistent data container with AAC_Tool value stored.
     *
     * @param itemStack
     * @return
     */
    public boolean isItemPanelTool(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (!meta.getPersistentDataContainer().has(this.namespacedKeyAACTool)) {
            return false;
        }

        return meta.getPersistentDataContainer().get(this.namespacedKeyAACTool, PersistentDataType.STRING).equalsIgnoreCase("AAC_Tool");
    }


    /**
     * Returns the String value of the persistent data container for itemstack specified
     *
     * @param itemStack
     * @return
     */
    public String getPersistentDataContainer(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        if (dataContainer.has(this.namespacedKey)) {
            return meta.getPersistentDataContainer().get(this.namespacedKey, PersistentDataType.STRING);
        }

        return null;
    }

    /**
     * Set the persistent data container for the item
     *
     * @param itemStack
     * @param output
     */
    private void SetPersistentDataContainer(ItemStack itemStack, String output) {
        if (itemStack == null) {
            plugin.toConsole("Failed to set output info: " + output);
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        meta.getPersistentDataContainer().set(this.namespacedKey, PersistentDataType.STRING, output);
        itemStack.setItemMeta(meta);
    }

    /**
     * Iinitialize the panel items by putting the itemstack data into a map
     */
    public void initializePanelOptions() {
        plugin.debug("Initirializing Panel from config.");
        panelOptions.clear();

        // Loop over the panel options in the config
        for (String path : plugin.getConfig().getConfigurationSection("panel").getKeys(false)) {
            ItemStack itemStack = null;
            String panelItem = "panel." + path;
            String icon = plugin.getConfig().getString(panelItem + ".icon", "");
            String name = plugin.getConfig().getString(panelItem + ".name", "");
            String playerName = plugin.getConfig().getString(panelItem + ".player", "");
            String texture = plugin.getConfig().getString(panelItem + ".texture", "");
            List<Component> lore = new ArrayList<Component>();
            lore.add(Component.text(plugin.getConfig().getString(panelItem + ".lore", "")));
            String output = plugin.getConfig().getString(panelItem + ".output", "");

            // Get material based on config entry. If material is not found in game, skip
            Material material = Material.getMaterial(icon.toUpperCase());
            if (material == null) {
                plugin.toConsole("Error in " + path + ": Material " + icon + " not found.");
                break;
            }

            // If material is set to player_head
            if (icon.equalsIgnoreCase("PLAYER_HEAD") && (!texture.equals("") || !playerName.equals(""))) {

                // Prioritize texture. If they entered one, they probably want it.
                if (!texture.isEmpty()) {
                    // Create the player head with texture and other info
                    CreatePlayerHead playerHead = new CreatePlayerHead();
                    itemStack = playerHead.getSkull(UUID.randomUUID(), texture, Component.text(playerName), lore);

                    // If something failed in retrieving the skull, rather than just break completely, give the panel a blank player head
                    if (itemStack == null) {
                        itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
                    }
                }
                // Second is player name. If one is set, get the player's currect skin file
                else if (!playerName.isEmpty()) {
                    // Create the player head with texture and other info
                    CreatePlayerHead playerHead = new CreatePlayerHead();
                    itemStack = playerHead.getSkull(playerName, lore);

                    // If something failed in retrieving the skull, rather than just break completely, give the panel a blank player head
                    if (itemStack == null) {
                        itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
                    }
                }
                // If neither is set, just use the generic player head
                else {
                    itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
                }

                // set Display and lore info
                SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
                meta.displayName(Component.text(name).color(TextColor.color(80, 120, 255)));
                meta.lore(lore);
                itemStack.setItemMeta(meta);
            }
            // Any other material
            else {
                itemStack = new ItemStack(material, 1);
                ItemMeta meta = itemStack.getItemMeta();

                // Set display name and lore
                meta.displayName(Component.text(name).color(TextColor.color(80, 120, 255)));
                meta.lore(lore);

                itemStack.setItemMeta(meta);
            }

            // Store the output string of the panel item for when it's clicked.
            if (this.namespacedKey == null) {
                plugin.debug("Warning, namespacedkey is null!");
                break;
            } else {
                // Set the persistent data contain info
                SetPersistentDataContainer(itemStack, output);
            }

            // Add panel option to inventory GUI
            panelOptions.put(path, itemStack);
        }
    }

    public void initializePanelTool() {
        panelTool.clear();

        panelTool.put("icon", plugin.getConfig().getString("tool.icon", "knowledge_book"));
        panelTool.put("name", plugin.getConfig().getString("tool.name", "Augmentative and Alternative Communication"));
        panelTool.put("lore", plugin.getConfig().getString("tool.lore", "Click open AAC"));
        plugin.debug("Panel Tool Initialized.");
    }

    /**
     * Returns the inventory/GUI for the clickable items
     *
     * @param player
     * @return
     */
    public Inventory getGUI(Player player, int page) {
        int inventorySize = 54;
        int endIndex = inventorySize - 10; // -10 because we want the bottom row for navigation so it should be left empty
        int startIndex = page * endIndex - endIndex;
        if (page > 1) {
            startIndex = startIndex + 1;
        }
        int lastIndex = startIndex + endIndex;

        ArrayList<String> sortedKeys = new ArrayList<String>(panelOptions.keySet());
        Collections.sort(sortedKeys);

        String title = plugin.getConfig().getString("settings.title");

        // Create the inventory
        Inventory inventory = plugin.getServer().createInventory(player, inventorySize, title);

        /*
         Loop over panel options to populate inventory
        */
        int slot = 0;
        int counter = 0;

        // Loop over panel to put panel items into inventory slots.
        for (String key : sortedKeys) {
            if (counter >= startIndex && counter <= lastIndex) {
                inventory.setItem(slot, panelOptions.get(key));
                slot++;
            }
            counter++;
        }

        plugin.debug("Panel size: " + panelOptions.size() + ". Page #: " + page + ". Start Index: " + startIndex + ". Last Index: " + lastIndex);

        // Next and previous buttons
        // If page 1, there is no previous page.
        if (page != 1) {
            inventory.setItem(45, getPreviousButton(page - 1));
        }
        // Check for how many items there are. If more than what fits on this page, show the next page button
        if (panelOptions.size() > lastIndex) {
            inventory.setItem(53, getNextButton(page + 1));
        }

        return inventory;
    }


    /**
     * Retrieve the panel tool item to put into the player's inventory when they run the command /aac get
     *
     * @return
     */
    public ItemStack getTool() {
        String displayName = panelTool.get("name");
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(panelTool.get("lore")));

        // Get the material, if there is one. If not, default it to knowledge book.
        Material material = Material.getMaterial(panelTool.get("icon"));
        if (material == null)
            material = Material.KNOWLEDGE_BOOK;

        // Get the item stack and set data to it.
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        // Set display and lore data
        meta.displayName(Component.text(displayName));
        meta.lore(lore);

        // Store persistent data as an identifier to know when this tool is being clicked
        meta.getPersistentDataContainer().set(this.namespacedKeyAACTool, PersistentDataType.STRING, "AAC_Tool");

        item.setItemMeta(meta);

        return item;
    }

    /**
     * Generate and return the Next Button item stack
     *
     * @return
     */
    public ItemStack getNextButton(int page) {
        ItemStack itemStack;
        Component displayName = Component.text(plugin.getConfig().getString("nexticon.name", "Next"));
        String materialName = plugin.getConfig().getString("nexticon.material", "BEACON");
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(plugin.getConfig().getString("nexticon.lore", "Go to Next Page")));
        String texture = plugin.getConfig().getString("nexticon.texture");

        // Default check. We can't have a player head without a texture.
        // If it is blank, reset the material to a beacon.
        if (materialName.equalsIgnoreCase("player_head") && texture.equals("")) {
            plugin.debug("Material is set to player head but texture is blank.");
            materialName = "beacon";
        }

        // Get material, if there is one
        Material material = Material.getMaterial(materialName.toUpperCase());

        // If material is set to player_head
        if (materialName.equalsIgnoreCase("PLAYER_HEAD")) {

            // Create the player head with texture and other info
            CreatePlayerHead playerHead = new CreatePlayerHead();
            itemStack = playerHead.getSkull(UUID.randomUUID(), texture, displayName, lore);

        }
        // Any other material
        else {
            itemStack = new ItemStack(material, 1);
            ItemMeta meta = itemStack.getItemMeta();

            meta.displayName(displayName);
            meta.lore(lore);

            itemStack.setItemMeta(meta);

        }
        // Get the metadata to set the persistent data container with the page number to load
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(namespacedKeyNext, PersistentDataType.INTEGER, page);
        itemStack.setItemMeta(meta);

        return itemStack;
    }


    /**
     * Generate and return the Previous Button item stack
     *
     * @return
     */
    public ItemStack getPreviousButton(int page) {
        ItemStack itemStack;
        Component displayName = Component.text(plugin.getConfig().getString("previousicon.name", "Next"));
        String materialName = plugin.getConfig().getString("previousicon.material", "BEACON");
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(plugin.getConfig().getString("previousicon.lore", "Go to Next Page")));
        String texture = plugin.getConfig().getString("previousicon.texture");

        // Default check. We can't have a player head without a texture.
        // If it is blank, reset the material to a beacon.
        if (materialName.equalsIgnoreCase("player_head") && texture.equals("")) {
            plugin.debug(plugin.getString("error_player_head_no_material"));
            materialName = "beacon";
        }

        // Get material, if there is one
        Material material = Material.getMaterial(materialName.toUpperCase());

        // If material is set to player_head
        if (materialName.equalsIgnoreCase("PLAYER_HEAD")) {

            // Create the player head with texture and other info
            CreatePlayerHead playerHead = new CreatePlayerHead();
            itemStack = playerHead.getSkull(UUID.randomUUID(), texture, displayName, lore);
        }
        // Any other material
        else {
            itemStack = new ItemStack(material, 1);
            ItemMeta meta = itemStack.getItemMeta();

            meta.displayName(displayName);
            meta.lore(lore);

            itemStack.setItemMeta(meta);

            return itemStack;
        }

        // Get the metadata to set the persistent data container with the page number to load
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(namespacedKeyPrevious, PersistentDataType.INTEGER, page);
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    /**
     * Returns true or false if the persistent data exists within the item stack
     *
     * @param itemStack
     * @return
     */
    public boolean isNextButton(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().has(this.namespacedKeyNext);
    }

    public int getNextPage(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().get(this.namespacedKeyNext, PersistentDataType.INTEGER);
    }

    public boolean isPreviousButton(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().has(this.namespacedKeyPrevious);
    }

    public int getPreviousPage(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().get(this.namespacedKeyPrevious, PersistentDataType.INTEGER);
    }
}
