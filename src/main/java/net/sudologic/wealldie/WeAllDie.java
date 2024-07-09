package net.sudologic.wealldie;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class WeAllDie extends JavaPlugin implements Listener, CommandExecutor {
    private static final String GAME_WORLD_NAME = "GameWorld";
    private static boolean gameRunning = false;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("startgame").setExecutor(this);
        World targetWorld = Bukkit.getWorld(GAME_WORLD_NAME);
        if (targetWorld == null) {
            createGameWorld();
            targetWorld = Bukkit.getWorld(GAME_WORLD_NAME);
            if (targetWorld == null) {
                getLogger().severe("The world '" + GAME_WORLD_NAME + "' could not be loaded!");
            }
        }
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Bukkit.broadcastMessage("Teleporting all players to the main world in 10 seconds.");

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            World mainWorld = Bukkit.getWorld("world");
            if (mainWorld == null) {
                Bukkit.getLogger().severe("The main world 'world' could not be found!");
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(mainWorld.getSpawnLocation());
                preparePlayer(player);
            }

            deleteGameWorlds();
            createGameWorld();
            gameRunning = false;
        }, 200L);
    }

    public boolean deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("World " + worldName + " does not exist or is already unloaded.");
            return false;
        }

        if (!Bukkit.unloadWorld(world, false)) {
            getLogger().severe("Could not unload world " + worldName);
            return false;
        }

        return deleteDirectory(new File(world.getWorldFolder().getPath()));
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return path.delete();
    }

    public void deleteGameWorlds() {
        String[] worldNames = {GAME_WORLD_NAME, "world_nether", "world_the_end"};
        for (String worldName : worldNames) {
            if (deleteWorld(worldName)) {
                getLogger().info("Successfully deleted " + worldName);
            } else {
                getLogger().warning("Failed to delete " + worldName);
            }
        }
    }

    private void createGameWorld() {
        WorldCreator creator = new WorldCreator(GAME_WORLD_NAME);
        Bukkit.createWorld(creator);
        getLogger().info(GAME_WORLD_NAME + " has been created.");
        creator = new WorldCreator("world_nether");
        creator.environment(World.Environment.NETHER);
        Bukkit.createWorld(creator);
        getLogger().info("world_nether has been created.");
        creator = new WorldCreator("world_the_end");
        creator.environment(World.Environment.THE_END);
        Bukkit.createWorld(creator);
        getLogger().info("world_the_end has been created.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender.isOp() || sender.hasPermission("wealldie.startgame"))) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        if(gameRunning) {
            sender.sendMessage("The game is already running.");
            return true;
        }
        World targetWorld = Bukkit.getWorld(GAME_WORLD_NAME);
        if(targetWorld == null) {
            sender.sendMessage("The game world does not exist yet. Please wait.");
            return true;
        }
        Bukkit.broadcastMessage("Game starting in 10 seconds.");
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(targetWorld.getSpawnLocation());
                preparePlayer(player);
                player.sendMessage("You have been teleported to the game world!");
            }
            sender.sendMessage("All players have been teleported to the game world.");
        }, 200L); // 200 ticks = 10 seconds
        gameRunning = true;
        return true;
    }

    public void preparePlayer(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.setExp(0);
    }
}