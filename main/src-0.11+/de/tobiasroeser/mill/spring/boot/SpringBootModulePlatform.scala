package de.tobiasroeser.mill.spring.boot

import mill.api.PathRef
import mill.scalalib.{Dep, DepSyntax, JavaModule}
import mill.{Agg, T}

trait SpringBootModulePlatform extends JavaModule {

  def springBootToolsIvyDeps: T[Agg[Dep]]

  private def fullWorkerIvyDeps: T[Agg[Dep]] = T {
    springBootToolsIvyDeps() ++
      Agg(ivy"${Versions.millSpringBootWorkerImplIvyDep}")
  }

  def springBootToolsClasspath: T[Agg[PathRef]] = T {
    resolveDeps(T.task { fullWorkerIvyDeps().map(bindDependency()) })()
  }
}
