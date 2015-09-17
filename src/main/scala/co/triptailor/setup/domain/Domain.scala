package co.triptailor.setup.domain

import org.joda.time.DateTime

object Sentiment {
  val VeryPositive = 5
  val Positive     = 4
  val Neutral      = 3
  val Negative     = 2
  val VeryNegative = 1

  def apply(value: Int) =
    value + 1 match {
      case 1 => VeryNegative
      case 2 => Negative
      case 3 => Neutral
      case 4 => Positive
      case 5 => VeryPositive
    }
}

case class HostelMetaData(name: String, city: String, country: String, hoscars: Int, services: Seq[String])
case class UnratedReview(text: String, date: Option[DateTime])
case class UnratedDocument(reviewData: Seq[UnratedReview], info: HostelMetaData)

case class RatingMetrics(sentiment: Int, freq: Double, cfreq: Double)
case class Position(start: Int, end: Int) {
  override def productPrefix = ""
}

case class AnnotatedToken(attribute: String, position: Position)
case class AnnotatedPositionedToken(attribute: String, positions: Seq[Position])
case class AnnotatedSentence(text: String, tokens: Seq[AnnotatedPositionedToken], sentiment: Int)

case class RatedSentence(positionedSentence: AnnotatedSentence, metrics: Map[String,RatingMetrics])
case class RatedReview(text: String, tokens: Seq[AnnotatedPositionedToken], metrics: Map[String, RatingMetrics])
case class RatedDocument(reviews: Seq[RatedReview], info: HostelMetaData)