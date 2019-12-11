package io.nichijou.ktor.ws

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.event.Level
import java.io.Reader
import java.lang.reflect.Method
import java.net.URL
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random


@KtorExperimentalAPI
@ObsoleteCoroutinesApi
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
fun main() {
  embeddedServer(
    CIO,
    host = "localhost",
    port = 80,
    watchPaths = listOf("doofen-server"),
    module = Application::module
  ).start(wait = true)
}

object CIO : ApplicationEngineFactory<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
  override fun create(environment: ApplicationEngineEnvironment, configure: CIOApplicationEngine.Configuration.() -> Unit): CIOApplicationEngine {
    environment.monitor.subscribe(ApplicationStarted, ::onStarted)
    environment.monitor.subscribe(ApplicationStopped, ::onStopped)
    return CIOApplicationEngine(environment, configure)
  }
}

fun onStarted(app: Application) {
  app.log.info("应用启动完成...")
  val url = "http://localhost"
  try {
    browse(url)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

fun onStopped(app: Application) {
  app.log.info("Bye...")
}

@ObsoleteCoroutinesApi
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
fun Application.module() {
  install(CallLogging) {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/") }
  }
  install(Compression) {
    gzip {
      priority = 1.0
    }
    deflate {
      priority = 10.0
      minimumSize(1024)
    }
  }

  install(WebSockets) {
    pingPeriod = Duration.ofSeconds(15)
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE
    masking = false
  }

  install(ContentNegotiation) {
    gson {
    }
  }

  val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(Duration.ofMinutes(1))
    .readTimeout(Duration.ofMinutes(1))
    .retryOnConnectionFailure(true)
    .build()
  val gson = Gson()
  val baseUrl = "http://www.doofen.com/idoofen/pages/schoolexam/stuExamLast"
  val typeOfTop = object : TypeToken<DoofenResp<Top>>() {}.type
  val typeOfDoofen = object : TypeToken<DoofenResp<Doofen>>() {}.type
  val poolContext = newFixedThreadPoolContext(36, "doofen-pool")
  var job: Job? = null

  suspend fun <T> getData(url: String, callback: ((reader: Reader) -> T)) =
    suspendCancellableCoroutine<T> { continuation ->
      val request = Request.Builder()
        .url(url)
        .addHeader(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36"
        )
        .build()
      val call = okHttpClient.newCall(request)
      continuation.invokeOnCancellation {
        log.debug("取消请求: ${URL(url).path}")
        call.cancel()
      }
      try {
        call.execute().body?.use {
          try {
            continuation.resume(callback(it.charStream()))
          } catch (e: Exception) {
            continuation.resumeWithException(e)
          }
        } ?: continuation.resumeWithException(NullPointerException("ResponseBody is null."))
      } catch (e: Exception) {
        continuation.resumeWithException(e)
      }
//        使用enqueue方式请求，队列依次执行，不符合预期并行运行
//        call.enqueue(object : okhttp3.Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                continuation.resumeWithException(e)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                response.body?.let {
//                    try {
//                        continuation.resume(callback(it.charStream()))
//                    } catch (e: Exception) {
//                        continuation.resumeWithException(e)
//                    }
//                } ?: continuation.resumeWithException(NullPointerException("ResponseBody is null."))
//            }
//        })
    }

  routing {
    resource("/", "views/index.html")
    resource("/favicon.ico", "views/favicon.ico")

    static("css") {
      resources("views/css")
    }
    static("js") {
      resources("views/js")
    }
    static("img") {
      resources("views/img")
    }

    get("/top/{school}/{examId}") {
      val school = this.context.parameters["school"]
      val examId = this.context.parameters["examId"]
      val doofenResp =
        getData("$baseUrl/${school}/${examId}/getMaxScore.do?_r=${_r()}&_=${System.currentTimeMillis()}") {
          gson.fromJson<DoofenResp<Top>>(it, typeOfTop)
        }
      if (doofenResp.success == true) {
        call.respond(Resp.top(doofenResp.data))
      } else {
        call.respond(Resp.fail())
      }
    }
    webSocket("/hi") {
      send(gson.toJson(Resp.connected()))
      while (true) {
        try {
          val frame = incoming.receiveOrClosed().valueOrNull
          if (frame is Frame.Text) {
            val msg = frame.readText()
            log.debug("收到消息：$msg")
            try {
              job?.cancel()
            } catch (e: Exception) {
              e.printStackTrace()
            }
            job = GlobalScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
              log.error(
                "GlobalScope 解析出错",
                throwable
              );
            }) global@{
              if (msg.isNotBlank()) {
                val req = gson.fromJson<Req>(msg, Req::class.java) ?: return@global
                for (i in req.from..req.to) {
                  this@global.launch(poolContext + CoroutineExceptionHandler { _, throwable ->
                    log.error(
                      "解析出错",
                      throwable
                    );
                  }) child@{
                    val stuId = req.school + req.grade + _num(i)
                    if (!isActive) {// 基本不会出现这个情况
                      log.debug("未完成但取消的学号：$stuId")
                      return@child
                    }
                    val url =
                      "${baseUrl}/${req.school}/${req.grade}/$stuId?_r=${_r()}&_=${System.currentTimeMillis()}"
                    val doofenResp = getData(url) {
                      gson.fromJson<DoofenResp<Doofen>>(it, typeOfDoofen)
                    }
                    val data = if (doofenResp.success != true) {
                      Doofen(stuId = stuId)
                    } else {
                      doofenResp.data?.apply {
                        this.stuId = stuId
                      }
                    }
                    val resp = gson.toJson(Resp.data(data ?: return@child))
                    if (outgoing.isClosedForSend) {
                      job?.cancel()
                    } else {
                      this@webSocket.send(Frame.Text(resp))
                    }
                    log.debug("已完成学号：$stuId")
                  }
                }
              }
            }
          }
        } catch (e: Exception) {
          log.error("发生错误", e)
        }
      }
    }
  }
}

