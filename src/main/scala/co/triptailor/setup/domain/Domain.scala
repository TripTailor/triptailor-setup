package co.triptailor.setup.domain

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

case class ReviewMetaData(text: String, reviewYear: Int, hoscars: Int)
case class UnratedReview(meta: ReviewMetaData, hostelName: String, city: String, country: String)

case class RatingMetrics(sentiment: Int, freq: Double, cfreq: Double)
case class Position(start: Int, end: Int)

case class AnnotatedToken(attribute: String, position: Position)
case class AnnotatedPositionedToken(attribute: String, positions: Seq[Position])
case class AnnotatedSentence(text: String, tokens: Seq[AnnotatedPositionedToken], sentiment: Int)

case class RatedSentence(positionedSentence: AnnotatedSentence, metrics: Map[String,RatingMetrics])
case class RatedReview(tokens: Seq[AnnotatedPositionedToken], metrics: Map[String, RatingMetrics])