package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import net.sf.opendse.model.properties.TaskPropertyService;

class WhileNumTest {

  EnactmentGraph result;

  /**
   * Checks that the graph has the right numbers of nodes and edges.
   * 
   */
  @Test
  void testNumbers() {
    assertTrue((result instanceof EnactmentGraph));

    int numFunc = (int) result.getVertices().stream()
        .filter(node -> TaskPropertyService.isProcess(node)).count();
    int numData = (int) result.getVertices().stream()
        .filter(node -> TaskPropertyService.isCommunication(node)).count();
    int edgeNum = result.getEdgeCount();

    assertEquals(3, numFunc);
    assertEquals(8, numData);
    assertEquals(12, edgeNum);
  }


  @BeforeEach
  void setup() {
    // read in the graph
    Workflow whileWf = Graphs.getWhileNum();
    result = GraphGenerationAfcl.generateEnactmentGraph(whileWf);
  }
}