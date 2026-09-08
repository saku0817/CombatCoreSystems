package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.RegionDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class RegionService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();

    public RegionService(JavaPlugin plugin, DefinitionRegistry definitions) { this.plugin = plugin; this.definitions = definitions; }

    public Optional<RegionDefinition> at(Location location) {
        return definitions.snapshot().regions().values().stream().filter(region -> region.contains(location))
                .min(Comparator.comparingLong(RegionDefinition::volume));
    }

    public Optional<String> flag(Location location, String flag) { return at(location).map(region -> region.flags().get(flag)); }

    public ItemStack wand() {
        Material material = Material.matchMaterial(definitions.snapshot().config("config.yml").getString("region-wand.material", "WOODEN_AXE"));
        ItemStack item = new ItemStack(material == null ? Material.WOODEN_AXE : material);
        ItemMeta meta = item.getItemMeta();
        meta.itemName(mini.deserialize("<gold>CCS Region Wand</gold>"));
        meta.lore(List.of(mini.deserialize("<gray>左クリック: Pos1 / 右クリック: Pos2</gray>")));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWand(PlayerInteractEvent event) {
        if (!event.getPlayer().hasPermission("combatcoresystems.admin.region") || event.getClickedBlock() == null) return;
        if (!isWand(event.getItem(), wand())) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        boolean first = event.getAction() == Action.LEFT_CLICK_BLOCK;
        select(event.getPlayer(), first, event.getClickedBlock().getLocation());
    }

    static boolean isWand(ItemStack item, ItemStack wand) {
        return item != null && item.isSimilar(wand);
    }

    public void select(Player player, boolean first, Location location) {
        Selection old = selections.getOrDefault(player.getUniqueId(), new Selection(null, null));
        selections.put(player.getUniqueId(), first ? new Selection(location, old.pos2) : new Selection(old.pos1, location));
        player.sendMessage(mini.deserialize("<green>Pos" + (first ? 1 : 2) + ": " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "</green>"));
    }

    public boolean create(Player player, String id, boolean fullHeight) {
        if (!id.matches("[a-z0-9_-]+") || definitions.snapshot().regions().containsKey(id)) return false;
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || selection.pos1 == null || selection.pos2 == null || selection.pos1.getWorld() != selection.pos2.getWorld()) return false;
        File file = new File(plugin.getDataFolder(), "regions.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String root = "regions." + id;
        yaml.set(root + ".world", selection.pos1.getWorld().getName());
        yaml.set(root + ".full-height", fullHeight);
        writeLocation(yaml, root + ".pos1", selection.pos1);
        writeLocation(yaml, root + ".pos2", selection.pos2);
        return saveAndReload(yaml, file);
    }

    public boolean delete(String id) {
        if (!definitions.snapshot().regions().containsKey(id)) return false;
        File file = new File(plugin.getDataFolder(), "regions.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("regions." + id, null);
        return saveAndReload(yaml, file);
    }

    public boolean setFlag(String id, String flag, String value) {
        if (!definitions.snapshot().regions().containsKey(id)) return false;
        File file = new File(plugin.getDataFolder(), "regions.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("regions." + id + ".flags." + flag, value);
        return saveAndReload(yaml, file);
    }

    private boolean saveAndReload(YamlConfiguration yaml, File file) {
        try { yaml.save(file); return definitions.reloadSafely(); }
        catch (IOException ex) { plugin.getLogger().warning("Could not save regions.yml: " + ex.getMessage()); return false; }
    }

    private void writeLocation(YamlConfiguration yaml, String path, Location location) {
        yaml.set(path + ".x", location.getBlockX()); yaml.set(path + ".y", location.getBlockY()); yaml.set(path + ".z", location.getBlockZ());
    }

    public Optional<Selection> selection(UUID player) { return Optional.ofNullable(selections.get(player)); }
    public record Selection(Location pos1, Location pos2) {}
}
