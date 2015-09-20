package co.triptailor.setup.domain

import java.io.File

case class DocumentEntry(city: String, country: String, generalFile: File, reviewFile: File)

object FileParser {
  lazy val data = new File("./data")

  def documentEntries =
    for {
      country  ← data.listFiles
      city     ← country.listFiles
      txtFiles = city.listFiles.filter(_.getName contains "txt")
      (generalFiles, reviewFiles) = txtFiles.partition(f => f.getName contains "_general.txt")
      (generalFile, reviewFile) ← generalFiles zip reviewFiles
    } yield DocumentEntry(city.getName, country.getName, generalFile, reviewFile)

}
