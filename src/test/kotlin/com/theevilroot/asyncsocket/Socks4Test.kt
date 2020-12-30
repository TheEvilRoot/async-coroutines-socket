package com.theevilroot.asyncsocket

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

class Socks4Test {

    lateinit var channel: AsynchronousSocketChannel

    @Before
    fun initChannel() {
        channel = AsynchronousSocketChannel.open()
    }

    @Test
    fun testSocks4Connect(): Unit = runBlocking {
        val socket = Socks4CoroutineSocket(InetSocketAddress("127.0.0.1", 1082), channel, "user")
        socket.init()
        socket.connect(InetSocketAddress("52.48.142.75", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        val request = "GET /?format=json HTTP/1.1\r\nConnection: close\r\nHost: api.ipify.org\r\nAccept: */*\r\nUser-Agent: curl/1.1.1\r\n\r\n"
        Assert.assertEquals(request.length, socket.write(ByteBuffer.wrap(request.toByteArray())))

        // read some data, we don't need much
        val response = ByteBuffer.allocate(32)
        Assert.assertTrue(socket.read(response) > 0)

        val http = ByteArray(4)
        response.position(0)
        response.get(http, 0, 4)
        Assert.assertTrue(http.contentEquals("HTTP".toByteArray()))

        socket.close()
    }
}