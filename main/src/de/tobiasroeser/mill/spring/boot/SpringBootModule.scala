package de.tobiasroeser.mill.spring.boot

import de.tobiasroeser.mill.spring.boot.worker.SpringBootWorker
import mill.{Agg, T}
import mill.api.PathRef
import mill.define.{Target, Task, Worker}
import mill.modules.Jvm
import mill.scalalib.{Dep, DepSyntax, JavaModule}

import java.net.{URL, URLClassLoader}

trait SpringBootModule extends SpringBootModulePlatform {

  def springBootToolsVersion: T[String]

  override def springBootToolsIvyDeps: T[Agg[Dep]] = T {
    Agg(ivy"org.springframework.boot:spring-boot-loader-tools:${springBootToolsVersion()}")
  }

  def springBootToolsWorker: Worker[SpringBootWorker] = T.worker {
    val cl =
      new URLClassLoader(
        springBootToolsClasspath().map(_.path.toIO.toURI().toURL()).toArray[URL],
        getClass().getClassLoader()
      )
    val className =
      classOf[SpringBootWorker].getPackage().getName() + ".impl." + classOf[SpringBootWorker].getSimpleName() + "Impl"
    val impl = cl.loadClass(className)
    val ctr = impl.getConstructor()
    val worker = ctr.newInstance().asInstanceOf[SpringBootWorker]
    if (worker.getClass().getClassLoader() != cl) {
      T.ctx()
        .log
        .error(
          """Worker not loaded from worker classloader.
            |You should not add the mill-spring-boot-worker JAR to the mill build classpath""".stripMargin
        )
    }
    if (worker.getClass().getClassLoader() == classOf[SpringBootWorker].getClassLoader()) {
      T.ctx().log.error("Worker classloader used to load interface and implementation")
    }
    worker
  }

  def springBootPrependScript: T[String] = T {
    mill.modules.Jvm.launcherUniversalScript(
      mainClass = "org.springframework.boot.loader.JarLauncher",
      shellClassPath = Agg("$0"),
      cmdClassPath = Agg("%~dpnx0"),
      jvmArgs = forkArgs()
    )
  }

  def springBootAssembly: T[PathRef] = T {
    val libs = runClasspath().map(_.path)
    val base = jar().path
    val mainClass = finalMainClass()
    val dest = T.dest / "out.jar"
    val worker = springBootToolsWorker()
    val script = springBootPrependScript()

    worker.repackageJar(
      dest = dest,
      base = base,
      mainClass = mainClass,
      libs = libs,
      assemblyScript = script
    )

    PathRef(dest)
  }

}
