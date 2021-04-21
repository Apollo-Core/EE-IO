package at.uibk.dps.ee.io.modules;

import org.opt4j.core.config.annotations.File;
import org.opt4j.core.config.annotations.Info;
import org.opt4j.core.config.annotations.Order;
import at.uibk.dps.ee.guice.modules.OutputModule;
import ch.qos.logback.classic.util.ContextInitializer;

/**
 * Module to configure the location of the logback config file.
 * 
 * @author Fedor Smirnov
 */
public class LoggingModule extends OutputModule {

  @Order(1)
  @Info("Path of the file configuring the loggers.")
  @File
  public String pathToConfigFile = "./logging/config/logback.xml";

  @Override
  protected void config() {
    // configure the location of the logback config file
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, pathToConfigFile);
  }

  public String getPathToConfigFile() {
    return pathToConfigFile;
  }

  public void setPathToConfigFile(final String pathToConfigFile) {
    this.pathToConfigFile = pathToConfigFile;
  }
}
