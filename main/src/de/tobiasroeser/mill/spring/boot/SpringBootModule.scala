package de.tobiasroeser.mill.spring.boot

import de.tobiasroeser.mill.spring.boot.worker.SpringBootWorker
import mill.{Agg, T}
import mill.api.PathRef
import mill.define.{Target, Task, Worker}
import mill.modules.Jvm
import mill.scalalib.{Dep, DepSyntax, JavaModule}

import java.net.{URL, URLClassLoader}

trait SpringBootModule extends JavaModule {

  def springBootToolsVersion: T[String]

  def springBootToolsIvyDeps: T[Agg[Dep]] = T {
    Agg(ivy"org.springframework.boot:spring-boot-loader-tools:${springBootToolsVersion()}")
  }

  private def fullWorkerIvyDeps = T {
    springBootToolsIvyDeps() ++
      Agg(ivy"${Versions.millSpringBootWorkerImplIvyDep}")
  }

  def springBootToolsClasspath: T[Agg[PathRef]] = T {
    resolveDeps(fullWorkerIvyDeps)()
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

//  override def upstreamAssembly: T[PathRef] = T {
//    val libs = runClasspath().map(_.path)
//    val base = jar().path
//    val mainClass = finalMainClass()
//    val dest = T.dest / "out.jar"
//    val worker = springBootToolsWorker()
//
//    worker.repackageJar(
//      dest = dest,
//      base = base,
//      mainClass = mainClass,
//      libs = libs
//    )
//
//    PathRef(dest)
//  }

  override def assembly: T[PathRef] = T {
    //    val base = upstreamAssembly().path
    //    val jar = T.dest / "out.jar"
    //    SpringBootUtil.createJar(
    //      dest = jar,
    //      base = Some(base),
    //    )

    val libs = runClasspath().map(_.path)
    val base = jar().path
    val mainClass = finalMainClass()
    val dest = T.dest / "out.jar"
    val worker = springBootToolsWorker()

    worker.repackageJar(
      dest = dest,
      base = base,
      mainClass = mainClass,
      libs = libs
    )

    PathRef(dest)
  }

  def assemblyManifest: Target[Jvm.JarManifest] = T {
    manifest()
  }

}
