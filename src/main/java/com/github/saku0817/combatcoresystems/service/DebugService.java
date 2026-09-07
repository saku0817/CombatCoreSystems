package com.github.saku0817.combatcoresystems.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugService {
    private final JavaPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Scheduled> scheduled = new ConcurrentHashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();

    public DebugService(JavaPlugin plugin) { this.plugin = plugin; }

    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L); }
    public void enable(UUID user, Duration duration) { sessions.put(user, new Session(System.currentTimeMillis() + duration.toMillis(), Set.of("all"))); }
    public void disable(UUID user) { sessions.remove(user); scheduled.remove(user); }
    public void schedule(UUID user, Duration delay, Duration duration) { scheduled.put(user, new Scheduled(System.currentTimeMillis() + delay.toMillis(), duration)); }
    public boolean enabled(UUID user) { Session session = sessions.get(user); return session != null && session.endsAt > System.currentTimeMillis(); }
    public Optional<Session> session(UUID user) { return Optional.ofNullable(sessions.get(user)); }
    public Optional<Scheduled> scheduled(UUID user) { return Optional.ofNullable(scheduled.get(user)); }

    public void log(UUID user, String category, String message) {
        Session session = sessions.get(user);
        if (session == null || (!session.categories.contains("all") && !session.categories.contains(category))) return;
        Player player = Bukkit.getPlayer(user);
        Component component = mini.deserialize("<gray>[DEBUG:" + category + "]</gray> " + message);
        if (player != null) player.sendMessage(component);
        plugin.getComponentLogger().info(component);
        writeFile(category, message);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().endsAt <= now);
        for (var entry : List.copyOf(scheduled.entrySet())) {
            if (entry.getValue().startsAt > now) continue;
            enable(entry.getKey(), entry.getValue().duration);
            scheduled.remove(entry.getKey());
        }
    }

    private void writeFile(String category, String message) {
        try {
            Path directory = plugin.getDataFolder().toPath().resolve("logs/debug"); Files.createDirectories(directory);
            Path file = directory.resolve(LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".log");
            String line = LocalDateTime.now() + " [" + category + "] " + message + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { plugin.getLogger().warning("Could not write debug log: " + ex.getMessage()); }
    }

    public record Session(long endsAt, Set<String> categories) {}
    public record Scheduled(long startsAt, Duration duration) {}
}
