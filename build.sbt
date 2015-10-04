name := """triptailor-setup"""

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion        = "2.3.12"
val akkaStreamsVersion = "1.0"

val utilityDependencies = Seq(
  "org.joda" % "joda-convert" % "1.7"
)

val dbDependencies = Seq(
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick" % "3.0.3",
  "com.typesafe.slick" %% "slick-codegen" % "3.0.3",
  "com.zaxxer" % "HikariCP" % "2.3.5" % Compile
)

val nlpAnalysisDependencies = Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models"
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % akkaStreamsVersion,
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaStreamsVersion
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

val ammoniteRepl = Seq(
  "com.lihaoyi" % "ammonite-repl" % "0.4.5" cross CrossVersion.full
)

libraryDependencies ++= utilityDependencies ++ dbDependencies ++ akkaDependencies ++
  nlpAnalysisDependencies ++ testDependencies ++ ammoniteRepl

initialCommands in console := """ammonite.repl.Repl.main(Array())"""

enablePlugins(JavaAppPackaging)
