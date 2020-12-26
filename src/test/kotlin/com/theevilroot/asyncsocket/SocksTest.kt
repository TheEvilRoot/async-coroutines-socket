import com.theevilroot.asyncsocket.SocksCoroutineSocket
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel

class SocksTest {

    lateinit var channel: AsynchronousSocketChannel

    @Before
    fun initChannel() {
        channel = AsynchronousSocketChannel.open()
    }

    @Test
    suspend fun testNoAuth() {
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1081), channel)
        socket.init()
        socket.connect(InetSocketAddress("52.48.142.75", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.NO_AUTH, socket.method)

        socket.close()
    }

    @Test
    suspend fun testUserPassAuthCorrect() {
        val credentials = "username" to "password"
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1080), channel, credentials)
        socket.init()
        socket.connect(InetSocketAddress("52.48.142.75", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.USER_PASS, socket.method)

        socket.close()
    }


    @Test
    suspend fun testUserPassAuthIncorrect() {
        val credentials = "blah" to "password"
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1080), channel, credentials)
        Assert.assertThrows(SocksCoroutineSocket.SocksException::class.java) {
            runBlocking { socket.init() }
        }
    }

    @Test
    suspend fun testUserPassAuthWithoutCredentials() {
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1080), channel)
        Assert.assertThrows(SocksCoroutineSocket.SocksException::class.java) {
            runBlocking { socket.init() }
        }
    }

    @Test
    suspend fun testCredentialsWithNoAuthServer() {
        val credentials = "blah" to "password"
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1081), channel, credentials)
        socket.init()
        socket.connect(InetSocketAddress("52.48.142.75", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.NO_AUTH, socket.method)

        socket.close()
    }
}