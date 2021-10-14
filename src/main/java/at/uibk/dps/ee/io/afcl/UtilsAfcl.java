package at.uibk.dps.ee.io.afcl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.IfThenElse;
import at.uibk.dps.afcl.functions.ParallelFor;
import at.uibk.dps.afcl.functions.While;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.objects.Condition.CombinedWith;
import at.uibk.dps.ee.model.objects.Condition.Operator;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections.CollectionOperation;

/**
 * Convenience methods for working with the AFCL classes and syntax.
 * 
 * @author Fedor Smirnov
 *
 */
public final class UtilsAfcl {

  /**
   * No constructor
   */
  private UtilsAfcl() {}

  /**
   * Enum for the different compounds used in AFCL.
   * 
   * @author Fedor Smirnov
   *
   */
  public enum CompoundType {
    Atomic, If, ParallelFor, While
  }

  /**
   * Returns an optional of the data in with the given name of the given function.
   * 
   * @param function the given function
   * @param name the name of the desired data in
   * @return an optional of the data in with the given name of the given function
   */
  public static Optional<DataIns> getDataInWithName(final Function function, final String name) {
    Optional<DataIns> result = Optional.empty();
    for (final DataIns dataIn : AfclApiWrapper.getDataIns(function)) {
      if (dataIn.getName().equals(name)) {
        result = Optional.of(dataIn);
        break;
      }
    }
    return result;
  }

  /**
   * Gets the collection operation corresponding to the afcl contraint name
   * 
   * @param constraintName the afcl contraint name
   * @param value the constraint value
   * @return the collection operation corresponding to the afcl contraint name
   */
  public static CollectionOperation getCollectionOperationType(final String constraintName,
      final String value) {
    switch (constraintName) {
      case ConstantsAfcl.constraintNameElementIndex:
        return CollectionOperation.ElementIndex;
      case ConstantsAfcl.constraintNameReplicate:
        return CollectionOperation.Replicate;
      case ConstantsAfcl.constraintNameBlock:
        return CollectionOperation.Block;
      case ConstantsAfcl.constraintNameSplit:
        return CollectionOperation.Split;
      default:
        throw new IllegalArgumentException("unknown collection operation " + constraintName);
    }
  }

  /**
   * Returns true if the given string is an int
   * 
   * @param string the given string
   * @return true if the given string is an int
   */
  public static boolean isInt(final String string) {
    try {
      Integer.parseInt(string);
      return true;
    } catch (NumberFormatException exc) {
      return false;
    }
  }

  /**
   * Returns the compound type of the provided function
   * 
   * @param function the provided function
   * @return the compound type of the provided function
   */
  public static CompoundType getCompoundType(final Function function) {
    if (function instanceof AtomicFunction) {
      return CompoundType.Atomic;
    } else if (function instanceof IfThenElse) {
      return CompoundType.If;
    } else if (function instanceof ParallelFor) {
      return CompoundType.ParallelFor;
    } else if (function instanceof While) {
      return CompoundType.While;
    } else {
      throw new IllegalArgumentException(
          "The function " + function.getName() + " is a compound of an unknown type.");
    }
  }

  /**
   * Returns the operator object for the given afcl string.
   * 
   * @param afclOperatorString the given afcl string
   * @return the {@link Operator} object
   */
  public static Operator getOperatorForString(final String afclOperatorString) {
    return Optional.ofNullable(ConstantsAfcl.stringsToCondOperators.get(afclOperatorString))
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown condition operator type " + afclOperatorString));
  }

  /**
   * Returns the combinedWith enum associated with the given string from the AFCL
   * file.
   * 
   * @param combinedWithString the string from the AFCL file
   * @return the combinedWith enum associated with the given string from the AFCL
   *         file
   */
  public static CombinedWith getCombinedWithForString(final String combinedWithString) {
    switch (combinedWithString) {
      case ConstantsAfcl.combinedWithStringAnd:
        return CombinedWith.And;
      case ConstantsAfcl.combinedWithStringOr:
        return CombinedWith.Or;
      default:
        throw new IllegalArgumentException("Unknown combinedWith string: " + combinedWithString);
    }
  }

  /**
   * Returns the enum used by the property service based on the string used in the
   * afcl file.
   * 
   * @param afclString the string used in the afcl file
   * @return the enum used by the property service based on the string used in the
   *         afcl file
   */
  public static DataType getDataTypeForString(final String afclString) {
    return Optional.ofNullable(ConstantsAfcl.stringToDataTypes.get(afclString))
        .orElseThrow(() -> new IllegalArgumentException("Unknown data type " + afclString));
  }

