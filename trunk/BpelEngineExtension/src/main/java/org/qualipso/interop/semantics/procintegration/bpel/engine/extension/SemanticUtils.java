/*
 * IST-FP6-034763 QualiPSo: QualiPSo is a unique alliance
 * of European, Brazilian and Chinese ICT industry players,
 * SMEs, governments and academics to help industries and
 * governments fuel innovation and competitiveness with Open
 * Source software. To meet that goal, the QualiPSo consortium
 * intends to define and implement the technologies, processes
 * and policies to facilitate the development and use of Open
 * Source software components, with the same level of trust
 * traditionally offered by proprietary software. QualiPSo is
 * partially funded by the European Commission under EU’s sixth
 * framework program (FP6), as part of the Information Society
 * Technologies (IST) initiative. 
 *
 * This program has been created as part of the QualiPSo work
 * package on "Semantic Interoperability". The basic idea of this work    
 * package is to demonstrate how semantic technologies can be used to 
 * cope with the diversity and heterogeneity of software and services 
 * in the OSS domain.
 *
 * You can redistribute this program and/or modify it under the terms 
 * of the GNU Lesser General Public License version 3 (LGPL v3.0).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 *
 * You should have received a copy of the LGPL
 * along with this program; if not, please have a look at
 * http://www.gnu.org/licenses/lgpl.html 
 * to obtain the full license text.
 *
 * Author of this program: 
 * Fraunhofer Institute FOKUS, http://www.fokus.fraunhofer.de
 *
 *
*/

package org.qualipso.interop.semantics.procintegration.bpel.engine.extension;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jess.Rete;

import org.apache.log4j.Logger;
import org.jaxen.FunctionCallException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.hp.hpl.jena.rdf.arp.ARP;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.XSD;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.inference.pellet.ProtegePelletOWLAPIReasoner;
import edu.stanford.smi.protegex.owl.inference.protegeowl.ReasonerManager;
import edu.stanford.smi.protegex.owl.inference.reasoner.ProtegeReasoner;
import edu.stanford.smi.protegex.owl.inference.reasoner.exception.ProtegeReasonerException;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.jena.parser.ProtegeOWLParser;
import edu.stanford.smi.protegex.owl.jena.parser.ProtegeOWLParser.ARPInvokation;
import edu.stanford.smi.protegex.owl.model.NamespaceManager;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;
import edu.stanford.smi.protegex.owl.model.OWLOntology;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.impl.DefaultOWLIndividual;
import edu.stanford.smi.protegex.owl.model.triplestore.TripleStore;
import edu.stanford.smi.protegex.owl.swrl.bridge.exceptions.SWRLRuleEngineBridgeException;
import edu.stanford.smi.protegex.owl.swrl.bridge.jess.SWRLJessBridge;

/**
 * Provides several semantic utility methods for handling ontology models.
 */
public class SemanticUtils {

    /**
     * Property for setting indent levels.
     */
    protected static final String INDENT_AMOUNT_XALAN = "{http://xml.apache.org/xalan}indent-amount";
    
    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(SemanticUtils.class);

    /**
     * Output some statistics of the SWRL bridge to the debug log.
     * 
     * @param logger
     *            the logger
     * @param bridge
     *            the bridge
     */
    public static void logBridgeStatistics(Logger logger, SWRLJessBridge bridge) {
        logger.debug("number of imported classes : " + bridge.getNumberOfImportedClasses());
        logger.debug("number of imported axioms : " + bridge.getNumberOfImportedAxioms());
        logger.debug("number of imported property assertion axioms : "
                + bridge.getNumberOfImportedPropertyAssertionAxioms());
        logger.debug("number of imported rules : " + bridge.getNumberOfImportedSWRLRules());
        logger.debug("number of imported individuals : " 
                + bridge.getNumberOfImportedIndividuals());
        logger.debug("number of inferred individuals : " 
                + bridge.getNumberOfInferredIndividuals());
        logger.debug("number of inferred property assertion axioms : "
                + bridge.getNumberOfInferredPropertyAssertionAxioms());
    }

