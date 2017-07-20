package fund.cyber.markets.model

import java.time.Instant

open class Event

class ExchangeMetadataInitializedEvent<out M : ExchangeMetadata>(
        val metadata: M
) : Event()


class ConnectionWithExchangeIsEstablished(
        val exchange: String,
        val time: Long = Instant.now().epochSecond
) : Event()

class ConnectionWithExchangeIsLost(
        val exchange: String,
        val time: Long = Instant.now().epochSecond
) : Event()

class ConnectionWithExchangeIsReestablished(
        val exchange: String,
        val time: Long = Instant.now().epochSecond
) : Event()