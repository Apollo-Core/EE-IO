package at.uibk.dps.ee.io.json;

import java.util.Map;

import com.google.gson.JsonElement;

/**
 * Each resource entry contains the information about a particular resource.
 * 
 * @author Fedor Smirnov
 */
public class ResourceEntry {

  // corresponds to the enactment mode
  protected String type;
  // corresponds to the implementation ID
  protected String implementationId;
  protected Map<String, JsonElement> properties;

  /**
   * Default constructor used by Gson.
   * 
   * @param type string describing the resource type
   * @param properties map of properties
   */
  public ResourceEntry(final String type, final String implementationId,
      final Map<String, JsonElement> properties) {
    this.type = type;
    this.implementationId = implementationId;
    this.properties = properties;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getImplementationId() {
    return implementationId;
  }

  public void setImplementationId(String implementationId) {
    this.implementationId = implementationId;
  }

  public Map<String, JsonElement> getProperties() {
    return properties;
  }

  public void setProperties(final Map<String, JsonElement> properties) {
    this.properties = properties;
  }
}
