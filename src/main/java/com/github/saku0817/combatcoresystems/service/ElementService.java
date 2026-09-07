package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.ElementAttachment;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.ReactionDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ElementService {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final AtomicLong order = new AtomicLong();
    private final Map<UUID, EnumMap<Element, Attached>> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> reactionCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, EnumMap<Element, ResistanceDown>> resistanceDowns = new ConcurrentHashMap<>();

    public ElementService(JavaPlugin plugin, DefinitionRegistry definitions) {
        this.plugin = plugin;
        this.definitions = definitions;
    }

    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::expire, 5L, 5L); }

    public Optional<ReactionTrigger> attach(LivingEntity target, Element incoming, double durationSeconds) {
        if (incoming == Element.PHYSICAL) return Optional.empty();
        long now = System.currentTimeMillis();
        EnumMap<Element, Attached> current = attachments.computeIfAbsent(target.getUniqueId(), ignored -> new EnumMap<>(Element.class));
        current.entrySet().removeIf(entry -> entry.getValue().endsAt <= now);
        Attached same = current.get(incoming);
        if (same != null) {
            current.put(incoming, new Attached(now + (long) (durationSeconds * 1000), order.incrementAndGet()));
            return Optional.empty();
        }
        Map.Entry<Element, Attached> newest = current.entrySet().stream().max(Comparator.comparingLong(e -> e.getValue().order)).orElse(null);
        if (newest != null) {
            ReactionDefinition reaction = definitions.snapshot().reactions().values().stream()
                    .filter(candidate -> candidate.matches(newest.getKey(), incoming)).findFirst().orElse(null);
            if (reaction != null) {
                long readyAt = reactionCooldowns.computeIfAbsent(target.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                        .getOrDefault(reaction.id(), 0L);
                if (readyAt <= now) {
                    current.remove(newest.getKey());
                    reactionCooldowns.get(target.getUniqueId()).put(reaction.id(), now + (long) (reaction.cooldownSeconds() * 1000));
                    return Optional.of(new ReactionTrigger(reaction, newest.getKey(), incoming));
                }
            }
        }
        current.put(incoming, new Attached(now + (long) (durationSeconds * 1000), order.incrementAndGet()));
        return Optional.empty();
    }

    public void applyResistanceDown(UUID target, Element element, double amount, double durationSeconds) {
        if (element == null || amount == 0 || durationSeconds <= 0) return;
        resistanceDowns.computeIfAbsent(target, ignored -> new EnumMap<>(Element.class))
                .put(element, new ResistanceDown(amount, System.currentTimeMillis() + (long) (durationSeconds * 1000)));
    }

    public double resistanceDown(UUID target, Element element) {
        ResistanceDown down = resistanceDowns.getOrDefault(target, new EnumMap<>(Element.class)).get(element);
        if (down == null || down.endsAt <= System.currentTimeMillis()) return 0;
        return down.amount;
    }

    public Map<Element, Long> remaining(UUID entity) {
        long now = System.currentTimeMillis();
        EnumMap<Element, Long> result = new EnumMap<>(Element.class);
        attachments.getOrDefault(entity, new EnumMap<>(Element.class)).forEach((element, value) -> {
            if (value.endsAt > now) result.put(element, value.endsAt - now);
        });
        return result;
    }

    public void resumePlayer(PlayerData data) {
        EnumMap<Element, Attached> resumed = new EnumMap<>(Element.class);
        long now = System.currentTimeMillis();
        for (ElementAttachment attachment : data.getElements()) {
            if (attachment.getElement() != Element.PHYSICAL && attachment.getRemainingMillis() > 0) {
                resumed.put(attachment.getElement(), new Attached(now + attachment.getRemainingMillis(), attachment.getAttachedOrder()));
            }
        }
        if (!resumed.isEmpty()) attachments.put(data.uuid(), resumed);
        data.getElements().clear();
    }

    public void pausePlayer(PlayerData data) {
        long now = System.currentTimeMillis();
        data.getElements().clear();
        EnumMap<Element, Attached> current = attachments.remove(data.uuid());
        if (current == null) return;
        current.forEach((element, value) -> {
            if (value.endsAt > now) data.getElements().add(new ElementAttachment(element, value.endsAt - now, value.order));
        });
    }

    public void clear(UUID entity) {
        attachments.remove(entity);
        reactionCooldowns.remove(entity);
        resistanceDowns.remove(entity);
    }

    public boolean remove(UUID entity, Element element) {
        EnumMap<Element, Attached> values = attachments.get(entity);
        return values != null && values.remove(element) != null;
    }

    private void expire() {
        long now = System.currentTimeMillis();
        attachments.values().forEach(map -> map.entrySet().removeIf(e -> e.getValue().endsAt <= now));
        attachments.entrySet().removeIf(e -> e.getValue().isEmpty());
        reactionCooldowns.values().forEach(map -> map.entrySet().removeIf(e -> e.getValue() <= now));
        resistanceDowns.values().forEach(map -> map.entrySet().removeIf(e -> e.getValue().endsAt <= now));
    }

    private record Attached(long endsAt, long order) {}
    private record ResistanceDown(double amount, long endsAt) {}
    public record ReactionTrigger(ReactionDefinition definition, Element existing, Element incoming) {}
}
