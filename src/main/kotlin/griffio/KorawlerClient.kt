package griffio

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.FileWriter
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun okClient(): OkHttpClient {

  val acceptAllTrustManager = object : X509TrustManager {
    override fun checkClientTrusted(certs: Array<out X509Certificate>?, authType: String?) {
    }

    override fun checkServerTrusted(certs: Array<out X509Certificate>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<out X509Certificate> = emptyArray()
  }

  val sc = SSLContext.getInstance("SSL")
  sc.init(null, arrayOf(acceptAllTrustManager), SecureRandom())

  return OkHttpClient.Builder()
      .sslSocketFactory(sc.socketFactory, acceptAllTrustManager)
      .hostnameVerifier { hostname, sslSession -> true }
      .followRedirects(false).followSslRedirects(false).build()
}

fun main(args: Array<String>) {

  val url = "https://raw.githubusercontent.com/griffio/griffio.github.io/master/_data/techblogs.csv"

  val csvSeq = URL(url).readText().trim().lines().asSequence()

  val header = csvSeq.first()

  val koData = csvSeq.drop(1).map {
    it.split(",").let { values ->
      KoData(values[0], URL(values[1]), values[2], URL(values[3]))
    }
  }

  val threadPool = Executors.newCachedThreadPool { runnable ->
    Thread(runnable).apply {
      setUncaughtExceptionHandler { thread, throwable -> throwable.printStackTrace() }
    }
  }

  val completionService = ExecutorCompletionService<KoData?>(threadPool)

  val ok = okClient()

  koData.forEach { data ->
    completionService.submit {
      requests(ok, data)
    }
  }

  val result = StringBuilder(header).appendln()

  koData.forEach {
    completionService.take().get()?.let {
      result.appendln("${it.name},${it.blog},${it.github},${it.careers}")
    }
  }

  val iso = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

  FileWriter("%s.csv".format(iso)).use { f ->
    f.write(result.toString())
  }

  threadPool.shutdown()
}

fun requests(ok: OkHttpClient, data: KoData): KoData? {
  request(ok, data.blog)?.let { blog ->
    request(ok, data.careers)?.let { careers ->
      return data.copy(blog = URL(blog), careers = URL(careers))
    }
  }
  return null
}

fun request(ok: OkHttpClient, url: URL): String? {

  val response = Korawler(ok).getSynchronousOKResponse(url)

  return when (response) {
    is OKResponse.Success -> {
      url.toString()
    }
    is OKResponse.Redirect -> {
      if (response.location.startsWith("/")) URL(url, response.location).toString() else response.location
    }
    is OKResponse.NotFound -> {
      println("$url [404]")
      null
    }
    is OKResponse.Error -> {
      println("$url [500]")
      null
    }
    is OKResponse.Exception -> {
      println("$url ${response.exception?.message}")
      null
    }
  }
}

fun requestAsync(ok: OkHttpClient, url: HttpUrl) {
  Korawler(ok).getAsynchronous(url)
}