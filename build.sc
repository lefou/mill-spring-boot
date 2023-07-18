// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:`

// imports
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion

import mill._
import mill.api.Loose
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Command, Module, TaskModule, Target, Task}
import mill.scalalib._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.publish._

trait Deps {
  def springBootToolsVersion = "2.7.3"
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String = "2.13.11"
  def testWithMill: Seq[String]

  def millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  def millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  def millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  def osLib = ivy"com.lihaoyi::os-lib:0.6.3"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.13"
  val scoverageVersion = "2.0.10"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
  val utilsFunctional = ivy"de.tototec:de.tototec.utils.functional:2.0.1"
  val springBootLoaderTools = ivy"org.springframework.boot:spring-boot-loader-tools:2.7.13"
}
object Deps_0_11 extends Deps {
  override def millVersion = "0.11.0"
  override def millPlatform = "0.11"
  override def testWithMill = Seq("0.11.1", millVersion)
  override def osLib = ivy"com.lihaoyi::os-lib:0.9.1"
}
object Deps_0_10 extends Deps {
  override def millVersion = "0.10.0"
  override def millPlatform = "0.10"
  override def testWithMill = Seq("0.10.12", "0.10.3", millVersion)
  override def osLib = ivy"com.lihaoyi::os-lib:0.8.0"
}

val millApiVersions = Seq(Deps_0_11, Deps_0_10).map(x => x.millPlatform -> x)

val millItestVersions = millApiVersions.flatMap { case (_, d) => d.testWithMill.map(_ -> d) }

val projectName = "mill-spring-boot"

trait PluginModule extends ScalaModule with PublishModule with ScoverageModule {
  def millPlatform: String
  def deps: Deps = millApiVersions.toMap.apply(millPlatform)
  override def scalaVersion: T[String] = deps.scalaVersion
  override def publishVersion: T[String] = VcsVersion.vcsState().format()
  override def platformSuffix = s"_mill${millPlatform}"

  override def sources = T.sources {
    super.sources() ++
      ZincWorkerUtil.matchingVersions(millPlatform).map(s => PathRef(millSourcePath / s"src-$s")) ++
      ZincWorkerUtil.versionRanges(millPlatform, millApiVersions.map(_._1)).map(s =>
        PathRef(millSourcePath / s"src-$s")
      )
  }

  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-deprecation")
  override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-deprecation")
  override def scoverageVersion = deps.scoverageVersion

  def pomSettings = T {
    PomSettings(
      description = "Spring Boot packaging support for mill",
      organization = "de.tototec",
      url = s"https://github.com/lefou/${projectName}",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("lefou", projectName),
      developers = Seq(Developer("lefou", "Tobias Roeser", "https.//github.com/lefou"))
    )
  }

  override def skipIdea: Boolean = millApiVersions.head._2.scalaVersion != deps.scalaVersion

  trait Tests extends ScalaTests with ScoverageTests
}

object main extends Cross[MainCross](millApiVersions.map(_._1))
trait MainCross extends PluginModule with Cross.Module[String] { main =>
  override def millPlatform = crossValue
  override def artifactName = T { "de.tobiasroeser.mill.spring.boot" }
  override def moduleDeps: Seq[PublishModule] = Seq(worker)
  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
  }
  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

  object test extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(
      deps.scalaTest
    )
  }

  override def generatedSources: Target[Seq[PathRef]] = T {
    super.generatedSources() :+ versionFile()
  }

  def versionFile: Target[PathRef] = T {
    val dest = T.ctx().dest
    val body =
      s"""package de.tobiasroeser.mill.spring.boot
         |
         |/**
         | * Build-time generated versions file.
         | */
         |object Versions {
         |  /** The mill-kotlin version. */
         |  val millSpringBootVersion = "${publishVersion()}"
         |  /** The mill API version used to build mill-kotlin. */
         |  val buildTimeMillVersion = "${deps.millVersion}"
         |  /** The worker impl ivy dep. */
         |  val millSpringBootWorkerImplIvyDep = "${worker.impl.pomSettings().organization}:${worker.impl.artifactId()}:${worker.impl.publishVersion()}"
         |}
         |""".stripMargin

    os.write(dest / "Versions.scala", body)
    PathRef(dest)
  }

  object worker extends PluginModule {
    override def millPlatform = main.millPlatform
    override def artifactName = "de.tobiasroeser.mill.spring.boot.worker"
    override def compileIvyDeps: T[Loose.Agg[Dep]] = Agg(
      deps.millMainApi,
      deps.osLib
    )

    object impl extends PluginModule {
      override def millPlatform = main.millPlatform
      override def artifactName = "de.tobiasroeser.mill.spring.boot.worker.impl"
      override def moduleDeps: Seq[PublishModule] = Seq(worker)
      override def compileIvyDeps: T[Loose.Agg[Dep]] =
        Agg(
          deps.osLib,
          deps.millMainApi,
          deps.springBootLoaderTools
        )
    }
  }
}

