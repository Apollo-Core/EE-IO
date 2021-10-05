package at.uibk.dps.ee.io.afcl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonPrimitive;

import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.ParallelFor;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOuts;
import at.uibk.dps.ee.io.UtilsIO;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * Static method container for the methods used when creating the enactment
 * graph parts modeling elements of sequence compounds.
 * 
 * @author Fedor Smirnov
 */
public final class AfclCompoundsParallelFor {

  /**
   * No constructor.
   */
  private AfclCompoundsParallelFor() {}

  /**
   * Adds the nodes modeling the contents of the provided parallel for compound to
   * the provided enactment graph
   * 
   * @param graph the provided enactment graph
   * @param parallelFor the parallelFor compound
   * @param workflow the afcl workflow
   */
  static void addParallelFor(final EnactmentGraph graph, final ParallelFor parallelFor,
      final Workflow workflow) {

    // process the iterators and add the distribute function
    final List<String> iterators =
        parallelFor.getIterators().stream().map(String::trim).collect(Collectors.toList());
    final String distributionId = parallelFor.getName() + ConstantsEEModel.KeywordSeparator1
        + ConstantsEEModel.FuncNameUtilityDistribution;

    final Task distributionNode =
        PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask(distributionId,
            OperationType.Distribution, parallelFor.getName());

    final List<DataIns> dataIns = AfclApiWrapper.getDataIns(parallelFor);

    // remember the while nodes which are there before the for body is added
    Set<Task> whileStartsOutside = AfclCompoundsWhile.getWhileNodesInGraph(graph, true);
    Set<Task> whileCounterOutside = AfclCompoundsWhile.getWhileNodesInGraph(graph, false);

    if (isIntIteratorList(iterators)) {
      // create/find the node providing the iteration number
      processIterator(iterators.get(0), graph, dataIns, distributionNode, parallelFor.getName());

      // create the output of the distribution function
      final String nodeId = distributionNode.getId() + ConstantsEEModel.KeywordSeparator1
          + ConstantsEEModel.JsonKeyConstantIterator;
      final Task intIteratorDist = new Communication(nodeId);
      PropertyServiceData.setDataType(intIteratorDist, DataType.Boolean);
      PropertyServiceDependency.addDataDependency(distributionNode, intIteratorDist,
          ConstantsEEModel.JsonKeyConstantIterator, graph);

      // make the loop body while remembering new nodes
      final Set<Task> functionsBeforeAdding = AfclCompounds.getFunctionNodes(graph);
      AfclCompounds.processTheLoopBody(parallelFor, graph, workflow);
      final Set<Task> functionsAfterAdding = AfclCompounds.getFunctionNodes(graph);
      functionsAfterAdding.removeAll(functionsBeforeAdding);
      // connect all functions to the loop output
      for (final Task bodyFunction : functionsAfterAdding) {
        PropertyServiceDependency.addDataDependency(intIteratorDist, bodyFunction,
            ConstantsEEModel.JsonKeyConstantIterator, graph);
      }
    } else {
      for (final String iterator : iterators) {
        processIterator(iterator, graph, dataIns, distributionNode, parallelFor.getName());
      }
      // process the loop body and remember the new functions
      final Set<Task> functionsBeforeAdding = AfclCompounds.getFunctionNodes(graph);
      AfclCompounds.processTheLoopBody(parallelFor, graph, workflow);
      final Set<Task> functionsAfterAdding = AfclCompounds.getFunctionNodes(graph);
      functionsAfterAdding.removeAll(functionsBeforeAdding);
      // connect the "roots" of the body subgraph to the dist node
      getSubGraphRoots(graph, functionsAfterAdding, distributionNode).forEach(
          subGraphRoot -> connectSubGraphRootToDistNode(graph, distributionNode, subGraphRoot));
    }

    // make sure that the nested while starts suceed the distribution node
    AfclCompoundsWhile.enforceWhileStartOrder(whileStartsOutside, distributionNode, graph, true);
    AfclCompoundsWhile.enforceWhileStartOrder(whileCounterOutside, distributionNode, graph, false);

    // process the data outs and add the aggregate function
    final Optional<List<DataOuts>> dataOuts = Optional.ofNullable(parallelFor.getDataOuts());
    if (dataOuts.isPresent()) {
      for (final DataOuts dataOut : dataOuts.get()) {
        attachAggregatedDataOut(dataOut, graph, parallelFor, workflow);
      }
    }
  }

