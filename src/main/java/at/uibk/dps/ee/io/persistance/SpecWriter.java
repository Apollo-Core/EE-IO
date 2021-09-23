package at.uibk.dps.ee.io.persistance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import at.uibk.dps.ee.model.graph.AbstractConcurrentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.io.Common;
import net.sf.opendse.model.Attributes;
import net.sf.opendse.model.Edge;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Node;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Specification;
import net.sf.opendse.model.Task;
import nu.xom.Serializer;

/**
 * The {@link SpecWriter} is used to generate an XML document describing the
 * specification of a given enactment.
 * 
 * @author Fedor Smirnov
 *
 */
public class SpecWriter {

  protected static final String xmlStringSrc = "source";
  protected static final String xmlStringDst = "destination";
  protected static final String xmlStringEdgeType = "edge type";
  protected static final String xmlStringAttribute = "attribute";

  /**
   * No constructor.
   */
  private SpecWriter() {}
  
  protected static Set<Class<?>> primitives = new HashSet<Class<?>>();

  static {
      primitives.add(Boolean.class);
      primitives.add(Integer.class);
      primitives.add(Character.class);
      primitives.add(Double.class);
      primitives.add(String.class);
  }

  protected static boolean isPrimitive(Class<?> cls) {
      return cls.isPrimitive() || primitives.contains(cls);
  }


  public static void writeSpecification(EnactmentSpecification spec, String fileName) {
    writeSpecification(spec, new File(fileName));
  }

  protected static void writeSpecification(EnactmentSpecification spec, File file) {
    try {
      FileOutputStream out = new FileOutputStream(file);
      writeSpecification(spec, out);
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected static void writeSpecification(EnactmentSpecification spec, OutputStream out) {
    nu.xom.Element eSpec = specToElement(spec);
    eSpec.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    eSpec.addAttribute(
        new nu.xom.Attribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance",
            "http://opendse.sourceforge.net http://opendse.sourceforge.net/schema.xsd"));
    nu.xom.Document doc = new nu.xom.Document(eSpec);
    try {
      Serializer serializer = new Serializer(out);
      serializer.setIndent(2);
      serializer.setMaxLength(2000);
      serializer.write(doc);
      serializer.flush();
    } catch (IOException ex) {
      System.out.println(ex + " " + out);
    }
  }

  protected static nu.xom.Element specToElement(EnactmentSpecification spec) {
    nu.xom.Element result = new nu.xom.Element(spec.getClass().getName());
    result.appendChild(eGraphToElement(spec.getEnactmentGraph()));
    result.appendChild(rGraphToElement(spec.getResourceGraph()));
    result.appendChild(mappingsToElement(spec.getMappings()));
    result.appendChild(attributesToElement(spec.getAttributes()));
    return result;
  }

  protected static nu.xom.Element eGraphToElement(EnactmentGraph eGraph) {
    return graphToElement(eGraph);
  }

  protected static nu.xom.Element rGraphToElement(ResourceGraph rGraph) {
    return graphToElement(rGraph);
  }

  protected static <N extends Node, E extends Edge> nu.xom.Element graphToElement(
      AbstractConcurrentGraph<N, E> aGraph) {
    nu.xom.Element result = new nu.xom.Element(aGraph.getClass().getName());
    aGraph.getVertices().forEach(node -> result.appendChild(NodeToElement(node)));
    aGraph.getEdges().forEach(edge -> result.appendChild(EdgeToElement(edge, aGraph.getSource(edge),
        aGraph.getDest(edge), aGraph.getEdgeType(edge))));
    return result;
  }

  protected static nu.xom.Element mappingsToElement(MappingsConcurrent mappings) {
    nu.xom.Element result = new nu.xom.Element(mappings.getClass().getName());
    mappings.mappingStream().forEach(mapping -> result.appendChild(mappingToElement(mapping)));
    return result;
  }

  protected static nu.xom.Element mappingToElement(Mapping<Task, Resource> mapping) {
    nu.xom.Element result = new nu.xom.Element(mapping.getClass().getName());
    result.addAttribute(new nu.xom.Attribute(xmlStringSrc, mapping.getSource().getId()));
    result.addAttribute(new nu.xom.Attribute(xmlStringDst, mapping.getTarget().getId()));
    result.appendChild(attributesToElement(mapping.getAttributes()));
    return result;
  }

  protected static nu.xom.Element NodeToElement(Node node) {
    nu.xom.Element result = new nu.xom.Element(node.getClass().getName());
    result.appendChild(attributesToElement(node.getAttributes()));
    return result;
  }

  protected static nu.xom.Element EdgeToElement(Edge edge, Node src, Node dst, EdgeType edgeType) {
    nu.xom.Element result = new nu.xom.Element(edge.getClass().getName());
    result.addAttribute(new nu.xom.Attribute(xmlStringSrc, src.getId()));
    result.addAttribute(new nu.xom.Attribute(xmlStringDst, dst.getId()));
    result.addAttribute(new nu.xom.Attribute(xmlStringEdgeType, edgeType.name()));
    result.appendChild(attributesToElement(edge.getAttributes()));
    return result;
  }

  protected static nu.xom.Element attributesToElement(Attributes attributes) {
    nu.xom.Element result = new nu.xom.Element(attributes.getClass().getName());
    attributes.getAttributeNames().stream()
        .filter(attrName -> attributes.getAttribute(attrName) != null).forEach(strAttrName -> result
            .appendChild(attributeToElement(strAttrName, attributes.getAttribute(strAttrName))));
    return result;
  }

  protected static nu.xom.Element attributeToElement(String attrName, Object attribute) {
    nu.xom.Element result = new nu.xom.Element(xmlStringAttribute);
    Class<?> cls = attribute.getClass();
    if (isPrimitive(cls)) {
      result.addAttribute(new nu.xom.Attribute("type", getType(cls)));
      result.appendChild(attribute.toString());

    }
  }
}
