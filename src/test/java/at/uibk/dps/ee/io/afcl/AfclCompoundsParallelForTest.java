package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import net.sf.opendse.model.Task;

public class AfclCompoundsParallelForTest {

  @Test
  public void testIsIntIterator() {
    assertTrue(AfclCompoundsParallelFor.isIntIterator("5"));
    assertTrue(AfclCompoundsParallelFor.isIntIterator("function/output"));
    assertFalse(AfclCompoundsParallelFor.isIntIterator("myDataIn"));
  }

  @Test
  public void testProcessIteratorConstantInt() {

    EnactmentGraph graph = new EnactmentGraph();
    Task distributionNode = new Task("task");
    graph.addVertex(distributionNode);

    List<DataIns> dataIns = new ArrayList<>();
    String parForName = "parFor";

    String iteratorString = "5";

    AfclCompoundsParallelFor.processIterator(iteratorString, graph, dataIns, distributionNode,
        parForName);

    assertEquals(2, graph.getVertexCount());
    assertEquals(1, graph.getPredecessorCount(distributionNode));
    Task iteratorContent = graph.getPredecessors(distributionNode).iterator().next();

    assertEquals(NodeType.Constant, PropertyServiceData.getNodeType(iteratorContent));
    assertEquals(5, PropertyServiceData.getContent(iteratorContent).getAsInt());

  }
}
