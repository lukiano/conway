package conway

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object Main extends CommandIOApp(
  name = "conway",
  header = "Conway's Game of Life (defaults: 80 generations, 100 ms delay).",
  helpFlag = false
):
  private[conway] val options: Opts[(Int, Int)] =
    val generations = Opts.argument[Int]("generations")
      .withDefault(80)
      .validate("Generations must be nonnegative")(_ >= 0)
    val delayMillis = Opts.argument[Int]("delayMillis")
      .withDefault(100)
      .validate("Delay must be nonnegative")(_ >= 0)
    Opts.flag("help", help = "Display this help text.", short = "h").asHelp
      .orElse((generations, delayMillis).tupled)

  private def run(generations: Int, delayMillis: Int): IO[ExitCode] =
    for {
      _ <- Simulation.run[IO, Grid[Organism]](SimulationConfig(generations, delayMillis, System.console() != null, Game.glider(), Game.step))
    } yield ExitCode.Success

  override def main: Opts[IO[ExitCode]] =
    options.map { run.tupled }
