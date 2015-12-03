package universal

import scala.concurrent.Future

trait TestService {
  def echoMessage(message: String): Future[String]
}
