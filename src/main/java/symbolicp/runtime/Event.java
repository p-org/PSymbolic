package symbolicp.runtime;


import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;


public class Event implements ValueSummary<Event> {

    private final PrimVS<EventName> name;
    private final Map<EventName, ValueSummary> map;
    private final PrimVS<Machine> machine;

    public PrimVS<Boolean> canRun() {
        Bdd cond = Bdd.constFalse();
        for (GuardedValue<Machine> machine : getMachine().getGuardedValues()) {
            cond = cond.or(machine.value.hasStarted().getGuard(true).and(machine.guard));
            if (BoolUtils.isEverFalse(machine.value.hasStarted())) {
                Bdd unstarted = machine.value.hasStarted().getGuard(false).and(machine.guard);
                PrimVS<EventName> names = this.guard(unstarted).getName();
                for (GuardedValue<EventName> name : names.getGuardedValues()) {
                    if (name.value.equals(EventName.Init.instance)) {
                        cond = cond.or(name.guard);
                    }
                }
            }
        }
        return BoolUtils.fromTrueGuard(cond);
    }

    public Event getForMachine(Machine machine) {
        Bdd cond = this.machine.getGuard(machine);
        return this.guard(cond);
    }

    private Event(PrimVS<EventName> names, PrimVS<Machine> machine, Map<EventName, ValueSummary> map) {
        this.name = names;
        this.machine = machine;
        this.map = new HashMap<>(map);
    }

    public Event(EventName name, PrimVS<Machine> machine) {
        this(new PrimVS<>(name), machine, new HashMap<>());
    }

    public Event(PrimVS<EventName> name, PrimVS<Machine> machine) {
        this(name, machine, new HashMap<>());
    }

    public Event() {
        this(new PrimVS<>(), new PrimVS<>());
    }

    public Event(EventName name, PrimVS<Machine> machine, ValueSummary payload) {
        this(new PrimVS<>(name), machine, payload);
    }

    public Event(PrimVS<EventName> names, PrimVS<Machine> machine, ValueSummary payload) {
        this.name = names;
        this.machine = machine;
        this.map = new HashMap<>();
        for (GuardedValue<EventName> name : names.getGuardedValues()) {
            assert(!this.map.containsKey(name));
            if (payload != null) {
                this.map.put(name.value, payload.guard(name.guard));
            }
        }
    }

    public PrimVS<EventName> getName() {
        return this.name;
    }

    public PrimVS<Machine> getMachine() {
        return this.machine;
    }

    public ValueSummary getPayload() {
        List<GuardedValue<EventName>> names = this.name.getGuardedValues();
        assert(names.size() <= 1);
        if (names.size() == 0) {
            return null;
        } else {
            return map.getOrDefault(names.get(0).value, null);
        }
    }

    @Override
    public boolean isEmptyVS() {
        return name.isEmptyVS();
    }

    @Override
    public Event guard(Bdd guard) {
        Map<EventName, ValueSummary> newMap = new HashMap<>();
        PrimVS<EventName> newName = name.guard(guard);
        for (Map.Entry<EventName, ValueSummary> entry : map.entrySet()) {
            if (!newName.getGuard(entry.getKey()).isConstFalse()) {
                newMap.put(entry.getKey(), entry.getValue().guard(guard));
            }
        }
        return new Event(name.guard(guard), machine.guard(guard), map);
    }

    @Override
    public Event merge(Iterable<Event> summaries) {
        List<PrimVS<EventName>> namesToMerge = new ArrayList<>();
        List<PrimVS<Machine>> machinesToMerge = new ArrayList<>();
        Map<EventName, ValueSummary> newMap = new HashMap<>();

        for (Map.Entry<EventName, ValueSummary> entry : this.map.entrySet()) {
            if (!name.getGuard(entry.getKey()).isConstFalse() && entry.getValue() != null) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }

        for (Event summary : summaries) {
            namesToMerge.add(summary.name);
            machinesToMerge.add(summary.machine);
            for (Map.Entry<EventName, ValueSummary> entry : summary.map.entrySet()) {
                newMap.computeIfPresent(entry.getKey(), (key, value) -> value.merge(summary.map.get(key)));
                if (entry.getValue() != null)
                    newMap.putIfAbsent(entry.getKey(), entry.getValue());
                if (newMap.containsKey(entry.getKey()) && newMap.get(entry.getKey()) == null) {
                    assert(false);
                }
            }
        }

        return new Event(name.merge(namesToMerge), machine.merge(machinesToMerge), newMap);
    }

    @Override
    public Event merge(Event summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public Event update(Bdd guard, Event update) {
        if (guard.isConstTrue()) {
            assert (this.guard(guard.not()).merge(update.guard(guard)).symbolicEquals(update, guard).getGuard(true).isConstTrue());
        }
        return this.guard(guard.not()).merge(update.guard(guard));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(Event cmp, Bdd pc) {
        return BoolUtils.and(
                BoolUtils.and(this.name.symbolicEquals(cmp.name, pc), this.machine.symbolicEquals(cmp.machine, pc)),
                new PrimVS<>(this.map.equals(cmp.map)).guard(pc));
    }

    @Override
    public Bdd getUniverse() {
        return name.getUniverse();
    }

    @Override
    public String toString() {
        String str = "[";
        int i = 0;
        for (GuardedValue<EventName> name : getName().getGuardedValues()) {
            //ScheduleLogger.log("name: " + name.value + " mach: " + this.guard(name.guard).getMachine());
            //if (getMachine().guard(name.guard).getGuardedValues().size() > 1) assert(false);
            str += getMachine().guard(name.guard);
            if (map.size() > 0 && map.containsKey(name.value)) {
                str += " -- ";
                str += map.get(name.value);
            }
            if (i < getName().getGuardedValues().size() - 1)
                str += System.lineSeparator();
        }
        return str + "]";
    }

}
