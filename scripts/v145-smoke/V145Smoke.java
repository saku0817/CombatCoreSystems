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
public final class V145Smoke extends JavaPlugin {
    private int checks;
    @Override public void onEnable() {
        if (!Bukkit.getIp().equals("127.0.0.1") || Bukkit.getPort() != 25568) throw new IllegalStateException("Only isolated loopback smoke server allowed");
        Bukkit.getScheduler().runTask(this, () -> {
            try { runChecks(); fieldChecks(); }
            catch (Throwable error) { getLogger().log(java.util.logging.Level.SEVERE, "V145_SMOKE_FAIL", error); }
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
        triggerChecks(player,data,slots,definitions,players,items,stats,combat);
    }
    @SuppressWarnings("unchecked") private void triggerChecks(Player player, PlayerData data, ItemStack[] slots,
            DefinitionRegistry definitions, PlayerDataService players, ItemService items, StatService stats,
            CombatStateService combat) throws Exception {
        var old=definitions.snapshot();
        var yaml=new HashMap<>(old.yaml());
        var weapons=new YamlConfiguration(); weapons.loadFromString(old.config("weapons.yml").saveToString());
        var extension=new YamlConfiguration(); extension.loadFromString("""
            triggers:
              mark:
                event: NORMAL_ATTACK
                actions: [{type: ADD_STACK, id: aim, scope: TARGET, amount: 1, max: 5}]
              aimed:
                event: BEFORE_HIT
                conditions: {stack: {id: aim, scope: EVENT_TARGET, min: 1}}
                actions:
                  - type: MODIFY_EVENT_STATS
                    modifiers: {percent: {CRIT_RATE: 0.20, ATK_PERCENT: 0.50}}
              ready:
                event: STACK_REACHED
                conditions: {stack: {id: aim, scope: EVENT_TARGET, min: 2}}
                actions: [{type: APPLY_EFFECT, effect: power}]
              clear:
                event: ULTIMATE
                actions:
                  - {type: CLEAR_STACK, id: aim, scope: ALL_TARGETS, store-result: consumed}
                  - type: APPLY_EFFECT
                    effect: power
                    conditions: {context-value: {key: consumed, min: 2}}
              overheal:
                event: OVERHEAL
                actions: [{type: APPLY_DYNAMIC_MODIFIER, stat: ATK_FLAT, source: EVENT_OVERHEAL, multiplier: 0.10, duration: 10}]
              low:
                event: HP_BELOW
                hp-percent: 0.5
                actions: [{type: APPLY_EFFECT, effect: power}]
              high:
                event: HP_ABOVE
                hp-percent: 0.5
                actions: [{type: REMOVE_EFFECT, effect: power}]
              field:
                event: SKILL
                actions:
                  - type: CREATE_FIELD
                    id: fixture
                    duration: 10
                    area: {shape: SPHERE, radius: 5}
                    enemy-effects: [power]
                    effect-mode: WHILE_INSIDE
            """);
        weapons.set("weapons.guiding_star.triggers",extension.getConfigurationSection("triggers"));
        yaml.put("weapons.yml",weapons);
        var buffYaml=new YamlConfiguration(); buffYaml.loadFromString(old.config("buffs.yml").saveToString());
        buffYaml.set("buffs.power.modifiers.override.CRIT_RATE",1.0); yaml.put("buffs.yml",buffYaml);
        var snap=new DefinitionRegistry.Snapshot(Map.copyOf(yaml),old.reactions(),old.weapons(),old.equipment(),old.buffs(),
                old.mobs(),old.vanillaMobs(),old.bosses(),old.regions(),old.weaponStages());
        ((AtomicReference<DefinitionRegistry.Snapshot>)field(definitions,"current")).set(snap);
        Arrays.fill(slots,null); slots[0]=items.create("guiding_star",1).orElseThrow();
        data.getEquipment().clear(); data.getBuffs().clear(); data.getDebuffs().clear(); stats.invalidate(player.getUniqueId());
        var levels=new LevelService(definitions,stats);
        var mobs=new MobService(this,definitions);
        var displays=new DamageDisplayService(this,definitions);
        displays.start();
        var party=new PartyService(null,players);
        var damage=new DamageService(this,definitions,players,stats,combat,new ElementService(this,definitions),mobs,party,displays,new RegionService(this,definitions),levels,items);
        var healing=new HealService(players,stats,displays,levels,mobs);
        var buffs=new BuffService(this,definitions,players,stats,damage,healing);
        var runtime=new TriggerService(this,definitions,players,items,stats,combat,damage,healing,buffs,party,mobs);
        stats.bindEffects(buffs,runtime.dynamic()); damage.bindBuffs(buffs); damage.bindTriggers(runtime); healing.bindTriggers(runtime);
        buffs.bindEvents((target,id)->runtime.buffEvent(target,id,true),(target,id)->runtime.buffEvent(target,id,false));
        var target=player.getWorld().spawn(player.getLocation().add(1,0,0),org.bukkit.entity.Cow.class);
        target.setAI(false);
        try {
            var hit=new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.NORMAL_ATTACK,null,player,target);
            hit.normalAttack=true; hit.finalDamage=10; runtime.emit(hit);
            var stackService=(StackService)field(runtime,"stacks");
            var key=new StackService.Key(player.getUniqueId(),"weapon:guiding_star","aim",target.getUniqueId());
            check(stackService.count(key)==1,"generic normal attack stack");
            var before=new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.BEFORE_HIT,null,player,target);
            before.normalAttack=true; runtime.emit(before);
            check(Math.abs(before.sourceModifiers.advanced(StatKey.CRIT_RATE,.1)-.3)<1e-9,"target-local critical bonus");
            check(Math.abs(before.sourceModifiers.primary(StatKey.ATK_FLAT,StatKey.ATK_PERCENT,100)-150)<1e-9,"target-local attack bonus");
            var other=new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.BEFORE_HIT,null,player,player);
            runtime.emit(other); check(other.sourceModifiers.primary(StatKey.ATK_FLAT,StatKey.ATK_PERCENT,100)==100,"modifier does not leak to other targets");
            var second=new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.NORMAL_ATTACK,null,player,target);
            second.normalAttack=true; runtime.emit(second);
            check(stackService.count(key)==2,"second stack count, actual="+stackService.count(key));
            check(buffs.has(player,"power"),"stack reached applies buff through nested event; buffs="+data.getBuffs().stream().map(TimedEffect::getId).toList()+" targetResolvable="+(Bukkit.getEntity(target.getUniqueId())!=null));
            check(stats.get(player,data).value(StatKey.CRIT_RATE)==1,"buff override fixes final critical rate to 100 percent");
            buffs.remove(player,"power");
            check(stats.get(player,data).value(StatKey.CRIT_RATE)<1,"removing override restores original critical rate");
            var clear=new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.ULTIMATE,null,player,target);
            runtime.emit(clear); check(stackService.count(key)==0 && ((Number)clear.values.get("consumed")).intValue()==2,"all-target consumed result");
            check(buffs.has(player,"power"),"action branches on stored consumption");
            runtime.healed(player,player,150,50,100);
            check(runtime.dynamic().modifiers(player.getUniqueId()).get(StatKey.ATK_FLAT)==10,"overheal snapshots dynamic attack");
            buffs.remove(player,"power"); data.setHealth(1);
            runtime.emit(new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.HP_BELOW,null,player,null));
            check(buffs.has(player,"power"),"generic low HP");
            buffs.remove(player,"power");
            runtime.emit(new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.HP_BELOW,null,player,null));
            check(!buffs.has(player,"power"),"low HP edge not continuously repeated");
            data.setHealth(10000); buffs.apply(player,"power",player.getUniqueId());
            runtime.emit(new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.HP_ABOVE,null,player,null));
            check(!buffs.has(player,"power"),"HP_ABOVE removes buff");
            double maxHp=stats.get(player,data).maxHp(); data.setHealth(maxHp-5);
            check(healing.healAmount(player,player,100,false)==100,"common healing requested amount");
            check(data.getHealth()==maxHp,"common healing caps actual HP");
            check(runtime.dynamic().modifiers(player.getUniqueId()).get(StatKey.ATK_FLAT)==9.5,"common healing computes 95 overheal then dynamic modifier");
            // Real entity field ownership: removing a lease must retain the ordinary copy.
            buffs.apply(target,"power",player.getUniqueId()); buffs.acquire(target,"fixture-lease","power",player.getUniqueId());
            check(buffs.effects(target).size()==2,"owned field effect coexists with ordinary buff");
            buffs.release(target.getUniqueId(),"fixture-lease"); check(buffs.effects(target).size()==1,"field exit preserves ordinary buff");
        } finally { target.remove(); }
    }
    private static Object primitive(Class<?> type) {
        if (type == boolean.class) return false; if (type == int.class) return 0; if (type == long.class) return 0L;
        if (type == double.class) return 0.0; if (type == float.class) return 0F; if (type == short.class) return (short) 0; if (type == byte.class) return (byte) 0;
        return null;
    }

    private void fieldChecks() throws Exception {
        JavaPlugin ccs=(JavaPlugin)Bukkit.getPluginManager().getPlugin("CombatCoreSystems");
        DefinitionRegistry definitions=(DefinitionRegistry)field(ccs,"definitions");
        var players=new PlayerDataService(ccs,null,definitions);
        var items=new ItemService(ccs,definitions); var combat=new CombatStateService(ccs,10000);
        var stats=new StatService(ccs,definitions,combat,items); var mobs=new MobService(ccs,definitions);
        var party=new PartyService(null,players); var displays=new DamageDisplayService(ccs,definitions);
        var levels=new LevelService(definitions,stats);
        var damage=new DamageService(ccs,definitions,players,stats,combat,new ElementService(ccs,definitions),mobs,party,displays,new RegionService(ccs,definitions),levels,items);
        var buffs=new BuffService(ccs,definitions,players,stats,damage,new HealService(players,stats,displays,levels,mobs));
        String effect=definitions.snapshot().buffs().keySet().iterator().next();
        var origin=Bukkit.getWorlds().getFirst().getSpawnLocation();
        origin.getChunk().addPluginChunkTicket(this);
        var owner=origin.getWorld().spawn(origin,org.bukkit.entity.Cow.class); owner.setAI(false); owner.setInvulnerable(true);
        var target=origin.getWorld().spawn(origin.clone().add(1,0,0),org.bukkit.entity.Cow.class); target.setAI(false); target.setInvulnerable(true);
        var events=new ArrayList<com.github.saku0817.combatcoresystems.model.trigger.EventContext>();
        var fields=new FieldService(new TargetSelectorService(party,damage,players,stats,mobs),buffs,events::add);
        Bukkit.getScheduler().runTask(this,()->{
            try {
                check(Bukkit.getEntity(owner.getUniqueId())!=null,"field owner registered after spawn tick: valid="+owner.isValid()+" dead="+owner.isDead()+" chunkLoaded="+owner.getChunk().isLoaded()+" worldLookup="+(owner.getWorld().getEntity(owner.getUniqueId())!=null));
                buffs.apply(target,effect,owner.getUniqueId());
                Map<String,Object> options=new LinkedHashMap<>(); options.put("id","one"); options.put("duration",10);
                options.put("area",Map.of("shape","SPHERE","radius",3)); options.put("enemy-effects",List.of(effect));
                var context=new com.github.saku0817.combatcoresystems.model.trigger.EventContext(com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.SKILL,null,owner,target);
                fields.create(owner,"fixture",options,context); fields.tick();
                check(buffs.effects(target).size()==2,"field entry adds owned buff beside ordinary buff");
                options.put("id","two"); fields.create(owner,"fixture",Map.copyOf(options),context); fields.tick();
                check(buffs.effects(target).size()==3,"overlapping fields have independent leases");
                fields.remove(owner.getUniqueId(),"fixture","one");
                check(buffs.effects(target).size()==2,"removing one field preserves other field and ordinary buff");
                target.teleport(origin.clone().add(20,0,0)); fields.tick();
                check(buffs.effects(target).size()==1,"leaving field preserves ordinary buff only");
                check(events.stream().anyMatch(e->e.event==com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.ENTER_FIELD),"ENTER_FIELD emitted");
                check(events.stream().anyMatch(e->e.event==com.github.saku0817.combatcoresystems.model.trigger.TriggerEvent.LEAVE_FIELD),"LEAVE_FIELD emitted");
                getLogger().info("V145_SMOKE_PASS: "+checks+" assertions (Paper items/entities + simulated player; no real clients)");
            } catch(Throwable error) { getLogger().log(java.util.logging.Level.SEVERE,"V145_SMOKE_FAIL",error); }
            finally { fields.reset(); owner.remove(); target.remove(); origin.getChunk().removePluginChunkTicket(this); }
        });
    }
}
