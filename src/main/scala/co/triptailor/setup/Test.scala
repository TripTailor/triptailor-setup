package co.triptailor.setup

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FlattenStrategy, Source}
import co.triptailor.setup.db.DBTableInsertion
import co.triptailor.setup.domain._
import co.triptailor.setup.nlp.NLPAnalysisService
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

// TODO: Need to retry reviews that get `java.lang.OutOfMemoryError` errors
object Test extends NLPAnalysisService {

  def main(args: Array[String]): Unit = {
    val parallelism = Runtime.getRuntime.availableProcessors() + 1
    implicit val system = ActorSystem("nlp-test")
    implicit val mat    = ActorMaterializer()

    val parser       = new UnratedDocumentParser
    implicit val dao = new DBTableInsertion

    Source(FileParser.documentEntries.toVector).take(20).map(parser.parse)
     .flatten(FlattenStrategy.concat).map(sourceFromUnratedReviews(_, parallelism)).flatten(FlattenStrategy.concat)
     .runForeach(println) onComplete {
      case Success(v) => println("Done inserting dependencies"); system.shutdown()
      case Failure(e) => println(e.getClass); println(e.getMessage); println(e.getStackTrace.mkString("\n")); system.shutdown()
    }

  }

  private def sourceFromUnratedReviews(unratedDocument: UnratedDocument, parallelism: Int)(implicit dao: DBTableInsertion, mat: ActorMaterializer) =
    if (unratedDocument.reviewData.isEmpty)
      Source(dao.addHostelDependencies(RatedDocument(Seq.empty, unratedDocument.info))).map(Future.successful)
    else
      Source(unratedDocument.reviewData.toVector).mapAsyncUnordered(parallelism)(rateReview)
        .grouped(Int.MaxValue).map { ratedReviews =>

        dao.addHostelDependencies(RatedDocument(ratedReviews, unratedDocument.info))

      }

  def config: Config = ConfigFactory.load("nlp")

}