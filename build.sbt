import scoverage.ScoverageKeys

name := "gfc-guava"

organization := "org.gfccollective"

scalaVersion := "2.13.5"

crossScalaVersions := Seq(scalaVersion.value, "2.12.13")

scalacOptions += "-target:jvm-1.8"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= Seq(
  "org.gfccollective" %% "gfc-util" % "1.0.0",
  "org.gfccollective" %% "gfc-concurrent" % "1.0.0",
  "com.google.guava" % "guava" % "30.1.1-jre",
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "org.scalatest" %% "scalatest" % "3.2.8" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.7.0" % Test,
  "org.mockito" % "mockito-core" % "3.9.0" % Test,
)

ScoverageKeys.coverageMinimum := 65.0

ScoverageKeys.coverageFailOnMinimum := true

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache-style" -> url("https://raw.githubusercontent.com/gfc-collective/gfc-guava/main/LICENSE"))

homepage := Some(url("https://github.com/gfc-collective/gfc-guava"))

pomExtra := (
  <scm>
    <url>https://github.com/gfc-collective/gfc-guava.git</url>
    <connection>scm:git:git@github.com:gfc-collective/gfc-guava.git</connection>
  </scm>
  <developers>
    <developer>
      <id>gheine</id>
      <name>Gregor Heine</name>
      <url>https://github.com/gheine</url>
    </developer>
    <developer>
      <id>ebowman</id>
      <name>Eric Bowman</name>
      <url>https://github.com/ebowman</url>
    </developer>
    <developer>
      <id>andreyk0</id>
      <name>Andrey Kartashov</name>
      <url>https://github.com/andreyk0</url>
    </developer>
    <developer>
      <id>sullis</id>
      <name>Sean Sullivan</name>
      <url>https://github.com/sullis</url>
    </developer>
  </developers>
)
