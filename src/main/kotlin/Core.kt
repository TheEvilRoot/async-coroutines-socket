import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

suspend fun createAsync(readDispatcher: CoroutineDispatcher, writeDispatcher: CoroutineDispatcher, id: Int): Deferred<Unit> {
    val println = { s: String -> println("${Thread.currentThread().name} :  $id: $s") }

    val channel = AsynchronousSocketChannel.open()
    val socket = CoroutineSocket(channel)
    val addr = InetSocketAddress("localhost", 8888)
    println("connecting...")
    socket.connect(addr)
    println("connected")

    val queue = Channel<Byte>()

    GlobalScope.launch(readDispatcher) {
        val buffer = ByteBuffer.allocate(2048)

        var index = 0
        while (true) {
            buffer.position(0)

            val count = socket.read(buffer)
            if (count < 2048) {
                println("read failure: $count < 2048")
                break
            }

            when (buffer[0]) {
                0xaa.toByte() -> { println("got ping"); queue.send(0xaa.toByte()) }
                0xbb.toByte() -> println("got pong")
                else -> println("got message")
            }

            if (++index == 4) {
                byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0xaa.toByte(), 0x8)
                    .forEach { queue.send(it) }
            }
        }
    }

    return GlobalScope.async(writeDispatcher) {
        while (true) {
            val byte = queue.receive()
            val arr = (0 until 2048).map { byte }.toByteArray()

            if (byte == 0xaa.toByte())
                println("responding pong")

            val count = socket.write(ByteBuffer.wrap(arr))
            if (count < 2048) {
                println("write failure : $count < 2048")
                break
            }
            delay(500)
        }
    }

}

fun main() {
    val read = newFixedThreadPoolContext(1, "read-thread")
    val write = newFixedThreadPoolContext(1, "write-thread")
    runBlocking {
        (0 until 10).map { createAsync(read, read, it) }
            .awaitAll()
    }
}