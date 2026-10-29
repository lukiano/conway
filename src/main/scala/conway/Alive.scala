package conway

trait Alive[A] {
    def isAlive(a: A): Boolean
    def live: A
    def dead: A
}

object Alive:
  def apply[A]()(using tc: Alive[A]): Alive[A] = tc

  extension [A](value: A)(using tc: Alive[A])
    def isAlive: Boolean = tc.isAlive(value)

  given monoid[T](using tc: Alive[T]): cats.Monoid[T] with
    def empty: T = Alive().live

    def combine(t1: T, t2: T): T =
      val tc = Alive()
      if tc.isAlive(t1) && tc.isAlive(t2) then tc.live else tc.dead
