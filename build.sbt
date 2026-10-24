scalaVersion := "3.10.0"
organization := "example"
version := "0.1.0-SNAPSHOT"

lazy val root = project.in(file(".")).settings(
  name := "conway",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.13.0",
    "org.scalameta" %% "munit" % "1.3.6" % Test
  ),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
  Compile / mainClass := Some("conway.Main")
)
