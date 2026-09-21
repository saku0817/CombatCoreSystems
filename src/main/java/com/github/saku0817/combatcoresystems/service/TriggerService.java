package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageResult;
import com.github.saku0817.combatcoresystems.api.v1.event.AfterDamageEvent;
import com.github.saku0817.combatcoresystems.api.v1.event.CombatStateEvent;
import com.github.saku0817.combatcoresystems.config.*;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.function.Supplier;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

/** Shared synchronous event engine. Sources are compiled once per validated snapshot. */
public final class TriggerService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final ItemService items;
    private final StatService stats;
    private final CombatStateService combat;
    private final DamageService damage;
    private final HealService healing;
    private final BuffService buffs;
    private final TargetSelectorService selectors;
    private final StackService stacks=new StackService();
    private final DynamicEffectService dynamic=new DynamicEffectService();
    private final FieldService fields;
    private DefinitionRegistry.Snapshot snapshot;
    private Map<String,List<TriggerCatalog.Source>> catalog=Map.of();
    private final Map<String,Long> cooldowns=new HashMap<>();
    private final Map<String,Integer> activations=new HashMap<>();
    private final Map<String,Boolean> hpStates=new HashMap<>();
    private final Map<UUID,String> combats=new HashMap<>();
    private EventContext current;
    private double nextDefIgnore;
    private boolean resetting;
    private boolean scanInventory,scanHotbar,monitors;
    public TriggerService(JavaPlugin plugin,DefinitionRegistry definitions,PlayerDataService players,ItemService items,
                          StatService stats,CombatStateService combat,DamageService damage,HealService healing,BuffService buffs,
                          PartyService parties,MobService mobs) {
        this.plugin=plugin; this.definitions=definitions; this.players=players; this.items=items; this.stats=stats;
        this.combat=combat; this.damage=damage; this.healing=healing; this.buffs=buffs;
        selectors=new TargetSelectorService(parties,damage,players,stats,mobs);
        fields=new FieldService(selectors,buffs,this::emit);
        stacks.onChange(this::stackChanged);
    }
    public DynamicEffectService dynamic() { return dynamic; }
    public EventContext current() { return current; }
    public void start() { refresh(); Bukkit.getScheduler().runTaskTimer(plugin,this::tick,5,5); }
    private void refresh() {
        if (snapshot==definitions.snapshot()) return;
        snapshot=definitions.snapshot();
        List<String> errors=new ArrayList<>();
        Map<String,org.bukkit.configuration.file.YamlConfiguration> files=new HashMap<>();
        for (String file : List.of("weapons.yml","equipment.yml","sets.yml","divine_hearts.yml")) files.put(file,snapshot.config(file));
        catalog=TriggerCatalog.compile(files,snapshot.buffs().keySet(),errors);
        var all=catalog.values().stream().flatMap(Collection::stream).toList();
        scanInventory=all.stream().anyMatch(s -> s.placement().equals("talent") && text(s.options(),"hand","MAIN_HAND").equalsIgnoreCase("INVENTORY"));
        scanHotbar=all.stream().anyMatch(s -> s.placement().equals("talent") && text(s.options(),"hand","MAIN_HAND").equalsIgnoreCase("HOT_BAR"));
        monitors=all.stream().flatMap(s -> s.triggers().stream()).anyMatch(t -> Set.of(TriggerEvent.HP_BELOW,TriggerEvent.HP_ABOVE,TriggerEvent.TICK).contains(t.event()));
        resetting=true;
        try { fields.reset(); stacks.reset(); dynamic.reset(); cooldowns.clear(); activations.clear(); hpStates.clear(); }
        finally { resetting=false; }
        errors.forEach(plugin.getLogger()::warning);
    }
    private List<TriggerCatalog.Source> sources(Player player) {
        refresh(); PlayerData data=players.find(player.getUniqueId()).orElse(null); if (data==null) return List.of();
        List<TriggerCatalog.Source> result=new ArrayList<>(); Set<String> seen=new HashSet<>();
        int selected=player.getInventory().getHeldItemSlot();
        for (int slot=0;slot<=40;slot++) {
            if (slot>=36 && slot<=39) continue;
            if (slot!=selected && slot!=40 && !scanInventory && !(slot<=8 && scanHotbar)) continue;
            var stack=player.getInventory().getItem(slot);
            if (!snapshot.weapons().containsKey(items.id(stack).orElse(""))) continue;
            ItemInstance item=items.instance(stack).orElse(null);
            WeaponDefinition weapon=item==null ? null : snapshot.weapon(item);
            if (weapon==null || !weapon.canEquip(data.getLevel())) continue;
            for (var source : catalog.getOrDefault("weapon:"+weapon.id()+":"+Math.clamp(item.getLimitBreak(),0,5),List.of())) {
                boolean active=slot==selected || slot==40;
                if (source.placement().equals("talent")) {
                    WeaponOptions.Hand hand=WeaponOptions.Hand.valueOf(text(source.options(),"hand","MAIN_HAND").toUpperCase(Locale.ROOT));
                    active=hand.includes(slot,selected);
                }
                if (active && seen.add(source.definition()+":"+source.placement())) result.add(source);
            }
        }
        for (ItemInstance item : data.getEquipment().values()) {
            String root=snapshot.equipment().containsKey(item.getDefinitionId()) ? "equipment:" : "divine-hearts:";
            if (seen.add(root+item.getDefinitionId())) result.addAll(catalog.getOrDefault(root+item.getDefinitionId(),List.of()));
        }
        for (var set : SetEffectService.counts(snapshot,data).entrySet()) for (var source : catalog.getOrDefault("sets:"+set.getKey(),List.of()))
            if (set.getValue()>=(source.placement().equals("two-piece") ? 2 : 4)) result.add(source);
        return result;
    }
    public void emit(EventContext context) {
        if (resetting) return;
        if (!(context.source instanceof Player player) || !player.isOnline() || players.find(player.getUniqueId()).isEmpty()) return;
        EventContext prior=current; current=context;
        try {
            context.player=player; context.combatId=combats.getOrDefault(player.getUniqueId(),"");
            for (var source : sources(player)) for (var trigger : source.triggers()) {
                if (trigger.event()!=context.event) continue;
                if ((context.event==TriggerEvent.SKILL || context.event==TriggerEvent.ULTIMATE)
                        && source.definition().startsWith("weapon:") && !context.weapon.isBlank() && !source.definition().equals("weapon:"+context.weapon)) continue;
                if (source.placement().equals("skill") && context.event==TriggerEvent.ULTIMATE
                        || source.placement().equals("ultimate") && context.event==TriggerEvent.SKILL) continue;
                if (Set.of(TriggerEvent.BEFORE_HIT,TriggerEvent.HIT,TriggerEvent.NORMAL_ATTACK,TriggerEvent.HEAL,TriggerEvent.OVERHEAL).contains(context.event)) {
                    if (source.placement().equals("skill") && !context.skill || source.placement().equals("ultimate") && !context.ultimate) continue;
                }
                if (context.values.containsKey("stackSource") && !source.definition().equals(context.values.get("stackSource"))) continue;
                String key=player.getUniqueId()+":"+source.definition()+":"+source.placement()+":"+trigger.id();
                if (context.event==TriggerEvent.HP_BELOW || context.event==TriggerEvent.HP_ABOVE) {
                    double ratio=selectors.health(player)/Math.max(1,selectors.maxHealth(player));
                    boolean active=context.event==TriggerEvent.HP_BELOW ? ratio<=trigger.hpPercent() : ratio>trigger.hpPercent();
                    Boolean before=hpStates.put(key,active);
                    if (!active || Boolean.TRUE.equals(before)) continue;
                }
                if (context.event==TriggerEvent.STACK_REACHED) {
                    var condition=child(trigger.conditions(),"stack");
                    if (!Objects.equals(text(condition,"id",""),context.values.get("stackId"))) continue;
                    double threshold=number(condition,"min",1);
                    if (number(context.values,"oldStacks",0)>=threshold || number(context.values,"newStacks",0)<threshold) continue;
                }
                if (!conditions(trigger.conditions(),player,source.definition(),context)) continue;
                long now=System.currentTimeMillis();
                if (cooldowns.getOrDefault(key,0L)>now) continue;
                String counter=key+":"+trigger.activationScope()+(trigger.activationScope().equals("COMBAT") ? ":"+context.combatId : "");
                if (trigger.activationScope().equals("COMBAT") && !combat.inCombat(player.getUniqueId()) && context.event!=TriggerEvent.COMBAT_END) continue;
                if (trigger.maxActivations()>0 && activations.getOrDefault(counter,0)>=trigger.maxActivations()) continue;
                if (!context.chain.enter(player.getUniqueId(),source.definition(),source.placement()+":"+trigger.id())) {
                    if (context.chain.depth()>=16) plugin.getLogger().warning("Trigger chain depth limit reached: "+context.chain.id());
                    continue;
                }
                cooldowns.put(key,now+trigger.cooldown());
                if (trigger.maxActivations()>0) activations.merge(counter,1,Integer::sum);
                try { actions(player,source,trigger.actions(),context,trigger.target(),trigger.id()); }
                finally { context.chain.leave(); }
            }
        } finally { current=prior; }
    }
    public boolean conditions(Map<String,Object> c,Player owner,String definition,EventContext context) {
        double hp=selectors.health(owner)/Math.max(1,selectors.maxHealth(owner)); LivingEntity target=context.target;
        for (String key : c.keySet()) {
            boolean actual;
            switch(key) {
                case "min-hp-percent" -> { if (hp<number(c,key,0)) return false; continue; }
                case "max-hp-percent" -> { if (hp>number(c,key,1)) return false; continue; }
                case "min-distance", "max-distance" -> {
                    if (target==null || !owner.getWorld().equals(target.getWorld())) return false;
                    double distance=owner.getLocation().distance(target.getLocation());
                    if (key.equals("min-distance") ? distance<number(c,key,0) : distance>number(c,key,0)) return false; continue;
                }
                case "element" -> { if (context.attribute!=Element.parse(text(c,key,"")).orElse(null)) return false; continue; }
                case "buff-present" -> { if (!buffs.has(owner,text(c,key,""))) return false; continue; }
                case "buff-absent" -> { if (buffs.has(owner,text(c,key,""))) return false; continue; }
                case "inside-field" -> { if (!fields.inside(owner,text(c,key,""))) return false; continue; }
                case "outside-field" -> { if (fields.inside(owner,text(c,key,""))) return false; continue; }
                case "stack", "context-value" -> {
                    var condition=child(c,key); double value;
                    if (key.equals("context-value")) {
                        Object raw=context.values.get(text(condition,"key","")); if (!(raw instanceof Number n)) return false; value=n.doubleValue();
                    } else {
                        String scope=text(condition,"scope","SELF").toUpperCase(Locale.ROOT), id=text(condition,"id","");
                        StackService.Key stack=stackKey(owner,definition,id,scope,context);
                        value=scope.equals("ALL_TARGETS") ? stacks.total(owner.getUniqueId(),definition,id) : stack==null ? 0 : stacks.count(stack);
                    }
                    if (value<number(condition,"min",-Double.MAX_VALUE) || value>number(condition,"max",Double.MAX_VALUE)) return false; continue;
                }
                case "requires-combat" -> actual=combat.inCombat(owner.getUniqueId());
                case "requires-target" -> actual=target!=null && !target.isDead();
                case "damage-positive" -> actual=context.finalDamage>0;
                case "heal-positive" -> actual=context.healAmount>0;
                case "overheal-positive" -> actual=context.overheal>0;
                case "normal-attack-only" -> actual=context.normalAttack;
                case "skill-only" -> actual=context.skill;
                case "ultimate-only" -> actual=context.ultimate;
                case "critical" -> actual=context.critical;
                case "party-required" -> actual=selectors.hasParty(owner);
                case "target-is-self" -> actual=owner.equals(target);
                case "target-is-ally" -> actual=target!=null && selectors.ally(owner,target);
                case "target-is-enemy" -> actual=target!=null && selectors.enemy(owner,target);
                default -> throw new IllegalArgumentException("Unknown condition "+key);
            }
            if (actual!=Boolean.TRUE.equals(c.get(key))) return false;
        }
        return true;
    }
    private StackService.Key stackKey(Player owner,String source,String id,String scope,EventContext context) {
        LivingEntity target=switch(scope) { case "TARGET","EVENT_TARGET" -> context.target; case "ATTACKER" -> context.attacker; case "VICTIM" -> context.victim; default -> owner; };
        if (target==null) return null;
        return new StackService.Key(owner.getUniqueId(),source,id,scope.equals("SELF") ? null : target.getUniqueId());
    }
    private void actions(Player owner,TriggerCatalog.Source source,List<Map<String,Object>> actions,EventContext context,String fallbackTarget,String triggerId) {
        int index=0;
        for (var action : actions) {
            String actionKey=source.definition()+":"+source.placement()+":"+triggerId+":"+(index++);
            if (!conditions(child(action,"conditions"),owner,source.definition(),context)) continue;
            String type=text(action,"type","").toUpperCase(Locale.ROOT); double result=0;
            String scope=text(action,"scope","SELF").toUpperCase(Locale.ROOT),id=text(action,"id","");
            if (Set.of("ADD_STACK","SET_STACK","CLEAR_STACK","CONSUME_STACK").contains(type)) {
                StackService.Key key=stackKey(owner,source.definition(),id,scope,context);
                if (type.equals("CLEAR_STACK") && scope.equals("ALL_TARGETS")) result=stacks.clearTargets(owner.getUniqueId(),source.definition(),id);
                else if (key!=null) {
                    var p=child(source.options(),"target-stack-policy");
                    var policy=new StackService.Policy(text(p,"stack-id","").equals(id) ? integer(p,"max-targets",0,0) : 0,
                            StackService.Overflow.valueOf(text(p,"overflow","REMOVE_OLDEST").toUpperCase(Locale.ROOT)));
                    int amount=integer(action,"amount",1,0), max=integer(action,"max",1,1);
                    long duration=Math.round(number(action,"duration",0)*1000);
                    var reapply=StackService.Reapply.valueOf(text(action,"reapply","REFRESH"));
                    result=switch(type) {
                        case "ADD_STACK" -> stacks.add(key,amount,max,duration,reapply,policy);
                        case "SET_STACK" -> stacks.set(key,amount,max,duration,reapply,policy);
                        case "CLEAR_STACK" -> stacks.clear(key);
                        default -> stacks.consume(key,amount);
                    };
                }
            } else if (type.equals("CREATE_FIELD")) fields.create(owner,source.definition(),action,context);
            else if (type.equals("REMOVE_FIELD")) fields.remove(owner.getUniqueId(),source.definition(),id);
            else if (type.equals("MODIFY_EVENT_STATS")) {
                if (context.event!=TriggerEvent.BEFORE_HIT) continue;
                EventModifier modifier=Set.of("EVENT_TARGET","VICTIM","OTHER").contains(text(action,"target","SOURCE")) ? context.targetModifiers : context.sourceModifiers;
                child(action,"modifiers").forEach((mode,values) -> map(values).forEach((stat,value) -> modifier.add(mode,StatKey.valueOf(stat),((Number)value).doubleValue())));
            } else for (LivingEntity target : selectors.select(text(action,"target",fallbackTarget).toUpperCase(Locale.ROOT),owner,context,action)) {
                switch(type) {
                    case "APPLY_EFFECT" -> { if (buffs.apply(target,text(action,"effect",""),owner.getUniqueId())) result++; }
                    case "REMOVE_EFFECT" -> { if (buffs.remove(target,text(action,"effect",""))) result++; }
                    case "DAMAGE" -> {
                        List<WeaponOptions.Component> components=new ArrayList<>();
                        if (action.get("components") instanceof List<?> values) for (Object raw : values) {
                            var c=map(raw); components.add(new WeaponOptions.Component(ReferenceStat.valueOf(text(c,"reference","ATK").toUpperCase(Locale.ROOT)),number(c,"multiplier",1),Element.parse(text(c,"bonus-attribute","PHYSICAL")).orElseThrow()));
                        }
                        double prior=nextDefIgnore; nextDefIgnore=number(action,"def-ignore",0);
                        try { result+=damage.apply(new DamageRequest(owner.getUniqueId(),target.getUniqueId(),ReferenceStat.valueOf(text(action,"reference","ATK")),
                                number(action,"multiplier",1),Element.parse(text(action,"attribute","PHYSICAL")).orElseThrow(),true,false,0,"trigger:"+actionKey,components)).finalDamage(); }
                        finally { nextDefIgnore=prior; }
                    }
                    case "HEAL" -> result+=healing.healAmount(owner,target,reference(text(action,"reference","HP"),owner,target,context)*number(action,"multiplier",1),false);
                    case "APPLY_DYNAMIC_MODIFIER" -> {
                        double value=reference(text(action,"source",""),owner,target,context)*number(action,"multiplier",1);
                        dynamic.apply(target.getUniqueId(),owner.getUniqueId()+":"+actionKey,StatKey.valueOf(text(action,"stat","")),value,
                                Math.round(number(action,"duration",0)*1000),text(action,"reapply","REFRESH")); stats.invalidate(target.getUniqueId()); result=value;
                    }
                }
            }
            if (action.containsKey("store-result")) context.values.put(text(action,"store-result",""),result);
        }
    }
    private double reference(String key,LivingEntity owner,LivingEntity target,EventContext context) {
        return switch(key) {
            case "FIXED" -> 1;
            case "EVENT_DAMAGE" -> context.finalDamage;
            case "EVENT_HEAL" -> context.healAmount;
            case "EVENT_EFFECTIVE_HEAL" -> context.effectiveHeal;
            case "EVENT_OVERHEAL" -> context.overheal;
            case "TARGET_MAX_HP" -> selectors.maxHealth(target);
            case "TARGET_ATK" -> healing.value(target,ReferenceStat.ATK);
            case "TARGET_DEF" -> healing.value(target,ReferenceStat.DEF);
            case "HP","SOURCE_MAX_HP" -> selectors.maxHealth(owner);
            case "DEF","SOURCE_DEF" -> healing.value(owner,ReferenceStat.DEF);
            default -> healing.value(owner,ReferenceStat.ATK);
        };
    }
    public DamageResult damage(DamageRequest request,Supplier<DamageResult> operation) {
        LivingEntity attacker=request.attacker()!=null && Bukkit.getEntity(request.attacker()) instanceof LivingEntity e ? e : null;
        LivingEntity target=Bukkit.getEntity(request.target()) instanceof LivingEntity e ? e : null;
        EventContext event=new EventContext(TriggerEvent.BEFORE_HIT,current==null ? null : current.chain,attacker,target);
        if (attacker instanceof Player p) event.weapon=items.id(p.getInventory().getItemInMainHand()).orElse("");
        if (request.source().startsWith("skill:") || request.source().startsWith("ultimate:")) event.ability=request.source().substring(request.source().indexOf(':')+1);
        event.attribute=request.element(); event.normalAttack=request.source().equals("normal_attack");
        event.skill=request.source().startsWith("skill:") || request.source().startsWith("trigger:") && current!=null && current.skill;
        event.ultimate=request.source().startsWith("ultimate:") || request.source().startsWith("trigger:") && current!=null && current.ultimate;
        event.reaction=request.source().startsWith("reaction:"); event.sourceModifiers.add("flat",StatKey.DEF_IGNORE,nextDefIgnore);
        double priorIgnore=nextDefIgnore; nextDefIgnore=0;
        EventContext prior=current; current=event;
        try { return operation.get(); }
        finally { current=prior; nextDefIgnore=priorIgnore; }
    }
    public void beforeHit() { if (current!=null) emit(current); }
    @EventHandler public void onDamage(AfterDamageEvent event) {
        if (!event.getResult().applied() || event.getResult().finalDamage()<=0) return;
        var request=event.getRequest();
        LivingEntity attacker=request.attacker()!=null && Bukkit.getEntity(request.attacker()) instanceof LivingEntity e ? e : null;
        LivingEntity victim=Bukkit.getEntity(request.target()) instanceof LivingEntity e ? e : null;
        EventContext hit=current==null ? new EventContext(TriggerEvent.HIT,null,attacker,victim) : current.child(TriggerEvent.HIT,attacker,victim);
        hit.finalDamage=event.getResult().finalDamage(); hit.damage=hit.finalDamage; hit.critical=event.getResult().critical();
        hit.attribute=request.element(); hit.reaction=request.source().startsWith("reaction:"); hit.normalAttack=request.source().equals("normal_attack");
        hit.skill|=request.source().startsWith("skill:"); hit.ultimate|=request.source().startsWith("ultimate:");
        emit(hit);
        if (hit.normalAttack) emit(hit.child(TriggerEvent.NORMAL_ATTACK,attacker,victim));
        EventContext received=hit.child(TriggerEvent.TAKE_DAMAGE,victim,attacker); received.attacker=attacker; received.victim=victim; emit(received);
    }
    public void cast(Player owner,LivingEntity target,String weapon,int stage,boolean ultimate) {
        EventContext event=new EventContext(ultimate ? TriggerEvent.ULTIMATE : TriggerEvent.SKILL,current==null ? null : current.chain,owner,target);
        event.weapon=weapon; event.skill=!ultimate; event.ultimate=ultimate;
        emit(event);
        for (var source : sources(owner)) if (source.definition().equals("weapon:"+weapon) && source.placement().equals(ultimate ? "ultimate" : "skill")) {
            EventContext prior=current; current=event;
            try {
                var direct=new ArrayList<Map<String,Object>>();
                for (var action : TriggerDefinition.actions(source.options(),snapshot.buffs().keySet())) {
                    var inherited=new LinkedHashMap<>(action);
                    if (!inherited.containsKey("area") && source.options().containsKey("area")) inherited.put("area",source.options().get("area"));
                    direct.add(inherited);
                }
                actions(owner,source,direct,event,"SELF","direct");
            }
            finally { current=prior; }
        }
    }
    public boolean canCast(Player owner,LivingEntity target,String weapon,boolean ultimate,Map<String,Object> raw) {
        Map<String,Object> additional=new LinkedHashMap<>(map(raw));
        // v1.4.4 intentionally lets empty casts bypass target/distance requirements.
        for (String legacy : List.of("min-hp-percent","max-distance","requires-combat","requires-target")) additional.remove(legacy);
        EventContext event=new EventContext(ultimate ? TriggerEvent.ULTIMATE : TriggerEvent.SKILL,null,owner,target);
        event.skill=!ultimate; event.ultimate=ultimate;
        return conditions(additional,owner,"weapon:"+weapon,event);
    }
    public void healed(LivingEntity source,LivingEntity target,double requested,double effective,double overheal) {
        EventContext event=current==null ? new EventContext(TriggerEvent.HEAL,null,source,target) : current.child(TriggerEvent.HEAL,source,target);
        event.healer=source; event.healed=target; event.healAmount=requested; event.requestedHeal=requested; event.effectiveHeal=effective; event.overheal=overheal;
        emit(event); emit(event.child(TriggerEvent.RECEIVE_HEAL,target,source));
        if (overheal>0) emit(event.child(TriggerEvent.OVERHEAL,source,target));
    }
    public void buffEvent(LivingEntity target,String id,boolean added) {
        EventContext event=new EventContext(added ? TriggerEvent.BUFF_APPLIED : TriggerEvent.BUFF_REMOVED,current==null ? null : current.chain,target,target);
        event.values.put("buffId",id); emit(event);
    }
    private void stackChanged(StackService.Change change) {
        Player owner=Bukkit.getPlayer(change.key().owner());
        if (owner==null && current!=null && current.source instanceof Player p && p.getUniqueId().equals(change.key().owner())) owner=p;
        if (owner==null) return;
        LivingEntity target=change.key().target()!=null && Bukkit.getEntity(change.key().target()) instanceof LivingEntity e ? e : null;
        if (target==null && current!=null && current.target!=null && current.target.getUniqueId().equals(change.key().target())) target=current.target;
        EventContext event=new EventContext(TriggerEvent.STACK_CHANGED,current==null ? null : current.chain,owner,target);
        event.values.put("stackSource",change.key().source()); event.values.put("stackId",change.key().id());
        event.values.put("oldStacks",change.oldStacks()); event.values.put("newStacks",change.newStacks());
        event.values.put("stackDelta",change.newStacks()-change.oldStacks()); event.values.put("stackTarget",change.key().target());
        emit(event); if (change.newStacks()>change.oldStacks()) emit(event.child(TriggerEvent.STACK_REACHED,owner,target));
    }
    private void tick() {
        refresh(); stacks.expire(); fields.tick();
        if (!monitors) { long now=System.currentTimeMillis(); cooldowns.values().removeIf(end -> end<=now); return; }
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<TriggerCatalog.Source> active=sources(player);
            Set<String> hpKeys=new HashSet<>();
            String prefix=player.getUniqueId()+":";
            for (var source : active) for (var trigger : source.triggers())
                if (trigger.event()==TriggerEvent.HP_BELOW || trigger.event()==TriggerEvent.HP_ABOVE)
                    hpKeys.add(prefix+source.definition()+":"+source.placement()+":"+trigger.id());
            hpStates.keySet().removeIf(key -> key.startsWith(prefix) && !hpKeys.contains(key));
            for (TriggerEvent event : List.of(TriggerEvent.HP_BELOW,TriggerEvent.HP_ABOVE,TriggerEvent.TICK))
                if (active.stream().anyMatch(s -> s.triggers().stream().anyMatch(t -> t.event()==event))) emit(new EventContext(event,null,player,null));
        }
        long now=System.currentTimeMillis(); cooldowns.values().removeIf(end -> end<=now);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEnvironmentDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent || event.getFinalDamage()<=0 || !(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin,() -> {
            if (!player.isOnline()) return;
            EventContext context=new EventContext(TriggerEvent.TAKE_DAMAGE,null,player,null);
            context.attacker=null; context.victim=player; context.damage=event.getFinalDamage(); context.finalDamage=event.getFinalDamage(); emit(context);
        });
    }
    @EventHandler public void onCombat(CombatStateEvent event) {
        // Accessors are resolved against the stable CCS API below.
        if (!(Bukkit.getEntity(event.getEntityId()) instanceof Player player)) return;
        boolean active=event.isEntering();
        String ending=combats.getOrDefault(player.getUniqueId(),"");
        if (active) combats.put(player.getUniqueId(),UUID.randomUUID().toString());
        emit(new EventContext(active ? TriggerEvent.COMBAT_START : TriggerEvent.COMBAT_END,current==null ? null : current.chain,player,null));
        if (!active) {
            combats.remove(player.getUniqueId(),ending);
            activations.keySet().removeIf(k -> k.startsWith(player.getUniqueId()+":") && k.endsWith(":COMBAT:"+ending));
        }
    }
    private void forget(UUID owner,boolean death) {
        stacks.forget(owner); fields.forget(owner); dynamic.forget(owner);
        String prefix=owner+":"; hpStates.keySet().removeIf(k -> k.startsWith(prefix)); cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
        activations.keySet().removeIf(k -> k.startsWith(prefix) && (!death || k.endsWith(":LIFE") || k.contains(":COMBAT:")));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { forget(event.getPlayer().getUniqueId(),false); combats.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { forget(event.getEntity().getUniqueId(),true); }
}
