package co.triptailor.setup

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, JsNumber, JsValue, Json}
import slick.backend.DatabaseConfig

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

  def main(args: Array[String]): Unit = {

  }

  private def addReviewSentiments(reviews: Seq[ReviewRow]) = {
    def buildSentimentsJsonb(review: ReviewRow) = review.sentiment.fold[JsValue] {
      JsArray(Seq())
    } { sentimentsString =>
      JsArray(sentimentsString.split(",").toSeq.map(nbr => JsNumber(BigDecimal(nbr))))
    }
    reviews.map(buildSentimentsJsonb)
  }

  private def buildUpdateReviewData(data: Seq[ReviewData]) = {
    def buildSentimentsJsonb(review: ReviewRow) = review.sentiment.fold[JsValue] {
      JsArray(Seq())
    } { sentimentsString =>
      JsArray(sentimentsString.split(",").toSeq.map(nbr => JsNumber(BigDecimal(nbr))))
    }

    def buildAttributesJsonb(data: ReviewData) = {
      def extractPosition(matched: Regex.Match) = matched match {
        case PositionsRegex(start, end, sentence) => AttributePosition(start.toInt, end.toInt, sentence.toInt)
      }

      val mappings = data.attributes.groupBy(_.id).mapValues(_.head.name)

      data.reviewAttributes.map { ar =>
        val positions =
          for {
            m ← PositionsRegex.findAllMatchIn(ar.positions)
          } yield extractPosition(m)
        ReviewAttribute(ar.attributeId, mappings(ar.attributeId), positions.toSeq)
      }

    }

    reviewsDataQuery.result.map(buildReviewsData).flatMap { reviewsData =>
      DBIO.sequence {
        reviewsData.map { data =>
          val sentiments = buildSentimentsJsonb(data.review)
          val attributes = buildAttributesJsonb(data)
          val updateQuery = Review.filter(_.id === data.review.id).map(r => (r.sentiments, r.attributes))

          updateQuery.update(Some(sentiments), Some(Json.toJson(attributes)))
        }
      }
    }
  }

  private def buildReviewsData(components: Seq[ReviewDataComponent]) = {
    @scala.annotation.tailrec
    def buildReviewData(acc: ReviewData)(grouped: Seq[ReviewDataComponent]): ReviewData =
      if (grouped.isEmpty)
        acc
      else {
        val comp = grouped.head
        val reviewData =
          if (acc == null)
            ReviewData(comp.r, Seq(comp.ar), Seq(comp.a))
          else
            acc.copy(reviewAttributes = acc.reviewAttributes :+ comp.ar, attributes = acc.attributes :+ comp.a)
        buildReviewData(reviewData)(grouped.tail)
      }

    components.groupBy(_.r.id).mapValues(buildReviewData(ReviewData(null, null, null))).values
  }

  private def reviewsDataQuery =
    for {
      (r, ar) ← Review join AttributeReview on (_.id === _.reviewId)
      (ar, a) ← AttributeReview join Attribute on (_.attributeId === _.id)
    } yield (r, ar, a) <> (ReviewDataComponent.tupled, ReviewDataComponent.unapply)

  private[this] case class ReviewDataComponent(r: ReviewRow, ar: AttributeReviewRow, a: AttributeRow)
  private[this] case class ReviewData(review: ReviewRow, reviewAttributes: Seq[AttributeReviewRow], attributes: Seq[AttributeRow])

  private[this] case class AttributePosition(start: Int, end: Int, sentence: Int)
  private[this] case class ReviewAttribute(id: Int, name: String, positions: Seq[AttributePosition])

}
