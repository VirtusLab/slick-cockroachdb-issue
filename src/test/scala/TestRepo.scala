import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext

case class CurrentSum(id: String, currentSum: Double)

class TestRepo private ()(implicit ec: ExecutionContext) {

  import PostgresProfile.api._

  private class Sums(tag: Tag) extends Table[CurrentSum](tag, "sums") {
    def id         = column[String]("id", O.PrimaryKey)
    def currentSum = column[Double]("current_sum")
    def *          = (id, currentSum) <> (CurrentSum.tupled, CurrentSum.unapply)
  }

  private val currentSums = TableQuery[Sums]

  private val findById = currentSums.findBy(_.id)

  def addAndPersist(id: String, number: Double): DBIO[Unit] =
    for {
      maybeExisting <- findById(id).map(_.forUpdate).result.headOption
      row = maybeExisting.map(cs => cs.copy(currentSum = cs.currentSum + number)).getOrElse(CurrentSum(id, number))
      _ <- currentSums.insertOrUpdate(row)
    } yield ()

  def fetch(id: String): DBIO[Option[Double]] = findById(id).map(q => q.map(_.currentSum)).result.headOption
}

object TestRepo {
  def apply(ec: ExecutionContext): TestRepo = new TestRepo()(ec)
}
