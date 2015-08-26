package co.triptailor.setup.domain

import java.io.File

object FileParser {
  lazy val data = new File("./data")

  private def groupFiles =
    for {
      country ← data.listFiles
      city    ← country.listFiles
    } yield city.listFiles.groupBy(_.getPath.replaceAll(".jpg", "").replaceAll("_general.txt", "").replaceAll("_reviews.txt", ""))

}
