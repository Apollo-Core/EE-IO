package at.uibk.dps.ee.io.json;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ResourceEntryTest {

  @Test
  public void test() {
    String type = "addition";
    String implId = "impl";
    String key1 = "first";
    String key2 = "second";
    JsonElement first = new JsonPrimitive(true);
    JsonElement second = new JsonPrimitive(42);

    Map<String, JsonElement> properties = new HashMap<>();
    properties.put(key1, first);
    properties.put(key2, second);

    ResourceEntry tested = new ResourceEntry(type, implId, properties);
    assertEquals(type, tested.getType());
    assertEquals(properties, tested.getProperties());

    tested.setType("otherString");
    tested.setProperties(new HashMap<>());

    assertNotEquals(type, tested.getType());
    assertNotEquals(properties, tested.getProperties());
  }
}
