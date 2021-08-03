package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;

class HierarchyLevelingAfclTest {

  Workflow workFlow;
  Workflow wfColl;

  /**
   * Request from within the while
   */
  @Test
  void testInnerRequest() {
    Function sum = AfclApiWrapper.getFunction(workFlow, "add");
    String requested = "innerWhile/innerIterator";
    String expected = "whileWhile/inputTwo";
    String result = HierarchyLevellingAfcl.getSrcDataId(requested, sum, workFlow);
    assertEquals(expected, result);
  }

  /**
   * Request from outside the while
   */
  @Test
  void testOuterRequest() {
    Function outerWhile = AfclApiWrapper.getFunction(workFlow, "while");
    String requested = "innerWhile/innerIterator";
    String expected = "add/sumResult";
    String result = HierarchyLevellingAfcl.getSrcDataId(requested, outerWhile, workFlow);
    assertEquals(expected, result);
  }

  /**
   * Request from the wf data out
   */
  @Test
  void testWfOutRequest() {
    String requested = "innerWhile/innerIterator";
    String expected = "add/sumResult";
    String result = HierarchyLevellingAfcl.getSrcDataId(requested, null, workFlow);
    assertEquals(expected, result);
  }

  /**
   * Request for a constant
   */
  @Test
  void testRequestForConstant() {
    Function function = AfclApiWrapper.getFunction(wfColl, "sumUpFunction");
    String requested = "while/sum";
    String expected = ConstantsEEModel.ConstantNodeAffix + "/0";
    String result = HierarchyLevellingAfcl.getSrcDataId(requested, function, wfColl);
    assertEquals(expected, result);
  }

  @BeforeEach
  void setup() {
    workFlow = Graphs.getWhileWhile();
    wfColl = Graphs.getWhileColl();
  }
}
