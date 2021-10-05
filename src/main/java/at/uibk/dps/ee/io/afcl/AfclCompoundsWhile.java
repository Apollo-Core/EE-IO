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
  public static void addWhile(final EnactmentGraph graph, final While whileCompound,
      final Workflow workflow) {
    // create the data node representing the while start
    final Task whileStart = PropertyServiceData.createWhileStart(whileCompound.getName());
    graph.addVertex(whileStart);
    // create the data node representing the loop counter
    final String counterId = whileCompound.getName() + ConstantsAfcl.SourceAffix
        + ConstantsEEModel.WhileLoopCounterSuffix;
    final Task loopCounter = PropertyServiceData.createWhileCounter(counterId);
    graph.addVertex(loopCounter);
    // create the contents of the loop body
    final Set<Task> beforeAddingLoopBody = AfclCompounds.getFunctionNodes(graph);
    final Set<Task> whileDataNodesBeforeBody = getWhileNodesInGraph(graph, true);
    AfclCompounds.processTheLoopBody(whileCompound, graph, workflow);
    // create the condition
    final Task stopDecision = createCondition(graph, whileCompound, workflow);
    final Set<Task> loopBodyFunctions = AfclCompounds.getFunctionNodes(graph);
    loopBodyFunctions.removeAll(beforeAddingLoopBody);
    enforceWhileStartOrder(whileDataNodesBeforeBody, whileStart, graph, true);
    // connect the functions to the while start
    loopBodyFunctions.forEach(bodyFunction -> PropertyServiceDependency
        .addDataDependency(whileStart, bodyFunction, ConstantsEEModel.JsonKeyWhileStart, graph));
    // create the node representing the end of the while compound/iteration
    final Task whileEnd =
        PropertyServiceFunctionUtilityWhile.createWhileEndTask(whileStart, loopCounter);
    // connect the condition node and the loop counter to the while end
    PropertyServiceDependency.addDataDependency(loopCounter, whileEnd,
        ConstantsEEModel.JsonKeyWhileCounter, graph);
    PropertyServiceDependency.addDataDependency(stopDecision, whileEnd,
        ConstantsEEModel.JsonKeyWhileDecision, graph);
    // for each data out, connect the stuff within the loop body to the while end,
    // and create a data node representing the final result of the while
    whileCompound.getDataOuts()
        .forEach(dataOut -> processWhileDataOut(dataOut, graph, whileCompound.getName(), whileEnd));
  }

  /**
   * Ensures that any while start node in the provided nested node set will start
   * after the given predecessor node by adding an extra sequentiality node
   * between the predecessor and the while start.
   * 
   * @param whileTasksPreTransform the set of while tasks which were there before
   *        the transformation
   * @param predecessor the task which must be executed before the while
   * @param lookingForWhileStarts true: looking for while starts; false: looking
   *        for while counters
   * @param graph the enactment graph
   */
  public static void enforceWhileStartOrder(Set<Task> whileTasksPreTransform, Task predecessor,
      EnactmentGraph graph, boolean lookingForWhileStarts) {
    Set<Task> innerWhiles = getWhileNodesInGraph(graph, lookingForWhileStarts);
    innerWhiles.removeAll(whileTasksPreTransform);
    innerWhiles.stream().filter(node -> TaskPropertyService.isCommunication(node))
        .filter(dataNode -> lookingForWhileStarts ? PropertyServiceData.isWhileStart(dataNode)
            : PropertyServiceData.isWhileCounter(dataNode))
        .forEach(nestedWhileStart -> enforceSequentialityNestedWhile(predecessor, nestedWhileStart,
            graph));
  }

  /**
   * Add a sequelizer node between the while start and the nested while start.
   * 
   * @param whileStart the while start node
   * @param nestedWhileStart the nested while start node
   * @param graph the enactment graph
   */
  static void enforceSequentialityNestedWhile(final Task whileStart, final Task nestedWhileStart,
      final EnactmentGraph graph) {
    PropertyServiceFunctionUtility.enforceSequentiality(whileStart, nestedWhileStart, graph);
    if (PropertyServiceData.isWhileStart(nestedWhileStart)) {
      PropertyServiceData.resetContent(nestedWhileStart);
    }
  }

  /**
   * Returns the while nodes contained in the given graph
   * 
   * @param graph the given graph
   * @param lookingForWhileStart true iff looking for while start nodes, false iff
   *        looking for while counters
   * @return the while start nodes in the given graph
   */
  public static Set<Task> getWhileNodesInGraph(final EnactmentGraph graph,
      boolean lookingForWhileStart) {
    return graph.getVertices().stream().filter(node -> TaskPropertyService.isCommunication(node))
        .filter(dataNode -> lookingForWhileStart ? PropertyServiceData.isWhileStart(dataNode)
            : PropertyServiceData.isWhileCounter(dataNode))
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
   * @param whileEnd the task node modeling the end of the while compound
   */
  static void processWhileDataOut(final DataOuts dataOut, final EnactmentGraph graph,
      final String whileId, final Task whileEnd) {
    // process/create the successor (overall while result)
    final String successorId = UtilsAfcl.getDataNodeId(whileId, dataOut.getName());
    final String jsonKey = dataOut.getName();
    final DataType dataType = UtilsAfcl.getDataTypeForString(dataOut.getType());
    final Task dataNode = AfclCompounds.assureDataNodePresence(successorId, dataType, graph);
    PropertyServiceData.annotateOriginalWhileEnd(dataNode, whileEnd.getId());
    PropertyServiceDependency.addDataDependency(whileEnd, dataNode, jsonKey, graph);
    // process the predecessor (data node from within the while loop body)
    final String srcString = dataOut.getSource();
    final Task functionResult = AfclCompounds.assureDataNodePresence(srcString, dataType, graph);
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
  static Task createCondition(final EnactmentGraph graph, final While whileCompound,
      final Workflow workflow) {
    final String nodeId = whileCompound.getName() + ConstantsEEModel.KeywordSeparator1
        + ConstantsEEModel.WhileConditionSuffix;
    final List<Condition> conditions = new ArrayList<>();
    final Task conditionNode =
        PropertyServiceFunctionUtilityCondition.createConditionEvaluation(nodeId, conditions);
    whileCompound.getCondition().forEach(cond -> conditions.add(
        AfclCompoundsIf.addConditionNode(graph, cond, conditionNode, workflow, whileCompound)));
    PropertyServiceFunctionUtilityCondition.setConditions(conditionNode, conditions);
    final Task stopDecisionVariable = new Communication(whileCompound.getName()
        + ConstantsEEModel.KeywordSeparator1 + ConstantsEEModel.WhileConditionBoolSuffix);
    PropertyServiceDependency.addDataDependency(conditionNode, stopDecisionVariable,
        ConstantsEEModel.JsonKeyIfDecision, graph);
    return stopDecisionVariable;
  }
}
