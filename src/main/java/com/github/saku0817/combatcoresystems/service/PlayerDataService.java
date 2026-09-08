package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.storage.StorageService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlayerDataService {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final DefinitionRegistry definitions;
    private final Map<UUID, PlayerData> loaded = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile boolean retryPending;

    public PlayerDataService(JavaPlugin plugin, StorageService storage, DefinitionRegistry definitions) {
        this.plugin = plugin;
        this.storage = storage;
        this.definitions = definitions;
    }

    public void load(Player player, Consumer<PlayerData> whenReady) {
        UUID uuid = player.getUniqueId();
        PlayerData existing = loaded.get(uuid);
        if (existing != null) { whenReady.accept(existing); return; }
        if (!loading.add(uuid)) return;
        storage.loadPlayer(uuid, player.getName()).whenComplete((data, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            loading.remove(uuid);
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not load player " + uuid, error);
                player.kick(miniMessage.deserialize("<red>CCSプレイヤーデータを安全に読み込めませんでした。管理者へ連絡してください。</red>"));
                return;
            }
            if (!player.isOnline()) return;
            loaded.put(uuid, data);
            whenReady.accept(data);
        }));
    }

    public Optional<PlayerData> find(UUID uuid) { return Optional.ofNullable(loaded.get(uuid)); }
    public PlayerData require(Player player) {
        PlayerData data = loaded.get(player.getUniqueId());
        if (data == null) throw new IllegalStateException("Player data is not ready for " + player.getName());
        return data;
    }
    public Collection<PlayerData> loaded() { return Collections.unmodifiableCollection(loaded.values()); }

    public CompletableFuture<Void> reloadOnline(Consumer<PlayerData> afterReplace) {
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        List<CompletableFuture<PlayerData>> requests = online.stream()
                .map(player -> storage.loadPlayer(player.getUniqueId(), player.getName())).toList();
        return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).thenCompose(ignored -> {
            CompletableFuture<Void> applied = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    for (int i = 0; i < online.size(); i++) {
                        PlayerData data = requests.get(i).join(); loaded.put(online.get(i).getUniqueId(), data); afterReplace.accept(data);
                    }
                    applied.complete(null);
                } catch (Throwable error) { applied.completeExceptionally(error); }
            });
            return applied;
        });
    }

    public CompletableFuture<Void> saveAll() {
        List<PlayerData> snapshots = loaded.values().stream().map(storage::snapshot).toList();
        return storage.savePlayers(snapshots).whenComplete((ignored, error) -> {
            if (error == null) { retryPending = false; return; }
            retryPending = true;
            storage.reportFailure(error, this::notifyStorageAdmins);
        });
    }

    public CompletableFuture<Void> saveAndUnload(Player player) {
        PlayerData data = loaded.remove(player.getUniqueId());
        if (data == null) return CompletableFuture.completedFuture(null);
        return storage.savePlayers(List.of(storage.snapshot(data))).whenComplete((ignored, error) -> {
            if (error != null) {
                loaded.put(data.uuid(), data);
                retryPending = true;
                storage.reportFailure(error, this::notifyStorageAdmins);
            }
        });
    }

    public void schedule() {
        long autosaveTicks = Math.max(20L, definitions.snapshot().config("config.yml").getLong("autosave-minutes", 20) * 60L * 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, autosaveTicks, autosaveTicks);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> { if (retryPending) saveAll(); }, 600L, 600L);
    }

    private void notifyStorageAdmins(String message) {
        Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("combatcoresystems.admin.storage.alert"))
                .forEach(p -> p.sendMessage(miniMessage.deserialize(message)));
    }
}
