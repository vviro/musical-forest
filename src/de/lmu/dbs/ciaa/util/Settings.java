package de.lmu.dbs.ciaa.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import de.lmu.dbs.ciaa.classifier.ForestParameters;
import de.lmu.dbs.ciaa.classifier.features.Feature;

/**
 * Class for loading and managing the settings for the application.
 * 
 * @author Thomas Weber
 *
 */
public class Settings {

	/**
	 * The configuration file (XML)
	 */
	protected String configFile = null;
	
	/**
	 * XML schema for the configuration file
	 */
	protected String configFileSchema = null;

	/**
	 * Root element, contains the XML tree.
	 */
	protected Element root;
	
	/**
	 * Create settings object from an unvalidated XML config file.
	 * 
	 * @param configFile
	 * @throws Exception 
	 */
	public Settings(String configFile) throws Exception {
		this(configFile, null);
	}
	
	/**
	 * Create settings object from a validated XML config file.
	 * 
	 * @param configFile
	 * @throws Exception 
	 */
	public Settings(String configFile, String configFileSchema) throws Exception {
		this.configFile = configFile;
		this.configFileSchema = configFileSchema;
		parseConfigFile(this.configFileSchema != null);
	}

	/**
	 * Generates a parameter set for forest growing. You have to set some of
	 * the params manually...see class documentation of RandomTreeParameters.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public ForestParameters getForestParameters() throws Exception {
		Element general = root.getChild("General");
		Element forest = root.getChild("Forest");
		Element transform = root.getChild("Transformation");
		ForestParameters r = new ForestParameters();

		// General
		r.workingFolder = general.getAttributeValue("workingFolder");
		r.nodedataFilePrefix = general.getAttributeValue("nodedataFilePrefix");
		r.frequencyTableFile = general.getAttributeValue("frequencyTableFile");
		r.loadForest = Boolean.parseBoolean(general.getAttributeValue("loadForest"));
		
		r.logProgress = Boolean.parseBoolean(general.getAttributeValue("logProgress"));
		r.logNodeInfo = Boolean.parseBoolean(general.getAttributeValue("logNodeInfo"));
		r.saveGainThresholdDiagrams = Integer.parseInt(general.getAttributeValue("saveGainThresholdDiagrams"));
		r.saveNodeClassifications = Integer.parseInt(general.getAttributeValue("saveNodeClassifications"));
		r.debugThreadForking = Boolean.parseBoolean(general.getAttributeValue("debugThreadForking"));
		r.debugThreadPolling = Boolean.parseBoolean(general.getAttributeValue("debugThreadPolling"));
		
		// Forest
		r.forestSize = Integer.parseInt(forest.getAttributeValue("forestSize"));
		r.maxDepth = Integer.parseInt(forest.getAttributeValue("maxDepth"));
		r.maxNumOfNodeThreads = Integer.parseInt(forest.getAttributeValue("maxNumOfNodeThreads"));
		r.numOfWorkerThreadsPerNode = Integer.parseInt(forest.getAttributeValue("numOfWorkerThreadsPerNode"));
		r.threadWaitTime = Integer.parseInt(forest.getAttributeValue("threadWaitTime"));
		r.percentageOfRandomValuesPerFrame = Double.parseDouble(forest.getAttributeValue("percentageOfRandomValuesPerFrame"));
		r.numOfRandomFeatures = Integer.parseInt(forest.getAttributeValue("numOfRandomFeatures"));
		r.entropyThreshold = Double.parseDouble(forest.getAttributeValue("entropyThreshold"));
		r.xMin = Integer.parseInt(forest.getAttributeValue("xMin"));
		r.xMax = Integer.parseInt(forest.getAttributeValue("xMax"));
		r.yMin = Integer.parseInt(forest.getAttributeValue("yMin"));
		r.yMax = Integer.parseInt(forest.getAttributeValue("yMax"));
		r.thresholdMax = Integer.parseInt(forest.getAttributeValue("thresholdMax"));
		r.thresholdCandidatesPerFeature = Integer.parseInt(forest.getAttributeValue("thresholdCandidatesPerFeature"));

		// CQT
		r.binsPerOctave = Double.parseDouble(transform.getAttributeValue("binsPerOctave"));
		r.fMin = Double.parseDouble(transform.getAttributeValue("fMin"));
		r.fMax = Double.parseDouble(transform.getAttributeValue("fMax"));
		r.step = Integer.parseInt(transform.getAttributeValue("step"));
		r.threshold = Double.parseDouble(transform.getAttributeValue("threshold"));
		r.spread = Double.parseDouble(transform.getAttributeValue("spread"));
		r.divideFFT = Double.parseDouble(transform.getAttributeValue("divideFFT"));
		r.cqtBufferLocation = transform.getAttributeValue("cqtBufferLocation");
		
		String clsName = forest.getAttributeValue("featureFactoryClass");
		r.featureFactory = (Feature)Class.forName(clsName).getConstructor().newInstance();
		return r;
	}
	
	/**
	 * Parses the config XML file using SAXBuilder from the <code>JDOM</code> package.
	 *
	 * @param validate validate the XML to the schema in this.configFileSchema
	 * @throws Exception 
	 */
	protected void parseConfigFile(boolean validate) throws Exception {
		if (configFile == null) throw new Exception("No configuration file has been specified");
		
		// Get SAXBuilder instance (with XSD validation)
		SAXBuilder parser = null;
		if (validate) {
			System.out.println("Parsing and validating configuration file " + configFile + " (validation schema: " + configFileSchema + ")");
			
			parser = new SAXBuilder(XMLReaders.XSDVALIDATING);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", configFileSchema); // set the schema manually, because automatic locating is not working inside WEB-INF
		} else {
			System.out.println("Parsing configuration file " + configFile);
			
			parser = new SAXBuilder();
		}
		 
		Document doc = null; 
		doc = parser.build(configFile); // parse XML
		root = doc.getRootElement(); // Get root node of XML
		//process = root.getChild("Process");
	}
	
}
