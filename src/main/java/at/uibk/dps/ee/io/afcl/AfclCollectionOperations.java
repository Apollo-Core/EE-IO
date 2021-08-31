package at.uibk.dps.ee.io.afcl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections.CollectionOperation;
import net.sf.opendse.model.Task;

/**
 * Static method container with methods used to model the operations
 * transforming collections.
 * 
 * @author Fedor Smirnov
 */
public final class AfclCollectionOperations {

  private AfclCollectionOperations() {}

  /**
   * Processes the dataIn's list of collections constraints and adjusts the graph
   * accordingly. Returns the data node with the processed data.
   * 
   * @param dataIn the given dataIn
   * @param originalData the node modeling the collection data before the
   *        processing
   * @param graph the graph
   * @param finalDataType the data type ultimately expected by the consumer
   *        function
   * @return the data node with the processed data
   */
  public static Task modelCollectionOperations(final DataIns dataIn, final Task originalData,
      final EnactmentGraph graph, final DataType finalDataType) {
    if (!PropertyServiceData.getDataType(originalData).equals(DataType.Collection)) {
      throw new IllegalStateException(
          "Collections operations applied to non-collection data " + originalData.getId());
    }
    // get a list of the collection constraints
    final List<PropertyConstraint> collectionConstraints = dataIn.getConstraints().stream()
        .filter(constraint -> isCollectionConstraint(constraint)).collect(Collectors.toList());
    Task processedData = originalData;
    for (final PropertyConstraint contraint : collectionConstraints) {
      processedData = modelCollectionOperation(contraint, dataIn.getName(), processedData, graph,
          finalDataType);
    }
    return processedData;
  }

  /**
   * Same as the above method, but w.r.t. one collection operation. Applied
   * iteratively for operation chaining.
   * 
   * @param contraint the processed constraint
   * @param jsonKey the key that the consumer will use to access the processed
   *        data
   * @param originalData data before processing with this constraint
   * @param graph the enactment graph
   * @return the node with the processed data (from the one operation)
   */
  protected static Task modelCollectionOperation(final PropertyConstraint contraint,
      final String jsonKey, final Task originalData, final EnactmentGraph graph,
      final DataType finalDataType) {
    // create the function node modeling the operation
    final String subCollectionString = contraint.getValue();
    final CollectionOperation operationType =
        UtilsAfcl.getCollectionOperationType(contraint.getName(), subCollectionString);
    final Task operationNode = PropertyServiceFunctionUtilityCollections
        .createCollectionOperation(originalData.getId(), subCollectionString, operationType);
    // connect it to the original data
    PropertyServiceDependency.addDataDependency(originalData, operationNode, jsonKey, graph);
    // make the src connections where necessary
    final List<String> inputStrings = getSubstrings(subCollectionString, operationType);
    final boolean illegalStringsPresent =
        !inputStrings.stream().allMatch(string -> isLegalEntryString(string, operationType));
    if (illegalStringsPresent) {
      throw new IllegalArgumentException("Illegal collection operation strings: " + inputStrings);
    }
    final Set<String> srcInputStrings = inputStrings.stream()
        .filter(string -> UtilsAfcl.isSrcString(string)).collect(Collectors.toSet());
    srcInputStrings.forEach(srcString -> attachOperationInput(srcString, operationNode, graph));

    // create the node for the processed data
    final String processedDataId = originalData.getId() + ConstantsEEModel.KeyWordSeparator2
        + operationType.name() + ConstantsEEModel.KeyWordSeparator2 + subCollectionString;
    final boolean oneElementResult =
        operationType.equals(CollectionOperation.ElementIndex) && inputStrings.size() == 1;
    final DataType processedDataType = oneElementResult ? finalDataType : DataType.Collection;
    final Task processedData =
        AfclCompounds.assureDataNodePresence(processedDataId, processedDataType, graph);
    PropertyServiceDependency.addDataDependency(operationNode, processedData, jsonKey, graph);
    return processedData;
  }

  /**
   * Connects the collection operation node to a data node supplying one of its
   * inputs.
   * 
   * @param srcString string denoting the data src (and used as json key of the
   *        dependency)
   * @param operationNode the function node
   * @param graph the enactment graph
   */
  protected static void attachOperationInput(final String srcString, final Task operationNode,
      final EnactmentGraph graph) {
    final Task inputData = AfclCompounds.assureDataNodePresence(srcString, DataType.Number, graph);
    PropertyServiceDependency.addDataDependency(inputData, operationNode, srcString, graph);
  }

