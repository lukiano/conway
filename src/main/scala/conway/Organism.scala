package conway

sealed class Organism
case object Spark extends Organism
case object Void extends Organism

object Organism:
  given Alive[Organism] with
    def isAlive(o: Organism) = o == live
    val live = Spark
    val dead = Void

  given cats.Eq[Organism] with
    def eqv(x: Organism, y: Organism): Boolean = x == y

  given cats.Show[Organism] with
    def show(o: Organism) = if o == Spark then "#" else "."


