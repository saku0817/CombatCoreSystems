package com.github.saku0817.combatcoresystems.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class StackServiceTest {
    final AtomicLong now = new AtomicLong(100);
    final StackService stacks = new StackService(now::get);
    final UUID owner = UUID.randomUUID();
    final StackService.Policy unlimited = new StackService.Policy(0,StackService.Overflow.REMOVE_OLDEST);
    StackService.Key key(UUID target) { return new StackService.Key(owner,"weapon:test","aim",target); }
    void add(StackService.Key key,StackService.Policy policy) { stacks.add(key,1,5,0,StackService.Reapply.REFRESH,policy); }
    @Test void targetIsolationAndCreationOrder() {
        var a=key(UUID.randomUUID()); var b=key(UUID.randomUUID()); var c=key(UUID.randomUUID()); var d=key(UUID.randomUUID());
        var policy=new StackService.Policy(3,StackService.Overflow.REMOVE_OLDEST);
        add(a,policy); now.incrementAndGet(); add(b,policy); now.incrementAndGet(); add(c,policy);
        now.incrementAndGet(); add(a,policy);
        assertEquals(100,stacks.state(a).createdAt()); assertEquals(103,stacks.state(a).updatedAt()); assertEquals(2,stacks.count(a));
        add(d,policy); assertEquals(0,stacks.count(a)); assertEquals(1,stacks.count(b)); assertEquals(1,stacks.count(c)); assertEquals(1,stacks.count(d));
    }
    @Test void oneTargetAndRejectNew() {
        var a=key(UUID.randomUUID()); var b=key(UUID.randomUUID());
        add(a,new StackService.Policy(1,StackService.Overflow.REJECT_NEW));
        add(b,new StackService.Policy(1,StackService.Overflow.REJECT_NEW)); assertEquals(0,stacks.count(b));
        add(b,new StackService.Policy(1,StackService.Overflow.REMOVE_OLDEST)); assertEquals(0,stacks.count(a)); assertEquals(1,stacks.count(b));
    }
    @Test void expiryAndRefresh() {
        var a=key(null);
        stacks.add(a,1,5,100,StackService.Reapply.REFRESH,unlimited);
        now.set(150); stacks.add(a,1,5,100,StackService.Reapply.REFRESH,unlimited);
        now.set(200); assertEquals(2,stacks.count(a)); now.set(250); assertEquals(0,stacks.count(a));
    }
    @Test void consumeReturnsActualTotalAndDoesNotClearSelf() {
        var a=key(UUID.randomUUID()); var b=key(UUID.randomUUID()); var self=key(null);
        stacks.set(a,3,5,0,StackService.Reapply.REFRESH,unlimited); stacks.set(b,2,5,0,StackService.Reapply.REFRESH,unlimited); add(self,unlimited);
        assertEquals(5,stacks.clearTargets(owner,"weapon:test","aim")); assertEquals(1,stacks.count(self));
        assertEquals(1,stacks.consume(self,3)); assertEquals(0,stacks.consume(self,3));
    }
    @Test void ownerAndDefinitionIsolation() {
        var target=UUID.randomUUID(); var a=key(target);
        var otherOwner=new StackService.Key(UUID.randomUUID(),"weapon:test","aim",target);
        var otherWeapon=new StackService.Key(owner,"weapon:other","aim",target);
        add(a,unlimited); add(otherOwner,unlimited); add(otherWeapon,unlimited);
        stacks.clear(a); assertEquals(1,stacks.count(otherOwner)); assertEquals(1,stacks.count(otherWeapon));
    }
    @Test void changesOnlyOnActualChange() {
        List<StackService.Change> changes=new ArrayList<>(); stacks.onChange(changes::add);
        var a=key(null); stacks.set(a,5,5,0,StackService.Reapply.REFRESH,unlimited); add(a,unlimited);
        assertEquals(1,changes.size()); assertEquals(0,changes.getFirst().oldStacks()); assertEquals(5,changes.getFirst().newStacks());
    }
}
