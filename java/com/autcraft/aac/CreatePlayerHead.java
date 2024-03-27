package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CreatePlayerHead {
    public ItemStack getSkull(UUID uuid, String texture, Component customName, List<Component> lore) {
        // Create the item stack in advance
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        //String url = Base64.getEncoder().withoutPadding().encodeToString(texture.getBytes());
        Base64.Decoder dec = Base64.getDecoder();
        String decoded = new String(dec.decode(texture));

        // Should be something like this:
        // {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/9631597dce4e4051e8d5a543641966ab54fbf25a0ed6047f11e6140d88bf48f"}}}
        // System.out.println("URL = " + decoded.substring(28, decoded.length() - 4));
        String url = decoded.substring(28, decoded.length() - 4);

        PlayerProfile profile = Bukkit.createPlayerProfile(uuid);
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new URL(url); // The URL to the skin, for example: https://textures.minecraft.net/texture/18813764b2abc94ec3c3bc67b9147c21be850cdf996679703157f4555997ea63a
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Invalid URL", exception);
        }
        textures.setSkin(urlObject); // Set the skin of the player profile to the URL
        profile.setTextures(textures); // Set the textures back to the profile

        // Set all that data to the itemstack metadata
        meta.setOwnerProfile(profile);

        // Display name
        if( customName != null ) {
            meta.displayName(customName);
        }

        // Lore
        if( !lore.isEmpty() ){
            meta.lore(lore);
        }

        skull.setItemMeta(meta);

        return skull;
    }
}
