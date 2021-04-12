package at.uibk.dps.ee.io.script;

import static org.junit.Assert.*;
import org.junit.Test;
import org.opt4j.core.config.ModuleRegister;
import org.opt4j.core.config.PropertyModule;
import static org.mockito.Mockito.mock;
import java.util.Set;

import com.google.inject.Module;
import at.uibk.dps.ee.io.modules.InputReaderFileModule;
import at.uibk.dps.ee.io.modules.OutputPrinterModule;
import at.uibk.dps.ee.io.modules.SpecificationInputModule;
import static org.mockito.Mockito.when;

public class ModuleLoadStringTest {

  
  @Test
  public void testStringInput() {
    ModuleRegister registerMock = mock(ModuleRegister.class);
    
    String inputModuleName = "at.uibk.dps.ee.io.modules.InputReaderFileModule";
    String outputPrinterName = "at.uibk.dps.ee.io.modules.OutputPrinterModule";
    String specInputName = "at.uibk.dps.ee.io.modules.SpecificationInputModule";
    
    try {
      Class<? extends Module> classSpecModule = Class.forName(specInputName).asSubclass(Module.class);
      Class<? extends Module> classInputModule = Class.forName(inputModuleName).asSubclass(Module.class); 
      Class<? extends Module> classOutputModule = Class.forName(outputPrinterName).asSubclass(Module.class);
      
      PropertyModule propMod1 = new PropertyModule(new OutputPrinterModule());
      PropertyModule propMod2 = new PropertyModule(new InputReaderFileModule());
      PropertyModule propMod3 = new PropertyModule(new SpecificationInputModule());
      
      when(registerMock.get(classOutputModule)).thenReturn(propMod1);
      when(registerMock.get(classInputModule)).thenReturn(propMod2);
      when(registerMock.get(classSpecModule)).thenReturn(propMod3);
      
    } catch (ClassNotFoundException e) {
      fail();
    }
    
    ModuleLoaderString tested = new ModuleLoaderString(registerMock);
    
    String configString = "<configuration>\n" + 
        "  <module class=\"at.uibk.dps.ee.io.modules.InputReaderFileModule\">\n" + 
        "    <property name=\"filePath\">./inputData/sixAtomic.json</property>\n" + 
        "  </module>\n" + 
        "  <module class=\"at.uibk.dps.ee.io.modules.OutputPrinterModule\"/>\n" + 
        "  <module class=\"at.uibk.dps.ee.io.modules.SpecificationInputModule\">\n" + 
        "    <property name=\"filePathAfcl\">./demoWfs/sixAtomics.yaml</property>\n" + 
        "    <property name=\"filePathMappingFile\">./typeMappings/sixAtomics.json</property>\n" + 
        "  </module>\n" + 
        "</configuration>";
    
      Set<Module> result = tested.loadModulesFromString(configString);
      assertEquals(3, result.size());
  }
}
