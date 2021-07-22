package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections.CollectionOperation;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

public class ElementIndexTest {

  @Test
  public void testGenerateAtomicWithElementIndex() {

    Workflow atomicElementIndexWf = Graphs.getElementIndexWf();
    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(atomicElementIndexWf);
    // we can assume that the atomic part is done right

    // what we want to check is that
    // we have one more data and function node
    int functionNum = 0;
    int dataNum = 0;
    for (Task task : result) {
      if (TaskPropertyService.isProcess(task)) {
        functionNum++;
      } else {
        dataNum++;
      }
    }
    assertEquals(7, dataNum);
    assertEquals(3, functionNum);

    // that we have the right predecessor relations and the right naming
    Task functionNode = result.getVertex(ConstantsTestCoreEEiO.wfFunctionNameAtomic);
    Task dataNodeProcessed2 = result.getPredecessors(functionNode).iterator().next();
    Task dataProcessingNode2 = result.getPredecessors(dataNodeProcessed2).iterator().next();

    Task dataNodeProcessed = result.getPredecessors(dataProcessingNode2).iterator().next();
    Task dataProcessingNode = result.getPredecessors(dataNodeProcessed).iterator().next();

    assertEquals(3, result.getPredecessorCount(dataProcessingNode));
    Task dataRawNode = result.getVertex(ConstantsTestCoreEEiO.elementIndexRawDataName);
    Task dataInputStride = result.getVertex(ConstantsTestCoreEEiO.elementIndexInputStride);
    Task dataInputIndex = result.getVertex(ConstantsTestCoreEEiO.elementIndexInputIndex);
    assertTrue(result.getPredecessors(dataProcessingNode).contains(dataRawNode));
    assertTrue(result.getPredecessors(dataProcessingNode).contains(dataInputIndex));
    assertTrue(result.getPredecessors(dataProcessingNode).contains(dataInputStride));

    assertEquals(ConstantsTestCoreEEiO.elementIndexRawDataName, dataRawNode.getId());
    assertEquals(ConstantsTestCoreEEiO.elementIndexDataProcessingName, dataProcessingNode.getId());
    assertEquals(ConstantsTestCoreEEiO.elementIndexProcessedDataName, dataNodeProcessed.getId());

    // that the function node is annotated correctly
    assertEquals(UtilityType.CollectionOperation,
        PropertyServiceFunctionUtility.getUtilityType(dataProcessingNode));
    assertEquals(ConstantsTestCoreEEiO.elementExpectedSubCollString,
        PropertyServiceFunctionUtilityCollections.getSubCollectionsString(dataProcessingNode));
    assertEquals(CollectionOperation.ElementIndex,
        PropertyServiceFunctionUtilityCollections.getCollectionOperation(dataProcessingNode));
    // check the json keys of the src inputs
    Dependency strideOut = result.getOutEdges(dataInputStride).iterator().next();
    Dependency indexOut = result.getOutEdges(dataInputIndex).iterator().next();
    assertEquals(ConstantsTestCoreEEiO.elementIndexIndexName,
        PropertyServiceDependency.getJsonKey(indexOut));
    assertEquals(ConstantsTestCoreEEiO.elementIndexInputStride,
        PropertyServiceDependency.getJsonKey(strideOut));
  }
}
