import com.theevilroot.asyncsocket.CoroutineSocket
import com.theevilroot.asyncsocket.SocksCoroutineSocket
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun ByteArray.hexlify(): String =
    joinToString(" ") { String.format("%02x", it) }

val req = "GET /?format=json HTTP/1.1\r\nConnection: close\r\nHost: api.ipify.org\r\nAccept: */*\r\nUser-Agent: curl/1.1.1\r\n\r\n"
    .toByteArray()

suspend fun runSocksHttpRequestTest(channel: AsynchronousSocketChannel) {
    println("opening socks5 socket...")
    val socket = SocksCoroutineSocket(InetSocketAddress(InetAddress.getByName("192.168.100.58"), 1080), channel)
    println("client created")
    socket.init()
    println("connecting...")
    socket.connect(InetSocketAddress("api.ipify.org", 80))
    println("connected...")
    println("writing...")
    ByteBuffer.wrap(req).let {
        socket.write(it)
    }
    println("wrote")
    val builder = StringBuilder()
    val buffer = ByteBuffer.allocate(4)
    while (true) {
        println("reading next 4 bytes...")
        val count = socket.read(buffer)
        println("read finished")
        if (count <= 0)
            break
        builder.append(buffer.array().copyOf(count).joinToString("") { it.toChar().toString() })
        buffer.clear()
    }
    println(builder.toString())
    socket.close()
}


suspend fun runRawHttpRequestTest(channel: AsynchronousSocketChannel) {
    println("opening raw socket...")
    val raw = CoroutineSocket(channel)
    raw.connect(InetSocketAddress("api.ipify.org", 80))
    ByteBuffer.wrap(req).let { raw.write(it) }

    val builder = StringBuilder()
    val buffer = ByteBuffer.allocate(4)
    while (true) {
        println("reading next 4 bytes...")
        val count = raw.read(buffer)
        println("read finished")
        if (count <= 0)
            break
        builder.append(buffer.array().copyOf(count).joinToString("") { it.toChar().toString() })
        buffer.clear()
    }
    println(builder.toString())
    raw.close()
}

suspend fun runCoroutineSocketTest(channel: AsynchronousSocketChannel, dispatcher: CoroutineDispatcher) = withContext(dispatcher) {
    val socket = CoroutineSocket(channel)

    launch {
        socket.connect(InetSocketAddress("localhost", 9999))

        val buffer = ByteBuffer.allocate(16)
        while (true) {
            println("${Thread.currentThread().name}: reading...")
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
            println("${Thread.currentThread().name}: ping?!")
            delay(500)
            println("pong!!")
            delay(500)
        }
    }
}


class TestCase(
    val name: String,
    val group: AsynchronousChannelGroup,
    val f: suspend (AsynchronousChannelGroup) -> Unit
) {

    sealed class Result {
        class Success(val case: TestCase): Result()
        object Failure: Result()
    }

    suspend operator fun invoke(): Result {
        return runCatching { f(group); Result.Success(this) }.recover {
            println("$name test failure!")
            println("${it.javaClass.simpleName}: ${it.localizedMessage}")
            Result.Failure
        }.getOrDefault(Result.Failure)
    }
}

suspend infix fun TestCase.Result.dot(testCase: TestCase): TestCase.Result =
    when (this) {
        is TestCase.Result.Failure -> TestCase.Result.Failure
        is TestCase.Result.Success -> testCase()
    }

fun main() {
    val thread = Executors.newFixedThreadPool(1)
    val group = AsynchronousChannelGroup.withThreadPool(thread)
    runBlocking {

        val socksHttpCase = TestCase("socks http request", group) {
            runSocksHttpRequestTest(AsynchronousSocketChannel.open(it))
        }
        val rawHttpCase = TestCase("raw http request", group) {
            runRawHttpRequestTest(AsynchronousSocketChannel.open(it))
        }
        val clientServerCase = TestCase("client-server", group) {
            runCoroutineSocketTest(AsynchronousSocketChannel.open(it), thread.asCoroutineDispatcher())
        }

        socksHttpCase()dot rawHttpCase dot clientServerCase
    }
    group.shutdownNow()
    group.awaitTermination(-1, TimeUnit.SECONDS)
}