package de.tobiasroeser.mill.spring.boot

import de.tobiasroeser.mill.spring.boot.worker.SpringBootWorker
import mill.{Agg, T}
import mill.api.PathRef
import mill.define.{Target, Task, Worker}
import mill.modules.Jvm
import mill.scalalib.{Dep, DepSyntax, JavaModule}

import java.net.{URL, URLClassLoader}

trait SpringBootModule extends SpringBootModulePlatform {

  /** Specifies the version of the Spring Boot tools. */
  def springBootToolsVersion: T[String]

  override def springBootToolsIvyDeps: T[Agg[Dep]] = T {
    Agg(
      ivy"org.springframework.boot:spring-boot-loader-tools:${springBootToolsVersion()}"
    )
  }

  def springBootToolsWorker: Worker[SpringBootWorker] = T.worker {
    val cl =
      new URLClassLoader(
        springBootToolsClasspath().map(_.path.toIO.toURI().toURL()).iterator.toArray[URL],
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

  /**
   * A script prepended to the resulting `springBootAssembly` to make it executable.
   * This uses the same prepend script as Mill `JavaModule` does,
   * so it supports most Linux/Unix shells (probably not `fish`)
   * as well as Windows cmd shell (the file needs a `.bat` or `.cmd` extension).
   * Set it to `""` if you don't want an executable JAR.
   */
  def springBootPrependScript: T[String] = T {
    // we use the deprecated class, to keep compat with Mill 0.10
    mill.modules.Jvm.launcherUniversalScript(
      mainClass = "org.springframework.boot.loader.JarLauncher",
      shellClassPath = Agg("$0"),
      cmdClassPath = Agg("%~dpnx0"),
      jvmArgs = forkArgs()
    )
  }

  /** The Class holding the Spring Boot Application entrypoint. By default, Spring Boot will try to auto-detect it. */
  def springBootMainClass: T[String] = T {
    mainClass().getOrElse {
      springBootToolsWorker().findMainClass(compile().classes.path)
    }
  }

  def springBootAssembly: T[PathRef] = T {
    val libs = runClasspath().map(_.path)
    val base = jar().path
    val mainClass = springBootMainClass()
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
