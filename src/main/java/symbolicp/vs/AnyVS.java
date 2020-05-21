//package symbolicp.vs;
//
//import symbolicp.bdd.Bdd;
//import symbolicp.runtime.TypeTag;
//
//import java.util.*;
//
//public class AnyVS extends UnionVS<TypeTag> {
//
//    public AnyVS(PrimVS<TypeTag> tag, Map<TypeTag, Object> payloads) {
//        super(tag, payloads);
//    }
//
//    public AnyVS(Bdd pc, TypeTag tag, Object payloads) {
//        super(pc, tag, payloads);
//    }
//
//    public PrimVS<TypeTag> getTag() {
//        return super.getTag();
//    }
//
//    public Object getPayload(TypeTag typeTag) {
//        return super.getPayload(typeTag);
//    }
//
//    public static class Ops implements ValueSummaryOps<AnyVS> {
//
//        private final UnionVS.Ops<TypeTag> base;
//        public Ops(Object... tagsAndOps) {
//            base = new UnionVS.Ops<>(tagsAndOps);
//        }
//
//        @Override
//        public boolean isEmpty(AnyVS typeTagUnionVS) {
//            return base.isEmpty(typeTagUnionVS);
//        }
//
//        @Override
//        public AnyVS empty() {
//            return (AnyVS) base.empty();
//        }
//
//        @Override
//        public AnyVS guard(AnyVS anyVS, Bdd guard) {
//            return (AnyVS) base.guard(anyVS, guard);
//        }
//
//        @Override
//        public AnyVS merge(Iterable<AnyVS> anyVS) {
//            // Direct Casting Does not Work here
//            List<UnionVS<TypeTag>> list = new ArrayList<>();
//            anyVS.iterator().forEachRemaining(list::add);
//            return (AnyVS) base.merge(list);
//        }
//
//        @Override
//        public PrimVS<Boolean> symbolicEquals(AnyVS left, AnyVS right, Bdd pc) {
//            return base.symbolicEquals(left, right, pc);
//        }
//    }
//}
