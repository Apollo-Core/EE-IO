package at.uibk.dps.ee.io.output;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.core.FailureHandler;
import at.uibk.dps.ee.core.OutputDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OutputDataPrinter} simply logs the enactment result (or the
 * failure cause).
 * 
 * @author Fedor Smirnov
 *
 */
@Singleton
public class OutputDataPrinter implements OutputDataHandler, FailureHandler {

  protected final Logger outputLogger = LoggerFactory.getLogger(OutputDataPrinter.class);

  @Override
  public void handleFailure(Throwable failureCause) {
    outputLogger.error("Enactment failed with message {}. No output produced.",
        failureCause.getMessage());
  }

  @Override
  public void handleOutputData(JsonObject outputData) {
    outputLogger.info("Workflow executed correctly.");
    outputLogger.info("Enactment result: {}", outputData.toString());
  }
}

