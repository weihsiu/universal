package universal

import scala.language.postfixOps
import scalacss.Defaults._

object ExampleStyles extends StyleSheet.Inline {
  import dsl._
  val alert = styleF.bool(x => styleS(
    color(if (x) red else black)
  ))
  val heavyFrame = style(
    borderWidth(10 px),
    borderStyle(solid),
    borderColor(black)
  )
  val defaultButton = style(
    addClassNames("btn", "btn-default"),
    heavyFrame
  )
}
