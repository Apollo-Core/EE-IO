package at.uibk.dps.ee.io.afcl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.Compound;
import at.uibk.dps.afcl.functions.IfThenElse;
import at.uibk.dps.afcl.functions.LoopCompound;
import at.uibk.dps.afcl.functions.ParallelFor;
import at.uibk.dps.afcl.functions.While;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOuts;
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
  static void processTheLoopBody(final LoopCompound loopCompound, final EnactmentGraph graph,
      final Workflow workflow) {
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
  static Set<Task> getFunctionNodes(final EnactmentGraph graph) {
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
  static void addDataIn(final EnactmentGraph graph, final Task function, final DataIns dataIn,
      final DataType expectedType) {
    if (UtilsAfcl.isConstantSrcString(AfclApiWrapper.getSource(dataIn))) {
      AfclCompounds.addDataInConstant(graph, function, dataIn, expectedType);
    } else {
      AfclCompounds.addDataInDefault(graph, function, dataIn, expectedType);
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
  static void addDataInConstant(final EnactmentGraph graph, final Task function,
      final DataIns dataIn, final DataType expectedType) {
    final String constString = UtilsAfcl.getDataId(dataIn.getSource());
    final String jsonKey = dataIn.getName();
    final DataType dataType = UtilsAfcl.getDataTypeForString(dataIn.getType());
    final JsonElement content = JsonParser.parseString(constString);
    final String dataNodeId = dataIn.getSource();
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
  static void addDataInDefault(final EnactmentGraph graph, final Task function,
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
  static Task assureDataNodePresence(final String dataNodeId, final DataType dataType,
      final EnactmentGraph graph) {
    if (graph.containsVertex(dataNodeId)) {
      final Task result = graph.getVertex(dataNodeId);
      final DataType actual = PropertyServiceData.getDataType(result);
      if (!actual.equals(dataType)) {
        if (actual.equals(DataType.Collection) && dataType.equals(DataType.Number)) {
          return result;
        } else {
          throw new IllegalStateException("The type specified by node " + dataNodeId
              + " does not match the type expected by a requestor/producer");
        }
      }
      return result;
    } else {
      final Task result = new Communication(dataNodeId);
      PropertyServiceData.setDataType(result, dataType);
      return result;
    }
  }

  /**
   * Parses the given workflow and extracts the while relations of the functions,
   * if any.
   * 
   * @param workFlow the given workflow
   * @return a map mapping the function IDs to their {@link WhileInputReference}s
   */
  static Map<String, Set<WhileInputReference>> parseWhileRelations(final Workflow workFlow) {
    final Map<String, Set<WhileInputReference>> resultMap = new ConcurrentHashMap<>();
    processFunctionListRefs(workFlow.getWorkflowBody(), workFlow, resultMap);
    return resultMap;
  }

  /**
   * Processes the given list of functions and finds the while references for each
   * of them.
   * 
   * @param functions the list of functions
   * @param workflow the overall workflow
   * @param references the map of function while references
   */
  static void processFunctionListRefs(final List<Function> functions, final Workflow workflow,
      final Map<String, Set<WhileInputReference>> references) {
    for (final Function function : functions) {
      if (function instanceof AtomicFunction) {
        final Set<WhileInputReference> functionRefs =
            processAtomicFunctionForWhileRefs((AtomicFunction) function, workflow);
        if (!functionRefs.isEmpty()) {
          references.put(function.getName(), functionRefs);
        }
      } else if (function instanceof Compound) {
        processCompoundRefs(references, (Compound) function, workflow);
      } else {
        throw new IllegalStateException(
            "Function " + function.getName() + " is neither atomic nor compound.");
      }
    }
  }

  /**
   * Processes the given compound and finds all while references for the atomic
   * functions contained therein.
   * 
   * @param references the map of function while references
   * @param compound the processed compound
   * @param workflow the overall workflow
   */
  static void processCompoundRefs(final Map<String, Set<WhileInputReference>> references,
      final Compound compound, final Workflow workflow) {
    final List<Function> functions = new ArrayList<>();
    if (compound instanceof ParallelFor) {
      final ParallelFor parFor = (ParallelFor) compound;
      functions.addAll(parFor.getLoopBody());
    } else if (compound instanceof While) {
      final While whileCom = (While) compound;
      functions.addAll(whileCom.getLoopBody());
    } else if (compound instanceof IfThenElse) {
      final IfThenElse ifComp = (IfThenElse) compound;
      functions.addAll(ifComp.getThenBranch());
      if (ifComp.getElseBranch() != null) {
        functions.addAll(ifComp.getElseBranch());
      }
    } else {
      throw new IllegalStateException(
          "Compound " + compound.getName() + " has an unknown compound type.");
    }
    processFunctionListRefs(functions, workflow, references);
  }


  /**
   * Gathers the while references by traversing the dataIns from inner to outer
   * compounds.
   * 
   * @param initialSource the source which the data in eventually points to (used
   *        in the 1st iteration of the while)
   * @param curSrcString the current src string (the position within the workflow)
   * @param curFunction the current function we are at in the workflow
   * @param workflow the workflow
   * @param result the set of the while references (filled by running this method)
   */
  static void gatherWhileRefsRec(final String initialSource, final String curSrcString,
      final Function curFunction, final Workflow workflow, final Set<WhileInputReference> result) {
    // base case
    if (!pointsToOuterFunction(curSrcString, workflow)) {
      return;
    }
    final String producerId = UtilsAfcl.getProducerId(curSrcString);
    final Function function = AfclApiWrapper.getFunction(workflow, producerId);
    if (AfclApiWrapper.contains(function, curFunction)) {
      // pointing to a function one level up -> continue
      final String dataId = UtilsAfcl.getDataId(curSrcString);
      if (function instanceof While) {
        final String referenceSrc = getNamedDataOut(function, dataId).getSource();
        // add while input reference
        final WhileInputReference inputReference =
            new WhileInputReference(initialSource, referenceSrc, function.getName());
        result.add(inputReference);
      }
      final String nextSrcString = getNamedDataIn(function, dataId).getSource();
      // continue on the next level
      gatherWhileRefsRec(initialSource, nextSrcString, function, workflow, result);
    } else {
      // pointing to an output on the same level
      return;
    }
  }

  static DataIns getNamedDataIn(final Function function, final String name) {
    Optional<DataIns> dIn = Optional.empty();
    for (final DataIns dataIn : AfclApiWrapper.getDataIns(function)) {
      if (dataIn.getName().equals(name)) {
        dIn = Optional.of(dataIn);
        break;
      }
    }
    return dIn.get();
  }

  static DataOuts getNamedDataOut(final Function function, final String name) {
    Optional<DataOuts> dOut = Optional.empty();
    for (final DataOuts dataOut : AfclApiWrapper.getDataOuts(function)) {
      if (dataOut.getName().equals(name)) {
        dOut = Optional.of(dataOut);
        break;
      }
    }
    return dOut.get();
  }


  /**
   * Checks the inputs of the given atomic function and returns the references to
   * its first and further sources.
   * 
   * @param function the atomic function
   * @param workflow the overall workflow
   * @return the set of the function's {@link WhileInputReference}s
   */
  static Set<WhileInputReference> processAtomicFunctionForWhileRefs(final AtomicFunction function,
      final Workflow workflow) {
    final Set<WhileInputReference> result = new HashSet<>();
    for (final DataIns dataIn : AfclApiWrapper.getDataIns(function)) {
      final String srcString = getActualSrc(dataIn.getSource(), function, workflow);
      final Set<WhileInputReference> inputRefs = new HashSet<>();
      gatherWhileRefsRec(srcString, dataIn.getSource(), function, workflow, inputRefs);
      result.addAll(inputRefs);
    }
    return result;
  }

  /**
   * Returns true if the given data in points to an enclosing function.
   * 
   * @param srcString the source string of the dataIn/dataOut
   * @param workflow the workflow
   * @return true if the given data in points to an enclosing function
   */
  static boolean pointsToOuterFunction(final String srcString, final Workflow workflow) {
    if (!UtilsAfcl.isSrcString(srcString)) {
      // Not pointing to data source -> irrelevant
      return false;
    }
    final String srcFunctionName = UtilsAfcl.getProducerId(srcString);
    return !srcFunctionName.equals(workflow.getName());
  }

  /**
   * Returns the actual src that the given file src string points to.
   * 
   * @param srcString the src string from the AFCL file
   * @param innerFunction the function with the src string
   * @param workflow the workflow
   * @return the actual src that the given file src string points to
   */
  static String getActualSrc(final String srcString, final Function innerFunction,
      final Workflow workflow) {
    return UtilsAfcl.isSrcString(srcString)
        ? HierarchyLevellingAfcl.getSrcDataId(srcString, innerFunction, workflow)
        : srcString;
  }
}
