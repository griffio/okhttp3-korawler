# okhttp3-korawler

Kotlin 1.0.4

https://github.com/square/okhttp

"com.squareup.okhttp3:okhttp:3.2.0"

Example
---

KorawlerClient keeps blog urls valid and updated

``` kotlin

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

```
