package co.triptailor.setup

import com.typesafe.config.{Config, ConfigFactory}

// TODO: Need to retry reviews that get `java.lang.OutOfMemoryError` errors
object Test extends AnnotatorService {
  def main(args: Array[String]): Unit = {
    val text =
      """The rooms are clean, same for the bathrooms. There are activities each night.
        |The rooftop is very amazing for partying and having drinks and the staff is so friendly and nice""".stripMargin

    val ratedReview = rateReview(text, reviewYear = 2014)
    ratedReview.tokens foreach println
  }

  def config: Config = ConfigFactory.load("nlp")

}