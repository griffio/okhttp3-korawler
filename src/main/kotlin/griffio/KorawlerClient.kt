package griffio

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Scanner
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun okClient(): OkHttpClient {

  val acceptAllTrustManager = object : X509TrustManager {
    override fun checkClientTrusted(certs: Array<out X509Certificate>?, authType: String?) { }
    override fun checkServerTrusted(certs: Array<out X509Certificate>?, authType: String?) { }
    override fun getAcceptedIssuers(): Array<out X509Certificate> = emptyArray()
  }

  val sc = SSLContext.getInstance("SSL")
  sc.init(null, arrayOf(acceptAllTrustManager), SecureRandom())

  return OkHttpClient.Builder()
      .sslSocketFactory(sc.socketFactory)
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
    val jobs = scanner.next()

    println(blog)
    println(jobs)

    val blogUrl = HttpUrl.parse(blog)
    val jobsUrl = HttpUrl.parse(jobs)

    request(ok, blogUrl)?.let { blog ->
      request(ok, jobsUrl)?.let { jobs ->
        result.append("$name,$blog,$github,$jobs").appendln()
      }
    }

  }

  FileWriter("blogs.csv").use { f ->
    f.write(result.toString())
  }
}

fun request(ok: OkHttpClient, url: HttpUrl): String? {

  val response = Korawler(ok).getSynchronousOKResponse(url)

  return when (response) {
    is OKResponse.Success -> {
      url.toString()
    }
    is OKResponse.Redirect -> {
      response.location
    }
    is OKResponse.NotFound -> {
      "$url [404]"
    }
    is OKResponse.Error -> {
      "$url [500]"
    }
  }
}

fun header(): StringBuilder {
  return StringBuilder("desc,url,github,jobs").appendln()
}

