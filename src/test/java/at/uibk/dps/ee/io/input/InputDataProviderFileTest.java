package at.uibk.dps.ee.io.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;

public class InputDataProviderFileTest {

  @Test
  public void testWrongFileContent() {
    assertThrows(JsonSyntaxException.class, () -> {
      new InputDataProviderFile(ConstantsTestCoreEEiO.jsonInputFileWrong);
    });
  }

  @Test
  public void testWrongPath() {
    assertThrows(IllegalArgumentException.class, () -> {
      new InputDataProviderFile("wrong path");
    });
  }

  @Test
  public void test() {
    InputDataProviderFile tested = new InputDataProviderFile(ConstantsTestCoreEEiO.jsonInputFile);
    JsonObject result = tested.getInputData();
    assertTrue(result.has("a"));
    assertTrue(result.has("b"));
    assertTrue(result.has("wait"));
    assertEquals(3, result.get("a").getAsInt());
    assertEquals(17, result.get("b").getAsInt());
    assertEquals("no", result.get("wait").getAsString());
  }
}
