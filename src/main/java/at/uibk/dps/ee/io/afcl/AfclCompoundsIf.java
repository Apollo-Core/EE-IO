package at.uibk.dps.ee.io.afcl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.common.base.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.IfThenElse;
import at.uibk.dps.afcl.functions.objects.DataOuts;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.objects.Condition;
import at.uibk.dps.ee.model.objects.Condition.CombinedWith;
import at.uibk.dps.ee.model.objects.Condition.Operator;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependencyControlIf;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCondition;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow.DataFlowType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;

/**
 * Static method container for the methods used when creating the enactment
 * graph parts modeling elements of if compounds.
 * 
 * @author Fedor Smirnov
 */
public final class AfclCompoundsIf {

  /**
   * No constructor.
   */
  private AfclCompoundsIf() {}

  /**
   * Adds the nodes and edges modeling the content of the given if compound.
   * 
   * @param graph the enactment graph
   * @param ifCompound the if compound to add
   * @param workflow the afcl workflow object
   */
  protected static void addIf(final EnactmentGraph graph, final IfThenElse ifCompound,
      final Workflow workflow) {
    // create and add the condition function, get the condition variable
    final Task conditionVariable = addConditionFunction(graph, ifCompound, workflow);
    // add the then branch
    addIfBranch(graph, ifCompound, workflow, conditionVariable, true);
    // add the else branch
    addIfBranch(graph, ifCompound, workflow, conditionVariable, false);
    // create and add a choice function for each data out
    for (final DataOuts dataOut : AfclApiWrapper.getDataOuts(ifCompound)) {
      addChoiceFunction(graph, dataOut, ifCompound, workflow, conditionVariable);
    }
  }

  /**
   * Adds the nodes modeling an if branch of the given if compound
   * 
   * @param graph the enactment graph
   * @param ifCompound the if compound that is being modeled
   * @param workflow the afcl workflow object
   * @param decisionVariable the decision variable
   * @param isThen true iff modeling the then branch
   */
  protected static void addIfBranch(final EnactmentGraph graph, final IfThenElse ifCompound,
      final Workflow workflow, final Task decisionVariable, final boolean isThen) {
    // remember all function nodes in the graph now
    final Set<Task> tasksBeforeAdding = AfclCompounds.getFunctionNodes(graph);
    // add the contents of the branch
    final List<Function> functionsToAdd =
        Optional.fromNullable(isThen ? ifCompound.getThenBranch() : ifCompound.getElseBranch())
            .or(new ArrayList<Function>());
    for (final Function function : functionsToAdd) {
      if (function instanceof AtomicFunction) {
        AfclCompoundsAtomic.addAtomicFunctionSubWfLevel(graph, (AtomicFunction) function, workflow);
      } else {
        AfclCompounds.addFunctionCompound(graph, function, workflow);
      }
    }
    // figure out which ones are new
    final Set<Task> tasksAfterAdding = AfclCompounds.getFunctionNodes(graph);
    tasksAfterAdding.removeAll(tasksBeforeAdding);
    // connect them to the condition variable
    tasksAfterAdding.forEach(newTask -> {
      PropertyServiceDependencyControlIf.addIfDependency(decisionVariable, newTask,
          decisionVariable.getId(), isThen, graph);
      return;
    });
  }

