package at.uibk.dps.ee.io.script;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opt4j.core.config.ModuleLoader;
import org.opt4j.core.config.ModuleRegister;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.google.inject.Module;

/**
 * The {@link ModuleLoaderString} receives a string in xml formal and converts
 * it to a set of modules.
 * 
 * @author Fedor Smirnov
 */
public class ModuleLoaderString extends ModuleLoader {

  /**
   * See parent constructor
   * 
   */
  public ModuleLoaderString(final ModuleRegister moduleRegister) {
    super(moduleRegister);
  }

  /**
   * Reads the given config string, converts it into an xml document, and returns
   * the modules contained therein.
   * 
   * @param xmlString the configuration string in xml format
   * @return the configured modules
   */
  public Set<Module> loadModulesFromString(final String xmlString) {
    final Set<Module> modules = new HashSet<>();
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder builder = factory.newDocumentBuilder();
      final Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
      modules.addAll(get(doc.getFirstChild()));
    } catch (ParserConfigurationException parserExc) {
      throw new IllegalStateException("ParserConfigException during module load.", parserExc);
    } catch (SAXException saxExc) {
      throw new IllegalStateException("SaxException during module load.", saxExc);
    } catch (IOException ioExc) {
      throw new IllegalStateException("IoException during module load.", ioExc);
    }
    return modules;
  }
}
