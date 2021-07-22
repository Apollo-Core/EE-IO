package at.uibk.dps.ee.io.output;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class OutputDataPrinterTest {

  @Test
  public void test() {
    JsonObject testInput = new JsonObject();
    testInput.addProperty("Prop1", true);
    testInput.addProperty("Prop2", 3);

    OutputDataPrinter tested = new OutputDataPrinter();
    Logger logger = (Logger) tested.outputLogger;
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    tested.handleOutputData(testInput);
    String expected1 = "Workflow executed correctly.";
    String expected2 = "Enactment result: " + testInput.toString();

    List<ILoggingEvent> logList = listAppender.list;
    assertEquals(expected1, logList.get(0).getFormattedMessage());
    assertEquals(Level.INFO, logList.get(0).getLevel());
    assertEquals(expected2, logList.get(1).getFormattedMessage());
    assertEquals(Level.INFO, logList.get(1).getLevel());
  }
}
