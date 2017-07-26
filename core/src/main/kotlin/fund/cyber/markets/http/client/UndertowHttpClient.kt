package fund.cyber.markets.http.client

import fund.cyber.markets.byteBuffersPool
import fund.cyber.markets.xnioSsl
import fund.cyber.markets.xnioWorker
import io.undertow.client.ClientCallback
import io.undertow.client.ClientConnection
import io.undertow.client.ClientExchange
import io.undertow.client.ClientRequest
import io.undertow.client.UndertowClient
import io.undertow.util.AttachmentKey
import io.undertow.util.Headers
import io.undertow.util.Methods
import io.undertow.util.StringReadChannelListener
import org.xnio.ChannelListeners
import org.xnio.OptionMap
import java.io.IOException
import java.net.URI


private val RESPONSE_BODY = AttachmentKey.create(String::class.java)

fun main(args: Array<String>) {
    UndertowHttpClient(UndertowClient.getInstance()).get(
        "https://poloniex.com/public?command=returnTicker"
    )
}

/**
 * Wrapper around default undertow http client.
 *
 * @author Ibragimov Ruslan
 */
class UndertowHttpClient(
    private val httpClient: UndertowClient
) {
    fun get(address: String) {
        val uri = URI(address)

        val connection = httpClient.connect(
            uri,
            xnioWorker,
            xnioSsl,
            byteBuffersPool,
            OptionMap.EMPTY
        ).get()

        connection.ioThread.execute {
            val request = ClientRequest()
            request.method = Methods.GET
            request.path = "${uri.path}?${uri.query}"
            request.requestHeaders.put(Headers.HOST, uri.host)
            connection.sendRequest(request, createClientCallback(connection))
        }

        connection.ioThread.execute {
            val request = ClientRequest()
            request.method = Methods.GET
            request.path = "${uri.path}?${uri.query}"
            request.requestHeaders.put(Headers.HOST, uri.host)
            connection.sendRequest(request, createClientCallback(connection))
        }
    }
}

private fun createClientCallback(connection: ClientConnection): ClientCallback<ClientExchange> {
    return object : ClientCallback<ClientExchange> {
        override fun completed(result: ClientExchange) {
            result.setResponseListener(object : ClientCallback<ClientExchange> {
                override fun completed(clientExchange: ClientExchange) {
                    object : StringReadChannelListener(clientExchange.connection.bufferPool) {

                        override fun stringDone(string: String) {
                            println(string)
                            clientExchange.response.putAttachment(RESPONSE_BODY, string)
                        }

                        override fun error(e: IOException) {
                            e.printStackTrace()
                        }
                    }.setup(result.responseChannel)
                }

                override fun failed(e: IOException) {
                    e.printStackTrace()
                }
            })

            try {
                result.requestChannel.shutdownWrites()
                if (!result.requestChannel.flush()) {
                    result.requestChannel.writeSetter.set(ChannelListeners.flushingChannelListener(null, null))
                    result.requestChannel.resumeWrites()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

//            IoUtils.safeClose(connection)
        }

        override fun failed(e: IOException) {
            e.printStackTrace()
//            IoUtils.safeClose(connection)
        }
    }
}

