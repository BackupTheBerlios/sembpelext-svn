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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.mindswap.owl.OWLClass;
import org.mindswap.owl.OWLFactory;
import org.mindswap.owl.OWLIndividual;
import org.mindswap.owl.OWLIndividualList;
import org.mindswap.owl.OWLKnowledgeBase;
import org.mindswap.owl.OWLObjectProperty;
import org.mindswap.owl.OWLValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Class representing a custom XPath function, used by the expression evaluator
 * to handle the semantic outputs stored in a ontology container model. 
 * The defined process output message variable can consist of several parts. 
 * For each part an OWL ontology fragment containing OWL individuals is returned. 
 * Since a custom function can only return one parameter the parts are
 * encoded as elements which are returned as one output node. 
 * This is performed by this function.
 */
public class HandleSemanticProcessOutputsFunction implements Function {

    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(HandleSemanticProcessOutputsFunction.class);

    /**
     * Extract the process outputs from the the process output container variable 
     * and put them into one accordingly named node elements each, 
     * wrapped into one single root element.
     * 
     * @param aContext
     *            the function context (currently ignored)
     * @param aArgs a list of arguments;
     *            the first argument is the process output container (DOM-Node)
     *            the second argument is the name of the process
     *            all other arguments are process outputs;
     *            a process output consists of two parts:
     *            1. the name of the output part element (String)
     *            2. the URI of the data type of the output (String)
     *            
     *            So, a valid number of arguments must abide to
     *            the equation <code>1 + 2*x</code>
     *            with <code>x</code> the number of output parts.
     *            
     * 
     * @return the RDF/XML serialisation of the resulting model
     * 
     * @throws FunctionCallException
     */
    public Object call(Context aContext, List aArgs) throws FunctionCallException 
    {
        if (aArgs.size() < 3) {
            String errorMessage = "Custom XPath Error in handleSemanticProcessOutputs: "
                    + "requires at least 3 parameters, but provided where " + aArgs.size();
            logger.error(errorMessage);
            throw new FunctionCallException(errorMessage);
        } else if (!(aArgs.get(0) instanceof Node)) {
            logger.error(new Exception(
                    "Custom XPath Error in handleSemanticProcessOutputs: " +
                    "second argument not of type org.w3c.dom.Node, " +
                    "it's a " + aArgs.get(0).getClass().toString()));
                return null;
        } else if (!(aArgs.get(1) instanceof String)) {
            String errorMessage = "Custom XPath Error in handleSemanticProcessOutputs: "
                    + "first argument not of type java.lang.String, " + "it's a "
                    + aArgs.get(1).getClass().toString();
            logger.error(errorMessage);
            throw new FunctionCallException(errorMessage);
        }
        for (int i = 2; i < aArgs.size(); i = i + 1) {
            if (!(aArgs.get(i) instanceof String)) {
                String errorMessage = "Custom XPath Error in handleSemanticProcessOutputs: " + i
                        + ". argument not of type java.lang.String, " + "it's a "
                        + aArgs.get(i).getClass().toString();
                logger.error(errorMessage);
                throw new FunctionCallException(errorMessage);
            }
        }

        OntModel outputContainer = null;
        try {
            // read output container model
            Node outputContainerRoot = SemanticUtils.findRdfNode((Node) aArgs.get(0));
            String outputContainerString = SemanticUtils.elementToXMLString(outputContainerRoot);

            URI outputContainerUri = 
                SemanticUtils.extractOntologyNameUriFromRDF(outputContainerString, true);
            OWLKnowledgeBase outputContainerKb = OWLFactory.createKB();
            outputContainer = (OntModel) outputContainerKb
                .read(new StringReader(outputContainerString), outputContainerUri )
                    .getImplementation();
        } catch (Exception e) {
            Exception exception = new Exception(
                    "Custom XPath Error in handleSemanticProcessOutputs: " +
                    "Invalid RDF/XML serialization of outputContainer. " +
                    "Maybe namespace declaration for 'xml:base' or 'xmlns:rdf' is missing.");
            logger.error(exception);
            throw new FunctionCallException(exception);
        }

        Element wrapperElement = null;        
        try {
            String processName = (String) aArgs.get(1);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // create a wrapper element for the part-elements
            String wrappingElementName = "<" + processName + "ReplyOutput/>";
            Document wrapperDoc = builder.parse(new ByteArrayInputStream(
                                                        wrappingElementName.getBytes()));
            wrapperElement = wrapperDoc.getDocumentElement();

            // extract the process outputs from the output container model
            HashMap outputs = extractProcessOutputs(outputContainer, processName);
            
            // for each part of the process output message variable 
            // get the corresponding value from the extracted process output container model
            // and create a DOM element with the according name
            // and add it to the output root element.
            for (int i = 2; i < aArgs.size(); i = i + 1) {
                String partName = (String) aArgs.get(i);
                OWLValue partValue = (OWLValue) outputs.get(partName);
                String partValueString = ((OWLIndividual)partValue).toRDF();

                // create the partValue document 
                Document partValueDoc = builder.parse(new ByteArrayInputStream(
                                                            partValueString.getBytes()));

                // import the partValue-Document into the wrapper-Document
                Node importedPartValueNode = wrapperDoc.importNode(
                                                partValueDoc.getDocumentElement(), true);

                // create an element with the partName
                Element partNameElement = wrapperDoc.createElement(partName);
                
                // add the partValue element to the partName element
                partNameElement.appendChild(importedPartValueNode);
                
                // add the part element to the wrapper element
                wrapperElement.appendChild(partNameElement);
            }
        } 
        catch (Exception ex) {
            throw new FunctionCallException("Could not handle semantic process outputs. ", ex);
        }

        // return the wrapper element with all partName-elements
        // containing the partValue-Elements
        return wrapperElement;
    }


