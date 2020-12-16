import java.sql.Connection

trait RawJdbcTestOps {

  val id = "testing"

  def rawAddAndPersist(conn: Connection, add: Double): Unit = {
    val queryStmt = conn.prepareStatement("select \"id\", \"current_sum\" from \"sums\" where \"id\" = ? for update")
    queryStmt.setString(1, id)

    // this causes the issue with CockroachDB - it opens an anonymous portal
    queryStmt.setMaxRows(1)
    // this allows for explicit portal management, driver won't issue portal closing commands without it
    // comment it out to see the test fail
    queryStmt.setFetchSize(1)

    var currentSum: Double = 0d
    val queryRs            = queryStmt.executeQuery()
    while (queryRs.next()) {
      currentSum = queryRs.getDouble(2)
    }

    // and this closes the portal, but only when fetchSize is also set (explicit ResultSet.close is missing in Slick)
    // comment it out to see the test fail
    queryRs.close()

    // this is done by Slick too
    queryStmt.close()

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
