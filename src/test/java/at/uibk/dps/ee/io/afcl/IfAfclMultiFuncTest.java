package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.objects.Condition;
import at.uibk.dps.ee.model.objects.Condition.CombinedWith;
import at.uibk.dps.ee.model.objects.Condition.Operator;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCondition;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow.DataFlowType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

public class IfAfclMultiFuncTest {

  @Test
  public void test() {
    Workflow wf = Graphs.getIfWfMulti();

    EnactmentGraph result = GraphGenerationAfcl.generateEnactmentGraph(wf);
    // test the node and edge number


    int funcNum = 0;
    int dataNum = 0;

    for (Task t : result) {
      if (TaskPropertyService.isProcess(t)) {
        funcNum++;
      } else {
        dataNum++;
      }
    }

    int edgeNum = result.getEdgeCount();
    assertEquals(5, funcNum);
    assertEquals(12, dataNum);
    assertEquals(21, edgeNum);

    Task func1Node = result.getVertex("func1");
    Task func2Node = result.getVertex("func2");
    Task func3Node = result.getVertex("func3");

    // get the utility nodes
    Task condFunc = null;
    Task choiceFunc = null;

    for (Task t : result) {
      if (TaskPropertyService.isProcess(t)) {
        if (PropertyServiceFunction.getUsageType(t).equals(UsageType.Utility)) {
          assertEquals(UtilityType.Condition, PropertyServiceFunctionUtility.getUtilityType(t));
          condFunc = t;
        }
        if (PropertyServiceFunction.getUsageType(t).equals(UsageType.DataFlow)) {
          assertEquals(DataFlowType.Multiplexer,
              PropertyServiceFunctionDataFlow.getDataFlowType(t));
          choiceFunc = t;
        }
      }
    }
    assertNotNull(choiceFunc);
    assertNotNull(condFunc);

    String condFuncId = ConstantsTestCoreEEiO.simpleIfIfName;
    assertEquals(condFuncId, condFunc.getId());

    // test the conditional function node

    // get the data predecessor nodes
    assertNotNull(result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionConst1Name));
    assertNotNull(result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionConst2Name));
    assertNotNull(result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionInput1Name));
    assertNotNull(result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionInput2Name));

    Task condInput1 = result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionInput1Name);
    Task condInput2 = result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionInput2Name);
    Task condConst1 = result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionConst1Name);
    Task condConst2 = result.getVertex(ConstantsTestCoreEEiO.simpleIfConditionConst2Name);

    assertEquals(NodeType.Constant, PropertyServiceData.getNodeType(condConst1));
    assertEquals(NodeType.Constant, PropertyServiceData.getNodeType(condConst2));
    assertEquals(DataType.Boolean, PropertyServiceData.getDataType(condConst1));
    assertTrue(PropertyServiceData.getContent(condConst1).getAsBoolean());
    assertEquals(DataType.String, PropertyServiceData.getDataType(condConst2));
    assertEquals("abc", PropertyServiceData.getContent(condConst2).getAsString());

    // get the data successor node
    String conditionVertexId =
        ConstantsTestCoreEEiO.simpleIfIfName + ConstantsEEModel.DecisionVariableSuffix;
    assertNotNull(result.getVertex(conditionVertexId));
    Task decisionVariable = result.getVertex(conditionVertexId);

    // its properties

    // the expected conditions
    Condition expCond1 = new Condition(ConstantsTestCoreEEiO.simpleIfConditionInput1Name,
        ConstantsTestCoreEEiO.simpleIfConditionConst1Name, Operator.EQUAL, false, DataType.Boolean,
        CombinedWith.And);
    Condition expCond2 = new Condition(ConstantsTestCoreEEiO.simpleIfConditionInput2Name,
        ConstantsTestCoreEEiO.simpleIfConditionConst2Name, Operator.STARTS_WITH, true,
        DataType.String, CombinedWith.Or);
    List<Condition> conditions = PropertyServiceFunctionUtilityCondition.getConditions(condFunc);
    assertTrue(conditions.contains(expCond1));
    assertTrue(conditions.contains(expCond2));

    // its in- and out- edges
    Set<Task> predecessors = new HashSet<>(result.getPredecessors(condFunc));
    assertEquals(4, predecessors.size());
    assertTrue(predecessors.contains(condInput1));
    assertTrue(predecessors.contains(condInput2));
    assertTrue(predecessors.contains(condConst1));
    assertTrue(predecessors.contains(condConst2));
    Set<Task> successors = new HashSet<>(result.getSuccessors(condFunc));
    assertEquals(1, successors.size());
    assertTrue(successors.contains(decisionVariable));
    assertEquals(NodeType.Decision, PropertyServiceData.getNodeType(decisionVariable));

    // check the json key of the in edge
    Dependency inEdge = result.getInEdges(decisionVariable).iterator().next();
    String expectedDecVarJsonKey = ConstantsEEModel.JsonKeyIfDecision;
    assertEquals(expectedDecVarJsonKey, PropertyServiceDependency.getJsonKey(inEdge));

    // the decision variable and its successors
    Set<Task> successorsCondition = new HashSet<>(result.getSuccessors(decisionVariable));
    assertEquals(4, successorsCondition.size());
    assertTrue(successorsCondition.contains(func1Node));
    assertTrue(successorsCondition.contains(func2Node));
    assertTrue(successorsCondition.contains(func3Node));
  }
}
