package at.uibk.dps.ee.io.modules;

import at.uibk.dps.ee.core.FailureHandler;
import at.uibk.dps.ee.core.OutputDataHandler;
import at.uibk.dps.ee.guice.modules.OutputModule;
import at.uibk.dps.ee.io.output.OutputDataPrinter;

/**
 * Binds the output printer as the output data handler.
 * 
 * @author Fedor Smirnov
 *
 */
public class OutputPrinterModule extends OutputModule {

  @Override
  protected void config() {
    bind(OutputDataHandler.class).to(OutputDataPrinter.class);
    bind(FailureHandler.class).to(OutputDataPrinter.class);
  }
}
