package com.theevilroot.asyncsocket

import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit

class Socks4CoroutineSocket(
    val socksIsa: InetSocketAddress,
    channel: AsynchronousSocketChannel,
    val userId: String,
    readTimeout: Pair<Long, TimeUnit>? = null
) : CoroutineSocket(channel, readTimeout) {

    lateinit var remoteIsa: InetSocketAddress

    private suspend fun socksConnect(isa: InetSocketAddress): InetSocketAddress {
        val message = byteArrayOf(
            0x04,
            0x01,
            (isa.port shr 8).toByte(),
            (isa.port and 0xff).toByte(),
            *isa.address.address,
            *userId.toByteArray(),
            0x00.toByte(),
        )
        ByteBuffer.wrap(message).let {
            val count = super.write(it)
            if (count < message.size) {
                throw SocksException("failed to write connect message. $count < ${message.size}")
            }
        }
        val response = ByteBuffer.allocate(8).also {
            val count = super.read(it)
            if (count < 8) {
                throw SocksException("failed to read connect response. $count < 8")
            }
        }
        if (response[1] != 90.toByte()) {
            throw SocksException(
                "failed to connect to remote server through proxy. socks error: ${String.format("%02x", response[1])}",
            )
        }

        val addr = ByteArray(4)
        val bPort = ByteArray(2)
        response.position(2)
        response.get(bPort, 0, 2)
        response.get(addr, 0, 4)
        val port = (bPort[0].toUByte().toInt() shl 8) + bPort[1].toUByte().toInt()
        return InetSocketAddress(InetAddress.getByAddress(addr), port)
    }

    suspend fun init() {
        super.connect(socksIsa)
    }

    override suspend fun connect(isa: InetSocketAddress) {
        try {
            remoteIsa = socksConnect(isa)
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    override suspend fun read(buffer: ByteBuffer): Int {
        if (!this::remoteIsa.isInitialized) {
            throw IllegalStateException("remote address is not initialized. please, connect first")
        }
        return super.read(buffer).also {
            if (it < 0) close()
        }
    }

    override suspend fun write(buffer: ByteBuffer): Int {
        if (!this::remoteIsa.isInitialized) {
            throw IllegalStateException("remote address is not initialized. please, connect first")
        }
        return super.write(buffer).also {
            if (it < 0) close()
        }
    }
}
