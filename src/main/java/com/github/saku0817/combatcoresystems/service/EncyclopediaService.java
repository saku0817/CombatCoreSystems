package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.MobDefinition;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class EncyclopediaService implements Listener {
    private final PlayerDataService players;
    private final MobService mobs;
    private final com.github.saku0817.combatcoresystems.config.DefinitionRegistry definitions;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public EncyclopediaService(PlayerDataService players, MobService mobs,
                               com.github.saku0817.combatcoresystems.config.DefinitionRegistry definitions) {
        this.players = players; this.mobs = mobs; this.definitions = definitions;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEncounter(EntityDamageByEntityEvent event) {
        LivingEntity attacker = resolve(event.getDamager());
        if (attacker instanceof Player player && event.getEntity() instanceof LivingEntity target) unlock(player, target);
        if (event.getEntity() instanceof Player player && attacker != null) unlock(player, attacker);
    }

    private LivingEntity resolve(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    public boolean unlock(Player player, LivingEntity entity) {
        MobDefinition definition = mobs.definition(entity).orElse(null);
        if (definition == null) return false;
        PlayerData data = players.require(player);
        boolean added = (definition.boss() ? data.getDiscoveredBosses() : data.getDiscoveredMobs()).add(definition.id());
        if (added) player.sendMessage(mini.deserialize("<aqua>図鑑に新しい対象を登録しました: " + definition.name() + "</aqua>"));
        return added;
    }

    public boolean set(PlayerData data, boolean boss, String id, boolean discovered) {
        var values = boss ? data.getDiscoveredBosses() : data.getDiscoveredMobs();
        return discovered ? values.add(id) : values.remove(id);
    }

    public void setAll(PlayerData data, boolean discovered) {
        data.getDiscoveredMobs().clear(); data.getDiscoveredBosses().clear();
        if (discovered) { data.getDiscoveredMobs().addAll(mobsDefinitions(false)); data.getDiscoveredBosses().addAll(mobsDefinitions(true)); }
    }

    private java.util.Set<String> mobsDefinitions(boolean boss) {
        if (boss) return definitions.snapshot().bosses().keySet();
        java.util.Set<String> ids = new java.util.LinkedHashSet<>(definitions.snapshot().mobs().keySet());
        ids.addAll(definitions.snapshot().vanillaMobs().keySet());
        return ids;
    }
}
