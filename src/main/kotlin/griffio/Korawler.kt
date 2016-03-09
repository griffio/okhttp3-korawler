package griffio

import okhttp3.Callback
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

fun main(args: Array<String>) {

    if (args.size != 2) {
        println("Usage: Korawler <cache dir> <root>")
        return
    }

    val client = OkHttpClient.Builder().build()
    val url = HttpUrl.parse(args[1])
    val korawler = Korawler(client)
    val blogs = korawler.getSynchronous(url)
    val csv = blogs.orEmpty()
    val lines = csv.lines()

    for (i in 1..lines.size -1) {
        val blogUrl = lines[i].substringAfter(",").substringBefore(",")
        if (!blogUrl.isNullOrBlank()) {
            println(blogUrl)
            korawler.getAsynchronous(HttpUrl.parse(blogUrl))
        }
    }

    korawler.drainQueuedCalls()

    client.dispatcher().executorService().shutdown()
}

public class Korawler(val client: OkHttpClient) {

    var queue = LinkedBlockingQueue<String>()

    fun getSynchronous(url: HttpUrl): String? {
        var request = Request.Builder().url(url).build()
        var response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code " + response)
        var responseHeaders = response.headers()
        for (i in 0..responseHeaders.size() - 1) {
            System.out.printf("%s %s", responseHeaders.name(i), responseHeaders.value(i))
        }
        return response.body().string()
    }

    fun getAsynchronous(url: HttpUrl) {

        var request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, ioEx: IOException?) {
                queue.add(String.format("%s %s", call?.request()?.url(), ioEx?.message))
            }

            override fun onResponse(call: Call?, resp: Response?) {
                resp?.body()?.close()
                queue.add(String.format("%s %s", call?.request()?.url(), resp?.isSuccessful))
            }
        })
    }

    fun drainQueuedCalls() {
        while (client.dispatcher().queuedCallsCount() > 0 || client.dispatcher().runningCallsCount() > 0 || queue.size > 0) {
            println(queue.take())
        }
    }
}