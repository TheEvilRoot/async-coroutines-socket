package com.theevilroot.asyncsocket

import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

class SocksCoroutineSocket(
    val socksIsa: InetSocketAddress,
    channel: AsynchronousSocketChannel,
    val credentials: Pair<String, String>? = null
) : CoroutineSocket(channel) {

    class SocksException(message: String) : IOException(message)

    enum class Method { NO_AUTH, USER_PASS }

    enum class AddressType(val id: Byte) {
        IPv4(0x01.toByte()),
        DOMAIN(0x03.toByte()),
        IPv6(0x04.toByte());

        private suspend fun sizeOfAddress(read: suspend (ByteBuffer) -> Int): Int = when (this) {
            IPv4 -> 0x04
            IPv6 -> 0x10
            DOMAIN -> ByteBuffer.allocate(1).let {
                val count = read(it)
                if (count < 1)
                    throw SocksException("failed to read domain name length for address. $count < 1")
                it[0].toUByte().toInt()
            }
        }

        suspend fun consumeWithPort(read: suspend (ByteBuffer) -> Int): ByteBuffer =
            sizeOfAddress(read).let {
                val buffer = ByteBuffer.allocate(it + 2)
                val count = read(buffer)
                if (count < it + 2)
                    throw SocksException("failed to read address. $count < ${it + 2}")
                buffer
            }
    }

    private suspend fun methodNegotiate(): Method {
        val methods = if (credentials == null) {
            byteArrayOf(0x00)
        } else {
            byteArrayOf(0x02)
        }
        val message = byteArrayOf(0x05, methods.size.toByte(), *methods)
        ByteBuffer.wrap(message).let {
            val count = super.write(it)
            if (count < message.size)
                throw SocksException("failed to write methods message. $count < ${message.size}")
        }

        val method = ByteBuffer.allocate(2).let {
            val count = super.read(it)
            if (count < 2)
                throw SocksException("failed to read methods response. $count < 2")
            if (it[0] != 0x05.toByte())
                throw SocksException("methods response: versions mismatch: ${it[0]}")
            it[1]
        }
        return when (method) {
            0x00.toByte() -> Method.NO_AUTH
            0x02.toByte() -> Method.USER_PASS
            else -> throw SocksException(String.format("method %02x is not supported", method))
        }
    }

    private suspend fun socksConnect(isa: InetSocketAddress): InetSocketAddress {
        if (!this::method.isInitialized)
            throw IllegalStateException("socks auth method is not initialized. maybe you forget to call init()?")
        val message = byteArrayOf(0x05, 0x01, 0x00, 0x01,
            *isa.address.address,
            (isa.port shr 8).toByte(),
            (isa.port and 0xff).toByte())
        ByteBuffer.wrap(message).let {
            val count = super.write(it)
            if (count < message.size)
                throw SocksException("failed to write connect message. $count < ${message.size}")
        }
        val header = ByteBuffer.allocate(4).also {
            val count = super.read(it)
            if (count < 4)
                throw SocksException("failed to read header of connect response. $count < 4")
            if (it[1] != 0x00.toByte())
                throw ConnectException("failed to connect to ${isa.address.canonicalHostName}:${isa.port}. socks server respond ${it[1]} code")
        }
        val addrType = AddressType.values().firstOrNull { it.id == header[3] }
            ?: throw SocksException("unknown address type ${header[3]}")
        val addressWithPort = addrType.consumeWithPort { buff -> super.read(buff) }
        val addr = ByteArray(4)
        val bPort = ByteArray(2)
        addressWithPort.position(0)
        addressWithPort.get(addr, 0, 4)
        addressWithPort.get(bPort, 0, 2)
        val port = (bPort[0].toUByte().toInt() shl 8) + bPort[1].toUByte().toInt()
        return InetSocketAddress(InetAddress.getByAddress(addr), port)
    }

    private suspend fun socksUserPassNegotiations() {
        if (credentials == null)
            throw SocksException("server want to use user/pass authentication, but no credentials provided")
        val (username, password) = credentials
        if (username.length > 255 || password.length > 255)
            throw SocksException("credentials is invalid. max length of username and password is 255")
        val message = byteArrayOf(0x01, username.length.toByte(), *username.toByteArray(),
            password.length.toByte(), *password.toByteArray())
        ByteBuffer.wrap(message).also {
            val count = super.write(it)
            if (count < message.size)
                throw SocksException("failed to write user/pass message. $count < ${message.size}")
        }
        val response = ByteBuffer.allocate(2).let {
            val count = super.read(it)
            if (count < 2)
                throw SocksException("failed to read user/pass response. $count < 2")
            it[1]
        }
        if (response != 0x00.toByte())
            throw SocksException("authentication failure. server respond $response != 0x00")
    }

    lateinit var method: Method
    lateinit var remoteIsa: InetSocketAddress

    suspend fun init() {
        // First of all, we should just connect to socks server as usual
        // com.theevilroot.asyncsocket.SocksCoroutineSocket.init will used as com.theevilroot.asyncsocket.CoroutineSocket.connect
        // but com.theevilroot.asyncsocket.SocksCoroutineSocket.connect will be used to send CONNECT
        // command to socks server
        // So, connect should only be called after the init
        // And init is using super.connect(ISA) to use parent's connect
        // function that actually perform connect syscall
        //
        // I wanted to but just can't use kotlin's init { } to do that because
        // super.connect(ISA) is suspend function and can be called only
        // in coroutine context. kotlin initialize function cannot be
        // suspend as much as constructor { } can't
        super.connect(socksIsa)
        try {
            method = methodNegotiate()
            when (method) {
                Method.USER_PASS -> socksUserPassNegotiations()
                else -> { }
            }
        } catch (s: SocksException) {
            close()
            throw s
        }
    }

    override suspend fun connect(isa: InetSocketAddress) {
        try {
            remoteIsa = socksConnect(isa)
        } catch (s: SocksException) {
            close()
            throw s
        }
    }

    override suspend fun read(buffer: ByteBuffer): Int {
        if (!this::remoteIsa.isInitialized)
            throw IllegalStateException("remote address is not initialized. please, connect first")
        return super.read(buffer).also {
            if (it < 0) close()
        }
    }

    override suspend fun write(buffer: ByteBuffer): Int {
        if (!this::remoteIsa.isInitialized)
            throw IllegalStateException("remote address is not initialized. please, connect first")
        return super.write(buffer).also {
            if (it < 0) close()
        }
    }
}