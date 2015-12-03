package universal

import autowire.Client
import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import upickle.{json, Js}
import upickle.default._

class AutowireClient(path: String) extends Client[Js.Value, Reader, Writer] {
  def read[A: Reader](x: Js.Value) = readJs[A](x)
  def write[A: Writer](x: A) = writeJs(x)
  def doCall(req: Request): Future[Js.Value] =
    dom.ext.Ajax.post(
      url = path + req.path.mkString("/"),
      data = json.write(Js.Obj(req.args.toSeq: _*))
    ).map(_.responseText).map(json.read)
}
