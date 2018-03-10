package io.vacco.mk.rpc

import io.vacco.mk.base.MkBlock
import io.vacco.mk.base.MkPaymentRecord

typealias MkBlockSummary = Pair<MkBlock, List<String>>
typealias MkBlockDetail = Pair<MkBlock, List<MkPaymentRecord>>
