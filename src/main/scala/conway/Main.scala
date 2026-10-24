package conway

import cats.syntax.all.*
import com.monovore.decline.{Command, Opts}

object Main:
  private[conway] val command: Command[(Int, Int)] = Command(
    name = "conway",
    header = "Conway's Game of Life (defaults: 80 generations, 100 ms delay).",
    helpFlag = false
  ) {
    val generations = Opts.argument[Int]("generations")
      .withDefault(80)
      .validate("Generations must be nonnegative")(_ >= 0)
    val delayMillis = Opts.argument[Int]("delayMillis")
      .withDefault(100)
      .validate("Delay must be nonnegative")(_ >= 0)
    Opts.flag("help", help = "Display this help text.", short = "h").asHelp
      .orElse((generations, delayMillis).tupled)
  }

  def run(generations: Int, delayMillis: Int): Unit =
    val frames = Iterator.iterate((Game.glider[Organism](), 0L)) { (board, generation) =>
      (Game.step(board), generation + 1L)
    }
    for (board, generation) <- frames.takeWhile(_._2 <= generations.toLong) do
      if System.console() != null then print("\u001b[H\u001b[2J")
      println(s"Generation $generation\n${board.show}\n")
      if generation < generations && delayMillis > 0 then Thread.sleep(delayMillis.toLong)

  def main(args: Array[String]): Unit =
    command.parse(args.toSeq) match
      case Left(help) if help.errors.isEmpty => println(help)
      case Left(help) =>
        Console.err.println(help)
        sys.exit(2)
      case Right((generations, delayMillis)) => run(generations, delayMillis)
