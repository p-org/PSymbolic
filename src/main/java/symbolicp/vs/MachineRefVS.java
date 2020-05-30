package symbolicp.vs;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.MachineTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MachineRefVS implements ValueSummary<MachineRefVS> {
    public final PrimVS<MachineTag> tag;
    public final PrimVS<Integer> id;

    public MachineRefVS(PrimVS<MachineTag> tag, PrimVS<Integer> id) {
        this.tag = tag;
        this.id = id;
    }

    public MachineRefVS(Bdd universe) {
        this();
    }

    public MachineRefVS() {
        tag = new PrimVS<MachineTag>();
        id = new PrimVS<Integer>();
    }

    private static final MachineTag nullMachineTag = new MachineTag("null", -1);
    private static final MachineRefVS nullMachineRef = new MachineRefVS(new PrimVS<>(nullMachineTag), new PrimVS<>(-1));

    public static MachineRefVS nullMachineRef() {
        return nullMachineRef;
    }

    @Override
    public boolean isEmpty() {
            return this.tag.isEmpty();
    }

    @Override
    public MachineRefVS guard(Bdd guard) {
        return new MachineRefVS(this.tag.guard(guard), this.id.guard(guard));
    }

    @Override
    public MachineRefVS merge(Iterable<MachineRefVS> summaries) {
        final List<PrimVS<MachineTag>> tagsToMerge = new ArrayList<>();
        final List<PrimVS<Integer>> idsToMerge = new ArrayList<>();
        for (MachineRefVS vs : summaries) {
            tagsToMerge.add(vs.tag);
            idsToMerge.add(vs.id);
        }
        return new MachineRefVS(tag.merge(tagsToMerge), id.merge(idsToMerge));
    }

    @Override
    public MachineRefVS update(Bdd guard, MachineRefVS update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(MachineRefVS cmp, Bdd pc) {
        return BoolUtils.and(tag.symbolicEquals(cmp.tag, pc), id.symbolicEquals(cmp.id, pc));
    }

    @Override
    public Bdd getUniverse() {
        return this.tag.getUniverse();
    }

    @Override
    public String toString() {
        return "MachineRefVS{" +
                "tag=" + tag +
                ", id=" + id +
                '}';
    }
}
