package at.uibk.dps.ee.io.output;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.core.OutputDataHandler;
import io.vertx.core.Future;
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
  public void handleOutputData(final Future<JsonObject> outputData) {
    outputData.onSuccess(this::handleSuccess);
    outputData.onFailure(this::handleFailure);
  }

  /**
   * Success handler
   * 
   * @param json the enactment result
   */
  protected void handleSuccess(JsonObject json) {
    outputLogger.info("Workflow executed correctly.");
    outputLogger.info("Enactment result: {}", json.toString());
  }

  /**
   * Failure handler
   * 
   * @param failureReason the reason for the failure
   */
  protected void handleFailure(Throwable failureReason) {
    outputLogger.error("Enactment failed. No output produced.");
  }
}

