package griffio

import java.net.URL

fun main(args: Array<String>) {

  val url = "https://raw.githubusercontent.com/griffio/griffio.github.io/master/_data/techblogs.csv"

  val csvSeq = URL(url).readText().trim().lines().asSequence()

  val header = csvSeq.first()

  val koData = csvSeq.drop(1).map {
    it.split(",").let { values ->
      KoData(values[0], URL(values[1]), values[2], URL(values[3]))
    }
  }

  val githubApi = "https://api.github.com/users/griffio/repos\\?per_page\\=12\\&page\\=1"

}