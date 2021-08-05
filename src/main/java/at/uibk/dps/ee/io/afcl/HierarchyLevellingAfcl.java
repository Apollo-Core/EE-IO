package at.uibk.dps.ee.io.afcl;

import java.util.Optional;
import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.IfThenElse;
import at.uibk.dps.afcl.functions.ParallelFor;
import at.uibk.dps.afcl.functions.While;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOuts;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;

/**
 * Static method container with methods used to flatten AFCL's compound
 * hierarchy while eliminating the redundant data description.
 * 
 * @author Fedor Smirnov
 */
public final class HierarchyLevellingAfcl {

  /**
   * No constructor
   */
  private HierarchyLevellingAfcl() {}

  /**
   * Returns the data id corresponding to the provided source string
   * 
   * @param afclSource the src string in the afcl file
   * @param funcWithSrc the function whose source is being determined
   * @param workflow the workflow built based on the afcl file
   * @return the data id in the flattened graph
   */
  public static String getSrcDataId(final String afclSource, final Function funcWithSrc,
      final Workflow workflow) {
    final String funcName = UtilsAfcl.getProducerId(afclSource);
    if (funcName.equals(workflow.getName())) {
      // pointing to a root node
      return afclSource;
    }

    final String dataName = UtilsAfcl.getDataId(afclSource);
    final Function function = AfclApiWrapper.getFunction(workflow, funcName);
    if (function instanceof AtomicFunction) {
      // pointing to the output of an atomic function
      checkAtomicFunctionOut((AtomicFunction) function, dataName);
      return afclSource;
    } else if (function instanceof IfThenElse) {
      return getSrcDataIdIfThenElse(afclSource, dataName, function, workflow, funcWithSrc);
    } else if (function instanceof ParallelFor) {
      final ParallelFor parFor = (ParallelFor) function;
      return getSrcDataIdParallelFor(parFor, afclSource, dataName, workflow, funcWithSrc);
    } else if (function instanceof While) {
      return getDataIdWhile(afclSource, dataName, (While) function, workflow, funcWithSrc);
    } else {
      throw new IllegalStateException(
          "Not yet implemented for " + function.getClass().getCanonicalName());
    }
  }

  /**
   * Returns the id of the correct data node described by the given src string
   * (pointing to a while compound).
   * 
   * @param afclSource the src string
   * @param dataName the data id
   * @param whileCompound the while compound
   * @param workflow the whole workflow
   * @param funcWithSrc the function whose src we are looking for
   * @return id of the correct data node described by the given src string
   *         (pointing to a while compound)
   */
  protected static String getDataIdWhile(final String afclSource, final String dataName,
      final While whileCompound, final Workflow workflow, final Function funcWithSrc) {
    // looking for the while counter
    if (dataName.equals(ConstantsEEModel.WhileLoopCounterSuffix)) {
      return afclSource;
    }
    Optional<String> optRes = Optional.empty();
    if (funcWithSrc == null || !AfclApiWrapper.contains(whileCompound, funcWithSrc)) {
      // case where the request comes from outside the while compound (inclusive the
      // case where we are looking for the src of the WF data out)
      if (dataOutWithNameExists(whileCompound, dataName)) {
        optRes = Optional.of(afclSource);
      }
    } else {
      // case where the requesting function is within the while compound
      optRes = getDataIdFromWhileDataIn(whileCompound, dataName, funcWithSrc, workflow);
    }
    return optRes.orElseThrow(() -> new IllegalStateException("The src string " + afclSource
        + " does not point to any valid port of the while compound " + whileCompound.getName()));
  }

  /**
   * Finds the data id of the source of a function which is within the while
   * compound.
   * 
   * @param whileCompound the while compound
   * @param dataName the data name
   * @param funcWithSrc the function within the while compound
   * @param workflow the workflow
   * @return the data id of the source of a function which is within the while
   *         compound
   */
  protected static Optional<String> getDataIdFromWhileDataIn(While whileCompound, String dataName,
      Function funcWithSrc, Workflow workflow) {
    Optional<String> result = Optional.empty();
    for (final DataIns dataIn : whileCompound.getDataIns()) {
      if (dataIn.getName().equals(dataName)) {
        final String srcString = dataIn.getSource();
        if (UtilsAfcl.isSrcString(srcString)) {
          result = Optional.of(getSrcDataId(dataIn.getSource(), funcWithSrc, workflow));
        } else {
          // Constant case
          result = Optional.of(ConstantsEEModel.ConstantNodeAffix + "/" + srcString);
        }
      }
    }
    return result;
  }

  /**
   * Checks whether the given while compound has a data out with the given name.
   * 
   * @param whileCompound the while compound
   * @param dataName the name
   * @return true iff the given name is found
   */
  protected static boolean dataOutWithNameExists(final While whileCompound, final String dataName) {
    for (final DataOuts dataOut : whileCompound.getDataOuts()) {
      if (dataOut.getName().equals(dataName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the corrected string for the case where the afcl string points to an
   * IF compound.
   * 
   * @param afclSource the afcl source string
   * @param dataName the name of the data the src string points to
   * @param function the if compound
   * @param workflow the workflow
   * @return the corrected string for the case where the afcl string points to an
   *         IF compound
   */
  protected static String getSrcDataIdIfThenElse(final String afclSource, final String dataName,
      final Function ifFunction, final Workflow workflow, final Function funcWithSrc) {
    if (AfclApiWrapper.pointsToInput(afclSource, ifFunction)) {
      // points to data in
      return getSrcDataId(AfclApiWrapper.getDataInSrc(ifFunction, dataName), funcWithSrc, workflow);
    } else {
      // points to data out of if compound => there should be a data node with the
      // data out src as id
      return AfclApiWrapper.getDataOutSrc(ifFunction, dataName);
    }
  }

  /**
   * Returns the correct source string for the case where the afcl string points
   * to a parallel for compound.
   * 
   * @param parFor the parallel for function
   * @param sourceString the parallel for string
   * @param dataName the name of the data the string points to
   * @param workflow the afcl workflow object
   * @return the correct source string for the case where the afcl string points
   *         to a parallel for compound
   */
  protected static String getSrcDataIdParallelFor(final ParallelFor parFor,
      final String sourceString, final String dataName, final Workflow workflow,
      final Function funcWithSrc) {
    if (AfclApiWrapper.pointsToInput(sourceString, parFor)) {
      // parallel for data in
      if (parFor.getIterators().contains(dataName)) {
        // distribution node's id should match the source
        return sourceString;
      } else {
        // backtrack to producer
        return getSrcDataId(AfclApiWrapper.getDataInSrc(parFor, dataName), funcWithSrc, workflow);
      }
    } else {
      // the aggregated data node's ID should match the src String
      return sourceString;
    }
  }

  /**
   * Checks that the given atomic function has an output with the provided name
   * throws an exception if this is not the case.
   * 
   * @param atomic the given atomic function
   * @param dataOutName the name of the data out
   */
  protected static void checkAtomicFunctionOut(final AtomicFunction atomic,
      final String dataOutName) {
    for (final DataOutsAtomic dataOut : AfclApiWrapper.getDataOuts(atomic)) {
      if (dataOutName.equals(dataOut.getName())) {
        return;
      }
    }
    throw new IllegalArgumentException(
        "The atomic function " + atomic.getName() + " has no data out named " + dataOutName);
  }
}
