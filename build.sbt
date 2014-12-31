name := "gfc-guava"

organization := "com.gilt"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.11.4", "2.10.4")

libraryDependencies ++= Seq(
  "com.gilt" %% "gfc-util" % "0.0.3",
  "com.google.guava" % "guava" % "18.0",
  "com.google.code.findbugs" % "jsr305" % "2.0.2",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test"
)

instrumentSettings

ScoverageKeys.highlighting := true

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
  </developers>
)

