package at.uibk.dps.ee.io.persistance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.io.testclasses.AtomicEGGenerator;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class EnactmentGraphIOTest {

  @Test
  public void test() {
    String filePath = ConstantsTestCoreEEiO.xmlFileTestReadWrite;
    EnactmentGraph original = AtomicEGGenerator.generateGraph();

    EnactmentGraphIO.writeEnactmentGraph(original, filePath);
    EnactmentGraph restored = EnactmentGraphIO.readEnactmentGraph(filePath);

    // check the nodes
    for (Task origNode : original) {
      if (restored.getVertex(origNode.getId()) == null) {
        fail();
      }
      Task restoredNode = restored.getVertex(origNode.getId());
      for (String attrName : origNode.getAttributeNames()) {
        Object orgAttr = origNode.getAttribute(attrName);
        Object restoredAttr = restoredNode.getAttribute(attrName);
        assertEquals(orgAttr, restoredAttr);
      }
    }
    // check the edges
    for (Dependency orgDep : original.getEdges()) {
      if (restored.getEdge(orgDep.getId()) == null) {
        fail();
      }
      Dependency restoredDep = restored.getEdge(orgDep.getId());
      for (String attrName : orgDep.getAttributeNames()) {
        Object orgAttr = orgDep.getAttribute(attrName);
        Object restoredAttr = restoredDep.getAttribute(attrName);
        assertEquals(orgAttr, restoredAttr);
      }
    }

    // Delete the created file
    File graphFile = new File(filePath);
    assertTrue(graphFile.delete());
  }
}
