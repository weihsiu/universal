package universal

import cats.data.OneAnd
import cats.data.Validated._
import utest._
import utest.ExecutionContext.RunNow

object ValidatorsTestSuite extends TestSuite {
  import Validators._
  val tests = TestSuite {
    'nonEmptyValidator {
      assert(nonEmptyValidator("abc") == valid("abc"))
      assert(nonEmptyValidator("") == invalidNel("value is empty"))
    }
    'numberValidator {
      assert(numberValidator("123") == valid("123"))
      assert(numberValidator("-123") == invalidNel(""""-123" is not a number"""))
      assert(numberValidator("abc") == invalidNel(""""abc" is not a number"""))
    }
    'maxLengthValidator {
      assert(maxLengthValidator(3)("abc") == valid("abc"))
      assert(maxLengthValidator(3)("abcd") == invalidNel("""length of "abcd" is more then 3"""))
    }
    'combined {
      val maxNumberValidator = numberValidator <+> maxLengthValidator(3)
      assert(maxNumberValidator("123") == valid("123"))
      assert(maxNumberValidator("1234") == invalidNel("""length of "1234" is more then 3"""))
      assert(maxNumberValidator("abc") == invalidNel(""""abc" is not a number"""))
      assert(maxNumberValidator("abcd") == invalid(OneAnd(""""abcd" is not a number""", List("""length of "abcd" is more then 3"""))))
    }
  }
}
