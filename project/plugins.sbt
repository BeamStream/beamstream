resolvers += Classpaths.typesafeResolver

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.3.0")

resolvers += "Untyped Public Repo" at "http://repo.untyped.com"

addSbtPlugin("untyped" % "sbt-less" % "0.2-SNAPSHOT")

addSbtPlugin("untyped" % "sbt-closure" % "0.6-SNAPSHOT")

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.8"))

//resolvers += "sonatype.repo" at "https://oss.sonatype.org/content/groups/public"

//libraryDependencies <+= sbtVersion(v => "eu.getintheloop" %% "sbt-cloudbees-plugin" % ("0.3.1_"+v))