import cats.data.Validated.{Invalid, Valid}
import cats.{Semigroup, Applicative}
import cats.data._
import cats.std.list._

package object universal {

  implicit def nelSemigroup[A]: Semigroup[NonEmptyList[A]] =
    new Semigroup[NonEmptyList[A]] {
      def combine(x: NonEmptyList[A], y: NonEmptyList[A]): NonEmptyList[A] = x.combine(y)
    }

  implicit def validatedApplicative[E: Semigroup]: Applicative[({type l[A] = Validated[E, A]})#l] =
    new Applicative[({type l[A] = Validated[E, A]})#l] {
      def ap[A, B](fa: Validated[E, A])(f: Validated[E, A => B]): Validated[E, B] = (fa, f) match {
        case (Valid(a), Valid(fab)) => Valid(fab(a))
        case (i @ Invalid(_), Valid(_)) => i
        case (Valid(_), i @ Invalid(_)) => i
        case (Invalid(e1), Invalid(e2)) => Invalid(Semigroup[E].combine(e1, e2))
      }
      def pure[A](x: A): Validated[E, A] = Validated.valid(x)
    }
}
