name := """trader"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
  "com.github.etaty" % "rediscala_2.11" % "1.6.0",
  "org.msgpack" %% "msgpack-scala" % "0.6.11"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"