    /**
     * Extracts all Outputs stored in a container and put them as DOM-Nodes into 
     * the corresponding parts of a BPEL output message variable.
     * The outputs in the container are stored in the form:
     * - process hasOutput aOutput 
     * - aOutput parameterObject aValue
     * 
     * @param Model the ontology model representing the container with all the process outputs
     * @param service the service for which the outputs should be extracted
     * @return Element with parts of the process output message variable encoded 
     * as accordingly named elements 
     * 
     * @throws Exception
     */
    private static HashMap extractProcessOutputs(OntModel outputContainerModel, String processName)
            throws Exception {
        long extractOutput_t1 = System.currentTimeMillis();
    
        // add statements to OWL-S KB
        OWLKnowledgeBase kb = OWLFactory.createKB();
        ((Model) kb.getImplementation()).add(outputContainerModel);
    
        // get process
        OWLClass Process = kb
                .createClass(new URI(
                        "http://localhost:8080/ontologies/ServiceAnchor.owl#Process"));
        OWLObjectProperty hasOutput = kb.createObjectProperty(new URI(
                "http://localhost:8080/ontologies/ServiceAnchor.owl#hasOutput"));
        OWLObjectProperty parameterValue = kb
                .createObjectProperty(new URI(
                        "http://localhost:8080/ontologies/ServiceAnchor.owl#parameterObject"));
    
        OWLIndividualList processList = kb.getInstances(Process);
        
        // catch process from service to invoke
        OWLIndividual process = null;
        for (int i = 0; i < processList.size(); i++) {
            process = (OWLIndividual) processList.get(i);
            String label = process.getLabel();
            
            // break if right process found
            if (label.equals(processName + "Process"))
            {
                break;
            }
        }
    
        // get outputs and store in HashMap
        HashMap outputs = new HashMap();
        OWLIndividualList containerOutputList = process.getProperties(hasOutput);
    
        // traverse all outputs in container
        for (int j = 0; j < containerOutputList.size(); j++) {
            OWLIndividual containerOutput = (OWLIndividual) containerOutputList.get(j);
            String outputLabel = containerOutput.getLabel();

            OWLValue outputValue = containerOutput.getProperty(parameterValue);
            
            outputs.put(outputLabel, outputValue);
        }
    
        long extractOutput_t2 = System.currentTimeMillis();
        logger.info("extractOutputTimeConsumption: "
                + (extractOutput_t2 - extractOutput_t1));
    
        return outputs;
    }

}
