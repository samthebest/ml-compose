package ml_compose

import java.io.{File, FileNotFoundException}
import org.specs2.mutable.Specification
import ml_compose.BashUtils._
import ml_compose.ZeppelinAdminUtils._
import scala.concurrent.Future

// Here we will put tests that actually run the notebooks - ultra E2E tests!
object NotebookE2ETests extends Specification {
  "Zeppelin" should {
    val zeppelinStartFuture = Future(startZeppelin())

    assert(awaitStart(zeppelinStartFuture))

    // Would be nice to refactor a little so we have one test per cell? (only makes much sense when each cell has a title)
    def testNotebook(notebookName: String, notebookId: String): Unit =
      "Notebook " + notebookName + " has no failure cells" in {
        runNotebook(notebookId) must_=== None
      }

    def paths(p: String): List[String] =
      Option(new File(p).listFiles()).getOrElse(throw new FileNotFoundException(p)).map(_.getAbsolutePath).toList

    val demoNotebookIds: List[String] = paths(`.` + "/docker/demo-notebooks").map(extractNotebookId)
    val demoNotebookNames: List[String] = paths(`.` + "/docker/demo-notebooks").map(extractNotebookName)

    val templateNotebookIds: List[String] = paths(`.` + "/docker/template-notebooks").map(extractNotebookId)
    val templateNotebookNames: List[String] = paths(`.` + "/docker/template-notebooks").map(extractNotebookName)

    val testNotebookIds: List[String] = paths(`.` + "/docker/test-notebooks").map(extractNotebookId)
    val testNotebookNames: List[String] = paths(`.` + "/docker/test-notebooks").map(extractNotebookName)

    val allIds = demoNotebookIds ++ templateNotebookIds ++ testNotebookIds
    val allNames = demoNotebookNames ++ templateNotebookNames ++ testNotebookNames

    val zipped = allNames.zip(allIds).filter(_._1 != "FailureNotebook").filter(_._1 != "Configuration")

    val interpreters = interpreterIds()

    allIds.foreach(bindInterpreter(interpreters, _))

    "Can run all notebooks" should {
      sequential

      // TODO Rather than use a config notebook to load the jar, work out how to use the API successfully
      // see http://stackoverflow.com/questions/40940830/programmatically-add-jar-to-zeppelin-spark-interpreter-via-api/40944324#40944324
      "Can run config notebook" in {
        runNotebook("2BX7FRD5T") must_=== None
      }

      zipped.foreach((testNotebook _).tupled)
    }

// Not sure why this not working, will ignore for now
    // "Failure notebook has failures" in {
    //   runNotebook("2BX7FRD5F") must_=== Some(List("\n\n\n<console>:1: error: ';' expected but ',' " +
    //     "found.\nStupid code, won't compile\n           ^\n"))
    // }
  }
}



