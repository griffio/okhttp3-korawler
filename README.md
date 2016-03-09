# okhttp3-korawler

Kotlin 1.0

https://github.com/square/okhttp

"com.squareup.okhttp3:okhttp:3.2.0"

Example
---

crawler -> korawler

Synchronous
```
client.newCall(request).execute()

```

ASynchronous
```
client.newCall(request).enqueue(object : Callback {
   override fun onFailure(call: Call?, ioEx: IOException?) {
                ...
   }

   override fun onResponse(call: Call?, resp: Response?) {
                resp?.body()?.close()
                ...
   }
})
```