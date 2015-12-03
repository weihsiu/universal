package universal

import autowire.Server
import upickle.Js
import upickle.default._

object AutowireServer extends Server[Js.Value, Reader, Writer] {
  def read[A: Reader](x: Js.Value) = readJs[A](x)
  def write[A: Writer](x: A) = writeJs(x)
}