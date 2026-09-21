package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import java.util.*;
import java.util.function.Consumer;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

public final class FieldService {
    private record Key(UUID owner,String source,String id) {}
    private static final class Field {
        final Key key; final Location origin; final Area area; final long expires; final Map<String,Object> options;
        final Map<UUID,List<String>> inside=new LinkedHashMap<>();
        Field(Key key,Location origin,Area area,long expires,Map<String,Object> options) {
            this.key=key; this.origin=origin.clone(); this.area=area; this.expires=expires; this.options=options;
        }
    }
    private final Map<Key,Field> fields=new LinkedHashMap<>();
    private final TargetSelectorService selectors;
    private final BuffService buffs;
    private final Consumer<EventContext> events;
    public FieldService(TargetSelectorService selectors,BuffService buffs,Consumer<EventContext> events) { this.selectors=selectors; this.buffs=buffs; this.events=events; }
    public void create(LivingEntity owner,String source,Map<String,Object> options,EventContext context) {
        String id=text(options,"id",""); remove(owner.getUniqueId(),source,id);
        Area area=area(child(options,"area")); Location origin=selectors.origin(owner,context,area);
        if (origin==null) return;
        long duration=Math.round(number(options,"duration",0)*1000);
        Key key=new Key(owner.getUniqueId(),source,id);
        fields.put(key,new Field(key,origin,area,duration==0 ? Long.MAX_VALUE : System.currentTimeMillis()+duration,options));
    }
    public boolean inside(LivingEntity entity,String id) {
        return fields.values().stream().anyMatch(f -> f.key.id().equals(id) && f.expires>System.currentTimeMillis() && selectors.contains(f.origin,f.area,entity.getLocation()));
    }
    private String lease(Field field,String effect) { return "field:"+field.key+":"+effect; }
    private void leave(Field field,UUID target,List<String> effects,LivingEntity owner) {
        effects.forEach(id -> buffs.release(target,lease(field,id)));
        if (Bukkit.getEntity(target) instanceof LivingEntity entity) {
            EventContext event=new EventContext(TriggerEvent.LEAVE_FIELD,null,entity,owner); event.values.put("fieldId",field.key.id()); events.accept(event);
        }
    }
    public void remove(UUID owner,String source,String id) {
        Field old=fields.remove(new Key(owner,source,id)); if (old==null) return;
        LivingEntity entity=Bukkit.getEntity(owner) instanceof LivingEntity e ? e : null;
        old.inside.forEach((target,effects) -> leave(old,target,effects,entity));
    }
    public void tick() {
        for (Field field : List.copyOf(fields.values())) {
            LivingEntity owner=Bukkit.getEntity(field.key.owner()) instanceof LivingEntity e ? e : null;
            if (owner==null || owner.isDead() || field.expires<=System.currentTimeMillis()) { remove(field.key.owner(),field.key.source(),field.key.id()); continue; }
            Set<UUID> seen=new HashSet<>();
            String mode=text(field.options,"effect-mode","WHILE_INSIDE");
            for (LivingEntity target : selectors.inArea(field.origin,field.area)) {
                String filter=text(field.options,"target-filter","ALL");
                if (filter.equals("ENEMY") && !selectors.enemy(owner,target) || filter.equals("ALLY") && !selectors.ally(owner,target)) continue;
                List<String> effects=selectors.ally(owner,target) ? strings(field.options,"ally-effects")
                        : selectors.enemy(owner,target) ? strings(field.options,"enemy-effects") : List.of();
                UUID uuid=target.getUniqueId(); seen.add(uuid);
                List<String> old=field.inside.put(uuid,effects);
                if (old!=null && !old.equals(effects)) old.forEach(id -> buffs.release(uuid,lease(field,id)));
                if (old==null) {
                    EventContext event=new EventContext(TriggerEvent.ENTER_FIELD,null,target,owner); event.values.put("fieldId",field.key.id()); events.accept(event);
                }
                if (fields.get(field.key)!=field) break;
                if (mode.equals("WHILE_INSIDE")) effects.forEach(id -> buffs.acquire(target,lease(field,id),id,owner.getUniqueId()));
                else if (mode.equals("REFRESH_WHILE_INSIDE")) effects.forEach(id -> buffs.refresh(target,id,owner.getUniqueId()));
                else if (old==null) effects.forEach(id -> buffs.apply(target,id,owner.getUniqueId()));
            }
            for (UUID uuid : List.copyOf(field.inside.keySet())) if (!seen.contains(uuid)) leave(field,uuid,field.inside.remove(uuid),owner);
        }
    }
    public void forget(UUID owner) {
        for (Key key : List.copyOf(fields.keySet())) if (key.owner().equals(owner)) remove(key.owner(),key.source(),key.id());
    }
    public void reset() { for (Key key : List.copyOf(fields.keySet())) remove(key.owner(),key.source(),key.id()); }
}
