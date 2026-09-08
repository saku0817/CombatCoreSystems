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
    private final Map<UUID, Map<String, AbilityState>> cooldowns = new ConcurrentHashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();

    public SkillService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                        CombatStateService combat, DamageService damage, ItemService items) {
        this.definitions = definitions; this.players = players; this.stats = stats; this.combat = combat; this.damage = damage; this.items = items;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSneakAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !player.isSneaking() || !(event.getEntity() instanceof LivingEntity target)) return;
        if (activate(player, target, false)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSneakUse(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking() || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        LivingEntity target = rayTarget(event.getPlayer());
        if (activate(event.getPlayer(), target, true)) event.setCancelled(true);
    }

    public boolean activate(Player player, LivingEntity target, boolean ultimate) {
        PlayerData data = players.find(player.getUniqueId()).orElse(null);
        if (data == null) return false;
        String weaponId = items.id(player.getInventory().getItemInMainHand()).orElse(combat.state(player.getUniqueId()).activeWeapon());
        if (weaponId.isBlank()) return false;
        WeaponDefinition weapon = definitions.snapshot().weapons().get(weaponId);
        if (weapon == null) return false;
        if (data.getLevel() < weapon.minimumEquipLevel() || data.getLevel() > weapon.maximumEquipLevel()) return false;
        WeaponDefinition.SkillDefinition ability = ultimate ? weapon.ultimate() : weapon.skill();
        if (ability == null || !conditionsMet(player, target, ability.conditions())) return false;

        ItemInstance instance = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
        int limitBreak = instance == null ? 0 : instance.getLimitBreak();
        double baseCooldown = override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".cooldown", ability.cooldownSeconds());
        int maxCharges = Math.max(1, (int) override(weapon, limitBreak, (ultimate ? "ultimate" : "skill") + ".charges", ability.charges()));
        String cooldownId = weaponId + ":" + (ultimate ? "ultimate" : "skill");
        AbilityState state = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(cooldownId, ignored -> new AbilityState(maxCharges));
        double adjustedCooldown = CoreMath.cooldownSeconds(baseCooldown, stats.get(player, data).value(StatKey.COOLDOWN));
        state.refresh(maxCharges, adjustedCooldown);
        if (!state.consume(maxCharges, adjustedCooldown)) {
            player.sendActionBar(mini.deserialize("<red>あと " + String.format(Locale.ROOT, "%.1f", state.remainingSeconds()) + "秒</red>"));
            return true;
        }

        combat.touch(player, weaponId);
        stats.invalidate(player.getUniqueId());
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
        if (targets.isEmpty() && !ability.target().equalsIgnoreCase("SELF")) return true;
        if (ability.target().equalsIgnoreCase("SELF")) { targets.clear(); targets.add(player); }
        Element activeElement = element;
        for (LivingEntity current : targets) {
            damage.apply(new DamageRequest(player.getUniqueId(), current.getUniqueId(), ability.referenceStat(), multiplier,
                    activeElement, true, false, 0, ultimate ? "ultimate:" + ability.id() : "skill:" + ability.id()));
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

    private LivingEntity rayTarget(Player player) {
        RayTraceResult ray = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 16,
                0.5, entity -> entity instanceof LivingEntity && !entity.equals(player));
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
        }
        return result;
    }

    private double number(Object value) { return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value)); }

    public Status status(UUID player, boolean ultimate) {
        String suffix = ultimate ? ":ultimate" : ":skill";
        AbilityState state = cooldowns.getOrDefault(player, Map.of()).entrySet().stream().filter(e -> e.getKey().endsWith(suffix)).map(Map.Entry::getValue).findFirst().orElse(null);
        if (state == null) return new Status(true, 0, 1);
        state.refresh(state.maximumCharges, state.cooldownSeconds);
        return new Status(state.charges > 0, state.remainingSeconds(), state.charges);
    }

    public record Status(boolean ready, double remainingSeconds, int charges) {}

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
