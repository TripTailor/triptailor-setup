package co.triptailor.setup.db

import co.triptailor.setup.domain.{RatedDocument, RatedReview}
import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class DBTableInsertion(implicit val ec: ExecutionContext) {
  import DBTableInsertion._
  import db.Tables._

  def addHostelDependencies(document: RatedDocument) = {
    val info = document.info
    for {
      locationId ← idempotentInsertLocation(info.city, info.country)
      hostelId   ← insertHostelInfo(info.name, document.reviews.size, locationId)
      _          ← insertHostelDependencies(document, hostelId)
    } yield hostelId
  }

  def insertHostelInfo(name: String, noReviews: Int, locationId: Int): Future[Int] =
    triptailorDB.run(insertHostelInfoQuery(name, noReviews, locationId))

  def insertHostelDependencies(document: RatedDocument, hostelId: Int): Future[Int] =
    for {
      serviceIds  ← Future.sequence(document.info.services.map(idempotentInsertService))
      _           = insertHostelServices(hostelId, serviceIds)
      reviewIds   ← Future.sequence(document.reviews.map(_.text).map(text => triptailorDB.run(insertReviewQuery(hostelId, text))))
      _           = insertHostelAttributes(hostelId, document, reviewIds)
    } yield hostelId

  private def insertHostelAttributes(hostelId: Int, document: RatedDocument, reviewIds: Seq[Int]) =
    document.reviews zip reviewIds foreach { case (review, reviewId) =>
      val ratingMetrics = document.metrics
      val tokens        = review.tokens

      tokens foreach { token =>
        idempotentInsertAttribute(token.attribute).map { aid =>
          val tokenPositions = token.positions.mkString(",")
          val metrics        = ratingMetrics(token.attribute)
          triptailorDB.run(DBIO.seq(
            insertAttributeReviewQuery(AttributeReviewRow(aid, reviewId, tokenPositions)),
            insertHostelAttributeQuery(HostelAttributeRow(hostelId, aid, metrics.freq, metrics.cfreq, metrics.sentiment))
          ))
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