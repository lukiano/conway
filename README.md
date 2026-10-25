# Conway's Game of Life with a comonad

Scala 3.10.0, sbt 2.0.11, Cats Core 2.13.0, Cats Effect 3.7.1, and
Decline Effect 2.6.2. Cats Core has no published 3.x release; the `_3`
dependency is its Scala 3 artifact. Cats Effect is a separate library.

## Run

With a JDK (21 or newer) and the sbt runner installed:

```sh
cd /Users/luciano/git/conway
sbt run
sbt 'run 40 150'
sbt 'run 4 0'
sbt test
```

Arguments are the number of generation transitions and the delay in milliseconds.
The initial generation is also printed. Defaults are 80 transitions and 100 ms.
Use `sbt 'run --help'` (or `sbt 'run -h'`) for generated usage.
CommandIOApp prints help to standard error. Help exits with status 0;
invalid arguments exit with status 1.
The default seed is a glider on a 20-column, 12-row torus: both edges wrap.
`#` means alive, `.` means dead. Frames clear the display when a console is
available; otherwise they are printed sequentially. Ctrl+C stops the demo.

## Where the comonad matters

`Grid[A]` contains an immutable rectangular grid and a focused coordinate.
Its companion supplies `given Comonad[Grid]` from Cats:

- `extract` reads the focused cell.
- `map` transforms values while preserving the focus.
- `coflatMap` (also known as `extend`) applies a context-dependent function
  at every coordinate. Each context shares the original immutable cell data
  and changes only its focus. The result preserves the original focus.

`Game.rule: Grid[Boolean] => Boolean` computes one cell's next state by
reading its eight neighbors and applying B3/S23: birth with three neighbors,
survival with two or three. The complete evolution is:

```scala
Comonad[Grid].coflatMap(board)(rule)
```

Every calculation reads the same old generation, so updates are simultaneous.
The board is finite and wraps; on dimensions smaller than three, different
neighbor offsets can refer to the same physical cell and count separately.
The demo uses larger dimensions.

`LifeSuite` checks all 512 local configurations, a block, a blinker, glider
movement, wrapping, input validation, and the comonad and functor laws on
all binary 2-by-2 grids at every focus.

The implementation stores O(width × height) cells per generation. It is a
small educational terminal application; it has no graphical UI or persistence.

Release references: [Scala](https://www.scala-lang.org/download/3.10.0.html),
[sbt](https://www.scala-sbt.org/download/),
[Cats](https://github.com/typelevel/cats/releases/tag/v2.13.0),
[Cats Comonad](https://typelevel.org/cats/typeclasses/comonad.html).

## Argument parsing and effects

[Decline](https://ben.kirw.in/decline/) parses the two optional positional
integers, supplies defaults, validates nonnegative values, and generates
help and error messages. Cats' `.tupled` combines the two `Opts[Int]` values
into `Opts[(Int, Int)]`. There are no custom argument or error types.

`Main` extends `CommandIOApp` from `decline-effect`. Its `main` describes an
`Opts[IO[ExitCode]]`; the library owns parsing, help, exit codes, and the
Cats Effect runtime. `Main` checks whether a terminal is attached inside
`IO.delay` and passes that flag to the simulation for screen clearing.

`Simulation.scala` contains `run[F[_]: Temporal: Console]`, returning `F[Unit]`.
It has no dependency on concrete `IO`: output uses `Console[F]`, and delays
use `Temporal[F].sleep`, which suspends the fiber without blocking a thread.
The first frame is immediate and there is no delay after the last frame.

`SimulationSuite` uses a recording console and Cats Effect's `TestControl`
to verify frame contents, ordering, and timing with virtual time. Its delays
do not require real waiting or produce terminal output.
