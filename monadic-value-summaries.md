# Notes on a Monadic Value Summary API

The task of designing a symbolic execution system reduces to that of determining, for each concrete language construct in the source language, how to 'lift' that construct into one which operates on a symbolic representation of the program state.  The strength of symbolic execution lies in our ability to design these 'lifted' abstractions so that they exploit the specific structure of our symbolic representations as much as possible, usually by finding opportunities to share data and computation across multiple implicitly-represented concrete program states.  However, when we attempt to develop lifted versions of the most primitive operations in the language, such as arithmetic and boolean operations, there is often little special structure to exploit, and in these cases we would like to be able to apply some general procedure that allows us to lift *any* arbitrary concrete operation into an operation on value summaries.

In this note, I propose a *monadic interface for value summaries* as a solution to the problem of lifting arbitrary concrete operations to the symbolic level.  Here, a 'monadic interface' for value summaries is one based on two operations, `pure` and `flatMap`, having the following abstract type signatures:

```
pure : T -> ValueSummary<T>

flatMap : (T, (T -> ValueSummary<U>)) -> ValueSummary<U>
```

  A monadic interface to value summaries could provide the following potential benefits:

1. It turns out that the `flatMap` operation is sufficiently expressive to lift concrete operations of *any arity* to the value summary level.  This is to say that given an arbitrary black-box concrete operation
    ```
    f : (T1, T2, ..., Tn) -> U
    ```
     iterating the `flatMap` operation allows us to derive an appropriate symbolic operation
     ```
     f_lifted : (ValueSummary<T1>, ValueSummary<T2>, ..., ValueSummary<Tn>) -> ValueSummary<U>
     ```
     This offers a convenient and uniform way to lift primitive operations like arithmetic to the symbolic level.
     (In fact, we do not even need the full expressiveness of a monadic interface to achieve this; we could instead also adopt the weaker structure of an [applicative functor](https://wiki.haskell.org/Applicative_functor))

2. The technique of lifting arbitrary programs into arbitrary monads is simple and well-understood, and can easily accomodate lifting complex control flow constructs.

3. While we would not want to use the monadic interface everywhere, as we can often gain performance by exploiting special structure in the operations we are lifting, a reference algorithm based on the monadic interface may be useful at design-time to provide a 'ground truth' for the semantics of our symbolic execution algorithm.  The rules of our actual implementation should be justifiable as semantically-equivalent refinements of what we would obtain by mechanically lifting the entire program to work on value summaries via `flatMap`.

In fact, below we give explanations of some basic features of the MultiPath algorithm, viewed as performance optimizations refining the 'ground truth' monadic algorithm.

## Performance Optimization: Contextual Path Constraint Information

One limitation of the `flatMap` operation as described above is that the 'inner computation' being lifted is completely context insensitive.  Consider for example the following pseudocode implementation of addition on value summaries over integers:

```
function addSymbolic(
  a: ValueSummary<Integer>,
  b: ValueSummary<Integer>
) -> ValueSummary<Integer> {
  return a.flatMap(x -> b.flatMap(y -> pure(x + y)));
}
```

Suppose we then apply the operation `addSymbolic(a, b)` to the following value summaries:

```
a = {
  1 if b_1,
  2 if not(b_1)
}

b = {
  100 if b_1,
  200 if not(b_1)
}
```

The computation would proceed as follows:

- First, for each of the two concrete values `x` in `a`, the outer `flatMap` call in `addSymbolic` would evaluate the function `x -> b.flatMap(y -> pure(x + y))`
- Within each of these two calls, for each of the two concrete values `y` in `b` the inner `flatMap` call would evaluate the function `y -> pure(x + y)`
- For each of the two values of `x`, a two-element value summary would be produced for each value of `y`:
    ```
    // Within the case x = 1:
    {
      101 if b_1,
      201 if not(b_1)
    }

    // Within the case x = 2:
    {
      102 if b_1,
      202 if not(b_1)
    }
    ```
- Only after computing these two inner value summaries will the outer call to `flatMap` guard each by the guard associated with its value of `x`:
    ```
    // From case x = 1, guarded by the associated constraint b_1
    {
      101 if (b_1 && b_1),
      201 if (not(b_1) && b_1)
    }

    // From case x = 2, guarded by the associated constraint not(b_1)
    {
      102 if (b_1 && not(b_1)),
      202 if (not(b_1) && not(b_1))
    }
    ```
- Finally, after guarding each inner value summary by its appropriate path constraint, the outer call to `flatMap` will merge and prune the two value summaries, producing:
    ```
    {
      101 if b_1,
      202 if not(b_1)
    }
    ```

Clearly, some unecessary work has been done here.  Ultimately, the cases `100 + 2 = 102` and `200 + 1 = 201` never needed to be computed, as they arose as the result of choosing concrete values from `a` and `b` guarded by incompatible path constraints.  However, because the inner value value summary computations lacked contextual information about the path constraints associated with the values of `x` they were given, these computations were unable to prune these unecessary cases until they were passed up to the outer call to `flatMap` on `a`.  While the consequences in this case were relatively minor, if addition were a computationally expensive operation then it would be a serious problem that we did twice as much work as was strictly necessary.

These problems become much more severe when one considers the possibility of implementing an `if` expression via `flatMap`:

```
function naiveSymbolicIf(
  cond: ValueSummary<Bool>,
  thenBody: (() -> ValueSummary<T>),
  elseBody: (() -> ValueSummary<T>)
) -> ValueSummary<T> {
  return cond.flatMap(cond_val ->
    cond_val ? thenBody() : elseBody()
  );
}
```

Although the function `naiveSymbolicIf` is semantically correct, it suffers the same problem as `addSymbolic`, in that each inner value summary evaluated lacks crucial contextual information about the path constraints by which it which eventually be guarded.  Indeed, we would encounter serious issues if we used `naiveSymbolicIf` to perform symbolic execution on the following source program:
```
if (b) {
  if (!b) {
    extremelyExpensiveComputation();
  }
  x
} else {
  y
}
```
The problem, as with the addition example above, is that if `b` can take on both concrete values `true` and `false`, then evaluation of the symbolic state associated with the 'then' branch of the above `if` statement will (symbolically!) perform `extremelyExpensiveComputation()` even though it can never occur at runtime, only for all of the results associated with that path to ultimately be pruned by the `flatMap` call in `naiveSymbolicIf`.

The crucial observation here is that the only purpose in life for expressions such as `thenBody()` in `naiveSymbolicIf`, or `b.flatMap(y -> pure(x + y))` in `addSymbolic`, is to be guarded by some additional path constraint later on.  It is the fact that this guard operation is *delayed* until the value summary is already computed that causes wasted computation, and the solution is evidently to somehow ensure this information is present at the time the value summary is initially computed.

This suggests adopting the following convention throughout the symbolic execution engine runtime: wherever the naive 'ground truth' reference algorithm would define a function

```
f : (A1, ..., An) -> ValueSummary<T>
```

the actual implementation should instead define a function

```
f_preguarded : (A1, ..., An, PathConstraint) -> ValueSummary<T>
```

satisfying the property

```
forall (a1 : A1, ..., an : An, p : PathConstraint), (f_preguarded(a1, ..., an, p) = guard(f(a1, ..., an), p))
```

This allows `f_preguarded` to take full advantage of its knowledge of the path constraint with which it will be guarded, so that it can prune irrelevant values *before* performing computations on them.

## Performance Optimization: Implicit Representation of Compound Data Structures

When programming in a monadic context, one typically works with functions whose entire return value is wrapped in the appropriate monadic type.  In the case of symbolic execution, however, such a representation can quickly lead to pathological behavior.  Consider a (rather contrived) concrete function `f` taking two integers and returning a pair containing both of them doubled.  The naive implementation of a lifted version of this function would work as follows:

```
function f_lifted(
  a: ValueSummary<Integer>,
  b: ValueSummary<Integer>
) -> ValueSummary<Pair<Integer, Integer>> {
  return a.flatMap(x -> b.flatMap(y -> pure(makePair(x * 2, y * 2))));
}
```

The problem with this function is that it explicitly represents every possible *combination* of the two integers, even though those values may be only used independently in the future.  In the worst case, each integer may have only `n` possible values, but the resulting value summary of pairs will have `n^2` concrete guarded values.  The problem only gets (exponentially) worse as we add more entries to the tuple, or consider the case of an arbitrary-length sequence.  We immediately see an opportunity for another performance optimization: rather than explicitly represent the return value as a value summary over pairs, we would much rather represent it as a pair of value summaries.  In fact, any value summary `v` over `Pair<T, U>` admits an implicit representation `(v1, v2)` as a pair of value summaries over `T` and over `U`, interpreted semantically as

```
interpret((v1, v2)) = v1.flatMap(x -> v2.flatMap(y -> pure(makePair(x, y))))
```

Wherever possible, using implicit representation of composite data structures, and keeping explicitly-represented value summaries near the 'leaves' of the data model, helps dramatically reduce the number of concrete value entries we need to represent.  Of course, in the worst case operations on composite data structures will still need to consider all possible combinations of each value in each field, but for many common language constructs one can exploit special structure to take full advantage of these compact implicit representations.