  /**
   * Converts the subcollection string into a list of strings, where each entry is
   * a number, defined either as a value or as a source-reference to a data out
   * generating the value at run time.
   * 
   * @param subcollectionString the subcollection string annotate in the afcl file
   * @param operation the operation that the subcollection string describes
   * @return list of strings, each of them describing a number
   */
  protected static List<String> getSubstrings(final String subcollectionString,
      final CollectionOperation operation) {
    switch (operation) {
      case Replicate:
        return getSubstringsReplicateSplit(subcollectionString);
      case Split:
        return getSubstringsReplicateSplit(subcollectionString);
      case Block:
        return getSubstringsBlock(subcollectionString);
      case ElementIndex:
        return getSubstringsEIdx(subcollectionString);
      default:
        throw new IllegalStateException("Unknown collection operation: " + operation.name());
    }
  }

  /**
   * See the getSubstrings method. This one is used for replicate and split.
   */
  protected static List<String> getSubstringsReplicateSplit(final String subcollectionString) {
    return Arrays.asList(subcollectionString);
  }

  /**
   * See the getSubstrings method. This one is used for element index.
   */
  protected static List<String> getSubstringsEIdx(final String subcollectionString) {
    final List<String> result = new ArrayList<>();
    if (subcollectionString.contains(ConstantsAfcl.constraintSeparatorEIdxOuter)) {
      for (final String innerSubString : subcollectionString
          .split(ConstantsAfcl.constraintSeparatorEIdxOuter)) {
        result.addAll(getInnerEidxSubstrings(innerSubString));
      }
    } else {
      result.addAll(getInnerEidxSubstrings(subcollectionString));
    }
    return result;
  }

  /**
   * See the getSubstrings method. This one is used for block.
   */
  protected static List<String> getSubstringsBlock(final String subcollectionString) {
    if (!subcollectionString.contains(ConstantsAfcl.constraintSeparatorBlock)) {
      throw new IllegalArgumentException(
          "Incorrect Block Constraint Argument: " + subcollectionString);
    }
    return Arrays.asList(subcollectionString.split(ConstantsAfcl.constraintSeparatorBlock)[0],
        subcollectionString.split(ConstantsAfcl.constraintSeparatorBlock)[1]);
  }

  /**
   * Splits the given string describing one Eidx access into a list of substrings,
   * each of which is a number, emtpy, or a src string.
   * 
   * @param innerEidxString
   * @return
   */
  protected static List<String> getInnerEidxSubstrings(final String innerEidxString) {
    if (innerEidxString.contains(ConstantsAfcl.constraintSeparatorEIdxInner)) {
      return Arrays.asList(innerEidxString.split(ConstantsAfcl.constraintSeparatorEIdxInner));
    } else {
      return Arrays.asList(innerEidxString);
    }
  }

  /**
   * Returns true if the given substring is legal.
   * 
   * @param entryString the string to check
   * @param operation the operation type
   * @return true if the given substring is legal.
   */
  protected static boolean isLegalEntryString(final String entryString,
      final CollectionOperation operation) {
    if (UtilsAfcl.isSrcString(entryString)) {
      return true;
    }
    final String noWsString = entryString.trim();
    if (operation.equals(CollectionOperation.ElementIndex) && noWsString.isEmpty()) {
      return true;
    }
    return UtilsAfcl.isInt(noWsString);
  }

  /**
   * Returns true if the provided dataIn has at least one constraint defining a
   * collection operation.
   * 
   * @param dataIn the given dataIn
   * @return true if the provided dataIn has at least one constraint defining a
   *         collection operation
   */
  public static boolean hasCollectionOperations(final DataIns dataIn) {
    if (!AfclApiWrapper.hasConstraints(dataIn)) {
      return false;
    }
    return dataIn.getConstraints().stream()
        .anyMatch(constraint -> isCollectionConstraint(constraint));
  }

  /**
   * Returns true if the provided constraint relates to collection operations.
   * 
   * @param constraint the provided constraint.
   * @return true if the provided constraint relates to collection operations
   */
  protected static boolean isCollectionConstraint(final PropertyConstraint constraint) {
    final String name = constraint.getName();
    return name.equals(ConstantsAfcl.constraintNameBlock)
        || name.equals(ConstantsAfcl.constraintNameElementIndex)
        || name.equals(ConstantsAfcl.constraintNameReplicate)
        || name.equals(ConstantsAfcl.constraintNameSplit);
  }
}
