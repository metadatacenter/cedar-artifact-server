import sbt._
import sbt.Keys._

object MyBuild extends Build {

  val projectArtifactId = Pom.projectArtifactId(file("."))
  val projectScalaVersion = Pom.scalaVersion(file("."))

  val commonProjectName = "cedar-template-server-common"
  val coreProjectName = "cedar-template-server-core"

  val playProjectName = "cedar-template-server-play"
  val playRepoProjectName = "cedar-repo-server-play"

  val commonProject = Project(commonProjectName, file(commonProjectName))
    .settings(
      version := Pom.projectVersion(baseDirectory.value),
      scalaVersion := projectScalaVersion,
      libraryDependencies ++= Pom.dependencies(baseDirectory.value))

  val coreProject = Project(coreProjectName, file(coreProjectName))
    .dependsOn(commonProject)
    .settings(
      version := Pom.projectVersion(baseDirectory.value),
      scalaVersion := projectScalaVersion,
      libraryDependencies ++= Pom.dependencies(baseDirectory.value).filterNot(d => d.name == commonProject.id))

  val playProject = Project(playProjectName, file(playProjectName))
    .enablePlugins(play.PlayScala)
    .dependsOn(commonProject, coreProject)
    .settings(
      version := Pom.projectVersion(baseDirectory.value),
      scalaVersion := projectScalaVersion,
      libraryDependencies ++= Pom.dependencies(baseDirectory.value).filterNot(d => d.name == coreProject.id || d.name == commonProject.id))

  val playRepoProject = Project(playRepoProjectName, file(playRepoProjectName))
    .enablePlugins(play.PlayScala)
    .dependsOn(commonProject, coreProject)
    .settings(
      version := Pom.projectVersion(baseDirectory.value),
      scalaVersion := projectScalaVersion,
      libraryDependencies ++= Pom.dependencies(baseDirectory.value).filterNot(d => d.name == coreProject.id || d.name == commonProject.id))

  override def rootProject = Some(playProject)
}
