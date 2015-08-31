package co.triptailor.setup

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import co.triptailor.setup.db.DBTableInsertion
import co.triptailor.setup.domain.{RatedDocument, UnratedReview, UnratedDocumentParser}
import co.triptailor.setup.nlp.NLPAnalysisService
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

// TODO: Need to retry reviews that get `java.lang.OutOfMemoryError` errors
object Test extends NLPAnalysisService {
  def main(args: Array[String]): Unit = {
    val parallelism = Runtime.getRuntime.availableProcessors() + 1
    implicit val system = ActorSystem("nlp-test")
    implicit val materializer = ActorMaterializer()

    val start = System.currentTimeMillis()

    val parser = new UnratedDocumentParser
    val dao    = new DBTableInsertion

    val city = "San Francisco"
    val country = "USA"
    parser.parse(
      new java.io.File(s"/Users/sheaney/Documents/triptailor-setup/data/$country/$city/HI_-_San_Francisco_-_City_Center_general.txt"),
      new java.io.File(s"/Users/sheaney/Documents/triptailor-setup/data/$country/$city/HI_-_San_Francisco_-_City_Center_reviews.txt"),
      country,
      city
    ).mapAsync(parallelism = 1) { unratedDocument =>
      val info = unratedDocument.info
      Source(unratedDocument.reviewData.toVector).mapAsyncUnordered(parallelism)(rateReview)
        .grouped(Int.MaxValue).runWith(Sink.head) map { ratedReviews =>
        dao.addHostelDependencies(RatedDocument(ratedReviews, info))
      }
    } runForeach (_ foreach println) onComplete {
      case Success(v) => println("Done inserting dependencies"); system.shutdown()
      case Failure(e) => println(e.getMessage); println(e.getStackTrace.mkString("\n")); system.shutdown()
    }

//    parser.parse(
//      new java.io.File("/Users/sheaney/Documents/triptailor-setup/data/USA/San Francisco/HI_-_San_Francisco_-_City_Center_general.txt"),
//      new java.io.File("/Users/sheaney/Documents/triptailor-setup/data/USA/San Francisco/HI_-_San_Francisco_-_City_Center_reviews.txt")
//    ).runForeach(println) onComplete {
//      _ => println("Done parsing files")
//    }

//    syncFlow.runForeach(_.tokens foreach println) onComplete { _ =>
//      system.shutdown()
//      es.shutdown()
//      println(s"total time: ${System.currentTimeMillis() - start}")
//    }

//    asyncFlow(parallelism).runForeach(_.tokens foreach println) onComplete { _ =>
//      system.shutdown()
//      es.shutdown()
//      println(s"total time: ${System.currentTimeMillis() - start}")
//    }

  }

  def syncFlow(implicit ec: ExecutionContext) =
    Source(sampleText).mapAsync(parallelism = 1)(text => rateReview(UnratedReview(text, None)))

  def asyncFlow(parallelism: Int)(implicit ec: ExecutionContext) =
    Source(sampleText).mapAsyncUnordered(parallelism)(text => rateReview(UnratedReview(text, None)))

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