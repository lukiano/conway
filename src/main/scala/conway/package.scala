
import cats.kernel.Semigroup
package object conway {
    case class WrongArgs(message: String, status: Int)

    given Semigroup[WrongArgs] with
      override def combine(x: WrongArgs, y: WrongArgs): WrongArgs =
        WrongArgs(x.message + y.message, Math.max(x.status, y.status))

    case class Args(generations: Int, delayMillis: Int)
}