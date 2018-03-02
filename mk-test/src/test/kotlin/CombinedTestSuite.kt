import org.junit.runner.RunWith
import org.junit.runners.Suite
import unit.*

@RunWith(Suite::class)
@Suite.SuiteClasses(
    PersistenceSpec::class,
    SecretUtilsSpec::class,
    RpcTransportSpec::class,
    BitcoindTransportSpec::class,
    GethTransportSpec::class
) class CombinedTestSuite
