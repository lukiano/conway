package conway

import cats.{Applicative, Show}
import cats.effect.Temporal
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.Stream
import scala.concurrent.duration.*


case class SimulationConfig(
    generations: Int,
    delay: FiniteDuration,
    clearScreen: Boolean,
) {
  val prefix = if clearScreen then "\u001b[H\u001b[2J" else ""
}

object Simulation:  
  private def display[F[_]: Console, T: Show](current: T, generation: Long, config: SimulationConfig): F[Unit] =
    Console[F].println(s"${config.prefix}Generation $generation\n${current.show}\n")

  private def delay[F[_]: Temporal](period: FiniteDuration): Stream[F, Unit] =
    Stream.unit ++ Stream.fixedDelay(period)

  def run[F[_]: Temporal: Console, B: Show](stream: Stream[F, B], config: SimulationConfig): F[Unit] =
    delay(config.delay)
      .zipRight(stream)
      .zipWithIndex
      .evalMap((current, generation) => display(current, generation, config))
      .take(config.generations + 1)
      .compile
      .drain
