package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class AfclCompoundsTest {

  @Test
  public void testAddConstantDataNode() {
    String funcName = "func";
    EnactmentGraph graph = new EnactmentGraph();
    Task function = new Task(funcName);
    graph.addVertex(function);

    String dataInName = "secondIn";
    DataIns dataIn = new DataIns();
    dataIn.setName(dataInName);
    dataIn.setType("number");
    dataIn.setSource("5");

    AfclCompounds.addDataInConstant(graph, function, dataIn, DataType.Number);

    assertEquals(1, graph.getEdgeCount());
    assertEquals(2, graph.getVertexCount());

    Dependency dep = graph.getEdges().iterator().next();
    Task data = graph.getSource(dep);
    Task func = graph.getDest(dep);

    assertEquals(func, function);

    assertEquals(dataInName, PropertyServiceDependency.getJsonKey(dep));
    assertEquals("Constant/5", data.getId());
    assertEquals(NodeType.Constant, PropertyServiceData.getNodeType(data));
    assertEquals(DataType.Number, PropertyServiceData.getDataType(data));
    assertEquals(5, PropertyServiceData.getContent(data).getAsInt());
  }
}
