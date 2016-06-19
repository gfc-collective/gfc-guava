name := "gfc-guava"

organization := "com.gilt"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7", "2.10.6")

libraryDependencies ++= Seq(
  "com.gilt" %% "gfc-util" % "0.1.5",
  "com.gilt" %% "gfc-concurrent" % "0.3.3",
  "com.google.guava" % "guava" % "19.0",
  "com.google.code.findbugs" % "jsr305" % "3.0.1",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

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
  </developers>
)

