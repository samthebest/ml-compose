package ml_compose

import java.io.File
import com.m3.curly.{HTTP, Request}

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try
import BashUtils._
import spray.json._
import DefaultJsonProtocol._

object ZeppelinAdminUtils {
  val ip = Option(System.getenv("DOCKER_HOST")).map(_.stripPrefix("tcp://").split(":").head).getOrElse("localhost")
  val port = 8080

  val projectName = "ml_compose"

  implicit class PimpedStringSeq(l: Seq[String]) {
    def writeLines(p: String): Unit = {
      val pw = new java.io.PrintWriter(new File(p))
      try pw.write(l.mkString("\n") + "\n") finally pw.close()
    }
  }

  def curlyPost(url: String, body: String): String = {
    val request = new Request(url)
    request.setReadTimeoutMillis(100 * 1000)
    request.setBody(body.getBytes, "text/json")

    val result = HTTP.post(request)
    assert(result.getStatus == 200, s"Bad status (${result.getStatus}: ${result.getTextBody}")
    result.getTextBody
  }

  def curlyPostNoBody(url: String): String = {
    val request = new Request(url)
    request.setReadTimeoutMillis(100 * 1000)

    val result = HTTP.post(request)
    assert(result.getStatus == 200, s"Bad status (${result.getStatus}: ${result.getTextBody}")
    result.getTextBody
  }

  def curlyPut(url: String, body: String): String = {
    val request = new Request(url)
    request.setReadTimeoutMillis(100 * 1000)
    request.setBody(body.getBytes, "text/json")

    val result = HTTP.put(request)
    assert(result.getStatus == 200, s"Bad status (${result.getStatus}: ${result.getTextBody}")
    result.getTextBody
  }

  def curlyGet(url: String): String = {
    val request = new Request(url)
    request.setReadTimeoutMillis(60 * 1000)

    val result = HTTP.get(request)
    assert(result.getStatus == 200, s"Bad status (${result.getStatus}: ${result.getTextBody}")
    result.getTextBody
  }

  // TODO Work out how to supress noisy logging
  def zeppelinApiGet(segment: String): String = curlyGet("http://" + ip + ":" + port + segment)
  def zeppelinApiPost(segment: String, json: String): String =
    curlyPost("http://" + ip + ":" + port + segment, json)
  def zeppelinApiPostNoBody(segment: String): String =
    curlyPostNoBody("http://" + ip + ":" + port + segment)
  def zeppelinApiPut(segment: String, json: String): String =
    curlyPut("http://" + ip + ":" + port + segment, json)


  def startZeppelin(): Unit = runScriptDebug("run-local-mode.sh", System.getenv("CUSTOM_ZEPPELIN_ARGS"))

  def stopZeppelin(): Unit = runScript("stop.sh")

  // TODO Use json parsing
  def extractNotebookId(path: String): String =
    Source.fromFile(path).getLines().find(_.startsWith("  \"id\": \""))
    .map(_.stripPrefix("  \"id\": \"").stripSuffix("\",")).get


  def extractNotebookName(path: String): String =
    Source.fromFile(path).getLines().find(_.startsWith("  \"name\": \""))
    .map(_.stripPrefix("  \"name\": \"").stripSuffix("\",")).get


  def awaitStart(future: Future[Unit], maxRetries: Int = 300): Boolean = {
    var retries = 0
    println(s"INFO: Waiting for $projectName to start")

    var finished = false
    var wasSuccess = false

    val isSuccess = () => Try(apiReady()).isSuccess

    while (!finished) {
      wasSuccess = isSuccess()
      finished = wasSuccess || future.isCompleted || retries == maxRetries
      println("INFO: ... " + retries)
      java.lang.Thread.sleep(5 * 1000)
      retries += 1
    }

    println("INFO: Zeppelin " + (if (wasSuccess) "started" else "did not start!"))

    wasSuccess
  }

  def apiReady(): Boolean = {
    zeppelinApiGet("/api/interpreter").parseJson.asJsObject.fields("body").asJsObject.fields.values.toList
    .map(_.asJsObject.fields("id").convertTo[String])

    true
  }

  def interpreterIds(): List[String] = zeppelinApiGet("/api/interpreter/setting")
                                       .parseJson.asJsObject.fields("body").convertTo[List[JsObject]].map(_.fields("id").convertTo[String])

  def bindInterpreter(interpreterIds: List[String], notebookId: String): String = {
    println("Binding interpreter for notebook: " + notebookId, ", interpreter ids = " + interpreterIds)
    zeppelinApiPut(s"/api/notebook/interpreter/bind/$notebookId", interpreterIds.toJson.toString())
  }

  // TODO DRY, jobFinishedAndSuccess and jobFailures duplicate code, also make unnecessarily many calls to API
  def runNotebook(notebookId: String): Option[List[String]] = {
    zeppelinApiPostNoBody(s"/api/notebook/job/$notebookId")

    var retries = 0
    val max = 20

    while (!jobFinishedAndSuccess(notebookId) && jobFailures(notebookId).isEmpty && retries != max) {
      java.lang.Thread.sleep(2000)
      retries += 1
    }

    jobFailures(notebookId)
  }

  def jobFinishedAndSuccess(notebookId: String): Boolean = {
    val jobStatsJson = zeppelinApiGet(s"/api/notebook/job/$notebookId").parseJson.asJsObject

    println("jobStatsJson = " + jobStatsJson)

    val status = jobStatsJson.fields("status").convertTo[String]

    println("status = " + status)

    val cellStatus: List[String] =
      jobStatsJson.fields("body").convertTo[List[JsObject]].map(_.fields("status").convertTo[String])

    println("cellStatus = " + cellStatus)

    if (status == "OK") {
      val numCells = cellStatus.size
      val numSuccesses = cellStatus.count(_ == "FINISHED")

      numCells == numSuccesses
    } else false
  }

  // TODO return titles as well if they exist
  def jobFailures(notebookId: String): Option[List[String]] =
    zeppelinApiGet(s"/api/notebook/job/$notebookId").parseJson.asJsObject.fields("body")
    .convertTo[List[JsObject]].map(_.fields("status").convertTo[String]).find(_ == "ERROR")
    .map(_ => zeppelinApiGet(s"/api/notebook/$notebookId").parseJson.asJsObject
              .fields("body").asJsObject.fields("paragraphs").convertTo[List[JsObject]]
              .flatMap(_.fields.get("result").map(_.asJsObject))
              .filter(_.fields("code").convertTo[String] == "ERROR")
              .map(_.fields("msg").convertTo[String]))
}
