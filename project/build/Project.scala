import sbt._
import Process._

import untyped.{ClosureCompilerPlugin, LessCssPlugin}

class LiftProject(info: ProjectInfo) extends DefaultWebProject(info) with ClosureCompilerPlugin with LessCssPlugin {
  lazy val isAutoScan = systemOptional[Boolean]("autoscan", false).value
  val liftVersion = "2.4-M4"
  val specsVersion = buildScalaVersion match {
    case "2.8.0" => "1.6.5"
    case "2.9.1" => "1.6.9"
    case _       => "1.6.8"
  }

  // uncomment the following if you want to use the snapshot repo
  //val scalatoolsSnapshot = ScalaToolsSnapshots

  override def classpathFilter = super.classpathFilter -- "*-sources.jar"
  override def scanDirectories = if (isAutoScan) super.scanDirectories else Nil

  // Lift
  lazy val lift_mongodb = "net.liftweb" %% "lift-mongodb-record" % liftVersion

  // misc
  lazy val logback = "ch.qos.logback" % "logback-classic" % "0.9.26"

  // test-scope
  lazy val specs = "org.scala-tools.testing" %% "specs" % specsVersion % "test->default"
  lazy val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default"

  // google-closure plugin
  override def closureSourcePath: Path = "src" / "main" / "javascript"

  // less.css plugin
  override def lessSourceFilter: NameFilter = filter("bootstrap.less") // only compile the main bootstrap file
  override def lessSourcePath: Path = "src" / "main" / "less"

	// Initialize Boot by default
  override def consoleInit =
    """
      |import bootstrap.liftweb.Boot
      |
      |val b = new Boot
      |b.boot
      |
    """.stripMargin

  lazy val deploy = task {
    "vmc update beamstream --path target/scala_%s/".format(buildScalaVersion) ! log
    None
  } dependsOn(packageAction)
}
