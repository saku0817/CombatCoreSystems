package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.MobDefinition;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class MobService implements Listener {
    private final DefinitionRegistry definitions;
    private final NamespacedKey definitionKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey bossKey;
    private final NamespacedKey autoSpawnKey;
    private final NamespacedKey virtualHealthKey;
    private final NamespacedKey virtualMaxHealthKey;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Set<UUID> managed = ConcurrentHashMap.newKeySet();
    private DefinitionRegistry.Snapshot indexedSnapshot;
    private final java.util.Map<EntityType, MobDefinition> vanillaIndex = new java.util.EnumMap<>(EntityType.class);
    private final java.util.Map<UUID, PhaseModifiers> phaseModifiers = new ConcurrentHashMap<>();

    public MobService(JavaPlugin plugin, DefinitionRegistry definitions) {
        this.definitions = definitions;
        this.definitionKey = new NamespacedKey(plugin, "mob_definition");
        this.levelKey = new NamespacedKey(plugin, "mob_level");
        this.bossKey = new NamespacedKey(plugin, "boss");
        this.autoSpawnKey = new NamespacedKey(plugin, "auto_spawned");
        this.virtualHealthKey = new NamespacedKey(plugin, "mob_virtual_health");
        this.virtualMaxHealthKey = new NamespacedKey(plugin, "mob_virtual_max_health");
    }

    public Optional<LivingEntity> spawn(String id, boolean boss, Location location, boolean automatic) {
        MobDefinition definition = (boss ? definitions.snapshot().bosses() : definitions.snapshot().mobs()).get(id);
        if (definition == null || location.getWorld() == null) return Optional.empty();
        EntityType type = EntityType.valueOf(definition.entityType().toUpperCase());
        if (!type.isAlive()) return Optional.empty();
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        int level = ThreadLocalRandom.current().nextInt(definition.minLevel(), definition.maxLevel() + 1);
        entity.getPersistentDataContainer().set(definitionKey, PersistentDataType.STRING, definition.id());
        entity.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        entity.getPersistentDataContainer().set(bossKey, PersistentDataType.BOOLEAN, boss);
        entity.getPersistentDataContainer().set(autoSpawnKey, PersistentDataType.BOOLEAN, automatic);
        managed.add(entity.getUniqueId());
        // A spawn event may already have applied a vanilla override to this new entity.
        entity.getPersistentDataContainer().remove(virtualMaxHealthKey);
        entity.getPersistentDataContainer().remove(virtualHealthKey);
        applyDefinition(entity, definition, level);
        if (boss) entity.setPersistent(true);
        return Optional.of(entity);
    }

    public Optional<MobDefinition> definition(LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(definitionKey, PersistentDataType.STRING);
        if (id == null) return vanillaDefinition(entity);
        boolean boss = Boolean.TRUE.equals(entity.getPersistentDataContainer().get(bossKey, PersistentDataType.BOOLEAN));
        return Optional.ofNullable((boss ? definitions.snapshot().bosses() : definitions.snapshot().mobs()).get(id));
    }

    public void start() {
        org.bukkit.Bukkit.getWorlds().forEach(world -> world.getLivingEntities().forEach(entity -> {
            if (entity.getPersistentDataContainer().has(definitionKey, PersistentDataType.STRING)) {
                definition(entity).ifPresent(definition -> { applyDefinition(entity, definition, level(entity)); managed.add(entity.getUniqueId()); });
            } else applyVanillaDefinition(entity);
        }));
    }

    public long customExperience(LivingEntity entity) {
        MobDefinition configured = definition(entity).orElse(null);
        if (configured != null) return configured.dropCustomExp() ? configured.exp() : 0;
        if (!(entity instanceof org.bukkit.entity.Mob)) return 0;
        var config = definitions.snapshot().config("mobs.yml");
        return config.getBoolean("vanilla-defaults.drop-custom-exp", true) ? Math.max(0, config.getLong("vanilla-defaults.custom-exp", 5)) : 0;
    }

    @EventHandler public void onCreatureSpawn(CreatureSpawnEvent event) { applyVanillaDefinition(event.getEntity()); }

    @EventHandler public void onEntitiesLoad(org.bukkit.event.world.EntitiesLoadEvent event) {
        for (var entity : event.getEntities()) if (entity instanceof LivingEntity living && !(living instanceof Player)) {
            if (living.getPersistentDataContainer().has(definitionKey, PersistentDataType.STRING))
                definition(living).ifPresent(value -> { applyDefinition(living, value, level(living)); managed.add(living.getUniqueId()); });
            else applyVanillaDefinition(living);
        }
    }

    private Optional<MobDefinition> vanillaDefinition(LivingEntity entity) {
        var snapshot = definitions.snapshot();
        if (indexedSnapshot != snapshot) {
            vanillaIndex.clear();
            snapshot.vanillaMobs().values().forEach(value -> vanillaIndex.put(EntityType.valueOf(value.entityType().toUpperCase(java.util.Locale.ROOT)), value));
            indexedSnapshot = snapshot;
        }
        return Optional.ofNullable(vanillaIndex.get(entity.getType()));
    }

    private void applyVanillaDefinition(LivingEntity entity) {
        if (entity instanceof Player || entity.getPersistentDataContainer().has(definitionKey, PersistentDataType.STRING)) return;
        MobDefinition definition = vanillaDefinition(entity).orElse(null);
        if (definition == null) {
            var armor = entity.getAttribute(Attribute.ARMOR);
            if (armor != null) armor.setBaseValue(0);
            return;
        }
        int level = entity.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER,
                ThreadLocalRandom.current().nextInt(definition.minLevel(), definition.maxLevel() + 1));
        entity.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        applyDefinition(entity, definition, level);
        managed.add(entity.getUniqueId());
    }

    private void applyDefinition(LivingEntity entity, MobDefinition definition, int level) {
        double hp = CoreMath.linear(definition.hpAtMin(), definition.hpAtMax(), level - definition.minLevel() + 1,
                definition.maxLevel() - definition.minLevel() + 1);
        boolean alreadyVirtual = entity.getPersistentDataContainer().has(virtualMaxHealthKey, PersistentDataType.DOUBLE);
        double priorHealth = alreadyVirtual ? health(entity) : hp;
        entity.getPersistentDataContainer().set(virtualMaxHealthKey, PersistentDataType.DOUBLE, hp);
        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        double physical = physicalMaximum(hp, definitions.snapshot().config("mobs.yml").getDouble("virtual-health.physical-cap", 1024));
        if (maxHealth != null) maxHealth.setBaseValue(physical);
        var attack = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(attack(definition, level));
        var armor = entity.getAttribute(Attribute.ARMOR);
        if (armor != null) armor.setBaseValue(0);
        setHealth(entity, Math.min(hp, priorHealth));
        if (definition.showLevel()) entity.customName(mini.deserialize("<gray>Lv." + level + "</gray> " + definition.name()));
    }

    public int level(LivingEntity entity) { return entity.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1); }
    public double maxHealth(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(virtualMaxHealthKey, PersistentDataType.DOUBLE,
                entity.getAttribute(Attribute.MAX_HEALTH) == null ? entity.getHealth() : entity.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
    public double health(LivingEntity entity) {
        Double saved = entity.getPersistentDataContainer().get(virtualHealthKey, PersistentDataType.DOUBLE);
        if (saved == null) return entity.getHealth();
        var attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double physicalMax = attribute == null ? maxHealth(entity) : attribute.getValue();
        // Reflect environmental damage and other plugins' healing without a per-tick entity scan.
        if (Math.abs(entity.getHealth() - physicalHealth(saved, maxHealth(entity), physicalMax)) < 0.0001) return saved;
        return CoreMath.toVirtualHealth(entity.getHealth(), physicalMax, maxHealth(entity));
    }
    public void setHealth(LivingEntity entity, double value) {
        if (!entity.getPersistentDataContainer().has(virtualMaxHealthKey, PersistentDataType.DOUBLE)) {
            entity.setHealth(Math.max(0, Math.min(maxHealth(entity), value)));
            return;
        }
        double maximum = maxHealth(entity);
        double current = Math.max(0, Math.min(maximum, value));
        entity.getPersistentDataContainer().set(virtualHealthKey, PersistentDataType.DOUBLE, current);
        if (current <= 0) { entity.setHealth(0); return; }
        double physicalMaximum = entity.getAttribute(Attribute.MAX_HEALTH) == null ? maximum : entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        entity.setHealth(physicalHealth(current, maximum, physicalMaximum));
    }
    static double physicalMaximum(double virtualMaximum, double configuredCap) { return Math.min(Math.max(1, virtualMaximum), Double.isFinite(configuredCap) ? Math.clamp(configuredCap, 1, 1024) : 1024); }
    static double physicalHealth(double current, double virtualMaximum, double physicalMaximum) {
        if (current <= 0) return 0;
        return Math.max(0.01, Math.min(physicalMaximum, current / Math.max(1e-9, virtualMaximum) * physicalMaximum));
    }
    public double attack(MobDefinition definition, int level) { return CoreMath.linear(definition.atkAtMin(), definition.atkAtMax(), level - definition.minLevel() + 1, definition.maxLevel() - definition.minLevel() + 1); }
    public double defense(MobDefinition definition, int level) { return CoreMath.linear(definition.defAtMin(), definition.defAtMax(), level - definition.minLevel() + 1, definition.maxLevel() - definition.minLevel() + 1); }
    public double defense(LivingEntity entity, MobDefinition definition, int level) { return defense(definition, level) * phaseModifiers.getOrDefault(entity.getUniqueId(), PhaseModifiers.DEFAULT).defenseMultiplier; }
    public boolean immune(LivingEntity entity, Element element) { return definition(entity).map(d -> d.immunities().contains(element)).orElse(false); }
    public boolean isBoss(LivingEntity entity) { return Boolean.TRUE.equals(entity.getPersistentDataContainer().get(bossKey, PersistentDataType.BOOLEAN)); }
    public boolean isAutomatic(LivingEntity entity) { return Boolean.TRUE.equals(entity.getPersistentDataContainer().get(autoSpawnKey, PersistentDataType.BOOLEAN)); }
    public void track(LivingEntity entity) { if (definition(entity).isPresent()) managed.add(entity.getUniqueId()); }
    public Set<UUID> managedIds() { managed.removeIf(uuid -> org.bukkit.Bukkit.getEntity(uuid) == null); return Set.copyOf(managed); }
    public void untrack(UUID entity) { managed.remove(entity); phaseModifiers.remove(entity); }
    public void setPhaseModifiers(LivingEntity entity, double attackMultiplier, double defenseMultiplier) {
        phaseModifiers.put(entity.getUniqueId(), new PhaseModifiers(attackMultiplier, defenseMultiplier));
        definition(entity).ifPresent(definition -> {
            var attack = entity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(attack(definition, level(entity)) * attackMultiplier);
        });
    }
    private record PhaseModifiers(double attackMultiplier, double defenseMultiplier) { private static final PhaseModifiers DEFAULT = new PhaseModifiers(1, 1); }
}
