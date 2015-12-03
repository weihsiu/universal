package universal

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.reactive.subjects.PublishSubject
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport
import monifu.concurrent.Implicits.globalScheduler
import scala.async.Async._

@JSExport
object Todos {
  @JSExport
  def main(content: dom.Element): Unit = {
    todo(content)
  }
  def todo(content: dom.Element): Unit = {
    val monitor = DomUtils.queryParams.get("monitor").map(_ == "true").getOrElse(false)

    object TodoServiceClient extends AutowireClient("/todo/")

    trait Event
    case object AddItem extends Event
    case class RemoveItem(item: String) extends Event
    case class TextChanged(text: String) extends Event
    case class SubmitState(state: TodoState) extends Event
    case class StateRetrieved(state: TodoState) extends Event
    val eventBus = PublishSubject[Event]
    def postEvent(e: Event): Callback = Callback(eventBus.onNext(e))

    val TodoList = ReactComponentB[List[String]]("TodoList")
        .render_P(props => <.ul(props.map(item => <.li(^.onClick --> postEvent(RemoveItem(item)), item))))
        .build
    val TodoApp = {
      var states: List[TodoState] = List.empty
      class Backend($: BackendScope[Unit, TodoState]) {
        eventBus foreach {
          case AddItem => $.modState(s => TodoState("", s.text +: s.items)).runNow
          case RemoveItem(item) => $.modState(s => s.copy(items = s.items.filterNot(_ == item))).runNow
          case TextChanged(text) => $.modState(_.copy(text = text)).runNow
          case SubmitState(state) => TodoServiceClient[TodoService].submitState(state).call()
          case StateRetrieved(state) => $.modState(_ => state).runNow
        }
        def startRetrievingStates = Callback(if (monitor) {
          async {
            while (true) {
              val state = await(TodoServiceClient[TodoService].retrieveState().call())
              $.modState(_ => state).runNow
            }
          }
        })
        def render(s: TodoState) = {
          if (!monitor) eventBus.onNext(SubmitState(s))
          <.div(
            <.h3("TODO"),
            TodoList(s.items),
            <.form(^.onSubmit ==> ((e: ReactEventI) => postEvent(AddItem) >> e.preventDefaultCB),
              <.input(^.onChange ==> ((e: ReactEventI) => postEvent(TextChanged(e.target.value))), ^.value := s.text),
              <.button("Add #", s.items.length + 1)
            )
          )
        }
      }
      ReactComponentB[Unit]("TodoApp")
          .initialState(TodoState("", Nil))
          .renderBackend[Backend]
          .componentDidMount(_.backend.startRetrievingStates)
          .buildU
    }
    ReactDOM.render(TodoApp(), content)
  }
}