package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

class WhileNumTest {

  EnactmentGraph result;

  /**
   * Checks that the graph has the right numbers of nodes and edges.
   * 
   */
  @Test
  void testNumbers() {
    int numFunc = (int) result.getVertices().stream()
        .filter(node -> TaskPropertyService.isProcess(node)).count();
    int numData = (int) result.getVertices().stream()
        .filter(node -> TaskPropertyService.isCommunication(node)).count();
    int edgeNum = result.getEdgeCount();

    Task function = result.getVertex("increment");
    Task input = result.getVertex("single Atomic/input");
    Dependency markedEdge = result.findEdge(input, function);

    assertEquals(3, numFunc);
    assertEquals(8, numData);
    assertEquals(12, edgeNum);

    assertTrue(PropertyServiceDependency.isWhileAnnotated(markedEdge));
    assertEquals("increment/sum", PropertyServiceDependency.getReplicaSrcReference(markedEdge));
  }


  @BeforeEach
  void setup() {
    // read in the graph
    Workflow whileWf = Graphs.getWhileNum();
    result = GraphGenerationAfcl.generateEnactmentGraph(whileWf);
  }
}
