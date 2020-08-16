package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;
import java.util.stream.Collectors;

public class Schedule {

    public interface Choice {}

    public class RepeatChoice implements Choice {
        PrimVS<Machine> senderChoice = new PrimVS<>();
        PrimVS<Boolean> boolChoice = new PrimVS<>();
        PrimVS<Integer> intChoice = new PrimVS<>();

        public RepeatChoice() {
        }

        public RepeatChoice(PrimVS<Machine> senderChoice, PrimVS<Boolean> boolChoice, PrimVS<Integer> intChoice) {
            this.senderChoice = senderChoice;
            this.boolChoice = boolChoice;
            this.intChoice = intChoice;
        }

        public Bdd getUniverse() {
            return senderChoice.getUniverse().or(boolChoice.getUniverse().or(intChoice.getUniverse()));
        }

        public RepeatChoice guard(Bdd pc) {
            RepeatChoice c = new RepeatChoice();
            c.senderChoice = senderChoice.guard(pc);
            c.boolChoice = boolChoice.guard(pc);
            c.intChoice = intChoice.guard(pc);
            return c;
        }

        public void addSenderChoice(PrimVS<Machine> choice) {
            senderChoice = choice;
        }

        public void addBoolChoice(PrimVS<Boolean> choice) {
            boolChoice = choice;
        }

        public void addIntChoice(PrimVS<Integer> choice) {
            intChoice = choice;
        }

        public void clear() {
            senderChoice = new PrimVS<>();
            boolChoice = new PrimVS<>();
            intChoice = new PrimVS<>();
        }
    }

    public class Choices implements Choice {
        List<PrimVS<Machine>> senderChoice = new ArrayList<>();
        List<PrimVS<Boolean>> boolChoice = new ArrayList<>();
        List<PrimVS<Integer>> intChoice = new ArrayList<>();

        public Choices() {
        }

        public Choices guard(Bdd pc) {
            Choices c = new Choices();
            c.senderChoice = senderChoice.stream().map(x -> x.guard(pc)).collect(Collectors.toList());
            c.boolChoice = new ArrayList<>(boolChoice);
            c.intChoice = new ArrayList<>(intChoice);
            return c;
        }

        public Choices(List<PrimVS<Machine>> senderChoice, List<PrimVS<Boolean>> boolChoice, List<PrimVS<Integer>> intChoice) {
            this.senderChoice = new ArrayList<>(senderChoice);
            this.boolChoice = new ArrayList<>(boolChoice);
            this.intChoice = new ArrayList<>(intChoice);
        }

        public boolean isEmpty() {
            return senderChoice.isEmpty() && boolChoice.isEmpty() && intChoice.isEmpty();
        }

        public void addSenderChoice(List<PrimVS<Machine>> choice) {
            senderChoice = new ArrayList<>(choice);
        }

        public void addBoolChoice(List<PrimVS<Boolean>> choice) {
            boolChoice = new ArrayList<>(choice);
        }

        public void addIntChoice(List<PrimVS<Integer>> choice) {
            intChoice = new ArrayList<>(choice);
            ;
        }

        public void clear() {
            senderChoice = new ArrayList<>();
            boolChoice = new ArrayList<>();
            intChoice = new ArrayList<>();
        }
    }

    List<Choices> fullChoice = new ArrayList<>();
    List<RepeatChoice> repeatChoice = new ArrayList<>();
    List<Choices> backtrackChoice = new ArrayList<>();

    public void addSenderChoice(List<PrimVS<Machine>> choice, int depth) {
        if (depth >= fullChoice.size()) {
            fullChoice.add(new Choices());
        }
        fullChoice.get(depth).addSenderChoice(choice);
    }

    public void addBoolChoice(List<PrimVS<Boolean>> choice, int depth) {
        if (depth >= fullChoice.size()) {
            fullChoice.add(new Choices());
        }
        fullChoice.get(depth).addBoolChoice(choice);
    }

    public void addIntChoice(List<PrimVS<Integer>> choice, int depth) {
        if (depth >= fullChoice.size()) {
            fullChoice.add(new Choices());
        }
        fullChoice.get(depth).addIntChoice(choice);
    }

    public void addRepeatSender(PrimVS<Machine> choice, int depth) {
        if (depth >= repeatChoice.size()) {
            repeatChoice.add(new RepeatChoice());
        }
        repeatChoice.get(depth).addSenderChoice(choice);
    }

    public void addRepeatBool(PrimVS<Boolean> choice, int depth) {
        if (depth >= repeatChoice.size()) {
            repeatChoice.add(new RepeatChoice());
        }
        repeatChoice.get(depth).addBoolChoice(choice);
    }

    public void addRepeatInt(PrimVS<Integer> choice, int depth) {
        if (depth >= repeatChoice.size()) {
            repeatChoice.add(new RepeatChoice());
        }
        repeatChoice.get(depth).addIntChoice(choice);
    }

    public void addBacktrackSender(List<PrimVS<Machine>> choice, int depth) {
        if (depth >= backtrackChoice.size()) {
            backtrackChoice.add(new Choices());
        }
        backtrackChoice.get(depth).addSenderChoice(choice);
    }

    public void addBacktrackBool(List<PrimVS<Boolean>> choice, int depth) {
        if (depth >= backtrackChoice.size()) {
            backtrackChoice.add(new Choices());
        }
        backtrackChoice.get(depth).addBoolChoice(choice);
    }

