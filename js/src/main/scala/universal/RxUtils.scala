package universal

import org.scalajs.dom.{Element, html}
import rx._
import rx.core.Obs
import scala.language.implicitConversions
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

// originally from https://github.com/lihaoyi/workbench-example-app/blob/todomvc/src/main/scala/example/Framework.scala
object RxUtils {

  /**
    * Wraps reactive strings in spans, so they can be referenced/replaced
    * when the Rx changes.
    */
  implicit def RxStr[T](r: Rx[T])(implicit f: T => Frag): Frag = {
    rxMod(Rx(span(r())))
  }

  /**
    * Sticks some Rx into a Scalatags fragment, which means hooking up an Obs
    * to propagate changes into the DOM via the element's ID. Monkey-patches
    * the Obs onto the element itself so we have a reference to kill it when
    * the element leaves the DOM (e.g. it gets deleted).
    */
  implicit def rxMod[T <: html.Element](r: Rx[HtmlTag]): Frag = {
    def rSafe = r.toTry match {
      case Success(v) => v.render
      case Failure(e) => span(e.toString, backgroundColor := "red").render
    }
    var last = rSafe
    Obs(r, skipInitial = true){
      val newLast = rSafe
      last.parentElement.replaceChild(newLast, last)
      last = newLast
    }
    bindNode(last)
  }
  implicit def RxAttrValue[T: AttrValue] = new AttrValue[Rx[T]]{
    def apply(t: Element, a: Attr, r: Rx[T]): Unit = {
      Obs(r){ implicitly[AttrValue[T]].apply(t, a, r())}
    }
  }
  implicit def RxStyleValue[T: StyleValue] = new StyleValue[Rx[T]]{
    def apply(t: Element, s: Style, r: Rx[T]): Unit = {
      Obs(r){ implicitly[StyleValue[T]].apply(t, s, r())}
    }
  }
}