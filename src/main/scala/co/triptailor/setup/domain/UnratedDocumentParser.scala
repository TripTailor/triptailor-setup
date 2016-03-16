package co.triptailor.setup.domain

import java.io.File
import java.net.URL

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, SourceShape}
import akka.util.ByteString
import org.joda.time.format.DateTimeFormat

class UnratedDocumentParser(implicit system: ActorSystem, materializer: ActorMaterializer) {
  import UnratedDocumentParser._

  def parse(doc: DocumentEntry): Source[UnratedDocument, NotUsed] =
    Source.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val zip = b.add(ZipWith((meta: HostelMetaData, reviews: Seq[UnratedReview], imagesUrls: Seq[String]) =>
        (meta, reviews, imagesUrls)
      ))

      parseInfoFile(doc.infoFile, doc.city, doc.country) ~> zip.in0
      parseReviewsFile(doc.reviewFile) ~> zip.in1
      parseImagesFile(doc.imagesFile) ~> zip.in2

      SourceShape(zip.out)
    }).map { case (unratedHostelMetaData, unratedReviewsMetaData, imagesUrls) =>
      UnratedDocument(unratedReviewsMetaData, unratedHostelMetaData, imagesUrls)
    }

  def parseImagesFile(file: File) =
    FileIO.fromFile(file)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .fold(Seq.empty[String])(_ :+ _)

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
    def buildLocation(latLong: String) =
      try {
        val Array(lat, long) = latLong.split(",")
        Some(Coordinates(java.lang.Double.parseDouble(lat), java.lang.Double.parseDouble(long)))
      } catch {
        case _: Throwable => None
      }

    var lines = scala.io.Source.fromFile(f).getLines().toSeq
    lines = lines.tail
    val name = lines.head
    lines = lines.tail
    val url = new URL(lines.head)
    lines = lines.tail
    val address = lines.head
    lines = lines.tail
    val coords = buildLocation(lines.head)
    lines = if (coords.nonEmpty) lines.tail else lines
    val desc = lines.head
    lines = lines.tail
    val noServices = lines.head.toInt
    lines = lines.tail
    val services = lines.take(noServices)
    lines = lines.drop(noServices)
    val noXtras = lines.head.toInt
    lines = lines.tail
    val xtras = lines.take(noXtras)
    val metaData = HostelMetaData(name, address, city, country, url, coords, desc, services, xtras)
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
