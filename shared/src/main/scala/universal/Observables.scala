package universal

import monifu.concurrent.Scheduler
import monifu.concurrent.cancelables.BooleanCancelable
import monifu.reactive.Ack.Cancel
import monifu.reactive.Observable
import scala.concurrent.Future

object Observables {
  implicit class RichObservable[+A](val observable: Observable[A]) extends AnyVal {
    def once(nextFn: A => Unit)(implicit s: Scheduler): BooleanCancelable = observable subscribe { x =>
      nextFn(x)
      Future.successful(Cancel)
    }
  }
}
