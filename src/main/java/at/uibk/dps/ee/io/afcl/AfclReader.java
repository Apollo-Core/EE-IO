package at.uibk.dps.ee.io.afcl;

import java.io.IOException;

import org.opt4j.core.start.Constant;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.utils.Utils;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;

/**
 * The {@link AfclReader} generates the {@link EnactmentGraph} based on a
 * provided .afcl/.cfcl file.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class AfclReader implements EnactmentGraphProvider {

  protected final EnactmentGraph enactmentGraph;

  /**
   * Built via injection in cases the file is read from a file on the local
   * storage.
   * 
   * @param filePath the path to the .afcl/.cfcl file
   */
  @Inject
  public AfclReader(
      @Constant(value = "filePath", namespace = AfclReader.class) final String filePath) {
    try {
      final byte[] wfData = UtilsAfcl.readFileToBytes(filePath);
      this.enactmentGraph = generateEnactmentGraph(wfData);
    } catch (IOException ioExc) {
      throw new IllegalStateException("IOException when reading the WF from the path: " + filePath,
          ioExc);
    }
  }

  /**
   * Constructor which is provided with the byte representation of the workflow
   * file.
   * 
   * @param workflowData
   */
  public AfclReader(final byte[] workflowData) {
    this.enactmentGraph = generateEnactmentGraph(workflowData);
  }

  @Override
  public EnactmentGraph getEnactmentGraph() {
    return this.enactmentGraph;
  }

  /**
   * Generates the enactment graph based on the .afcl/.cfcl file (provided as byte
   * array).
   * 
   * @param wfData the wfdata
   * @return The enactment graph modeling the enactment process.
   */
  protected final EnactmentGraph generateEnactmentGraph(final byte[] wfData) {
    return GraphGenerationAfcl.generateEnactmentGraph(bytes2Workflow(wfData));
  }

  /**
   * Converts the workflow data into a processable {@link Workflow} object.
   * 
   * @param workflowAsBytes byte array with the workflow information
   * @return the {@link Workflow} corresponding to the input data.
   */
  protected static Workflow bytes2Workflow(final byte[] workflowAsBytes) {
    try {
      return Utils.readYAMLNoValidation(workflowAsBytes);
    } catch (IOException ioExc) {
      throw new IllegalArgumentException(
          "IOException when converting the wf input into the AFCL workflow object.", ioExc);
    }
  }
}
