package symbolicp.bdd;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;
import symbolicp.vs.PrimVS;

import java.util.ArrayList;
import java.util.List;

public class SMTBddImpl implements BddLib<BooleanFormula> {
    final private SolverContext context;
    final private FormulaManager formulaManager;
    final private BooleanFormulaManager booleanFormulaManager;
    final private IntegerFormulaManager integerFormulaManager;
    private long idx = 0;

    public SMTBddImpl() {
        try {
            Configuration config = Configuration.defaultConfiguration();
            LogManager logger = BasicLogManager.create(config);
            ShutdownManager shutdown = ShutdownManager.create();

            context = SolverContextFactory.createSolverContext(
                    config, logger, shutdown.getNotifier(), SolverContextFactory.Solvers.SMTINTERPOL);
            formulaManager = context.getFormulaManager();
            booleanFormulaManager = formulaManager.getBooleanFormulaManager();
            integerFormulaManager = formulaManager.getIntegerFormulaManager();
        } catch (InvalidConfigurationException e){
            e.printStackTrace();
            throw new RuntimeException("Invalid configuration for SMT");
        }
    }

    /*
    public PrimVS getNondetChoice(List<PrimVS> choices) {
        if(choices.size() == 0) return new PrimVS<>();
        List<PrimVS> results = new ArrayList<>();
        integerFormulaManager.makeVariable("var_" + idx++);
        for (PrimVS choice : choices) {

        }
    }

     */

    @Override
    public BooleanFormula constFalse() {
        return booleanFormulaManager.makeFalse();
    }

    @Override
    public BooleanFormula constTrue() {
        return booleanFormulaManager.makeTrue();
    }

    @Override
    public boolean isConstFalse(BooleanFormula booleanFormula) {
        try (ProverEnvironment prover = context.newProverEnvironment()) {
            prover.addConstraint(booleanFormula);
            boolean isUnsat = prover.isUnsat();
            return isUnsat;
        } catch(InterruptedException | SolverException e) {
            e.printStackTrace();
            throw new RuntimeException("Issue querying solver");
        }
    }

    private BooleanFormula simplify(BooleanFormula booleanFormula) {
        try {
            return formulaManager.simplify(booleanFormula);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Issue simplifying");
        }
    }

    @Override
    public boolean isConstTrue(BooleanFormula booleanFormula) {
        return isConstFalse(simplify(booleanFormulaManager.not(booleanFormula)));
    }

    @Override
    public BooleanFormula and(BooleanFormula left, BooleanFormula right) {
        return simplify(booleanFormulaManager.and(left, right));
    }

    @Override
    public BooleanFormula or(BooleanFormula left, BooleanFormula right) {
        return booleanFormulaManager.or(left, right);
    }

    @Override
    public BooleanFormula not(BooleanFormula booleanFormula) {
        return simplify(booleanFormulaManager.not(booleanFormula));
    }

    @Override
    public BooleanFormula implies(BooleanFormula left, BooleanFormula right) {
        return booleanFormulaManager.implication(left, right);
    }

    @Override
    public BooleanFormula ifThenElse(BooleanFormula cond, BooleanFormula thenClause, BooleanFormula elseClause) {
        return booleanFormulaManager.or(booleanFormulaManager.and(cond, thenClause),
                booleanFormulaManager.and(booleanFormulaManager.not(cond), elseClause));
    }

    @Override
    public BooleanFormula newVar() {
        return booleanFormulaManager.makeVariable("var_" + idx++);
    }

    @Override
    public String toString(BooleanFormula booleanFormula) {
        return booleanFormula.toString();
    }

    @Override
    public BooleanFormula fromString(String s) {
        return formulaManager.parse(s);
    }
}
