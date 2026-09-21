package com.github.saku0817.combatcoresystems.service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Transient owner/definition-isolated stacks; server-thread access only. */
public final class StackService {
    public enum Reapply { REFRESH, EXTEND, IGNORE }
    public enum Overflow { REMOVE_OLDEST, REMOVE_NEWEST, REMOVE_LOWEST_STACK, REJECT_NEW }
    public record Key(UUID owner, String source, String id, UUID target) {}
    public record State(long createdAt, long updatedAt, int count, long expiresAt) {}
    public record Change(Key key, int oldStacks, int newStacks) {}
    public record Policy(int maxTargets, Overflow overflow) {
        public Policy { if (maxTargets < 0) throw new IllegalArgumentException("negative max-targets"); Objects.requireNonNull(overflow); }
    }
    private final Map<Key, State> states = new LinkedHashMap<>();
    private final LongSupplier clock;
    private Consumer<Change> listener = ignored -> {};
    public StackService() { this(System::currentTimeMillis); }
    public StackService(LongSupplier clock) { this.clock = clock; }
    public void onChange(Consumer<Change> listener) { this.listener = Objects.requireNonNull(listener); }
    public State state(Key key) { expire(key); return states.get(key); }
    public int count(Key key) { State state = state(key); return state == null ? 0 : state.count(); }
    public long total(UUID owner, String source, String id) {
        expire();
        return states.entrySet().stream().filter(e -> e.getKey().target()!=null && matches(e.getKey(), owner, source, id))
                .mapToLong(e -> e.getValue().count()).sum();
    }
    private static boolean matches(Key key, UUID owner, String source, String id) {
        return key.owner().equals(owner) && key.source().equals(source) && key.id().equals(id);
    }
    public int add(Key key, int amount, int max, long durationMillis, Reapply reapply, Policy policy) {
        if (amount < 0) throw new IllegalArgumentException("negative stack amount");
        return set(key, (int) Math.min(max, (long) count(key) + amount), max, durationMillis, reapply, policy);
    }
    public int set(Key key, int amount, int max, long durationMillis, Reapply reapply, Policy policy) {
        if (max < 1 || amount < 0 || durationMillis < 0) throw new IllegalArgumentException("invalid stack bounds");
        expire(key);
        State old = states.get(key);
        int before = old == null ? 0 : old.count(), after = Math.min(max, amount);
        if (after == 0) { remove(key); return 0; }
        if (old == null && key.target() != null && policy != null && policy.maxTargets() > 0) {
            List<Map.Entry<Key, State>> targets = states.entrySet().stream()
                    .filter(e -> e.getKey().target() != null && matches(e.getKey(), key.owner(), key.source(), key.id())).toList();
            if (targets.size() >= policy.maxTargets()) {
                if (policy.overflow() == Overflow.REJECT_NEW || policy.overflow() == Overflow.REMOVE_NEWEST) return 0;
                Comparator<Map.Entry<Key, State>> order = Comparator.comparingLong(e -> e.getValue().createdAt());
                if (policy.overflow() == Overflow.REMOVE_LOWEST_STACK)
                    order = Comparator.<Map.Entry<Key, State>>comparingInt(e -> e.getValue().count()).thenComparing(order);
                for (var entry : targets.stream().sorted(order).limit(targets.size()-policy.maxTargets()+1L).toList()) remove(entry.getKey());
            }
        }
        long now = clock.getAsLong();
        long expires = durationMillis == 0 ? Long.MAX_VALUE : saturatingAdd(now, durationMillis);
        if (old != null && reapply == Reapply.IGNORE) expires = old.expiresAt();
        if (old != null && reapply == Reapply.EXTEND) expires = durationMillis == 0 ? Long.MAX_VALUE : saturatingAdd(old.expiresAt(), durationMillis);
        states.put(key, new State(old == null ? now : old.createdAt(), now, after, expires));
        if (before != after) listener.accept(new Change(key, before, after));
        return after;
    }
    private static long saturatingAdd(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    public int consume(Key key, int amount) {
        if (amount < 0) throw new IllegalArgumentException("negative consumption");
        State state = state(key);
        if (state == null) return 0;
        int used = Math.min(amount, state.count());
        if (used == state.count()) remove(key);
        else if (used > 0) {
            states.put(key, new State(state.createdAt(), clock.getAsLong(), state.count() - used, state.expiresAt()));
            listener.accept(new Change(key, state.count(), state.count() - used));
        }
        return used;
    }
    public int clear(Key key) { expire(key); return remove(key); }
    public long clearTargets(UUID owner, String source, String id) {
        expire(); long total = 0;
        for (Key key : List.copyOf(states.keySet())) if (key.target() != null && matches(key, owner, source, id)) total += remove(key);
        return total;
    }
    private int remove(Key key) {
        State old = states.remove(key);
        if (old == null) return 0;
        listener.accept(new Change(key, old.count(), 0)); return old.count();
    }
    private void expire(Key key) { State state = states.get(key); if (state != null && state.expiresAt() <= clock.getAsLong()) remove(key); }
    public void expire() { for (Key key : List.copyOf(states.keySet())) expire(key); }
    public void forget(UUID entity) { states.keySet().removeIf(key -> key.owner().equals(entity) || entity.equals(key.target())); }
    public void reset() { states.clear(); }
}
