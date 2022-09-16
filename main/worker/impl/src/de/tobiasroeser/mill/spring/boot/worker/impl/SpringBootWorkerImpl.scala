package de.tobiasroeser.mill.spring.boot.worker.impl

import de.tobiasroeser.mill.spring.boot.worker.SpringBootWorker
import mill.api.Ctx
import org.springframework.boot.loader.tools.{Libraries, Library, LibraryCallback, LibraryScope, Repackager}
import os.Path

class SpringBootWorkerImpl() extends SpringBootWorker {

//  override def createJar(
//      dest: Path,
//      base: Option[Path],
//      libs: Seq[Path]
//  )(ctx: Ctx): Unit = {
//
//
//
//  }

  override def repackageJar(
      dest: Path,
      base: Path,
      mainClass: String,
      libs: Seq[Path]
  )(implicit ctx: Ctx): Unit = {
    val repack = new Repackager(base.toIO)
    repack.setMainClass(mainClass)

    val libraries: Libraries = { libCallback =>
        libs.foreach { lib =>
          libCallback.library(new Library(lib.toIO, LibraryScope.RUNTIME))
        }
    }
    repack.repackage(dest.toIO, libraries)
    ()
  }
}
