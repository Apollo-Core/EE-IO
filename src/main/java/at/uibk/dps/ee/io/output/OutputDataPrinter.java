package at.uibk.dps.ee.io.output;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.core.OutputDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OutputDataPrinter} simply prints the enactment result to the
 * standard out.
 * 
 * @author Fedor Smirnov
 *
 */
@Singleton
public class OutputDataPrinter implements OutputDataHandler {

  protected final Logger outputLogger = LoggerFactory.getLogger(OutputDataPrinter.class);
  
  @Override
  public void handleOutputData(final JsonObject outputData) {
    outputLogger.info("Workflow executed correctly.");
    outputLogger.info("Enactment result: {}", outputData.toString());
  }
}

