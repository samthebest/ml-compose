package ml_compose

import scala.sys.process._

object BashUtils {
  def `.`: String = System.getProperty("user.dir")

  def executeString(s: String): (String, String, Int) = {
    var stderr = ""
    var stdout = ""
    val exitCode = s ! ProcessLogger(stdout += "\n" + _, stderr += "\n" + _)
    (stdout, stderr, exitCode)
  }

  def runScript(script: String, args: String = ""): (String, String, Int) =
    executeString(`.` + "/bin/" + script + " " + args)

  def runScriptDebug(script: String, args: String = ""): Int = `.` + "/bin/" + script + " " + args !

  def executeStringAndPrintln(s: String): Int = s ! ProcessLogger(println, System.err.println)

}
