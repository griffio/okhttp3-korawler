package griffio

import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.security.cert.X509Certificate
import java.util.concurrent.LinkedBlockingQueue
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>) {

  if (args.size != 2) {
    println("Usage: Korawler <cache dir> <root>")
    return
  }

  val client = OkHttpClient.Builder().build()
  val url = HttpUrl.parse(args[1])
  val korawler = Korawler(client)
  val blogs = korawler.getSynchronous(url)
  val csv = blogs.orEmpty()
  val lines = csv.lines()

  for (i in 1..lines.size - 1) {
    val blogUrl = lines[i].substringAfter(",").substringBefore(",")
    if (!blogUrl.isNullOrBlank()) {
      println(blogUrl)
      korawler.getAsynchronous(HttpUrl.parse(blogUrl))
    }
  }

  korawler.drainQueuedCalls()

  client.dispatcher().executorService().shutdown()
}

sealed class OKResponse {
  class Success(val body: String) : OKResponse()
  class Redirect(val location: String) : OKResponse()
  class NotFound() : OKResponse()
  class Error(val code: Int) : OKResponse()
}

val tls = object:X509TrustManager {
  override fun checkClientTrusted(p0: Array<out X509Certificate>?, authType: String?) {
  }

  override fun checkServerTrusted(certs: Array<out X509Certificate>?, authType: String?) {
  }

  override fun getAcceptedIssuers(): Array<out X509Certificate> {
    return emptyArray()
  }
}

public class Korawler(val client: OkHttpClient) {

  var queue = LinkedBlockingQueue<String>()

  fun getSynchronousOKResponse(url: HttpUrl): OKResponse {
    val request = Request.Builder().url(url).build()
    try {
      val response = client.newCall(request).execute()
      val body = response.body()
      val content = body.string()
      body.close()
      return when {
        response.isRedirect -> OKResponse.Redirect(response.header("Location"))
        response.isSuccessful && !getMetaRefresh(content) -> OKResponse.Success("OK")
        response.code() == 416 -> OKResponse.Success("OK")
        response.code() == HTTP_NOT_FOUND -> OKResponse.NotFound()
        else -> OKResponse.Error(response.code())
      }
    } catch (e: Exception) {
      return OKResponse.Error(500)
    }
  }

  fun getMetaRefresh(content: String): Boolean {
    return content.matches(Regex("(?i).*http-equiv=\"refresh\".*"))
  }

  fun getSynchronous(url: HttpUrl): String? {
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()

    if (!response.isSuccessful) throw IOException("Unexpected code " + response)
    return response.body().string()
  }

  fun getAsynchronous(url: HttpUrl) {

    val request = Request.Builder().head().url(url).build()

    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call?, ioEx: IOException?) {
        queue.add(String.format("%s %s", call?.request()?.url(), ioEx?.message))
      }

      override fun onResponse(call: Call?, resp: Response?) {
        resp?.body()?.close()
        queue.add(String.format("%s %s", call?.request()?.url(), resp?.isSuccessful))
      }
    })
  }

  fun drainQueuedCalls() {
    while (client.dispatcher().queuedCallsCount() > 0 || client.dispatcher().runningCallsCount() > 0 || queue.size > 0) {
      println(queue.take())
    }
  }
}