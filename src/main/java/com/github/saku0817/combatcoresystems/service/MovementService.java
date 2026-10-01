package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.function.*;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

/** Server-side movement, bounded local queries, no chunk loads and no cross-world corrections. */
public final class MovementService {
    public static final Set<String> TYPES=Set.of("MOVE","DASH","LEAP","KNOCKBACK","PULL","TELEPORT");
    private final JavaPlugin plugin;
    private final TargetSelectorService selectors;
    private final Consumer<EventContext> events;
    private final Map<UUID,State> active=new HashMap<>();
    private final Set<UUID> cancelling=new HashSet<>();
    private boolean resetting;
    private static final class State {
        final LivingEntity owner,target;final UUID world;final Location start;Location previous;
        final Vector direction;final double distance;final int ticks;int elapsed;double traveled;
        final Map<String,Object> action;final EventContext context;final Consumer<LivingEntity> hit;final Runnable end;
        final Map<UUID,Integer> hits=new HashMap<>();
        State(LivingEntity owner,LivingEntity target,Vector direction,Map<String,Object> action,EventContext context,Consumer<LivingEntity> hit,Runnable end) {
            this.owner=owner;this.target=target;this.direction=direction;this.action=action;this.context=context;this.hit=hit;this.end=end;
            start=target.getLocation().clone();previous=start.clone();world=start.getWorld().getUID();
            distance=number(action,"distance",0);ticks=Math.max(1,(int)Math.ceil(number(action,"duration",0)*20));
        }
    }
    public MovementService(JavaPlugin plugin,TargetSelectorService selectors,Consumer<EventContext> events) {this.plugin=plugin;this.selectors=selectors;this.events=events;}
    public void start() {Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);}
    public static void validate(Map<String,Object> action) {
        String type=choice(action,"type","",TYPES);
        range(action,"distance",0,0,64);range(action,"duration",0,0,10);range(action,"strength",1,0,8);
        range(action,"horizontal",0,0,8);range(action,"vertical",0,-8,8);range(action,"hit-interval",.25,.05,10);
        if(Set.of("DASH","MOVE").contains(type)&&number(action,"distance",0)/Math.max(1,Math.ceil(number(action,"duration",0)*20))>8)
            throw new IllegalArgumentException("Movement speed must not exceed 8 blocks/tick");
        choice(action,"collision","STOP",Set.of("STOP"));
        choice(action,"hit-policy","ONCE_PER_TARGET",Set.of("ONCE_PER_TARGET","ONCE_PER_TICK","REPEAT"));
        choice(action,"path-target-filter","ENEMY",Set.of("ENEMY","ALLY","ALL"));
        choice(action,"direction",type.equals("KNOCKBACK")?"AWAY_FROM_SOURCE":"FORWARD",
                type.equals("KNOCKBACK")?Set.of("AWAY_FROM_SOURCE","TOWARD_SOURCE","SOURCE_LOOK","CUSTOM"):
                Set.of("FORWARD","BACKWARD","LEFT","RIGHT","TOWARD_TARGET","AWAY_FROM_TARGET","LOOK_DIRECTION","VECTOR"));
        choice(action,"destination","EVENT_TARGET",Set.of("SELF","EVENT_TARGET","LOCATION"));
        choice(action,"toward","SELF",Set.of("SELF","EVENT_TARGET"));
        for(String field:List.of("vector","offset","path-area","hit-area")) {
            var value=child(action,field);
            for(String key:value.keySet()) {
                Set<String> allowed=field.equals("vector")?Set.of("x","y","z"):field.equals("offset")?Set.of("forward","right","up"):
                        Set.of("shape","width","height");
                if(!allowed.contains(key))throw new IllegalArgumentException("Unknown movement "+field+" key: "+key);
                if(key.equals("shape")){choice(value,key,"BOX",Set.of("BOX"));continue;}
                range(value,key,0,field.endsWith("area")?0:-64,64);
            }
        }
    }
    private Vector direction(LivingEntity owner,LivingEntity target,EventContext context,Map<String,Object> action) {
        Vector look=target.getLocation().getDirection(),forward=look.clone().setY(0);
        if(forward.lengthSquared()<1e-10)forward=new Vector(0,0,1);else forward.normalize();
        Vector right=new Vector(-forward.getZ(),0,forward.getX());
        String type=text(action,"type","").toUpperCase(Locale.ROOT);
        String direction=text(action,"direction",type.equals("KNOCKBACK")?"AWAY_FROM_SOURCE":"FORWARD").toUpperCase(Locale.ROOT);
        if(type.equals("PULL")) {
            LivingEntity anchor=text(action,"toward","SELF").equalsIgnoreCase("SELF")?owner:context.target;
            return difference(target,anchor);
        }
        return switch(direction) {
            case "BACKWARD" -> forward.multiply(-1);case "LEFT" -> right.multiply(-1);case "RIGHT" -> right;
            case "LOOK_DIRECTION" -> look;case "SOURCE_LOOK" -> owner.getLocation().getDirection();
            case "TOWARD_TARGET" -> difference(target,context.target);case "AWAY_FROM_TARGET" -> negate(difference(target,context.target));
            case "TOWARD_SOURCE" -> difference(target,owner);case "AWAY_FROM_SOURCE" -> negate(difference(target,owner));
            case "VECTOR","CUSTOM" -> {var vector=child(action,"vector");yield new Vector(number(vector,"x",0),number(vector,"y",0),number(vector,"z",0));}
            default -> forward;
        };
    }
    private Vector negate(Vector vector) {return vector==null?null:vector.multiply(-1);}
    private Vector difference(LivingEntity from,LivingEntity to) {
        return to==null||!from.getWorld().equals(to.getWorld())?null:to.getLocation().toVector().subtract(from.getLocation().toVector());
    }
    public boolean execute(LivingEntity owner,LivingEntity target,Map<String,Object> action,EventContext context,Consumer<LivingEntity> hit,Runnable end) {
        if(resetting||cancelling.contains(owner.getUniqueId())||cancelling.contains(target.getUniqueId()))return false;
        if(owner.isDead()||target.isDead()||!owner.getWorld().equals(target.getWorld())||!selectors.ally(owner,target)&&!selectors.enemy(owner,target))return false;
        String type=text(action,"type","").toUpperCase(Locale.ROOT);
        if(type.equals("TELEPORT")) {
            Location destination=switch(text(action,"destination","EVENT_TARGET").toUpperCase(Locale.ROOT)) {
                case "SELF"->owner.getLocation();case "LOCATION"->context.location;default->context.target==null?null:context.target.getLocation();
            };
            if(destination==null||!destination.getWorld().equals(target.getWorld()))return false;
            destination=destination.clone();var offset=child(action,"offset");Vector forward=destination.getDirection().setY(0);
            if(forward.lengthSquared()>1e-10)forward.normalize();
            destination.add(forward.clone().multiply(number(offset,"forward",0)))
                    .add(new Vector(-forward.getZ(),0,forward.getX()).multiply(number(offset,"right",0))).add(0,number(offset,"up",0),0);
            if(!safe(target,destination))return false;
            cancel(target.getUniqueId(),"REPLACED");emit(context,TriggerEvent.MOVE_START,owner,target,type,"");
            boolean moved=target.teleport(destination,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            emit(context,TriggerEvent.MOVE_END,owner,target,type,moved?"COMPLETE":"CANCELLED");if(moved)end.run();return moved;
        }
        Vector vector=direction(owner,target,context,action);if(vector==null||vector.lengthSquared()<1e-10)return false;vector.normalize();
        cancel(target.getUniqueId(),"REPLACED");
        if(type.equals("DASH")||type.equals("MOVE")) {
            State state=new State(owner,target,vector,action,context,hit,end);active.put(target.getUniqueId(),state);
            emit(context,TriggerEvent.MOVE_START,owner,target,type,"");return true;
        }
        double strength=number(action,type.equals("LEAP")?"horizontal":"strength",type.equals("LEAP")?0:1);
        Vector velocity=vector.multiply(strength);velocity.setY(number(action,"vertical",type.equals("LEAP")?1:type.equals("PULL")?velocity.getY():.2));
        if(!safePath(target,target.getLocation(),target.getLocation().add(velocity)))return false;
        emit(context,TriggerEvent.MOVE_START,owner,target,type,"");target.setVelocity(velocity);
        emit(context,TriggerEvent.MOVE_END,owner,target,type,"COMPLETE");end.run();return true;
    }
    private void emit(EventContext parent,TriggerEvent event,LivingEntity owner,LivingEntity target,String type,String reason) {
        EventContext context=parent.child(event,owner,target);context.movementType=type;context.location=target.getLocation();
        context.values.put("movementEndReason",reason);events.accept(context);
    }
    private void tick() {
        for(State state:List.copyOf(active.values())) {
            UUID id=state.target.getUniqueId();if(active.get(id)!=state)continue;
            try {
            if(!state.owner.isValid()||!state.target.isValid()||state.owner.isDead()||state.target.isDead()) {finish(state,"UNAVAILABLE",false);continue;}
            if(!state.target.getWorld().getUID().equals(state.world)||!state.owner.getWorld().getUID().equals(state.world)){finish(state,"WORLD_CHANGE",false);continue;}
            Location now=state.target.getLocation();double moved=now.distance(state.previous);
            if(moved>8){finish(state,"EXTERNAL_MOVE",false);continue;}
            state.traveled+=moved;path(state,state.previous,now);state.previous=now;
            if(active.get(id)!=state)continue;
            if(state.elapsed>=state.ticks||state.traveled>=state.distance){finish(state,"COMPLETE",true);continue;}
            double step=Math.min(8,Math.max(0,state.distance-state.traveled)/Math.max(1,state.ticks-state.elapsed));
            Vector velocity=state.direction.clone().multiply(step);Location next=now.clone().add(velocity);
            if(!safePath(state.target,now,next)){finish(state,"COLLISION",true);continue;}
            state.target.setVelocity(velocity);state.elapsed++;
            emit(state.context,TriggerEvent.MOVE_TICK,state.owner,state.target,text(state.action,"type","DASH"),"");
            } catch(RuntimeException failure) {
                // One invalid callback must not stop the shared task for every moving entity.
                active.remove(id,state);
                try {if(state.target.isValid())state.target.setVelocity(new Vector());}
                catch(RuntimeException cleanupFailure) {failure.addSuppressed(cleanupFailure);}
                plugin.getLogger().log(java.util.logging.Level.WARNING,"Movement cancelled for "+id,failure);
            }
        }
    }
    private void path(State state,Location from,Location to) {
        if(!state.action.containsKey("path-actions")&&!state.action.containsKey("on-hit"))return;
        Map<String,Object> area=child(state.action,state.action.containsKey("path-area")?"path-area":"hit-area");
        double width=number(area,"width",1.5),height=number(area,"height",2.5);
        BoundingBox box=BoundingBox.of(from.toVector(),to.toVector()).expand(width/2,height,width/2);
        String filter=text(state.action,"path-target-filter","ENEMY").toUpperCase(Locale.ROOT);
        for(var raw:to.getWorld().getNearbyEntities(box))if(raw instanceof LivingEntity target&&!target.isDead()&&!target.equals(state.target)) {
            if(filter.equals("ENEMY")&&!selectors.enemy(state.owner,target)||filter.equals("ALLY")&&!selectors.ally(state.owner,target))continue;
            if(!MovementPath.intersects(from.toVector(),to.toVector(),target.getBoundingBox(),width,height))continue;
            String policy=text(state.action,"hit-policy","ONCE_PER_TARGET").toUpperCase(Locale.ROOT);
            Integer last=state.hits.get(target.getUniqueId());
            if(last!=null&&(policy.equals("ONCE_PER_TARGET")||policy.equals("REPEAT")&&state.elapsed-last<Math.ceil(number(state.action,"hit-interval",.25)*20)))continue;
            state.hits.put(target.getUniqueId(),state.elapsed);state.hit.accept(target);
            emit(state.context,TriggerEvent.MOVE_HIT,state.owner,target,text(state.action,"type","DASH"),"");
            if(active.get(state.target.getUniqueId())!=state)break;
        }
    }
    private boolean safePath(LivingEntity entity,Location start,Location end) {
        Vector delta=end.toVector().subtract(start.toVector());int steps=Math.max(1,(int)Math.ceil(delta.length()/.2));
        for(int i=1;i<=steps;i++)if(!safe(entity,start.clone().add(delta.clone().multiply((double)i/steps))))return false;
        return true;
    }
    private boolean safe(LivingEntity entity,Location point) {
        World world=point.getWorld();if(world==null||!world.equals(entity.getWorld())||!world.getWorldBorder().isInside(point))return false;
        BoundingBox box=entity.getBoundingBox().clone().shift(point.toVector().subtract(entity.getLocation().toVector()));
        if(box.getMinY()<world.getMinHeight()||box.getMaxY()>world.getMaxHeight())return false;
        for(int x=(int)Math.floor(box.getMinX());x<=Math.floor(box.getMaxX()-1e-7);x++)
            for(int z=(int)Math.floor(box.getMinZ());z<=Math.floor(box.getMaxZ()-1e-7);z++) {
                if(!world.isChunkLoaded(x>>4,z>>4))return false;
                for(int y=(int)Math.floor(box.getMinY());y<=Math.floor(box.getMaxY()-1e-7);y++) {
                    var block=world.getBlockAt(x,y,z);
                    if(!block.isPassable()&&block.getBoundingBox().overlaps(box))return false;
                }
            }
        return true;
    }
    private void finish(State state,String reason,boolean completed) {
        if(!active.remove(state.target.getUniqueId(),state))return;
        if(state.target.isValid())state.target.setVelocity(new Vector());
        if(state.owner.isValid()&&state.target.isValid())emit(state.context,TriggerEvent.MOVE_END,state.owner,state.target,text(state.action,"type","DASH"),reason);
        if(completed&&state.owner.isValid()&&!state.owner.isDead())state.end.run();
    }
    public void cancel(UUID entity,String reason) {
        if(!cancelling.add(entity))return;
        try {for(State state:List.copyOf(active.values()))if(state.owner.getUniqueId().equals(entity)||state.target.getUniqueId().equals(entity))cancelState(state,reason);}
        finally {cancelling.remove(entity);}
    }
    private void cancelState(State state,String reason) {
        try {finish(state,reason,false);}
        catch(RuntimeException failure) {plugin.getLogger().log(java.util.logging.Level.WARNING,"Movement cancellation callback failed",failure);}
    }
    public void reset() {
        if(resetting)return;
        resetting=true;
        try {for(State state:List.copyOf(active.values()))cancelState(state,"RELOAD");}
        finally {active.clear();resetting=false;}
    }
}
