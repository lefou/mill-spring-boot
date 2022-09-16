import $ivy.`de.tototec:de.tobiasroeser.lambdatest:0.7.1`

import mill.scalalib._
import de.tobiasroeser.lambdatest

object Deps {
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.20"
  val logbackClassic = ivy"ch.qos.logback:logback-classic:1.2.8"
}

def checkNonexistantFile(f: os.Path): Option[String] = Option(f)
  .filter(f => os.exists(f))
  .map(f => s"File should not exist: ${f}")

def checkEmptyDir(d: os.Path): Option[String] = Option(d)
  .filter(d => os.exists(d) && !os.list(d).isEmpty)
  .map(d => s"Directory present or non-empty: ${d}")

def checkExistingFile(f: os.Path): Option[String] = Option(f)
  .filter(f => !os.exists(f))
  .map(f => s"File should exist: ${f}")

def checkFileContents(f: os.Path, c: String): Option[String] =
  if (!os.exists(f)) Option(s"File does not exist: ${f}")
  else {
    val contents = os.read(f).trim()
    val expected = c.trim()
    compare(contents, expected).map(m => s"Contents does not match. File: ${f}" + "\n" + m)
  }

def checkFileContains(f: os.Path, c: String): Option[String] =
  if (!os.exists(f)) Option(s"File does not exist: ${f}")
  else {
    val contents = os.read(f).trim()
    if (contents.contains(c)) None
    else Option(s"File contents does not match. File: ${f}" + "\nExpected partial content:\n" + c)
  }

def compare(s: String, s2: String): Option[String] = {
  try {
    lambdatest.Assert.assertEquals(s, s2)
    None
  } catch {
    case e: AssertionError => Some(e.getMessage())
  }
}
