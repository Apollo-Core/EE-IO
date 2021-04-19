package at.uibk.dps.ee.io.json;

import java.util.List;

/**
 * Each {@link FunctionTypeEntry} contains the information about the resources
 * that functions of a certain type can be enacted on.
 * 
 * @author Fedor Smirnov
 */
public class FunctionTypeEntry {

  // corresponds to the TypeID
  protected String functionType;
  protected List<ResourceEntry> resources;

  /**
   * Default constructor used by Gson
   * 
   * @param functionType the function type string
   * @param resources the list of resource entries
   */
  public FunctionTypeEntry(final String functionType, final List<ResourceEntry> resources) {
    this.functionType = functionType;
    this.resources = resources;
  }

  public String getFunctionType() {
    return functionType;
  }

  public void setFunctionType(final String functionType) {
    this.functionType = functionType;
  }

  public List<ResourceEntry> getResources() {
    return resources;
  }

  public void setResources(final List<ResourceEntry> resources) {
    this.resources = resources;
  }
}
