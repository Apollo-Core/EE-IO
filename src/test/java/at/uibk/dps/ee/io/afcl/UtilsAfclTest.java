package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.ee.io.afcl.UtilsAfcl.CompoundType;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;

public class UtilsAfclTest {

  @Test
  public void testGetCompoundType() {
    assertEquals(CompoundType.Atomic, UtilsAfcl.getCompoundType(new AtomicFunction()));
  }

  @Test
  public void testDataNodeId() {
    String producerId = "producer";
    String dataId = "data";
    String expected = producerId + ConstantsAfcl.SourceAffix + dataId;
    assertEquals(expected, UtilsAfcl.getDataNodeId(producerId, dataId));
  }

  @Test
  public void testGetDataTypeForStringWrongInput() {
    assertThrows(IllegalArgumentException.class, () -> {
      UtilsAfcl.getDataTypeForString("unknown");
    });
  }

  @Test
  public void testGetDataTypeForString() {
    assertEquals(DataType.String, UtilsAfcl.getDataTypeForString(ConstantsAfcl.typeStringString));
    assertEquals(DataType.Number, UtilsAfcl.getDataTypeForString(ConstantsAfcl.typeStringNumber));
    assertEquals(DataType.Object, UtilsAfcl.getDataTypeForString(ConstantsAfcl.typeStringObject));
    assertEquals(DataType.Collection,
        UtilsAfcl.getDataTypeForString(ConstantsAfcl.typeStringCollection));
    assertEquals(DataType.Boolean, UtilsAfcl.getDataTypeForString(ConstantsAfcl.typeStringBoolean));
  }

  @Test
  public void testGetCompoundTypeUnknown() {
    assertThrows(IllegalArgumentException.class, () -> {
      Function input = mock(Function.class);
      when(input.getName()).thenReturn("name");
      UtilsAfcl.getCompoundType(input);
    });
  }

  @Test
  public void testSrcStringOperations() {
    String producerString = "source";
    String dataIdString = "data";
    String srcString = producerString + ConstantsAfcl.SourceAffix + dataIdString;

    assertEquals(producerString, UtilsAfcl.getProducerId(srcString));
    assertEquals(dataIdString, UtilsAfcl.getDataId(srcString));
  }

  @Test
  public void testSrcStringCheck() {
    String producerString = "source";
    String dataIdString = "data";
    String constString = "5";

    String correct = producerString + ConstantsAfcl.SourceAffix + dataIdString;
    String incorrect1 = ConstantsAfcl.SourceAffix + dataIdString;
    String incorrect2 = producerString + ConstantsAfcl.SourceAffix;

    assertTrue(UtilsAfcl.isSrcString(correct));
    assertFalse(UtilsAfcl.isSrcString(incorrect1));
    assertFalse(UtilsAfcl.isSrcString(incorrect2));
    assertFalse(UtilsAfcl.isSrcString(constString));
  }

  @Test
  public void testIsSetResLinkAtomFunc() {
    AtomicFunction atom = new AtomicFunction();
    assertFalse(UtilsAfcl.isResourceSetAtomFunc(atom));
    String resName = "res";
    List<PropertyConstraint> propList = new ArrayList<>();
    PropertyConstraint propConst = new PropertyConstraint();
    propConst.setName(ConstantsAfcl.propertyConstraintResourceLink);
    propConst.setValue(resName);
    propList.add(propConst);
    atom.setProperties(propList);
    assertTrue(UtilsAfcl.isResourceSetAtomFunc(atom));
  }

  @Test
  public void testReadFileToBytes() {
    final byte[] result;
    try {
      result = UtilsAfcl.readFileToBytes(ConstantsTestCoreEEiO.filePathReadTestFile);
      assertTrue(Arrays.equals(ConstantsTestCoreEEiO.expectedByteValue, result));
    } catch (IOException exc) {
      fail("IOException when testing file to byte reading.");
    }
  }
}
