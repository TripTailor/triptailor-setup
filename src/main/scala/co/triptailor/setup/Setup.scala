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

object Setup extends NLPAnalysisService {
  def main(args: Array[String]): Unit = {
    val parallelism = Runtime.getRuntime.availableProcessors() + 1

    implicit val system = ActorSystem("nlp-test")
    implicit val mat = ActorMaterializer()
    implicit val dao = new DBTableInsertion

    val parser = new UnratedDocumentParser

    Source(FileParser.documentEntries.toVector)
      .dropWhile(_.generalFile.getName != (startEntry + "_general.txt"))
      .map(parser.parse)
      .flatten(FlattenStrategy.concat)
      .map(sourceFromUnratedReviews(_, parallelism))
      .flatten(FlattenStrategy.concat)
      .runForeach(println) onComplete {
        case Success(v) =>
          println("Done inserting dependencies")
          system.shutdown()
        case Failure(e) =>
          println(e.getClass)
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          system.shutdown()
      }

  }

  private def sourceFromUnratedReviews(unratedDocument: UnratedDocument, parallelism: Int)
                                      (implicit dao: DBTableInsertion, mat: ActorMaterializer) =
    if (unratedDocument.reviewData.isEmpty)
      Source(dao.addHostelDependencies(RatedDocument(Seq.empty, Map.empty, unratedDocument.info)))
        .map { hostelId =>
          println(s"Done inserting hostel info for ${unratedDocument.info.name} with hostel_id=$hostelId")
          hostelId
        }.map(Future.successful)
    else
      Source(unratedDocument.reviewData.toVector).mapAsyncUnordered(parallelism)(rateReview)
        .grouped(Int.MaxValue).mapAsync(parallelism = 4) { ratedReviews =>

        val metrics = ratedReviews.map(_.metrics).reduce(mergeMetrics)
        dao.addHostelDependencies(RatedDocument(ratedReviews, metrics, unratedDocument.info)).map { hostelId =>
          println(s"Done inserting hostel info for ${unratedDocument.info.name} with hostel_id=$hostelId")
          hostelId
        }
      }

  def config: Config = ConfigFactory.load("nlp")

}
