package symbolicp.vs;

import symbolicp.bdd.Bdd;

public interface ValueSummary<Self extends ValueSummary<Self>> {
    Self guard(Bdd cond);
    Self merge(Self other);

    // There are two important design questions here that I consider open: how do we represent 'empty' (which is needed
    // for correctness in some places), and how do we represent merges of more than two value summaries (which is
    // valuable for performance)?
    //
    // The use cases for 'empty' are actually fairly limited, and I think in most cases where it is currently needed you
    // can just allow the variable to be 'null' and handle 'null' as a special case.
    //
    // For multi-way merges, I think it is acceptable to lose the performance benefits for now.  If we do eventually need
    // to recover the performance of multi-way merges (which are rarely needed anyway), we should be able to achieve it
    // via special-cased operations on the relevant concrete value summary type.
}
