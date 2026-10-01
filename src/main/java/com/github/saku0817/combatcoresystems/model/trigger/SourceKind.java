package com.github.saku0817.combatcoresystems.model.trigger;

public enum SourceKind {
    NORMAL_ATTACK, SKILL, ULTIMATE, TALENT, TRIGGER, BUFF, DEBUFF, DOT, FIELD, REACTION, MOB_ATTACK, ENVIRONMENT;
    public static SourceKind from(String source) {
        if(source==null)return ENVIRONMENT;
        if(source.equals("normal_attack"))return NORMAL_ATTACK;
        String prefix=source.contains(":")?source.substring(0,source.indexOf(':')):source;
        return switch(prefix.toLowerCase(java.util.Locale.ROOT)) {
            case "skill" -> SKILL; case "ultimate" -> ULTIMATE; case "talent" -> TALENT;
            case "buff","dot","effect" -> DOT; case "debuff" -> DEBUFF; case "field" -> FIELD;
            case "reaction" -> REACTION; case "trigger" -> TRIGGER; case "mob","mob_attack" -> MOB_ATTACK;
            default -> ENVIRONMENT;
        };
    }
}
