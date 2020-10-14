import java.io.Closeable
import scala.io.Source

import io.github.davidmweber.FlywayPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.MergeStrategy

object Common {

  // Dependency versions
  private val circeVersion      = "0.13.0"
  private val doobieVersion     = "0.9.0"
  private val flywayVersion     = "6.4.2"
  private val h2Version         = "1.4.200"
  private val http4sVersion     = "0.21.4"
  private val logbackVersion    = "1.2.3"
  private val oauthJwtVersion   = "3.10.3"
  private val postgresVersion   = "42.2.12"
  private val pureConfigVersion = "0.12.3"
  private val specs2Version     = "4.9.4"

  // Compiler plugin dependency versions
  private val kindProjectorVersion    = "0.11.0"
  private val betterMonadicForVersion = "0.3.1"

  final val settings: Seq[Setting[_]] =
    projectSettings ++ dependencySettings ++ flywaySettings ++ compilerPlugins ++ assemblySettings

  private[this] def projectSettings = Seq(
    organization := "com.hhandoko",
    name := "realworld",
    version := using(Source.fromFile("VERSION.txt")) { _.mkString },
    scalaVersion := "2.13.3"
  )

  private[this] def dependencySettings = Seq(
    libraryDependencies ++= Seq(
      "ch.qos.logback"        %  "logback-classic"        % logbackVersion,
      "com.auth0"             %  "java-jwt"               % oauthJwtVersion,
      "com.github.pureconfig" %% "pureconfig"             % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
      "com.h2database"        %  "h2"                     % h2Version % Test,
      "io.circe"              %% "circe-generic"          % circeVersion,
      "org.flywaydb"          %  "flyway-core"            % flywayVersion % Test,
      "org.http4s"            %% "http4s-blaze-server"    % http4sVersion,
      "org.http4s"            %% "http4s-circe"           % http4sVersion,
      "org.http4s"            %% "http4s-dsl"             % http4sVersion,
      "org.postgresql"        %  "postgresql"             % postgresVersion,
      "org.tpolecat"          %% "doobie-core"            % doobieVersion,
      "org.tpolecat"          %% "doobie-h2"              % doobieVersion % Test,
      "org.tpolecat"          %% "doobie-hikari"          % doobieVersion,
      "org.tpolecat"          %% "doobie-postgres"        % doobieVersion,
      "org.tpolecat"          %% "doobie-specs2"          % doobieVersion % Test,
      "org.specs2"            %% "specs2-core"            % specs2Version % Test
    )
  )

  private[this] def compilerPlugins = Seq(
    // Add syntax for type lambdas
    // See: https://github.com/non/kind-projector
    addCompilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorVersion cross CrossVersion.full),

    // Desugaring scala `for` without implicit `withFilter`s
    // See: https://github.com/oleg-py/better-monadic-for
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForVersion)
  )

  private[this] def flywaySettings = Seq(
    // Flyway database schema migrations
    flywayUrl := "jdbc:postgresql://0.0.0.0:5432/postgres",
    flywayUser := "postgres",
    flywayPassword := "S3cret!",

    // Separate the schema and seed, as unit tests does not require seed test data
    flywayLocations := Seq("filesystem:db/migration/postgresql", "filesystem:db/seed")
  )

  private[this] def assemblySettings = Seq(
    assemblyMergeStrategy in assembly := {
      case "module-info.class" =>
        MergeStrategy.concat
      case f =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(f)
    }
  )

  /**
   * Basic auto-closing implementation for closeable resource.
   * 
   * @param res Closeable resource.
   * @param fn Lambda function performing resource operations.
   * @tparam T Resource type parameters.
   * @tparam U Lambda function result type parameters.
   * @return Lambda function result.
   */
  private[this] def using[T <: Closeable, U](res: T)(fn: T => U): U =
    try { fn(res) } finally { res.close() }

}
