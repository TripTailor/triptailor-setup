package co.triptailor.setup.domain

import java.io.File

object FileParser {
  lazy val data          = new File("./data")
  lazy val relevantFiles = """(info.txt)|(reviews.txt)""".r

  def documentEntries =
    for {
      country                     ← data.listFiles
      city                        ← country.listFiles
      hostel                      ← city.listFiles
      files                       = hostel.listFiles.filter(f => relevantFiles.pattern.matcher(f.getName).matches)
      (generalFiles, reviewFiles) = files.partition(f => f.getName equals "info.txt")
      (generalFile, reviewFile)   ← generalFiles zip reviewFiles
    } yield DocumentEntry(city.getName, country.getName, generalFile, reviewFile)

}
