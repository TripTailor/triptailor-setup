package co.triptailor.setup.domain

import java.io.File

object FileParser {
  lazy val data = new File("data")

  def documentEntries =
    for {
      country  ← data.listFiles
      city     ← country.listFiles
      if(city.getName.equals("San_Miguel_de_Allende"))
      txtFiles = city.listFiles.filter(_.getName contains "txt")
      (generalFiles, reviewFiles) = txtFiles.partition(f => f.getName contains "_general.txt")
      (generalFile, reviewFile) ← generalFiles zip reviewFiles
    } yield DocumentEntry(city.getName, country.getName, generalFile, reviewFile)

}
