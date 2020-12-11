import com.dimafeng.testcontainers.{ CockroachContainer, ForAllTestContainer }
import org.flywaydb.core.Flyway
import org.scalatest.Suite
import org.testcontainers.utility.DockerImageName

trait CockroachBackedSpec extends ForAllTestContainer { self: Suite =>

  override val container: CockroachContainer =
    CockroachContainer(
      dockerImageName = DockerImageName.parse("cockroachdb/cockroach:v20.1.0"),
      urlParams = Map("reWriteBatchedInserts" -> "true")
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
