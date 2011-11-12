name := "beamstream"

organization := "com.beamstream"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers += "Liftmodules repo" at "https://repository-liftmodules.forge.cloudbees.com/release"

{
  val liftVersion = "2.4-M5"
  libraryDependencies ++= Seq(
    "net.liftweb" %% "lift-mongodb-record" % liftVersion,
    "net.liftweb" %% "lift-textile" % liftVersion,
    "net.liftmodules" %% "mongoauth" % (liftVersion+"-0.1"),
    "net.liftmodules" %% "google-analytics" % (liftVersion+"-0.9"),
    "ch.qos.logback" % "logback-classic" % "1.0.0",
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "net.databinder" %% "dispatch-core" % "0.8.5",
    "net.databinder" %% "dispatch-http" % "0.8.5",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test",
    "org.eclipse.jetty" % "jetty-webapp" % "7.5.4.v20111024" % "container"
  )
}

scalacOptions += "-deprecation"

seq(lessSettings:_*)

(LessKeys.filter in (Compile, LessKeys.less)) := "*styles.less"

seq(jsSettings:_*)

(JsKeys.filter in (Compile, JsKeys.js)) := "*.jsm"

(sourceDirectory in (Compile, JsKeys.js)) <<= (sourceDirectory in Compile)(_ / "javascript")

(resourceManaged in (Compile, JsKeys.js)) <<= (resourceManaged in Compile)(_ / "js")

seq(webSettings :_*)

// add managed resources, where less and closure publish to, to the webapp
(webappResources in Compile) <+= (resourceManaged in Compile)

// make compile depend on less and closure
(Keys.compile in Compile) <<= Keys.compile in Compile dependsOn (JsKeys.js in Compile, LessKeys.less in Compile)

//seq(bees.RunCloudPlugin.deploymentSettings :_*)

// To publish to the Cloudbees repos:

//publishTo := Some("beamstream repository" at "https://repository-beamstream.forge.cloudbees.com/release/")

//credentials += Credentials( file("/private/beamstream/cloudbees.credentials") )
