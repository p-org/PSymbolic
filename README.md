# PSymbolic
This repository implements a symbolic execution engine for P

## P Compiler Files

The files in the `tmp` directory should be placed in locations in the P compiler.
In particular, they should go within the corresponding directories in `P/Src/Pc/CompilerCore`.
For example, `tmp/CompilerCore/Backend/Symbolic/SymbolicCodeGenerator.cs` should replace `P/Src/Pc/CompilerCore/Backend/Symbolic/SymbolicCodeGenerator.cs`.

## To Build

 * Build the P repository project as normal from https://github.com/p-org/P/tree/symbolic-codegen
 * In the code from the P repository, comment out the line `git submodule update --init --recursive` from the `Bld/build-compiler.sh` file
 * This repository should be `PSymbolic` directory in the P directory (replace whatever already exists because it is the old code from GitHub)
 * Replace the files as described above
 * Re-run `Bld/build-compiler.sh`

## Regression Testing

The regression test cases are located in the `SymbolicRegressionTests` directory.
In IntelliJ, running `src/test/java/symbolicp/SymbolicRegression` will run the engine on all the examples in `SymbolicRegressionTests`
that are located either in a subdirectory called `Correct`, `DynamicError`, or `StaticError`.

In the `SymbolicRegressionTests` directory, the programs in the `TooLong` directory are not run but can be run by moving them to a
subdirectory of `SymbolicRegressionTests` with one of those names. They take a very long time to run so were not included.
