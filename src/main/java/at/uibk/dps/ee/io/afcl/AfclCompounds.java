package at.uibk.dps.ee.io.afcl;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.IfThenElse;
import at.uibk.dps.afcl.functions.LoopCompound;
import at.uibk.dps.afcl.functions.ParallelFor;
import at.uibk.dps.afcl.functions.While;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * Static method container for the construction of the graph structures modeling
 * different AFCL compound.
 * 
 * @author Fedor Smirnov
 *
 */
public final class AfclCompounds {

  /**
   * No constructor.
   */
  private AfclCompounds() {}

  /**
   * Adds and annotates the elements which model the given function to the graph.
   * 
   * @param graph the graph to annotate
   * @param function the function to model
   * @param workflow the afcl workflow object
   */
  public static void addFunctionCompound(final EnactmentGraph graph, final Function function,
      final Workflow workflow) {
    switch (UtilsAfcl.getCompoundType(function)) {
      case Atomic: {
        AfclCompoundsAtomic.addAtomicFunctionWfLevel(graph, (AtomicFunction) function);
        return;
      }
      case If: {
        AfclCompoundsIf.addIf(graph, (IfThenElse) function, workflow);
        break;
      }
      case ParallelFor: {
        AfclCompoundsParallelFor.addParallelFor(graph, (ParallelFor) function, workflow);
        break;
      }
      case While: {
        AfclCompoundsWhile.addWhile(graph, (While) function, workflow);
        break;
      }
      default:
        throw new IllegalArgumentException(
            "Unexpected value: " + UtilsAfcl.getCompoundType(function));
    }
  }

  /**
   * Processes the loop body and adds all nodes.
   * 
   * @param loopCompound the loop compound
   * @param graph the enactment graph
   * @param workflow the workflow
   */
  protected static void processTheLoopBody(final LoopCompound loopCompound,
      final EnactmentGraph graph, final Workflow workflow) {
    // process the loop body
    for (final Function function : loopCompound.getLoopBody()) {
      if (function instanceof AtomicFunction) {
        AfclCompoundsAtomic.addAtomicFunctionSubWfLevel(graph, (AtomicFunction) function, workflow);
      } else {
        addFunctionCompound(graph, function, workflow);
      }
    }
  }

  /**
   * Returns a set of all function nodes in the given graph.
   * 
   * @param graph the given graph
   * @return a set of all function nodes in the given graph
   */
  protected static Set<Task> getFunctionNodes(final EnactmentGraph graph) {
    final Set<Task> result = new HashSet<>();
    for (final Task task : graph) {
      if (TaskPropertyService.isProcess(task)) {
        result.add(task);
      }
    }
    return result;
  }

  /**
   * Processes the given dataIn: generates a data node and connects it to the
   * function node.
   * 
   * @param graph the enactment graph
   * @param function the node modeling the function with the given data in
   * @param dataIn the given data in (representing a constant input)
   * @param expectedType the data type we expect the node to have
   */
  protected static void addDataIn(final EnactmentGraph graph, final Task function,
      final DataIns dataIn, final DataType expectedType) {
    if (UtilsAfcl.isSrcString(AfclApiWrapper.getSource(dataIn))) {
      AfclCompounds.addDataInDefault(graph, function, dataIn, expectedType);
    } else {
      AfclCompounds.addDataInConstant(graph, function, dataIn, expectedType);
    }
  }

  /**
   * Processes the given dataIn representing constant data: generates a constant
   * data node and connects it to the function node.
   * 
   * @param graph the enactment graph
   * @param function the node modeling the function with the given data in
   * @param dataIn the given data in (representing a constant input)
   * @param expectedType the datatype we expect the node to have
   */
  protected static void addDataInConstant(final EnactmentGraph graph, final Task function,
      final DataIns dataIn, final DataType expectedType) {
    final String jsonKey = AfclApiWrapper.getName(dataIn);
    final DataType dataType = UtilsAfcl.getDataTypeForString(dataIn.getType());
    final String jsonString = AfclApiWrapper.getSource(dataIn);
    final JsonElement content = JsonParser.parseString(jsonString);
    final String dataNodeId = function.getId() + 
        ConstantsEEModel.ConstantNodeAffix + ConstantsAfcl.SourceAffix + jsonKey;
    final Task constantDataNode =
        PropertyServiceData.createConstantNode(dataNodeId, dataType, content);
    PropertyServiceDependency.addDataDependency(constantDataNode, function, jsonKey, graph);
  }

  /**
   * Processes the given data in by adding an edge (if data already in graph) or
   * an edge and a data node (if data not yet in graph) to the graph.
   * 
   * @param graph the enactment graph
   * @param function the node modeling the function with the given data in
   * @param dataIn the given data in
   * @param expectedType the type expected in the node we are looking for
   */
  protected static void addDataInDefault(final EnactmentGraph graph, final Task function,
      final DataIns dataIn, final DataType expectedType) {
    // create/retrieve the data node
    final String dataNodeId = AfclApiWrapper.getSource(dataIn);
    final String srcFunc = UtilsAfcl.getProducerId(dataNodeId);
    if (srcFunc.equals(function.getId())) {
      throw new IllegalStateException("Function " + function.getId() + " depends on itself.");
    }
    final String jsonKey = AfclApiWrapper.getName(dataIn);
    // retrieve or create the data node
    final Task dataNodeIn = assureDataNodePresence(dataNodeId, expectedType, graph);
    Task connectsToFunction = dataNodeIn;
    // check whether we have any collection operations
    if (AfclCollectionOperations.hasCollectionOperations(dataIn)) {
      final DataType expectedDataType = UtilsAfcl.getDataTypeForString(dataIn.getType());
      connectsToFunction = AfclCollectionOperations.modelCollectionOperations(dataIn, dataNodeIn,
          graph, expectedDataType);
    }
    // connect the current node to the function
    PropertyServiceDependency.addDataDependency(connectsToFunction, function, jsonKey, graph);
  }

  /**
   * If a node with the given id is not in the graph, creates the node. Otherwise
   * checks that the node has the specified type. Returns the created/retrieved
   * node.
   * 
   * @param dataNodeId the id of the data node
   * @param dataType the data type of the node
   * @param graph the enactment graph
   * @return the created/retrieved node
   */
  protected static Task assureDataNodePresence(final String dataNodeId, final DataType dataType,
      final EnactmentGraph graph) {
    Task result = graph.getVertex(dataNodeId);
    if (result == null) {
      result = new Communication(dataNodeId);
      PropertyServiceData.setDataType(result, dataType);
    } else {
      if (!PropertyServiceData.getDataType(result).equals(dataType)) {
        throw new IllegalStateException("The type specified by node " + dataNodeId
            + " does not match the type expected by a requestor/producer");
      }
    }
    return result;
  }
}
