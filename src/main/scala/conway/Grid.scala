package conway

import cats.Comonad
import cats.Show

import cats.syntax.show.*

/** An immutable, nonempty rectangular grid with a focused cell and wrapping edges. */
final case class Grid[A](rows: Vector[Vector[A]], focusX: Int = 0, focusY: Int = 0):
  require(rows.nonEmpty && rows.head.nonEmpty, "grid must be nonempty")
  val width: Int = rows.head.size
  val height: Int = rows.size
  require(rows.forall(_.size == width), "grid must be rectangular")
  require(focusX >= 0 && focusX < width && focusY >= 0 && focusY < height,
    "focus must be inside the grid")

  def at(x: Int, y: Int): A =
    rows(Math.floorMod(y, height))(Math.floorMod(x, width))

  def relative(dx: Int, dy: Int): A = at(focusX + dx, focusY + dy)

object Grid:
  given Comonad[Grid] with
    def extract[A](fa: Grid[A]): A = fa.rows(fa.focusY)(fa.focusX)

    def map[A, B](fa: Grid[A])(f: A => B): Grid[B] =
      Grid(fa.rows.map(_.map(f)), fa.focusX, fa.focusY)

    def coflatMap[A, B](fa: Grid[A])(f: Grid[A] => B): Grid[B] =
      // Every rule sees the same generation, focused on a different cell.
      val rows = Vector.tabulate(fa.height, fa.width) { (y, x) =>
        f(fa.copy(focusX = x, focusY = y))
      }
      Grid(rows, fa.focusX, fa.focusY)

  given catsShowForGrid[A: Show]: Show[Grid[A]] with
    def show(grid: Grid[A]): String =
      grid.rows.map(_.map(a => a.show).mkString).mkString("\n")

  