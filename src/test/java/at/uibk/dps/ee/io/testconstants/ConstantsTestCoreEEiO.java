package at.uibk.dps.ee.io.testconstants;

import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections.CollectionOperation;

/**
 * Container for the constants used within the tests.
 * 
 * @author Fedor Smirnov
 *
 */
public class ConstantsTestCoreEEiO {

  // constants for the resouces test input
  public static final String resourceTestInputPath =
      "src/test/resources/resourceInputFiles/testInput.json";


  // constants for the afcl test input
  public static final String cfclFileSingleAtomic =
      "src/test/resources/wfInputFiles/singleAtomic.yaml";
  public static final String cfclFileIf = "src/test/resources/wfInputFiles/simpleIf.yaml";
  public static final String cfclFileIfMulti =
      "src/test/resources/wfInputFiles/simpleIfMultiFunc.yaml";
  public static final String cfclFileIfNoElse =
      "src/test/resources/wfInputFiles/simpleIfNoElse.yaml";
  public static final String cfclFileElementIndex =
      "src/test/resources/wfInputFiles/atomicElementIndex.yaml";
  public static final String cfclFileParallelFor = "src/test/resources/wfInputFiles/parFor.yaml";
  public static final String cfclFileParallelForConstIterator =
      "src/test/resources/wfInputFiles/parForConstIterator.yaml";
  public static final String cfclFileParallelForIntIterator =
      "src/test/resources/wfInputFiles/parForIntIterator.yaml";
  public static final String cfclFileParallelForNoInput =
      "src/test/resources/wfInputFiles/parForNoInput.yaml";
  public static final String afclFileWhileColl = "src/test/resources/wfInputFiles/while.yaml";
  public static final String afclFileWhileNum = "src/test/resources/wfInputFiles/whileNum.yaml";
  public static final String afclFileWhileWhile = "src/test/resources/wfInputFiles/whileWhile.yaml";

  // single atomic names
  public static final String wfNameAtomic = "single Atomic";
  public static final String inputNameAtomic = "input_name";
  public static final String outputNameAtomic = "output_name";
  public static final String wfInputJsonNameAtomic = "inputSource";
  public static final String wfFunctionNameAtomic = "atomicFunction";
  public static final String wfFunctionInputNameAtomic = "myInput";
  public static final String wfFunctionConstantInputNameAtomic = "myInput2";
  public static final String wfFunctionOutputNameAtomic = "myOutput";
  public static final String wfFunctionResourceNameAtomic = "my_res_link";
  public static final int wfSingleAtomicConstant = 5;

  // simple if names
  public static final String simpleIfIfName = "ifCompound";
  public static final String simpleIfConditionInput1Name = "simple_if/cond1";
  public static final String simpleIfConditionInput2Name = "simple_if/cond2";
  public static final String simpleIfConditionConst1Name = "ifCompound/true";
  public static final String simpleIfConditionConst2Name = "ifCompound/abc";
  public static final String simpleIfFunc1OutName = "func1/output";
  public static final String simpleIfFunc2OutName = "func2/out";

  // element index names
  public static final String elementIndexRawDataName = "single Atomic/input_collection";
  public static final String elementIndexIndexName = "single Atomic/input_index";
  public static final String elementIndexStrideName = "single Atomic/input_stride";
  public static final String elementIndexInputIndex = "single Atomic/input_index";
  public static final String elementIndexInputStride = "single Atomic/input_stride";
  public static final String elementExpectedSubCollString =
      elementIndexIndexName + ", 3:6:" + elementIndexInputStride;
  public static final String elementIndexProcessedDataName = elementIndexRawDataName
      + ConstantsEEModel.KeyWordSeparator2 + CollectionOperation.ElementIndex.name()
      + ConstantsEEModel.KeyWordSeparator2 + elementExpectedSubCollString;
  public static final String elementIndexDataProcessingName = elementIndexRawDataName
      + ConstantsEEModel.KeywordSeparator1 + CollectionOperation.ElementIndex.name()
      + ConstantsEEModel.KeywordSeparator1 + elementExpectedSubCollString;

  // parallel for names
  public static final String parForRawDistDataName = "single Atomic/wf_coll";
  public static final String parForRawConstDataName = "single Atomic/wf_coll2";
  public static final String parForRawAggrNodeName = "single Atomic/wf_coll2";
  public static final String parForParForName = "parallelFor";
  public static final String parForColl1Name = "parallel_coll";
  public static final String parForColl2Name = "parallel_coll2";

  // constants for the read/write test
  public static final String xmlFileTestReadWrite =
      "src/test/resources/xmlFiles/testAtomicWrite.xml";
  public static final String xmlFileTestAtomic = "src/test/resources/xmlFiles/testAtomic.xml";
  public static final String jsonInputFile = "src/test/resources/testInput/testInput.json";
  public static final String jsonInputFileWrong =
      "src/test/resources/testInput/testInputWrong.json";

  // constants for the ee starter
  public static final String configFileModuleRead = "src/test/resources/testConfigs/testConfig.xml";
}
