package at.uibk.dps.ee.io.afcl;

import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUser;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import net.sf.opendse.model.Task;

/**
 * Static method container for the methods used to add the nodes modeling atomic
 * functions to the enactment graph.
 * 
 * @author Fedor Smirnov
 */
public final class AfclCompoundsAtomic {

  /**
   * No constructor.
   */
  private AfclCompoundsAtomic() {}

  /**
   * Adds the elements of the provided atomic function to the graph. This method
   * assumes that we are on the WF level (the sources of the functions can be
   * directly mapped to nodes).
   * 
   * @param graph the graph
   * @param atomicFunc the provided atomic function
   */
  protected static void addAtomicFunctionWfLevel(final EnactmentGraph graph,
      final AtomicFunction atomicFunc) {
    final Task atomicTask = createTaskFromAtomicFunction(atomicFunc);
    // process the inputs
    for (final DataIns dataIn : AfclApiWrapper.getDataIns(atomicFunc)) {
      correctDataIn(dataIn);
      final DataType expectedType = UtilsAfcl.getDataTypeForString(dataIn.getType());
      AfclCompounds.addDataIn(graph, atomicTask, dataIn, expectedType);
    }
    // process the outputs
    for (final DataOutsAtomic dataOut : AfclApiWrapper.getDataOuts(atomicFunc)) {
      addDataOut(graph, atomicTask, dataOut);
    }
  }

  /**
   * Checks whether the given data in (at wf level) references a constant and
   * corrects the data in src accordingly
   * 
   * @param dataIn the data in
   */
  protected static void correctDataIn(final DataIns dataIn) {
    if (!UtilsAfcl.isSrcString(dataIn.getSource())) {
      final String corrected =
          ConstantsEEModel.ConstantNodeAffix + ConstantsAfcl.SourceAffix + dataIn.getSource();
      dataIn.setSource(corrected);
    }
  }

  /**
   * Adds the node modeling the given atomic function (which is described on a
   * compound level, i.e., not the highest level of the workflow) to the enactment
   * graph.
   * 
   * @param graph the enactment graph
   * @param atomic the atomic function
   * @param workflow the afcl workflow object
   */
  protected static void addAtomicFunctionSubWfLevel(final EnactmentGraph graph,
      final AtomicFunction atomic, final Workflow workflow) {
    correctAtomicDataIns(atomic, workflow);
    addAtomicFunctionWfLevel(graph, atomic);
  }

  /**
   * Processes the given data out by adding an edge (if data already in graph) or
   * an edge and a data node (if data not yet in graph).
   * 
   * @param graph the enactment graph
   * @param function the node modeling the atomic function with given data out
   * @param dataOut the given data out
   */
  protected static void addDataOut(final EnactmentGraph graph, final Task function,
      final DataOutsAtomic dataOut) {
    final String functionName = function.getId();
    final String jsonKey = AfclApiWrapper.getName(dataOut);
    final String dataNodeId = UtilsAfcl.getDataNodeId(functionName, jsonKey);
    final DataType dataType = UtilsAfcl.getDataTypeForString(dataOut.getType());
    // retrieve or create the data node
    final Task dataNodeOut = AfclCompounds.assureDataNodePresence(dataNodeId, dataType, graph);
    PropertyServiceDependency.addDataDependency(function, dataNodeOut, jsonKey, graph);
  }

  /**
   * Corrects the data in of the given atomic function to point directly to the
   * data input.
   * 
   * @param function the atomic function
   * @param workflow the afcl workflow object
   */
  protected static void correctAtomicDataIns(final AtomicFunction function,
      final Workflow workflow) {
    for (final DataIns dataIn : AfclApiWrapper.getDataIns(function)) {
      final String srcString = dataIn.getSource();
      if (!UtilsAfcl.isSrcString(srcString)) {
        // constant data in
        continue;
      }
      final String actualSrc = HierarchyLevellingAfcl.getSrcDataId(srcString, function, workflow);
      dataIn.setSource(actualSrc);
    }
  }

  /**
   * Creates a task node from the given atomic function.
   * 
   * @param atomFunc the given atomic function
   * @return the task node modeling the given atomic function.
   */
  protected static Task createTaskFromAtomicFunction(final AtomicFunction atomFunc) {
    final String funcId = atomFunc.getName();
    final String functionTypeString = atomFunc.getType();
    return PropertyServiceFunctionUser.createUserTask(funcId, functionTypeString);
  }

}
