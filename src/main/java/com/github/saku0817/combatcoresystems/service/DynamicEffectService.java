package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.StatKey;
import java.util.*;

public final class DynamicEffectService {
    private record Entry(StatKey stat,double value,long expires) {}
    private final Map<UUID,Map<String,Entry>> effects=new HashMap<>();
    public void apply(UUID target,String key,StatKey stat,double value,long duration,String reapply) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite dynamic modifier");
        var entries=effects.computeIfAbsent(target,ignored -> new LinkedHashMap<>());
        Entry old=entries.get(key); long now=System.currentTimeMillis();
        if (old!=null && old.expires()>now && reapply.equals("IGNORE")) return;
        long expires=duration==0 ? Long.MAX_VALUE : now+duration;
        if (old!=null && old.expires()>now && reapply.equals("EXTEND")) expires=old.expires()==Long.MAX_VALUE || duration==0 ? Long.MAX_VALUE : old.expires()+duration;
        entries.put(key,new Entry(stat,value,expires));
    }
    public Map<StatKey,Double> modifiers(UUID target) {
        var entries=effects.get(target); if (entries==null) return Map.of();
        entries.values().removeIf(e -> e.expires()<=System.currentTimeMillis());
        if (entries.isEmpty()) { effects.remove(target); return Map.of(); }
        Map<StatKey,Double> result=new EnumMap<>(StatKey.class);
        entries.values().forEach(e -> result.merge(e.stat(),e.value(),Double::sum));
        return result;
    }
    public void forget(UUID target) { effects.remove(target); }
    public void reset() { effects.clear(); }
}
