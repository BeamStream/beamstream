import sbt._
import Process._

import bees.RunCloudPlugin
import untyped.{ClosureCompilerPlugin, LessCssPlugin}

class LiftProject(info: ProjectInfo) extends DefaultWebProject(info)
  with ClosureCompilerPlugin
  with LessCssPlugin
  with RunCloudPlugin
{
  lazy val isAutoScan = systemOptional[Boolean]("autoscan", false).value
  val liftVersion = "2.4-SNAPSHOT"
  val specsVersion = buildScalaVersion match {
    case "2.8.0" => "1.6.5"
    case "2.9.1" => "1.6.9"
    case _       => "1.6.8"
  }

  // Repos
  //val scalatoolsSnapshot = ScalaToolsSnapshots
  //val liftmodulesReleases = "Liftmodules repo" at "https://repository-liftmodules.forge.cloudbees.com/release"

  override def classpathFilter = super.classpathFilter -- "*-sources.jar"
  override def scanDirectories = if (isAutoScan) super.scanDirectories else Nil

  // Lift
  lazy val lift_mongodb = "net.liftweb" %% "lift-mongodb-record" % liftVersion

  // Liftmodules

  // misc
  lazy val lift_auth_mongo = "com.eltimn" %% "lift-auth-mongo" % "0.1-SNAPSHOT"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "0.9.26"
  //lazy val commons_collections = "commons-collections" % "commons-collections" % "3.2.1"
  //lazy val commons_logging = "commons-logging" % "commons-logging" % "1.1.1"
  lazy val servlet = "javax.servlet" % "servlet-api" % "2.5" % "provided"

  lazy val dispatch = "net.databinder" %% "dispatch-core" % "0.8.5"
  lazy val dispatch_http = "net.databinder" %% "dispatch-http" % "0.8.5"
  //lazy val dispatch_json = "net.databinder" %% "dispatch-lift-json" % "0.8.5" intransitive()

  // test-scope
  lazy val specs = "org.scala-tools.testing" %% "specs" % specsVersion % "test"
  lazy val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test"

  // google-closure plugin
  override def closureSourcePath: Path = "src" / "main" / "javascript"

  // less.css plugin
  override def lessSourceFilter: NameFilter = filter("*styles.less") // only compile the main file
  override def lessSourcePath: Path = "src" / "main" / "less"

  // CloudBees
  override def beesApplicationId = Some("beamstream")
  override def beesUsername = Some("beamstream")

  // Initialize Boot by default
  /*
  override def consoleInit =
    """
      |import bootstrap.liftweb.Boot
      |
      |val b = new Boot
      |b.boot
      |
    """.stripMargin
  */

  lazy val cfdeploy = task {
    "vmc update beamstream --path target/scala_%s/".format(buildScalaVersion) ! log
    None
  } dependsOn(packageAction)
}
