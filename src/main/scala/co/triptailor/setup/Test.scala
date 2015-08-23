package co.triptailor.setup

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import co.triptailor.setup.nlp.NLPAnalysisService
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext

// TODO: Need to retry reviews that get `java.lang.OutOfMemoryError` errors
object Test extends NLPAnalysisService {
  def main(args: Array[String]): Unit = {
    val parallelism = Runtime.getRuntime.availableProcessors() + 1
    val es  = Executors.newFixedThreadPool(parallelism)
    implicit val ec = ExecutionContext.fromExecutor(es)
    implicit val system = ActorSystem("nlp-test")
    implicit val materializer = ActorMaterializer()

    val start = System.currentTimeMillis()

//    syncFlow.runForeach(_.tokens foreach println) onComplete { _ =>
//      system.shutdown()
//      es.shutdown()
//      println(s"total time: ${System.currentTimeMillis() - start}")
//    }

    asyncFlow(parallelism).runForeach(_.tokens foreach println) onComplete { _ =>
      system.shutdown()
      es.shutdown()
        println(s"total time: ${System.currentTimeMillis() - start}")
    }
  }

  def syncFlow(implicit ec: ExecutionContext) =
    Source(sampleText).mapAsync(parallelism = 1)(rateReview(_, reviewYear = 2004))

  def asyncFlow(parallelism: Int)(implicit ec: ExecutionContext) =
    Source(sampleText).mapAsync(parallelism)(rateReview(_, reviewYear = 2004))

  def sampleText =
    Vector(
      """The rooms are clean, same for the bathrooms. There are activities each night.
        |The rooftop is very amazing for partying and having drinks and the staff is so friendly and nice""".stripMargin,
      "The hostel was terrible, it was dirty and stinky",
      "I've been in better places...",
      "It's been a long time since I've traveled, but the place we stayed was definitely worth it",
      "This is a completely neutral review"
    )

  def config: Config = ConfigFactory.load("nlp")

}