  /**
   * Connects a subgraph root to its distribution node by means of a sequence node
   * 
   * @param graph the enactment graph
   * @param distributionNode the distribution node
   * @param subRoot the sub graph root
   */
  static void connectSubGraphRootToDistNode(final EnactmentGraph graph, final Task distributionNode,
      final Task subRoot) {
    final String seqId =
        distributionNode.getId() + ConstantsEEModel.KeywordSeparator1 + subRoot.getId();
    final Task seqNode = PropertyServiceData.createSequentialityNode(seqId);
    PropertyServiceDependency.addDataDependency(distributionNode, seqNode,
        ConstantsEEModel.JsonKeySequentiality, graph);
    PropertyServiceDependency.addDataDependency(seqNode, subRoot,
        ConstantsEEModel.JsonKeySequentiality, graph);
  }

  /**
   * Returns the set of tasks which were produced when processing the loop body,
   * but do not have a connection to the distribution node (or other tasks from
   * the subgraph).
   * 
   * @param graph the enactment graph
   * @param subGraphTasks the newly created tasks
   * @param distributionNode the distribution node
   * @return the set of tasks which were produced when processing the loop body,
   *         but do not have a connection to the distribution node (or other tasks
   *         from the subgraph)
   */
  static Set<Task> getSubGraphRoots(final EnactmentGraph graph, final Set<Task> subGraphTasks,
      final Task distributionNode) {
    final Set<Task> result = new HashSet<>(subGraphTasks);
    result.removeIf(task -> !isSubGraphRoot(task, subGraphTasks, graph, distributionNode));
    return result;
  }

