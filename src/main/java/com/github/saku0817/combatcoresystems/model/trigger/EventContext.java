package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.Element;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.*;

public final class EventContext {
    public final TriggerChain chain;
    public final TriggerEvent event;
    public LivingEntity source, target, attacker, victim, healer, healed;
    public Player player;
    public String weapon = "", ability = "", combatId = "";
    public double damage, finalDamage, healAmount, requestedHeal, effectiveHeal, overheal;
    public Element attribute = Element.PHYSICAL;
    public boolean critical, reaction, normalAttack, skill, ultimate;
    public Location location;
    public final Map<String, Object> values = new LinkedHashMap<>();
    public final EventModifier sourceModifiers = new EventModifier();
    public final EventModifier targetModifiers = new EventModifier();
    public EventContext(TriggerEvent event, TriggerChain chain, LivingEntity source, LivingEntity target) {
        this.event = Objects.requireNonNull(event); this.chain = chain == null ? new TriggerChain() : chain;
        this.source = source; this.target = target; this.attacker = source; this.victim = target;
        this.player = source instanceof Player p ? p : null;
        this.location = target != null ? target.getLocation() : source == null ? null : source.getLocation();
    }
    public EventContext child(TriggerEvent event, LivingEntity source, LivingEntity target) {
        EventContext result = new EventContext(event, chain, source, target);
        result.weapon = weapon; result.ability = ability; result.combatId = combatId;
        result.damage = damage; result.finalDamage = finalDamage;
        result.healAmount = healAmount; result.requestedHeal=requestedHeal; result.effectiveHeal = effectiveHeal; result.overheal = overheal;
        result.attribute = attribute; result.critical = critical; result.reaction = reaction;
        result.normalAttack = normalAttack; result.skill = skill; result.ultimate = ultimate;
        result.healer = healer; result.healed = healed; result.values.putAll(values);
        return result;
    }
}
