package co.triptailor.setup

import java.util.Properties
import java.util.stream.Collectors

import com.typesafe.config.Config
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.NamedEntityAnnotation
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._

trait NLPConfig {
  def config: Config
  def annotators: String
}

trait AnnotatorService extends NLPConfig {
  val annotators = config.getStringList("nlp.annotators").stream().collect(Collectors.joining(","))

  def rateReview(text: String, timeModifier: Double): RatedReview = {
    val props = new Properties
    props.setProperty("annotators", annotators)

    val pipeline      = new StanfordCoreNLP(props)
    val unratedReview = new Annotation(text)

    pipeline.annotate(unratedReview)

    val unratedSentences = unratedReview.get(classOf[SentencesAnnotation]).asScala
    val ratedSentences   = rateSentences(unratedSentences, timeModifier)

    RatedReview(ratedSentences, ratedSentences.map(_.metrics).reduce(mergeMetrics))
  }

  private def rateSentences(sentences: Seq[CoreMap], timeModifier: Double): Seq[RatedSentence] =
    for {
      sentence          ← sentences
      tree              = sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
      sentiment         = RNNCoreAnnotations.getPredictedClass(tree)
      tokens            = buildAnnotatedTokens(sentence.get(classOf[TokensAnnotation]).asScala)
      annotatedSentence = AnnotatedSentence(sentence.toString, tokens, Sentiment(sentiment))
      metrics           = calculateSentenceMetrics(annotatedSentence, timeModifier)
    } yield RatedSentence(annotatedSentence, metrics)

  private def buildAnnotatedTokens(tokens: Seq[CoreLabel]): Seq[AnnotatedToken] =
    for {
      token ← tokens
      lemma = token.get(classOf[LemmaAnnotation])
      pos   = token.get(classOf[PartOfSpeechAnnotation])
      ne    = token.get(classOf[NamedEntityAnnotation])
      start = token.beginPosition()
      end   = token.endPosition()
    } yield AnnotatedToken(lemma, start, end)

  private def mergeMetrics(leftMetrics: Map[AnnotatedToken,RatingMetrics], rightMetrics: Map[AnnotatedToken,RatingMetrics]) = {
    val (lMetrics, rMetrics) =
      if (leftMetrics.size > rightMetrics.size)
        (leftMetrics, rightMetrics)
      else
        (rightMetrics, leftMetrics)

    rMetrics.keysIterator.foldLeft(lMetrics) { (metrics, token) =>
      val rightMetrics = rMetrics(token)
      val leftMetrics  = lMetrics.getOrElse(token, RatingMetrics(0, 0, 0))
      val updatedMetrics = leftMetrics.copy(
        rating = leftMetrics.rating + rightMetrics.rating,
        freq   = leftMetrics.freq   + rightMetrics.freq,
        cfreq  = leftMetrics.cfreq  + rightMetrics.cfreq
      )
      metrics + (token -> updatedMetrics)
    }
  }

  private def calculateSentenceMetrics(sentence: AnnotatedSentence, timeModifier: Double): Map[AnnotatedToken,RatingMetrics] = {
    sentence.tokens.foldLeft(Map.empty[AnnotatedToken,RatingMetrics]) { (metrics, token) =>
      val tokenMetrics = metrics.getOrElse(token, RatingMetrics(0, 0, 0))
      val updatedMetrics = tokenMetrics.copy(
        rating = tokenMetrics.rating + sentence.sentiment,
        freq   = tokenMetrics.freq,
        cfreq  = tokenMetrics.cfreq + timeModifier
      )
      metrics + (token -> updatedMetrics)
    }
  }

}