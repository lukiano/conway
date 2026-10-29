package conway

import cats.effect.ExitCode
import cats.effect.IO
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.Stream
import scala.concurrent.duration.*

object Main extends CommandIOApp(
  name = "conway",
  header = "Conway's Game of Life (defaults: 80 generations, 100 ms delay).",
  helpFlag = false
):
  private[conway] lazy val options: Opts[(Int, FiniteDuration)] =
    val generations = Opts.argument[Int]("generations")
      .withDefault(80)
      .validate("Generations must be nonnegative")(_ >= 0)
    val delay = Opts.argument[FiniteDuration]("delay")
      .withDefault(100.millis)
      .validate("Delay must be nonnegative")(_ >= Duration.Zero)
      .validate("Delay must be finite")(_.isFinite)
    Opts.flag("help", help = "Display this help text.", short = "h").asHelp
      .orElse((generations, delay).tupled)

  private def run(generations: Int, delay: FiniteDuration): IO[ExitCode] =
    val config = Simulation.Config(generations, delay, System.console() != null)
    val stream = Stream.iterate[Simulation.Configured[IO], Grid[Organism]](Game.glider[Organism]())(_ coflatMap Game.rule)
    Simulation.run(stream).run(config).as(ExitCode.Success)

  override def main: Opts[IO[ExitCode]] =
    options.map { run.tupled }
