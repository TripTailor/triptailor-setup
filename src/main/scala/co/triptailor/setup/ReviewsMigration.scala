package co.triptailor.setup

import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig

object ReviewsMigration {
  import db.drivers.ExtendedPostgresDriver.api._
  import db.Tables._

  private val triptailorDB =
    DatabaseConfig.forConfig[slick.driver.PostgresDriver]("triptailor", ConfigFactory.load("db")).db

  def main(args: Array[String]): Unit = {

  }

  private def reviews = Review.result

}
