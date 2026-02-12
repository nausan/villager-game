package org.tnd1dks.villagerMinigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class EventManager implements Listener
{
    World world = Bukkit.getWorld("world");
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    @EventHandler
    public void OnEntityDamage(EntityDamageEvent event) {
        if (!VillagerMinigame.gameStarted && !VillagerMinigame.decisionStarted) return;
        if (!(event.getCause() == EntityDamageEvent.DamageCause.KILL
                || event.getCause() == EntityDamageEvent.DamageCause.CUSTOM
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK))
        { event.setCancelled(true); }
        if (event.getEntity() instanceof Player && event.getDamageSource().getCausingEntity() instanceof Player) event.setCancelled(true);
    }

    @EventHandler
    public void OnEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!VillagerMinigame.gameStarted && !VillagerMinigame.decisionStarted) return;

        if (!(event.getDamager() instanceof Player damager)) { event.setCancelled(true); return; }
        Team team = scoreboard.getTeam("Murderer");
        if (event.getEntity() instanceof Villager villager && team.hasPlayer(damager)) {
            villager.damage(100);
            if (!villager.getScoreboardTags().contains("fake")) {
                Objective murdererHp = scoreboard.getObjective("murdererHealth");
                murdererHp.getScore(damager).setScore(murdererHp.getScore(damager).getScore() - VillagerMinigame.penaltyDamage);
                damager.damage(Math.max(0, damager.getHealth() - murdererHp.getScore(damager).getScore()));
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void OnPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        if (!VillagerMinigame.gameStarted) return;
        event.deathMessage(Component.empty());
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        Team murdererTeam = scoreboard.getTeam("Murderer");
        if (murdererTeam.hasEntity(event.getPlayer())) {
            world.sendMessage(Component.text("우민 " + event.getPlayer().getName() + "이 처형당했습니다."));
            return;
        }
        world.sendMessage(Component.text("가짜 주민 " + event.getPlayer().getName() + "이 우민에게 살해당했습니다."));
    }

    @EventHandler
    public void OnPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Team murdererTeam = scoreboard.getTeam("Murderer");
        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            player.getInventory().removeItem(player.getInventory().getItemInMainHand());
            new BukkitRunnable() {
                int i = 0;
                @Override
                public void run() {
                    if (i == 40) { cancel(); return; }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (murdererTeam.hasPlayer(p) || p.getGameMode() == GameMode.SPECTATOR) continue;
                        world.spawnParticle(Particle.DUST, p.getLocation().add(new Location(world, 0f, 0.6f, 0f)), 2, new Particle.DustOptions(Color.YELLOW, 2f));
                    }
                    i++;
                }
            }.runTaskTimer(VillagerMinigame.plugin, 0L, 2L);
            for (Player p : Bukkit.getOnlinePlayers()) {
                world.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_4, 1f, 2f);
            }
            world.sendMessage(Component.text("우민이 토템을 사용했습니다!", NamedTextColor.GOLD));
        }
    }

    @EventHandler
    public void OnEntityResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void OnPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setAllowFlight(false);
        event.joinMessage(Component.text("입장: " + player.getName(), NamedTextColor.YELLOW));
        if (VillagerMinigame.decisionStarted || VillagerMinigame.gameStarted) return;
        player.setHealthScale(20.0f);
        if (!player.isOp()) player.setGameMode(GameMode.ADVENTURE);
    }

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.quitMessage(Component.text("퇴장: " + player.getName(), NamedTextColor.YELLOW));
    }
}
