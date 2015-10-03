package co.triptailor.setup.nlp

import java.util.Properties
import java.util.stream.Collectors

import co.triptailor.setup.domain._
import com.typesafe.config.Config
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.NamedEntityAnnotation
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, blocking}

trait NLPConfig {
  def config: Config
  def baseYear: Int
  def annotators: String
  def stopWords: Set[String]
}

trait NLPAnalysisService extends NLPConfig {
  val baseYear   = config.getInt("nlp.baseYear")
  val annotators = config.getStringList("nlp.annotators").stream().collect(Collectors.joining(","))
  val stopWords  = config.getStringList("nlp.stopWords").asScala.toSet

  val props = new Properties
  props.setProperty("annotators", annotators)
  val pipeline = new StanfordCoreNLP(props)

  def rateReview(reviewData: UnratedReview)(implicit ec: ExecutionContext): Future[RatedReview] = {
    val unratedReview = new Annotation(reviewData.text)

    Future {
      blocking {
        pipeline.annotate(unratedReview)

        val unratedSentences = unratedReview.get(classOf[SentencesAnnotation]).asScala
        val ratedSentences   = rateSentences(unratedSentences, reviewData.date.map(_.toString("yyyy").toDouble) getOrElse 2000d)

        val tokens  = mergeAnnotatedPositionedTokens(ratedSentences.flatMap(_.positionedSentence.tokens))
        val metrics = ratedSentences.map(_.metrics).reduceOption(mergeMetrics) getOrElse Map.empty[String, RatingMetrics]
        RatedReview(reviewData.text, tokens, metrics)
      }
    }
  }

  protected def mergeMetrics(leftMetrics: Map[String,RatingMetrics], rightMetrics: Map[String,RatingMetrics]) = {
    val (lMetrics, rMetrics) =
      if (leftMetrics.size > rightMetrics.size)
        (leftMetrics, rightMetrics)
      else
        (rightMetrics, leftMetrics)

    rMetrics.keysIterator.foldLeft(lMetrics) { (metrics, attribute) =>
      val rightMetrics = rMetrics(attribute)
      val leftMetrics  = lMetrics.getOrElse(attribute, RatingMetrics(0, 0, 0))
      val updatedMetrics = leftMetrics.copy(
        sentiment = leftMetrics.sentiment + rightMetrics.sentiment,
        freq      = leftMetrics.freq      + rightMetrics.freq,
        cfreq     = leftMetrics.cfreq     + rightMetrics.cfreq
      )
      metrics.updated(attribute, updatedMetrics)
    }
  }

  private def rateSentences(sentences: Seq[CoreMap], reviewYear: Double): Seq[RatedSentence] =
    for {
      sentence           ← sentences
      tree               = sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
      sentiment          = RNNCoreAnnotations.getPredictedClass(tree)
      tokens             = buildAnnotatedTokens(sentence.get(classOf[TokensAnnotation]).asScala)
      annotatedSentence  = AnnotatedSentence(sentence.toString, createAnnotatedPositionedTokens(tokens), Sentiment(sentiment))
      metrics            = calculateSentenceMetrics(annotatedSentence, reviewYear)
    } yield RatedSentence(annotatedSentence, metrics)

  private def mergeAnnotatedPositionedTokens(tokens: Seq[AnnotatedPositionedToken]) =
    tokens.foldLeft(Map.empty[String, AnnotatedPositionedToken]) {
      (annotatedPositionedTokens, token) =>
        val positionedTokenOpt = annotatedPositionedTokens.get(token.attribute)
        positionedTokenOpt.fold {
          annotatedPositionedTokens.updated(token.attribute, token)
        } { positionedToken =>
          annotatedPositionedTokens.updated(token.attribute, positionedToken.copy(positions = positionedToken.positions ++ token.positions))
        }
    }.values.toSeq

  private def buildAnnotatedTokens(tokens: Seq[CoreLabel]): Seq[AnnotatedToken] =
    for {
      token ← tokens
      lemma = token.get(classOf[LemmaAnnotation]) if !(stopWords contains lemma) && !(lemma matches ".*?[^a-zA-Z-]+.*?")
      pos   = token.get(classOf[PartOfSpeechAnnotation]) if pos.equals("NN") || pos.equals("NNS") || pos.equals("JJ")
      ne    = token.get(classOf[NamedEntityAnnotation])
      start = token.beginPosition()
      end   = token.endPosition()
    } yield AnnotatedToken(lemma, Position(start, end))

  private def createAnnotatedPositionedTokens(tokens: Seq[AnnotatedToken]): Seq[AnnotatedPositionedToken] = {
    def mergeTokens(positionedToken: AnnotatedPositionedToken, token: AnnotatedToken) =
      positionedToken.copy(positions = positionedToken.positions :+ token.position)

    def buildAnnotatedPositionedToken(values: Seq[AnnotatedToken]) =
      values.drop(1).foldLeft(AnnotatedPositionedToken(values.head.attribute, Seq(values.head.position)))(mergeTokens)

    tokens.groupBy(_.attribute).values.map(buildAnnotatedPositionedToken).toSeq
  }

  private def calculateSentenceMetrics(sentence: AnnotatedSentence, reviewYear: Double) =
    sentence.tokens.map { token =>
      val timeModifier = 1 / math.log(baseYear - reviewYear)
      token.attribute -> RatingMetrics(freq = 1, cfreq = timeModifier, sentiment = sentence.sentiment)
    }.toMap

}