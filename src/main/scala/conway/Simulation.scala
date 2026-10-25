package conway

import cats.Show
import cats.effect.Temporal
import cats.effect.std.Console
import cats.syntax.all.*
import scala.concurrent.duration.*

object Simulation:
  def loop[F[_]: Temporal: Console, A: Alive: Show](board: Grid[A], generation: Int, generations: Int, delayMillis: Int, prefix: String): F[Unit] =
    Console[F].println(s"${prefix}Generation $generation\n${board.show}\n").flatMap { _ =>
      if generation >= generations.toLong then Temporal[F].unit
      else
        val pause =
          if delayMillis > 0 then Temporal[F].sleep(delayMillis.millis)
          else Temporal[F].unit
        pause.flatMap(_ => loop(Game.step(board), generation + 1, generations, delayMillis, prefix))
    }

  def run[F[_]: Temporal: Console](
      generations: Int,
      delayMillis: Int,
      clearScreen: Boolean = false
  ): F[Unit] =
    val prefix = if clearScreen then "\u001b[H\u001b[2J" else ""
    for {
      _ <- Temporal[F].unit
      _ <- loop(Game.glider[Organism](), 0, generations, delayMillis, prefix)
    } yield ()
