package at.uibk.dps.ee.io.resources;

import java.util.Optional;
import org.opt4j.core.start.Constant;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.io.json.ResourceEntry;
import at.uibk.dps.ee.io.json.ResourceInformationJsonFile;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.ResourceGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceLink;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping.EnactmentMode;
import at.uibk.dps.ee.model.properties.PropertyServiceResource;
import at.uibk.dps.ee.model.properties.PropertyServiceResourceServerless;
import net.sf.opendse.model.Resource;

/**
 * The {@link ResourceGraphProviderFile} provides access to the resource graph
 * which it builds from a .json file
 * 
 * @author Fedor Smirnov
 *
 */
@Singleton
public class ResourceGraphProviderFile implements ResourceGraphProvider {

  protected final ResourceGraph resourceGraph;

  /**
   * Injection constructor.
   * 
   * @param filePath the path to the file specifying the type mappings.
   */
  @Inject
  public ResourceGraphProviderFile(@Constant(value = "filePath",
      namespace = ResourceGraphProviderFile.class) final String filePath) {
    this.resourceGraph = readResourceGraph(filePath);
  }

  @Override
  public ResourceGraph getResourceGraph() {
    return resourceGraph;
  }

  /**
   * Reads the type mapping json file located under the provided file path.
   * Converts the information to a {@link ResourceGraph}. Returns the resource
   * graph.
   * 
   * @param filePath the filePath of the json file describing the type mapping
   * @return the resource graph built based on the information in the file
   */
  protected final ResourceGraph readResourceGraph(final String filePath) {
    final ResourceGraph result = new ResourceGraph();
    final ResourceInformationJsonFile resourceInformation =
        ResourceInformationJsonFile.readFromFile(filePath);
    // always add a node representing the EE
    final Resource eeRes = PropertyServiceResource.createResource(ConstantsEEModel.idLocalResource);
    
    result.addVertex(eeRes);
    resourceInformation.stream()
        .flatMap(functionTypeEntry -> functionTypeEntry.getResources().stream())
        .forEach(resourceEntry -> processResourceEntry(result, eeRes, resourceEntry));
    return result;
  }

  /**
   * Method to process a resource entry by adding a resource node to the graph and
   * connecting it to the EE node.
   * 
   * @param resourceGraph the resource graph
   * @param eeRes the resource modeling the EE
   * @param resEntry the resource entry
   */
  protected void processResourceEntry(final ResourceGraph resourceGraph, final Resource eeRes,
      final ResourceEntry resEntry) {
    final EnactmentMode resourceType = EnactmentMode.valueOf(resEntry.getType());
    Optional<Resource> newResourceOpt;
    if (resourceType.equals(EnactmentMode.Local)) {
      // nothing to do, EE already in the graph
      return;
    } else if (resourceType.equals(EnactmentMode.Serverless)) {
      final String uri =
          resEntry.getProperties().get(PropertyServiceResourceServerless.propNameUri).getAsString();
      newResourceOpt =
          Optional.of(PropertyServiceResourceServerless.createServerlessResource(uri, uri));

    } else {
      throw new IllegalArgumentException("Unknown resource type: " + resourceType.name());
    }
    final Resource newRes = newResourceOpt.orElseThrow();
    // annotate all properties (if not already set)
    resEntry.getProperties().entrySet().stream()
        .filter(entry -> !newRes.getAttributeNames().contains(entry.getKey()))
        .forEach(entry -> newRes.setAttribute(entry.getKey(), entry.getValue()));
    // connect resource to ee node
    PropertyServiceLink.connectResources(resourceGraph, eeRes, newRes);
  }
}
