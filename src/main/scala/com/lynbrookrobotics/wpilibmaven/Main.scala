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

  val repoDir = new File("repo")

  // WPIlib

  def eclipseURLBase(channel: String) = s"http://first.wpi.edu/FRC/roborio/$channel/eclipse"

  val wpiDownloadsDir = new File("wpi-downloads")
  wpiDownloadsDir.mkdir()

  def getLatestWPIVersion(channel: String): String = {
    (new URL(s"${eclipseURLBase(channel)}/content.jar") #> (wpiDownloadsDir / "content.jar")).!
    s"unzip -o ${(wpiDownloadsDir / "content.jar").getAbsolutePath} -d ${wpiDownloadsDir.getAbsolutePath}/content".!

    val prefix = "<unit id='edu.wpi.first.wpilib.plugins.java' version='"
    Source.fromFile(wpiDownloadsDir / "content" / "content.xml").getLines().
      find(_.trim.startsWith(prefix)).get.trim.drop(prefix.length).dropRight(2).trim
  }

  def downloadWPILib(version: String, channel: String) = {
    val pluginJARPath = s"${eclipseURLBase(channel)}/plugins/edu.wpi.first.wpilib.plugins.java_$version.jar"
    (new URL(pluginJARPath) #> (wpiDownloadsDir / "plugin.jar")).!

    s"unzip -o ${(wpiDownloadsDir / "plugin.jar").getAbsolutePath} -d ${wpiDownloadsDir.getAbsolutePath}/plugin".!
    s"unzip -o ${(wpiDownloadsDir / "plugin" / "resources" / "java.zip").getAbsolutePath} -d ${wpiDownloadsDir.getAbsolutePath}/java".!
  }

  def installWPILib(version: String): Unit = {
    val wpilibMain = wpiDownloadsDir / "java" / "lib" / "WPILib.jar"
    val wpilibSources = wpiDownloadsDir / "java" / "lib" / "WPILib-sources.jar"

    (s"mvn deploy:deploy-file " +
      s"-Dfile=${wpilibMain.getAbsolutePath} " +
      s"-Durl=file://${repoDir.getAbsolutePath} " +
      s"-DgroupId=edu.wpi.first " +
      s"-DartifactId=wpilib " +
      s"-Dversion=$version " +
      s"-Dsources=${wpilibSources.getAbsolutePath}").!

    val networktablesMain = wpiDownloadsDir / "java" / "lib" / "NetworkTables.jar"
    val networktablesSources = wpiDownloadsDir / "java" / "lib" / "NetworkTables-sources.jar"

    (s"mvn deploy:deploy-file " +
      s"-Dfile=${networktablesMain.getAbsolutePath} " +
      s"-Durl=file://${repoDir.getAbsolutePath} " +
      s"-DgroupId=edu.wpi.first " +
      s"-DartifactId=networktables " +
      s"-Dversion=$version " +
      s"-Dsources=${networktablesSources.getAbsolutePath}").!
  }

  // CTR

  val ctrDownloadsDir = new File("ctr-downloads")
  ctrDownloadsDir.mkdir()

  val ctrURL = "http://www.ctr-electronics.com/downloads/lib"

  def getLatestCTRVersion = "4.4.1.9" // ¯\_(ツ)_/¯ TODO: write scraper method

  def downloadCTR(version: String) = {
    val ctrZipPath = s"${ctrURL}/CTRE_FRCLibs_NON-WINDOWS_v${version}.zip"
    (new URL(ctrZipPath) #> (ctrDownloadsDir / "ctr.zip")).!

    s"unzip -o ${(ctrDownloadsDir / "ctr.zip").getAbsolutePath} -d ${ctrDownloadsDir.getAbsolutePath}/ctr"
  }

  def installCTR(version: String) = {
    val ctrLib = ctrDownloadsDir / "ctr" / "java" / "lib" / "CTRLib.jar"
    val ctrJavadoc = ctrDownloadsDir / "ctr" / "java" / "docs" / "CTRLib-javadoc.jar"

    (s"mvn deploy:deploy-file " +
      s"-Dfile=${ctrLib.getAbsolutePath} " +
      s"-Durl=file://${repoDir.getAbsolutePath} " +
      s"-DgroupId=com.ctre " +
      s"-DartifactId=ctrlib " +
      s"-Dversion=$version " +
      s"-Djavadoc=${ctrJavadoc.getAbsolutePath}").!
  }

  // General

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

  val ctrVersion = getLatestCTRVersion
  latestVersionsHTML.println(s"<h2>Latest CTRLib version: $ctrVersion</h2>")
  downloadCTR(ctrVersion)
  installCTR(ctrVersion)

  latestVersionsHTML.println("</body>")
  latestVersionsHTML.println("</html>")
  latestVersionsHTML.close()
}
