package de.lmu.dbs.jforest.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

/**
 * XML parameter file loader base class.
 * 
 * @author Thomas Weber
 *
 */
public abstract class ParamLoader {

	/**
	 * Parses the config XML file using SAXBuilder from the <code>JDOM</code> package.
	 *
	 * @param validate validate the XML to the schema in this.configFileSchema
	 * @throws Exception 
	 */
	public Element parseConfigFile(String configFile, String configFileSchema) throws Exception {
		if (configFile == null) throw new Exception("No configuration file has been specified");
		
		// Get SAXBuilder instance (with XSD validation)
		SAXBuilder parser = null;
		if (configFileSchema != null) {
			//System.out.println("Parsing and validating configuration file " + configFile + " (validation schema: " + configFileSchema + ")");
			
			parser = new SAXBuilder(XMLReaders.XSDVALIDATING);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", configFileSchema); // set the schema manually, because automatic locating is not working inside WEB-INF
		} else {
			//System.out.println("Parsing configuration file " + configFile);
			
			parser = new SAXBuilder();
		}
		 
		Document doc = null; 
		doc = parser.build(configFile); // parse XML
		return doc.getRootElement(); // Get root node of XML
	}

}
