import java.sql.Connection

trait RawJdbcTestOps {

  val id = "testing"

  def rawAddAndPersist(conn: Connection, add: Double): Unit = {
    val queryStmt = conn.prepareStatement("SELECT id, current_sum FROM sums WHERE id = ?")
    queryStmt.setString(1, id)

    var currentSum: Double = 0d
    val queryRs            = queryStmt.executeQuery()
    while (queryRs.next()) {
      currentSum = queryRs.getDouble(2)
    }

    val updateStmt = conn.prepareStatement(
      "update \"sums\" set \"current_sum\"=? where \"id\"=?; insert into \"sums\" (\"id\",\"current_sum\") select ?,? where not exists (select 1 from \"sums\" where \"id\"=?)"
    )
    updateStmt.setDouble(1, currentSum + add)
    updateStmt.setString(2, id)
    updateStmt.setString(3, id)
    updateStmt.setDouble(4, currentSum + add)
    updateStmt.setString(5, id)

    updateStmt.executeUpdate()

    ()
  }

  def fetchSum(conn: Connection): Double = {
    val queryStmt = conn.prepareStatement("SELECT current_sum FROM sums WHERE id = ?")
    queryStmt.setString(1, id)

    var currentSum: Double = 0d
    val queryRs            = queryStmt.executeQuery()
    while (queryRs.next()) {
      currentSum = queryRs.getDouble(1)
    }

    currentSum
  }

}
