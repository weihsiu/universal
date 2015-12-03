name := "universal"

lazy val root = project.in(file("."))
  .aggregate(universalJS, universalJVM)

lazy val universal = crossProject.in(file("."))
  .settings(
    name := "universal",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.5.3",
      "com.lihaoyi" %%% "autowire" % "0.2.5",
      "com.lihaoyi" %%% "upickle" % "0.3.6",
      "org.spire-math" %%% "cats" % "0.3.0",
      "org.monifu" %%% "monifu" % "1.0-RC3",
      "org.scala-lang.modules" %% "scala-async" % "0.9.6-RC2",
      "com.lihaoyi" %%% "scalarx" % "0.2.8",
      "com.github.japgolly.scalacss" %%% "core" % "0.3.1",
      "com.lihaoyi" %%% "utest" % "0.3.1"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .jvmSettings(
    Revolver.settings ++ (
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-dsl" % "0.11.0",
        "org.http4s" %% "http4s-blaze-server" % "0.11.0",
        "org.slf4j" % "slf4j-api" % "1.7.13",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "ch.qos.logback" % "logback-core" % "1.1.3",
        "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.8.2",
        "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1"
      )
    ): _*
  )
  .jsSettings(
    skip in packageJSDependencies := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "com.github.japgolly.scalajs-react" %%% "core" % "0.10.1",
      "com.github.japgolly.scalacss" %%% "ext-react" % "0.3.1"
    ),
    jsDependencies ++= Seq(
      "org.webjars.npm" % "react" % "0.14.2" / "react-with-addons.js" commonJSName "React" minified "react-with-addons.min.js",
      "org.webjars.npm" % "react-dom" % "0.14.2" / "react-dom.js" commonJSName "ReactDOM" minified "react-dom.min.js" dependsOn "react-with-addons.js"
    )
  )

lazy val universalJVM = universal.jvm
lazy val universalJS = universal.js

lazy val copyJs = taskKey[Unit]("copy javascript files to web directory")
copyJs := {
  val srcDir = (fastOptJS in Compile in universalJS).value.data.getParentFile
  val destDir = baseDirectory.value / "web"
  val srcFiles = (srcDir * "*.js" +++ srcDir * "*.map").get
  srcFiles.foreach(f => IO.copyFile(f, destDir / f.name))
}

addCommandAlias("build", ";copyJs")