    /**
     * Transforms a DOM into it´s String-representation.
     * 
     * @param aElement
     *            the DOM
     * @return String the resulting string
     * @throws TransformerException 
     */
    public static String elementToXMLString(Object aElement) throws TransformerException
    {
        DOMSource source = new DOMSource((Node) aElement);
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);

        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(DOMSource.FEATURE) && tfactory.getFeature(StreamResult.FEATURE)) {
            Transformer transformer = tfactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(INDENT_AMOUNT_XALAN, "3");

            transformer.transform(source, result);
        }

        return stringWriter.toString();
    }

    /**
     * Parses a RDF/XML-serialization and returns the base-URI if a value for
     * 'xml:base' was set, <code>null</code> otherwise.
     * 
     * @param rdfString
     *            RDF/XML-serialization to parse
     * @param generateURI TODO
     * @return the base-URI
     * @throws URISyntaxException
     */
    public static URI extractOntologyNameUriFromRDF(String rdfString, boolean generateURI) 
            throws URISyntaxException {
        int startPos = -1;
        int endPos = -1;
        String uri = null;
        
        startPos = rdfString.indexOf("xml:base=\"") + 10;
        endPos = rdfString.indexOf("\"", startPos + 1);
        uri = rdfString.substring(startPos, endPos);

        // "xml:base" not found --> look for default namespace
        if (startPos == 9) {
            startPos = rdfString.indexOf("xmlns=\"") + 7;
            endPos = rdfString.indexOf("\"", startPos + 1) - 1; // -1 because protege puts a '#' after namespace-declaration
            uri = rdfString.substring(startPos, endPos);
            
            if (startPos != 6)
            {
                // write the uri as base-URI into the rdf:RDF-element, 
                // because it is necessary for importing into Protege
                rdfString = rdfString.substring(0, endPos + 2)
                            + " xml:base=\"" + uri + "\" "
                            + rdfString.substring(endPos + 2);
            }
        }

        // default-namespace not found --> look for ontology name
        if (startPos == 6) {
            startPos = rdfString.indexOf("Ontology rdf:about=\"") + 20;
            endPos = rdfString.indexOf("\"", startPos + 1);
            uri = rdfString.substring(startPos, endPos);
        } 

        if (startPos == 19) {
            // no URI found that can be used as the name
            int randomNumber = new Random().nextInt();
            if (randomNumber < 0)
            {
                randomNumber *= -1;
            }
            uri = "http://generatedOntologyName" + randomNumber + ".owl";
        }

        return new URI(uri);
    }
    /**
     * Parses a RDF/XML-serialization for the xml:base URI 
     * and if it is not present in the string set it
     * according to the (hopefully present) valuie for 'xmlns='.
     * 
     * @param rdfString
     *            RDF/XML-serialization to repair the xml:base attibute
     * 
     * @return the resulting rdf-xml-serialisation
     * 
     */
    public static String repairBaseUriInRDFSerialisation(String rdfString) {
        int startPos = -1;
        int endPos = -1;
        String uri = null;
        
        startPos = rdfString.indexOf("xml:base=\"") + 10;
        endPos = rdfString.indexOf("\"", startPos + 1);
        uri = rdfString.substring(startPos, endPos);

        // "xml:base" not found --> look for default namespace
        if (startPos == 9) {
            startPos = rdfString.indexOf("xmlns=\"") + 7;
            endPos = rdfString.indexOf("\"", startPos + 1) - 1; // -1 because protege puts a '#' after namespace-declaration
            uri = rdfString.substring(startPos, endPos);
            
            if (startPos != 6) {
                // write the uri as base-URI into the rdf:RDF-element, 
                // because it is necessary for importing into Protege
                rdfString = rdfString.substring(0, endPos + 2)
                            + " xml:base=\"" + uri + "\" "
                            + rdfString.substring(endPos + 2);
            }
        }

        return rdfString;
    }

    /**
     * Parse the incoming DOM-model for the first occurring RDF-node and return
     * this node.
     * 
     * @param domModel
     * @return the RDF-root-node
     * @throws InvalidOWLModelSerialzationException
     */
    public static Node findRdfNode(Node domModel) throws Exception {
        // XPathAPI.selectSingleNode((Node) owlIndividual, "//rdf:RDF | /RDF
        // | /*/rdf:RDF | //*[@xmlns:rdf] | type");
    
        Node curNode = domModel;
        // for debug: SemanticUtils.elementToXMLString(curNode);
    
        while (curNode.getNodeName() != null
                && curNode.getNodeName().toLowerCase().indexOf("rdf") == -1) {
            if (curNode.hasChildNodes()) {
                curNode = findRdfNode(curNode.getFirstChild());
            } else if (curNode.getNextSibling() != null) {
                curNode = curNode.getNextSibling();
            } else {
                return curNode;
            }
        }
    
    
        return curNode;
    }

    /**
     * Parses the given XML String and returns a Document.
     * 
     * @param aXmlString
     *            the string to parse
     * @param rootElementName the name of the wrapping element or <code>null</code>
     *            if no wrapping element should be put arround the content
     * @return Document the DOM
     * @throws FunctionCallException
     */
    public static Document xmlStringToElement(Object aXmlString, String rootElementName)
            throws FunctionCallException {
        if (!(aXmlString instanceof String)) {
            logger
                    .error("Custom XPath Error in xmlStringToElement: " +
                            "argument not of type java.lang.String, it's a "
                            + aXmlString.getClass().toString());
            return null;
        }

        try {
            String documentString = (String) aXmlString;

            // Workaround: We have to encapsulate the xmlString with one more
            // "rootElement",
            // because the rootelement is automatically renamed during SOAP
            // encoding
            // and we don´t want to loose the original name (which should be
            // "RDF") of the document root.
            if (rootElementName != null) {
                documentString = "<" + rootElementName + ">" + documentString + "</"
                        + rootElementName + ">";
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);
            factory.setValidating(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(documentString.getBytes()));

            return document;

        } catch (Exception e) {
            throw new FunctionCallException("Custom XPath Error - xmlStringToElement"
                    + e.getLocalizedMessage());
        }
    }

    /**
     * This method removes bad namespace declarations such as "p1 = http://".
     * This is a workaround for a bug in the namespace mechanism of Protege3.4
     * build 128. caused by creating objects that do not have a namespace
     * declaration
     * 
     * @param bridgingModel
     *            the model where bad namespace declarations should be removed
     */
    public static JenaOWLModel removeBadNamespaceDeclarations(JenaOWLModel bridgingModel) {
        NamespaceManager namespaceManager;
        namespaceManager = bridgingModel.getNamespaceManager();
        
        String badUriNamespacePrefix = namespaceManager.getPrefix("http://");
        while (badUriNamespacePrefix != null) {
            // remove the bad prefix which causes problems
            namespaceManager.removePrefix(badUriNamespacePrefix);
            // check for other bad URIs
            badUriNamespacePrefix = namespaceManager.getPrefix("http://");
        }

        badUriNamespacePrefix = namespaceManager.getPrefix(
                "http://dummy-ontologies.com/dummy.owl#");
        while (badUriNamespacePrefix != null) {
            // remove the bad prefix which causes problems
            namespaceManager.removePrefix(badUriNamespacePrefix);
            // check for other bad URIs
            badUriNamespacePrefix = namespaceManager.getPrefix(
                    "http://dummy-ontologies.com/dummy.owl#");
        }
        return bridgingModel;
    }

    /**
     * Creates an instance for reasoning over the given owl model. There are two
     * implementations that support a direct connection to pellet:
     * ProtegePelletOWLAPIReasoner and ProtegePelletJenaReasoner Currently the
     * ProtegePelletOWLAPIReasoner, is used internally, since this is
     * recommended by protege developers. However, ProtegePelletJenaReasoner
     * does transfer SWRL rules to Pellet. So it should be considered to use
     * this implementation as soon as Pellet supports SWRL build-ins. This is
     * not the case with Pellet version 1.5.1 yet.
     * 
     * @param owlModel
     *            the owl model to reason over.
     * 
     * @return an instance of a Protege reasoner
     */
    public static final ProtegeReasoner createProtegeReasoner(JenaOWLModel owlModel) {

        // Get the reasoner manager and obtain a reasoner for the OWL model.
        ReasonerManager reasonerManager = ReasonerManager.getInstance();

        // Get an instance of the Protege Pellet reasoner
        // ProtegeReasoner reasoner =
        // reasonerManager.createProtegeReasoner(owlModel,
        // ProtegePelletJenaReasoner.class);
        ProtegeReasoner reasoner = reasonerManager.createProtegeReasoner(owlModel,
                ProtegePelletOWLAPIReasoner.class);

        return reasoner;
    }

    /**
     * Perform consistency checks on the model, then classifies, executed SWRL
     * rules and reclassifies instances in the model.
     * 
     * @param jenaModel
     *            the model to reason on
     * 
     * @throws ProtegeReasonerException
     * @throws SWRLRuleEngineBridgeException
     */
    public static void performOWLandSWRLreasoning(JenaOWLModel jenaModel)
            throws ProtegeReasonerException, SWRLRuleEngineBridgeException {

        // make sure the top-triple-strore is the active one, so that 
        // inferred knowledge is stored there
        String active = jenaModel.getTripleStoreModel().getActiveTripleStore().getName();
        jenaModel.getTripleStoreModel().setActiveTripleStore(jenaModel.getTripleStoreModel()
                .getTopTripleStore());
        active = jenaModel.getTripleStoreModel().getActiveTripleStore().getName();

        // create the reasoner for an initial check of the bridging model
        ProtegeReasoner reasoner = SemanticUtils.createProtegeReasoner(jenaModel);
        
        // perform owl reasoning
//        reasoner.computeInconsistentConcepts();
//        reasoner.classifyTaxonomy();
//        reasoner.computeInferredIndividualTypes();      

        // Create Jess rule engine and the bridge
        // to copy swrl-rules and owl-facts to Jess.
        Rete rete = new Rete();
        SWRLJessBridge bridge = null;

        // create the bridge from SWRL to Jess-Rules
        bridge = new SWRLJessBridge(jenaModel, rete);

        // just in case...
        bridge.reset();
        bridge.resetRuleEngine();
        
        // 1. copy swrl-rules and owl-facts to Jess
        // 2. fire the SWRL-rules
        // 2.b) debug output
        // 3. updates the Protege-OWL-model with new facts
        bridge.importSWRLRulesAndOWLKnowledge();
        bridge.run();
        // debug
        SemanticUtils.logBridgeStatistics(logger, bridge);
        bridge.writeInferredKnowledge2OWL();
                
        // debug
        if (logger.isDebugEnabled()) {
            SemanticUtils.logBridgeStatistics(logger, bridge);
        }

        // after the semantic bridge was executed,
        // again perform owl reasoning
        reasoner.computeInconsistentConcepts();
        reasoner.classifyTaxonomy();
        reasoner.computeInferredIndividualTypes();
        
//        // debug: save model
//        Collection clses = jenaModel.getUserDefinedOWLNamedClasses();
//        OntModel ontModel = new JenaCreator(jenaModel, false, true,
//        clses,null).createOntModel();
//        try {
//             final String ns = jenaModel.getNamespaceManager().getDefaultNamespace();
//             String ontologyName = jenaModel.getDefaultOWLOntology().getNamespace();
//             String fileName = "afterReasoning_" + ontologyName.substring(
//                    ontologyName.lastIndexOf("/")+1);
//    
//             File tempFile2 = File.createTempFile(fileName, ".owl");
//             JenaOWLModel.save(tempFile2, ontModel, FileUtils.langXMLAbbrev, ns);
//        }
//        catch (IOException ex) {
//            //do something
//        }
//        
//        Vector errors2 = new Vector(); 
//        String ontologyName = jenaModel.getDefaultOWLOntology().getNamespace();
//        String fileName = ontologyName.substring(ontologyName.lastIndexOf("/")+1);
//        try {
//            File tempFile2 = File.createTempFile(fileName, ".owl");
////            FileOutputStream fos = new FileOutputStream(tempFile2);
////            jenaModel.getOntModel().writeAll(fos, FileUtils.langXMLAbbrev, ontologyName);
//            JenaOWLModel.save(tempFile2, jenaModel.getOntModel(), 
//                  FileUtils.langXMLAbbrev, jenaModel.getDefaultOWLOntology().getNamespace());
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        if (!errors2.isEmpty())
//        {
//            for (Iterator iterator = errors2.iterator(); iterator.hasNext();) {
//                Object error = iterator.next();
//                logger.debug("Error: " + error);
//            }
//        }
    }

    /**
     * Import the given container (representing a model) into the specified
     * model.
     * 
     * @param container
     *            the model to be imported (as RDF/XML DOM-Node)
     * @param model
     *            the model to import into
     * @throws Exception 
     */
    public void importIntoModel(Node container, JenaOWLModel owlModel) 
            throws Exception
    {
        OWLOntology defaultOWLOntology = owlModel.getDefaultOWLOntology();

        // write model to string
        Node modelRoot = SemanticUtils.findRdfNode(container);
        String modelString = SemanticUtils.elementToXMLString(modelRoot);

        // parse the name of the ontology to be imported
        URI ontologyURI = SemanticUtils.extractOntologyNameUriFromRDF(modelString, true);

        TripleStore activeTripleStore = owlModel.getTripleStoreModel().getActiveTripleStore();
        TripleStore ts = owlModel.getTripleStoreModel().createTripleStore(ontologyURI.toString());

        ProtegeOWLParser parser = new ProtegeOWLParser(owlModel, true);
        parser.setImporting(true);

        // if(prefixForDefaultNamespace != null) {
        // parser.setPrefixForDefaultNamespace(prefixForDefaultNamespace);
        // }

        // Get a reader for the model to be imported
        StringReader modelStringReader = new StringReader(modelString);
        ARPInvokation arpInvokation = createARPInvokation(modelStringReader, 
                ontologyURI.toString());
        parser.loadTriples(ts, ontologyURI.toString(), arpInvokation);

        owlModel.getTripleStoreModel().setActiveTripleStore(activeTripleStore);
        owlModel.getOWLFrameStore().copyFacetValuesIntoNamedClses();

        defaultOWLOntology.addImports(ontologyURI);
    }

    private ARPInvokation createARPInvokation(final Reader reader, final String uri) {
        ARPInvokation invokation = new ARPInvokation() {
            public void invokeARP(ARP arp) throws Exception {

                setErrorLevel(arp);
                arp.load(reader, uri);
                reader.close();
            }
        };
        return invokation;
    }

    private void setErrorLevel(ARP arp) {
        String errorLevel = ApplicationProperties.getApplicationOrSystemProperty(
                "jena.parser.error_level", "lax");

        if (errorLevel.equalsIgnoreCase("default")) {
            arp.getOptions().setDefaultErrorMode();
        } else if (errorLevel.equalsIgnoreCase("lax")) {
            arp.getOptions().setLaxErrorMode();
        } else if (errorLevel.equalsIgnoreCase("strict")) {
            arp.getOptions().setStrictErrorMode();
        } else {
            arp.getOptions().setLaxErrorMode();
        }
    }

    static boolean isPrimitiveType(String parameterTypeURI) {
        boolean primitive = parameterTypeURI.toString().equals(
                XSD.xstring.getURI().toString())
                || parameterTypeURI.equals(XSD.xint.getURI().toString())
                || parameterTypeURI.equals(XSD.xshort.getURI().toString())
                || parameterTypeURI.equals(XSD.xlong.getURI().toString())
                || parameterTypeURI.equals(XSD.xfloat.getURI().toString())
                || parameterTypeURI.equals(XSD.xdouble.getURI().toString())
                || parameterTypeURI.equals(XSD.xbyte.getURI().toString())
                || parameterTypeURI.equals(XSD.xboolean.getURI().toString())
                || parameterTypeURI.equals(XSD.nonNegativeInteger.getURI()
                        .toString())
                || parameterTypeURI.equals(XSD.nonPositiveInteger.getURI()
                        .toString());
        return primitive;
    }

    /**
     * Check if the given namespace is related to Protege.
     * 
     * @param namespace the namespace
     * 
     * @return <code>true</code> if the given namespace is Protege-related
     */
    private static boolean isProtegeRelatedNamespace(String namespace) {
        
        if (namespace.endsWith("#"))
        {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        
        return "http://www.w3.org/2003/11/swrl".equals(namespace)
                || "http://swrl.stanford.edu/ontologies/built-ins/3.3/temporal.owl"
                    .equals(namespace)
                || "http://swrl.stanford.edu/ontologies/3.3/swrla.owl".equals(namespace)
                || "http://swrl.stanford.edu/ontologies/built-ins/3.3/swrlx.owl".equals(namespace)
                || "http://swrl.stanford.edu/ontologies/built-ins/3.4/swrlm.owl".equals(namespace)
                || "http://www.w3.org/2003/11/swrlb".equals(namespace)
                || "http://swrl.stanford.edu/ontologies/built-ins/3.3/abox.owl".equals(namespace)
                || "http://protege.stanford.edu/plugins/owl/protege".equals(namespace)
                || "http://sqwrl.stanford.edu/ontologies/built-ins/3.4/sqwrl.owl".equals(namespace)
                || "http://swrl.stanford.edu/ontologies/built-ins/3.3/tbox.owl".equals(namespace)
                || "http://swrl.stanford.edu/ontologies/built-ins/3.4/swrlxml.owl"
                .equals(namespace);
    }


    /**
     * Check if the given namespace is an RDF-, OWL-, or XSD-related namespace.
     * 
     * @param namespace the namespace
     * 
     * @return <code>true</code> if the given namespace is Protege-related
     */
    private static boolean isRdfOwlOrXsdRelatedNamespace(String namespace) {
        
        if (namespace.endsWith("#"))
        {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        
        return "http://www.daml.org/2001/03/daml+oil#".equals(namespace)
              || "http://www.w3.org/2000/01/rdf-schema#".equals(namespace)
              || "http://www.w3.org/2001/XMLSchema#".equals(namespace)
              || "http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(namespace)
              || "http://www.w3.org/2002/07/owl#".equals(namespace);
    }
    
    /**
     * Recursively iterates over all properties of the given individual and adds all individuals
     * to the collection of individuals that are in a object-relation to the given individual.
     *  
     * @param currentRootIndividual
     * @param individuals collection of individuals
     */
    public static void getTransitiveObjectPropertyValues(RDFResource currentRootIndividual, 
            Collection individuals)
    {
        // add the currently processed individual to the list
        individuals.add(currentRootIndividual);
        logger.debug("Recursively processing individual: " 
                + currentRootIndividual.getBrowserText());
        
        // recursively iterate over all properties, find object property values  
        // (DefaultOWLIndividual) and add them to the list of individuals
        for (Iterator propertyIt = currentRootIndividual.getRDFProperties().iterator(); 
        propertyIt.hasNext();) {
            RDFProperty property = (RDFProperty) propertyIt.next();
            logger.debug("  found property: " + property.getBrowserText());
        
            if (property instanceof OWLObjectProperty)
            {
                for (Iterator propertyValueIt = 
                        currentRootIndividual.listPropertyValues(property); 
                        propertyValueIt.hasNext();) 
                {
                    Object propertyValue = propertyValueIt.next();
    
                    if (propertyValue instanceof DefaultOWLIndividual && 
                            !individuals.contains(propertyValue))
                    {
                        getTransitiveObjectPropertyValues((RDFResource) propertyValue, 
                                individuals);
                    }
                }
            }
        }                
    }
    

    /**
     * Remove the T-Box and and all parts of the A-Box that are Protege-internal
     * from the given model.
     * 
     * @param originalJenaModel the model to clean up
     * @param sourceIndividuals the individuals that should be considered 
     *          when the minimal model is generated
     * @return the cleaned up model
     * 
     * @throws Exception
     */
    public static JenaOWLModel removeTBox(JenaOWLModel originalJenaModel, 
            RDFResource rootIndividual) throws Exception {
        Vector individualsToCopy = new Vector();
        Vector registeredNamespaces = new Vector();
        
        getTransitiveObjectPropertyValues(rootIndividual, individualsToCopy);
        
        for (Iterator sourceIndividualsIt = individualsToCopy.iterator(); 
        sourceIndividualsIt.hasNext();) {
            RDFResource individual = (RDFResource) sourceIndividualsIt.next();
            
            String namespace = individual.getNamespace();
            if (namespace == null)
            {
                // if the individual has no namespace, use the namespace of its type
                namespace = individual.getRDFType().getNamespace();
            }
                
            // register the namespaces of all types of individuals for imports
            for (Iterator types = individual.getDirectTypes().iterator(); types.hasNext();) {
                RDFResource type = (RDFResource) types.next();                    
                if (!isProtegeRelatedNamespace(namespace) && 
                        !isRdfOwlOrXsdRelatedNamespace(namespace))
                {
                    registeredNamespaces.add(type.getNamespace());
                }
            }
            for (Iterator types = individual.getInferredTypes().iterator(); types.hasNext();) {
                RDFResource type = (RDFResource) types.next();
                if (!isProtegeRelatedNamespace(namespace) && 
                        !isRdfOwlOrXsdRelatedNamespace(namespace))
                {
                    registeredNamespaces.add(type.getNamespace());
                }
            }                
            for (Iterator types = individual.getRDFProperties().iterator(); types.hasNext();) {
                RDFResource property = (RDFResource) types.next();
                if (!isProtegeRelatedNamespace(namespace) && 
                        !isRdfOwlOrXsdRelatedNamespace(namespace))
                {
                    registeredNamespaces.add(property.getNamespace());
                }
            } 
            if (logger.isDebugEnabled())
            {
                for (Iterator iterator = registeredNamespaces.iterator(); iterator.hasNext();) {
                    String registeredNamespace = (String) iterator.next();
                    logger.debug("Registered namespace: " + registeredNamespace);
                }
            }
        }  
        
        // create new empty model
        JenaOWLModel resultingJenaModel = ProtegeOWL.createJenaOWLModel();
        
        // copy namespace/prefix-mappings to new model
        NamespaceManager nsmFrom = originalJenaModel.getNamespaceManager();
        NamespaceManager nsmTo = resultingJenaModel.getNamespaceManager();
        for (Iterator iterator = nsmFrom.getPrefixes().iterator(); iterator.hasNext();) {
            String prefix = (String) iterator.next();
            String namespace = nsmFrom.getNamespaceForPrefix(prefix);
            if (!isProtegeRelatedNamespace(namespace))
            {
                logger.debug("Copy prefix/namespace mapping for: " + prefix + " --> " + namespace);
                nsmTo.setPrefix(namespace, prefix);
            }
        }
        // set the same default namespace for the new model 
        // (since it has to represent the original model)
        String defaultNamespace = nsmFrom.getDefaultNamespace();
        logger.debug("Default Namespace: " + defaultNamespace);
        nsmTo.setDefaultNamespace(defaultNamespace);
        resultingJenaModel.getTripleStoreModel().getTopTripleStore()
            .setDefaultNamespace(defaultNamespace);
        logger.debug(originalJenaModel.getDefaultOWLOntology().getBrowserText());
        
        // copy imports to new model
        for (Iterator iterator = originalJenaModel.getAllImports().iterator(); 
                iterator.hasNext();) {
            String importURI = (String) iterator.next();
            
            if (registeredNamespaces.contains(importURI) 
                    || registeredNamespaces.contains(importURI + "#"))
            {
                logger.debug("Copy import for: " + importURI);
                resultingJenaModel.getDefaultOWLOntology().addImports(importURI);
            }
        }

        // write intermediate model without individuals 
        // to avoid the mysterious "copy slot assertion" error
        Collection errors = new Vector(); 
        File tempFile = File.createTempFile("removeTBox_intermediate_ontology_", ".owl");
        JenaOWLModel.save(tempFile, resultingJenaModel.getOntModel(), FileUtils.langXMLAbbrev, 
                originalJenaModel.getDefaultOWLOntology().getNamespace());
        if (!errors.isEmpty())
        {
            for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                Object error = iterator.next();
                logger.debug("Error: " + error);
            }
        }
        
        // load the intermediate model
        resultingJenaModel = 
            ProtegeOWL.createJenaOWLModelFromURI(tempFile.toURL().toString());        

        // copy individuals to intermediate model
        for (Iterator iterator = individualsToCopy.iterator(); iterator.hasNext();) {
            RDFResource individual = (RDFResource) iterator.next();
            
            String namespace = individual.getNamespace();
        
            logger.debug("copy individual: " + individual.getBrowserText() 
                    + " with namespace: " + individual.getNamespace() 
                    + " and namespacePrefix: " + individual.getNamespacePrefix() 
                    + " and Class: " + individual.getClass().getName());
            //individual.shallowCopy(resultingJenaModel, null);

            // create new instance with same name and direct types
            Collection allTypes = individual.getDirectTypes();
            //String name = "@_:" + individual.getLocalName();
            String name = individual.getName();
            edu.stanford.smi.protegex.owl.model.OWLIndividual newIndividual = 
                resultingJenaModel.getOWLThingClass().createOWLIndividual(name);
            
            // add the asserted types
            for (Iterator inferredTypesIt = individual.getInferredTypes().iterator(); 
                    inferredTypesIt.hasNext();) {
                Cls type = (Cls) inferredTypesIt.next();
                newIndividual.addDirectType(type);
            }
            
            // also add the inferred types as asserted types now
            for (Iterator inferredTypesIt = individual.getInferredTypes().iterator(); 
                    inferredTypesIt.hasNext();) {
                Cls type = (Cls) inferredTypesIt.next();
                newIndividual.addDirectType(type);
            }              
        
        }  
        // copy properties of individuals to intermediate model
        for (Iterator iterator = individualsToCopy.iterator(); iterator.hasNext();) {
            RDFResource individual = (RDFResource) iterator.next();
            
            String namespace = individual.getNamespace();      
            logger.debug("copy properties of individual: " + individual.getBrowserText() 
                    + " with namespace: " + individual.getNamespace() 
                    + " and namespacePrefix: " + individual.getNamespacePrefix() 
                    + " and Class: " + individual.getClass().getName());

            //String name = "@_:" + individual.getLocalName();
            String name = individual.getName();
            edu.stanford.smi.protegex.owl.model.OWLIndividual newIndividual = 
                resultingJenaModel.getOWLIndividual(name);

            // create all properties
            for (Iterator propertyIt = individual.getRDFProperties().iterator(); 
                    propertyIt.hasNext();) 
            {
                RDFProperty property = (RDFProperty) propertyIt.next();
                logger.debug("  property: " + property.getBrowserText());
                
                Collection propertyValues = new Vector();
                for (Iterator iterator2 = individual.listPropertyValues(property); 
                        iterator2.hasNext();) 
                {
                    Object propertyValue = iterator2.next();
                    
//                    if (propertyValue instanceof DefaultOWLIndividual)
//                    {
//                        String prefix = ((DefaultOWLIndividual)propertyValue).getNamespacePrefix();
//                        if (prefix == null)
//                        {
//                            propertyValue = resultingJenaModel.getOWLIndividual(
//                                    "@_:" + ((DefaultOWLIndividual)propertyValue).getLocalName());
//                        }
//                    }
                    
                    logger.debug("  property value: " + propertyValue);
                   
                    if (!"protege".equals(property.getNamespacePrefix()))
                    {
                        logger.debug("  - copy property " + property.getName());
                        propertyValues.add(propertyValue);
                    }
                }
                newIndividual.setPropertyValues(property, propertyValues);
            }                
        }  

        // write resulting model
        Vector errors2 = new Vector(); 
        File tempFile2 = File.createTempFile("removeTBox_result_", ".owl");
        JenaOWLModel.save(tempFile2, resultingJenaModel.getOntModel(), FileUtils.langXMLAbbrev, 
                originalJenaModel.getDefaultOWLOntology().getNamespace());
        if (!errors2.isEmpty())
        {
            for (Iterator iterator = errors2.iterator(); iterator.hasNext();) {
                Object error = iterator.next();
                logger.debug("Error: " + error);
            }
        }

        return resultingJenaModel;
    }

    /**
     * Write the inferred model without imported TBox statements into a String.
     * 
     * @param dataflowModel
     * @param sourceIndividuals the individuals that should be considered 
     *          when the minimal model is generated
     * @return the serialized inferred model
     * 
     * @throws IOException
     */
    public static String writeMinimalInferredModel(JenaOWLModel dataflowModel, 
            RDFResource sourceIndividual) throws Exception 
    {
        String output;
        // write the resulting ontology model
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
       
// Method 1)
//        OntModel resultingModel = SemanticUtils.prepareSerialisation(dataflowModel, null);
//        final String ns = dataflowModel.getNamespaceManager().getDefaultNamespace();
//        resultingModel.write(bout, FileUtils.langXML, ns);

// Method 2   
//        dataflowModel = removeTBox(dataflowModel);
//        dataflowModel.getOntModel().writeAll(bout, FileUtils.langXML, 
//                dataflowModel.getDefaultOWLOntology().getNamespace());
//        output = bout.toString();
//        bout.close();

// Method 3       
        dataflowModel = removeTBox(dataflowModel, sourceIndividual);

        Vector errors2 = new Vector(); 
        String ontologyName = dataflowModel.getDefaultOWLOntology().getNamespace();
        String fileName = ontologyName.substring(ontologyName.lastIndexOf("/")+1);
        File tempFile2 = File.createTempFile(fileName, "_minimal.owl");
        JenaOWLModel.save(tempFile2, dataflowModel.getOntModel(), FileUtils.langXML, 
                dataflowModel.getDefaultOWLOntology().getNamespace());
        if (!errors2.isEmpty())
        {
            for (Iterator iterator = errors2.iterator(); iterator.hasNext();) {
                Object error = iterator.next();
                logger.debug("Error: " + error);
            }
        }

        FileInputStream fis = new FileInputStream(tempFile2); 
        BufferedInputStream bis = new BufferedInputStream(fis); 
        DataInputStream dis = new DataInputStream(bis); 

        String line;
        StringBuffer fileContent = new StringBuffer();
        while ( (line=dis.readLine()) != null ) { 
            fileContent.append(line);
        } 
        
        dis.close();
        bis.close();
        fis.close();
        
        output = fileContent.toString();   
        
        // clean output: remove xml-version info, since it causes problems later
        output = output.substring("<?xml version=\"1.0\"?>".length());
        
        logger.debug("WRITE RESULTING MODEL: " + output);
        
        return output;
    }
}