  /**
   * Adds the choice function and the data modeling the result of the if compound
   * described by the given data out.
   * 
   * @param graph the enactment graph
   * @param dataOut the given data out
   * @param ifCompound the given if compound
   * @param workflow the afcl workflow
   * @param conditionVariable the data node containing the decision variable
   */
  protected static void addChoiceFunction(final EnactmentGraph graph, final DataOuts dataOut,
      final IfThenElse ifCompound, final Workflow workflow, final Task conditionVariable) {
    checkDataOutIfSrc(dataOut, graph, workflow, ifCompound);
    final String srcString = AfclApiWrapper.getSource(dataOut);
    final String firstSrc = UtilsAfcl.getFirstSubStringIfOut(srcString);
    final String secondSrc = UtilsAfcl.getSecondSubStringIfOut(srcString);
    final Task firstSrcNode = graph.getVertex(firstSrc);
    final Task secondSrcNode = Optional.fromNullable(graph.getVertex(secondSrc))
        .or(graph.getVertex(HierarchyLevellingAfcl.getSrcDataId(secondSrc, ifCompound, workflow)));
    // create the choice function node
    final String funcNodeId = firstSrc + ConstantsEEModel.EarliestArrivalFuncAffix + secondSrc;
    final Task choiceFunction = PropertyServiceFunctionDataFlow.createDataFlowFunction(funcNodeId,
        DataFlowType.Multiplexer); // change this to a muxer
    // add the inputs (kind-of the same as data ins for an atomic)
    PropertyServiceDependencyControlIf.addIfDependency(firstSrcNode, choiceFunction,
        ConstantsEEModel.JsonKeyThen, true, graph);
    PropertyServiceDependencyControlIf.addIfDependency(secondSrcNode, choiceFunction,
        ConstantsEEModel.JsonKeyElse, false, graph);
    // add the output
    final String dataNodeId = srcString;
    final DataType dataType = UtilsAfcl.getDataTypeForString(dataOut.getType());
    // retrieve or create the data node
    final Task dataNodeOut = AfclCompounds.assureDataNodePresence(dataNodeId, dataType, graph);
    // create, annotate, and add the dependency to the graph
    PropertyServiceDependency.addDataDependency(choiceFunction, dataNodeOut,
        ConstantsEEModel.JsonKeyIfResult, graph);
    // connect the choice function to the decision variable (normal dependency)
    PropertyServiceDependency.addDataDependency(conditionVariable, choiceFunction,
        ConstantsEEModel.JsonKeyIfDecision, graph);
  }

  /**
   * Checks that sanity of the data out of the if compound. Throws an exception if
   * it detects any entries which don't make sense.
   * 
   * @param dataOut the data out to check
   * @param graph the hitherto created enactment graph
   * @param ifElse the if compound
   */
  protected static void checkDataOutIfSrc(final DataOuts dataOut, final EnactmentGraph graph,
      final Workflow workflow, final IfThenElse ifElse) {
    final String srcString = AfclApiWrapper.getSource(dataOut);
    if (!UtilsAfcl.isIfOutSrc(srcString)) {
      throw new IllegalArgumentException("The src of data out " + AfclApiWrapper.getName(dataOut)
          + " does not look like the out of an if compound.");
    }
    final String firstSrc = UtilsAfcl.getFirstSubStringIfOut(srcString);
    final String dataOutName = AfclApiWrapper.getName(dataOut);
    if (!UtilsAfcl.isSrcString(firstSrc)) {
      throw new IllegalArgumentException(
          "First part of the if data out " + dataOutName + " does not point to a function out.");
    }
    final String secondSrc = UtilsAfcl.getSecondSubStringIfOut(srcString);
    if (!UtilsAfcl.isSrcString(secondSrc)) {
      throw new IllegalArgumentException(
          "Second part of the if data out " + dataOutName + " does not point to a function out.");
    }
    if (graph.getVertex(firstSrc) == null) {
      throw new IllegalStateException("Src of if data out " + firstSrc + " not in the graph");
    }
    if (graph.getVertex(secondSrc) == null && graph
        .getVertex(HierarchyLevellingAfcl.getSrcDataId(secondSrc, ifElse, workflow)) == null) {
      throw new IllegalStateException("Src of if data out " + secondSrc + " not in the graph");
    }
  }

  /**
   * Adds the node modeling the evaluation of the condition function of the if
   * compound. Returns the data node modeling the condition variable.
   * 
   * @param graph the enactment graph
   * @param ifCompound the if compound that is being modeled
   * @param workflow the afcl workflow object
   * @return the data node modeling the condition variable
   */
  protected static Task addConditionFunction(final EnactmentGraph graph,
      final IfThenElse ifCompound, final Workflow workflow) {
    final String nodeId = AfclApiWrapper.getName(ifCompound);
    final List<Condition> conditions = new ArrayList<>();
    final Task funcNode =
        PropertyServiceFunctionUtilityCondition.createConditionEvaluation(nodeId, conditions);
    for (final at.uibk.dps.afcl.functions.objects.Condition afclCondition : ifCompound
        .getCondition()) {
      conditions.add(addConditionNode(graph, afclCondition, funcNode, workflow, ifCompound));
    }
    PropertyServiceFunctionUtilityCondition.setConditions(funcNode, conditions);

    final String decVarId =
        AfclApiWrapper.getName(ifCompound) + ConstantsEEModel.DecisionVariableSuffix;
    // create the decision variable
    final Task decisionVariableNode = new Communication(decVarId);
    PropertyServiceData.setNodeType(decisionVariableNode, NodeType.Decision);
    PropertyServiceDependency.addDataDependency(funcNode, decisionVariableNode,
        ConstantsEEModel.JsonKeyIfDecision, graph);
    return decisionVariableNode;
  }

