package symbolicp.vs;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.MachineTag;

import java.util.ArrayList;
import java.util.List;

public class MachineRefVS implements ValueSummary<MachineRefVS>{
    public final PrimVS<MachineTag> tag;
    public final PrimVS<Integer> id;

    public MachineRefVS(PrimVS<MachineTag> tag, PrimVS<Integer> id) {
        this.tag = tag;
        this.id = id;
    }

    private static final MachineTag nullMachineTag = new MachineTag("null", -1);
    private static final MachineRefVS nullMachineRef = new MachineRefVS(new PrimVS<>(nullMachineTag), new PrimVS<>(-1));

    public static MachineRefVS nullMachineRef() {
        return nullMachineRef;
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(MachineRefVS other, Bdd pc) {
        return
                VSOps.symbolicEquals(tag, other.tag, pc).map2(
                        VSOps.symbolicEquals(id, other.id, pc),
                        (tagsEqual, idsEqual) -> tagsEqual && idsEqual);
    }

    @Override
    public MachineRefVS guard(Bdd cond) {
        return new MachineRefVS(VSOps.guard(tag, cond), VSOps.guard(id, cond));
    }

    @Override
    public MachineRefVS merge(MachineRefVS other) {
        return new MachineRefVS(VSOps.merge2(tag, other.tag), VSOps.merge2(id, other.id));
    }

    @Override
    public String toString() {
        return "MachineRefVS{" +
                "tag=" + tag +
                ", id=" + id +
                '}';
    }
}
