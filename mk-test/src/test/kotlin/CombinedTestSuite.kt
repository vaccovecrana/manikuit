import org.junit.runner.RunWith
import org.junit.runners.Suite
import unit.*

@RunWith(Suite::class)
@Suite.SuiteClasses(
    MkSplitSpec::class,
    PersistenceSpec::class,
    RpcTransportSpec::class,
    BitcoindTransportSpec::class,
    ParityTransportSpec::class
) class CombinedTestSuite
