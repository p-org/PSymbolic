package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.vs.BoolUtils;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NondetUtil {

    private static int log2(int bits)
    {
        if( bits == 0 )
            return 0; // or throw exception
        return 31 - Integer.numberOfLeadingZeros(bits);
    }

    private static List<Bdd> generateAllCombos(List<Bdd> bdds) {
        Bdd thisBdd = bdds.get(0);
        List<Bdd> remaining = bdds.subList(1, bdds.size());
        if (remaining.size() == 0) {
            List<Bdd> res = new ArrayList<>();
            res.add(thisBdd);
            res.add(thisBdd.not());
            return res;
        }
        List<Bdd> rec = generateAllCombos(remaining);
        List<Bdd> res = rec.stream().map(x -> x.and(thisBdd)).collect(Collectors.toList());
        res.addAll(rec.stream().map(x -> x.and(thisBdd.not())).collect(Collectors.toList()));
        return res;
    }

    public static PrimVS getNondetChoice(List<PrimVS> choices) {
        if(choices.size() == 0) return new PrimVS<>();
        List<PrimVS> results = new ArrayList<>();
        PrimVS empty = choices.get(0).guard(Bdd.constFalse());
        List<Bdd> choiceVars = new ArrayList<>();

        int numVars = 1;
        while ((1 << numVars) - choices.size() < 0) {
            numVars++;
        }
        int residual = (1 << numVars) - choices.size();

        for (int i = 0; i < numVars; i++) {
            choiceVars.add(Bdd.newVar());
        }

        List<Bdd> choiceConds = generateAllCombos(choiceVars);
        assert(choiceConds.size() - residual == choices.size());
        while (residual != 0) {
            Bdd last = choiceConds.remove(choiceConds.size() - 1);
            Bdd newLast = choiceConds.get(choiceConds.size() - 1);
            choiceConds.set(choiceConds.size() - 1, newLast.or(last));
            residual--;
        }
        assert(choices.size() == choiceConds.size());

        Bdd accountedPc = Bdd.constFalse();
        for (int i = 0; i < choices.size(); i++) {
            PrimVS choice = choices.get(i).guard(choiceConds.get(i));
            results.add(choice);
            accountedPc = accountedPc.or(choice.getUniverse()); //and(choice.getUniverse().not());
        }

        Bdd residualPc = accountedPc.not();
        for (PrimVS choice : choices) {
            if (residualPc.isConstFalse()) break;
            Bdd enabledCond = choice.getUniverse();
            PrimVS guarded = choice.guard(residualPc);
            results.add(guarded);
            residualPc = residualPc.and(enabledCond.not());
        }

        return empty.merge(results);
    }

    public static PrimVS getNondetChoiceAlt(List<PrimVS> choices) {
        if(choices.size() == 0) return new PrimVS<>();
        List<PrimVS> results = new ArrayList<>();
        PrimVS empty = choices.get(0).guard(Bdd.constFalse());

        Bdd residualPc = Bdd.constTrue();
        for (PrimVS choice : choices) {
            assert(!residualPc.isConstFalse());
            Bdd enabledCond = choice.getUniverse();
            Bdd choiceCond = Bdd.newVar().and(enabledCond);
            assert(!choiceCond.isConstFalse());
            if (choiceCond.isConstFalse()) {
                throw new RuntimeException();
            }

            //System.out.println("residual pc: " + residualPc);
            //System.out.println("choiceCond: " + choiceCond);
            Bdd returnPc = residualPc.and(choiceCond);
            assert(!returnPc.isConstFalse());
            results.add(choice.guard(returnPc));

            //System.out.println("new residual pc");
            //System.out.println("negated choice cond: " + choiceCond.not());
            residualPc = residualPc.and(choiceCond.not());
        }

        for (PrimVS choice : choices) {
            Bdd enabledCond = choice.getUniverse();

            Bdd returnPc = residualPc.and(enabledCond);
            results.add(choice.guard(returnPc));

            if (!choice.guard(returnPc).getUniverse().isConstFalse()) {
                ScheduleLogger.log("not false");
            }

            residualPc = residualPc.and(enabledCond.not());
        }

        final Bdd noneEnabledCond = residualPc;
        PrimVS<Boolean> isPresent = BoolUtils.fromTrueGuard(noneEnabledCond.not());

        assert(Checks.sameUniverse(noneEnabledCond.not(), empty.merge(results).getUniverse()));
        return empty.merge(results);
    }
}
