package universal

import cats._
import cats.data._
import cats.data.Validated._
import scala.util.matching.Regex

object Validators {

  type Validator = String => ValidatedNel[String, String]

  implicit class RichValidator(validator: Validator) {
    def <+>(other: Validator): Validator = v => Applicative[({type l[A] = ValidatedNel[String, A]})#l].map2(validator(v), other(v))((x, y) => x)
  }

  val nonEmptyValidator: Validator = v => if (v.isEmpty) invalidNel("value is empty") else valid(v)
  def regexValidator(regex: Regex): Validator = v => {
    val m = v match {
      case regex(_*) => true
      case _ => false
    }
    if (m) valid(v) else invalidNel(v)
  }
  val numberValidator: Validator = regexValidator("""\d+""".r) andThen (_.bimap(es => NonEmptyList(s""""${es.head}" is not a number"""), identity))
  def maxLengthValidator(length: Int): Validator = v => if (v.length > length) invalidNel(s"""length of "$v" is more then $length""") else valid(v)

}
