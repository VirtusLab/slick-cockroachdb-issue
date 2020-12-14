import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HikariJdbcIntegrationTest extends AnyWordSpec with Matchers with CockroachBackedSpec with RawJdbcTestOps {

  "cockroach via hikariCP-pooled postgres jdbc driver" should {
    lazy val ds = {
      val config = new HikariConfig()
      config.setDriverClassName("org.postgresql.Driver")
      config.setUsername(container.username)
      config.setPassword(container.password)
      config.setJdbcUrl(container.jdbcUrl)
      config.setInitializationFailTimeout(-1L)

      new HikariDataSource(config)
    }

    "handle queries that crash in slick" in {
      val conn = ds.getConnection
      conn.setAutoCommit(false)

      try {
        rawAddAndPersist(conn, 2.3)
        rawAddAndPersist(conn, 3.2)

        println(s"Current sum is ${fetchSum(conn)}")

        conn.commit()
      } finally {
        conn.close()
      }
    }
  }

}
