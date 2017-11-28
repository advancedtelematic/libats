
import net.virtualvoid.sbt.graph.DependencyGraphPlugin.autoImport._
import net.virtualvoid.sbt.graph.ModuleGraph
import net.virtualvoid.sbt.graph.rendering.DOT
import sbt._
import sbt.Keys._
import sbt.{AutoPlugin, TaskKey}

object DependencyGraph extends AutoPlugin {
  override lazy val projectSettings = dependencyGraphSettings

  object autoImport {
    lazy val moduledepDot = TaskKey[Unit]("moduleDepDot", "generate dot graph with this project sub module dependencies")
  }

  import autoImport._

  lazy val filter = ScopeFilter(inAnyProject, inAnyConfiguration)

  lazy val groupByProject: Def.Initialize[Task[ModuleGraph]] =
    Def.task { (moduleGraph in thisProject).value }

  val dependencyGraphSettings = Seq (
    dependencyDotNodeLabel := { (_: String, name: String, c: String) =>
      name.replaceAll("_2.+$", "")
    },
    moduledepDot := {
      lazy val all = groupByProject.all(filter).value.foldLeft(ModuleGraph(Seq.empty, Seq.empty)) { case (acc, g) =>
        val uniqNodes = (acc.nodes ++ g.nodes).groupBy(_.id.name).mapValues(_.head).values
        val uniqEdges = (acc.edges ++ g.edges).groupBy { case (a, b) => s"${a.name}-${b.name}" }.mapValues(_.head).values

        ModuleGraph(uniqNodes.toSeq, uniqEdges.toSeq)
      }

      val libatsNodes = all.nodes.filter(n => n.id.name.contains("libats") && !n.id.name.contains("libats_root"))
      val libatsEdges = all.edges.filter { case (a, b) => a.name.contains("libats") && b.name.contains("libats")}

      val newGraph = ModuleGraph(libatsNodes, libatsEdges)

      val header =
        """
          |digraph "dependency-graph" {
          |graph[rankdir="TB"]
          |edge [arrowtail="none"]
          |node [shape="box" fontname="sans serif"]
        """.stripMargin

      val dot = DOT.dotGraph(newGraph, header, dependencyDotNodeLabel.value, DOT.AngleBrackets)

      println(dot)
    }
  )
}