    public void addBacktrackInt(List<PrimVS<Integer>> choice, int depth) {
        if (depth >= backtrackChoice.size()) {
            backtrackChoice.add(new Choices());
        }
        backtrackChoice.get(depth).addIntChoice(choice);
    }

    public List<PrimVS<Machine>> getSenderChoice(int depth) {
        return fullChoice.get(depth).senderChoice;
    }

    public List<PrimVS<Boolean>> getBoolChoice(int depth) {
        return fullChoice.get(depth).boolChoice;
    }

    public List<PrimVS<Integer>> getIntChoice(int depth) {
        return fullChoice.get(depth).intChoice;
    }

    public PrimVS<Machine> getRepeatSender(int depth) {
        return repeatChoice.get(depth).senderChoice;
    }

    public PrimVS<Boolean> getRepeatBool(int depth) {
        return repeatChoice.get(depth).boolChoice;
    }

    public PrimVS<Integer> getRepeatInt(int depth) {
        return repeatChoice.get(depth).intChoice;
    }

    public List<PrimVS<Machine>> getBacktrackSender(int depth) {
        return backtrackChoice.get(depth).senderChoice;
    }

    public List<PrimVS<Boolean>> getBacktrackBool(int depth) {
        return backtrackChoice.get(depth).boolChoice;
    }

    public List<PrimVS<Integer>> getBacktrackInt(int depth) {
        return backtrackChoice.get(depth).intChoice;
    }

    public void clearChoice(int depth) {
        fullChoice.get(depth).clear();
    }

    public void clearRepeat(int depth) {
        repeatChoice.get(depth).clear();
    }

    public void clearBacktrack(int depth) {
        backtrackChoice.get(depth).clear();
    }

    public int size() {
        return fullChoice.size();
    }

    private Map<Class<? extends Machine>, ListVS<PrimVS<Machine>>> createdMachines = new HashMap<>();
    private Set<Machine> machines = new HashSet<>();

    private Bdd pc = Bdd.constTrue();

    public Schedule() {
    }

    private Schedule(List<Choices> fullChoice,
                     List<RepeatChoice> repeatChoice,
                     List<Choices> backtrackChoice,
                     Map<Class<? extends Machine>, ListVS<PrimVS<Machine>>> createdMachines,
                     Set<Machine> machines,
                     Bdd pc) {
        this.fullChoice = new ArrayList<>(fullChoice);
        this.repeatChoice = new ArrayList<>(repeatChoice);
        this.backtrackChoice = new ArrayList<>(backtrackChoice);
        this.createdMachines = new HashMap<>(createdMachines);
        this.machines = new HashSet<>(machines);
        this.pc = pc;
    }

    public Set<Machine> getMachines() {
        return machines;
    }

    public Schedule guard(Bdd pc) {
        List<Choices> newFullChoice = new ArrayList<>();
        List<RepeatChoice> newRepeatChoice = new ArrayList<>();
        List<Choices> newBacktrackChoice = new ArrayList<>();
        for (Choices c : fullChoice) {
            newFullChoice.add(c.guard(pc));
        }
        for (RepeatChoice c : repeatChoice) {
            newRepeatChoice.add(c.guard(pc));
        }
        for (Choices c : backtrackChoice) {
            newBacktrackChoice.add(c.guard(pc));
        }
        return new Schedule(newFullChoice, newRepeatChoice, newBacktrackChoice, createdMachines, machines, pc);
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

    public boolean hasMachine(Class<? extends Machine> type, PrimVS<Integer> idx, Bdd otherPc) {
        if (!createdMachines.containsKey(type)) return false;
        // TODO: may need fixing
        ScheduleLogger.log("has machine of type");
        ScheduleLogger.log(idx + " in range? " + createdMachines.get(type).inRange(idx).getGuard(false));
        if (!createdMachines.get(type).inRange(idx).getGuard(false).isConstFalse()) return false;
        PrimVS<Machine> machines = createdMachines.get(type).get(idx);
        return !machines.guard(pc).guard(otherPc).getUniverse().isConstFalse();
    }

    public PrimVS<Machine> getMachine(Class<? extends Machine> type, PrimVS<Integer> idx) {
        PrimVS<Machine> machines = createdMachines.get(type).get(idx);
        return machines.guard(pc);
    }

    public Schedule getSingleSchedule() {
        Bdd pc = Bdd.constTrue();
        for (RepeatChoice choice : repeatChoice) {
            RepeatChoice guarded = choice.guard(pc);
            PrimVS<Machine> sender = guarded.senderChoice;
            if (sender.getGuardedValues().size() > 0) {
                pc = pc.and(sender.getGuardedValues().get(0).guard);
            } else {
                PrimVS<Boolean> boolChoice = guarded.boolChoice;
                if (boolChoice.getGuardedValues().size() > 0) {
                    pc = pc.and(boolChoice.getGuardedValues().get(0).guard);
                } else {
                    PrimVS<Integer> intChoice = guarded.intChoice;
                    if (intChoice.getGuardedValues().size() > 0) {
                        pc = pc.and(intChoice.getGuardedValues().get(0).guard);
                    }
                }
            }
        }
        return this.guard(pc);
    }

    public Bdd getLengthCond(int size) {
        if (size == 0) return Bdd.constFalse();
        return repeatChoice.get(size - 1).getUniverse();
    }
}