  /**
   * Returns true if the given task is a "root" of the subgraph, i.e., it has no
   * connections to either the distribution node or any other task within the
   * subgraph.
   * 
   * @param task the given task
   * @param subGraphTasks the tasks in the subgraph
   * @param graph the graph
   * @param distributionNode the distribution node
   * @return true if the given task is a "root" of the subgraph, i.e., it has no
   *         connections to either the distribution node or any other task within
   *         the subgraph
   */
  static boolean isSubGraphRoot(final Task task, final Set<Task> subGraphTasks,
      final EnactmentGraph graph, final Task distributionNode) {
    for (final Task predecessor : graph.getPredecessors(task)) {
      // iterate the comm predecessors
      if (!TaskPropertyService.isCommunication(predecessor)
          || graph.getPredecessorCount(predecessor) > 1) {
        throw new IllegalStateException(
            "Task " + predecessor.getId() + " should not precede task " + task.getId());
      }
      if (!graph.getPredecessors(predecessor).isEmpty()) {
        // check the task predecessor
        final Task precedingTask = graph.getPredecessors(predecessor).iterator().next();
        if (precedingTask.equals(distributionNode) || subGraphTasks.contains(precedingTask)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Creates the nodes modeling (a) the aggregation function of the data out and
   * (b) the aggregated data. Attaches both nodes to the graph
   * 
   * @param dataOut the processed data out
   * @param graph the enactment graph
   * @param parallelFor the parFor compound
   * @param workflow the workflow
   */
  static void attachAggregatedDataOut(final DataOuts dataOut, final EnactmentGraph graph,
      final ParallelFor parallelFor, final Workflow workflow) {
    // create the aggregation function
    final String parallelForName = parallelFor.getName();
    final String aggregationId = parallelForName + ConstantsEEModel.KeywordSeparator1
        + ConstantsEEModel.FuncNameUtilityAggregation + ConstantsEEModel.KeywordSeparator1
        + dataOut.getName();
    final Task aggregationNode = PropertyServiceFunctionDataFlowCollections
        .createCollectionDataFlowTask(aggregationId, OperationType.Aggregation, parallelForName);
    // find the source and connect the aggregation node to it
    final String srcString =
        HierarchyLevellingAfcl.getSrcDataId(dataOut.getSource(), parallelFor, workflow);
    final Task dataToAggregate = Optional.ofNullable(graph.getVertex(srcString)).orElseThrow(
        () -> new IllegalStateException("Cannot find data to aggregate: " + srcString));
    PropertyServiceDependency.addDataDependency(dataToAggregate, aggregationNode,
        ConstantsEEModel.JsonKeyAggregation, graph);
    // create the node for the aggregated data
    final DataType dataType = UtilsAfcl.getDataTypeForString(dataOut.getType());
    if (!dataType.equals(DataType.Collection)) {
      throw new IllegalStateException("The data out of a parallel for must be a collection.");
    }
    final String aggregatedId = parallelForName + ConstantsAfcl.SourceAffix + dataOut.getName();
    final Task aggregatedData = new Communication(aggregatedId);
    PropertyServiceData.setDataType(aggregatedData, dataType);
    // connect it to the aggregation function
    PropertyServiceDependency.addDataDependency(aggregationNode, aggregatedData,
        ConstantsEEModel.JsonKeyAggregation, graph);
  }

  /**
   * Returns true iff the given list specifies a number-driven execution.
   * 
   * @param iterators the iterator list
   * @return true iff the given list specifies a number-driven execution
   */
  static boolean isIntIteratorList(final List<String> iterators) {
    if (iterators.size() == 1) {
      return isIntIterator(iterators.get(0));
    } else if (iterators.size() > 1) {
      if (iterators.stream().anyMatch(iterator -> isIntIterator(iterator))) {
        throw new IllegalArgumentException(
            "Int iterators are only allowed in a list with a single entry.");
      }
      return false;
    } else {
      throw new IllegalArgumentException("Empty Iterator list provided.");
    }
  }

  /**
   * Returns true iff the given iterator defines the iteration number rather than
   * a collection to iterate over.
   * 
   * @param iterator the given iterator
   * @return true iff the given iterator defines the iteration number rather than
   *         a collection to iterate over
   */
  static boolean isIntIterator(final String iterator) {
    return UtilsIO.readableAsInt(iterator) || UtilsAfcl.isSrcString(iterator);
  }

  /**
   * Processes the given iterator string by adding the necessary nodes to the
   * graph.
   * 
   * @param iterator the iterator string
   * @param graph the enactment graph
   * @param dataIns the list of data ins
   * @param distributionNode the node modeling the distribution operation.
   */
  static void processIterator(final String iterator, final EnactmentGraph graph,
      final List<DataIns> dataIns, final Task distributionNode, final String parallelForName) {
    // connect the data to distribute
    if (UtilsAfcl.isSrcString(iterator)) {
      // iterator from source
      final Task inputData = AfclCompounds.assureDataNodePresence(iterator, DataType.Number, graph);
      PropertyServiceDependency.addDataDependency(inputData, distributionNode,
          ConstantsEEModel.JsonKeyConstantIterator, graph);
    } else if (UtilsIO.readableAsInt(iterator)) {
      // iterator from constant number
      final int content = UtilsIO.readAsInt(iterator);
      final String jsonKey = ConstantsEEModel.JsonKeyConstantIterator;
      final Task dataTask = PropertyServiceData.createConstantNode(
          distributionNode.getId() + ConstantsEEModel.KeywordSeparator1 + jsonKey, DataType.Number,
          new JsonPrimitive(content));
      PropertyServiceDependency.addDataDependency(dataTask, distributionNode, jsonKey, graph);
    } else {
      // iterator from a parallelFor dataIn
      if (!dataIns.stream().anyMatch(dataIn -> dataIn.getName().equals(iterator))) {
        throw new IllegalStateException("No dataIns for the iterator " + iterator);
      }
      final DataIns dataInIterator =
          dataIns.stream().filter(dataIn -> dataIn.getName().equals(iterator)).findAny().get();
      AfclCompounds.addDataInDefault(graph, distributionNode, dataInIterator, DataType.Collection);
      final String distributedDataId =
          parallelForName + ConstantsAfcl.SourceAffix + dataInIterator.getName();
      final DataType dataType = UtilsAfcl.getDataTypeForString(dataInIterator.getType());
      final Task distributedData =
          AfclCompounds.assureDataNodePresence(distributedDataId, dataType, graph);
      final String jsonKey = dataInIterator.getName();
      PropertyServiceDependency.addDataDependency(distributionNode, distributedData, jsonKey,
          graph);
    }
  }
}
