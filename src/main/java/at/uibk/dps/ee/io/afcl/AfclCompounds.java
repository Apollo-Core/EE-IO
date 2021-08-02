package at.uibk.dps.ee.io.afcl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    final String dataNodeId =
        function.getId() + ConstantsEEModel.ConstantNodeAffix + ConstantsAfcl.SourceAffix + jsonKey;
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
      DataType actual = PropertyServiceData.getDataType(result);
      if (!actual.equals(dataType)) {
        if (actual.equals(DataType.Collection) && dataType.equals(DataType.Number)) {
          return result;
        } else {
          throw new IllegalStateException("The type specified by node " + dataNodeId
              + " does not match the type expected by a requestor/producer");
        }
      }
    }
    return result;
  }

  /**
   * Parses the given workflow and extracts the while relations of the functions,
   * if any.
   * 
   * @param workFlow the given workflow
   * @return a map mapping the function IDs to their {@link WhileInputReference}s
   */
  protected static Map<String, Set<WhileInputReference>> parseWhileRelations(Workflow workFlow) {
    Map<String, Set<WhileInputReference>> resultMap = new HashMap<>();
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
  protected static void processFunctionListRefs(List<Function> functions, Workflow workflow,
      Map<String, Set<WhileInputReference>> references) {
    for (Function function : functions) {
      if (function instanceof AtomicFunction) {
        Set<WhileInputReference> functionRefs =
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
  protected static void processCompoundRefs(Map<String, Set<WhileInputReference>> references,
      Compound compound, Workflow workflow) {
    List<Function> functions = new ArrayList<>();
    if (compound instanceof ParallelFor) {
      ParallelFor parFor = (ParallelFor) compound;
      functions.addAll(parFor.getLoopBody());
    } else if (compound instanceof While) {
      While whileCom = (While) compound;
      functions.addAll(whileCom.getLoopBody());
    } else if (compound instanceof IfThenElse) {
      IfThenElse ifComp = (IfThenElse) compound;
      functions.addAll(ifComp.getThenBranch());
      functions.addAll(ifComp.getElseBranch());
    } else {
      throw new IllegalStateException(
          "Compound " + compound.getName() + " has an unknown compound type.");
    }
    processFunctionListRefs(functions, workflow, references);
  }

  /**
   * Checks the inputs of the given atomic function and returns the references to
   * its first and further sources.
   * 
   * @param function the atomic function
   * @param workflow the overall workflow
   * @return the set of the function's {@link WhileInputReference}s
   */
  protected static Set<WhileInputReference> processAtomicFunctionForWhileRefs(
      AtomicFunction function, Workflow workflow) {
    Set<WhileInputReference> result = new HashSet<>();
    for (DataIns dataIn : function.getDataIns()) {
      String source = dataIn.getSource();
      if (!UtilsAfcl.isSrcString(source)) {
        continue;
      }
      String srcFunctionName = UtilsAfcl.getProducerId(source);
      if (srcFunctionName.equals(workflow.getName())) {
        continue;
      }
      Function srcFunction = AfclApiWrapper.getFunction(workflow, srcFunctionName);
      if (srcFunction instanceof While) {
        While whileFunc = (While) srcFunction;
        String dataName = UtilsAfcl.getDataId(source);
        result.add(getInputReference(whileFunc, dataName, workflow));
      }
    }
    return result;
  }

  /**
   * Generates the while input reference for the given data in of the given while
   * function.
   * 
   * @param whileFunction the given while function
   * @param dataName the data in name
   * @param workflow the overall workflow
   * @return the while input reference for the given data in of the given while
   *         function
   */
  protected static WhileInputReference getInputReference(While whileFunction, String dataName,
      Workflow workflow) {
    // find the actual source of the While's data in
    Optional<DataIns> dataInOpt = Optional.empty();
    for (DataIns dataIn : whileFunction.getDataIns()) {
      if (dataIn.getName().equals(dataName)) {
        dataInOpt = Optional.of(dataIn);
        break;
      }
    }
    DataIns srcDataIn = dataInOpt.orElseThrow(() -> new IllegalArgumentException("While function "
        + whileFunction.getName() + " does not have a data in with the name " + dataName));
    String srcStringDataIn = srcDataIn.getSource();
    String actualSrcDataIn = UtilsAfcl.isSrcString(srcStringDataIn)
        ? HierarchyLevellingAfcl.getSrcDataId(srcStringDataIn, workflow)
        : srcStringDataIn;
    // find the actual source of the While's data out
    Optional<DataOuts> dataOutOpt = Optional.empty();
    for (DataOuts dataOut : whileFunction.getDataOuts()) {
      if (dataOut.getName().equals(dataName)) {
        dataOutOpt = Optional.of(dataOut);
        break;
      }
    }
    DataOuts srcDataOut =
        dataOutOpt.orElseThrow(() -> new IllegalArgumentException("While function "
            + whileFunction.getName() + " does not have a data out with the name " + dataName));
    String srcStringDataOut = srcDataOut.getSource();
    String actualSrcDataOut = UtilsAfcl.isSrcString(srcStringDataOut)
        ? HierarchyLevellingAfcl.getSrcDataId(srcStringDataOut, workflow)
        : srcStringDataOut;
    return new WhileInputReference(actualSrcDataIn, actualSrcDataOut);
  }
}
