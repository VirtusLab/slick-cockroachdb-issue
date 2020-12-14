import com.dimafeng.testcontainers.{CockroachContainer, ForAllTestContainer}
import org.flywaydb.core.Flyway
import org.scalatest.Suite
import org.testcontainers.utility.DockerImageName

trait CockroachBackedSpec extends ForAllTestContainer { self: Suite =>
  // Ugly but works ;)
  val format:  java.lang.String = "%4$s %2$s%n %5$s%6$s%n"
  System.setProperty("java.util.logging.SimpleFormatter.format", format)

  override val container: CockroachContainer =
    CockroachContainer(
      dockerImageName = DockerImageName.parse("cockroachdb/cockroach:v20.1.0"),
      urlParams = Map(
        "reWriteBatchedInserts" -> "true",
        "sslmode" -> "disable",
        "loggerFile" -> s"pgjdbc-${self.suiteName}.log"
      )
    )

  override def afterStart(): Unit = {
    super.afterStart()
    Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .locations("classpath:testdb/migration")
      .load()
      .migrate()
  }

}
