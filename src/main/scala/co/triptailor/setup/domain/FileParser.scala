package co.triptailor.setup.domain

import java.io.File

object FileParser {
  private lazy val data   = new File("./data")
  private val GeneralFile = "info.txt".r
  private val ReviewsFile = "reviews.txt".r
  private val ImagesFile  = "images.txt".r

  def documentEntries =
    for {
      country     ← data.listFiles.toSeq
      city        ← country.listFiles.toSeq
      hostel      ← city.listFiles.toSeq
      files       = hostel.listFiles.toSeq
      generalFile ← files.find(f => GeneralFile.pattern.matcher(f.getName).matches)
      reviewsFile ← files.find(f => ReviewsFile.pattern.matcher(f.getName).matches)
      imagesFile  ← files.find(f => ImagesFile.pattern.matcher(f.getName).matches)
    } yield DocumentEntry(city.getName, country.getName, generalFile, reviewsFile, imagesFile)
}
