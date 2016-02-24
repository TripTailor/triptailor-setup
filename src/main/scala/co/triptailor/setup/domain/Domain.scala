package co.triptailor.setup.domain

import java.io.File

import org.joda.time.DateTime

object Sentiment {
  val VeryPositive = 3
  val Positive     = 2
  val Neutral      = 1
  val Negative     = -1
  val VeryNegative = -2

  def apply(value: Int) =
    value match {
      case 0 => VeryNegative
      case 1 => Negative
      case 2 => Neutral
      case 3 => Positive
      case 4 => VeryPositive
    }
}

case class DocumentEntry(city: String, country: String, generalFile: File, reviewFile: File)

case class ReviewMetaData(reviewer: Option[String], city: Option[String], gender: Option[String], age: Option[Int])
case class HostelMetaData(name: String, city: String, country: String, hoscars: Int, services: Seq[String])
case class UnratedReview(text: String, date: Option[DateTime], meta: ReviewMetaData)
case class UnratedDocument(reviewData: Seq[UnratedReview], info: HostelMetaData)

case class RatingMetrics(sentiment: Int, freq: Double, cfreq: Double)
case class Position(start: Int, end: Int, sentenceNbr: Int) {
  override def productPrefix = ""
}

case class AnnotatedToken(attribute: String, position: Position)
case class AnnotatedPositionedToken(attribute: String, positions: Seq[Position])
case class AnnotatedSentence(text: String, tokens: Seq[AnnotatedPositionedToken], sentiment: Int)

case class RatedSentence(positionedSentence: AnnotatedSentence, metrics: Map[String,RatingMetrics])
case class RatedReview(text: String, tokens: Seq[AnnotatedPositionedToken], metrics: Map[String, RatingMetrics],
                       sentiments: Seq[Int], date: Option[DateTime], meta: ReviewMetaData)
case class RatedDocument(reviews: Seq[RatedReview], metrics: Map[String, RatingMetrics], info: HostelMetaData)
