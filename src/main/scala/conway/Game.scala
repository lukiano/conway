package conway

import cats.Comonad
import cats.syntax.coflatMap.*

object Game:
  private val neighbors =
    for
      dy <- -1 to 1
      dx <- -1 to 1
      if dx != 0 || dy != 0
    yield (dx, dy)


  /** The local B3/S23 rule: read the focused cell and its eight neighbors. */
  def rule[T : Alive](context: Grid[T]): T =
    val tc = Alive()
    val alive = neighbors.count { context.relative.tupled andThen tc.isAlive }
    val t = Comonad[Grid].extract(context)
    if (alive == 3 || (tc.isAlive(t) && alive == 2)) tc.live else tc.dead

  def step[T : Alive](board: Grid[T]): Grid[T] =
    board coflatMap rule

  def glider[T : Alive](width: Int = 20, height: Int = 12): Grid[T] =
    val tc = Alive()
    require(width >= 3 && height >= 3, "glider needs at least a 3 by 3 grid")
    val seed = Set((1, 0), (2, 1), (0, 2), (1, 2), (2, 2))
    Grid(Vector.tabulate(height, width)((y, x) => if seed.contains(x, y) then tc.live else tc.dead ))
