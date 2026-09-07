package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.DamageApi;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageResult;
import com.github.saku0817.combatcoresystems.api.v1.event.AfterDamageEvent;
import com.github.saku0817.combatcoresystems.api.v1.event.BeforeDamageEvent;
import com.github.saku0817.combatcoresystems.api.v1.event.ElementReactionEvent;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class DamageService implements DamageApi, Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final ElementService elements;
    private final MobService mobs;
    private final PartyService parties;
    private final DamageDisplayService displays;
    private final RegionService regions;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey projectileWeaponKey;

    public DamageService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                         CombatStateService combat, ElementService elements, MobService mobs, PartyService parties,
                         DamageDisplayService displays, RegionService regions) {
        this.plugin = plugin;
        this.definitions = definitions;
        this.players = players;
        this.stats = stats;
        this.combat = combat;
        this.elements = elements;
        this.mobs = mobs;
        this.parties = parties;
        this.displays = displays;
        this.regions = regions;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.projectileWeaponKey = new NamespacedKey(plugin, "projectile_weapon");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        String weapon = player.getInventory().getItemInMainHand().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (weapon != null) event.getEntity().getPersistentDataContainer().set(projectileWeaponKey, PersistentDataType.STRING, weapon);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(target)) { if (attacker != null) event.setCancelled(true); return; }
        if (!allowed(attacker, target)) { event.setCancelled(true); return; }

        String weaponId = weaponId(event.getDamager(), attacker);
        combat.touch(attacker, weaponId);
        combat.touch(target, "");
        if (attacker instanceof Player player) stats.invalidate(player.getUniqueId());

        double multiplier = 1.0;
        if (attacker instanceof Player player && !(event.getDamager() instanceof Projectile)) {
            double cooled = player.getAttackCooldown();
            multiplier = 0.2 + cooled * cooled * 0.8;
        }
        Element element = mobs.definition(attacker).map(MobDefinition::nativeElement).orElse(Element.PHYSICAL);
        if (attacker instanceof Player player) {
            PlayerData data = players.require(player); ItemInstance heart = data.getEquipment().get(EquipmentSlot.DIVINE_HEART);
            if (heart != null) {
                var heartConfig = definitions.snapshot().config("divine_hearts.yml");
                String path = "divine-hearts." + heart.getDefinitionId() + ".rules.";
                element = Element.parse(heartConfig.contains(path + "normal-attack-attribute")
                        ? heartConfig.getString(path + "normal-attack-attribute") : heartConfig.getString(path + "normal-attack-element")).orElse(element);
            }
        }
        DamageRequest request = new DamageRequest(attacker.getUniqueId(), target.getUniqueId(), ReferenceStat.ATK,
                multiplier, element, true, false, 0, "normal_attack");
        DamageResult result = apply(request);
        if (!result.applied()) return;
        event.setCancelled(true);
        if (result.finalDamage() > 0) applyStandardKnockback(attacker, target);
    }

    private boolean allowed(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof Player first && target instanceof Player second) {
            boolean global = definitions.snapshot().config("config.yml").getBoolean("pvp-enabled", true);
            String regional = regions.flag(target.getLocation(), "pvp").orElse("");
            boolean areaAllows = regional.equalsIgnoreCase("allow") || (regional.isBlank() && global);
            if (!areaAllows) return false;
            PlayerData firstData = players.find(first.getUniqueId()).orElse(null);
            PlayerData secondData = players.find(second.getUniqueId()).orElse(null);
            if (firstData == null || secondData == null || !firstData.isPvpEnabled() || !secondData.isPvpEnabled()) return false;
            return !parties.sameParty(first.getUniqueId(), second.getUniqueId());
        }
        return true;
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    private String weaponId(Entity damageEntity, LivingEntity attacker) {
        if (damageEntity instanceof Projectile projectile) {
            String tagged = projectile.getPersistentDataContainer().get(projectileWeaponKey, PersistentDataType.STRING);
            if (tagged != null) return tagged;
        }
        if (attacker instanceof Player player) {
            String id = player.getInventory().getItemInMainHand().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            return id == null ? "" : id;
        }
        return "";
    }

    @Override public DamageResult apply(DamageRequest request) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Damage API must be called from the server thread");
        Entity rawTarget = Bukkit.getEntity(request.target());
        if (!(rawTarget instanceof LivingEntity target) || target.isDead()) return DamageResult.failed(request.source(), "target_not_available");
        LivingEntity attacker = null;
        if (request.attacker() != null) {
            Entity rawAttacker = Bukkit.getEntity(request.attacker());
            if (!(rawAttacker instanceof LivingEntity living)) return DamageResult.failed(request.source(), "attacker_not_available");
            attacker = living;
        }
        BeforeDamageEvent before = new BeforeDamageEvent(request);
        Bukkit.getPluginManager().callEvent(before);
        if (before.isCancelled()) return DamageResult.failed(request.source(), "cancelled");

        DamageResult result = calculate(request, attacker, target);
        if (!result.applied()) return result;
        long overdamage = overdamage(target, result.finalDamage());
        subtractHealth(target, result.finalDamage());
        UUID owner = attacker == null ? target.getUniqueId() : attacker.getUniqueId();
        displays.damage(owner, target, result.finalDamage(), result.critical(), null, false, overdamage);
        Bukkit.getPluginManager().callEvent(new AfterDamageEvent(request, result));

        if (!request.fixedDamage() && request.element() != Element.PHYSICAL && !mobs.immune(target, request.element()) && attacker != null && !target.isDead()) {
            var config = definitions.snapshot().config("config.yml");
            double duration = config.contains("attribute-attachment-seconds")
                    ? config.getDouble("attribute-attachment-seconds", 5)
                    : config.getDouble("element-attachment-seconds", 5);
            LivingEntity reactionAttacker = attacker;
            elements.attach(target, request.element(), duration).ifPresent(trigger -> applyReaction(reactionAttacker, target, request.referenceStat(), trigger));
        }
        return result;
    }

    private DamageResult calculate(DamageRequest request, LivingEntity attacker, LivingEntity target) {
        if (request.fixedDamage()) {
            return new DamageResult(true, CoreMath.roundedDamage(request.fixedAmount()), false, request.element(),
                    request.fixedAmount(), 1, 0, request.source(), "");
        }
        if (attacker == null) return DamageResult.failed(request.source(), "attacker_required");
        CombatantStats source = combatant(attacker);
        CombatantStats defender = combatant(target);
        double reference = switch (request.referenceStat()) { case HP -> source.hp; case ATK -> source.atk; case DEF -> source.def; };
        double base = Math.max(0, reference * request.multiplier());
        double effectiveDef = CoreMath.effectiveDefense(defender.def, defender.defDown);
        double defenseCoefficient = CoreMath.defenseCoefficient(source.level, defender.level, effectiveDef);
        double damage = base * defenseCoefficient;
        double resistance = 0;
        if (request.element() != Element.PHYSICAL) {
            if (mobs.immune(target, request.element())) damage = 0;
            else {
                resistance = CoreMath.finalResistance(defender.resistance(request.element()),
                        defender.resistanceDown(request.element()) + elements.resistanceDown(target.getUniqueId(), request.element()));
                damage *= (1 + source.elementDamage(request.element())) * (1 - resistance);
            }
        }
        boolean critical = request.canCritical() && ThreadLocalRandom.current().nextDouble() < Math.min(1, source.critRate);
        if (critical) damage *= 1 + source.critDamage;
        return new DamageResult(true, CoreMath.roundedDamage(damage), critical, request.element(), base,
                defenseCoefficient, resistance, request.source(), "");
    }

    private void applyReaction(LivingEntity attacker, LivingEntity central, ReferenceStat referenceStat, ElementService.ReactionTrigger trigger) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(central);
        if (trigger.definition().radius() > 0) {
            central.getWorld().getNearbyLivingEntities(central.getLocation(), trigger.definition().radius(),
                    entity -> !entity.equals(attacker) && !entity.equals(central)).forEach(targets::add);
        }
        CombatantStats source = combatant(attacker);
        double reference = switch (referenceStat) { case HP -> source.hp; case ATK -> source.atk; case DEF -> source.def; };
        for (LivingEntity target : targets) {
            CombatantStats defender = combatant(target);
            double total = 0;
            for (Map.Entry<Element, Double> component : trigger.definition().components().entrySet()) {
                if (mobs.immune(target, component.getKey())) continue;
                double resistance = CoreMath.finalResistance(defender.resistance(component.getKey()),
                        defender.resistanceDown(component.getKey()) + elements.resistanceDown(target.getUniqueId(), component.getKey()));
                int hits = component.getKey() == trigger.definition().multiHitElement() ? trigger.definition().hits() : 1;
                total += reference * component.getValue() * (1 + source.elementDamage(component.getKey())) * (1 - resistance) * hits;
            }
            boolean critical = ThreadLocalRandom.current().nextDouble() < Math.min(1, source.critRate);
            if (critical) total *= 1 + source.critDamage;
            long rounded = CoreMath.roundedDamage(total);
            long overdamage = overdamage(target, rounded);
            subtractHealth(target, rounded);
            displays.damage(attacker.getUniqueId(), target, rounded, critical, trigger.definition().name(), false, overdamage);
            if (trigger.definition().levitation() > 0) target.setVelocity(target.getVelocity().setY(trigger.definition().levitation()));
            if (trigger.definition().resistanceDownElement() != null) elements.applyResistanceDown(target.getUniqueId(),
                    trigger.definition().resistanceDownElement(), trigger.definition().resistanceDown(), trigger.definition().resistanceDownSeconds());
            Bukkit.getPluginManager().callEvent(new ElementReactionEvent(target.getUniqueId(), trigger.definition().id(),
                    trigger.existing(), trigger.incoming(), rounded));
        }
    }

    private CombatantStats combatant(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = players.require(player);
            PlayerStats value = stats.get(player, data);
            return new CombatantStats(value.level(), value.maxHp(), value.atk(), value.def(), value.value(StatKey.CRIT_RATE),
                    value.value(StatKey.CRIT_DAMAGE), value.value(StatKey.DEF_DOWN), value);
        }
        MobDefinition definition = mobs.definition(entity).orElse(null);
        int level = definition == null ? 1 : mobs.level(entity);
        double hp = attribute(entity, Attribute.MAX_HEALTH, entity.getHealth());
        double atk = attribute(entity, Attribute.ATTACK_DAMAGE, 2);
        double def = definition == null ? 0 : mobs.defense(entity, definition, level);
        return new CombatantStats(level, hp, atk, def, 0.05, 0.5, 0, definition);
    }

    private double attribute(LivingEntity entity, Attribute attribute, double fallback) {
        var instance = entity.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    private void subtractHealth(LivingEntity target, long damage) {
        if (damage <= 0 || target.isDead()) return;
        target.setLastDamage(damage);
        target.setNoDamageTicks(target.getMaximumNoDamageTicks());
        target.setHealth(Math.max(0, target.getHealth() - damage));
    }

    private long overdamage(LivingEntity target, long damage) {
        if (!definitions.snapshot().config("config.yml").getBoolean("text-display.show-overdamage", true)) return 0;
        return CoreMath.overdamage(damage, target.getHealth());
    }

    private void applyStandardKnockback(LivingEntity attacker, LivingEntity target) {
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() == 0) return;
        target.setVelocity(target.getVelocity().multiply(0.5).add(direction.normalize().multiply(0.4)).setY(0.2));
    }

    private record CombatantStats(int level, double hp, double atk, double def, double critRate, double critDamage,
                                  double defDown, Object details) {
        double elementDamage(Element element) {
            if (details instanceof PlayerStats player) return player.elementDamage(element);
            return 0;
        }
        double resistance(Element element) {
            if (details instanceof PlayerStats player) return player.resistance(element);
            if (details instanceof MobDefinition mob) return mob.resistances().getOrDefault(element, 0.0);
            return 0;
        }
        double resistanceDown(Element element) { return details instanceof PlayerStats player ? player.resistanceDown(element) : 0; }
    }
}
