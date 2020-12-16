import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class SlickIntegrationTest extends AnyWordSpec with Matchers with CockroachBackedSpec {

  "cockroachdb integration for slick" should {
    lazy val config = ConfigFactory
      .parseString {
        s"""slick = {
           |  profile = "slick.jdbc.PostgresProfile$$"
           |  db {
           |    url      = "${container.jdbcUrl}"
           |    user     = "${container.username}"
           |    password = "${container.password}"
           |    maxConnections = 1
           |    minConnections = 1
           |    numThreads = 1
           |    maxThreads = 1
           |  }
           |  numThreads = 1
           |  maxThreads = 1
           |}
           |""".stripMargin
      }

    "handle implicit transactions (no .transactionally)" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      try {
        Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-0", 5.0)), 5.seconds)
        Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-0", 5.0)), 5.seconds)

        val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-0")), 5.seconds)

        sum shouldEqual Some(10.0)
      } finally {
        pgDbConfig.db.close()
      }
    }

    "handle single transaction" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      try {
        import pgDbConfig.profile.api._

        Await.result(
          pgDbConfig.db.run(repo.addAndPersist("testing-1", 5.0).transactionally),
          5.seconds
        )

        val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-1")), 5.seconds)

        sum shouldEqual Some(5.0)
      } finally {
        pgDbConfig.db.close()
      }
    }

    "handle multiple separate transactions" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      try {
        import pgDbConfig.profile.api._

        Await.result(
          pgDbConfig.db.run(repo.addAndPersist("testing-2", 5.0).transactionally),
          5.seconds
        )
        Await.result(
          pgDbConfig.db.run(repo.addAndPersist("testing-2", 5.0).transactionally),
          5.seconds
        )

        val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-2")), 5.seconds)

        sum shouldEqual Some(10.0)
      } finally {
        pgDbConfig.db.close()
      }
    }

    "handle separate transactions mixed - implicit and declared" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      try {
        import pgDbConfig.profile.api._

        Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-3", 5.0)), 5.seconds)
        Await.result(
          pgDbConfig.db.run(repo.addAndPersist("testing-3", 5.0).transactionally),
          5.seconds
        )

        val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-3")), 5.seconds)

        sum shouldEqual Some(10.0)
      } finally {
        pgDbConfig.db.close()
      }
    }

    "handle separate transactions mixed - declared and implicit" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      try {
        import pgDbConfig.profile.api._

        Await.result(
          pgDbConfig.db.run(repo.addAndPersist("testing-4", 5.0).transactionally),
          5.seconds
        )
        Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-4", 5.0)), 5.seconds)

        val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-4")), 5.seconds)

        sum shouldEqual Some(10.0)
      } finally {
        pgDbConfig.db.close()
      }
    }

    "handle multiple statements in one transaction" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      try {
        import pgDbConfig.profile.api._

        val fut = pgDbConfig.db.run {
          (for {
            _ <- repo.addAndPersist("testing-5", 2.3)
            _ <- repo.addAndPersist("testing-5", 3.2)
          } yield ()).transactionally
        }
        Await.result(fut, 5.seconds)

        val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-5")), 5.seconds)

        sum shouldEqual Some(5.5)
      } finally {
        pgDbConfig.db.close()
      }
    }

  }

}
