import $file.plugins
import $file.shared

import helper.Deps
import de.tobiasroeser.lambdatest

import mill._, mill.scalalib._
import de.tobiasroeser.mill.spring.boot.SpringBootModule

object app extends ScalaModule with SpringBootModule {
  override def millSourcePath = super.millSourcePath / os.up
  def scalaVersion = "2.13.16"
  override def springBootToolsVersion = "2.7.3"
  // Add many dependencies to ensure we do not fail the executable assembly
  // See these Mill issues
  // - https://github.com/com-lihaoyi/mill/issues/528
  // - https://github.com/com-lihaoyi/mill/issues/2650
  override def ivyDeps = Agg(
    Deps.slf4j,
    ivy"com.lihaoyi::scalatags:0.8.2",
    ivy"com.lihaoyi::mainargs:0.4.0",
    ivy"org.apache.avro:avro:1.11.1",
    ivy"dev.zio::zio:2.0.15",
    ivy"org.typelevel::cats-core:2.9.0",
    ivy"org.apache.spark::spark-core:3.4.0",
    ivy"dev.zio::zio-metrics-connectors:2.0.8",
    ivy"dev.zio::zio-http:3.0.0-RC2"
  )
  override def runIvyDeps = Agg(
    Deps.logbackClassic
  )
}

def validateJar() = T.command {
  val jar = T.workspace / "out" / "app" / "jar.dest" / "out.jar"
  lambdatest.Assert.assertTrue(os.exists(jar))
  ()
}

def validateAssembly() = T.command {
  val jar = T.workspace / "out" / "app" / "springBootAssembly.dest" / "out.jar"
  lambdatest.Assert.assertTrue(os.exists(jar), "missing? " + os.list(jar / os.up))

  val res = os.proc("java", "-jar", jar, "-o", "ran.log").call(cwd = T.dest)
  lambdatest.Assert.assertEquals(os.read(T.dest / "ran.log").trim(), "1".trim(), s"res: ${res}")

  val exe = if (scala.util.Properties.isWin) {
    os.copy(from = jar, to = T.dest / "app.bat")
    T.dest / "app.bat"
  } else jar
  val res2 = os.proc(exe, "-o", "ran2.log").call(cwd = T.dest)
  lambdatest.Assert.assertEquals(os.read(T.dest / "ran2.log").trim(), "1".trim(), s"res: ${res2}")
  ()
}
