lazy val SlickVersion          = "3.3.2"
lazy val LogbackClassicVersion = "1.2.3"
lazy val TestContainersVersion = "0.38.7"
lazy val ScalatestVersion      = "3.2.3"
lazy val PostgresDriverVersion = "42.2.18"
lazy val FlywayVersion         = "6.3.3"

name := "slick-cockroachdb-issue"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick"                            % SlickVersion,
  "com.typesafe.slick" %% "slick-hikaricp"                   % SlickVersion,
  "org.postgresql"      % "postgresql"                       % PostgresDriverVersion,
  "ch.qos.logback"      % "logback-classic"                  % LogbackClassicVersion,
  "org.flywaydb"        % "flyway-core"                      % FlywayVersion         % Test,
  "org.scalatest"      %% "scalatest"                        % ScalatestVersion      % Test,
  "com.dimafeng"       %% "testcontainers-scala-scalatest"   % TestContainersVersion % Test,
  "com.dimafeng"       %% "testcontainers-scala-cockroachdb" % TestContainersVersion % Test
)

Test / fork := true
