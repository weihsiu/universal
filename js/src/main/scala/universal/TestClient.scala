package universal

import autowire._
import org.scalajs.dom
import org.scalajs.dom.html
import rx._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import universal.RxUtils._

@JSExport
object TestClient {

  object TestServiceClient extends AutowireClient("/test/")

  @JSExport
  def main(content: dom.Element): Unit = {
//    rxTest(content)
    echoTest
  }

  def rxTest(content: dom.Element): Unit = {
    val v = Var("")
    content.appendChild(
      div(
        h1("Hello"),
        ul(
          (1 to 10).map(n => li(n))
        ),
        input(onkeyup := { e: dom.Event => v() = e.target.asInstanceOf[html.Input].value }),
        br,
        Rx(span(v()))
      ).render
    )
  }

  def echoTest: Unit = {
    TestServiceClient[TestService].echoMessage("hello world").call().foreach(m => dom.console.log(m))
  }
}
