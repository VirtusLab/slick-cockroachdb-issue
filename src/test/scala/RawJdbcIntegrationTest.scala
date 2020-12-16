import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.{ Connection, DriverManager }

class RawJdbcIntegrationTest extends AnyWordSpec with Matchers with CockroachBackedSpec with RawJdbcTestOps {

  "cockroach via raw postgres jdbc driver" should {
    lazy val url = s"${container.jdbcUrl}&user=${container.username}&password=${container.password}"
    Class.forName("org.postgresql.Driver")
    def getConnection: Connection = DriverManager.getConnection(url)

    "handle queries that crash in slick" in {
      val conn = getConnection
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
