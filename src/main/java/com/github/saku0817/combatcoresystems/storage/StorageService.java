package com.github.saku0817.combatcoresystems.storage;

import com.github.saku0817.combatcoresystems.model.PartyData;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class StorageService implements AutoCloseable {
    public enum Backend { SQLITE, YAML, JSON, MYSQL, MARIADB }

    private final JavaPlugin plugin;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CCS-Storage");
        thread.setDaemon(true);
        return thread;
    });
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final Set<UUID> pendingPlayers = ConcurrentHashMap.newKeySet();
    private final Object connectionLock = new Object();
    private volatile Backend backend;
    private volatile Connection connection;
    private File flatFile;
    private final Map<String, String> flatPlayers = new LinkedHashMap<>();
    private final Map<String, String> flatParties = new LinkedHashMap<>();
    private volatile long lastSuccessfulSave;

    public StorageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize(YamlConfiguration settings) throws Exception {
        try {
            backend = Backend.valueOf(settings.getString("backend", "SQLITE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unsupported storage backend", ex);
        }
        if (backend == Backend.YAML || backend == Backend.JSON) {
            flatFile = new File(plugin.getDataFolder(), backend == Backend.YAML ? "data.yml" : "data.json");
            loadFlatFile();
            return;
        }
        Class.forName(switch (backend) {
            case SQLITE -> "org.sqlite.JDBC";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            default -> throw new IllegalStateException("Unexpected backend");
        });
        String url;
        Properties properties = new Properties();
        if (backend == Backend.SQLITE) {
            File database = new File(plugin.getDataFolder(), settings.getString("sqlite.file", "data.db"));
            url = "jdbc:sqlite:" + database.getAbsolutePath();
        } else {
            String prefix = backend == Backend.MYSQL ? "mysql" : "mariadb";
            url = "jdbc:" + prefix + "://" + settings.getString("mysql.host", "localhost") + ":"
                    + settings.getInt("mysql.port", 3306) + "/" + settings.getString("mysql.database", "combatcoresystems")
                    + "?useSSL=" + settings.getBoolean("mysql.use-ssl", false);
            properties.setProperty("user", settings.getString("mysql.username", "root"));
            properties.setProperty("password", settings.getString("mysql.password", ""));
        }
        connection = DriverManager.getConnection(url, properties);
        connection.setAutoCommit(true);
        createSchema();
    }

    private void createSchema() throws SQLException {
        synchronized (connectionLock) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS ccs_players (uuid VARCHAR(36) PRIMARY KEY, data_version INTEGER NOT NULL, payload TEXT NOT NULL, updated_at BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS ccs_parties (id VARCHAR(36) PRIMARY KEY, data_version INTEGER NOT NULL, payload TEXT NOT NULL, updated_at BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS ccs_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value TEXT NOT NULL)");
            }
        }
    }

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String payload = readPayload("ccs_players", "uuid", uuid.toString());
                if (payload == null) return new PlayerData(uuid, name);
                PlayerData data = gson.fromJson(payload, PlayerData.class);
                if (data.getDataVersion() > PlayerData.DATA_VERSION) {
                    throw new FutureDataVersionException("Player " + uuid + " uses data-version " + data.getDataVersion());
                }
                data.setLastName(name);
                return data.normalize();
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, ioExecutor);
    }

    public CompletableFuture<Map<String, PartyData>> loadParties() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> rows = readAll("ccs_parties", "id");
                Map<String, PartyData> result = new LinkedHashMap<>();
                for (Map.Entry<String, String> row : rows.entrySet()) {
                    PartyData party = gson.fromJson(row.getValue(), PartyData.class);
                    if (party.getDataVersion() > PlayerData.DATA_VERSION) throw new FutureDataVersionException("Party " + row.getKey() + " uses a future data-version");
                    result.put(row.getKey(), party);
                }
                return result;
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, ioExecutor);
    }

    public CompletableFuture<Void> savePlayers(Collection<PlayerData> players) {
        List<PlayerData> snapshot = List.copyOf(players);
        snapshot.forEach(data -> pendingPlayers.add(data.uuid()));
        return CompletableFuture.runAsync(() -> {
            try {
                for (PlayerData data : snapshot) {
                    data.setLastSaveEpochMillis(System.currentTimeMillis());
                    writePayload("ccs_players", "uuid", data.getUuid(), data.getDataVersion(), gson.toJson(data));
                    pendingPlayers.remove(data.uuid());
                }
                flushFlatFile();
                lastSuccessfulSave = System.currentTimeMillis();
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, ioExecutor);
    }

    public CompletableFuture<Void> saveParties(Collection<PartyData> parties) {
        List<PartyData> snapshot = List.copyOf(parties);
        return CompletableFuture.runAsync(() -> {
            try {
                Set<String> live = new HashSet<>();
                for (PartyData party : snapshot) {
                    live.add(party.getId());
                    writePayload("ccs_parties", "id", party.getId(), party.getDataVersion(), gson.toJson(party));
                }
                deleteMissingParties(live);
                flushFlatFile();
                lastSuccessfulSave = System.currentTimeMillis();
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, ioExecutor);
    }

    public CompletableFuture<File> exportTo(File output) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject root = new JsonObject();
                root.addProperty("data-version", PlayerData.DATA_VERSION);
                root.addProperty("created-at", Instant.now().toString());
                root.add("players", gson.toJsonTree(readAll("ccs_players", "uuid")));
                root.add("parties", gson.toJsonTree(readAll("ccs_parties", "id")));
                Files.writeString(output.toPath(), gson.toJson(root), StandardCharsets.UTF_8);
                return output;
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, ioExecutor);
    }

    public CompletableFuture<Void> restoreExport(File input) {
        return CompletableFuture.runAsync(() -> {
            try (FileReader reader = new FileReader(input, StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                int version = root.get("data-version").getAsInt();
                if (version > PlayerData.DATA_VERSION) throw new FutureDataVersionException("Backup uses future data-version " + version);
                Map<String, String> players = gson.fromJson(root.get("players"), Map.class);
                Map<String, String> parties = gson.fromJson(root.get("parties"), Map.class);
                clearTable("ccs_players");
                clearTable("ccs_parties");
                for (var row : players.entrySet()) writePayload("ccs_players", "uuid", row.getKey(), version, row.getValue());
                for (var row : parties.entrySet()) writePayload("ccs_parties", "id", row.getKey(), version, row.getValue());
                flushFlatFile();
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, ioExecutor);
    }

    private String readPayload(String table, String keyColumn, String key) throws Exception {
        if (isFlat()) return (table.equals("ccs_players") ? flatPlayers : flatParties).get(key);
        synchronized (connectionLock) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT data_version, payload FROM " + table + " WHERE " + keyColumn + " = ?")) {
                statement.setString(1, key);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    if (result.getInt(1) > PlayerData.DATA_VERSION) throw new FutureDataVersionException("Stored row uses future data-version");
                    return result.getString(2);
                }
            }
        }
    }

    private Map<String, String> readAll(String table, String keyColumn) throws Exception {
        if (isFlat()) return Map.copyOf(table.equals("ccs_players") ? flatPlayers : flatParties);
        synchronized (connectionLock) {
            Map<String, String> result = new LinkedHashMap<>();
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT " + keyColumn + ", data_version, payload FROM " + table)) {
                while (rows.next()) {
                    if (rows.getInt(2) > PlayerData.DATA_VERSION) throw new FutureDataVersionException("Stored row uses future data-version");
                    result.put(rows.getString(1), rows.getString(3));
                }
            }
            return result;
        }
    }

    private void writePayload(String table, String keyColumn, String key, int version, String payload) throws Exception {
        if (isFlat()) {
            (table.equals("ccs_players") ? flatPlayers : flatParties).put(key, payload);
            return;
        }
        synchronized (connectionLock) {
            String sql = backend == Backend.SQLITE
                    ? "INSERT INTO " + table + " (" + keyColumn + ", data_version, payload, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(" + keyColumn + ") DO UPDATE SET data_version=excluded.data_version,payload=excluded.payload,updated_at=excluded.updated_at"
                    : "INSERT INTO " + table + " (" + keyColumn + ", data_version, payload, updated_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE data_version=VALUES(data_version),payload=VALUES(payload),updated_at=VALUES(updated_at)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                statement.setInt(2, version);
                statement.setString(3, payload);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            }
        }
    }

    private void deleteMissingParties(Set<String> live) throws Exception {
        if (isFlat()) { flatParties.keySet().retainAll(live); return; }
        synchronized (connectionLock) {
            if (live.isEmpty()) { clearTable("ccs_parties"); return; }
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id FROM ccs_parties")) {
                List<String> stale = new ArrayList<>();
                while (rows.next()) if (!live.contains(rows.getString(1))) stale.add(rows.getString(1));
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM ccs_parties WHERE id = ?")) {
                    for (String id : stale) { delete.setString(1, id); delete.addBatch(); }
                    delete.executeBatch();
                }
            }
        }
    }

    private void clearTable(String table) throws Exception {
        if (isFlat()) { (table.equals("ccs_players") ? flatPlayers : flatParties).clear(); return; }
        synchronized (connectionLock) { try (Statement statement = connection.createStatement()) { statement.executeUpdate("DELETE FROM " + table); } }
    }

    private boolean isFlat() { return backend == Backend.YAML || backend == Backend.JSON; }

    private void loadFlatFile() throws IOException {
        if (!flatFile.exists()) return;
        if (backend == Backend.JSON) {
            try (FileReader reader = new FileReader(flatFile, StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root == null) return;
                if (root.has("data-version") && root.get("data-version").getAsInt() > PlayerData.DATA_VERSION) throw new FutureDataVersionException("Flat data uses a future version");
                if (root.has("players")) flatPlayers.putAll(gson.fromJson(root.get("players"), Map.class));
                if (root.has("parties")) flatParties.putAll(gson.fromJson(root.get("parties"), Map.class));
            }
        } else {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(flatFile);
            if (yaml.getInt("data-version", 1) > PlayerData.DATA_VERSION) throw new FutureDataVersionException("YAML data uses a future version");
            ConfigurationCopy.copySection(yaml, "players", flatPlayers);
            ConfigurationCopy.copySection(yaml, "parties", flatParties);
        }
    }

    private void flushFlatFile() throws IOException {
        if (!isFlat()) return;
        if (backend == Backend.JSON) {
            JsonObject root = new JsonObject();
            root.addProperty("data-version", PlayerData.DATA_VERSION);
            root.add("players", gson.toJsonTree(flatPlayers));
            root.add("parties", gson.toJsonTree(flatParties));
            try (FileWriter writer = new FileWriter(flatFile, StandardCharsets.UTF_8)) { gson.toJson(root, writer); }
        } else {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("data-version", PlayerData.DATA_VERSION);
            flatPlayers.forEach((key, value) -> yaml.set("players." + key, value));
            flatParties.forEach((key, value) -> yaml.set("parties." + key, value));
            yaml.save(flatFile);
        }
    }

    public Backend backend() { return backend; }
    public PlayerData snapshot(PlayerData data) { return gson.fromJson(gson.toJson(data), PlayerData.class).normalize(); }
    public long lastSuccessfulSave() { return lastSuccessfulSave; }
    public int pendingSaveCount() { return pendingPlayers.size(); }

    public void reportFailure(Throwable throwable, Consumer<String> adminNotifier) {
        plugin.getLogger().log(Level.SEVERE, "CCS data save failed; data remains in memory and will be retried", throwable);
        adminNotifier.accept("<red>CCSデータ保存に失敗しました。30秒後に再試行します。</red>");
    }

    @Override public void close() {
        ioExecutor.shutdown();
        try { ioExecutor.awaitTermination(15, TimeUnit.SECONDS); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        synchronized (connectionLock) {
            if (connection != null) try { connection.close(); } catch (SQLException ex) { plugin.getLogger().log(Level.WARNING, "Could not close database", ex); }
        }
    }

    private static final class ConfigurationCopy {
        static void copySection(YamlConfiguration yaml, String path, Map<String, String> target) {
            var section = yaml.getConfigurationSection(path);
            if (section != null) for (String key : section.getKeys(false)) target.put(key, section.getString(key, ""));
        }
    }

    public static final class FutureDataVersionException extends RuntimeException {
        public FutureDataVersionException(String message) { super(message); }
    }
}
