package de.tobiasroeser.mill.spring.boot.worker

import mill.api.Ctx
import os.Path

trait SpringBootWorker {
  def repackageJar(
      dest: os.Path,
      base: os.Path,
      mainClass: String,
      libs: Seq[os.Path],
      assemblyScript: String
  )(implicit ctx: Ctx): Unit

  def findMainClass(classesPath: Path): String
}
