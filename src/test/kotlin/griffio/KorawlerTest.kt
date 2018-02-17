package griffio

import okhttp3.HttpUrl
import org.junit.Test
import java.net.URL
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

class KorawlerTest {

  @Test
  fun `request for ssl certs lets encrypt`() {

    val ok = okClient()

    val response = Korawler(ok).getSynchronousOKResponse(URL("https://letsencrypt.org/"))

    println(response)
  }

  @Test
  fun `request for ssl certs ssl start`() {

    val ok = okClient()

    val response = Korawler(ok).getSynchronousOKResponse(URL("https://www.startssl.com/"))

    println(response)
  }

  @Test
  fun `request has relative redirect Location header`() {

    val ok = okClient()

    val response = Korawler(ok).getSynchronousOKResponse(URL("https://www.lyst.com/careers"))

    println(response)
  }

  @Test
  fun `async request`() {

    val url = HttpUrl.parse("https://letsencrypt.org/") ?: throw Exception("url parse error")

    val ok = okClient()

    Korawler(ok).apply {
      getAsynchronous(url)
      drainQueuedCalls()
    }
  }

  @Test
  fun `synchronous service request`() {

    val ok = okClient()

    val korawler = Korawler(ok)

    val urls = listOf(URL("https://www.startssl.com/"), URL("https://letsencrypt.org/"), URL("https://www.google.com/"))

    for (url in urls) {
      println(korawler.getSynchronousOKResponse(url))
    }

    System.out.println("Done.")
  }

  @Test
  fun `completion service request`() {

    val ok = okClient()

    val korawler = Korawler(ok)

    val completionService = ExecutorCompletionService<OKResponse>(Executors.newFixedThreadPool(3))

    val urls = listOf(URL("https://www.startssl.com/"), URL("https://letsencrypt.org/"), URL("https://www.google.com/"))

    for (url in urls) {
      completionService.submit { korawler.getSynchronousOKResponse(url) }
    }

    for (url in urls) {
      completionService.take().get().let { println(it) }
    }

    System.out.println("Done.")
  }
}
