package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.vs.BoolUtils;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.ArrayList;
import java.util.List;

public class NondetUtil {

    public static PrimVS getNondetChoice(List<PrimVS> choices) {
        assert(choices.size() > 0);
        List<PrimVS> results = new ArrayList<>();
        PrimVS empty = choices.get(0).guard(Bdd.constFalse());

        Bdd residualPc = Bdd.constTrue();
        for (PrimVS choice : choices) {
            Bdd enabledCond = choice.getUniverse();
            Bdd choiceCond = Bdd.newVar().and(enabledCond);

            Bdd returnPc = residualPc.and(choiceCond);
            results.add(choice.guard(returnPc));

            residualPc = residualPc.and(choiceCond.not());
        }

        for (PrimVS choice : choices) {
            Bdd enabledCond = choice.getUniverse();

            Bdd returnPc = residualPc.and(enabledCond);
            results.add(choice.guard(returnPc));

            residualPc = residualPc.and(enabledCond.not());
        }

        final Bdd noneEnabledCond = residualPc;
        PrimVS<Boolean> isPresent = BoolUtils.fromTrueGuard(noneEnabledCond.not());

        assert(Checks.sameUniverse(noneEnabledCond.not(), empty.merge(results).getUniverse()));
        return empty.merge(results);
    }
}
