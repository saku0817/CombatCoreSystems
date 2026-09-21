package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

public final class TargetSelectorService {
    private final PartyService parties;
    private final DamageService damage;
    private final PlayerDataService players;
    private final StatService stats;
    private final MobService mobs;
    public TargetSelectorService(PartyService parties,DamageService damage,PlayerDataService players,StatService stats,MobService mobs) {
        this.parties=parties; this.damage=damage; this.players=players; this.stats=stats; this.mobs=mobs;
    }
    public double health(LivingEntity entity) {
        return entity instanceof Player p ? players.find(p.getUniqueId()).map(d->d.getHealth()).orElse(0.0) : mobs.health(entity);
    }
    public double maxHealth(LivingEntity entity) {
        return entity instanceof Player p ? players.find(p.getUniqueId()).map(d->stats.get(p,d).maxHp()).orElse(1.0) : mobs.maxHealth(entity);
    }
    public boolean ally(LivingEntity owner,LivingEntity target) {
        return owner.equals(target) || owner instanceof Player && target instanceof Player && parties.sameParty(owner.getUniqueId(),target.getUniqueId());
    }
    public boolean enemy(LivingEntity owner,LivingEntity target) { return !ally(owner,target) && damage.canAffect(owner,target); }
    public boolean hasParty(LivingEntity owner) { return parties.findByPlayer(owner.getUniqueId()).isPresent(); }
    private List<LivingEntity> party(LivingEntity owner,boolean includeSelf) {
        List<LivingEntity> result=new ArrayList<>();
        parties.findByPlayer(owner.getUniqueId()).ifPresent(p -> p.getMembers().forEach(member -> {
            Player player=Bukkit.getPlayer(member.asUuid());
            if (player != null && !player.isDead() && player.getWorld().equals(owner.getWorld()) && (includeSelf || !player.equals(owner))) result.add(player);
        }));
        if (includeSelf && !result.contains(owner)) result.add(owner);
        return result;
    }
    public Location origin(LivingEntity owner,EventContext context,Area area) {
        return switch(area.origin()) {
            case SELF -> owner.getLocation();
            case EVENT_TARGET -> context.target == null ? null : context.target.getLocation();
            case LOCATION -> context.location == null ? null : context.location.clone();
        };
    }
    public List<LivingEntity> inArea(Location origin,Area area) {
        if (origin == null || origin.getWorld() == null) return List.of();
        return origin.getWorld().getNearbyLivingEntities(origin,area.searchRadius(),e -> !e.isDead() && contains(origin,area,e.getLocation())).stream()
                .sorted(Comparator.comparing(e -> e.getUniqueId().toString())).toList();
    }
    public boolean contains(Location origin,Area area,Location point) {
        if (!origin.getWorld().equals(point.getWorld())) return false;
        Vector delta=point.toVector().subtract(origin.toVector());
        Vector forward=origin.getDirection().setY(0);
        if (forward.lengthSquared()<1e-10) forward=new Vector(0,0,1); else forward.normalize();
        Vector right=new Vector(forward.getZ(),0,-forward.getX());
        return area.contains(delta.dot(right),delta.getY(),delta.dot(forward));
    }
    public List<LivingEntity> select(String selector,LivingEntity owner,EventContext context,Map<String,Object> options) {
        List<LivingEntity> out=new ArrayList<>();
        switch(selector) {
            case "SELF", "SOURCE" -> out.add(owner);
            case "EVENT_TARGET", "OTHER" -> { if (context.target!=null) out.add(context.target); }
            case "ATTACKER" -> { if (context.attacker!=null) out.add(context.attacker); }
            case "VICTIM" -> { if (context.victim!=null) out.add(context.victim); }
            case "ALL_PARTY_MEMBERS", "ALL_PARTY_MEMBERS_AND_SELF" -> out.addAll(party(owner,selector.endsWith("AND_SELF")));
            case "LOWEST_HP_PARTY_MEMBER", "LOWEST_HP_PARTY_MEMBER_OR_SELF" -> {
                List<LivingEntity> members=party(owner,true);
                if (!hasParty(owner) && !selector.endsWith("OR_SELF")) members.clear();
                members.stream().min(Comparator.<LivingEntity>comparingDouble(e -> health(e)/Math.max(1,maxHealth(e)))
                        .thenComparingDouble(e -> e.getLocation().distanceSquared(owner.getLocation())).thenComparing(e -> e.getUniqueId().toString())).ifPresent(out::add);
            }
            case "NEAREST_ENEMY", "NEAREST_ALLY" -> owner.getWorld().getNearbyLivingEntities(owner.getLocation(),64,
                    e -> !e.equals(owner) && !e.isDead() && (selector.endsWith("ENEMY") ? enemy(owner,e) : ally(owner,e))).stream()
                    .min(Comparator.<LivingEntity>comparingDouble(e -> e.getLocation().distanceSquared(owner.getLocation())).thenComparing(e -> e.getUniqueId().toString())).ifPresent(out::add);
            case "ENTITIES_IN_AREA", "ALLIES_IN_AREA", "ENEMIES_IN_AREA" -> {
                Area area=TriggerDefinition.area(TriggerDefinition.child(options,"area"));
                out.addAll(inArea(origin(owner,context,area),area));
                if (selector.equals("ALLIES_IN_AREA")) out.removeIf(e -> !ally(owner,e));
                if (selector.equals("ENEMIES_IN_AREA")) out.removeIf(e -> !enemy(owner,e));
            }
            default -> throw new IllegalArgumentException("Unknown selector: " + selector);
        }
        String filter=TriggerDefinition.text(options,"target-filter","ALL");
        out.removeIf(e -> e.isDead() || !e.getWorld().equals(owner.getWorld()) || filter.equals("ENEMY") && !enemy(owner,e) || filter.equals("ALLY") && !ally(owner,e));
        if (options.containsKey("area")) {
            Area area=TriggerDefinition.area(TriggerDefinition.child(options,"area"));
            Location origin=origin(owner,context,area);
            out.removeIf(e -> origin==null || !contains(origin,area,e.getLocation()));
        }
        return List.copyOf(out);
    }
}
