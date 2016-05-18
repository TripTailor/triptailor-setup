package co.triptailor.setup.db

import java.sql.Date

import co.triptailor.setup.domain.{HostelMetaData, RatedDocument, RatedReview, RatingMetrics}
import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig

import scala.concurrent.ExecutionContext

class DBTableInsertion(implicit val ec: ExecutionContext) {
  import DBTableInsertion._
  import Tables._
  import slick.driver.PostgresDriver.api._

  def addHostelDependencies(document: RatedDocument) =
    triptailorDB.run {
      for {
        locationId ← insertLocationQuery(document.info.city, document.info.country)
        hostelId   ← insertHostelQuery(document.info, document.reviews.size, locationId, document.imagesUrls)
        _          ← insertHostelDependencies(document, hostelId)
      } yield hostelId
    }

  private def insertHostelDependencies(document: RatedDocument, hostelId: Int) =
    for {
      serviceIds ← DBIO.sequence(document.info.services.map(insertServiceQuery))
      _          ← DBIO.sequence(insertHostelServicesQueries(hostelId, serviceIds))
      reviewIds  ← DBIO.sequence(document.reviews.map(review => insertReviewQuery(hostelId, review)))
      _          ← DBIO.sequence(insertAttributeReviewsQueries(hostelId, document.reviews, reviewIds))
      _          ← DBIO.sequence(insertHostelAttributesQueries(hostelId, document.metrics))
    } yield hostelId

  private def insertAttributeReviewsQueries(hostelId: Int, reviews: Seq[RatedReview], reviewIds: Seq[Int]) =
    for {
      (review, reviewId) ← reviews zip reviewIds
      token              ← review.tokens
    } yield idempotentInsertAttributeQuery(token.attribute) flatMap { aid =>
      val tokenPositions = token.positions.mkString(",")
      insertAttributeReviewQuery(AttributeReviewRow(aid, reviewId, tokenPositions))
    }

  private def insertHostelAttributesQueries(hostelId: Int, metrics: Map[String, RatingMetrics]) =
    for {
      attribute ← metrics.keys
    } yield idempotentInsertAttributeQuery(attribute) flatMap { aid =>
      val m = metrics(attribute)
      insertHostelAttributeQuery(HostelAttributeRow(hostelId, aid, m.freq, m.cfreq, m.sentiment))
    }

  private def insertHostelServicesQueries(hostelId: Int, serviceIds: Seq[Int]) =
    serviceIds.map(sid => insertHostelServiceQuery(HostelServiceRow(hostelId, sid)))

  private def insertReviewQuery(hostelId: Int, review: RatedReview) =
    Review.map(r => (r.hostelId, r.text, r.year, r.reviewer, r.city, r.gender, r.age, r.sentiment)) returning Review.map(_.id) += {
      (hostelId, review.text, review.date.map(d => new Date(d.getMillis)), review.meta.reviewer,
        review.meta.city, review.meta.gender, review.meta.age, Some(review.sentiments.mkString(",")))
    }

  private def insertHostelAttributeQuery(row: HostelAttributeRow) =
    (HostelAttribute returning HostelAttribute.map(row => (row.hostelId, row.attributeId))) += row

  private def insertHostelServiceQuery(row: HostelServiceRow) =
    (HostelService returning HostelService.map(row => (row.hostelId, row.serviceId))) += row

  private def insertAttributeReviewQuery(row: AttributeReviewRow) =
    (AttributeReview returning AttributeReview.map(row => (row.attributeId, row.reviewId))) += row

  private def insertHostelQuery(info: HostelMetaData, noReviews: Int, locationId: Int, imagesUrls: Seq[String]) =
    Hostel.map(h => (h.name, h.noReviews, h.locationId, h.address, h.description, h.images)) returning Hostel.map(_.id) += {
      (info.name, noReviews, locationId, Some(info.address), Some(info.description), Some(imagesUrls.mkString(",")))
    }

  private def insertLocationQuery(city: String, country: String) =
    Location.map(l => (l.city, l.country)) returning Location.map(_.id) += (city, country)

  private def insertServiceQuery(name: String) =
    Service.map(_.name) returning Service.map(_.id) += name

  private def idempotentInsertAttributeQuery(name: String) =
    Attribute.filter(_.name === name).map(_.id).result.headOption.flatMap { attributeIdOpt =>
      attributeIdOpt.fold[DBIOAction[Int,NoStream,Effect.All]] {
        Attribute.map(_.name) returning Attribute.map(_.id) += name
      } { attributeId =>
        DBIO.successful(attributeId)
      }
    }.transactionally

}

object DBTableInsertion {
  val triptailorDB = DatabaseConfig.forConfig[slick.driver.PostgresDriver]("triptailor", ConfigFactory.load("db")).db
}