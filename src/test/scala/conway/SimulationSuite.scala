package conway

import cats.Show
import cats.effect.{IO, Ref}
import cats.effect.std.Console
import cats.effect.testkit.TestControl
import cats.effect.unsafe.implicits.global
import java.nio.charset.Charset
import scala.concurrent.duration.*

class SimulationSuite extends munit.FunSuite:
  private def record(
      generations: Int,
      delayMillis: Int,
      clearScreen: Boolean = false
  ): IO[(Vector[(FiniteDuration, String)], FiniteDuration)] =
    Ref.of[IO, Vector[(FiniteDuration, String)]](Vector.empty).flatMap { output =>
      given Console[IO] with
        def readLineWithCharset(charset: Charset): IO[String] =
          IO.raiseError(new UnsupportedOperationException("unexpected input"))
        def print[A](a: A)(using show: Show[A]): IO[Unit] =
          IO.monotonic.flatMap(time => output.update(_ :+ (time, show.show(a))))
        def println[A](a: A)(using show: Show[A]): IO[Unit] = print(show.show(a) + "\n")
        def error[A](a: A)(using show: Show[A]): IO[Unit] =
          IO.raiseError(new AssertionError(show.show(a)))
        def errorln[A](a: A)(using show: Show[A]): IO[Unit] = error(a)

      val program = Simulation.run[IO](generations, delayMillis, clearScreen)
      for
        before <- output.get
        _ = assert(before.isEmpty)
        start <- IO.monotonic
        _ <- program
        end <- IO.monotonic
        frames <- output.get
      yield (frames.map { (time, text) => (time - start, text) }, end - start)
    }

  test("frames are emitted in order with virtual delays and no final sleep") {
    TestControl.executeEmbed(record(2, 100)).map { (frames, elapsed) =>
      assertEquals(frames.map(_._1), Vector(0.millis, 100.millis, 200.millis))
      assertEquals(elapsed, 200.millis)
      val initial = Game.glider[Organism]()
      val boards = Iterator.iterate(initial)(Game.step).take(3).toVector
      val expected = boards.zipWithIndex.map { (board, generation) =>
        s"Generation $generation\n${Show[Grid[Organism]].show(board)}\n\n"
      }
      assertEquals(frames.map(_._2), expected)
    }.unsafeToFuture()
  }

  test("zero generations emits one frame without waiting and can clear the screen") {
    TestControl.executeEmbed(record(0, 500, clearScreen = true)).map { (frames, elapsed) =>
      assertEquals(frames.size, 1)
      assertEquals(elapsed, Duration.Zero)
      assert(frames.head._2.startsWith("\u001b[H\u001b[2JGeneration 0\n"))
    }.unsafeToFuture()
  }

  test("zero delay emits all frames without advancing virtual time") {
    TestControl.executeEmbed(record(2, 0)).map { (frames, elapsed) =>
      assertEquals(frames.size, 3)
      assertEquals(elapsed, Duration.Zero)
    }.unsafeToFuture()
  }
