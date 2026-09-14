package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillService implements Listener {
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final DamageService damage;
    private final ItemService items;
    private final JavaPlugin plugin;
    private BuffService buffs;
    private LevelService levels;
    private final Map<UUID, Integer> inventoryDrops = new HashMap<>();
    private final Map<UUID, Map<String, AbilityState>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Boolean, Integer>> inputTicks = new HashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();

    public SkillService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                        CombatStateService combat, DamageService damage, ItemService items) {
        this.definitions = definitions; this.players = players; this.stats = stats; this.combat = combat; this.damage = damage; this.items = items;
        this.plugin = plugin;
    }

    public void bindEffects(BuffService buffs, LevelService levels) { this.buffs = buffs; this.levels = levels; }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSneakAttack(io.papermc.paper.event.player.PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || !(event.getAttacked() instanceof LivingEntity target)) return;
        if (input(player, target, true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSneakUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking() || (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)) return;
        LivingEntity target = rayTarget(event.getPlayer());
        if (input(event.getPlayer(), target, true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrop(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getAction().name().startsWith("DROP_")) {
            markInventoryDrop(event.getWhoClicked().getUniqueId());
            if (definitions.snapshot().config("config.yml").getBoolean("controls.debug-inputs", false))
                plugin.getLogger().info("[skill-input] " + event.getWhoClicked().getName() + " inventory=" + event.getAction() + " slot=" + event.getSlot());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void traceDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!definitions.snapshot().config("config.yml").getBoolean("controls.debug-inputs", false)) return;
        Player player = event.getPlayer();
        plugin.getLogger().info("[skill-input] " + player.getName() + " drop cancelled=" + event.isCancelled()
                + " inventoryDrop=" + inventoryDrops.containsKey(player.getUniqueId()) + " view=" + player.getOpenInventory().getType()
                + " permission=" + player.hasPermission("combatcoresystems.command.skill")
                + " weapon=" + items.id(event.getItemDrop().getItemStack()).orElse("none"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        // Only a real cursor item can become a close-time inventory drop.
        // Geyser may close a view immediately before a normal hand-drop packet.
        var cursor = event.getPlayer().getItemOnCursor();
        if (!cursor.getType().isAir()) markInventoryDrop(event.getPlayer().getUniqueId());
    }

    private void markInventoryDrop(UUID id) {
        int tick = org.bukkit.Bukkit.getCurrentTick();
        inventoryDrops.put(id, tick);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> inventoryDrops.remove(id, tick));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHandDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!definitions.snapshot().config("config.yml").getBoolean("controls.drop-skill", true)
                || inventoryDrops.containsKey(player.getUniqueId())
                || player.getOpenInventory().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING
                || !player.hasPermission("combatcoresystems.command.skill")) return;
        ItemInstance dropped = items.instance(event.getItemDrop().getItemStack()).orElse(null);
        WeaponDefinition weapon = dropped == null ? null : definitions.snapshot().weapons().get(dropped.getDefinitionId());
        if (weapon == null || weapon.skill() == null) return;
        int slot = player.getInventory().getHeldItemSlot();
        event.setCancelled(true);
        // Cancellation restores the removed item after dispatch; never cast using an empty hand.
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (player.getInventory().getHeldItemSlot() != slot) {
                fail(player, "hand-changed", "ドロップ操作中に持ち替えたため、スキルを中止しました。");
                return;
            }
            ItemInstance restored = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
            if (restored != null && restored.getInstanceId().equals(dropped.getInstanceId())) activate(player, false);
            else fail(player, "hand-restore", "武器の手持ち復元を確認できませんでした。持ち直してから再試行してください。");
        });
    }

    private boolean input(Player player, LivingEntity target, boolean ultimate) {
        if (!definitions.snapshot().config("config.yml").getBoolean("controls.sneak-attack-ultimate", true)
                || !player.hasPermission("combatcoresystems.command." + (ultimate ? "ultimate" : "skill"))
                || items.id(player.getInventory().getItemInMainHand()).isEmpty()) return false;
        Map<Boolean, Integer> ticks = inputTicks.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        int tick = org.bukkit.Bukkit.getCurrentTick();
        WeaponDefinition weapon = definitions.snapshot().weapons().get(items.id(player.getInventory().getItemInMainHand()).orElse(""));
        if (weapon == null || weapon.ultimate() == null) return false;
        if (Objects.equals(ticks.put(ultimate, tick), tick)) return true;
        activate(player, target, ultimate);
        return true; // An attempted ultimate never leaks a simultaneous normal hit, even on cooldown.
    }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) { inputTicks.remove(event.getPlayer().getUniqueId()); inventoryDrops.remove(event.getPlayer().getUniqueId()); }

    public boolean activate(Player player, LivingEntity target, boolean ultimate) {
        PlayerData data = players.find(player.getUniqueId()).orElse(null);
        if (data == null) return fail(player, "loading", "プレイヤーデータを読み込み中です。");
        String weaponId = items.id(player.getInventory().getItemInMainHand()).orElse("");
        if (weaponId.isBlank()) return false;
        WeaponDefinition weapon = definitions.snapshot().weapons().get(weaponId);
        if (weapon == null) return fail(player, "unknown-weapon", "この武器の設定が読み込まれていません。");
        if (!weapon.canEquip(data.getLevel())) return fail(player, "equip-level", "武器の装備可能レベルを満たしていません。");
        WeaponDefinition.SkillDefinition ability = ultimate ? weapon.ultimate() : weapon.skill();
        if (ability == null) return fail(player, "undefined", "この武器には使用する技が設定されていません。");
        if (!conditionsMet(player, target, ability.conditions())) return fail(player, "conditions", "技の発動条件を満たしていません。体力・対象・距離・戦闘状態を確認してください。");
        if (java.util.stream.Stream.concat(ability.options().selfEffects().stream(), ability.options().targetEffects().stream())
                .anyMatch(id -> !definitions.snapshot().buffs().containsKey(id))) return fail(player, "effect", "参照先のバフ・デバフ設定が見つかりません。");
        if (target != null && !ability.target().equalsIgnoreCase("SELF") && !damage.canAffect(player, target))
            return fail(player, "target", "この対象には技を使用できません。");

        ItemInstance instance = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
        int limitBreak = instance == null ? 0 : instance.getLimitBreak();
        double baseCooldown = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".cooldown", ability.cooldownSeconds());
        int maxCharges = Math.max(1, (int) override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".charges", ability.charges()));
        String cooldownId = weaponId + ":" + (ultimate ? "ultimate" : "skill");
        AbilityState state = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(cooldownId, ignored -> new AbilityState(maxCharges));
        double adjustedCooldown = CoreMath.cooldownSeconds(baseCooldown, stats.get(player, data).value(StatKey.COOLDOWN));
        state.refresh(maxCharges, adjustedCooldown);
        double multiplier = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".multiplier", ability.multiplier());
        double radius = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".radius", ability.radius());
        Element element = ability.element();
        String attributePath = (ultimate ? "ultimate" : "skill") + ".attribute";
        Object overrideElement = cumulativeOverride(weapon, limitBreak, attributePath);
        if (overrideElement == null) overrideElement = cumulativeOverride(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".element");
        if (overrideElement != null) element = Element.parse(String.valueOf(overrideElement)).orElse(element);
        ItemInstance heart = data.getEquipment().get(EquipmentSlot.DIVINE_HEART);
        if (heart != null) {
            String path = "divine-hearts." + heart.getDefinitionId() + ".rules." + (ultimate ? "ultimate" : "skill");
            var divine = definitions.snapshot().config("divine_hearts.yml");
            if (divine.contains(path + ".attribute")) element = Element.parse(divine.getString(path + ".attribute")).orElse(element);
            else if (divine.contains(path + ".element")) element = Element.parse(divine.getString(path + ".element")).orElse(element);
            multiplier *= divine.getDouble(path + ".multiplier", 1.0);
            radius += divine.getDouble(path + ".radius-add", 0.0);
        }

        List<LivingEntity> targets = new ArrayList<>();
        if (target != null) targets.add(target);
        if (radius > 0) {
            player.getWorld().getNearbyLivingEntities(target == null ? player.getLocation() : target.getLocation(), radius,
                    entity -> !entity.equals(player)).forEach(entity -> { if (!targets.contains(entity)) targets.add(entity); });
        }
        if (ability.target().equalsIgnoreCase("SELF")) { targets.clear(); targets.add(player); }
        else targets.removeIf(current -> !damage.canAffect(player, current));
        if (targets.isEmpty()) return fail(player, "target", "対象が見つかりません。対象に照準を合わせてください。");
        if (!state.consume(maxCharges, adjustedCooldown)) {
            player.sendActionBar(mini.deserialize(definitions.snapshot().config("messages.yml").getString("skill-failure.cooldown", "<red>あと <seconds>秒</red>")
                    .replace("<seconds>", String.format(Locale.ROOT, "%.1f", state.remainingSeconds()))));
            return true;
        }
        combat.touch(player, weaponId);
        String announcement = definitions.snapshot().config("messages.yml").getString(
                "ability-announcement." + (ultimate ? "ultimate" : "skill"), "<aqua>発動：<name></aqua>");
        if (!announcement.isBlank()) player.sendMessage(mini.deserialize(announcement.replace("<name>", ability.name())));
        if (ability.options().currentHpCost() > 0 && levels != null)
            levels.setVirtualHealth(player, data, data.getHealth() * (1 - ability.options().currentHpCost()));
        if (buffs != null) for (String id : ability.options().selfEffects()) buffs.apply(player, id, player.getUniqueId());
        stats.invalidate(player.getUniqueId());
        Element activeElement = element;
        for (LivingEntity current : targets) {
            if (!current.equals(player) && !damage.canAffect(player, current)) continue;
            boolean applied = !ability.options().damageEnabled() || damage.apply(new DamageRequest(player.getUniqueId(), current.getUniqueId(), ability.referenceStat(), multiplier,
                    activeElement, true, false, 0, ultimate ? "ultimate:" + ability.id() : "skill:" + ability.id(), ability.options().components())).applied();
            if (applied) {
                if (buffs != null) for (String id : ability.options().targetEffects()) buffs.apply(current, id, player.getUniqueId());
                WeaponVisuals.play(current, ability.options().visual());
            }
        }
        return true;
    }

    private boolean conditionsMet(Player player, LivingEntity target, Map<String, Object> conditions) {
        PlayerData data = players.require(player);
        PlayerStats currentStats = stats.get(player, data);
        double hpRatio = data.getHealth() / Math.max(1, currentStats.maxHp());
        if (conditions.containsKey("min-hp-percent") && hpRatio < number(conditions.get("min-hp-percent"))) return false;
        if (Boolean.TRUE.equals(conditions.get("requires-target")) && target == null) return false;
        if (conditions.containsKey("max-distance") && (target == null || target.getLocation().distanceSquared(player.getLocation()) > Math.pow(number(conditions.get("max-distance")), 2))) return false;
        return !Boolean.TRUE.equals(conditions.get("requires-combat")) || combat.inCombat(player.getUniqueId());
    }

    public boolean activate(Player player, boolean ultimate) {
        if (items.id(player.getInventory().getItemInMainHand()).isEmpty()) return fail(player, "weapon", "スキルを設定したCCS武器を手に持ってください。");
        return activate(player, rayTarget(player), ultimate);
    }

    private boolean fail(Player player, String key, String fallback) {
        var message = mini.deserialize(definitions.snapshot().config("messages.yml").getString("skill-failure." + key, "<red>" + fallback + "</red>"));
        player.sendActionBar(message);
        if (definitions.snapshot().config("config.yml").getBoolean("controls.failure-chat", true)) player.sendMessage(message);
        return false;
    }

    private LivingEntity rayTarget(Player player) {
        RayTraceResult ray = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 16,
                0.5, entity -> entity instanceof LivingEntity && !entity.equals(player) && player.hasLineOfSight(entity));
        Entity hit = ray == null ? null : ray.getHitEntity();
        return hit instanceof LivingEntity living ? living : null;
    }

    private double override(WeaponDefinition weapon, int limitBreak, String key, double fallback) {
        Object value = cumulativeOverride(weapon, limitBreak, key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private Object cumulativeOverride(WeaponDefinition weapon, int limitBreak, String key) {
        Object result = null;
        for (int level = 0; level <= limitBreak; level++) {
            Map<String, Object> values = weapon.limitBreaks().get(level);
            if (values != null && values.containsKey(key)) result = values.get(key);
            if (values != null && key.endsWith(".cooldown") && values.containsKey(key + "-seconds")) result = values.get(key + "-seconds");
        }
        return result;
    }

    private double number(Object value) { return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value)); }

    public Status status(UUID player, boolean ultimate) {
        Player online = org.bukkit.Bukkit.getPlayer(player);
        ItemInstance held = online == null ? null : items.instance(online.getInventory().getItemInMainHand()).orElse(null);
        WeaponDefinition weapon = held == null ? null : definitions.snapshot().weapons().get(held.getDefinitionId());
        var ability = weapon == null ? null : ultimate ? weapon.ultimate() : weapon.skill();
        if (ability == null) return new Status(false, 0, 0, 0);
        String kind = ultimate ? "ultimate" : "skill";
        int maximum = Math.max(1, (int) override(weapon, held.getLimitBreak(), kind + ".charges", ability.charges()));
        AbilityState state = cooldowns.getOrDefault(player, Map.of()).get(weapon.id() + ":" + kind);
        if (state == null) return new Status(true, 0, maximum, maximum);
        double cooldown = CoreMath.cooldownSeconds(override(weapon, held.getLimitBreak(), kind + ".cooldown", ability.cooldownSeconds()), stats.get(online, players.require(online)).value(StatKey.COOLDOWN));
        state.refresh(maximum, cooldown);
        return new Status(state.charges > 0, state.remainingSeconds(), state.charges, maximum);
    }

    public record Status(boolean ready, double remainingSeconds, int charges, int maximumCharges) {}

    private static final class AbilityState {
        private int charges;
        private int maximumCharges;
        private double cooldownSeconds;
        private long nextChargeAt;
        AbilityState(int maximum) { this.charges = maximum; this.maximumCharges = maximum; }
        void refresh(int maximum, double cooldown) {
            maximumCharges = maximum; cooldownSeconds = cooldown;
            long now = System.currentTimeMillis();
            while (charges < maximumCharges && nextChargeAt > 0 && now >= nextChargeAt) {
                charges++;
                nextChargeAt = charges < maximumCharges ? nextChargeAt + (long) (cooldown * 1000) : 0;
            }
            charges = Math.min(charges, maximumCharges);
        }
        boolean consume(int maximum, double cooldown) {
            refresh(maximum, cooldown);
            if (charges <= 0) return false;
            charges--;
            if (charges < maximum && nextChargeAt == 0) nextChargeAt = System.currentTimeMillis() + (long) (cooldown * 1000);
            if (cooldown <= 0) refresh(maximum, cooldown);
            return true;
        }
        double remainingSeconds() { return charges > 0 || nextChargeAt == 0 ? 0 : Math.max(0, (nextChargeAt - System.currentTimeMillis()) / 1000.0); }
    }
}
