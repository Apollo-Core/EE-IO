package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.properties.TaskPropertyService;

class WhileNestedTest {

  EnactmentGraph result;
  Dependency markedEdge;

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
    
    assertEquals(6, numFunc);
    assertEquals(14, numData);
    assertEquals(26, edgeNum);
    
    assertTrue(PropertyServiceDependency.isWhileAnnotated(markedEdge));
    assertEquals("increment/sum", PropertyServiceDependency.getDataRefForWhile(markedEdge, "innerWhile"));
    assertEquals("innerWhile/sum", PropertyServiceDependency.getDataRefForWhile(markedEdge, "outerWhile"));    
  }


  @BeforeEach
  void setup() {
    // read in the graph
    Workflow whileWf = Graphs.getWhileNested();
    result = GraphGenerationAfcl.generateEnactmentGraph(whileWf);
    markedEdge = result.getEdge("complexWhile/input--increment");
  }

}
