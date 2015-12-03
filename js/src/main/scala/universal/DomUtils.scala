package universal

import org.scalajs.dom

object DomUtils {
  def queryParams: Map[String, String] = {
    if (dom.location.search.isEmpty) Map.empty
    else Map(dom.location.search.tail.split('&') map { kv =>
      val Array(k, v) = kv.split('=')
      (k, v)
    }: _*)
  }
  def removeChildren(node: dom.Node): Unit = {
    while (node.hasChildNodes()) node.removeChild(node.lastChild)
  }
}