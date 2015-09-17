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

  }

  def config: Config = ConfigFactory.load("nlp")

}