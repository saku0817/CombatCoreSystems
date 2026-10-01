package com.github.saku0817.combatcoresystems.model;

import java.util.*;

/** Exact v1.4.6 values; percentage columns are converted to ratios once, here. */
public final class EquipmentGrowthTable {
    private EquipmentGrowthTable() {}
    private static final Map<StatKey, double[]> MAIN = new EnumMap<>(StatKey.class);
    private static final Map<StatKey, double[]> SUB = new EnumMap<>(StatKey.class);
    static {
        main(StatKey.HP_FLAT, false, 152,191,231,270,310,349,389,429,468,508,547,587,626,666,705);
        main(StatKey.DEF_FLAT, false, 76,95,115,135,155,174,194,214,234,254,273,293,313,333,352);
        MAIN.put(StatKey.ATK_FLAT, MAIN.get(StatKey.DEF_FLAT));
        main(StatKey.HP_PERCENT, true, 9.3,11.7,14.1,16.5,19,21.4,23.8,26.2,28.6,31.1,33.5,35.9,38.3,40.7,43.2);
        MAIN.put(StatKey.ATK_PERCENT, MAIN.get(StatKey.HP_PERCENT));
        main(StatKey.DEF_PERCENT, true, 11.6,14.6,17.7,20.7,23.7,26.7,29.8,32.8,35.8,38.8,41.9,44.9,47.9,50.9,54);
        main(StatKey.CRIT_DAMAGE, true, 13.9,17.6,21.2,24.8,28.5,32.1,35.7,39.3,43,46.6,50.2,53.9,57.5,61.1,64.8);
        main(StatKey.CRIT_RATE, true, 6.9,8.8,10.6,12.4,14.2,16,17.8,19.6,21.5,23.3,25.1,26.9,28.7,30.5,32.4);
        main(StatKey.HEALING_POWER, true, 7.4,9.4,11.3,13.2,15.2,17.1,19,21,22.9,24.8,26.8,28.7,30.6,32.6,34.5);
        for (String element : List.of("FIRE","WATER","WIND","THUNDER","MOON"))
            for (String suffix : List.of("_DAMAGE","_RESISTANCE"))
                main(StatKey.valueOf(element+suffix), true, 8.3,10.5,12.7,14.9,17.1,19.2,21.4,23.6,25.8,27.9,30.1,32.3,34.5,36.7,38.8);
        sub(StatKey.HP_FLAT, false, 38,76,114,152,190,228);
        sub(StatKey.DEF_FLAT, false, 19,38,57,76,95,114);
        SUB.put(StatKey.ATK_FLAT, SUB.get(StatKey.DEF_FLAT));
        sub(StatKey.HP_PERCENT, true, 3.8,7.6,11.4,15.2,19,22.8);
        SUB.put(StatKey.ATK_PERCENT, SUB.get(StatKey.HP_PERCENT));
        sub(StatKey.DEF_PERCENT, true, 4.8,9.6,14.4,19.2,24,28.8);
        sub(StatKey.CRIT_DAMAGE, true, 5.8,11.6,17.4,23.2,29,34.8);
        sub(StatKey.CRIT_RATE, true, 2.9,5.8,8.7,11.6,14.5,17.4);
    }
    private static void main(StatKey key, boolean percent, double... values) { MAIN.put(key, ratios(percent, values)); }
    private static void sub(StatKey key, boolean percent, double... values) { SUB.put(key, ratios(percent, values)); }
    private static double[] ratios(boolean percent, double[] values) {
        if (percent) for (int i=0;i<values.length;i++) values[i]/=100;
        return values;
    }
    public static Set<StatKey> mainCandidates(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Set.of(StatKey.HP_FLAT,StatKey.HP_PERCENT,StatKey.DEF_FLAT,StatKey.DEF_PERCENT);
            case CHEST -> Set.of(StatKey.ATK_FLAT,StatKey.ATK_PERCENT);
            case LEGS -> Set.of(StatKey.CRIT_RATE,StatKey.CRIT_DAMAGE);
            case FEET -> Set.of(StatKey.HP_PERCENT,StatKey.DEF_PERCENT,StatKey.ATK_PERCENT,StatKey.HEALING_POWER);
            case RESONANCE -> Set.of(StatKey.FIRE_DAMAGE,StatKey.WATER_DAMAGE,StatKey.WIND_DAMAGE,StatKey.THUNDER_DAMAGE,StatKey.MOON_DAMAGE,
                    StatKey.FIRE_RESISTANCE,StatKey.WATER_RESISTANCE,StatKey.WIND_RESISTANCE,StatKey.THUNDER_RESISTANCE,StatKey.MOON_RESISTANCE);
            default -> Set.of();
        };
    }
    public static Set<StatKey> subCandidates() { return Collections.unmodifiableSet(SUB.keySet()); }
    public static int maximumLevel(int rarity) { return switch(rarity) { case 3 -> 9; case 4 -> 12; case 5 -> 15; default -> throw new IllegalArgumentException("Equipment rarity must be 3, 4 or 5"); }; }
    public static int maximumSubstats(int rarity) { maximumLevel(rarity); return rarity-1; }
    public static double mainValue(EquipmentSlot slot, StatKey key, int level) {
        if (!mainCandidates(slot).contains(key) || level<1 || level>15) throw new IllegalArgumentException("Invalid main stat or level");
        return MAIN.get(key)[level-1];
    }
    public static double subValue(StatKey key, int upgrades) {
        if (!SUB.containsKey(key) || upgrades<0 || upgrades>5) throw new IllegalArgumentException("Invalid substat or upgrade count");
        return SUB.get(key)[upgrades];
    }
}
