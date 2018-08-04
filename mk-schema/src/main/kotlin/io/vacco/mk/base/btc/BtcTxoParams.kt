package io.vacco.mk.base.btc

data class BtcTxoParams(val txid: String, val vout: Long,
                        val scriptPubKey: String, val amount: String,
                        var redeemScript: String?)
