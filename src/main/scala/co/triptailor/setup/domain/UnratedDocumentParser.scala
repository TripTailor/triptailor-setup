package co.triptailor.setup.domain

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.io.{Framing, SynchronousFileSource}
import akka.stream.scaladsl.{FlowGraph, Source, Zip}
import akka.util.ByteString
import org.joda.time.format.DateTimeFormat

class UnratedDocumentParser(implicit system: ActorSystem, materializer: ActorMaterializer) {
  import UnratedDocumentParser._

  def parse(generalFile: File, reviewsFile: File): Source[UnratedDocument, Unit] =
    Source() { implicit b =>
      import FlowGraph.Implicits._

      val zip = b.add(Zip[UnratedHostelMetaData, Seq[UnratedReview]]())
      parseGeneralFile(generalFile) ~> zip.in0
      parseReviewsFile(reviewsFile) ~> zip.in1

      zip.out
    } map { case (unratedHostelMetaData, unratedReviewsMetaData) =>
      UnratedDocument(unratedReviewsMetaData, unratedHostelMetaData)
    }

  def parseReviewsFile(file: File) =
    SynchronousFileSource(file).
      via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = 512, allowTruncation = true)).
      map(_.utf8String).
      grouped(2).
      map { case Seq(meta, text) =>
        UnratedReview(text, parseDate(meta))
      }.fold(Seq.empty[UnratedReview]) { case (acc, unratedReviewMetaData) =>
        unratedReviewMetaData +: acc
      }

  private def parseGeneralFile(file: File) = {
    val lines = scala.io.Source.fromFile(file).getLines()
    val hostelName = lines.next().replaceAll(";", "").replaceAll("_", " ")
    lines.next(); lines.next(); lines.next(); lines.next()
    val hoscars = lines.next().toInt
    for (i ← 1 to hoscars) lines.next()
    val awards = lines.next().toInt
    for (i ← 1 to awards) lines.next()
    val noServices = lines.next().toInt
    val services   = lines.take(noServices).toSeq

    val metaData = UnratedHostelMetaData(hostelName, hoscars, services)
    Source(FastFuture.successful[UnratedHostelMetaData](metaData))
  }

  private def parseDate(line: String) =
    line match {
      case Date(day, month, year) => Some(dateFormat.parseDateTime(s"$day $month $year"))
      case _ => None
    }
}

object UnratedDocumentParser {
  val dateFormat = DateTimeFormat.forPattern("dd MMM yyyy")
  val Date = """^[^,]+,(\d{2}).*?([A-Za-z]{3}).*?(\d{4}).*?$""".r
}