/**
 * Explore some Maven deps.
 */
object explore extends ScalaModule {
  val Deps = Deps_0_11
  def scalaVersion = Deps.scalaVersion
  override def ivyDeps = Agg(
    Deps.springBootLoaderTools,
    ivy"org.springframework.boot:spring-boot-maven-plugin:${Deps.springBootLoaderTools.version}"
  )
}

object itest extends Cross[ItestCross](millItestVersions.map(_._1))
trait ItestCross extends MillIntegrationTestModule with Cross.Module[String] {
  def millItestVersion = crossValue
  val millPlatform = millItestVersions.toMap.apply(millItestVersion).millPlatform
  def deps: Deps = millApiVersions.toMap.apply(millPlatform)

  override def millTestVersion = millItestVersion
  override def pluginsUnderTest = Seq(main(millPlatform))
  override def temporaryIvyModules = Seq(main(millPlatform).worker, main(millPlatform).worker.impl)
  override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    testCases().map { tc =>
      tc -> (tc.path.last match {
        case "spring-boot-simple" => Seq(
            TestInvocation.Targets(Seq("app.jar")),
            TestInvocation.Targets(Seq("validateJar")),
            TestInvocation.Targets(Seq("app.run")),
            TestInvocation.Targets(Seq("app.springBootAssembly")),
            TestInvocation.Targets(Seq("validateAssembly"))
          )
        case "large-assembly" => Seq(
            TestInvocation.Targets(Seq("app.jar")),
            TestInvocation.Targets(Seq("validateJar")),
            TestInvocation.Targets(Seq("app.run")),
            TestInvocation.Targets(Seq("app.springBootAssembly")),
            TestInvocation.Targets(Seq("validateAssembly"))
          )
        case _ => Seq(
            TestInvocation.Targets(Seq("-d", "verify"))
          )
      })
    }

  override def temporaryIvyModulesDetails: Task[Seq[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))]] =
    Target.traverse(temporaryIvyModules) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }
  override def pluginUnderTestDetails: Task[Seq[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))]] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

//  override def perTestResources = T.sources(millSourcePath / "resources")

  override def perTestResources = T.sources {
    Seq(
      PathRef(millSourcePath / "resources"),
      generatedSharedSrc()
    )
  }

  def generatedSharedSrc = T {
    os.write(
      T.dest / "shared.sc",
      s"""import $$ivy.`org.scoverage::scalac-scoverage-runtime:${deps.scoverageVersion}`
         |import $$ivy.`org.scalatest::scalatest:${deps.scalaTest.dep.version}`
         |import $$file.helper
         |""".stripMargin
    )
    PathRef(T.dest)
  }
}

object P extends Module {

  /**
   * Update the millw script.
   */
  def millw() = T.command {
    val target = mill.util.Util.download("https://raw.githubusercontent.com/lefou/millw/master/millw")
    val millw = T.workspace / "millw"
    os.copy.over(target.path, millw)
    os.perms.set(millw, os.perms(millw) + java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE)
    target
  }

}
