import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

fun ByteArray.hexlify(): String =
    joinToString(" ") { String.format("%02x", it) }

suspend fun runSocksHttpRequestTest(channel: AsynchronousSocketChannel) {
    val socket = SocksCoroutineSocket(InetSocketAddress(InetAddress.getByName("18.193.144.23"), 1080), channel)
    socket.init()
    println("connecting...")
    socket.connect(InetSocketAddress("api.ipify.org", 80))
    println("connected...")
    val req = "GET /?format=json HTTP/1.1\r\nConnection: close\r\nHost: api.ipify.org\r\nAccept: */*\r\nUser-Agent: curl/1.1.1\r\n\r\n"
        .toByteArray()
    println("writing...")
    ByteBuffer.wrap(req).let {
        socket.write(it)
    }
    println("wrote")
    val builder = StringBuilder()
    val buffer = ByteBuffer.allocate(4)
    while (true) {
        val count = socket.read(buffer)
        if (count <= 0)
            break
        builder.append(buffer.array().joinToString("") { it.toChar().toString() })
        buffer.clear()
    }
    println(builder.toString())
}

suspend fun runCoroutineSocketTest(channel: AsynchronousSocketChannel, dispatcher: CoroutineDispatcher) = withContext(dispatcher) {
    val socket = SocksCoroutineSocket(InetSocketAddress("91.210.166.50", 1080), channel)

    launch {
        socket.connect(InetSocketAddress("localhost", 9999))

        val buffer = ByteBuffer.allocate(16)
        while (true) {
            println("reading...")
            val count = socket.read(buffer)
            println("read finished!")
            if (count <= 0) {
                println("socket read failure $count")
                break
            }
            println("socket: " + buffer.array().hexlify())
            buffer.clear()
        }
    }

    launch {
        while (true) {
            println("ping?!")
            delay(500)
            println("pong!!")
            delay(500)
        }
    }
}

@ObsoleteCoroutinesApi
fun main() {
    val thread = newFixedThreadPoolContext(1, "thread")
    val channel = AsynchronousSocketChannel.open()
    runBlocking {
        runSocksHttpRequestTest(channel)
    }
}