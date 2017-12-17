import scoverage.ScoverageKeys

name := "gfc-guava"

organization := "com.gilt"

scalaVersion := "2.12.4"

crossScalaVersions := Seq(scalaVersion.value, "2.11.11")

scalacOptions += "-target:jvm-1.7"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

libraryDependencies ++= Seq(
  "com.gilt" %% "gfc-util" % "0.1.7",
  "com.gilt" %% "gfc-concurrent" % "0.3.7",
  "com.google.guava" % "guava" % "23.5-jre",
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)

ScoverageKeys.coverageMinimum := 64.1

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

licenses := Seq("Apache-style" -> url("https://raw.githubusercontent.com/gilt/gfc-guava/master/LICENSE"))

homepage := Some(url("https://github.com/gilt/gfc-guava"))

pomExtra := (
  <scm>
    <url>https://github.com/gilt/gfc-guava.git</url>
    <connection>scm:git:git@github.com:gilt/gfc-guava.git</connection>
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

