package at.uibk.dps.ee.io.validation;

import at.uibk.dps.ee.model.graph.EnactmentGraph;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * Static container for the methods used to validate the created graphs.
 * 
 * @author Fedor Smirnov
 */
public final class GraphValidation {

  /**
   * No constructor.
   */
  private GraphValidation() {}

  /**
   * Validates the given graph. Throws an exception if graph is invalid.
   * 
   * @param graph the given graph
   */
  public static void validateGraph(final EnactmentGraph graph) {
    checkForDisconnectedDataNodes(graph);
  }

  /**
   * Throws an exception if any disconnected nodes are found.
   * 
   * @param graph the graph to check
   */
  static void checkForDisconnectedDataNodes(final EnactmentGraph graph) {
    for (final Task task : graph) {
      if (TaskPropertyService.isCommunication(task) && graph.getIncidentEdges(task).isEmpty()) {
        throw new IllegalStateException(
            "The generated graph contains a disconnected data node: " + task.getId());
      }
    }
  }
}
