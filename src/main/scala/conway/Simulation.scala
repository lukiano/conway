package conway

import cats.Show
import cats.data.ReaderT
import cats.effect.Temporal
import cats.effect.std.Console
import fs2.Stream
import scala.concurrent.duration.FiniteDuration



object Simulation:
  case class Config(
      generations: Int,
      delay: FiniteDuration,
      clearScreen: Boolean,
  ) {
    val prefix = if clearScreen then "\u001b[H\u001b[2J" else ""
  }
  type Configured[F[_]] = [A] =>> ReaderT[F, Config, A]

  private def display[F[_]: Console, T: Show](current: T, generation: Long): ReaderT[F, Config, Unit] =
    import cats.syntax.show.*
    ReaderT { config => 
      val message = s"${config.prefix}Generation $generation\n${current.show}\n\n"
      Console[F].print(message)
    }

  private def delay[F[_]: Temporal](period: FiniteDuration): Stream[F, Unit] =
    Stream.unit ++ Stream.fixedDelay(period)

  def run[F[_]: Temporal: Console, B: Show](stream: Stream[Configured[F], B]): ReaderT[F, Config, Unit] =
    ReaderT.ask[F, Config].flatMap { config =>
      delay[Configured[F]](config.delay)
        .zipRight(stream)
        .zipWithIndex
        .evalMap(display)
        .take(config.generations + 1)
        .compile
        .drain
    }
