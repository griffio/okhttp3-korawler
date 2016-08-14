package griffio

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.FileWriter
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Scanner
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

  val ok = okClient()

  val csv = URL("https://raw.githubusercontent.com/griffio/griffio.github.io/master/_data/techblogs.csv").readText()

  val scanner = Scanner(csv.trimEnd())

  scanner.useDelimiter("[,\n]")

  scanner.nextLine()
  val result = header()

  while (scanner.hasNextLine()) {

    val name = scanner.next()
    val blog = scanner.next()
    val github = scanner.next()
    val careers = scanner.next()

    println(blog)
    println(careers)

    val blogUrl = URL(blog)
    val jobsUrl = URL(careers)

    requests(ok, KoData(name, blogUrl, github, jobsUrl)).let {
      result.appendln("$name,$blog,$github,$careers")
    }
  }

  FileWriter("blogs.csv").use { f ->
    f.write(result.toString())
  }
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

fun header(): StringBuilder {
  return StringBuilder("desc,url,github,jobs").appendln()
}
