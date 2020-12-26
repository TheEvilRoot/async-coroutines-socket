package com.theevilroot.asyncsocket

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class CoroutineSocket(private val socket: AsynchronousSocketChannel) {

    var isConnected: Boolean = false
        private set
    val isOpened: Boolean
        get() = socket.isOpen

    open suspend fun connect(isa: InetSocketAddress) {
        suspendCoroutine<Void> {
            socket.connect(isa, it, ContinuationHandler<Void>())
        }
        isConnected = true
    }

    open suspend fun read(buffer: ByteBuffer): Int {
        return suspendCoroutine {
            socket.read(buffer, it, ContinuationHandler<Int>())
        }
    }

    open suspend fun write(buffer: ByteBuffer): Int {
        return suspendCoroutine {
            socket.write(buffer, it, ContinuationHandler<Int>())
        }
    }

    open suspend fun close() {
        isConnected = false
        socket.shutdownInput()
        socket.shutdownOutput()
        socket.close()
    }

    class ContinuationHandler<T> : CompletionHandler<T, Continuation<T>> {
        override fun completed(result: T, attachment: Continuation<T>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<T>) {
            attachment.resumeWithException(exc)
        }
    }
}