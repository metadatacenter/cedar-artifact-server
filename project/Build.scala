import sbt._
import sbt.Keys._

object MyBuild extends Build {

  val projectArtifactId = Pom.projectArtifactId(file("."))
  val projectScalaVersion = Pom.scalaVersion(file("."))
  val coreProjectName = projectArtifactId + "-core"
  val playProjectName = projectArtifactId + "-play"

  val buildResolvers = resolvers ++= Seq(
    "Local Maven Repository"    at "file://" + Path.userHome.absolutePath + "/.m2/repository"
  )

  val coreProject = Project(coreProjectName, file(coreProjectName))
      .settings(
        version := Pom.projectVersion(baseDirectory.value),
        scalaVersion := projectScalaVersion,
        libraryDependencies ++= Pom.dependencies(baseDirectory.value))
      .settings(buildResolvers:_*)

  val playProject = Project(playProjectName, file(playProjectName))
    .enablePlugins(play.PlayScala)
    .dependsOn(coreProject)
    .settings(
      version := Pom.projectVersion(baseDirectory.value),
      scalaVersion := projectScalaVersion,
      libraryDependencies ++= Pom.dependencies(baseDirectory.value).filterNot(d => d.name == coreProject.id))
    .settings(buildResolvers:_*)

  override def rootProject = Some(playProject)

  //javaOptions in Test += "-Dconfig.file=" + Option(System.getProperty("conf/application.test.conf")).getOrElse("conf/application.conf")
  javaOptions in Test += "-Dconfig.file=" + Option(System.getProperty("conf/application.test.conf"))
}