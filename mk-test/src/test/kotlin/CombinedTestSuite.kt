import org.junit.runner.RunWith
import org.junit.runners.Suite
import unit.*

@RunWith(Suite::class)
@Suite.SuiteClasses(
    BloomFilterSpec::class,
    NoOpBlockCacheSpec::class,
    MkSplitSpec::class,
    RpcTransportSpec::class,
    BitcoindTransportSpec::class,
    BitcoindTransferSpec::class,
    ParityTransportSpec::class,
    ParityTransferSpec::class
) class CombinedTestSuite
