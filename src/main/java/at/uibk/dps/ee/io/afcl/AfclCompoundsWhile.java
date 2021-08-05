package at.uibk.dps.ee.io.afcl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.While;
import at.uibk.dps.afcl.functions.objects.DataOuts;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.objects.Condition;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCondition;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * Static container for the methods necessary to create the enactment graph
 * elements realizing the "while" functionality of a given AFCL while compound.
 * 
 * @author Fedor Smirnov
 */
public final class AfclCompoundsWhile {

  /**
   * No constructor.
   */
  private AfclCompoundsWhile() {}

  /**
   * Adds the nodes modeling the functionality of the provided while compound to
   * the given enactment graph.
   * 
   * @param graph the given enactment graph
   * @param whileCompound the given while compound (AFCL)
   * @param workflow the AFCL workflow
   */
  public static void addWhile(EnactmentGraph graph, While whileCompound, Workflow workflow) {
    // create the data node representing the while start
    Task whileStart = PropertyServiceData.createWhileStart(whileCompound.getName());
    graph.addVertex(whileStart);
    // create the data node representing the loop counter
    String counterId = whileCompound.getName() + ConstantsAfcl.SourceAffix
        + ConstantsEEModel.WhileLoopCounterSuffix;
    Task loopCounter = PropertyServiceData.createWhileCounter(counterId);
    graph.addVertex(loopCounter);
    // create the contents of the loop body
    Set<Task> beforeAddingLoopBody = AfclCompounds.getFunctionNodes(graph);
    Set<Task> whileDataNodesBeforeBody = getWhileStartNodesInGraph(graph);
    AfclCompounds.processTheLoopBody(whileCompound, graph, workflow);
    // create the condition
    Task stopDecision = createCondition(graph, whileCompound, workflow);
    Set<Task> whileDataNodesAfterBody = getWhileStartNodesInGraph(graph);
    whileDataNodesAfterBody.removeAll(whileDataNodesBeforeBody);
    Set<Task> loopBodyFunctions = AfclCompounds.getFunctionNodes(graph);
    loopBodyFunctions.removeAll(beforeAddingLoopBody);
    whileDataNodesAfterBody
        .forEach(nestedWhile -> enforceSequentialityNestedWhile(whileStart, nestedWhile, graph));
    // connect the functions to the while start
    loopBodyFunctions.forEach(bodyFunction -> PropertyServiceDependency
        .addDataDependency(whileStart, bodyFunction, ConstantsEEModel.JsonKeyWhileStart, graph));
    // create the node representing the end of the while compound/iteration
    Task whileEnd = PropertyServiceFunctionUtilityWhile.createWhileEndTask(whileStart, loopCounter);
    // connect the condition node and the loop counter to the while end
    PropertyServiceDependency.addDataDependency(loopCounter, whileEnd,
        ConstantsEEModel.JsonKeyWhileCounter, graph);
    PropertyServiceDependency.addDataDependency(stopDecision, whileEnd,
        ConstantsEEModel.JsonKeyWhileDecision, graph);
    // for each data out, connect the stuff within the loop body to the while end,
    // and create a data node representing the final result of the while
    whileCompound.getDataOuts().forEach(dataOut -> processWhileDataOut(dataOut, graph,
        whileCompound.getName(), workflow, whileEnd));
  }

  /**
   * Add a sequelizer node between the while start and the nested while start.
   * 
   * @param whileStart the while start node
   * @param nestedWhileStart the nested while start node
   * @param graph the enactment graph
   */
  protected static void enforceSequentialityNestedWhile(Task whileStart, Task nestedWhileStart,
      EnactmentGraph graph) {
    PropertyServiceFunctionUtility.addSequelizerNode(whileStart, nestedWhileStart, graph);
    PropertyServiceData.resetContent(nestedWhileStart);
  }

  /**
   * Returns the while start nodes contained in the given graph
   * 
   * @param graph the given graph
   * @return the while start nodes in the given graph
   */
  protected static Set<Task> getWhileStartNodesInGraph(EnactmentGraph graph) {
    return graph.getVertices().stream().filter(node -> TaskPropertyService.isCommunication(node))
        .filter(dataNode -> PropertyServiceData.getNodeType(dataNode).equals(NodeType.WhileStart))
        .collect(Collectors.toSet());
  }

  /**
   * Processes the given data out of the while compound by (a) creating the nodes
   * representing the overall result of the while (successors of the while end)
   * and (b) connecting the nodes from within the loop body to the while end (as
   * its predecessors).
   * 
   * @param dataOut the processed (AFCL) data out
   * @param graph the enactment graph
   * @param whileId the name of the whole while compound
   * @param wf the (AFCL) workflow
   * @param whileEnd the task node modeling the end of the while compound
   */
  protected static void processWhileDataOut(DataOuts dataOut, EnactmentGraph graph, String whileId,
      Workflow wf, Task whileEnd) {
    // process/create the successor (overall while result)
    String successorId = UtilsAfcl.getDataNodeId(whileId, dataOut.getName());
    String jsonKey = dataOut.getName();
    DataType dataType = UtilsAfcl.getDataTypeForString(dataOut.getType());
    Task dataNode = AfclCompounds.assureDataNodePresence(successorId, dataType, graph);
    PropertyServiceData.annotateOriginalWhileEnd(dataNode, whileEnd.getId());
    PropertyServiceDependency.addDataDependency(whileEnd, dataNode, jsonKey, graph);
    // process the predecessor (data node from within the while loop body)
    String srcString = dataOut.getSource();
    Task functionResult = AfclCompounds.assureDataNodePresence(srcString, dataType, graph);
    PropertyServiceDependency.addDataDependency(functionResult, whileEnd, jsonKey, graph);
  }

  /**
   * Creates the nodes modeling the condition check of the given while loop and
   * adds them to the graph. Returns the node representing the decision whether to
   * continue.
   * 
   * @param graph the enactment graph
   * @param whileCompound the while compound
   * @param workflow the afcl workflow
   * @return the data node with the decision whether to continue
   */
  protected static Task createCondition(EnactmentGraph graph, While whileCompound,
      Workflow workflow) {
    String nodeId = whileCompound.getName() + ConstantsEEModel.KeywordSeparator1
        + ConstantsEEModel.WhileStopConditionSuffix;
    List<Condition> conditions = new ArrayList<>();
    Task conditionNode =
        PropertyServiceFunctionUtilityCondition.createConditionEvaluation(nodeId, conditions);
    whileCompound.getCondition().forEach(cond -> conditions.add(
        AfclCompoundsIf.addConditionNode(graph, cond, conditionNode, workflow, whileCompound)));
    PropertyServiceFunctionUtilityCondition.setConditions(conditionNode, conditions);
    Task stopDecisionVariable = new Communication(whileCompound.getName()
        + ConstantsEEModel.KeywordSeparator1 + ConstantsEEModel.WhileStopConditionBooleanSuffix);
    PropertyServiceDependency.addDataDependency(conditionNode, stopDecisionVariable,
        ConstantsEEModel.JsonKeyIfDecision, graph);
    return stopDecisionVariable;
  }
}
