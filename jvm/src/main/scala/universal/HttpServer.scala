package universal

import autowire.Core.Request
import java.io.File
import monifu.reactive.subjects.PublishSubject
import monifu.concurrent.Implicits.globalScheduler
import org.http4s.StaticFile
import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scalaz.concurrent.Task
import upickle.{Js, json}
import Observables._

object HttpServer extends App {

  object TestServiceImpl extends TestService {
    def echoMessage(message: String) = Future.successful(message)
  }

  object TodoServiceImpl extends TodoService {
    val states = PublishSubject[TodoState]
    def submitState(state: TodoState) = {
      states.onNext(state)
      KafkaStreams.todoSourceQueue.offer(state)
    }
    def retrieveState() = {
      val p = Promise[TodoState]
      states.once(p.success(_))
      p.future
    }
  }

  val httpService = HttpService {
    case req @ POST -> "test" /: path =>
      req.decode[String] { data =>
        Ok(AutowireServer.route[TestService]
            (TestServiceImpl)
            (Request(path.toList, json.read(data).asInstanceOf[Js.Obj].value.toMap)).map(json.write))
      }
    case req @ POST -> "todo" /: path =>
      req.decode[String] { data =>
        Ok(AutowireServer.route[TodoService]
            (TodoServiceImpl)
            (Request(path.toList, json.read(data).asInstanceOf[Js.Obj].value.toMap)).map(json.write))
      }
    case req @ GET -> "static" /: path =>
      StaticFile.fromFile(new File(s"../web${path.toString}"), Some(req)).fold(NotFound())(Task.now)
  }

  BlazeBuilder.bindHttp(8080)
      .mountService(httpService, "/")
      .withIdleTimeout(5 minute)
      .run
      .awaitShutdown
}
