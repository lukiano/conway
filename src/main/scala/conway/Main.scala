package conway

import cats.ApplicativeError
import cats.instances.either.*

import cats.Show

import cats.syntax.show.*

object Main:
  val usage = "Usage: sbt 'run [generations] [delayMillis]' (defaults: 80 100; both nonnegative integers)"

  
  private[conway] def parseArgs[F[_]](args: Seq[String])(
      using F: ApplicativeError[F, WrongArgs]
  ): F[Args] =
    if args.size > 2 then F.raiseError(WrongArgs(usage, 2))
    else
      val values = args.map(_.toIntOption.filter(_ >= 0))
      if values.exists(_.isEmpty) then F.raiseError(WrongArgs(usage, 2))
      else F.pure(Args(values.headOption.flatten.getOrElse(80), values.lift(1).flatten.getOrElse(100)))

  def main(args: Array[String]): Unit =
    if args.toSeq == Seq("--help") || args.toSeq == Seq("-h") then println(usage)
    else
      parseArgs[[A] =>> Either[WrongArgs, A]](args.toSeq) match
        case Left(wrongArgs) =>
          Console.err.println(wrongArgs.message)
          sys.exit(wrongArgs.status)
        case Right(Args(generations, delayMillis)) =>
          val frames = Iterator.iterate((Game.glider[Organism](), 0L)) { (board, generation) =>
            (Game.step(board), generation + 1L)
          }
          for (board, generation) <- frames.takeWhile(_._2 <= generations.toLong) do
            if System.console() != null then print("\u001b[H\u001b[2J")
            println(s"Generation $generation\n${board.show}\n")
            if generation < generations && delayMillis > 0 then Thread.sleep(delayMillis.toLong)
