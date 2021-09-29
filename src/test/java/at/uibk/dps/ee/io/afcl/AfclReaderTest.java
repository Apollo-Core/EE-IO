package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.persistance.EnactmentSpecIo;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Element;
import net.sf.opendse.model.Task;

public class AfclReaderTest {

  @Test
  public void testBytesToWf() {
    Workflow wf = Graphs.getSingleAtomicWf();
    // generic poking around to check that we actually get a wf. The functionality
    // itself is tested in AFCLCore
    assertEquals("single Atomic", wf.getName());
    assertEquals(1, wf.getWorkflowBody().size());
  }

  @Test
  public void testRead() {
    AfclReader tested = new AfclReader(ConstantsTestCoreEEiO.cfclFileSingleAtomic);
    EnactmentGraph result = tested.getEnactmentGraph();
    EnactmentSpecification eSpec =
        EnactmentSpecIo.readSpecFromFilePath(ConstantsTestCoreEEiO.xmlFileTestAtomic);
    EnactmentGraph expected = eSpec.getEnactmentGraph();
    for (Task task : expected) {
      compareElements(result.getVertex(task.getId()), task);
    }
    for (Dependency dep : expected.getEdges()) {
      compareElements(result.getEdge(dep.getId()), dep);
    }
  }

  protected static void compareElements(Element resultElement, Element expectedElement) {
    assertEquals(expectedElement.getId(), resultElement.getId());
    for (String attrName : expectedElement.getAttributeNames()) {
      assertEquals((Object) expectedElement.getAttribute(attrName),
          (Object) resultElement.getAttribute(attrName));
    }
  }
}
