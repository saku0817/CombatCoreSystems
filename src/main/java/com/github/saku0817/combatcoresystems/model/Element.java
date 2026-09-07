package com.github.saku0817.combatcoresystems.model;

import java.util.Locale;
import java.util.Optional;

public enum Element {
    FIRE("炎"), WATER("水"), WIND("風"), THUNDER("雷"), MOON("月"), PHYSICAL("無属性");

    private final String japaneseName;

    Element(String japaneseName) {
        this.japaneseName = japaneseName;
    }

    public String japaneseName() {
        return japaneseName;
    }

    public static Optional<Element> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (Element element : values()) {
            if (element.name().equals(normalized) || element.japaneseName.equals(value.trim())) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }
}
