package griffio

import java.net.URL
import java.util.HashSet
import java.util.Scanner

data class KoData(val name: String, val blog: URL, val github: String, val careers: URL)

fun loadFromCsv(csv: String): Set<KoData> {

  val result = HashSet<KoData>(300)

  val scanner = Scanner(csv.trim())

  scanner.useDelimiter("[,\n]")

  scanner.nextLine()

  while (scanner.hasNextLine()) {
    result.add(KoData(scanner.next(), URL(scanner.next()), scanner.next(), URL(scanner.next())))
  }

  return result
}