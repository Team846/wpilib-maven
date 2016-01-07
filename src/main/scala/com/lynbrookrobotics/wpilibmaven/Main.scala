package com.lynbrookrobotics.wpilibmaven

import java.io.File
import java.io.PrintWriter
import java.net.URL

import java.util.Date

import scala.io.Source
import scala.sys.process._

object Main extends App {
  implicit class BetterFile(val file: File) extends AnyVal {
    def /(subpath: String) = new File(file, subpath)
  }

  def eclipseURLBase(channel: String) = s"http://first.wpi.edu/FRC/roborio/$channel/eclipse"

  val downloadsDir = new File("wpi-downloads")
  downloadsDir.mkdir()

  val repoDir = new File("repo")

  def getLatestWPIVersion(channel: String): String = {
    (new URL(s"${eclipseURLBase(channel)}/content.jar") #> (downloadsDir / "content.jar")).!
    s"unzip -o ${(downloadsDir / "content.jar").getAbsolutePath} -d ${downloadsDir.getAbsolutePath}/content".!

    val prefix = "<unit id='edu.wpi.first.wpilib.plugins.java' version='"
    Source.fromFile(downloadsDir / "content" / "content.xml").getLines().
      find(_.trim.startsWith(prefix)).get.trim.drop(prefix.length).dropRight(2).trim
  }

  def downloadWPILib(version: String, channel: String) = {
    val pluginJARPath = s"${eclipseURLBase(channel)}/plugins/edu.wpi.first.wpilib.plugins.java_$version.jar"
    (new URL(pluginJARPath) #> (downloadsDir / "plugin.jar")).!

    s"unzip -o ${(downloadsDir / "plugin.jar").getAbsolutePath} -d ${downloadsDir.getAbsolutePath}/plugin".!
    s"unzip -o ${(downloadsDir / "plugin" / "resources" / "java.zip").getAbsolutePath} -d ${downloadsDir.getAbsolutePath}/java".!
  }

  def installWPILib(version: String): Unit = {
    val wpilibMain = downloadsDir / "java" / "lib" / "WPILib.jar"
    val wpilibSources = downloadsDir / "java" / "lib" / "WPILib-sources.jar"

    (s"mvn deploy:deploy-file " +
      s"-Dfile=${wpilibMain.getAbsolutePath} " +
      s"-Durl=file://${repoDir.getAbsolutePath} " +
      s"-DgroupId=edu.wpi.first " +
      s"-DartifactId=wpilib " +
      s"-Dversion=$version " +
      s"-Dsources=${wpilibSources.getAbsolutePath}").!

    val networktablesMain = downloadsDir / "java" / "lib" / "NetworkTables.jar"
    val networktablesSources = downloadsDir / "java" / "lib" / "NetworkTables-sources.jar"

    (s"mvn deploy:deploy-file " +
      s"-Dfile=${networktablesMain.getAbsolutePath} " +
      s"-Durl=file://${repoDir.getAbsolutePath} " +
      s"-DgroupId=edu.wpi.first " +
      s"-DartifactId=networktables " +
      s"-Dversion=$version " +
      s"-Dsources=${networktablesSources.getAbsolutePath}").!
  }

  val latestVersionsHTML = new PrintWriter(new File("repo/index.html"))

  latestVersionsHTML.println("<html>")
  latestVersionsHTML.println("<body>")

  val todayDate = new Date()
  latestVersionsHTML.println(s"<h1>Latest WPILib versions as of $todayDate</h1>")

  List("development", "beta", "stable", "release").foreach { channel =>
    val version = getLatestWPIVersion(channel)
    latestVersionsHTML.println(s"<h2>Latest $channel version: $version</h2>")
    downloadWPILib(version, channel)
    installWPILib(version)
  }

  latestVersionsHTML.println("</body>")
  latestVersionsHTML.println("</html>")
  latestVersionsHTML.close()
}
