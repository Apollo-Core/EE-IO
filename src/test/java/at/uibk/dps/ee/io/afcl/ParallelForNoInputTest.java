package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class ParallelForNoInputTest {

  @Test
  public void test() {
    Workflow wf = Graphs.getParallelForNoInput();
    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(wf);
    Task distTask = result.getVertex("parallelFor--Distribution");
    Task funct1 = result.getVertex("atomicFunctionFirst");
    Task seqTask = result.getPredecessors(funct1).iterator().next();
    assertTrue(PropertyServiceData.getNodeType(seqTask).equals(NodeType.Sequentiality));
    assertTrue(result.getSuccessors(distTask).contains(seqTask));
  }

  @Test
  public void getSubgraphRootsTest() {
    Task function1 = new Task("func1");
    Task function2 = new Task("func2");
    Task function3 = new Task("func3");

    Communication data1 = new Communication("data1");
    Communication data2 = new Communication("data2");
    Communication data3 = new Communication("data3");
    Communication data4 = new Communication("data4");

    Task distNode = new Task("dist");

    EnactmentGraph graph = new EnactmentGraph();

    addDependency(data1, function1, graph, 0);
    addDependency(function1, data3, graph, 1);
    addDependency(data3, function3, graph, 2);
    addDependency(distNode, data2, graph, 3);
    addDependency(data2, function2, graph, 4);
    addDependency(function2, data4, graph, 5);
    addDependency(data4, function3, graph, 6);

    Set<Task> subGraphTasks = new HashSet<>();
    subGraphTasks.add(function1);
    subGraphTasks.add(function2);
    subGraphTasks.add(function3);

    Set<Task> result = AfclCompoundsParallelFor.getSubGraphRoots(graph, subGraphTasks, distNode);
    assertEquals(1, result.size());
    assertTrue(result.contains(function1));
  }


  protected static void addDependency(Task src, Task dst, EnactmentGraph graph, int idx) {
    Dependency dep = new Dependency("Dep" + idx);
    graph.addEdge(dep, src, dst, EdgeType.DIRECTED);
  }
}
