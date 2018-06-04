import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.SbtGit.GitKeys._
import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.git.ConsoleGitRunner

object Versioning {
  lazy val settings = Seq(
    git.runner := ConsoleGitRunner,
    git.useGitDescribe := true,
    git.baseVersion := "0.0.1"
  )

  val Plugin = GitVersioning
}
