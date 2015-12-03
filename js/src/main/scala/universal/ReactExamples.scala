package universal

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import java.util.concurrent.TimeoutException
import monifu.concurrent.Cancelable
import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive._
import monifu.reactive.subjects.PublishSubject
import org.scalajs.dom
import scala.async.Async._
import scala.concurrent.duration._
import scala.language.{existentials, postfixOps}
import scala.scalajs.js.annotation.JSExport
import scala.util.Try
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import ExampleStyles._

@JSExport
object ReactExamples {
  @JSExport
  def main(content: dom.Element): Unit = {
    ExampleStyles.addToDocument
//        hello(content)
//        timer(content)
//        todo1(content)
//    todo2(content)
    todo3(content)
  }
  def hello(content: dom.Element): Unit = {
    val Hello = ReactComponentB[String]("Hello")
        .render($ => <.div("Hello ", $.props))
        .build
    ReactDOM.render(Hello("Walter"), content)
  }
  def timer(content: dom.Element): Unit = {
    val Timer = {
      case class State(secondsElapsed: Long)
      class Backend($: BackendScope[Unit, State]) {
        var cancelable: Option[Cancelable] = None
        def tick = $.modState(s => State(s.secondsElapsed + 1))
        def start = Callback {
          cancelable = Some(
            Observable
                .interval(1 second)
                .doWork(_ => tick.runNow)
                .subscribe
          )
        }
        def stop = Callback(cancelable.foreach(_.cancel))
        def render(s: State) = <.div(s"Second elapsed: ${s.secondsElapsed}")
      }
      ReactComponentB[Unit]("Timer")
          .initialState(State(0))
          .renderBackend[Backend]
          .componentDidMount(_.backend.start)
          .componentWillUnmount(_.backend.stop)
          .buildU
    }
    ReactDOM.render(Timer(), content)
  }
  def todo1(content: dom.Element): Unit = {
    val TodoList = ReactComponentB[List[String]]("TodoList")
        .render_P(props => <.ul(props.map(<.li(_))))
        .build
    val TodoApp = {
      case class State(text: String, items: List[String])
      class Backend($: BackendScope[Unit, State]) {
        def addItem(e: ReactEventI) =
          $.modState(s => State("", s.text +: s.items)) >> e.preventDefaultCB
        def textChanged(e: ReactEventI) = $.modState(_.copy(text = e.target.value))
        def render(s: State) =
          <.div(
            <.h3("TODO"),
            TodoList(s.items),
            <.form(^.onSubmit ==> addItem,
              <.input(^.onChange ==> textChanged, ^.value := s.text),
              <.button("Add #", s.items.length + 1)
            )
          )
      }
      ReactComponentB[Unit]("TodoApp")
          .initialState(State("", Nil))
          .renderBackend[Backend]
          .buildU
    }
    ReactDOM.render(TodoApp(), content)
  }
  def todo2(content: dom.Element): Unit = {
    trait Event
    case object AddItem extends Event
    case class RemoveItem(item: String) extends Event
    case class TextChanged(text: String) extends Event
    case object GoBackInTime extends Event
    val eventBus = PublishSubject[Event]
    def postEvent(e: Event): Callback = Callback(eventBus.onNext(e))

    val TodoList = ReactComponentB[List[String]]("TodoList")
        .render_P(props => <.ul(props.map(item => <.li(^.onClick --> postEvent(RemoveItem(item)), item))))
        .build
    val TodoApp = {
      case class State(text: String, items: List[String])
      var states: List[State] = List.empty
      class Backend($: BackendScope[Unit, State]) {
        eventBus foreach {
          case AddItem => $.modState(s => State("", s.text +: s.items)).runNow
          case RemoveItem(item) => $.modState(s => s.copy(items = s.items.filterNot(_ == item))).runNow
          case TextChanged(text) => $.modState(_.copy(text = text)).runNow
          case GoBackInTime => states match {
            case _ :: s :: ss =>
              states = ss
              $.modState(_ => s).runNow
            case _ =>
          }
        }
        def render(s: State) = {
          states = s +: states
          <.div(
            <.a(^.onClick --> postEvent(GoBackInTime), "Go back in time"),
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
          .initialState(State("", Nil))
          .renderBackend[Backend]
          .buildU
    }
    ReactDOM.render(TodoApp(), content)
  }
  def todo3(content: dom.Element): Unit = {
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
        eventBus foreach{
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
            <.h3(alert(monitor), "TODO"),
            TodoList(s.items),
            <.form(^.onSubmit ==> ((e: ReactEventI) => postEvent(AddItem) >> e.preventDefaultCB),
              <.input(^.onChange ==> ((e: ReactEventI) => postEvent(TextChanged(e.target.value))), ^.value := s.text),
              <.button(defaultButton, "Add #", s.items.length + 1)
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
