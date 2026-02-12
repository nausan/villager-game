package org.tnd1dks.villagerMinigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GameManager
{
    World world = Bukkit.getWorld("world");

    public void Test() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack emerald = new ItemStack(Material.TOTEM_OF_UNDYING);
            ItemMeta meta = emerald.getItemMeta();
            meta.displayName(Component.text("설사의 토템", NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("우클릭으로 사용", NamedTextColor.WHITE)));
            emerald.setItemMeta(meta);
            p.getInventory().addItem(emerald);
        }
    }

    public void PlayGame(CommandSender sender)
    {
        if (VillagerMinigame.gameStarted) { sender.sendMessage(Component.text("이미 시작함.", NamedTextColor.RED)); return; }
        if (Bukkit.getOnlinePlayers().size() < 2) {
            sender.sendMessage(Component.text("온라인 플레이어가 최소 2명 있어야 합니다.", NamedTextColor.RED));
            return;
        } else if (VillagerMinigame.murdererCount >= Bukkit.getOnlinePlayers().size()) {
            sender.sendMessage(Component.text("온라인 플레이어 수는 murderer 수보다 커야 합니다.", NamedTextColor.RED));
            return;
        } else if (VillagerMinigame.worldBorderSettings == null) {
            sender.sendMessage(Component.text("월드보더 크기가 정해지지 않았습니다.", NamedTextColor.RED));
            return;
        }

        Location center = new Location(world, -299.5f, -61.0f,350.5f);
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Integer> murdererIndices = new ArrayList<>();
        HashMap<Player, Villager> PV = new HashMap<>();
        Random random = new Random();

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard.getTeam("Civilian") != null) scoreboard.getTeam("Civilian").unregister();
        Team civilianTeam = scoreboard.registerNewTeam("Civilian");
        civilianTeam.color(NamedTextColor.YELLOW);
        if (scoreboard.getTeam("Murderer") != null) scoreboard.getTeam("Murderer").unregister();
        Team murdererTeam = scoreboard.registerNewTeam("Murderer");
        murdererTeam.color(NamedTextColor.RED);

        VillagerMinigame.decisionStarted = true;
        world.getWorldBorder().setCenter(new Location(world, 0f, 0f,0f));
        world.getWorldBorder().setSize(10000);
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, true);
        world.setTime(1000);
        for (int i = 0; i < players.size(); i++) {
            double angle = 360f / (double)players.size() * i;
            double x = center.getX() + Math.cos(angle * Math.PI / 180f) * 4f;
            double z = center.getZ() + Math.sin(angle * Math.PI / 180f) * 4f;
            Player player = players.get(i);
            player.teleport(new Location(world, x, center.getY(), z, (float)angle + 90f, 0));
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setAllowFlight(false);
            player.setWalkSpeed(0f);
            player.clearActivePotionEffects();
            civilianTeam.addEntity(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 4, false, false));
            player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0f);
        }

        Objective murdererHp = scoreboard.getObjective("murdererHealth");
        if (murdererHp == null) {
            murdererHp = scoreboard.registerNewObjective("murdererHealth", "dummy");
        }
        for (int i = 0; i < VillagerMinigame.murdererCount; i++) {
            int randomInteger = random.nextInt(players.size());
            while (murdererIndices.contains(randomInteger)) {
                randomInteger = random.nextInt(players.size());
            }
            murdererIndices.add(randomInteger);

            Score hpScore = murdererHp.getScore(players.get(randomInteger));
            hpScore.setScore((int) players.get(randomInteger).getAttribute(Attribute.MAX_HEALTH).getValue());
        }

        new BukkitRunnable() {
            int step = 0;
            int tick = 0;
            int glowingIndex = 0;
            final int cycleTick = 2;
            final int decisionTick = 40;
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() < players.size() || (!VillagerMinigame.decisionStarted && !VillagerMinigame.gameStarted)) { world.sendMessage(Component.text("게임이 취소되었습니다.")); cancel(); return; }
                tick ++;

                for (Player player : PV.keySet()) { // 플레이어랑 연결된 주민 tp
                    Villager villager = PV.get(player);
                    if (villager.isDead()) {
                        PV.remove(player);
                        villager.remove();
                        player.setHealth(0);
                    }
                    else {
                        villager.teleport(player.getLocation());
                    }
                }

                switch (step) {
                    case 0 -> { // 1초 기다리고 타이틀 띄우기
                        if (tick < 20) return;
                        world.showTitle(Title.title(Component.text("우민 선택", NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.empty(), 0, 40, 20));
                        world.playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.5f);
                        step ++;
                        tick = 0;
                    }
                    case 1, 5 -> { // 2초 기다리기
                        if (tick < 40) return;
                        step ++;
                        tick = 0;
                    }
                    case 2 -> { // cycleTick 마다 다음 순서 플레이어한테 발광 효과 적용, decisionTick 마다 다음 murdererIndex 플레이어를 빨간 팀에 넣고 발광 효과 적용, 선택이 끝난 후 타이틀
                        if (tick % cycleTick == 0) {
                            if (tick % decisionTick == 0) {
                                int murdererIndex = murdererIndices.get(tick / decisionTick - 1);
                                murdererTeam.addEntity(players.get(murdererIndex));
                                players.get(murdererIndex).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, murdererIndices.size() * decisionTick - tick + 40, 0, false, false));
                                world.playSound(players.get(murdererIndex).getLocation(), Sound.ENTITY_PILLAGER_AMBIENT, 2f, 1f);
                                world.sendMessage(Component.text(players.get(murdererIndex).getName() + " 선택됨", NamedTextColor.GOLD));
                            }
                            else {
                                do {
                                    glowingIndex++;
                                    if (glowingIndex >= players.size()) glowingIndex %= players.size();
                                } while (murdererTeam.hasEntity(players.get(glowingIndex)));
                                players.get((tick / cycleTick) % players.size()).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, cycleTick, 0, false, false));
                                world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
                            }
                        }

                        if (tick < decisionTick * murdererIndices.size()) return;
                        String murdererList = "";
                        for (Player player : players) {
                            if (murdererIndices.contains(players.indexOf(player))) {
                                murdererList += " " + player.getName() + " ";
                            }
                        }
                        world.showTitle(Title.title(Component.text("선택 완료", NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.text(murdererList, NamedTextColor.GOLD), 0, 40, 20));
                        world.playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.5f);

                        step ++;
                        tick = 0;
                    }
                    case 3 -> { // 2초 기다리고 필드에 랜덤으로 주민 소환
                        if (tick < 40) return;
                        for (int i = 0; i < VillagerMinigame.villagerCount; i++) {
                            SetWorldBorder(null, null);
                            world.getWorldBorder().setSize(10000);
                            Location location = world.getWorldBorder().getCenter();
                            switch (VillagerMinigame.worldBorderSettings) {
                                case VillagerMinigame.WorldBorderSettings.SMALL -> location.add(random.nextInt(30) - 15, 0, random.nextInt(30) - 15);
                                case VillagerMinigame.WorldBorderSettings.MEDIUM -> location.add(random.nextInt(40) - 20, 0, random.nextInt(40) - 20);
                                case VillagerMinigame.WorldBorderSettings.LARGE -> location.add(random.nextInt(60) - 30, 0, random.nextInt(60) - 30);
                            }
                            if (location.toHighestLocation().getY() > -57.0f) {
                                location = world.getWorldBorder().getCenter();
                                location.add(random.nextInt(4) - 2, 0, random.nextInt(4) - 2);
                            }
                            Villager villager = world.spawn(location.toHighestLocation().add(new Location(world, 0.0f, 1.0f, 0.0f)), Villager.class);
                            villager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(1.0f);
                        }
                        step ++;
                    }
                    case 4 -> { // 주민 역할은 피 1칸으로 설정하고 필드로 tp, 우민 역할은 피 5칸으로 설정, 각각 타이틀, 월드보더 설정
                        VillagerMinigame.decisionStarted = false;
                        VillagerMinigame.gameStarted = true;
                        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
                        for (Player player : players) {
                            player.setWalkSpeed(0.2f);
                            player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0.42f);
                            if (murdererIndices.contains(players.indexOf(player))) {
                                scoreboard.getObjective("murdererHealth").getScore(player).setScore(10);
                                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(10.0f);
                                player.setHealthScale(10.0f);
                                player.teleport(world.getWorldBorder().getCenter().toHighestLocation().add(new Location(world, 0.0f, 200f, 0.0f)));
                                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 220, 0, false, false));
                                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 220, 9, false, false));
                                player.showTitle(Title.title(Component.text(""), Component.text("당신은 우민입니다."), 10, 60, 10));
                                continue;
                            }
                            Villager newVillager = world.spawn(player.getLocation(), Villager.class);
                            newVillager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(1.0f);
                            newVillager.setAI(false);
                            newVillager.addScoreboardTag("fake");
                            PV.put(player, newVillager);
                            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.07f);
                            player.hideEntity(VillagerMinigame.plugin, newVillager);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, VillagerMinigame.gameLength * 20 + 220, 0, false, false));
                            if (scoreboard.getTeam(player.getName() + "_villager") != null) scoreboard.getTeam(player.getName() + "_villager").unregister();
                            Team team = scoreboard.registerNewTeam(player.getName() + "_villager");
                            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OWN_TEAM);
                            team.addEntity(player);
                            team.addEntity(newVillager);

                            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2.0f);
                            player.getAttribute(Attribute.SCALE).setBaseValue(0.99f);
                            player.setHealthScale(2.0f);
                            player.teleport(world.getWorldBorder().getCenter().toHighestLocation().add(new Location(world, 0.0f, 1.0f, 0.0f)));
                            player.showTitle(Title.title(Component.text(""), Component.text("당신은 가짜 주민입니다."), 10, 60, 10));
                        }
                        SetWorldBorder(null, null);
                        step ++;
                        tick = 0;
                    }
                    case 6 -> { // 10초 카운트다운 후 우민 역할 플레이어를 필드로 tp 시키고 도끼 지급, 각각 타이틀
                        if (tick % 20 == 0) {
                            NamedTextColor color = switch (10 - tick / 20) {
                                case 1 -> NamedTextColor.RED;
                                case 2 -> NamedTextColor.GOLD;
                                case 3 -> NamedTextColor.YELLOW;
                                default -> NamedTextColor.GREEN;
                            };
                            world.showTitle(Title.title(Component.text(10 - tick / 20, color), Component.empty()));
                        }

                        if (tick < 200) return;
                        for (Player player : players) {
                            player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_2, 1f, 1f);
                            if (murdererIndices.contains(players.indexOf(player))) {
                                player.teleport(world.getWorldBorder().getCenter().toHighestLocation().add(new Location(world, 0.0f, 1.0f, 0.0f)));
                                player.getInventory().addItem(new ItemStack(Material.IRON_AXE));
                                ItemStack emerald = new ItemStack(Material.TOTEM_OF_UNDYING);
                                ItemMeta meta = emerald.getItemMeta();
                                meta.displayName(Component.text("설사의 토템", NamedTextColor.GOLD));
                                meta.lore(List.of(Component.text("우클릭으로 사용", NamedTextColor.WHITE)));
                                emerald.setItemMeta(meta);
                                player.getInventory().addItem(emerald);
                                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, VillagerMinigame.gameLength * 20, 0, false, false));
                                player.showTitle(Title.title(Component.text("처치하세요", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD), Component.empty(), 10, 30, 10));
                                continue;
                            }
                            player.showTitle(Title.title(Component.text("생존하세요", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD), Component.empty(), 10, 30, 10));
                        }
                        step ++;
                        tick = 0;
                    }
                    case 7 -> { // 주민 다 죽거나 시간이 초과되거나 우민이 다 죽으면 끝
                        if (PV.isEmpty() || tick / 20 >= VillagerMinigame.gameLength) { step ++; return; }
                        world.sendActionBar(Component.text(VillagerMinigame.gameLength - tick / 20));

                        int murdererDeathToll = 0;
                        for (Integer index : murdererIndices) {
                            if (players.get(index).getGameMode() == GameMode.SPECTATOR) {
                                murdererDeathToll ++;
                            }
                        }
                        if (murdererDeathToll == murdererIndices.size()) {
                            step ++;
                        }

                        if (VillagerMinigame.gameLength * 20 - tick == 600) {
                            for (Entity entity : world.getEntities()) {
                                if (!(entity instanceof Villager)) continue;
                                ((Villager)entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0, false, false));
                            }
                            world.sendMessage(Component.text("모든 주민이 발광합니다 ( 30초 남음 )", NamedTextColor.GOLD));
                        }
                    }
                    default -> {    // 게임 종료
                        StopGame(null);
                        if (PV.isEmpty()) {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (murdererIndices.contains(players.indexOf(player))) {
                                    player.showTitle(Title.title(Component.text("승리", NamedTextColor.GREEN).decorate(TextDecoration.BOLD), Component.text("모든 주민을 처치했습니다.", NamedTextColor.GREEN), 0, 60, 10));
                                    player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_0, 1.5f, 1.4f);
                                } else if (players.contains(player)) {
                                    player.showTitle(Title.title(Component.text("패배", NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.text("모든 주민이 처치 당했습니다.", NamedTextColor.RED), 0, 60, 10));
                                    player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_7, 1.5f, 1.2f);
                                } else {
                                    player.showTitle(Title.title(Component.text(""), Component.text("우민 승리"), 0, 60, 10));
                                }
                            }
                        }
                        else {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (murdererIndices.contains(players.indexOf(player))) {
                                    player.showTitle(Title.title(Component.text("패배", NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.text("주민을 모두 처치하지 못했습니다.", NamedTextColor.RED), 0, 60, 10));
                                    player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_7, 1.5f, 1.2f);
                                } else if (players.contains(player)) {
                                    player.showTitle(Title.title(Component.text("승리", NamedTextColor.GREEN).decorate(TextDecoration.BOLD), Component.text("주민이 생존했습니다.", NamedTextColor.GREEN), 0, 60, 10));
                                    player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_0, 1.5f, 1.4f);
                                } else {
                                    player.showTitle(Title.title(Component.text(""), Component.text("주민 승리"), 0, 60, 10));
                                }
                            }
                        }
                        cancel();
                    }
                }
            }
        }.runTaskTimer(VillagerMinigame.plugin, 0, 1);
    }

    public void StopGame(CommandSender sender)
    {
        if (!VillagerMinigame.gameStarted) { sender.sendMessage(Component.text("시작 안 함.", NamedTextColor.RED)); return; }
        VillagerMinigame.decisionStarted = false;
        VillagerMinigame.gameStarted = false;
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.clearActivePotionEffects();
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0f);
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1f);
            player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0.42f);
            player.setHealthScale(20.0f);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void SetGameLength(CommandSender sender, String input)
    {
        int value;
        try { value = Integer.parseInt(input); } catch (Exception e) {
            sender.sendMessage(Component.text("유효하지 않은 입력입니다.", NamedTextColor.RED));
            return;
        }
        if (!(value >= 30 && value <= 1200)) { sender.sendMessage(Component.text("제한된 범위 밖의 값입니다. ( 30 - 1200 (단위 : 초))", NamedTextColor.RED)); return; }
        VillagerMinigame.gameLength = value;
        sender.sendMessage(Component.text("게임 시간이 설정되었습니다 : " + value));
    }

    public void SetWorldBorder(CommandSender sender, String input)
    {
        int size;
        if (sender == null && input == null) {
            Location location = switch (VillagerMinigame.worldBorderSettings) {
                case SMALL -> {
                    size = 40;
                    yield new Location(world, -509.0f, 0f, 336.0f);
                }
                case MEDIUM -> {
                    size = 50;
                    yield new Location(world, -503.0f, 0f, 335.0f);
                }
                case LARGE -> {
                    size = 70;
                    yield new Location(world, -510.0f, 0f, 352.0f);
                }
            };
            world.getWorldBorder().setSize(size);
            world.getWorldBorder().setCenter(location);
            return;
        }

        VillagerMinigame.WorldBorderSettings setting = switch (input) {
            case "small" -> VillagerMinigame.WorldBorderSettings.SMALL;
            case "medium" -> VillagerMinigame.WorldBorderSettings.MEDIUM;
            case "large" -> VillagerMinigame.WorldBorderSettings.LARGE;
            default -> null;
        };

        if (setting == null) {
            sender.sendMessage(Component.text("유효하지 않은 크기입니다.", NamedTextColor.RED));
            return;
        }

        Location location = switch (setting) {
            case SMALL -> {
                size = 40;
                yield new Location(world, -509.0f, 0f, 336.0f);
            }
            case MEDIUM -> {
                size = 50;
                yield new Location(world, -503.0f, 0f, 335.0f);
            }
            case LARGE -> {
                size = 70;
                yield new Location(world, -510.0f, 0f, 352.0f);
            }
        };

        world.getWorldBorder().setSize(size);
        world.getWorldBorder().setCenter(location);
        VillagerMinigame.worldBorderSettings = setting;
        sender.sendMessage(Component.text("월드보더 크기가 설정되었습니다 : " + VillagerMinigame.worldBorderSettings));
    }

    public void SetMurdererCount(CommandSender sender, String input)
    {
        int value;
        try { value = Integer.parseInt(input); } catch (Exception e) {
            sender.sendMessage(Component.text("유효하지 않은 입력입니다.", NamedTextColor.RED));
            return;
        }
        if (value < 1) { sender.sendMessage(Component.text("적어도 1명의 murderer 가 있어야 합니다.", NamedTextColor.RED)); return; }
        VillagerMinigame.murdererCount = value;
        sender.sendMessage(Component.text("우민 수가 설정되었습니다 : " + VillagerMinigame.murdererCount));
    }

    public void SetVillagerCount(CommandSender sender, String input)
    {
        int value;
        try { value = Integer.parseInt(input); } catch (Exception e) {
            sender.sendMessage(Component.text("유효하지 않은 입력입니다.", NamedTextColor.RED));
            return;
        }
        if (!(value >= 0 && value <= 100)) { sender.sendMessage(Component.text("제한된 범위 밖의 값입니다. ( 0 - 100 )", NamedTextColor.RED)); return; }
        VillagerMinigame.villagerCount = value;
        sender.sendMessage(Component.text("주민 수가 설정되었습니다 : " + VillagerMinigame.villagerCount));
    }
}