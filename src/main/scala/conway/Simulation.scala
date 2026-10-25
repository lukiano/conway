package conway

import cats.{Applicative, Show}
import cats.effect.Temporal
import cats.effect.std.Console
import cats.syntax.all.*
import scala.concurrent.duration.*


case class SimulationConfig[B](
    generations: Int,
    delayMillis: Int,
    clearScreen: Boolean,
    initial: B,
    next: B => B
) {
  val prefix = if clearScreen then "\u001b[H\u001b[2J" else ""
}

object Simulation:
  extension [F[_]: Applicative](effect: F[Unit])
    /** Gates this Unit effect before execution; subsequent effects still run. */
    def withFilter(predicate: Unit => Boolean): F[Unit] =
      effect.whenA(predicate(()))

  private def display[F[_]: Console, T: Show](current: T, generation: Int, config: SimulationConfig[T]): F[Unit] =
    Console[F].println(s"${config.prefix}Generation $generation\n${current.show}\n")

  private def loop[F[_]: Temporal: Console, T: Show](current: T, generation: Int, config: SimulationConfig[T]): F[Unit] =
    for {
      _ <- display(current, generation, config)
      _ <- Temporal[F].sleep(config.delayMillis.millis) if config.delayMillis > 0 && generation < config.generations.toLong
      _ <- loop(config.next(current), generation + 1, config) if generation < config.generations.toLong
    } yield ()

  def run[F[_]: Temporal: Console, B: Show](config: SimulationConfig[B]): F[Unit] =
    loop(config.initial, 0, config)
