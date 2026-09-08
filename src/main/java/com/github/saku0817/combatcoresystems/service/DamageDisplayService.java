package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class DamageDisplayService {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final Map<UUID, Deque<DisplayEntry>> byOwner = new HashMap<>();
    private final List<DisplayEntry> active = new ArrayList<>();
    private final MiniMessage mini = MiniMessage.miniMessage();

    public DamageDisplayService(JavaPlugin plugin, DefinitionRegistry definitions) {
        this.plugin = plugin;
        this.definitions = definitions;
    }

    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L); }

    public void damage(UUID owner, LivingEntity target, long amount, boolean critical, String reaction, boolean small) {
        damage(owner, target, amount, critical, reaction, small, 0);
    }

    public void damage(UUID owner, LivingEntity target, long amount, boolean critical, String reaction, boolean small, long overdamage) {
        if (!definitions.snapshot().config("config.yml").getBoolean("text-display.enabled", true)) return;
        String path = reaction != null ? "damage-display.reaction" : critical ? "damage-display.critical" : "damage-display.normal";
        String fallback = reaction != null ? "<aqua><reaction> <damage></aqua>" : critical ? "<gold>CRIT <damage></gold>" : "<white><damage></white>";
        Component text = mini.deserialize(template(path, fallback).replace("<reaction>", reaction == null ? "" : reaction)
                .replace("<damage>", Long.toString(amount)).replace("<overdamage>", Long.toString(overdamage)));
        spawn(owner, target, text, critical ? 1.35f : small ? 0.7f : 1.0f);
    }

    public void heal(UUID owner, LivingEntity target, long amount, boolean small) {
        spawn(owner, target, mini.deserialize(template("damage-display.heal", "<green>+<heal></green>").replace("<heal>", Long.toString(amount))), small ? 0.7f : 1.0f);
    }

    private void spawn(UUID owner, LivingEntity target, Component text, float scale) {
        int limit = definitions.snapshot().config("config.yml").getInt("text-display.max-per-player", 20);
        Deque<DisplayEntry> owned = byOwner.computeIfAbsent(owner, ignored -> new ArrayDeque<>());
        while (owned.size() >= limit) remove(owned.removeFirst());
        Location location = target.getLocation().add(ThreadLocalRandom.current().nextDouble(-0.35, 0.35), target.getHeight() + 0.35,
                ThreadLocalRandom.current().nextDouble(-0.35, 0.35));
        TextDisplay display = target.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(true);
            entity.setShadowed(true);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            Transformation transformation = entity.getTransformation();
            transformation.getScale().set(new Vector3f(scale));
            entity.setTransformation(transformation);
        });
        long duration = Math.max(1, Math.round(definitions.snapshot().config("config.yml").getDouble("text-display.duration-seconds", 0.8) * 20));
        DisplayEntry entry = new DisplayEntry(owner, display, Bukkit.getCurrentTick() + duration);
        owned.addLast(entry);
        active.add(entry);
    }

    private void tick() {
        Iterator<DisplayEntry> iterator = active.iterator();
        while (iterator.hasNext()) {
            DisplayEntry entry = iterator.next();
            if (!entry.display.isValid() || Bukkit.getCurrentTick() >= entry.removeTick) {
                entry.display.remove();
                Deque<DisplayEntry> owned = byOwner.get(entry.owner);
                if (owned != null) owned.remove(entry);
                iterator.remove();
                continue;
            }
            entry.display.teleport(entry.display.getLocation().add(0, 0.025, 0));
        }
    }

    private void remove(DisplayEntry entry) {
        active.remove(entry);
        entry.display.remove();
    }

    private String template(String path, String fallback) { return definitions.snapshot().config("messages.yml").getString(path, fallback); }

    private record DisplayEntry(UUID owner, TextDisplay display, long removeTick) {}
}
