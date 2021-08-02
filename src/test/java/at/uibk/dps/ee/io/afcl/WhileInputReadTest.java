package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;

class WhileInputReadTest {

  Workflow multiLevelInput;

  String functionName = "add";

  @Test
  void test() {
    Map<String, Set<WhileInputReference>> result = AfclCompounds.parseWhileRelations(multiLevelInput);
    assertEquals(1, result.size());
    assertEquals(3, result.get(functionName).size());
    
    checkMapEntry(result, functionName, "whileWhile/inputTwo", "add/sumResult");
    checkMapEntry(result, functionName, "whileWhile/inputOne", "add/sumResult");
    checkMapEntry(result, functionName, "whileWhile/inputOne", "add/sumResult");
  }

  /**
   * For the testing of while input reference entries
   * 
   * @param result the reference entry table
   * @param functionName the id of the function node
   * @param firstIteration the id of the data node used as the input for the first
   *        iteration
   * @param furtherIterations the id of the data node used in all later iterations
   */
  void checkMapEntry(Map<String, Set<WhileInputReference>> result, String functionName,
      String firstIteration, String furtherIterations) {
    assertTrue(result.containsKey(functionName));
    Set<WhileInputReference> value = result.get(functionName);
    WhileInputReference inputReference = new WhileInputReference(firstIteration, furtherIterations);
    assertTrue(value.contains(inputReference));
  }

  @BeforeEach
  void setup() {
    multiLevelInput = Graphs.getWhileMultiLevel();
  }
}
