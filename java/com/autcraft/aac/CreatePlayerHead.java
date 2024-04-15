package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class CreatePlayerHead {

    JSONParser PARSER = new JSONParser();

    /**
     * Retrieve a player head just from a player's name
     *
     * @param playerName
     * @return
     */
    public ItemStack getSkull(String playerName, List<Component> lore) {
        String uuid = null;
        String texture = null;
        try {
            // Retrieve the player's UUID if at all possible
            // If it fails, that player probably doesn't exist
            uuid = getUUIDFromMojangByName(playerName);
        } catch (IOException | ParseException e) {
            return null;
        }

        // Somehow mojang returned a blank uuid?
        if (uuid == null) {
            Bukkit.getLogger().info("Error: Could not retrieve UUID for player " + playerName + ". Using a Player Head for now but try \"/aac reload\" and see if it fixes it.");
            return null;
        }

        // Try to retrieve the texture from Mojang's Session server
        // If it fails, it means that Mojang's servers are down.
        try {
            texture = getSkinTextureByUUID(UUID.fromString(uuid));
        } catch (IOException | org.json.simple.parser.ParseException e) {
            return null;
        }

        // Now run the main function to return the item stack
        return getSkull(UUID.randomUUID(), texture, Component.text(playerName), lore);
    }

    public ItemStack getSkull(UUID uuid, String texture, Component customName, List<Component> lore) {
        // Create the item stack in advance
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        String url = null;

        // Depending on the length of the texture string, use the appropriate method to extra the URL
        if (texture.length() > 200) {
            try {
                url = getSkinURLFromMojang(texture);
            } catch (UnsupportedEncodingException | org.json.simple.parser.ParseException e) {
                Bukkit.getLogger().info("Unable to retrieve URL from " + texture);
            }
        } else {
            url = getSkinURLFromString(texture);
        }

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
        if (customName != null) {
            meta.displayName(customName);
        }

        // Lore
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        skull.setItemMeta(meta);

        return skull;
    }

    /**
     * Retrieve the player's UUID from just their name
     *
     * @param name
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public String getUUIDFromMojangByName(String name) throws IOException, ParseException {
        String uuid = null;

        // First obvious method is to just get it from the server itself.
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            return player.getUniqueId().toString();
        }

        // If the server has no record of the player, get it from Mojang's API
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        int responsecode = conn.getResponseCode();

        if (responsecode != 200) {
            //throw new RuntimeException("HttpResponseCode: " + responsecode);
            return null;
        } else {

            String inline = "";
            Scanner scanner = new Scanner(url.openStream());

            //Write all the JSON data into a string using a scanner
            while (scanner.hasNext()) {
                inline += scanner.nextLine();
            }

            //Close the scanner
            scanner.close();

            //Using the JSON simple library parse the string into a json object
            JSONParser parse = new JSONParser();
            JSONObject data_obj = null;
            try {
                data_obj = (JSONObject) parse.parse(inline);
            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }

            //Get the required object from the above created object
            if (data_obj != null) {
                uuid = data_obj.get("id").toString();
            }
        }
        if (uuid != null) {
            String result = uuid;
            result = uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32);
            uuid = result;
        }

        return uuid;
    }


    /**
     * If getting a head for a player, we must retrieve the texture from Mojang's Session servers.
     *
     * @param uuid
     * @return
     * @throws IOException
     * @throws org.json.simple.parser.ParseException
     */
    public String getSkinTextureByUUID(UUID uuid) throws IOException, org.json.simple.parser.ParseException {
        String texture = null;
        String apiURL = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString();

        URL url = new URL(apiURL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        int responsecode = conn.getResponseCode();

        if (responsecode != 200) {
            throw new RuntimeException("HttpResponseCode: " + responsecode);
        } else {

            String inline = "";
            Scanner scanner = new Scanner(url.openStream());

            //Write all the JSON data into a string using a scanner
            while (scanner.hasNext()) {
                inline += scanner.nextLine();
            }

            //Close the scanner
            scanner.close();

            //Using the JSON simple library parse the string into a json object
            JSONParser parse = new JSONParser();
            JSONObject data_obj = (JSONObject) parse.parse(inline);
            JSONArray propertiesArray = (JSONArray) data_obj.get("properties");
            for (JSONObject property : (List<JSONObject>) propertiesArray) {
                String name = (String) property.get("name");
                if (name.equals("textures")) {
                    return (String) property.get("value");
                }
            }

            //Get the required object from the above created object
            System.out.println(data_obj.values());
            texture = data_obj.get("properties").toString();
        }

        return texture;
    }

    /**
     * Get Skin URL from string entered into config file
     *
     * @param base64
     * @return
     */
    private String getSkinURLFromString(String base64) {
        //String url = Base64.getEncoder().withoutPadding().encodeToString(texture.getBytes());
        Base64.Decoder dec = Base64.getDecoder();
        String decoded = new String(dec.decode(base64));

        // Should be something like this:
        // {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/9631597dce4e4051e8d5a543641966ab54fbf25a0ed6047f11e6140d88bf48f"}}}
        // System.out.println("URL = " + decoded.substring(28, decoded.length() - 4));
        return decoded.substring(28, decoded.length() - 4);
    }

    /**
     * Get Skin URL from long string returned by Mojang's API
     *
     * @param base64
     * @return
     * @throws UnsupportedEncodingException
     * @throws org.json.simple.parser.ParseException
     */
    private String getSkinURLFromMojang(String base64) throws UnsupportedEncodingException, org.json.simple.parser.ParseException {
        String texture = null;
        String decodedBase64 = new String(Base64.getDecoder().decode(base64), "UTF-8");
        JSONObject base64json = (JSONObject) PARSER.parse(decodedBase64);
        JSONObject textures = (JSONObject) base64json.get("textures");
        if (textures.containsKey("SKIN")) {
            JSONObject skinObject = (JSONObject) textures.get("SKIN");
            texture = (String) skinObject.get("url");
        }
        return texture;
    }
}
