name := """triptailor-setup"""

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion        = "2.3.12"
val akkaStreamsVersion = "1.0"

val dbDependencies = Seq(
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick" % "3.0.1",
  "com.typesafe.slick" %% "slick-codegen" % "3.0.1",
  "com.zaxxer" % "HikariCP" % "2.3.5" % Compile
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
  "com.lihaoyi" % "ammonite-repl" % "0.4.3" cross CrossVersion.full
)

libraryDependencies ++= dbDependencies ++ akkaDependencies ++ testDependencies ++ ammoniteRepl

initialCommands in console := """ammonite.repl.Repl.main(Array())"""
