package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.ArrayList;
import java.util.List;

public class MachineRefVS<MachineTag> {
    public final PrimVS<MachineTag> tag;
    public final PrimVS<Integer> id;

    public MachineRefVS(PrimVS<MachineTag> tag, PrimVS<Integer> id) {
        this.tag = tag;
        this.id = id;
    }

    public static class Ops<MachineTag> implements ValueSummaryOps<MachineRefVS<MachineTag>> {
        private final PrimVS.Ops<MachineTag> tagOps = new PrimVS.Ops<>();
        private final PrimVS.Ops<Integer> intOps = new PrimVS.Ops<>();

        @Override
        public boolean isEmpty(MachineRefVS<MachineTag> vs) {
            return tagOps.isEmpty(vs.tag);
        }

        @Override
        public MachineRefVS<MachineTag> empty() {
            return new MachineRefVS<>(tagOps.empty(), intOps.empty());
        }

        @Override
        public MachineRefVS<MachineTag> guard(MachineRefVS<MachineTag> vs, Bdd guard) {
            return new MachineRefVS<>(tagOps.guard(vs.tag, guard), intOps.guard(vs.id, guard));
        }

        @Override
        public MachineRefVS<MachineTag> merge(Iterable<MachineRefVS<MachineTag>> summaries) {
            final List<PrimVS<MachineTag>> tagsToMerge = new ArrayList<>();
            final List<PrimVS<Integer>> idsToMerge = new ArrayList<>();
            for (MachineRefVS<MachineTag> vs : summaries) {
                tagsToMerge.add(vs.tag);
                idsToMerge.add(vs.id);
            }
            return new MachineRefVS<>(tagOps.merge(tagsToMerge), intOps.merge(idsToMerge));
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(MachineRefVS<MachineTag> left, MachineRefVS<MachineTag> right, Bdd pc) {
            return
                tagOps.symbolicEquals(left.tag, right.tag, pc).map2(
                    intOps.symbolicEquals(left.id, right.id, pc),
                    (tagsEqual, idsEqual) -> tagsEqual && idsEqual);
        }
    }
}
