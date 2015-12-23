package co.triptailor.setup

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
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
      .flatMapConcat(parser.parse)
      .flatMapConcat(sourceFromUnratedReviews(_, parallelism))
      .runForeach(println) onComplete {
        case Success(v) =>
          println("Done inserting dependencies")
          system.terminate()
        case Failure(e) =>
          println(e.getClass)
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          system.terminate()
      }

  }

  private def sourceFromUnratedReviews(unratedDocument: UnratedDocument, parallelism: Int)
                                      (implicit dao: DBTableInsertion, mat: ActorMaterializer) =
    if (unratedDocument.reviewData.isEmpty)
      Source.fromFuture(dao.addHostelDependencies(RatedDocument(Seq.empty, Map.empty, unratedDocument.info)))
        .map { hostelId =>
          println(s"Done inserting hostel info for ${unratedDocument.info.name} with hostel_id=$hostelId")
          hostelId
        }.map(Future.successful)
    else
      Source(unratedDocument.reviewData.toVector).mapAsyncUnordered(parallelism)(rateReview)
        .grouped(Int.MaxValue).mapAsync(parallelism = 1) { ratedReviews =>

        val metrics = ratedReviews.map(_.metrics).reduce(mergeMetrics)
        dao.addHostelDependencies(RatedDocument(ratedReviews, metrics, unratedDocument.info)).map { hostelId =>
          println(s"Done inserting hostel info for ${unratedDocument.info.name} with hostel_id=$hostelId")
          hostelId
        }
      }

  def config: Config = ConfigFactory.load("nlp")

}
