package at.uibk.dps.ee.io.script;

import java.util.HashSet;
import java.util.Set;
import org.opt4j.core.config.ModuleAutoFinder;
import org.opt4j.core.config.ModuleLoader;
import org.opt4j.core.config.ModuleRegister;
import org.opt4j.core.config.Starter;
import org.opt4j.core.config.Task;
import com.google.inject.Module;
import at.uibk.dps.ee.core.exception.FailureException;
import at.uibk.dps.ee.guice.modules.InputModule;
import at.uibk.dps.ee.guice.modules.VisualizationModule;
import at.uibk.dps.ee.guice.starter.EeTask;
import at.uibk.dps.ee.io.modules.InputReaderFileModule;
import at.uibk.dps.ee.io.modules.SpecificationInputModule;

/**
 * Class used to start the enactment without the activation of the interactive
 * configuration GUI.
 * 
 * @author Fedor Smirnov
 *
 */
public class EeStarter extends Starter {

  /**
   * Starts the EE task with the provided arguments
   * 
   * @param args the arguments to start the task
   * @throws FailureException the failure exception which can be thrown by the EE
   */
  public static void main(final String[] args) throws FailureException {
    final EeStarter starter = new EeStarter();
    starter.execute(args);
  }

  @Override
  public void execute(final String[] args) throws FailureException {
    try {
      if (args.length == 1) {
        // all information within one config file
        executeConfigFile(args[0]);
      } else if (args.length == 4) {
        // config file plus file locations of input, workflow, and typeMappings
        executeConfigFileFileLocations(args[0], args[1], args[2], args[3]);
      }

      else {
        throw new IllegalArgumentException("Wrong arguments provided for the EE script.");
      }
    } catch (FailureException failureExc) {
      throw (FailureException) failureExc;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Executes the enactment with the given configuration file, while using the
   * input, the workflow, and the type mappings found in the files given by the
   * other arguments.
   * 
   * @param configFile the config file
   * @param inputFile the file with the input
   * @param workflowFile the file describing the WF
   * @param typeMappingsFile the file describing the type mappings
   * @throws Exception
   */
  protected void executeConfigFileFileLocations(String configFile, String inputFile,
      String workflowFile, String typeMappingsFile) throws Exception {
    Set<Module> modules = getModulesFromConfigFile(configFile);
    modules.removeIf(module -> (module instanceof InputModule));
    SpecificationInputModule specModule = new SpecificationInputModule();
    specModule.setFilePathAfcl(workflowFile);
    specModule.setFilePathMappingFile(typeMappingsFile);
    InputReaderFileModule inputModule = new InputReaderFileModule();
    inputModule.setFilePath(inputFile);
    modules.add(specModule);
    modules.add(inputModule);
    runWithModules(modules);
  }

  /**
   * Executes the enactment with the given configuration file. It is assumed that
   * all the ways to obtain the necessary input are configured within the modules
   * specified by the configFile.
   * 
   * @param configFile the config file
   * @throws Exception
   */
  protected void executeConfigFile(String configFile) throws Exception {
    Set<Module> modules = getModulesFromConfigFile(configFile);
    runWithModules(modules);
  }

  /**
   * Runs the EE script with the provided modules.
   * 
   * @param modules the provided modules.
   * @throws Exception
   */
  protected void runWithModules(Set<Module> modules) throws Exception {
    Task task = new EeTask();
    task.init(modules);
    task.call();
  }

  /**
   * Returns the set of modules used in the given config file.
   * 
   * @param configFile the given config file
   * @return the set of modules used in the given config file
   */
  protected Set<Module> getModulesFromConfigFile(String configFile) {
    ModuleLoader loader = new ModuleLoader(new ModuleRegister(new ModuleAutoFinder()));
    Set<Module> modules = new HashSet<>();
    modules.addAll(loader.load(configFile));
    modules.removeIf(module -> (module instanceof VisualizationModule));
    return modules;
  }
}
