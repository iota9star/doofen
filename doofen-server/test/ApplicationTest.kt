package io.nichijou.ktor.ws

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URL
import java.util.*


//class ApplicationTest {
//    @Test
//    fun testRoot() {
//        withTestApplication({ module(testing = true) }) {
//            handleRequest(HttpMethod.Get, "/").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("HELLO WORLD!", response.content)
//            }
//        }
//    }
//}

val gson = Gson()
fun main() {
  val list = Vector<D.Data>()
  runBlocking {
    for (i in 280000..284100) {
      launch {
        val json = URL("http://www.doofen.com/idoofen/pages/schoolexam/stuSchName/$i/getSchName.do?&_r=28633&_=1575860201004").readText()
        val fromJson = gson.fromJson<D>(json, D::class.java) ?: return@launch
        print(".")
        if (fromJson.success == true) {
          print(" $i ")
          val data = fromJson.data ?: return@launch
          list.add(data)
        }
      }
    }
  }
  println(gson.toJson(list))
}

data class D(
  @SerializedName("data")
  var `data`: Data? = null,
  @SerializedName("success")
  var success: Boolean? = null // true
) {
  data class Data(
    @SerializedName("_id")
    var id: String? = null, // 280301
    @SerializedName("schAlias")
    var schAlias: String? = null, // 成都市天府第七中学
    @SerializedName("schName")
    var schName: String? = null // 成都市天府第七中学
  )
}
