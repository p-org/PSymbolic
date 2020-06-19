package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.run.Assert;
import symbolicp.vs.*;

import java.util.*;
import java.util.stream.Collectors;

public class Schedule {
    private List<PrimVS<Machine>> senderChoice = new ArrayList<>();
    private Map<Class<? extends Machine>, ListVS<PrimVS<Machine>>> createdMachines = new HashMap<>();
    private Set<Machine> machines = new HashSet<>();
    private ListVS<PrimVS<Boolean>> boolChoice = new ListVS<>(Bdd.constTrue());
    private ListVS<PrimVS<Integer>> intChoice = new ListVS<>(Bdd.constTrue());
    private Bdd pc = Bdd.constTrue();

    public Schedule() {}

    private Schedule(List<PrimVS<Machine>> senderChoice,
                     ListVS<PrimVS<Boolean>> boolChoice,
                     ListVS<PrimVS<Integer>> intChoice,
                     Map<Class<? extends Machine>, ListVS<PrimVS<Machine>>> createdMachines,
                     Set<Machine> machines,
                     Bdd pc) {
        this.senderChoice = new ArrayList<>(senderChoice);
        this.boolChoice = boolChoice;
        this.intChoice = intChoice;
        this.createdMachines = new HashMap<>(createdMachines);
        this.machines = new HashSet<>(machines);
        this.pc = pc;
    }

    public int size() {
        return senderChoice.size();
    }

    public Set<Machine> getMachines() { return machines; }

    public Schedule guard(Bdd pc) {
        List<PrimVS<Machine>> newSenderChoice = senderChoice.stream().map(x -> x.guard(pc)).collect(Collectors.toList());
        return new Schedule(newSenderChoice, boolChoice.guard(pc), intChoice.guard(pc), createdMachines, machines, pc);
    }

    public void makeMachine(Machine m, Bdd pc) {
        PrimVS<Machine> toAdd = new PrimVS<>(m).guard(pc);
        if (createdMachines.containsKey(m.getClass())) {
            createdMachines.put(m.getClass(), createdMachines.get(m.getClass()).add(toAdd));
        } else {
            createdMachines.put(m.getClass(), new ListVS<PrimVS<Machine>>(Bdd.constTrue()).add(toAdd));
        }
        machines.add(m);
    }

    public void scheduleSender(PrimVS<Machine> choice) {
        senderChoice.add(choice);
    }

    public void scheduleBoolChoice(PrimVS<Boolean> choice) {
        boolChoice = boolChoice.add(choice);
    }

    public void scheduleIntChoice(PrimVS<Integer> choice) {
        intChoice = intChoice.add(choice);
    }

    public PrimVS<Machine> getMachine(Class<? extends Machine> type, PrimVS<Integer> idx) {
        PrimVS<Machine> machines = createdMachines.get(type).get(idx);
        return machines.guard(pc);
    }

    public PrimVS<Machine> getSender(int i) {
        return senderChoice.get(i).guard(pc);
    }

    public PrimVS<Boolean> getBoolChoice(int i) {
        PrimVS<Boolean> res = boolChoice.get(new PrimVS<>(i).guard(boolChoice.getUniverse()));
        assert(res.getGuardedValues().size() == 1);
        return res.guard(pc);
    }

    public PrimVS<Integer> getIntChoice(int i) {
        return intChoice.get(new PrimVS<>(i).guard(intChoice.getUniverse())).guard(pc);
    }

    public Bdd getLengthCond(int length) {
        assert(length <= size());
        if (length == 0) return Bdd.constTrue();
        Bdd pc = senderChoice.get(length - 1).getUniverse();
        return pc;
    }

    public Schedule getSingleSchedule() {
        Bdd pc = Bdd.constTrue();
        for (PrimVS<Machine> choice : senderChoice) {
            PrimVS<Machine> guarded = choice.guard(pc);
            if (guarded.getGuardedValues().size() > 0) {
                pc = pc.and(guarded.getGuardedValues().get(0).guard);
            }
        }
        ListVS<PrimVS<Integer>> guardedIntChoice = intChoice.guard(pc);
        PrimVS<Integer> idx = new PrimVS<>(0);
        while (BoolUtils.isEverTrue(IntUtils.lessThan(idx, guardedIntChoice.size()))) {
            Bdd cond = IntUtils.lessThan(idx, guardedIntChoice.size()).getGuard(true);
            PrimVS<Integer> choice = guardedIntChoice.get(idx.guard(cond));
            if (choice.getGuardedValues().size() > 0) {
                pc = pc.and(choice.getGuardedValues().get(0).guard);
            }
            idx = IntUtils.add(idx, 1);
        }
        assert(intChoice.guard(pc).size().getGuardedValues().size() <= 1);
        ListVS<PrimVS<Boolean>> guardedBoolChoice = boolChoice.guard(pc);
        idx = new PrimVS<>(0);
        while (BoolUtils.isEverTrue(IntUtils.lessThan(idx, guardedBoolChoice.size()))) {
            Bdd cond = IntUtils.lessThan(idx, guardedBoolChoice.size()).getGuard(true).and(pc);
            PrimVS<Boolean> choice = guardedBoolChoice.get(idx.guard(cond));
            if (choice.getGuardedValues().size() > 0) {
                assert(!choice.getGuardedValues().get(0).guard.isConstFalse());
                pc = pc.and(choice.getGuardedValues().get(0).guard);
                assert(!pc.isConstFalse());
            }
            idx = IntUtils.add(idx, 1);
        }
        assert(boolChoice.guard(pc).size().getGuardedValues().size() <= 1);

        return this.guard(pc);
    }

}
