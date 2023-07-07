package de.tobiasroeser.mill.spring.boot.worker

import mill.api.Ctx

trait SpringBootWorker {
//  def createJar(
//      dest: os.Path,
//      base: Option[os.Path] = None,
//      libs: Seq[os.Path] = Seq()
//  )(ctx: Ctx): Unit

  def repackageJar(
      dest: os.Path,
      base: os.Path,
      mainClass: String,
      libs: Seq[os.Path],
      assemblyScript: String
  )(implicit ctx: Ctx): Unit
}
