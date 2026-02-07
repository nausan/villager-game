package org.tnd1dks.villagerMinigame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class VillagerMinigame extends JavaPlugin
{
    public static VillagerMinigame plugin;
    public static CommandManager commandManager;
    public static GameManager gameManager;
    public static EventManager eventManager;

    public static int gameLength;
    public static int murdererCount;
    public static int villagerCount;
    public static int penaltyDamage;
    public static WorldBorderSettings worldBorderSettings;
    public static boolean gameStarted;
    public static boolean decisionStarted;

    public enum WorldBorderSettings {
        SMALL,
        MEDIUM,
        LARGE
    }

    @Override
    public void onEnable() {
        plugin = this;
        commandManager = new CommandManager();
        gameManager = new GameManager();
        eventManager = new EventManager();

        gameLength = 120;
        murdererCount = 1;
        villagerCount = 10;
        penaltyDamage = 2;
        worldBorderSettings = null;
        gameStarted = false;
        decisionStarted = false;

        if (getCommand("minigame") != null) {
            getCommand("minigame").setExecutor(commandManager);
            getLogger().config("Minigame: command enabled");
        } else {
            getLogger().severe("Minigame: no command found!");
        }

        Bukkit.getPluginManager().registerEvents(eventManager, this);
    }
}