  /**
   * Returns true if a resource is set for the given function.
   * 
   * @param atomFunc the given function.
   * @return true if a resource is set for the given function
   */
  public static boolean isResourceSetAtomFunc(final AtomicFunction atomFunc) {
    if (atomFunc.getProperties() == null) {
      return false;
    }
    for (final PropertyConstraint propConstraint : atomFunc.getProperties()) {
      if (propConstraint.getName().equals(ConstantsAfcl.propertyConstraintResourceLink)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the ID of a data object which is created by the producer with the
   * producer ID and is named with the dataID
   * 
   * @param producerId the id of the producer (function or the WF)
   * @param dataId the name of the data
   * @return the ID of a data object which is created by the producer with the
   *         producer ID and is named with the dataID
   */
  public static String getDataNodeId(final String producerId, final String dataId) {
    return producerId + ConstantsAfcl.SourceAffix + dataId;
  }

  /**
   * Returns true iff the provided string describes data produced by a producer.
   * 
   * @param srcString the string to check
   * @return true iff the provided string describes data produced by a producer
   */
  public static boolean isSrcString(final String srcString) {
    final String affix = ConstantsAfcl.SourceAffix;
    return srcString.contains(affix) && !srcString.startsWith(affix) && !srcString.endsWith(affix);
  }

  /**
   * Returns true iff the provided string describes constant data.
   * 
   * @param string the provided string
   * @return true iff the provided string describes constant data
   */
  public static boolean isConstantSrcString(final String string) {
    return string.startsWith(ConstantsEEModel.ConstantNodeAffix + ConstantsAfcl.SourceAffix);
  }

  /**
   * Reads the producer ID from the given srcString
   * 
   * @param srcString the given srcString
   * @return the producer ID from the given srcString
   */
  public static String getProducerId(final String srcString) {
    return getSrcSubString(srcString, true);
  }

  /**
   * Reads the data ID from the given srcString
   * 
   * @param srcString the given srcString
   * @return the data ID from the given srcString
   */
  public static String getDataId(final String srcString) {
    return getSrcSubString(srcString, false);
  }

  /**
   * Returns a substring of the given src string correspopnding to the producer id
   * (true) or the data id (false).
   * 
   * @param srcString the srcString
   * @param producer true if asking for the producer, false for the data id
   * @return a substring of the given src string correspopnding to the producer id
   *         (true) or the data id (false)
   */
  static String getSrcSubString(final String srcString, final boolean producer) {
    return producer ? srcString.split(ConstantsAfcl.SourceAffix)[0]
        : srcString.split(ConstantsAfcl.SourceAffix)[1];
  }

  /**
   * Returns true iff the given string describes the src of a data out of an if
   * compound.
   * 
   * @param srcString the src string
   * @return true iff the given string describes the src of a data out of an if
   *         compound
   */
  public static boolean isIfOutSrc(final String srcString) {
    return srcString.contains(ConstantsAfcl.IfFuncSeparator);
  }

  /**
   * Returns the substring of the first function read from the given if src
   * string.
   * 
   * @param srcString the if src string
   * @return the substring of the first function read from the given if src string
   */
  public static String getFirstSubStringIfOut(final String srcString) {
    return getIfOutSubString(srcString, true);
  }

  /**
   * Returns the substring of the second function read from the given if src
   * string.
   * 
   * @param srcString the if src string
   * @return the substring of the second function read from the given if src
   *         string
   */
  public static String getSecondSubStringIfOut(final String srcString) {
    return getIfOutSubString(srcString, false);
  }

  /**
   * Returns either the first of the second substring of the given src string of
   * an if out.
   * 
   * @param srcString the given string
   * @param first true iff asking for the first string
   * @return either the first of the second substring of the given src string of
   *         an if out
   */
  static String getIfOutSubString(final String srcString, final boolean first) {
    return first ? srcString.split(ConstantsAfcl.IfFuncSeparator)[0]
        : srcString.split(ConstantsAfcl.IfFuncSeparator)[1];
  }

  /**
   * Reads a file at the specified path and converts it to an array of bytes.
   * 
   * @param filePath the path to the file
   * @return the byte array
   * @throws IOException exception thrown if reading fails
   */
  public static byte[] readFileToBytes(final String filePath) throws IOException {
    final File file = new File(filePath);
    final byte[] result = new byte[(int) file.length()];
    try (InputStream fis = Files.newInputStream(Paths.get(filePath))) {
      try (BufferedInputStream bis = new BufferedInputStream(fis)) {
        bis.read(result, 0, result.length);
      }
    }
    return result;
  }
}
