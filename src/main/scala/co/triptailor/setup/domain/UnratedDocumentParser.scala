package co.triptailor.setup.domain

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.io.Framing
import akka.stream.scaladsl.{FileIO, GraphDSL, Source, Zip}
import akka.stream.{ActorMaterializer, SourceShape}
import akka.util.ByteString
import org.joda.time.format.DateTimeFormat

class UnratedDocumentParser(implicit system: ActorSystem, materializer: ActorMaterializer) {
  import UnratedDocumentParser._

  def parse(doc: DocumentEntry): Source[UnratedDocument, Unit] =
    Source.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val zip = b.add(Zip[HostelMetaData, Seq[UnratedReview]]())

      parseInfoFile(doc.infoFile, doc.city, doc.country) ~> zip.in0
      parseReviewsFile(doc.reviewFile) ~> zip.in1

      SourceShape(zip.out)
    }).map { case (unratedHostelMetaData, unratedReviewsMetaData) =>
      UnratedDocument(unratedReviewsMetaData, unratedHostelMetaData)
    }

  def parseReviewsFile(file: File) =
    FileIO.fromFile(file)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .grouped(2)
      .map { case Seq(meta, text) =>
        UnratedReview(text, parseDate(meta), parseReviewMetaData(meta))
      }.fold(Seq.empty[UnratedReview]) { case (acc, unratedReviewMetaData) =>
        unratedReviewMetaData +: acc
      }

  private def parseInfoFile(f: File, city: String, country: String) = {
    var lines = scala.io.Source.fromFile(f).getLines().toSeq
    lines = lines.tail
    val name = lines.head
    lines = lines.tail
    val uri = lines.head
    lines = lines.tail.tail
    val latLong = lines.head
    lines = lines.tail
    val desc = lines.head
    lines = lines.tail
    val noServices = lines.head.toInt
    lines = lines.tail
    val services = lines.take(noServices)
    lines = lines.drop(noServices)
    val noXtras = lines.head.toInt
    lines = lines.tail
    val xtras = lines.take(noXtras)
    val metaData = HostelMetaData(name, city, country, hoscars = 0, services)
    Source.fromFuture(FastFuture.successful(metaData))
  }

  private def parseDate(line: String) =
    line match {
      case Date(day, month, year) => Some(dateFormat.parseDateTime(s"$day $month $year"))
      case _ => None
    }

  private def parseReviewMetaData(line: String) =
    line match {
      case ReviewerMetaData(name, city, gender, age) =>
        ReviewMetaData(Some(name).filter(_.nonEmpty), Some(city).filter(_.nonEmpty), Some(gender).filter(_.nonEmpty), Some(age).filter(_.nonEmpty).map(_.toInt))
      case _ =>
        ReviewMetaData(None, None, None, None)
    }
}

object UnratedDocumentParser {
  val dateFormat = DateTimeFormat.forPattern("dd MMM yyyy")

  val Date             = """^(\d{1,2}).*?([A-Za-z]{3}).*?(\d{4}).*?$""".r
  val ReviewerMetaData = """^[^,]+,([^,]*),([^,]*),([^,]*),(\d*).*?$""".r
}
