package co.triptailor.setup.db

import co.triptailor.setup.domain.{RatingMetrics, RatedDocument, RatedReview}
import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver.api._

import java.sql.Date

import scala.concurrent.{ExecutionContext, Future}

class DBTableInsertion(implicit val ec: ExecutionContext) {
  import DBTableInsertion._
  import Tables._

  def addHostelDependencies(document: RatedDocument) =
    for {
      locationId ← triptailorDB.run(idempotentInsertLocationQuery(document.info.city, document.info.country))
      hostelId   ← triptailorDB.run(idempotentInsertHostelQuery(document.info.name, document.reviews.size, locationId))
      _          ← insertHostelDependencies(document, hostelId)
    } yield hostelId

  private def insertHostelDependencies(document: RatedDocument, hostelId: Int): Future[Int] =
    for {
      serviceIds ← triptailorDB.run(DBIO.sequence(document.info.services.map(idempotentInsertServiceQuery)))
      _          ← triptailorDB.run(DBIO.sequence(insertHostelServicesQueries(hostelId, serviceIds)))
      reviewIds  ← triptailorDB.run(DBIO.sequence(document.reviews.map(review => insertReviewQuery(hostelId, review))))
      _          ← triptailorDB.run(DBIO.sequence(insertAttributeReviewsQueries(hostelId, document.reviews, reviewIds)))
      _          ← triptailorDB.run(DBIO.sequence(insertHostelAttributesQueries(hostelId, document.metrics)))
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
    HostelAttribute += row

  private def insertHostelServiceQuery(row: HostelServiceRow) =
    HostelService += row

  private def insertAttributeReviewQuery(row: AttributeReviewRow) =
    AttributeReview += row

  def idempotentInsertHostelQuery(name: String, noReviews: Int, locationId: Int) =
    Hostel.filter(_.name === name).map(_.id).result.headOption.flatMap { hostelIdOpt =>
      hostelIdOpt.fold[DBIOAction[Int,NoStream,Effect.All]] {
        Hostel.map(h => (h.name, h.noReviews, h.locationId)) returning Hostel.map(_.id) += (name, noReviews, locationId)
      } { hostelId =>
        DBIO.successful(hostelId)
      }
    }.transactionally

  private def idempotentInsertLocationQuery(city: String, country: String) =
    Location.filter(l => l.city === city && l.country === country).map(_.id).result.headOption.flatMap { locationIdOpt =>
      locationIdOpt.fold[DBIOAction[Int,NoStream,Effect.All]] {
        Location.map(l => (l.city, l.country)) returning Location.map(_.id) += (city, country)
      } { locationId =>
        DBIO.successful(locationId)
      }
    }.transactionally

  private def idempotentInsertAttributeQuery(name: String) =
    Attribute.filter(_.name === name).map(_.id).result.headOption.flatMap { attributeIdOpt =>
      attributeIdOpt.fold[DBIOAction[Int,NoStream,Effect.All]] {
        Attribute.map(_.name) returning Attribute.map(_.id) += name
      } { attributeId =>
        DBIO.successful(attributeId)
      }
    }.transactionally

  private def idempotentInsertServiceQuery(name: String) =
    Service.filter(_.name === name).map(_.id).result.headOption.flatMap { serviceIdOpt =>
      serviceIdOpt.fold[DBIOAction[Int,NoStream,Effect.All]] {
        Service.map(_.name) returning Service.map(_.id) += name
      } { serviceId =>
        DBIO.successful(serviceId)
      }
    }.transactionally

}

object DBTableInsertion {
  val triptailorDB = DatabaseConfig.forConfig[slick.driver.PostgresDriver]("triptailor", ConfigFactory.load("db")).db
}
