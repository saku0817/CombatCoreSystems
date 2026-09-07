package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BackupService {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final StorageService storage;
    private final PlayerDataService players;
    private final PartyService parties;
    private final StatService stats;
    private final LevelService levels;
    private final ElementService elements;

    public BackupService(JavaPlugin plugin, DefinitionRegistry definitions, StorageService storage,
                         PlayerDataService players, PartyService parties, StatService stats, LevelService levels,
                         ElementService elements) {
        this.plugin = plugin; this.definitions = definitions; this.storage = storage; this.players = players; this.parties = parties;
        this.stats = stats; this.levels = levels; this.elements = elements;
    }

    public void schedule() {
        long ticks = Math.max(20, definitions.snapshot().config("config.yml").getLong("backup-hours", 12) * 60L * 60L * 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::create, ticks, ticks);
    }

    public CompletableFuture<String> create() {
        parties.save();
        return players.saveAll().thenCompose(ignored -> {
            try {
                Path backupDirectory = plugin.getDataFolder().toPath().resolve("backups");
                Files.createDirectories(backupDirectory);
                String id = "cssbackup_" + LocalDateTime.now().format(FORMAT);
                Path export = Files.createTempFile(plugin.getDataFolder().toPath(), "ccs-export-", ".json");
                return storage.exportTo(export.toFile()).thenApply(file -> {
                    try {
                        Path zip = backupDirectory.resolve(id + ".zip");
                        writeZip(zip, export);
                        Files.deleteIfExists(export);
                        rotate(backupDirectory);
                        return id;
                    } catch (IOException ex) { throw new CompletionException(ex); }
                });
            } catch (IOException ex) { return CompletableFuture.failedFuture(ex); }
        });
    }

    public CompletableFuture<Void> restore(String id) {
        Path backup = plugin.getDataFolder().toPath().resolve("backups").resolve(id + (id.endsWith(".zip") ? "" : ".zip"));
        if (!Files.isRegularFile(backup)) return CompletableFuture.failedFuture(new FileNotFoundException(id));
        return create().thenCompose(preRestore -> CompletableFuture.supplyAsync(() -> {
            try {
                Path temp = Files.createTempDirectory(plugin.getDataFolder().toPath(), "restore-");
                unzipSafely(backup, temp);
                return temp;
            } catch (IOException ex) { throw new CompletionException(ex); }
        })).thenCompose(temp -> {
            Path export = temp.resolve("data-export.json");
            return storage.restoreExport(export.toFile()).thenRun(() -> {
                try {
                    for (String name : DefinitionRegistry.FILES) {
                        Path restored = temp.resolve("config").resolve(name);
                        if (Files.isRegularFile(restored)) Files.copy(restored, plugin.getDataFolder().toPath().resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    }
                    definitions.reloadSafely();
                    deleteTree(temp);
                } catch (IOException ex) { throw new CompletionException(ex); }
            }).thenCompose(ignored -> parties.load()).thenCompose(ignored -> players.reloadOnline(data -> {
                var player = Bukkit.getPlayer(data.uuid()); if (player == null) return;
                elements.clear(data.uuid()); elements.resumePlayer(data); stats.invalidate(data.uuid()); levels.apply(player, data, false);
            }));
        });
    }

    public List<String> list() {
        Path directory = plugin.getDataFolder().toPath().resolve("backups");
        if (!Files.isDirectory(directory)) return List.of();
        try (var stream = Files.list(directory)) {
            return stream.filter(path -> path.getFileName().toString().startsWith("cssbackup_") && path.toString().endsWith(".zip"))
                    .sorted(Comparator.reverseOrder()).map(path -> path.getFileName().toString().replaceFirst("\\.zip$", "")).toList();
        } catch (IOException ex) { return List.of(); }
    }

    private void writeZip(Path output, Path export) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            zip.putNextEntry(new ZipEntry("data-export.json")); Files.copy(export, zip); zip.closeEntry();
            for (String name : DefinitionRegistry.FILES) {
                Path file = plugin.getDataFolder().toPath().resolve(name);
                if (!Files.isRegularFile(file)) continue;
                zip.putNextEntry(new ZipEntry("config/" + name)); Files.copy(file, zip); zip.closeEntry();
            }
        }
    }

    private void rotate(Path directory) throws IOException {
        int retention = Math.max(1, definitions.snapshot().config("config.yml").getInt("backup-retention", 7));
        try (var stream = Files.list(directory)) {
            List<Path> backups = stream.filter(path -> path.getFileName().toString().startsWith("cssbackup_") && path.toString().endsWith(".zip"))
                    .sorted().toList();
            for (int i = 0; i < backups.size() - retention; i++) Files.deleteIfExists(backups.get(i));
        }
    }

    private void unzipSafely(Path zipPath, Path destination) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) throw new IOException("Unsafe backup entry");
                if (entry.isDirectory()) Files.createDirectories(target);
                else { Files.createDirectories(target.getParent()); Files.copy(zip, target); }
            }
        }
    }

    private void deleteTree(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
