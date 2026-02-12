package org.tnd1dks.villagerMinigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter
{
    World world = Bukkit.getWorld("world");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args)
    {
        if (!sender.isOp()) return false;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("play")) {
                VillagerMinigame.gameManager.PlayGame(sender);
            } else if (args[0].equalsIgnoreCase("stop")) {
                VillagerMinigame.gameManager.StopGame(sender);
            } else if (args[0].equalsIgnoreCase("time")) {
                world.sendMessage(Component.text("Game length: " + VillagerMinigame.gameLength, NamedTextColor.GREEN));
            } else if (args[0].equalsIgnoreCase("size")) {
                world.sendMessage(Component.text("Border size: " + VillagerMinigame.worldBorderSettings, NamedTextColor.GREEN));
            } else if (args[0].equalsIgnoreCase("murderers")) {
                world.sendMessage(Component.text("Murderer count: " + VillagerMinigame.murdererCount, NamedTextColor.GREEN));
            } else if (args[0].equalsIgnoreCase("villagers")) {
                world.sendMessage(Component.text("Villager count: " + VillagerMinigame.villagerCount, NamedTextColor.GREEN));
            } else if (args[0].equalsIgnoreCase("test")) {
                VillagerMinigame.gameManager.Test();
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("time")) {
                VillagerMinigame.gameManager.SetGameLength(sender, args[1]);
            } else if (args[0].equalsIgnoreCase("size")) {
                VillagerMinigame.gameManager.SetWorldBorder(sender, args[1]);
            } else if (args[0].equalsIgnoreCase("murderers")) {
                VillagerMinigame.gameManager.SetMurdererCount(sender, args[1]);
            } else if (args[0].equalsIgnoreCase("villagers")) {
                VillagerMinigame.gameManager.SetVillagerCount(sender, args[1]);
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args)
    {
        if (!sender.isOp()) return List.of();
        if (command.getName().equalsIgnoreCase("minigame")) {
            if (args.length == 1) return List.of("play", "stop", "time", "size", "murderers", "villagers");
            else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("size")) return List.of("small", "medium", "large");
            }
        }
        return List.of();
    }
}
