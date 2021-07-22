package at.uibk.dps.ee.io.json;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FunctionTypeEntryTest {

  @Test
  public void test() {

    String functionType = "addition";
    List<ResourceEntry> entries = new ArrayList<>();
    ResourceEntry mock1 = mock(ResourceEntry.class);
    ResourceEntry mock2 = mock(ResourceEntry.class);
    entries.add(mock1);
    entries.add(mock2);

    FunctionTypeEntry tested = new FunctionTypeEntry(functionType, entries);
    assertEquals(functionType, tested.getFunctionType());
    assertEquals(entries, tested.getResources());

    tested.setFunctionType("otherType");
    tested.setResources(new ArrayList<>());
    assertNotEquals(functionType, tested.getFunctionType());
    assertNotEquals(entries, tested.getResources());
  }
}
