package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

public class ParallelForTest {

  @Test
  public void testParFor() {
    Workflow wf = Graphs.getParallelForWf();
    // get the enactment graph
    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(wf);
    // check the func and the data count
    long funcCount =
        result.getVertices().stream().filter(task -> TaskPropertyService.isProcess(task)).count();
    long dataCount = result.getVertices().stream()
        .filter(task -> TaskPropertyService.isCommunication(task)).count();

    assertEquals(5, funcCount);
    assertEquals(7, dataCount);

    // check root and leaf number
    assertEquals(2,
        result.getVertices().stream().filter(
            task -> TaskPropertyService.isCommunication(task) && PropertyServiceData.isRoot(task))
            .count());
    assertEquals(1,
        result.getVertices().stream().filter(
            task -> TaskPropertyService.isCommunication(task) && PropertyServiceData.isLeaf(task))
            .count());

    // get all the nodes
    Task output = result.getVertices().stream()
        .filter(
            task -> TaskPropertyService.isCommunication(task) && PropertyServiceData.isLeaf(task))
        .findAny().orElseThrow(() -> new AssertionError());
    assertEquals(DataType.Collection, PropertyServiceData.getDataType(output));
    Task aggregationNode = result.getPredecessors(output).iterator().next();
    Task aggregationData = result.getPredecessors(aggregationNode).iterator().next();
    assertEquals(DataType.String, PropertyServiceData.getDataType(aggregationData));
    Task function2 = result.getPredecessors(aggregationData).iterator().next();
    Task intermediateData = result.getPredecessors(function2).iterator().next();
    assertEquals(DataType.Number, PropertyServiceData.getDataType(intermediateData));

    Task distInput = result.getVertex(ConstantsTestCoreEEiO.parForRawDistDataName);
    Task nonDistributedCollection = result.getVertex(ConstantsTestCoreEEiO.parForRawConstDataName);
    assertEquals(DataType.Collection, PropertyServiceData.getDataType(nonDistributedCollection));

    Task blockFunc = result.getSuccessors(distInput).iterator().next();
    Task blockData = result.getSuccessors(blockFunc).iterator().next();
    assertEquals(DataType.Collection, PropertyServiceData.getDataType(blockData));
    Task distributionNode = result.getSuccessors(blockData).iterator().next();
    Task distributionData = result.getSuccessors(distributionNode).iterator().next();
    assertEquals(DataType.Number, PropertyServiceData.getDataType(distributionData));

    // check the annotation

    // dist and aggr nodes
    assertEquals(OperationType.Aggregation,
        PropertyServiceFunctionDataFlowCollections.getOperationType(aggregationNode));
    assertEquals(OperationType.Distribution,
        PropertyServiceFunctionDataFlowCollections.getOperationType(distributionNode));
    assertEquals(ConstantsTestCoreEEiO.parForParForName,
        PropertyServiceFunctionDataFlowCollections.getScope(aggregationNode));
    assertEquals(ConstantsTestCoreEEiO.parForParForName,
        PropertyServiceFunctionDataFlowCollections.getScope(distributionNode));

  }

  @Test
  public void testParForMoreComplex() {
    Workflow wf = Graphs.getParallelForComplexWf();
    // get the enactment graph
    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(wf);
    Set<Task> functions = result.getVertices().stream()
        .filter(task -> TaskPropertyService.isProcess(task)).collect(Collectors.toSet());
    // check the func and the data count
    long funcCount = functions.size();
    long dataCount = result.getVertices().stream()
        .filter(task -> TaskPropertyService.isCommunication(task)).count();
    assertEquals(6, funcCount);
    assertEquals(10, dataCount);
    // get the distribution and the aggregation function
    Task distributionNode = result.getVertices().stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isDistributionNode(task))
        .findAny().orElseThrow(() -> new AssertionError("Distribution node not found"));
    Set<Task> aggregationNodes = result.getVertices().stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isAggregationNode(task))
        .collect(Collectors.toSet());
    assertEquals(2, aggregationNodes.size());
    for (Task aggre : aggregationNodes) {
      assertEquals(1, result.getPredecessorCount(aggre));
      assertEquals(1, result.getSuccessorCount(aggre));
    }
    assertEquals(2, result.getPredecessorCount(distributionNode));
    assertEquals(2, result.getSuccessorCount(distributionNode));

    for (Dependency outEdge : result.getOutEdges(distributionNode)) {
      assertTrue(PropertyServiceDependency.getJsonKey(outEdge)
          .equals(ConstantsTestCoreEEiO.parForColl1Name)
          || PropertyServiceDependency.getJsonKey(outEdge)
              .equals(ConstantsTestCoreEEiO.parForColl2Name));
    }
  }

  @Test
  public void testParForIntIterator() {
    Workflow wf = Graphs.getParallelForIntIteratorWf();
    // get the enactment graph
    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(wf);
    Set<Task> functions = result.getVertices().stream()
        .filter(task -> TaskPropertyService.isProcess(task)).collect(Collectors.toSet());
    // check the func and the data count
    long funcCount = functions.size();
    long dataCount = result.getVertices().stream()
        .filter(task -> TaskPropertyService.isCommunication(task)).count();
    assertEquals(5, funcCount);
    assertEquals(9, dataCount);
    // get the distribution and the aggregation function
    Task distributionNode = functions.stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isDistributionNode(task))
        .findAny().orElseThrow(() -> new AssertionError("Distribution node not found"));
    Set<Task> aggregationNodes = functions.stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isAggregationNode(task))
        .collect(Collectors.toSet());
    assertEquals(2, aggregationNodes.size());
    for (Task aggre : aggregationNodes) {
      assertEquals(1, result.getPredecessorCount(aggre));
      assertEquals(1, result.getSuccessorCount(aggre));
    }
    assertEquals(1, result.getPredecessorCount(distributionNode));
    assertEquals(1, result.getSuccessorCount(distributionNode));
  }
}
