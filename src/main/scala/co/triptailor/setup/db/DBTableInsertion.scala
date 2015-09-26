package co.triptailor.setup.db

import co.triptailor.setup.domain.{RatingMetrics, RatedDocument, RatedReview}
import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class DBTableInsertion(implicit val ec: ExecutionContext) {
  import DBTableInsertion._
  import db.Tables._

  def addHostelDependencies(document: RatedDocument) =
    for {
      locationId ← idempotentInsertLocation(document.info.city, document.info.country)
      hostelId   ← insertHostelInfo(document.info.name, document.reviews.size, locationId)
      _          ← insertHostelDependencies(document, hostelId)
    } yield hostelId

  private def insertHostelInfo(name: String, noReviews: Int, locationId: Int): Future[Int] =
    triptailorDB.run(insertHostelInfoQuery(name, noReviews, locationId))

  private def insertHostelDependencies(document: RatedDocument, hostelId: Int): Future[Int] =
    for {
      serviceIds ← Future.sequence(document.info.services.map(idempotentInsertService))
      _          ← insertHostelServices(hostelId, serviceIds)
      reviewIds  ← Future.sequence(document.reviews.map(_.text).map(text => triptailorDB.run(insertReviewQuery(hostelId, text))))
      _          ← insertAttributeReviews(hostelId, document.reviews, reviewIds)
      _          ← insertHostelAttributes(hostelId, document.metrics)
    } yield hostelId

  private def insertAttributeReviews(hostelId: Int, reviews: Seq[RatedReview], reviewIds: Seq[Int]) =
    Future.sequence {
      reviews zip reviewIds flatMap { case (review, reviewId) =>
        val tokens = review.tokens
        tokens map { token =>
          idempotentInsertAttribute(token.attribute) map { aid =>
            val tokenPositions = token.positions.mkString(",")
            triptailorDB.run(insertAttributeReviewQuery(AttributeReviewRow(aid, reviewId, tokenPositions)))
          }
        }
      }
    }

  private def insertHostelAttributes(hostelId: Int, metrics: Map[String,RatingMetrics]) =
    Future.sequence {
      metrics.keySet map { attribute =>
        idempotentInsertAttribute(attribute) map { aid =>
          val m = metrics(attribute)
          triptailorDB.run(insertHostelAttributeQuery(HostelAttributeRow(hostelId, aid, m.freq, m.cfreq, m.sentiment)))
        }
      }
    }

  private def insertHostelServices(hostelId: Int, serviceIds: Seq[Int]) =
    triptailorDB.run(DBIO.seq(serviceIds.map(sid => insertHostelServiceQuery(HostelServiceRow(hostelId, sid))): _*))

  private def insertHostelInfoQuery(name: String, noReviews: Int, locationId: Int) =
    Hostel.map(h => (h.name, h.noReviews, h.locationId)) returning Hostel.map(_.id) += (name, noReviews, locationId)

  private def insertReviewQuery(hostelId: Int, text: String) =
    Review.map(r => (r.hostelId, r.text)) returning Review.map(_.id) += (hostelId, text)

  private def insertHostelAttributeQuery(row: HostelAttributeRow) =
    HostelAttribute += row

  private def insertHostelServiceQuery(row: HostelServiceRow) =
    HostelService += row

  private def insertAttributeReviewQuery(row: AttributeReviewRow) =
    AttributeReview += row

  private def idempotentInsertLocation(city: String, country: String): Future[Int] =
    triptailorDB.run(Location.filter(l => l.city === city && l.country === country).map(_.id).result.headOption) flatMap { locationIdOpt =>
      locationIdOpt.fold {
        triptailorDB.run(Location.map(l => (l.city, l.country)) returning Location.map(_.id) += (city, country))
      } { locationId =>
        Future.successful(locationId)
      }
    }

  private def idempotentInsertAttribute(name: String): Future[Int] =
    triptailorDB.run(Attribute.filter(_.name === name).map(_.id).result.headOption) flatMap { attributeIdOpt =>
      attributeIdOpt.fold {
        triptailorDB.run(Attribute.map(_.name) returning Attribute.map(_.id) += name)
      } { attributeId =>
        Future.successful(attributeId)
      }
    }

  private def idempotentInsertService(name: String): Future[Int] =
    triptailorDB.run(Service.filter(_.name === name).map(_.id).result.headOption) flatMap { serviceIdOpt =>
      serviceIdOpt.fold {
        triptailorDB.run(Service.map(_.name) returning Service.map(_.id) += name)
      } { serviceId =>
        Future.successful(serviceId)
      }
    }

}

object DBTableInsertion {
  val triptailorDB = DatabaseConfig.forConfig[slick.driver.PostgresDriver]("triptailor", ConfigFactory.load("db")).db
}