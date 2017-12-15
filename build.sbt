organization := "phompang"

name := "tmd_weather_bot"

version := "1.0.1"

scalaVersion := "2.11.8"

enablePlugins(DockerPlugin)

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.github.philcali" % "cronish_2.11" % "0.1.3",
  "com.google.cloud" % "google-cloud-storage" % "1.2.3"
)

dockerfile in docker := {
  val jarFile = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName).mkString(":") + ":" + jarTarget
  new Dockerfile {
    // Base image
    from("frolvlad/alpine-oraclejdk8:slim")
    runRaw("apk add --update --no-cache tzdata && ln -sf /usr/share/zoneinfo/Asia/Bangkok /etc/localtime && echo 'Asia/Bangkok' > /etc/timezone && rm -fr /tmp/* /var/cache/apk/*")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

// Set names for the image
imageNames in docker := Seq(
  ImageName("phompang/tmd_weather_bot"),
  ImageName(namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value))
)
