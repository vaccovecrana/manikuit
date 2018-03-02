package io.vacco.mk.rpc

import io.vacco.mk.base.MkBlock
import io.vacco.mk.base.MkPaymentRecord

typealias CgBlockSummary = Pair<MkBlock, List<String>>
typealias CgBlockDetail = Pair<MkBlock, List<MkPaymentRecord>>
