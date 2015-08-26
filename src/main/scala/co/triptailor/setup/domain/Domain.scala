package co.triptailor.setup.domain

import org.joda.time.DateTime

object Sentiment {
  val VeryPositive = 4
  val Positive     = 3
  val Neutral      = 2
  val Negative     = 1
  val VeryNegative = 0

  def apply(value: Int) =
    if (value == 0) VeryNegative
    else if (value == 1) Negative
    else if (value == 2) Neutral
    else if (value == 3) Positive
    else VeryPositive
}

case class UnratedHostelMetaData(name: String, hoscars: Int, services: Seq[String])
case class UnratedReview(text: String, date: Option[DateTime])
case class UnratedDocument(reviewData: Seq[UnratedReview], info: UnratedHostelMetaData)

case class RatingMetrics(sentiment: Int, freq: Double, cfreq: Double)
case class Position(start: Int, end: Int)

case class AnnotatedToken(attribute: String, position: Position)
case class AnnotatedPositionedToken(attribute: String, positions: Seq[Position])
case class AnnotatedSentence(text: String, tokens: Seq[AnnotatedPositionedToken], sentiment: Int)

case class RatedSentence(positionedSentence: AnnotatedSentence, metrics: Map[String,RatingMetrics])
case class RatedReview(tokens: Seq[AnnotatedPositionedToken], metrics: Map[String, RatingMetrics])