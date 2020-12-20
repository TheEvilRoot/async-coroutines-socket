import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

fun ByteArray.hexlify(): String =
    joinToString(" ") { String.format("%02x", it) }

suspend fun run(channel: AsynchronousSocketChannel, dispatcher: CoroutineDispatcher) = withContext(dispatcher) {
    val socket = CoroutineSocket(channel)

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
        run(channel, thread)
    }
}