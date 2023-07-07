import $exec.plugins
import $exec.shared

import helper.Deps
import de.tobiasroeser.lambdatest

import mill._, mill.scalalib._
import de.tobiasroeser.mill.spring.boot.SpringBootModule

object app extends MavenModule with SpringBootModule {
  override def millSourcePath = super.millSourcePath / os.up
  override def springBootToolsVersion = "2.7.3"
  override def ivyDeps = Agg(
    Deps.slf4j
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
//  val baseJar = T.workspace / "out" / "app" / "upstreamAssembly.dest" / "out.jar"
//  lambdatest.Assert.assertTrue(os.exists(baseJar))
  val jar = T.workspace / "out" / "app" / "assembly.dest" / "out.jar"
  lambdatest.Assert.assertTrue(os.exists(jar), "missing? " + os.list(jar / os.up))

  val res = os.proc("java", "-jar", jar, "-o", "ran.log").call(cwd = T.dest)
  lambdatest.Assert.assertEquals(os.read(T.dest / "ran.log").trim(), "1".trim(), s"res: ${res}")

  val res2 = os.proc(jar, "-o", "ran2.log").call(cwd = T.dest)
  lambdatest.Assert.assertEquals(os.read(T.dest / "ran2.log").trim(), "1".trim(), s"res: ${res}")
  ()
}
