name := "Kojo"

version := "2.7"

scalaVersion := "2.12.6"

fork in run := true

scalacOptions := Seq("-feature", "-deprecation")
javaOptions in run ++= Seq("-Xmx1024m", "-Xss1m", "-XX:+UseConcMarkSweepGC", "-XX:+CMSClassUnloadingEnabled")

fork in Test := true

javaOptions in Test ++= Seq("-Xmx1024m", "-Xss1m", "-XX:+UseConcMarkSweepGC", "-XX:+CMSClassUnloadingEnabled")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-s")

// parallelExecution in Test := false

autoScalaLibrary := false

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-library" % "2.12.6",
    "org.scala-lang" % "scala-compiler" % "2.12.6",
    "org.scala-lang" % "scala-reflect" % "2.12.6",
    "com.typesafe.akka" % "akka-actor_2.12" % "2.5.14",
    "org.scala-lang.modules" % "scala-swing_2.12" % "2.0.3",
    "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6",
    "org.scala-lang.modules" % "scala-parser-combinators_2.12" % "1.1.1",
    "com.github.benhutchison" %% "scalaswingcontrib" % "1.7",
    "org.piccolo2d" % "piccolo2d-core" % "1.3.1",
    "org.piccolo2d" % "piccolo2d-extras" % "1.3.1",
    "com.vividsolutions" % "jts" % "1.13" intransitive(),
    "com.h2database" % "h2" % "1.3.168",
    "org.scalatest" % "scalatest_2.12" % "3.0.5" intransitive(),
    "org.scalactic" % "scalactic_2.12" % "3.0.5" intransitive(),
    "junit" % "junit" % "4.10" % "test",
    "org.jmock" % "jmock" % "2.5.1" % "test",
    "org.jmock" % "jmock-legacy" % "2.5.1" % "test",
    ("org.jmock" % "jmock-junit4" % "2.5.1" intransitive()) % "test",
    "cglib" % "cglib-nodep" % "2.1_3" % "test",
    "org.objenesis" % "objenesis" % "1.0" % "test",
    "org.hamcrest" % "hamcrest-core" % "1.1" % "test",
    "org.hamcrest" % "hamcrest-library" % "1.1" % "test",
    ("org.scalacheck"  % "scalacheck_2.12" % "1.14.0" intransitive()) % "test"
)

//Build distribution
val distOutpath             = settingKey[File]("Where to copy all dependencies and kojo")
val buildDist  = taskKey[Unit]("Copy runtime dependencies and built kojo to 'distOutpath'")

lazy val dist = project
  .in(file("."))
  .settings(
    distOutpath              := baseDirectory.value / "dist",
    buildDist   := {
      val allLibs:                List[File]          = dependencyClasspath.in(Runtime).value.map(_.data).filter(_.isFile).toList
      val buildArtifact:          File                = packageBin.in(Runtime).value
      val jars:                   List[File]          = buildArtifact :: allLibs
      val `mappings src->dest`:   List[(File, File)]  = jars.map(f => (f, distOutpath.value / f.getName))
      val log                                         = streams.value.log
      log.info(s"Copying to ${distOutpath.value}:")
      log.info(s"${`mappings src->dest`.map(f => s" * ${f._1}").mkString("\n")}")
      IO.copy(`mappings src->dest`)
    }
  )

//libraryDependencies += "com.novocode" % "junit-interface" % "0.10-M2" % "test"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

//Download source jars for dependencies:
EclipseKeys.withSource := true

packageOptions in (Compile, packageBin) +=
    Package.ManifestAttributes("Permissions" -> "all-permissions", "Application-Name" -> "Kojo")
    
publishMavenStyle in ThisBuild := false    
