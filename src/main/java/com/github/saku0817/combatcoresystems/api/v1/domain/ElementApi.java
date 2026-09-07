package com.github.saku0817.combatcoresystems.api.v1.domain;
import com.github.saku0817.combatcoresystems.model.Element;
import java.util.Map;
import java.util.UUID;
public interface ElementApi { boolean attach(UUID target, Element element, double seconds); boolean remove(UUID target, Element element); Map<Element, Long> remaining(UUID target); }