fun _num(num: Int): String {
  var s = num.toString()
  while (s.length < 8) {
    s = "0$s"
  }
  return s
}

fun _r(): String {
  val r = Random.nextInt(100000)
  var s = r.toString()
  while (s.length < 5) {
    s = "0$s"
  }
  return s
}

data class Req(
  val from: Int,
  val to: Int,
  val grade: String,
  val school: String
)

class Resp private constructor(
  val code: Int,
  val msg: String,
  val type: Int? = null,
  val data: Any? = null
) {
  companion object {
    private const val TYPE_MSG = 0
    private const val TYPE_DATA = 1
    private const val TYPE_TOP = 2

    fun connected(): Resp {
      return Resp(0, "已成功连接", TYPE_MSG)
    }

    fun data(data: Any?): Resp {
      return Resp(0, "success", TYPE_DATA, data)
    }

    fun top(data: Any?): Resp {
      return Resp(0, "success", TYPE_TOP, data)
    }

    fun fail(): Resp {
      return Resp(-1, "获取数据失败")
    }
  }
}

data class DoofenResp<T>(
  @SerializedName("data")
  var `data`: T? = null,
  @SerializedName("success")
  var success: Boolean? = null
)

data class Doofen @JvmOverloads constructor(
  @SerializedName("classAvgScore")
  var classAvgScore: Double? = null,
  @SerializedName("classStuNum")
  var classStuNum: Int? = null,
  @SerializedName("clsPr")
  var clsPr: Double? = null,
  @SerializedName("examGradeId")
  var examGradeId: Int? = null,
  @SerializedName("examId")
  var examId: Long? = null,
  @SerializedName("examName")
  var examName: String? = null,
  @SerializedName("exams")
  var exams: List<Exam?>? = null,
  @SerializedName("gradeAvgScore")
  var gradeAvgScore: Double? = null,
  @SerializedName("gradePr")
  var gradePr: Double? = null,
  @SerializedName("gradeStuNum")
  var gradeStuNum: Int? = null,
  @SerializedName("_id")
  var id: String? = null,


  @SerializedName("key10026")
  var key10026: Key10026? = null,
  @SerializedName("kps")
  var kps: List<Kp?>? = null,
  @SerializedName("paperScore")
  var paperScore: Double? = null,
  @SerializedName("stuClassRank")
  var stuClassRank: Int? = null,
  @SerializedName("stuGradeRank")
  var stuGradeRank: Int? = null,
  @SerializedName("stuScore")
  var stuScore: Double? = null,
  var stuId: String? = null
) {
  data class Exam(
    @SerializedName("classAvgScore")
    var classAvgScore: Double? = null,
    @SerializedName("classStuNum")
    var classStuNum: Int? = null,
    @SerializedName("gradeAvgScore")
    var gradeAvgScore: Double? = null,
    @SerializedName("gradeStuNum")
    var gradeStuNum: Int? = null,
    @SerializedName("_id")
    var id: String? = null,
    @SerializedName("paperScore")
    var paperScore: Double? = null,
    @SerializedName("stuClassRank")
    var stuClassRank: Int? = null,
    @SerializedName("stuGradeRank")
    var stuGradeRank: Int? = null,
    @SerializedName("stuScore")
    var stuScore: Double? = null,
    @SerializedName("stuStable")
    var stuStable: Int? = null,
    @SerializedName("xkId")
    var xkId: Int? = null,
    @SerializedName("xkName")
    var xkName: String? = null
  )

  data class Key10024(
    @SerializedName("1")
    var x1: List<X1?>? = null,
    @SerializedName("2")
    var x2: List<X2?>? = null,
    @SerializedName("3")
    var x3: List<X3?>? = null
  ) {
    data class X1(
      @SerializedName("classScore")
      var classScore: Double? = null,
      @SerializedName("gradeScore")
      var gradeScore: Double? = null,
      @SerializedName("qid_qcId")
      var qidQcId: String? = null,
      @SerializedName("qscore")
      var qscore: Double? = null,
      @SerializedName("stuScore")
      var stuScore: Double? = null
    )

    data class X2(
      @SerializedName("classScore")
      var classScore: Double? = null,
      @SerializedName("gradeScore")
      var gradeScore: Double? = null,
      @SerializedName("qid_qcId")
      var qidQcId: String? = null,
      @SerializedName("qscore")
      var qscore: Double? = null,
      @SerializedName("stuScore")
      var stuScore: Double? = null
    )

    data class X3(
      @SerializedName("classScore")
      var classScore: Double? = null,
      @SerializedName("gradeScore")
      var gradeScore: Double? = null,
      @SerializedName("qid_qcId")
      var qidQcId: String? = null,
      @SerializedName("qscore")
      var qscore: Double? = null,
      @SerializedName("stuScore")
      var stuScore: Double? = null
    )
  }

  data class Key10026(
    @SerializedName("0")
    var x0: List<X0?>? = null,
    @SerializedName("1")
    var x1: List<X1?>? = null,
    @SerializedName("2")
    var x2: List<X2?>? = null,
    @SerializedName("3")
    var x3: List<X3?>? = null
  ) {
    data class X0(
      @SerializedName("isStu")
      var isStu: Boolean? = null,
      @SerializedName("sRange")
      var sRange: String? = null,
      @SerializedName("stuNum")
      var stuNum: Int? = null
    )

    data class X1(
      @SerializedName("isStu")
      var isStu: Boolean? = null,
      @SerializedName("sRange")
      var sRange: String? = null,
      @SerializedName("stuNum")
      var stuNum: Int? = null
    )

    data class X2(
      @SerializedName("isStu")
      var isStu: Boolean? = null,
      @SerializedName("sRange")
      var sRange: String? = null,
      @SerializedName("stuNum")
      var stuNum: Int? = null
    )

    data class X3(
      @SerializedName("isStu")
      var isStu: Boolean? = null,
      @SerializedName("sRange")
      var sRange: String? = null,
      @SerializedName("stuNum")
      var stuNum: Int? = null
    )
  }

  data class Kp(
    @SerializedName("data")
    var `data`: List<Data?>? = null,
    @SerializedName("xkId")
    var xkId: Int? = null,
    @SerializedName("xkName")
    var xkName: String? = null
  ) {
    data class Data(
      @SerializedName("cnt")
      var cnt: Int? = null,
      @SerializedName("kpId")
      var kpId: Int? = null,
      @SerializedName("kpName")
      var kpName: String? = null
    )
  }
}


data class Top(
  @SerializedName("gradeTopScore")
  var gradeTopScore: Double? = null,
  @SerializedName("_id")
  var id: String? = null
)


fun browse(url: String) { // 获取操作系统的名字
  val osName = System.getProperty("os.name", "")
  if (osName.startsWith("Mac OS")) { // 苹果的打开方式
    val fileMgr = Class.forName("com.apple.eio.FileManager")
    val openURL: Method = fileMgr.getDeclaredMethod("openURL", String::class.java)
    openURL.invoke(null, url)
  } else if (osName.startsWith("Windows")) { // windows的打开方式。
    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url")
  } else { // Unix or Linux的打开方式
    val browsers = arrayOf("firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape")
    var browser: String? = null
    var count = 0
    while (count < browsers.size && browser == null) {
      // 执行代码，在brower有值后跳出，
      // 这里是如果进程创建成功了，==0是表示正常结束。
      if (Runtime.getRuntime().exec(arrayOf("which", browsers[count])).waitFor() == 0) browser = browsers[count]
      count++
    }
    if (browser == null) throw Exception("Could not find web browser") else  // 这个值在上面已经成功的得到了一个进程。
      Runtime.getRuntime().exec(arrayOf(browser, url))
  }
}
