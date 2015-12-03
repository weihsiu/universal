package universal

import scala.concurrent.Future

case class TodoState(text: String, items: List[String])

trait TodoService {
  def submitState(state: TodoState): Unit
  def retrieveState(): Future[TodoState]
}
