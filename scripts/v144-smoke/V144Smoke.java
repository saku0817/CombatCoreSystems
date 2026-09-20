package ccs.test;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.model.EquipmentSlot;
import com.github.saku0817.combatcoresystems.service.*;
import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Isolated Paper integration fixture. Never install on a production server. */
public final class V144Smoke extends JavaPlugin {
    private int checks;
    @Override public void onEnable() {
        if (!Bukkit.getIp().equals("127.0.0.1") || Bukkit.getPort() != 25567) throw new IllegalStateException("Only isolated loopback smoke server allowed");
        Bukkit.getScheduler().runTask(this, () -> {
            try { runChecks(); getLogger().info("V144_SMOKE_PASS: " + checks + " assertions (Paper items + simulated inventory/player; no real clients)"); }
            catch (Throwable error) { getLogger().log(java.util.logging.Level.SEVERE, "V144_SMOKE_FAIL", error); }
        });
    }
    private Object field(Object target, String name) throws Exception { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
    private void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") private void runChecks() throws Exception {
        JavaPlugin ccs = (JavaPlugin) Bukkit.getPluginManager().getPlugin("CombatCoreSystems");
        var base = ((DefinitionRegistry) field(ccs, "definitions")).snapshot();
        Map<String, YamlConfiguration> yaml = new HashMap<>(base.yaml());
        var gear = new YamlConfiguration(); gear.loadFromString("""
            equipment:
              chest:
                material: DIAMOND_CHESTPLATE
                slot: CHEST
                rarity: 5
                max-level: 15
                main-stat-candidates:
                  CRIT_RATE: {level-1: 0.05, max-level: 0.30}
                  CRIT_DAMAGE: {level-1: 0.10, max-level: 0.60}
                substat-candidates:
                  CRIT_RATE: {value: 0.05}
                  CRIT_DAMAGE: {value: 0.10}
                  ATK_PERCENT: {value: 0.05}
                  HP_FLAT: {value: 25}
                set: series
              echo:
                material: BLAZE_POWDER
                slot: RESONANCE
                rarity: 5
                main-stat: {type: FIRE_DAMAGE, level-1: 0.05, max-level: 0.20}
                substats: [ATK_PERCENT, CRIT_RATE]
                set: series
            """);
        var sets = new YamlConfiguration(); sets.loadFromString("""
            sets:
              series:
                name: test series
                two-piece:
                  modifiers: {ATK_PERCENT: 0.10}
                  triggers:
                    skill: {event: SKILL, effects: [power], cooldown-seconds: 60}
                    low: {event: HP_BELOW, hp-percent: 0.30, effects: [sunset], cooldown-seconds: 0}
            """);
        yaml.put("equipment.yml", gear); yaml.put("sets.yml", sets);
        DefinitionRegistry definitions = new DefinitionRegistry(ccs);
        Method parse = DefinitionRegistry.class.getDeclaredMethod("parseEquipment", YamlConfiguration.class, List.class, List.class); parse.setAccessible(true);
        List<String> errors = new ArrayList<>();
        var gearDefinitions = (Map<String, EquipmentDefinition>) parse.invoke(definitions, gear, errors, new ArrayList<String>());
        check(errors.isEmpty(), "equipment parser: " + errors);
        var buffDefinitions = new HashMap<>(base.buffs());
        buffDefinitions.put("power", new BuffDefinition("power", BuffDefinition.Kind.BUFF, BuffDefinition.Target.SELF, 10, false, 3,
                BuffDefinition.Reapply.STACK, Map.of(), Map.of(StatKey.ATK_PERCENT, .2), null));
        var snapshot = new DefinitionRegistry.Snapshot(Map.copyOf(yaml), base.reactions(), base.weapons(), gearDefinitions, Map.copyOf(buffDefinitions), base.mobs(), base.vanillaMobs(), base.bosses(), base.regions(), base.weaponStages());
        ((AtomicReference<DefinitionRegistry.Snapshot>) field(definitions, "current")).set(snapshot);
        var players = new PlayerDataService(ccs, null, definitions);
        UUID uuid = UUID.randomUUID(); PlayerData data = new PlayerData(uuid, "SmokeFixture");
        ((Map<UUID, PlayerData>) field(players, "loaded")).put(uuid, data);
        ItemStack[] slots = new ItemStack[41]; Inventory[] opened = new Inventory[1];
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(getClassLoader(), new Class[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getContents" -> slots.clone();
            case "getStorageContents" -> Arrays.copyOf(slots, 36);
            case "getItem" -> slots[(Integer) args[0]];
            case "setItem" -> { slots[(Integer) args[0]] = (ItemStack) args[1]; yield null; }
            case "getHelmet" -> slots[39]; case "getChestplate" -> slots[38]; case "getLeggings" -> slots[37]; case "getBoots" -> slots[36];
            case "getItemInMainHand" -> slots[0] == null ? new ItemStack(Material.AIR) : slots[0];
            case "getItemInOffHand" -> slots[40] == null ? new ItemStack(Material.AIR) : slots[40];
            case "getHeldItemSlot" -> 0; case "getSize" -> 41;
            case "firstEmpty" -> { int free = -1; for (int i = 0; i < 36; i++) if (slots[i] == null || slots[i].getType().isAir()) { free = i; break; } yield free; }
            case "toString" -> "SmokeInventory";
            default -> primitive(method.getReturnType());
        });
        Player player = (Player) Proxy.newProxyInstance(getClassLoader(), new Class[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid; case "getName", "toString" -> "SmokeFixture";
            case "getInventory" -> inventory; case "isOnline", "hasPermission" -> true;
            case "getWorld" -> Bukkit.getWorlds().getFirst(); case "getLocation" -> Bukkit.getWorlds().getFirst().getSpawnLocation();
            case "getHealth", "getMaxHealth" -> 20.0; case "getGameMode" -> GameMode.SURVIVAL;
            case "openInventory" -> { opened[0] = (Inventory) args[0]; yield null; }
            case "equals" -> proxy == args[0]; case "hashCode" -> uuid.hashCode();
            default -> primitive(method.getReturnType());
        });
        var items = new ItemService(ccs, definitions); var combat = new CombatStateService(ccs, 10000);
        var stats = new StatService(ccs, definitions, combat, items);
        var equipment = new EquipmentService(ccs, definitions, players, stats, combat, items);
        for (int i = 0; i < 30; i++) {
            ItemInstance instance = items.instance(items.create("chest", 1).orElseThrow()).orElseThrow();
            check(instance.getSubstats().size() == 4, "all four unique substats selected");
            check(instance.getSubstats().containsKey(instance.mainStat(gearDefinitions.get("chest")).name()), "main/sub overlap allowed");
        }
        slots[0] = items.create("chest", 1).orElseThrow(); check(equipment.equip(player, 0), "equip chest");
        check(slots[0] == null && slots[38] != null, "chest moves to armor slot");
        slots[1] = items.create("echo", 1).orElseThrow(); String firstId = items.instance(slots[1]).orElseThrow().getInstanceId();
        check(equipment.equip(player, 1), "equip resonance"); check(slots[1] == null && !data.getResonanceItem().isBlank(), "no ghost item in inventory");
        equipment.syncArmor(player); check(data.getEquipment().get(EquipmentSlot.RESONANCE).getInstanceId().equals(firstId), "sync retains stored resonance");
        slots[2] = items.create("echo", 1).orElseThrow(); String secondId = items.instance(slots[2]).orElseThrow().getInstanceId();
        check(equipment.equip(player, 2), "swap resonance"); check(items.instance(slots[2]).orElseThrow().getInstanceId().equals(firstId), "returns old exact instance");
        check(SetEffectService.counts(snapshot, data).get("series") == 2, "armor plus resonance counts as two");
        for (int i = 0; i < 36; i++) if (slots[i] == null) slots[i] = new ItemStack(Material.STONE);
        check(!equipment.unequip(player, EquipmentSlot.RESONANCE), "full inventory refuses unequip");
        check(data.getEquipment().get(EquipmentSlot.RESONANCE).getInstanceId().equals(secondId), "failed unequip retains item");
        slots[3] = null; check(equipment.unequip(player, EquipmentSlot.RESONANCE), "unequip when space exists");
        check(items.instance(slots[3]).orElseThrow().getInstanceId().equals(secondId) && data.getResonanceItem().isBlank(), "exact stored item returned once");
        data.getEquipment().put(EquipmentSlot.RESONANCE, items.instance(slots[2]).orElseThrow()); equipment.syncArmor(player);
        check(slots[2] == null && !data.getResonanceItem().isBlank(), "legacy registration migrated");
        PlayerData copy = new Gson().fromJson(new Gson().toJson(data), PlayerData.class).normalize();
        check(items.instance(equipment.storedResonance(copy)).orElseThrow().getInstanceId().equals(firstId), "resonance survives player JSON round trip");
        var enhancement = new EnhancementService(definitions, players, stats, items);
        String material = enhancement.materials("EQUIPMENT").getFirst().id(); data.getEnhancementMaterials().put(material, 100L);
        check(enhancement.enhanceItem(player, firstId, Map.of(material, 100L)).success(), "enhance equipped resonance");
        check(items.instance(equipment.storedResonance(data)).orElseThrow().getLevel() > 1, "enhanced level visible in stored slot");
        var buffs = new BuffService(ccs, definitions, players, stats, null, null);
        var triggers = new SetEffectService(ccs, definitions, players, stats, buffs);
        triggers.fire(player, SetTrigger.Event.SKILL, null); triggers.fire(player, SetTrigger.Event.SKILL, null);
        check(data.getBuffs().size() == 1 && data.getBuffs().getFirst().getStacks() == 1, "skill trigger applies once under cooldown");
        data.setHealth(1); triggers.fire(player, SetTrigger.Event.HP_BELOW, null); check(data.getDebuffs().size() == 1, "HP threshold triggers");
        data.getDebuffs().clear(); triggers.fire(player, SetTrigger.Event.HP_BELOW, null); check(data.getDebuffs().isEmpty(), "low HP is edge-triggered not repeated");
        data.setHealth(100); triggers.fire(player, SetTrigger.Event.HP_BELOW, null); data.setHealth(1); triggers.fire(player, SetTrigger.Event.HP_BELOW, null);
        check(data.getDebuffs().size() == 1, "HP reentry triggers again");
        var detailed = stats.describe(player, data); check(detailed.sources().containsKey("装備から"), "equipment source summarized");
        check(detailed.sources().containsKey("セット効果：test series"), "set source separate");
        var gui = new GuiService(ccs, definitions, players, stats, new LevelService(definitions, stats), combat, equipment, null, null, enhancement, items);
        gui.openEquipment(player); check(opened[0].getItem(30).getType() == Material.BLAZE_POWDER, "equipped resonance GUI icon");
        gui.openStats(player); Material[] dyes = {Material.RED_DYE, Material.CYAN_DYE, Material.LIME_DYE, Material.PURPLE_DYE, Material.LIGHT_BLUE_DYE};
        for (int i = 0; i < dyes.length; i++) check(opened[0].getItem(38 + i).getType() == dyes[i], "attribute dye order " + i);
        var admin = new AdminGuiService(ccs, definitions, players, items, equipment, stats, new LevelService(definitions, stats));
        admin.open(player); check(opened[0].getSize() == 54 && opened[0].getItem(10).getType() == Material.CHEST, "admin chest menu builds");
    }
    private static Object primitive(Class<?> type) {
        if (type == boolean.class) return false; if (type == int.class) return 0; if (type == long.class) return 0L;
        if (type == double.class) return 0.0; if (type == float.class) return 0F; if (type == short.class) return (short) 0; if (type == byte.class) return (byte) 0;
        return null;
    }
}
