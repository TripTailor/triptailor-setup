name := """triptailor-setup"""

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion        = "2.4.2"
val ammoniteVersion    = "0.5.6"
val slickVersion       = "3.1.1"

val utilityDependencies = Seq(
  "org.joda" % "joda-convert" % "1.7"
)

val dbDependencies = Seq(
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-codegen" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.zaxxer" % "HikariCP" % "2.3.5" % Compile
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

// Clear console at the start of each run
triggeredMessage in ThisBuild := Watched.clearWhenTriggered

// Ctrl-C quits to console view
cancelable in Global := true

enablePlugins(JavaAppPackaging)
