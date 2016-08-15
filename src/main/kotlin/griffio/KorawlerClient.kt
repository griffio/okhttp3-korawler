package griffio

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.FileWriter
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Scanner
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

  val csv = URL("https://raw.githubusercontent.com/griffio/griffio.github.io/master/_data/techblogs.csv").readText()

  val scanner = Scanner(csv.trim())

  scanner.useDelimiter("[,\n]")

  val header = scanner.nextLine()

  val koData = mutableListOf<KoData>()

  while (scanner.hasNextLine()) {
    val name = scanner.next()
    val blog = scanner.next()
    val github = scanner.next()
    val careers = scanner.next()
    koData.add(KoData(name, URL(blog), github, URL(careers)))
  }

  val threadPool = Executors.newFixedThreadPool(koData.size / 2)

  val completionService = ExecutorCompletionService<KoData?>(threadPool)

  val ok = okClient()

  koData.forEach { data ->
    completionService.submit {
      try {
        requests(ok, data)
      } catch(e: Exception) {
        e.printStackTrace()
        null
      }
    }
  }

  val result = StringBuilder(header).appendln()

  for (i in 1..koData.size) {
    completionService.take().get()?.let {
      println("submitted: ${i}: ${it}")
      result.appendln("${it.name},${it.blog},${it.github},${it.careers}")
    }
  }

  FileWriter("blogs.csv").use { f ->
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
      "$url [404]"
    }
    is OKResponse.Error -> {
      "$url [500]"
    }
    is OKResponse.Exception -> {
      response.exception.toString()
    }
  }
}

fun requestAsync(ok: OkHttpClient, url: HttpUrl) {
  Korawler(ok).getAsynchronous(url)
}