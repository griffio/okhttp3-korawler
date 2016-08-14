package griffio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class KoDataTest {

  @Test
  fun `load koData from csv`() {

    val data = loadFromCsv("""
      desc,url,github,careers
      a1,http://example.com/,b1,http://example.com/careers/
      b1,http://example.com/,b2,http://example.com/careers/
      b3,http://example.com/,b3,http://example.com/careers/
      b1,http://example.com/,b2,http://example.com/careers/
      """)

    assertEquals("input csv must be trimmed, valid format, duplicates removed", 3, data.size)
  }

  @Test
  fun `koData thread pool`() {

    val url = URL("file:////.")
    val data = setOf(
        KoData("a1", url, "a2", url),
        KoData("b1", url, "b2", url),
        KoData("c2", url, "c3", url))

    val threadPool = Executors.newFixedThreadPool(8)

    for (ko in data) {
      threadPool.submit({ Thread.sleep(3000); println(ko) })
    }

    threadPool.shutdown()
    val finished = threadPool.awaitTermination(2, TimeUnit.MINUTES)
    println(if (finished) "Successful" else "Timed out")
  }
}