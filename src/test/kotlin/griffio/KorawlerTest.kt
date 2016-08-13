package griffio

import okhttp3.HttpUrl
import org.junit.Test

class KorawlerTest {

  @Test
  fun `request for ssl certs lets encrypt`() {

    val ok = okClient()

    val response = Korawler(ok).getSynchronousOKResponse(HttpUrl.parse("https://letsencrypt.org/"))

    println(response)

  }

  @Test
  fun `request for ssl certs ssl start`() {

    val ok = okClient()

    val response = Korawler(ok).getSynchronousOKResponse(HttpUrl.parse("https://www.startssl.com/"))

    println(response)
  }
}