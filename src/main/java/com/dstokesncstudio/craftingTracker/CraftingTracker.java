package com.dstokesncstudio.craftingTracker;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CraftingTracker extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Map<Material, Integer>> craftingStats = new HashMap<>();
    //TODO Add save and load of all the player stats once plugin updates.
    File dataFile;

    //TODO Add this once the database class is set.
    //Database Stuff //
    /*
    private boolean databaseEnabled;
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    */

    @Override
    public void onEnable() {
        //TODO This is for the saving and loading of the config file
       /*
        // Save the default config if it doesn't exist
        // saveDefaultConfig();

        // Load the configuration
        // loadConfiguration();

        */

        // Register events and commands
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(this.getCommand("craftingstats")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("craftingstats")).setTabCompleter(this);

        dataFile = new File(getDataFolder(), "craftingStats.yml");
        if (!dataFile.exists()) {
            saveResource("craftingStats.yml", false); // Create the file if it doesn't exist
        }
        loadCraftingStats();
    }
    @Override
    public void onDisable() {
        saveCraftingStats();
    }
    //TODO use this latter when creating the database class.
    private void loadConfiguration() {
        /*
        FileConfiguration config = getConfig();

        databaseEnabled = config.getBoolean("database.enabled");
        dbHost = config.getString("database.host");
        dbPort = config.getInt("database.port");
        dbName = config.getString("database.database");
        dbUser = config.getString("database.user");
        dbPassword = config.getString("database.password");

        getLogger().info("Database enabled: " + databaseEnabled);
        if (databaseEnabled) {
            getLogger().info("Connecting to database at " + dbHost + ":" + dbPort);
            // Initialize your database connection here
        }
         */
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            UUID playerId = player.getUniqueId();
            Material itemType = event.getRecipe().getResult().getType();

            craftingStats.putIfAbsent(playerId, new HashMap<>());
            Map<Material, Integer> playerStats = craftingStats.get(playerId);
            playerStats.put(itemType, playerStats.getOrDefault(itemType, 0) + event.getRecipe().getResult().getAmount());
            saveCraftingStats();
        }

    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadConfiguration();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                openMainMenu((Player) sender);
                return true;
            } else {
                sender.sendMessage("This command can only be run by a player.");
                return false;
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("leaderboard")) {
            if (sender instanceof Player) {
                openLeaderboardGUI((Player) sender, 0);
                return true;
            } else {
                sender.sendMessage("This command can only be run by a player.");
                return false;
            }
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage("Player not found.");
            return false;
        }

        openCraftingStatsGUI((Player) sender, targetPlayer.getUniqueId(), 0);
        return true;
    }

    private void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Main Menu");

        // Create Leaderboard item
        ItemStack leaderboardItem = new ItemStack(Material.DIAMOND);
        ItemMeta leaderboardMeta = leaderboardItem.getItemMeta();
        if (leaderboardMeta != null) {
            leaderboardMeta.setDisplayName("Leaderboard");
            leaderboardItem.setItemMeta(leaderboardMeta);
        }
        inventory.setItem(11, leaderboardItem);

        // create Close Main Menu //
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("Close");
            closeItem.setItemMeta(closeMeta);
        }
        inventory.setItem(13, closeItem);

        // Create Crafting Stats item
        ItemStack statsItem = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("Crafting Stats");
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(15, statsItem);

        // Open inventory
        player.openInventory(inventory);
    }

    private void openCraftingStatsGUI(Player viewer, UUID targetPlayerId, int page) {
        Map<Material, Integer> playerStats = craftingStats.getOrDefault(targetPlayerId, Collections.emptyMap());

        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) playerStats.size() / itemsPerPage);
        totalPages = Math.max(totalPages, 1);

        Inventory inventory = Bukkit.createInventory(null, 54, "Crafting Stats - Page " + (page + 1));

        List<Map.Entry<Material, Integer>> statsList = new ArrayList<>(playerStats.entrySet());
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, statsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<Material, Integer> entry = statsList.get(i);
            ItemStack item = new ItemStack(entry.getKey());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(entry.getKey().toString());
                List<String> lore = new ArrayList<>();
                lore.add("Crafted: " + entry.getValue());
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.addItem(item);
        }

        if (page > 0) {
            ItemStack previousPage = new ItemStack(Material.ARROW);
            ItemMeta meta = previousPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Previous Page");
                previousPage.setItemMeta(meta);
            }
            inventory.setItem(45, previousPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Next Page");
                nextPage.setItemMeta(meta);
            }
            inventory.setItem(53, nextPage);
        }

        // Adding Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("Back");
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(4, backButton);

        viewer.openInventory(inventory);
    }

    private void openLeaderboardGUI(Player viewer, int page) {
        int itemsPerPage = 45;
        Inventory inventory = Bukkit.createInventory(null, 54, "Leaderboard - Page " + (page + 1));

        List<Material> materials = new ArrayList<>(EnumSet.allOf(Material.class));
        materials.sort((m1, m2) -> Integer.compare(getTotalCrafted(m2), getTotalCrafted(m1)));

        int totalPages = (int) Math.ceil((double) materials.size() / itemsPerPage);
        totalPages = Math.max(totalPages, 1);

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());

        for (int i = startIndex; i < endIndex; i++) {
            Material material = materials.get(i);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(material.toString());
                List<String> lore = new ArrayList<>();
                lore.add("Total Crafted: " + getTotalCrafted(material));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.addItem(item);
        }

        if (page > 0) {
            ItemStack previousPage = new ItemStack(Material.ARROW);
            ItemMeta meta = previousPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Previous Page");
                previousPage.setItemMeta(meta);
            }
            inventory.setItem(45, previousPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Next Page");
                nextPage.setItemMeta(meta);
            }
            inventory.setItem(53, nextPage);
        }

        // Adding Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("Back");
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(4, backButton);

        viewer.openInventory(inventory);
    }

    private void openTopPlayersGUI(Player viewer, Material material, int page) {
        int itemsPerPage = 45;
        Inventory inventory = Bukkit.createInventory(null, 54, "Top 10 - " + material.toString() + " - Page " + (page + 1));

        List<Map.Entry<UUID, Integer>> topPlayers = getTopPlayers(material);
        int totalPages = (int) Math.ceil((double) topPlayers.size() / itemsPerPage);
        totalPages = Math.max(totalPages, 1);

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, topPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Integer> entry = topPlayers.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(player.getName());
                List<String> lore = new ArrayList<>();
                lore.add("Crafted: " + entry.getValue());
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.addItem(item);
        }

        if (page > 0) {
            ItemStack previousPage = new ItemStack(Material.ARROW);
            ItemMeta meta = previousPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Previous Page");
                previousPage.setItemMeta(meta);
            }
            inventory.setItem(45, previousPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Next Page");
                nextPage.setItemMeta(meta);
            }
            inventory.setItem(53, nextPage);
        }

        // Adding Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("Back");
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(4, backButton);

        viewer.openInventory(inventory);
    }

    private int getTotalCrafted(Material material) {
        return craftingStats.values().stream()
                .mapToInt(stats -> stats.getOrDefault(material, 0))
                .sum();
    }

    //Save and load Functions //
    private void saveCraftingStats() {
        File file = new File(getDataFolder(), "craftingStats.yml");
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(file);
        for (UUID playerId : craftingStats.keySet()) {
            String playerKey = playerId.toString();
            for (Material material : craftingStats.get(playerId).keySet()) {
                int amount = craftingStats.get(playerId).get(material);
                statsConfig.set(playerKey + "." + material.name(), amount);
            }
        }
        try {
            statsConfig.save(file);
        } catch (IOException e) {
            getLogger().severe("Could not save crafting stats to file: " + e.getMessage());
        }
    }

    private void loadCraftingStats() {
        File file = new File(getDataFolder(), "craftingStats.yml");
        if (!file.exists()) return;
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(file);
        for (String playerKey : statsConfig.getKeys(false)) {
            UUID playerId = UUID.fromString(playerKey);
            Map<Material, Integer> playerStats = new HashMap<>();
            for (String materialKey : Objects.requireNonNull(statsConfig.getConfigurationSection(playerKey)).getKeys(false)) {
                Material material = Material.valueOf(materialKey);
                int amount = statsConfig.getInt(playerKey + "." + materialKey);
                playerStats.put(material, amount);
            }
            craftingStats.put(playerId, playerStats);
        }
    }

    private List<Map.Entry<UUID, Integer>> getTopPlayers(Material material) {
        return craftingStats.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getOrDefault(material, 0)))
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Crafting Stats") ||
                event.getView().getTitle().startsWith("Leaderboard") ||
                event.getView().getTitle().startsWith("Top 10") ||
                event.getView().getTitle().equals("Main Menu")) {
            event.setCancelled(true);

            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && currentItem.getItemMeta() != null) {
                String title = event.getView().getTitle();

                // Check if title contains page information or is the main menu
                String[] titleParts = title.split(" - ");
                if (titleParts.length > 1) {
                    try {
                        int currentPage = Integer.parseInt(titleParts[titleParts.length - 1].replaceAll("[^0-9]", "")) - 1;
                        Player player = (Player) event.getWhoClicked();

                        if (currentItem.getType() == Material.ARROW) {
                            if (event.getSlot() == 45) {
                                // Previous page
                                if (title.startsWith("Crafting Stats")) {
                                    openCraftingStatsGUI(player, player.getUniqueId(), currentPage - 1);
                                } else if (title.startsWith("Leaderboard")) {
                                    if (currentPage > 0) {
                                        openLeaderboardGUI(player, currentPage - 1);
                                    } else {
                                        openMainMenu(player);
                                    }
                                } else if (title.startsWith("Top 10")) {
                                    Material material = Material.matchMaterial(title.split(" - ")[1]);
                                    assert material != null;
                                    openTopPlayersGUI(player, material, currentPage - 1);
                                }
                            } else if (event.getSlot() == 53) {
                                // Next page
                                if (title.startsWith("Crafting Stats")) {
                                    openCraftingStatsGUI(player, player.getUniqueId(), currentPage + 1);
                                } else if (title.startsWith("Leaderboard")) {
                                    openLeaderboardGUI(player, currentPage + 1);
                                } else if (title.startsWith("Top 10")) {
                                    Material material = Material.matchMaterial(title.split(" - ")[1]);
                                    assert material != null;
                                    openTopPlayersGUI(player, material, currentPage + 1);
                                }
                            }
                        } else if (currentItem.getType() == Material.BARRIER && "Back".equals(currentItem.getItemMeta().getDisplayName())) {
                            // Handle back button click
                            if (title.startsWith("Top 10")) {
                                // Go back to the Leaderboard
                                openLeaderboardGUI(player, 0);
                            } else if (title.startsWith("Crafting Stats") || title.startsWith("Leaderboard")) {
                                if (title.startsWith("Leaderboard") && currentPage == 0) {
                                    // Go back to the Main Menu from the first page of the Leaderboard
                                    openMainMenu(player);
                                } else {
                                    // Go back to the Leaderboard or another appropriate screen
                                    openLeaderboardGUI(player, 0);
                                }
                            }
                        } else if (title.startsWith("Leaderboard")) {
                            Material material = currentItem.getType();
                            openTopPlayersGUI((Player) event.getWhoClicked(), material, 0);
                        }
                    } catch (NumberFormatException e) {
                        // Log error and ignore invalid page number
                        getLogger().warning("Failed to parse page number from title: " + title);
                    }
                } else if (title.equals("Main Menu")) {
                    if (currentItem.getType() == Material.DIAMOND && "Leaderboard".equals(currentItem.getItemMeta().getDisplayName())) {
                        openLeaderboardGUI((Player) event.getWhoClicked(), 0);
                    } else if (currentItem.getType() == Material.CRAFTING_TABLE && "Crafting Stats".equals(currentItem.getItemMeta().getDisplayName())) {
                        openCraftingStatsGUI((Player) event.getWhoClicked(), ((Player) event.getWhoClicked()).getUniqueId(), 0);
                    }else if(currentItem.getType() == Material.BARRIER && "Back".equals(currentItem.getItemMeta().getDisplayName())){
                        // close the inventory
                        ((Player) event.getWhoClicked()).closeInventory();
                    }
                }
            }
        }
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
