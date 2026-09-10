package com.github.saku0817.combatcoresystems.util;

import com.github.saku0817.combatcoresystems.model.Element;
import java.util.Locale;

/** Player-facing names; persistent keys remain unchanged. */
public final class DisplayNames {
    private DisplayNames() {}
    public static String japanese(String raw) {
        String key = raw.toUpperCase(Locale.ROOT).replace('-', '_');
        for (Element element : Element.values()) {
            if (key.equals(element.name())) return element.japaneseName();
            if (key.equals(element.name() + "_DAMAGE")) return element.japaneseName() + "属性ダメージ";
            if (key.equals(element.name() + "_RESISTANCE")) return element.japaneseName() + "属性耐性";
            if (key.equals(element.name() + "_RESISTANCE_DOWN")) return element.japaneseName() + "属性耐性低下";
        }
        return switch (key) {
            case "HP", "HP_FLAT" -> "体力";
            case "HP_PERCENT" -> "体力補正";
            case "ATK", "ATK_FLAT" -> "攻撃力";
            case "ATK_PERCENT" -> "攻撃力補正";
            case "DEF", "DEF_FLAT" -> "防御力";
            case "DEF_PERCENT" -> "防御力補正";
            case "CRIT_RATE" -> "会心率";
            case "CRIT_DAMAGE" -> "会心ダメージ";
            case "HEALING_POWER" -> "回復力";
            case "COOLDOWN" -> "クールタイム短縮";
            case "ATTACK_SPEED" -> "攻撃速度";
            case "DEF_DOWN" -> "防御力低下";
            case "HEAD" -> "頭";
            case "CHEST" -> "胴";
            case "LEGS" -> "脚";
            case "FEET" -> "足";
            case "RESONANCE" -> "残響";
            case "DIVINE_HEART" -> "神の心";
            case "MELEE", "MELEE_WEAPON" -> "近距離武器";
            case "RANGED", "RANGED_WEAPON" -> "遠距離武器";
            case "NORMAL_ATTACK_ATTRIBUTE", "NORMAL_ATTACK_ELEMENT" -> "通常攻撃の属性";
            case "SKILL.ATTRIBUTE", "SKILL.ELEMENT" -> "スキルの属性";
            case "ULTIMATE.ATTRIBUTE", "ULTIMATE.ELEMENT" -> "必殺技の属性";
            case "SKILL.MULTIPLIER" -> "スキル倍率補正";
            case "ULTIMATE.MULTIPLIER" -> "必殺技倍率補正";
            case "SKILL.RADIUS_ADD" -> "スキル範囲加算";
            case "ULTIMATE.RADIUS_ADD" -> "必殺技範囲加算";
            case "MIN_HP_PERCENT" -> "必要体力割合";
            case "REQUIRES_TARGET" -> "対象が必要";
            case "MAX_DISTANCE" -> "最大距離";
            case "REQUIRES_COMBAT" -> "戦闘中のみ";
            case "TRUE" -> "有効";
            case "FALSE" -> "無効";
            default -> raw;
        };
    }
}
