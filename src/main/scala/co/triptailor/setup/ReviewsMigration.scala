package co.triptailor.setup

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, JsNumber, JsValue, Json}
import slick.backend.DatabaseConfig

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.matching.Regex

object ReviewsMigration {
  import db.Tables._
  import db.drivers.ExtendedPostgresDriver.api._

  implicit val ec = scala.concurrent.ExecutionContext.global
  private[this] implicit val attributePositionWrites = Json.writes[AttributePosition]
  private[this] implicit val reviewAttributeWrites = Json.writes[ReviewAttribute]

  private val PositionsRegex = """\((\d+),(\d+),(\d+)\)""".r

  private val triptailorDB =
    DatabaseConfig.forConfig[slick.driver.PostgresDriver]("triptailor", ConfigFactory.load("db")).db

  def main(args: Array[String]): Unit =
    try {
      updateReviews()
      println("Done migrating reviews")
    } catch {
      case e: Throwable =>
        println(s"Error migrating reviews")
        println(e.getClass)
        println(e.getMessage)
        println(e.getStackTrace.mkString("\n"))
    }

  private def updateReviews() = {
    def updateReview(review: ReviewRow) =
      triptailorDB.run {
        retrieveAttributeReviewComponentsQuery(review.id)
          .result
          .map(buildAttributeReviewsData)
          .flatMap(buildUpdateReviewActions(review))
      }

    val rids = Await.result(triptailorDB.run(Review.map(_.id).result), 15.seconds)
    rids.foreach { rid =>
      Await.result(triptailorDB.run(Review.filter(_.id === rid).result.head.map(updateReview)), 7.seconds)
      print(s"$rid")
    }
  }

  private def retrieveAttributeReviewComponentsQuery(reviewId: Int) =
    for {
      ar ← AttributeReview if ar.reviewId === reviewId
      a  ← Attribute       if a.id        === ar.attributeId
    } yield (a, ar) <> (AttributeReviewDataComponent.tupled, AttributeReviewDataComponent.unapply)


  private def buildAttributeReviewsData(components: Seq[AttributeReviewDataComponent]) = {
    @annotation.tailrec
    def buildData(data: AttributeReviewData)(remaining: Seq[AttributeReviewDataComponent]): AttributeReviewData =
      if (remaining.isEmpty)
        data
      else {
        val nxt = remaining.head
        buildData(data.copy(as = data.as :+ nxt.a, ars = data.ars :+ nxt.ar))(remaining.tail)
      }
    buildData(AttributeReviewData(collection.mutable.ListBuffer(), collection.mutable.ListBuffer()))(components)
  }

  private def buildUpdateReviewActions(review: ReviewRow)(data: AttributeReviewData) = {
    val sentiments = buildSentimentsJsonb(review.sentiment)
    val attributes = buildAttributesJsonb(data)
    val updateQuery = Review.filter(_.id === review.id).map(r => (r.sentiments, r.attributes))
    updateQuery.update(Some(sentiments), Some(Json.toJson(attributes)))
  }

  private def buildSentimentsJsonb(reviewSentimentOpt: Option[String]) = reviewSentimentOpt.fold[JsValue] {
    JsArray(Seq())
  } { sentimentsString =>
    JsArray(sentimentsString.split(",").toSeq.map(nbr => JsNumber(BigDecimal(nbr))))
  }

  private def buildAttributesJsonb(data: AttributeReviewData) = {
    def extractPosition(matched: Regex.Match) = matched match {
      case PositionsRegex(start, end, sentence) => AttributePosition(start.toInt, end.toInt, sentence.toInt)
    }

    def buildReviewAttributes(ar: AttributeReviewRow) = {
      val mappings = data.as.groupBy(_.id).mapValues(_.head.name)
      val positions =
        for {
          m ← PositionsRegex.findAllMatchIn(ar.positions)
        } yield extractPosition(m)
      ReviewAttribute(ar.attributeId, mappings(ar.attributeId), positions.toSeq)
    }

    data.ars.map(buildReviewAttributes)
  }

  private[this] case class AttributePosition(start: Int, end: Int, sentence: Int)
  private[this] case class ReviewAttribute(attribute_id: Int, attribute_name: String, positions: Seq[AttributePosition])

  private[this] case class AttributeReviewData(as: Seq[AttributeRow], ars: Seq[AttributeReviewRow])
  private[this] case class AttributeReviewDataComponent(a: AttributeRow, ar: AttributeReviewRow)
}