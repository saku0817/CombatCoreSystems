package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Authenticated editor designed for a rental host's HTTPS reverse proxy. */
public final class AdminWebService {
    private static final int MAX_BODY = 2 * 1024 * 1024;
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final Gson gson = new Gson();
    private HttpServer server;
    private ExecutorService executor;
    private String page;

    public AdminWebService(JavaPlugin plugin, DefinitionRegistry definitions) { this.plugin = plugin; this.definitions = definitions; }

    public void start() {
        var config = definitions.snapshot().config("config.yml");
        if (!config.getBoolean("web-editor.enabled", false)) return;
        String token = config.getString("web-editor.token", "");
        if (token.length() < 16 || token.equals("CHANGE_ME")) {
            plugin.getLogger().severe("Web editor was not started: token must be at least 16 characters and not CHANGE_ME."); return;
        }
        try (InputStream input = plugin.getResource("web-editor.html")) {
            if (input == null) throw new IOException("web-editor.html is missing");
            page = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String bind = config.getString("web-editor.bind", "127.0.0.1");
            int port = Math.clamp(config.getInt("web-editor.port", 8765), 1, 65535);
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
            int threads = Math.clamp(config.getInt("web-editor.threads", 2), 1, 8);
            executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), runnable -> {
                Thread thread = new Thread(runnable, "ccs-web-editor"); thread.setDaemon(true); return thread;
            });
            server.setExecutor(executor); server.createContext("/", this::handle); server.start();
            plugin.getLogger().info("CCS web editor listening on " + bind + ":" + port + ". Use an HTTPS reverse proxy for public access.");
        } catch (IOException ex) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not start CCS web editor", ex); }
    }

    public void stop() { if (server != null) server.stop(1); if (executor != null) executor.shutdownNow(); }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            securityHeaders(exchange);
            if (!authenticated(exchange)) {
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"CombatCoreSystems\"");
                send(exchange, 401, "text/plain; charset=utf-8", "Authentication required"); return;
            }
            String path = exchange.getRequestURI().getPath(), method = exchange.getRequestMethod();
            if (path.equals("/") && method.equals("GET")) send(exchange, 200, "text/html; charset=utf-8", page);
            else if (path.equals("/api/summary") && method.equals("GET")) summary(exchange);
            else if (path.equals("/api/file") && method.equals("GET")) readFile(exchange);
            else if (path.equals("/api/file") && method.equals("POST")) writeFile(exchange);
            else if (path.equals("/api/document") && method.equals("POST")) inspectDocument(exchange);
            else send(exchange, 404, "application/json", "{\"error\":\"not_found\"}");
        } catch (Exception ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Web editor request failed", ex);
            try { send(exchange, 500, "application/json", "{\"error\":\"internal_error\"}"); } catch (IOException ignored) { }
        } finally { exchange.close(); }
    }

    private boolean authenticated(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Basic ")) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':'); if (separator < 0) return false;
            byte[] supplied = decoded.substring(separator + 1).getBytes(StandardCharsets.UTF_8);
            byte[] expected = definitions.snapshot().config("config.yml").getString("web-editor.token", "").getBytes(StandardCharsets.UTF_8);
            return expected.length >= 16 && MessageDigest.isEqual(supplied, expected);
        } catch (RuntimeException ex) { return false; }
    }

    private void summary(HttpExchange exchange) throws Exception {
        var snap = definitions.snapshot(); Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", plugin.getPluginMeta().getVersion());
        result.put("onlinePlayers", Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.getOnlinePlayers().size()).get());
        result.put("globalMultiplier", snap.config("config.yml").getDouble("damage.global-multiplier", 2.0));
        result.put("weapons", snap.weapons().keySet()); result.put("equipment", snap.equipment().keySet());
        result.put("mobs", snap.mobs().keySet()); result.put("bosses", snap.bosses().keySet());
        result.put("buffs", snap.buffs().keySet()); result.put("reactions", snap.reactions().keySet()); result.put("files", DefinitionRegistry.FILES);
        send(exchange, 200, "application/json; charset=utf-8", gson.toJson(result));
    }

    private void readFile(HttpExchange exchange) throws IOException {
        String name = requestedFile(exchange); if (name == null) { invalidFile(exchange); return; }
        byte[] data = Files.readAllBytes(new File(plugin.getDataFolder(), name).toPath());
        exchange.getResponseHeaders().set("ETag", fingerprint(data));
        send(exchange, 200, "text/yaml; charset=utf-8", new String(data, StandardCharsets.UTF_8));
    }

    private String yamlBody(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("text/yaml")) {
            send(exchange, 415, "application/json", "{\"error\":\"text_yaml_required\"}"); return null;
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
        if (body.length > MAX_BODY) { send(exchange, 413, "application/json", "{\"error\":\"too_large\"}"); return null; }
        return new String(body, StandardCharsets.UTF_8);
    }

    private void inspectDocument(HttpExchange exchange) throws Exception {
        String file = requestedFile(exchange); if (file == null) { invalidFile(exchange); return; }
        String body = yamlBody(exchange); if (body == null) return;
        try {
            YamlConfiguration yaml = new YamlConfiguration(); yaml.loadFromString(body);
            String id = query(exchange, "newId");
            if (id != null) WebEditorSupport.addEntry(yaml, file, id, query(exchange, "root"));
            send(exchange, 200, "application/json; charset=utf-8", gson.toJson(Map.of("tree", WebEditorSupport.tree(yaml), "yaml", id == null ? body : yaml.saveToString())));
        } catch (Exception ex) { send(exchange, 422, "application/json; charset=utf-8", gson.toJson(Map.of("error", String.valueOf(ex.getMessage())))); }
    }

    private void writeFile(HttpExchange exchange) throws Exception {
        String name = requestedFile(exchange); if (name == null) { invalidFile(exchange); return; }
        String yaml = yamlBody(exchange); if (yaml == null) return;
        YamlConfiguration parsed = new YamlConfiguration();
        try { parsed.loadFromString(yaml); }
        catch (Exception ex) { send(exchange, 422, "application/json", gson.toJson(Map.of("error", "invalid_yaml", "message", String.valueOf(ex.getMessage())))); return; }
        if (parsed.getInt("data-version", -1) < 1) { send(exchange, 422, "application/json", "{\"error\":\"data_version_required\"}"); return; }
        if (name.equals("config.yml") && parsed.getBoolean("web-editor.enabled") && parsed.getString("web-editor.token", "").length() < 16) {
            send(exchange, 422, "application/json", "{\"error\":\"token_too_short\"}"); return;
        }
        String expected = exchange.getRequestHeaders().getFirst("If-Match");
        if (expected == null) { send(exchange, 428, "application/json", "{\"error\":\"reload_file_before_save\"}"); return; }
        // Serialize the complete transaction with command reloads on the server thread.
        // Do not time out a queued reload and later apply an edit already reported as failed.
        SaveResult result = Bukkit.getScheduler().callSyncMethod(plugin, () -> saveTransaction(name, yaml, expected)).get();
        if (result.etag() != null) exchange.getResponseHeaders().set("ETag", result.etag());
        send(exchange, result.code(), "application/json; charset=utf-8", gson.toJson(result.body()));
    }

    private record SaveResult(int code, Map<String, Object> body, String etag) {}

    private SaveResult saveTransaction(String name, String yaml, String expected) throws Exception {
        Path target = new File(plugin.getDataFolder(), name).toPath();
        if (!fingerprint(Files.readAllBytes(target)).equals(expected))
            return new SaveResult(409, Map.of("error", "他の画面またはファイル管理で変更されています。再読込して差分を確認してください。"), null);
        Path backupDir = new File(plugin.getDataFolder(), "backups/web-editor/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID()).toPath();
        Files.createDirectories(backupDir); Path backup = backupDir.resolve(name); Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        Path temporary = Files.createTempFile(target.getParent(), name, ".tmp");
        boolean replaced = false;
        try {
            Files.writeString(temporary, yaml, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            replaced = true;
            if (!definitions.reloadSafely(false)) {
                Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                return new SaveResult(422, Map.of("error", "定義の検証に失敗したため復元しました。サーバーログを確認してください。", "backup", backup.toString()), expected);
            }
            return new SaveResult(200, Map.of("ok", true, "backup", backup.toString()), fingerprint(Files.readAllBytes(target)));
        } catch (Exception ex) {
            if (replaced) {
                Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                definitions.reloadSafely(false);
            }
            throw ex;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String fingerprint(byte[] data) {
        try { return "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)) + "\""; }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private String requestedFile(HttpExchange exchange) {
        String name = query(exchange, "name");
        return DefinitionRegistry.FILES.contains(name == null ? "" : name) ? name : null;
    }

    private String query(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getRawQuery(); if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] values = pair.split("=", 2);
            if (values.length == 2 && values[0].equals(key)) return URLDecoder.decode(values[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    private void invalidFile(HttpExchange exchange) throws IOException { send(exchange, 400, "application/json", "{\"error\":\"invalid_file\"}"); }
    private void securityHeaders(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders(); headers.set("X-Content-Type-Options", "nosniff"); headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer"); headers.set("Cache-Control", "no-store");
        headers.set("Content-Security-Policy", "default-src 'self'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'");
    }
    private void send(HttpExchange exchange, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(code, bytes.length); try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
