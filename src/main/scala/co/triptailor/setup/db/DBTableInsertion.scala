package co.triptailor.setup.db

import co.triptailor.setup.domain.{RatingMetrics, RatedDocument}
import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class DBTableInsertion(implicit val ec: ExecutionContext) {
  import DBTableInsertion._
  import Tables._

  def addHostelDependencies(document: RatedDocument) = {
    val info = document.info
    for {
      locationId ← idempotentInsertLocation(info.city, info.country)
      hostelId   ← insertHostelInfo(info.name, document.reviews.size, locationId)
      _          ← insertHostelDependencies(document, hostelId)
    } yield hostelId
  }

  def insertLocation(city: String, country: String): Future[Int] =
    triptailorDB.run(insertLocationQuery(city, country))

  def insertHostelInfo(name: String, noReviews: Int, locationId: Int): Future[Int] =
    triptailorDB.run(insertHostelInfoQuery(name, noReviews, locationId))

  def getLocationId(city: String, country: String): Future[Option[Int]] =
    triptailorDB.run(getLocationIdQuery(city, country).result.headOption)

  def insertHostelDependencies(document: RatedDocument, hostelId: Int): Future[Int] =
    for {
      reviewIds   ← Future.sequence(document.reviews.map(_.text).map(text => triptailorDB.run(insertReviewQuery(hostelId, text))))
      serviceIds  ← Future.sequence(document.info.services.map(idempotentInsertService))
      _           = insertHostelAttributes(hostelId, document.reviews.flatMap(_.metrics), reviewIds, serviceIds)
    } yield hostelId

  private def insertHostelAttributes(hostelId: Int, attributes: Seq[(String,RatingMetrics)], reviewIds: Seq[Int], serviceIds: Seq[Int]) =
    attributes zip reviewIds zip serviceIds map { case (((attr, metrics), reviewId), serviceId) =>
      idempotentInsertAttribute(attr).map { aid =>
        triptailorDB.run(insertHostelServiceQuery(HostelServiceRow(hostelId, serviceId)))
        triptailorDB.run(insertAttributeReviewQuery(AttributeReviewRow(aid, reviewId)))
        triptailorDB.run(insertHostelAttributeQuery(HostelAttributeRow(hostelId, aid, metrics.freq, metrics.cfreq, metrics.sentiment)))
//        triptailorDB.run(DBIO.seq(
//          insertHostelServiceQuery(HostelServiceRow(hostelId, serviceId)),
//          insertAttributeReviewQuery(AttributeReviewRow(aid, reviewId)),
//          insertHostelAttributeQuery(HostelAttributeRow(hostelId, aid, metrics.freq, metrics.cfreq, metrics.sentiment))
//        ))
      }
    }

  private def insertLocationQuery(city: String, country: String) =
    Location.map(l => (l.city, l.country)) returning Location.map(_.id) += (city, country)

  private def insertHostelInfoQuery(name: String, noReviews: Int, locationId: Int) =
    Hostel.map(h => (h.name, h.noReviews, h.locationId)) returning Hostel.map(_.id) += (name, noReviews, locationId)

  private def insertReviewQuery(hostelId: Int, text: String) =
    Review.map(r => (r.hostelId, r.text)) returning Review.map(_.id) += (hostelId, text)

  private def getLocationIdQuery(city: String, country: String) =
    Location.filter(l => l.city === city && l.country === country).map(_.id)

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