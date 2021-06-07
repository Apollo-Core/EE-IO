package at.uibk.dps.ee.io.spec;

import static org.junit.Assert.*;

import org.junit.Test;
import at.uibk.dps.ee.core.ContainerManager;
import at.uibk.dps.ee.io.resources.ResourceGraphProviderFile;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.graph.ResourceGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUser;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping.EnactmentMode;
import at.uibk.dps.ee.model.properties.PropertyServiceMappingLocal;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Mappings;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;

public class SpecificationProviderFileTest {

  @Test
  public void testMappingCreation() {
    EnactmentGraph eGraph = new EnactmentGraph();
    Task t1 = PropertyServiceFunctionUser.createUserTask("t1", "addition");
    Task t2 = PropertyServiceFunctionUser.createUserTask("t2", "subtraction");
    eGraph.addVertex(t1);
    eGraph.addVertex(t2);
    EnactmentGraphProvider eProvider = mock(EnactmentGraphProvider.class);
    when(eProvider.getEnactmentGraph()).thenReturn(eGraph);

    ContainerManager mockManager = mock(ContainerManager.class);

    String filePath = ConstantsTestCoreEEiO.resourceTestInputPath;
    ResourceGraphProvider rProvider = new ResourceGraphProviderFile(filePath);
    SpecificationProviderFile tested =
        new SpecificationProviderFile(eProvider, rProvider, filePath, mockManager);

    Mappings<Task, Resource> result = tested.getMappings();

    assertEquals(5, result.size());
    assertEquals(3, result.get(t1).size());
    assertEquals(2, result.get(t2).size());
    result.getAll().forEach(mapping -> checkMapping(mapping));
  }

  protected static void checkMapping(Mapping<Task, Resource> mapping) {
    EnactmentMode mode = PropertyServiceMapping.getEnactmentMode(mapping);
    assertTrue(mode.equals(EnactmentMode.Local) || mode.equals(EnactmentMode.Serverless));
    if (mode.equals(EnactmentMode.Local)) {
      assertNotNull(PropertyServiceMappingLocal.getImageName(mapping));
    }
    String implId = PropertyServiceMapping.getImplementationId(mapping);
    assertNotNull(implId);
  }
}
