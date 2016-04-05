name := """triptailor-setup"""

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion        = "2.4.3"
val ammoniteVersion    = "0.5.6"
val slickVersion       = "3.1.1"
val slickPGV           = "0.12.0"

val utilityDependencies = Seq(
  "org.joda" % "joda-convert" % "1.7"
)

val dbDependencies = Seq(
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-codegen" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.zaxxer" % "HikariCP" % "2.3.5" % Compile,
  "com.github.tminglei" %% "slick-pg" % slickPGV,
  "com.github.tminglei" %% "slick-pg_play-json" % slickPGV
)

val nlpAnalysisDependencies = Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models"
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

/*val ammoniteRepl = Seq(*/
  /*"com.lihaoyi" % "ammonite-repl" % ammoniteVersion cross CrossVersion.full*/
/*)*/

libraryDependencies ++= utilityDependencies ++ dbDependencies ++ akkaDependencies ++
  nlpAnalysisDependencies ++ testDependencies //++ ammoniteRepl

/*initialCommands in console := """ammonite.repl.Main.run("")"""*/
initialCommands in console := """
  | import akka.actor.ActorSystem
  | import akka.stream.ActorMaterializer
  | import scala.concurrent.{ Await, Future }
  | import scala.concurrent.duration._
  | import com.typesafe.config.ConfigFactory
  | import slick.backend.DatabaseConfig
  | import co.triptailor.setup.domain._
  | import co.triptailor.setup.nlp._
  | import co.triptailor.setup._
  | implicit val system = ActorSystem("triptailor-setup")
  | implicit val mat = ActorMaterializer()
  """.stripMargin

// Clear console at the start of each run
triggeredMessage in ThisBuild := Watched.clearWhenTriggered

// Ctrl-C quits to console view
cancelable in Global := true

enablePlugins(JavaAppPackaging)
