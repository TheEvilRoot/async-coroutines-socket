async-coroutines-socket
--------

is a small kotlin library that provides an implementation for `java.nio.channels.AsynchronousSocketChannel` wrapped in kotlin coroutines.

preamble
--------

Using `java.net.Socket` connect/read/write operations within kotlin coroutines causes blocking calls that blocks either 
current thread or other's dispatcher thread (in case you are run it on `Dispatchers.IO`).

For reason to have non-blocking socket operations `java.nio` have `AsynchronousSocketChannel` class that provides 
the same functional but with callbacks. So, it's connect/read/write functions returns right after it's called 
without waiting operation to be completed. When operation is actually finished, callback (`java.nio.channels.CompletionHandler` interface)
is called with either success or failure method. 

But using callback based socket is inconvenient after all. It's commonly causes lots of lambdas, interfaces and 
indentation levels. 

Here's out library comes in. Using a `suspendCoroutine` and `Continuation` mechanism in kotlin coroutines
we're wrapped a callback based functions into coroutine-blocking function. 

It's just blocking a current coroutine on connect/read/write call and resume it on callback functions -- success or failure.

usage
-------

Some examples of usage this library is stored in `src/test/kotlin` package.

##### Using a regular Socket

```kotlin

    suspend fun someFunction() {
        // Socket(InetAddress, int) actually calls connect(InetSocketAddress)
        // so this call is thread-blocking
        val socket = Socket(InetAddress.getByName("localhost"), 9999)
        val buffer = ByteArray(16)
        // this call will block entire thread 
        // and all coroutines that working on it right now
        val count = socket.getInputStream().read(buffer)
        // ... 
    }

```

##### Using a coroutine socket

```kotlin

    suspend fun someFunction(channel: AsynchronousSocketChannel) {
        val socket = CoroutineSocket(channel)
        // this call actually uses callback to resume coroutine
        // and other coroutines will working while this one 
        // is awaiting for connect() finish
        socket.connect(InetSocketAddress("localhost", 9999))
        val byteBuffer = ByteByffer.allocate(16)
        // same as connect(), read() will not block the thread
        // just a single coroutine until read operation is complete
        val count = socket.read(byteBuffer)
        // ...
    }
```