  /**
   * Adds the data required by the given afcl condition to the enactment graph.
   * Returns the condition object.
   * 
   * @param graph the enactment graph
   * @param condition the afcl condition
   * @param conditionFunction the task modeling the condition function
   */
  protected static Condition addConditionNode(final EnactmentGraph graph,
      final at.uibk.dps.afcl.functions.objects.Condition condition, final Task conditionFunction,
      final Workflow workflow, final Function ifElse) {
    final String firstInput =
        getConditionDataSrc(condition.getData1(), conditionFunction.getId(), workflow, ifElse);
    final String secondInput =
        getConditionDataSrc(condition.getData2(), conditionFunction.getId(), workflow, ifElse);
    final Operator operator = UtilsAfcl.getOperatorForString(condition.getOperator());
    final DataType dataType = UtilsAfcl.getDataTypeForString(condition.getType());
    final Task conditionInFirst = addConditionIn(graph, conditionFunction, firstInput, dataType);
    final Task conditionInSecond = addConditionIn(graph, conditionFunction, secondInput, dataType);
    final CombinedWith combinedWith =
        UtilsAfcl.getCombinedWithForString(condition.getCombinedWith());
    final boolean negation = condition.getNegation();
    return new Condition(conditionInFirst.getId(), conditionInSecond.getId(), operator, negation,
        dataType, combinedWith);
  }

  /**
   * Returns the actual src string for the given condition data.
   * 
   * @param conditionDataString the condition string
   * @param conditionFunctionId the id of the function node evaluating the
   *        condition
   * @param workflow the processed workflow
   * @return the actual src string for the given condition data
   */
  protected static String getConditionDataSrc(final String conditionDataString,
      final String conditionFunctionId, final Workflow workflow, final Function ifElse) {
    if (UtilsAfcl.isSrcString(conditionDataString)) {
      return HierarchyLevellingAfcl.getSrcDataId(conditionDataString, ifElse, workflow);
    } else {
      return conditionDataString;
    }
  }

  /**
   * Adds the data node representing an input (either a constant or sth with a
   * source) for a condition evaluation.
   * 
   * @param graph the enactment graph
   * @param conditionFunction the function node modeling the condition function
   * @param dataString the data string
   * @param dataType the data type
   * @return the created data node
   */
  protected static Task addConditionIn(final EnactmentGraph graph, final Task conditionFunction,
      final String dataString, final DataType dataType) {
    if (UtilsAfcl.isSrcString(dataString)) {
      return addConditionInDefault(graph, conditionFunction, dataString, dataType);
    } else {
      return addConditionInConstant(graph, conditionFunction, dataString, dataType);
    }
  }

  /**
   * Adds the dependency (and the data node) to model the constant data required
   * for the evaluation of the condition function.
   * 
   * @param graph the enactment graph
   * @param conditionFunction the node modeling the condition function
   * @param dataString the src string
   * @param dataType the expected data type
   * @return the created data node
   */
  protected static Task addConditionInConstant(final EnactmentGraph graph,
      final Task conditionFunction, final String dataString, final DataType dataType) {
    final String dataNodeId = conditionFunction.getId() + ConstantsAfcl.SourceAffix + dataString;
    final String jsonKey = dataNodeId;
    final JsonElement content = JsonParser.parseString(dataString);

    final Task result = PropertyServiceData.createConstantNode(dataNodeId, dataType, content);
    PropertyServiceDependency.addDataDependency(result, conditionFunction, jsonKey, graph);
    return result;
  }

  /**
   * Adds the dependency (and the data node) to model the data required for the
   * evaluation of the condition function.
   * 
   * @param graph the enactment graph
   * @param conditionFunction the node modeling the condition function
   * @param dataString the src string
   * @param dataType the expected data type
   * @return the created data node
   */
  protected static Task addConditionInDefault(final EnactmentGraph graph,
      final Task conditionFunction, final String dataString, final DataType dataType) {
    final String jsonKey = dataString;
    final Task result = AfclCompounds.assureDataNodePresence(dataString, dataType, graph);
    PropertyServiceDependency.addDataDependency(result, conditionFunction, jsonKey, graph);
    return result;
  }
}
