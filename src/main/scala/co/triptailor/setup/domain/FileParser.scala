package co.triptailor.setup.domain

import java.io.File

object FileParser {
  lazy val data = new File("./data")

  def documentEntries =
    for {
      country  ← data.listFiles
      city     ← country.listFiles
      hostel   ← city.listFiles
      txtFiles = hostel.listFiles.filter{ hostel => 
        hostel.getName.equals("info.txt") || hostel.getName.equals("reviews.txt")
      }
      (generalFiles, reviewFiles) = txtFiles.partition(f => f.getName equals "info.txt")
      (generalFile, reviewFile) ← generalFiles zip reviewFiles
    } yield DocumentEntry(city.getName, country.getName, generalFile, reviewFile)

}
