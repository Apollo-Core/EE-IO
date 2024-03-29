package at.uibk.dps.ee.io.spec;

import static org.junit.jupiter.api.Assertions.*;

import at.uibk.dps.ee.io.resources.ResourceGraphProviderFile;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.ResourceGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUser;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping.EnactmentMode;
import at.uibk.dps.ee.model.properties.PropertyServiceMappingLocal;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;

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

    String filePath = ConstantsTestCoreEEiO.resourceTestInputPath;
    ResourceGraphProvider rProvider = new ResourceGraphProviderFile(filePath);
    SpecificationProviderFile tested =
        new SpecificationProviderFile(eProvider, rProvider, filePath);

    MappingsConcurrent result = tested.getMappings();

    assertEquals(6, result.mappingStream().count());
    assertEquals(4, result.getMappings(t1).size());
    assertEquals(2, result.getMappings(t2).size());
    result.forEach(mapping -> checkMapping(mapping));
  }

  protected static void checkMapping(Mapping<Task, Resource> mapping) {
    EnactmentMode mode = PropertyServiceMapping.getEnactmentMode(mapping);
    assertTrue(mode.equals(EnactmentMode.Local) || mode.equals(EnactmentMode.Serverless)
        || mode.equals(EnactmentMode.Demo));
    if (mode.equals(EnactmentMode.Local)) {
      assertNotNull(PropertyServiceMappingLocal.getImageName(mapping));
    }
    String implId = PropertyServiceMapping.getImplementationId(mapping);
    assertNotNull(implId);
  }
}
