package at.uibk.dps.ee.io.spec;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opt4j.core.start.Constant;
import com.google.inject.Inject;
import at.uibk.dps.ee.io.json.ResourceEntry;
import at.uibk.dps.ee.io.json.ResourceInformationJsonFile;
import at.uibk.dps.ee.io.resources.ResourceGraphProviderFile;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.ResourceGraphProvider;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUser;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMappingLocal;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping.EnactmentMode;
import at.uibk.dps.ee.model.properties.PropertyServiceResourceServerless;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link SpecificationProviderFile} creates the specification by taking the
 * enactment and the resource graph and connecting them using the mappings it
 * creates based on the resource description file.
 * 
 * @author Fedor Smirnov
 *
 */
public class SpecificationProviderFile implements SpecificationProvider {

  protected final EnactmentGraphProvider enactmentGraphProvider;
  protected final ResourceGraphProvider resourceGraphProvider;
  protected final MappingsConcurrent mappings;
  protected final EnactmentSpecification specification;

  /**
   * Injection constructor.
   * 
   * @param enactmentGraphProvider class providing the {@link EnactmentGraph}
   * @param resourceGraphProvider class providing the {@link ResourceGraph}
   * @param filePath path to the file describing the functionType-to-resource
   *        relations
   */
  @Inject
  public SpecificationProviderFile(final EnactmentGraphProvider enactmentGraphProvider,
      final ResourceGraphProvider resourceGraphProvider, @Constant(value = "filePath",
          namespace = ResourceGraphProviderFile.class) final String filePath) {
    this.enactmentGraphProvider = enactmentGraphProvider;
    this.resourceGraphProvider = resourceGraphProvider;
    this.mappings = createMappings(getEnactmentGraph(), getResourceGraph(), filePath);
    this.specification = new EnactmentSpecification(getEnactmentGraph(), getResourceGraph(),
        getMappings(), ConstantsEEModel.SpecIdDefault);
  }

  @Override
  public final ResourceGraph getResourceGraph() {
    return resourceGraphProvider.getResourceGraph();
  }

  @Override
  public final EnactmentGraph getEnactmentGraph() {
    return enactmentGraphProvider.getEnactmentGraph();
  }

  @Override
  public final MappingsConcurrent getMappings() {
    return mappings;
  }

  /**
   * Reads the json file with the file information and uses it to create the
   * mappings.
   * 
   * @param eGraph the enactment graph
   * @param rGraph the resource graph
   * @param filePath the file path to the resource information
   * @return the mappings connected the eGraph and the rGraph
   */
  protected final MappingsConcurrent createMappings(final EnactmentGraph eGraph,
      final ResourceGraph rGraph, final String filePath) {
    final MappingsConcurrent result = new MappingsConcurrent();
    final ResourceInformationJsonFile resInfo = ResourceInformationJsonFile.readFromFile(filePath);
    eGraph.getVertices().stream().filter(task -> TaskPropertyService.isProcess(task))
        .filter(task -> PropertyServiceFunction.getUsageType(task).equals(UsageType.User))
        .flatMap(task -> getMappingsForTask(task, resInfo, rGraph).stream())
        .forEach(mapping -> result.addMapping(mapping));
    return result;
  }

  /**
   * Creates the mappings for the provided task based on the given resource
   * information.
   * 
   * @param task the provided task
   * @param resInfo the given resource information.
   * @param rGraph the resource graph
   * @return the mappings for the provided task based on the given resource
   *         information
   */
  protected Set<Mapping<Task, Resource>> getMappingsForTask(final Task task,
      final ResourceInformationJsonFile resInfo, final ResourceGraph rGraph) {
    final String funcTypeString = PropertyServiceFunctionUser.getTypeId(task);
    return resInfo.stream()
        .filter(functionEntry -> funcTypeString.equals(functionEntry.getFunctionType()))
        .flatMap(functionEntry -> functionEntry.getResources().stream())
        .map(resEntry -> createMapping(task, resEntry, rGraph)).collect(Collectors.toSet());
  }

  /**
   * Creates a mapping between the given task and the given resources and
   * annotates it with properties following the resource entry.
   * 
   * @param task the mapping source
   * @param resEntry the resource entry describing the attributes
   * @param resGraph the resource graph
   * @return a mapping between the given task and the given resources and
   *         annotates it with properties following the resource entry.
   */
  protected Mapping<Task, Resource> createMapping(final Task task, final ResourceEntry resEntry,
      final ResourceGraph resGraph) {
    final Resource res = getResourceForResourceEntry(resGraph, resEntry);
    final String resType = resEntry.getType();
    if (resType.equals(EnactmentMode.Local.name())) {
      return getLocalMappingEdge(task, res, resEntry);
    } else if (resType.equals(EnactmentMode.Serverless.name())) {
      return PropertyServiceMapping.createMapping(task, res, EnactmentMode.Serverless,
          PropertyServiceResourceServerless.getUri(res));
    } else if (resType.equals(EnactmentMode.Demo.name())) {
      return PropertyServiceMapping.createMapping(task, res, EnactmentMode.Demo, "demo");
    } else {
      throw new IllegalStateException(
          "Resource entry with unknown enactment mode: " + resEntry.getType());
    }
  }

  /**
   * Creates and annotates the local mapping between the given task and the local
   * resource.
   * 
   * @param task the mapped task
   * @param local the local resource
   * @param resEntry the resource entry
   * @return the local mapping between the given task and the local resource
   */
  protected Mapping<Task, Resource> getLocalMappingEdge(final Task task, final Resource local,
      final ResourceEntry resEntry) {
    if (!resEntry.getProperties().containsKey(PropertyServiceMappingLocal.propNameImage)) {
      throw new IllegalArgumentException(
          "Local resource entries must specify an image: " + resEntry);
    }
    final String imageName =
        resEntry.getProperties().get(PropertyServiceMappingLocal.propNameImage).getAsString();
    // create the mapping edge for the spec
    return PropertyServiceMappingLocal.createMappingLocal(task, local, imageName);
  }

  /**
   * Gets the resource node matching the provided resource entry
   * 
   * @param rGraph the resource graph
   * @param resEntry the resource entry
   * @return the resource node matching the provided resource entry
   */
  protected Resource getResourceForResourceEntry(final ResourceGraph rGraph,
      final ResourceEntry resEntry) {
    Optional<Resource> result;
    final String resType = resEntry.getType();
    if (resType.equals(EnactmentMode.Local.name()) || resType.equals(EnactmentMode.Demo.name())) {
      // Resource is local EE
      result = Optional.ofNullable(rGraph.getVertex(ConstantsEEModel.idLocalResource));
    } else if (resType.equals(EnactmentMode.Serverless.name())) {
      // Serverless resource => look for the Uri
      if (!resEntry.getProperties().containsKey(PropertyServiceResourceServerless.propNameUri)) {
        throw new IllegalArgumentException("No Uri annotated for serverless resource");
      }
      final String uri =
          resEntry.getProperties().get(PropertyServiceResourceServerless.propNameUri).getAsString();
      result = Optional.ofNullable(rGraph.getVertex(uri));
    } else {
      throw new IllegalArgumentException("Unknown resource type: " + resEntry.getType());
    }
    return result.orElseThrow();
  }

  @Override
  public EnactmentSpecification getSpecification() {
    return specification;
  }
}
