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
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Set<UUID> managed = ConcurrentHashMap.newKeySet();
    private final java.util.Map<UUID, PhaseModifiers> phaseModifiers = new ConcurrentHashMap<>();

    public MobService(JavaPlugin plugin, DefinitionRegistry definitions) {
        this.definitions = definitions;
        this.definitionKey = new NamespacedKey(plugin, "mob_definition");
        this.levelKey = new NamespacedKey(plugin, "mob_level");
        this.bossKey = new NamespacedKey(plugin, "boss");
        this.autoSpawnKey = new NamespacedKey(plugin, "auto_spawned");
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
        org.bukkit.Bukkit.getWorlds().forEach(world -> world.getLivingEntities().forEach(this::applyVanillaDefinition));
    }

    @EventHandler public void onCreatureSpawn(CreatureSpawnEvent event) { applyVanillaDefinition(event.getEntity()); }

    private Optional<MobDefinition> vanillaDefinition(LivingEntity entity) {
        return definitions.snapshot().vanillaMobs().values().stream()
                .filter(definition -> definition.entityType().equalsIgnoreCase(entity.getType().name())).findFirst();
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
        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(hp);
        var attack = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(attack(definition, level));
        var armor = entity.getAttribute(Attribute.ARMOR);
        if (armor != null) armor.setBaseValue(0);
        entity.setHealth(hp);
        if (definition.showLevel()) entity.customName(mini.deserialize("<gray>Lv." + level + "</gray> " + definition.name()));
    }

    public int level(LivingEntity entity) { return entity.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1); }
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
