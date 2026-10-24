package conway

import cats.Comonad

given Alive[Boolean] with
    def isAlive(b: Boolean) = b == live
    val live = true
    val dead = false

class LifeSuite extends munit.FunSuite:
  private def board(points: Set[(Int, Int)], size: Int = 6): Grid[Boolean] =
    Grid(Vector.tabulate(size, size)((y, x) => points.contains((x, y))))

  test("all 512 local configurations obey B3/S23") {
    for mask <- 0 until 512 do
      val cells = Vector.tabulate(3, 3)((y, x) => (mask & (1 << (y * 3 + x))) != 0)
      val context = Grid(cells, 1, 1)
      val count = Integer.bitCount(mask & ~(1 << 4))
      assertEquals(Game.rule(context), count == 3 || (cells(1)(1) && count == 2))
  }

  test("block is a still life") {
    val block = board(Set((2, 2), (2, 3), (3, 2), (3, 3)))
    assertEquals(Game.step(block), block)
  }

  test("blinker rotates, then returns after two simultaneous steps") {
    val horizontal = board(Set((1, 2), (2, 2), (3, 2)))
    val vertical = board(Set((2, 1), (2, 2), (2, 3)))
    assertEquals(Game.step(horizontal), vertical)
    assertEquals(Game.step(vertical), horizontal)
  }

  test("glider moves diagonally after four generations") {
    val initial = Game.glider(8, 8)
    val expected = Grid(Vector.tabulate(8, 8)((y, x) => initial.at(x - 1, y - 1)))
    assertEquals(Iterator.iterate(initial)(Game.step).drop(4).next(), expected)
  }

  test("neighbors wrap across both edges") {
    val horizontal = board(Set((5, 0), (0, 0), (1, 0)))
    val vertical = board(Set((0, 5), (0, 0), (0, 1)))
    assertEquals(Game.step(horizontal), vertical)
    assertEquals(Game.step(vertical), horizontal)
  }

  test("empty and singleton populations die out") {
    val empty = board(Set.empty)
    assertEquals(Game.step(empty), empty)
    assertEquals(Game.step(board(Set((2, 2)))), empty)
  }

  test("grid rejects empty, ragged, and invalid-focus inputs") {
    intercept[IllegalArgumentException](Grid(Vector.empty[Vector[Int]]))
    intercept[IllegalArgumentException](Grid(Vector(Vector.empty[Int])))
    intercept[IllegalArgumentException](Grid(Vector(Vector(1), Vector(2, 3))))
    intercept[IllegalArgumentException](Grid(Vector(Vector(1)), focusX = 1))
    intercept[IllegalArgumentException](Grid(Vector(Vector(1)), focusY = -1))
  }

  test("comonad identities, associativity and functor laws at every focus") {
    val c = Comonad[Grid]
    val f: Grid[Int] => Int = g => c.extract(g) + g.relative(1, 0)
    val g: Grid[Int] => Int = grid => c.extract(grid) - grid.relative(0, 1)
    for
      mask <- 0 until 16
      y <- 0 until 2
      x <- 0 until 2
    do
      val grid = Grid(Vector.tabulate(2, 2)((r, col) => (mask >> (r * 2 + col)) & 1), x, y)
      assertEquals(c.coflatMap(grid)(c.extract), grid)
      assertEquals(c.extract(c.coflatMap(grid)(f)), f(grid))
      assertEquals(c.coflatMap(c.coflatMap(grid)(f))(g),
        c.coflatMap(grid)(context => g(c.coflatMap(context)(f))))
      assertEquals(c.map(grid)(identity), grid)
      assertEquals(c.map(c.map(grid)(_ + 1))(_ * 2), c.map(grid)(a => (a + 1) * 2))
      assertEquals(c.map(grid)(_ + 1), c.coflatMap(grid)(context => c.extract(context) + 1))
  }

  test("command arguments have defaults and reject invalid values") {
    assertEquals(Main.command.parse(Seq.empty), Right((80, 100)))
    assertEquals(Main.command.parse(Seq("4")), Right((4, 100)))
    assertEquals(Main.command.parse(Seq("0", "0")), Right((0, 0)))
    for args <- Seq(Seq("-1"), Seq("x"), Seq("1", "-1"), Seq("2147483648"), Seq("1", "2", "3")) do
      assert(Main.command.parse(args).isLeft)
  }

  test("Decline reports validation errors and unexpected arguments") {
    val cases = Seq(
      Seq("-1") -> "Usage:",
      Seq("1", "-1") -> "Usage:",
      Seq("--", "-1") -> "Generations must be nonnegative",
      Seq("--", "1", "-1") -> "Delay must be nonnegative",
      Seq("x") -> "Usage:",
      Seq("2147483648") -> "Usage:",
      Seq("1", "2", "3") -> "Usage:",
      Seq("--unknown") -> "Usage:"
    )
    for (args, message) <- cases do
      Main.command.parse(args) match
        case Left(error) =>
          assert(error.errors.nonEmpty, error.toString)
          assert(error.toString.contains(message), error.toString)
        case Right(value) => fail(s"Expected a parsing error, got $value")
  }

  test("Decline accepts explicit values and the Int boundary") {
    assertEquals(Main.command.parse(Seq("40", "150")), Right((40, 150)))
    assertEquals(Main.command.parse(Seq(Int.MaxValue.toString)), Right((Int.MaxValue, 100)))
  }

  test("help requests produce generated usage without parsing errors") {
    for flag <- Seq("--help", "-h") do
      Main.command.parse(Seq(flag)) match
        case Left(help) =>
          assert(help.errors.isEmpty, help.toString)
          assert(help.toString.contains("Usage:"), help.toString)
          assert(help.toString.contains("generations"), help.toString)
          assert(help.toString.contains("delayMillis"), help.toString)
        case Right(value) => fail(s"Expected help, got $value")
  }
