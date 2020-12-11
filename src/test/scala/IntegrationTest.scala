import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class IntegrationTest extends AnyWordSpec with Matchers with CockroachBackedSpec {

  "cockroachdb integration for slick" should {
    lazy val config = ConfigFactory
      .parseString {
        s"""slick = {
           |  profile = "slick.jdbc.PostgresProfile$$"
           |  db {
           |    url      = "${container.jdbcUrl}"
           |    user     = "${container.username}"
           |    password = "${container.password}"
           |  }
           |}
           |""".stripMargin
      }

    "handle implicit transactions (no .transactionally)" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-0", 5.0)), 5.seconds)
      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-0", 5.0)), 5.seconds)

      val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-0")), 5.seconds)

      sum shouldEqual Some(10.0)
    }

    "handle single transaction" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      import pgDbConfig.profile.api._

      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-1", 5.0).transactionally), 5.seconds)

      val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-1")), 5.seconds)

      sum shouldEqual Some(5.0)
    }

    "handle multiple separate transactions" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      import pgDbConfig.profile.api._

      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-2", 5.0).transactionally), 5.seconds)
      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-2", 5.0).transactionally), 5.seconds)

      val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-2")), 5.seconds)

      sum shouldEqual Some(10.0)
    }

    "handle separate transactions mixed - implicit and declared" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      import pgDbConfig.profile.api._

      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-3", 5.0)), 5.seconds)
      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-3", 5.0).transactionally), 5.seconds)

      val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-3")), 5.seconds)

      sum shouldEqual Some(10.0)
    }

    "handle separate transactions mixed - declared and implicit" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      import pgDbConfig.profile.api._

      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-4", 5.0).transactionally), 5.seconds)
      Await.result(pgDbConfig.db.run(repo.addAndPersist("testing-4", 5.0)), 5.seconds)

      val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-4")), 5.seconds)

      sum shouldEqual Some(10.0)
    }

    "handle multiple statements in one transaction" in {
      val pgDbConfig = DatabaseConfig.forConfig[PostgresProfile]("slick", config)
      val repo       = TestRepo(global)

      import pgDbConfig.profile.api._

      val fut = pgDbConfig.db.run {
        (for {
          _ <- repo.addAndPersist("testing-5", 5.0)
          _ <- repo.addAndPersist("testing-5", 5.0)
        } yield ()).transactionally
      }
      Await.result(fut, 5.seconds)

      val sum = Await.result(pgDbConfig.db.run(repo.fetch("testing-5")), 5.seconds)

      sum shouldEqual Some(10.0)
    }

  }

}
