package at.uibk.dps.ee.io.script;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.google.inject.Module;
import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;

public class EeStarterTest {

  @Test
  public void testReadModules() {
    EeStarter tested = new EeStarter();
    String filePathConfigFile = ConstantsTestCoreEEiO.configFileModuleRead;
    Set<Module> result = tested.getModulesFromConfigFile(filePathConfigFile);
    assertEquals(3, result.size());
  }
}
