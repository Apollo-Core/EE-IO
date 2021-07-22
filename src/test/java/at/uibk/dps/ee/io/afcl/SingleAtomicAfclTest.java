package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency.TypeDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

public class SingleAtomicAfclTest {

  @Test
  public void testGenerateSingleAtomicGraph() {

    Workflow singleAtomicWf = Graphs.getSingleAtomicWf();
    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(singleAtomicWf);

    // test the general edge and node numbers
    assertEquals(4, result.getVertexCount());
    assertEquals(3, result.getEdgeCount());

    // test that we have the data ins and the data outs, set as root and leaf, type
    // as well

    // get the wf data outs and ins
    Set<Task> roots = new HashSet<>();
    Set<Task> leaves = new HashSet<>();
    Set<Task> processes = new HashSet<>();

    for (Task t : result) {
      if (result.getPredecessorCount(t) == 0) {
        roots.add(t);
      }
      if (result.getSuccessorCount(t) == 0) {
        leaves.add(t);
      }
      if (TaskPropertyService.isProcess(t)) {
        processes.add(t);
      }
    }

    assertEquals(2, roots.size());
    assertEquals(1, leaves.size());
    assertEquals(1, processes.size());

    // check the nodes

    // check the root and the constant data node
    Iterator<Task> taskIterator = roots.iterator();
    Task root1 = taskIterator.next();
    Task root2 = taskIterator.next();

    Task actualRoot = PropertyServiceData.isRoot(root1) ? root1 : root2;
    Task constantData = PropertyServiceData.isRoot(root1) ? root2 : root1;

    assertTrue(PropertyServiceData.isRoot(actualRoot));
    assertEquals(UtilsAfcl.getDataNodeId(ConstantsTestCoreEEiO.wfNameAtomic,
        ConstantsTestCoreEEiO.inputNameAtomic), actualRoot.getId());
    assertEquals(ConstantsTestCoreEEiO.wfInputJsonNameAtomic,
        PropertyServiceData.getJsonKey(actualRoot));
    assertEquals(DataType.Number, PropertyServiceData.getDataType(actualRoot));

    assertFalse(PropertyServiceData.isRoot(constantData));
    assertEquals(UtilsAfcl.getDataNodeId(ConstantsTestCoreEEiO.wfFunctionNameAtomic,
        ConstantsTestCoreEEiO.wfFunctionConstantInputNameAtomic), constantData.getId());
    assertEquals(DataType.Number, PropertyServiceData.getDataType(constantData));
    assertEquals(ConstantsTestCoreEEiO.wfSingleAtomicConstant,
        PropertyServiceData.getContent(constantData).getAsInt());

    // check the leaf
    Task leaf = leaves.iterator().next();
    assertTrue(PropertyServiceData.isLeaf(leaf));
    assertEquals(UtilsAfcl.getDataNodeId(ConstantsTestCoreEEiO.wfFunctionNameAtomic,
        ConstantsTestCoreEEiO.wfFunctionOutputNameAtomic), leaf.getId());
    assertEquals(ConstantsTestCoreEEiO.outputNameAtomic, PropertyServiceData.getJsonKey(leaf));
    assertEquals(DataType.String, PropertyServiceData.getDataType(leaf));

    // check the function
    Task func = processes.iterator().next();
    assertEquals(ConstantsTestCoreEEiO.wfFunctionNameAtomic, func.getId());
    assertEquals(UsageType.User, PropertyServiceFunction.getUsageType(func));

    // Check the edges
    Set<Dependency> inEdges = new HashSet<Dependency>(result.getInEdges(func));
    Set<Dependency> outEdges = new HashSet<Dependency>(result.getOutEdges(func));

    assertEquals(2, inEdges.size());
    assertEquals(1, outEdges.size());

    Iterator<Dependency> edgeIterator = inEdges.iterator();
    Dependency inputEdge1 = edgeIterator.next();
    Dependency inputEdge2 = edgeIterator.next();

    Dependency edgeToRoot =
        result.getEndpoints(inputEdge1).getFirst().equals(actualRoot) ? inputEdge1 : inputEdge2;
    Dependency edgeToConstant =
        result.getEndpoints(inputEdge1).getFirst().equals(actualRoot) ? inputEdge2 : inputEdge1;

    Dependency outputEdge = outEdges.iterator().next();

    assertEquals(TypeDependency.Data, PropertyServiceDependency.getType(outputEdge));
    assertEquals(TypeDependency.Data, PropertyServiceDependency.getType(edgeToRoot));
    assertEquals(TypeDependency.Data, PropertyServiceDependency.getType(edgeToConstant));

    assertEquals(actualRoot, result.getEndpoints(edgeToRoot).getFirst());
    assertEquals(constantData, result.getEndpoints(edgeToConstant).getFirst());
    assertEquals(leaf, result.getEndpoints(outputEdge).getSecond());

    assertEquals(ConstantsTestCoreEEiO.wfFunctionInputNameAtomic,
        PropertyServiceDependency.getJsonKey(edgeToRoot));
    assertEquals(ConstantsTestCoreEEiO.wfFunctionConstantInputNameAtomic,
        PropertyServiceDependency.getJsonKey(edgeToConstant));
    assertEquals(ConstantsTestCoreEEiO.wfFunctionOutputNameAtomic,
        PropertyServiceDependency.getJsonKey(outputEdge));
  }
}
