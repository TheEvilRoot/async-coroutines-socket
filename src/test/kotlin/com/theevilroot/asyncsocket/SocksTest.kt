package com.theevilroot.asyncsocket

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
    fun testNoAuth(): Unit = runBlocking {
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1081), channel)
        socket.init()
        socket.connect(InetSocketAddress("example.com", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.NO_AUTH, socket.method)

        socket.close()
    }

    @Test
    fun testUserPassAuthCorrect(): Unit = runBlocking {
        val credentials = "username" to "password"
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1080), channel, credentials)
        socket.init()
        socket.connect(InetSocketAddress("example.com", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.USER_PASS, socket.method)

        socket.close()
    }

    @Test
    fun testUserPassAuthIncorrect(): Unit = runBlocking {
        val credentials = "username" to "blah"
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1080), channel, credentials)
        Assert.assertThrows(SocksException::class.java) {
            runBlocking { socket.init() }
        }
    }

    @Test
    fun testUserPassAuthWithoutCredentials(): Unit = runBlocking {
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1080), channel)
        socket.init()
        socket.connect(InetSocketAddress("example.com", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.NO_AUTH, socket.method)

        socket.close()
    }

    @Test
    fun testCredentialsWithNoAuthServer(): Unit = runBlocking {
        val credentials = "username" to "blah"
        val socket = SocksCoroutineSocket(InetSocketAddress("localhost", 1081), channel, credentials)
        socket.init()
        socket.connect(InetSocketAddress("example.com", 80))

        Assert.assertTrue(socket.isOpened)
        Assert.assertTrue(socket.isConnected)

        Assert.assertEquals(SocksCoroutineSocket.Method.NO_AUTH, socket.method)

        socket.close()
    }
}