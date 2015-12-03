package universal

import akka.actor.ActorSystem
import akka.stream.{OverflowStrategy, ActorMaterializer}
import akka.stream.scaladsl._
import com.softwaremill.react.kafka.{ProducerProperties, ReactiveKafka}
import kafka.serializer.{Encoder, StringEncoder}
import org.reactivestreams.Subscriber
import upickle.default._

object KafkaStreams {
  implicit val system = ActorSystem("KafkaStreams")
  implicit val materializer = ActorMaterializer()

  val kafka = new ReactiveKafka()
  val todoSubscriber: Subscriber[TodoState] = kafka.publish(ProducerProperties(
    brokerList = "localhost:9092",
    topic = "todo",
    encoder = new Encoder[TodoState] {
      val encoder = new StringEncoder
      def toBytes(x: TodoState): Array[Byte] = encoder.toBytes(write(x))
    }
  ))

  val todoSourceQueue = Source.queue(10, OverflowStrategy.dropHead).to(Sink(todoSubscriber)).run
}
