//package symbolicp.vs;
//
//import symbolicp.bdd.Bdd;
//import symbolicp.runtime.EventTag;
//
//import java.util.*;
//
//public class UnionVS<EventTag> extends UnionVS<EventTag> {
//
//    public UnionVS<EventTag>(PrimVS<EventTag> tag, Map<EventTag, Object> payloads) {
//        super(tag, payloads);
//    }
//
//    public UnionVS<EventTag>(Bdd pc, EventTag tag, Object payloads) {
//        super(pc, tag, payloads);
//    }
//
//    public PrimVS<EventTag> getTag() {
//        return super.getTag();
//    }
//
//    public Object getPayload(EventTag EventTag) {
//        return super.getPayload(EventTag);
//    }
//
//    public static class Ops implements ValueSummaryOps<UnionVS<EventTag>> {
//
//        private final UnionVS.Ops<EventTag> base;
//        public Ops(Object... tagsAndOps) {
//            base = new UnionVS.Ops<>(tagsAndOps);
//        }
//
//        @Override
//        public boolean isEmpty(UnionVS<EventTag> eventTagUnionVS) {
//            return base.isEmpty(eventTagUnionVS);
//        }
//
//        @Override
//        public UnionVS<EventTag> empty() {
//            return base.empty();
//        }
//
//        @Override
//        public UnionVS<EventTag> guard(UnionVS<EventTag> eventTagUnionVS, Bdd guard) {
//            return base.guard(eventTagUnionVS, guard);
//        }
//
//        @Override
//        public PrimVS<Boolean> symbolicEquals(UnionVS<EventTag> left, UnionVS<EventTag> right, Bdd pc) {
//            return base.symbolicEquals(left, right, pc);
//        }
//
//        @Override
//        public UnionVS<EventTag> merge(Iterable<UnionVS<EventTag>> unionVS) {
//            // Direct Casting Does not Work here
//            return base.merge(unionVS);
//        }
//
//    }
//}
