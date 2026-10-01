package com.github.saku0817.combatcoresystems.model;

import java.util.*;
import java.util.random.RandomGenerator;

/** Persisted draws separate unlock milestones from upgrade milestones. No world/server state required. */
public final class EquipmentGrowth {
    private EquipmentGrowth() {}
    public static final int VERSION = 146;

    public static boolean migrate(ItemInstance item, EquipmentDefinition definition,
                                  List<EquipmentRolls.Candidate> mainPool, List<EquipmentRolls.Candidate> subPool) {
        if (item.getEquipmentGrowthVersion() >= VERSION) return false;
        // The same legacy instance in inventory and stored player data must migrate identically.
        initialize(item, definition, mainPool, subPool, new Random(Objects.hash(item.getInstanceId(), VERSION)), true);
        return true;
    }

    public static void initialize(ItemInstance item, EquipmentDefinition definition,
                                  List<EquipmentRolls.Candidate> mainPool, List<EquipmentRolls.Candidate> subPool,
                                  RandomGenerator random, boolean preserveKinds) {
        Set<StatKey> allowedMain = EquipmentGrowthTable.mainCandidates(definition.slot());
        List<EquipmentRolls.Candidate> mains = candidates(mainPool, allowedMain);
        StatKey main = preserveKinds ? item.mainStat(definition) : null;
        if (main == null || !allowedMain.contains(main)) main = EquipmentRolls.draw(mains,1,random).getFirst().key();
        item.setMainStat(main, EquipmentGrowthTable.mainValue(definition.slot(),main,1),
                EquipmentGrowthTable.mainValue(definition.slot(),main,EquipmentGrowthTable.maximumLevel(definition.rarity())));
        List<EquipmentRolls.Candidate> subs = candidates(subPool, EquipmentGrowthTable.subCandidates());
        LinkedHashSet<StatKey> chosen = new LinkedHashSet<>();
        int maximum = EquipmentGrowthTable.maximumSubstats(definition.rarity());
        if (preserveKinds) for (String raw : item.getSubstats().keySet()) {
            try {
                StatKey key = StatKey.valueOf(raw.toUpperCase(Locale.ROOT));
                if (chosen.size()<maximum && EquipmentGrowthTable.subCandidates().contains(key)) chosen.add(key);
            } catch (IllegalArgumentException ignored) { }
        }
        List<EquipmentRolls.Candidate> remaining = subs.stream().filter(c->!chosen.contains(c.key())).toList();
        EquipmentRolls.draw(remaining,maximum-chosen.size(),random).forEach(c->chosen.add(c.key()));
        item.getSubstats().clear(); item.getSubstatUpgrades().clear(); item.getEquipmentUpgradeRolls().clear();
        chosen.forEach(key->item.getSubstats().put(key.name(),EquipmentGrowthTable.subValue(key,0)));
        List<String> keys = new ArrayList<>(item.getSubstats().keySet());
        for (int i=0;i<EquipmentGrowthTable.maximumLevel(definition.rarity())/3;i++)
            item.getEquipmentUpgradeRolls().add(keys.get(random.nextInt(keys.size())));
        item.setEquipmentGrowthVersion(VERSION);
        setLevel(item,definition,item.getLevel());
    }

    private static List<EquipmentRolls.Candidate> candidates(List<EquipmentRolls.Candidate> configured, Set<StatKey> allowed) {
        List<EquipmentRolls.Candidate> filtered = configured.stream().filter(c->allowed.contains(c.key())).toList();
        return filtered.isEmpty() ? allowed.stream().sorted().map(k->new EquipmentRolls.Candidate(k,0,0,1)).toList() : filtered;
    }

    /** Used for explicit admin level setting: replay the saved roll history, never reroll. */
    public static void setLevel(ItemInstance item, EquipmentDefinition definition, int level) {
        item.setLevel(Math.clamp(level,1,EquipmentGrowthTable.maximumLevel(definition.rarity())));
        item.setUnlockedSubstats(Math.min(item.getSubstats().size(),item.getLevel()/3));
        item.getSubstatUpgrades().clear();
        item.getSubstats().keySet().forEach(key->item.getSubstatUpgrades().put(key,0));
        for (String key : item.getEquipmentUpgradeRolls().subList(0,Math.min(item.getEquipmentUpgradeRolls().size(),item.getLevel()/3)))
            if (item.getSubstats().containsKey(key)) item.getSubstatUpgrades().merge(key,1,Integer::sum);
        recalculate(item);
        if (item.getLevel()==EquipmentGrowthTable.maximumLevel(definition.rarity())) item.setExp(0);
    }

    /** A normal level gain retains administrator-edited counts and adds only this milestone's roll. */
    public static void advance(ItemInstance item, EquipmentDefinition definition) {
        if (item.getLevel()>=EquipmentGrowthTable.maximumLevel(definition.rarity())) return;
        item.setLevel(item.getLevel()+1);
        item.setUnlockedSubstats(Math.min(item.getSubstats().size(),item.getLevel()/3));
        if (item.getLevel()%3==0) {
            String key=item.getEquipmentUpgradeRolls().get(item.getLevel()/3-1);
            if(item.getSubstats().containsKey(key)) item.getSubstatUpgrades().compute(key,(k,v)->Math.min(5,(v==null?0:v)+1));
        }
        recalculate(item);
    }
    public static void recalculate(ItemInstance item) {
        item.getSubstats().replaceAll((key,unused)->EquipmentGrowthTable.subValue(StatKey.valueOf(key),item.getSubstatUpgrades().getOrDefault(key,0)));
    }
}
