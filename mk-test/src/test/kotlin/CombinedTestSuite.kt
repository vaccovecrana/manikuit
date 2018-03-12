import org.junit.runner.RunWith
import org.junit.runners.Suite
import unit.*

@RunWith(Suite::class)
@Suite.SuiteClasses(
    PersistenceSpec::class,
    RpcTransportSpec::class,
    BitcoindTransportSpec::class,
    BitcoindIpcSpec::class,
    ParityTransportSpec::class,
    ParityIpcSpec::class
) class CombinedTestSuite
