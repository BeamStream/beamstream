import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "beamstream"

organization := "BeamStream"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers += ScalaToolsSnapshots

resolvers += "Liftmodules repo" at "https://repository-liftmodules.forge.cloudbees.com/release"

{
  val liftVersion = "2.4-SNAPSHOT"
  libraryDependencies ++= Seq(
    "net.liftweb" %% "lift-mongodb-record" % liftVersion,
    "net.liftmodules" %% "mongoauth" % (liftVersion+"-0.1"),
    "ch.qos.logback" % "logback-classic" % "0.9.26",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "net.databinder" %% "dispatch-core" % "0.8.5",
    "net.databinder" %% "dispatch-http" % "0.8.5",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
  )
}

scalacOptions += "-deprecation"

seq(lessSettings:_*)

(LessKeys.filter in (Compile, LessKeys.less)) := "*styles.less"

(sourceDirectory in (Compile, LessKeys.less)) <<= (sourceDirectory in Compile)(_ / "less")

seq(closureSettings:_*)

(sourceDirectory in (Compile, ClosureKeys.closure)) <<= (sourceDirectory in Compile)(_ / "javascript")

seq(webSettings :_*)

// add managed resources, where less and closure publish to, to the webapp
(webappResources in Compile) <+= (resourceManaged in Compile)

// make compile depend on less and closure
(compile in Compile) <<= compile in Compile dependsOn (ClosureKeys.closure in Compile, LessKeys.less in Compile)

//seq(bees.RunCloudPlugin.deploymentSettings :_*)