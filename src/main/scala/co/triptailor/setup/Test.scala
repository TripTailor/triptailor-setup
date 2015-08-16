package co.triptailor.setup

import java.util.Properties
import java.util.stream.Collectors

import com.typesafe.config.{Config, ConfigFactory}
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.NamedEntityAnnotation
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations

import scala.collection.JavaConverters._

object Test extends AnnotatorService {
  def main(args: Array[String]): Unit = {
    val props      = new Properties
    val annotators = config.getStringList("nlp.annotators").stream().collect(Collectors.joining(","))
    props.setProperty("annotators", annotators)

    val pipeline = new StanfordCoreNLP(props)
    val text =
      """The rooms are clean, same for the bathrooms. There are activities each night.
        |The rooftop is very amazing for partying and having drinks and the staff is so friendly and nice""".stripMargin

    val document = new Annotation(text)

    pipeline.annotate(document)

    val sentences = document.get(classOf[SentencesAnnotation]).asScala

    for (sentence <- sentences) {
      val tree      = sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
      val sentiment = RNNCoreAnnotations.getPredictedClass(tree)
      println(sentence)

      for (token <- sentence.get(classOf[TokensAnnotation]).asScala) {
        val word = token.get(classOf[TextAnnotation])
        val lemma = token.get(classOf[LemmaAnnotation])
        val pos  = token.get(classOf[PartOfSpeechAnnotation])
        val ne   = token.get(classOf[NamedEntityAnnotation])

        val start = token.beginPosition()
        val end   = token.endPosition()
        println("**********")
        println(s"Position($start, $end)")
        println(word)
        println(lemma)
        println(pos)
        println(ne)
        println("**********")
      }

      println(s"Sentiment: $sentiment")
    }
  }

  def config: Config = ConfigFactory.load("nlp")
}