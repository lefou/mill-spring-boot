package de.tobiasroeser.mill.spring.boot.worker.impl

import de.tobiasroeser.mill.spring.boot.worker.SpringBootWorker
import mill.api.Ctx
import org.springframework.boot.loader.tools.{
  LaunchScript,
  Libraries,
  Library,
  LibraryScope,
  MainClassFinder,
  Repackager
}
import os.Path

class SpringBootWorkerImpl() extends SpringBootWorker {

  override def findMainClass(classesPath: os.Path): String = {
    MainClassFinder.findSingleMainClass(
      classesPath.toIO,
      "org.springframework.boot.autoconfigure.SpringBootApplication"
    )
  }

  override def repackageJar(
                             dest: Path,
                             base: Path,
                             mainClass: String,
                             libs: Seq[Path],
                             assemblyScript: String
                           )(implicit ctx: Ctx): Unit = {
    val repack = new Repackager(base.toIO)
    repack.setMainClass(mainClass)

    val libraries: Libraries = { libCallback =>
      libs.foreach { lib =>
        libCallback.library(new Library(lib.toIO, LibraryScope.RUNTIME))
      }
    }
    if (assemblyScript == null || assemblyScript.isEmpty) {
      repack.repackage(dest.toIO, libraries)
    } else {
      repack.repackage(dest.toIO, libraries, (() => assemblyScript.getBytes): LaunchScript)
    }
    ()
  }
}
