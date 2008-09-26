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

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import org.mindswap.owls.OWLSFactory;
import org.mindswap.owls.process.Input;
import org.mindswap.owls.process.InputList;
import org.mindswap.owls.process.Output;
import org.mindswap.owls.process.OutputList;
import org.mindswap.owls.process.Process;
import org.mindswap.owls.process.execution.ProcessExecutionEngine;
import org.mindswap.owls.service.Service;
import org.mindswap.query.ValueMap;
import org.w3c.dom.Node;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;

/**
 * Class representing a custom XPath function, used by the expression evaluator
 * to execute a semantic web service.
 */
public class SemanticWebServiceFunction implements Function {

    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(SemanticWebServiceFunction.class);

    /**
     * Execution a semantic web service call represented by a custom XPath function.
     * 
     * @param aContext the function context (currently ignored)
     * @param aArgs the two function arguments:
     *         1. service model URI (String), 
     *         2. input container model (RDF/XML serialization String), 
     *         3. output container model (RDF/XML serialization String)
     *         
     * @return the RDF/XML serialisation of the resulting model  
     * 
     * @throws FunctionCallException 
     */
    public Object call(Context aContext, List aArgs) throws FunctionCallException {

        if (aArgs.size() != 3) {
            logger.error(new Exception(
                "Custom XPath Error in executeSemanticWebService: requires 3 parameters, " +
                "but provided where " + aArgs.size()));
            return null;
        } else if (!(aArgs.get(0) instanceof String)) {
            logger.error(new Exception(
                "Custom XPath Error in executeSemanticWebService: " +
                "first argument not of type java.lang.String, " +
                "it's a " + aArgs.get(0).getClass().toString()));
            return null;
        } else if (!(aArgs.get(1) instanceof Node)) {
            logger.error(new Exception(
                "Custom XPath Error in executeSemanticWebService: " +
                "second argument not of type org.w3c.dom.Node, " +
                "it's a " + aArgs.get(1).getClass().toString()));
            return null;
        } else if (!(aArgs.get(2) instanceof Node)) {
            logger.error(new Exception(
                "Custom XPath Error in executeSemanticWebService: " +
                "third argument not of type org.w3c.dom.Node, " +
                "it's a " + aArgs.get(2).getClass().toString()));
            return null;
        }

        OntModel inputContainer = null;
        try {
            // read input container model
            Node inputContainerRoot = SemanticUtils.findRdfNode((Node) aArgs.get(1));
            String inputContainerString = SemanticUtils.elementToXMLString(inputContainerRoot);

            URI inputContainerUri = SemanticUtils
                .extractOntologyNameUriFromRDF(inputContainerString, true);
            OWLKnowledgeBase inputContainerKb = OWLFactory.createKB();
            //inputContainerKb.setAutoConsistency(true);
            inputContainer = (OntModel) inputContainerKb
                .read(new StringReader(inputContainerString), inputContainerUri )
                    .getImplementation();
        } catch (Exception e) {
            Exception exception = new Exception(
                    "Custom XPath Error in executeSemanticWebService: " +
                    "Invalid RDF/XML serialization of inputContainer. " +
                    "Maybe namespace declaration for 'xml:base' or 'xmlns:rdf' is missing.");
            logger.error(exception);
            throw new FunctionCallException(exception);
        }
        
        URI outputContainerUri = null;
        OntModel outputContainer = null;
        OWLKnowledgeBase outputContainerKb = OWLFactory.createKB();
        try {
            // read output container model
            Node outputContainerRoot = SemanticUtils.findRdfNode((Node) aArgs.get(2));
            String outputContainerString = SemanticUtils.elementToXMLString(outputContainerRoot);

            outputContainerUri = SemanticUtils
                .extractOntologyNameUriFromRDF(outputContainerString, true);
            //outputContainerKb.setAutoConsistency(true);
            outputContainer = (OntModel) outputContainerKb
                .read(new StringReader(outputContainerString), outputContainerUri )
                    .getImplementation();
        } catch (Exception e) {
            Exception exception = new Exception(
                    "Custom XPath Error in executeSemanticWebService: " +
                    "Invalid RDF/XML serialization of outputContainer. " +
                    "Maybe namespace declaration for 'xml:base' or 'xmlns:rdf' is missing.");
            logger.error(exception);
            throw new FunctionCallException(exception);
        }

        String output = null;
        // call the semantic web service
        try {
            long t1 = System.currentTimeMillis();
            
            // read the service profile
            Service service = OWLFactory.createKB().readService((String) aArgs.get(0));

            // extract input
            HashMap inputs = extractProcessInputs(inputContainer, service);

            // execute service
            HashMap outputs2 = executeSemanticWebService(service, inputs);

            // store outputs by filling outputContainer
            // extract input
            storeOutput(outputs2, outputContainer, service);

            long t2 = System.currentTimeMillis();
            logger.info("Semantic Web Service Time Consumption: " + (t2 - t1));
        
            // write the resulting container
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            outputContainer.write(bout, FileUtils.langXML, outputContainerUri.toString());
            logger.debug("Semantic Web Service Result: " + bout);
            
            output = bout.toString();            
            bout.close();
        } catch (Exception ex) {
            Exception exception = new Exception(
                    "Custom XPath Error in executeSemanticWebService. ", ex);
            logger.error(exception);
            throw new FunctionCallException(exception);        
        }

        // write the resulting document
        return SemanticUtils.xmlStringToElement(output, "out0");
    }

    /**
     * Executes semantic web service by using the owl-s API
     * (Assumption: there is only one service operation defined
     * in the service description).
     * 
     * @param service the service to execute
     * @param inputs the inputs for the service
     * @return the service outputs
     * @throws Exception
     */
    private static HashMap executeSemanticWebService(Service service, HashMap inputs)
            throws Exception {
        // long t1 = System.currentTimeMillis();
    
        // get process
        Process process = service.getProcess();
    
        // set inputs
        ValueMap values = new ValueMap();
        Iterator iter = inputs.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            String inputName = ((String) iter.next());
            OWLValue inputValue = (OWLValue) inputs.get(inputName);
            logger.debug("extracting input " + inputName + " "
                    + (inputValue != null));
    
            // catch input with matching label
            InputList owlsInputList = service.getProcess().getInputs();
    
            // traverse all inputs in owl-s service description
            // and set the matching one
            for (int j = 0; j < owlsInputList.size(); j++) {
                Input owlsInput = (Input) owlsInputList.get(j);
                String owlsInputName = owlsInput.getLabel();
                if (owlsInputName.equals(inputName)) {
                    values.setValue(owlsInput, inputValue);
    
                    break;
                }
            }
    
        }
        
        // execute service
        ProcessExecutionEngine exec = OWLSFactory.createExecutionEngine();
        // Logger.info("exec");
        exec.setPreconditionCheck(false);
        values = exec.execute(process, values);
        // Logger.info("exec");
    
        // store outputs in HashMap
        HashMap outputs = new HashMap();
        OutputList outputlist = process.getOutputs();
        Iterator outputIter = outputlist.iterator();
        for (int i = 0; outputIter.hasNext(); i++) {
            Output output = ((Output) outputIter.next());
            if (SemanticUtils.isPrimitiveType(output.getParamType().getURI().toString())) {
                outputs.put(output.getLabel(), values.getDataValue(output));
            } else {
                outputs.put(output.getLabel(), values
                        .getIndividualValue(output));
            }
        }
    
        // long t2 = System.currentTimeMillis();
        // 
        // Logger.info("ExecutionTimeConsumption: " + (t2 - t1));
        return outputs;
    }

    /**
     * Extracts all inputs stored in a container into a HashMap<InputName,OWLIndividual>
     * The inputs in the container are stored in the form:
     * - process hasInput aInput 
     * - aInput parameterValue aValue
     * 
     * @param Model the ontology model representing the container with all the process inputs
     * @param service the service for which the inputs should be extracted
     * @return Hashmap of process inputs for the given service (= atomic process)
     * @throws Exception
     */
    private static HashMap extractProcessInputs(OntModel inputContainerModel, Service service)
            throws Exception {
        long extractInputSWS1t1 = System.currentTimeMillis();
    
        // add statements to OWL-S KB
        OWLKnowledgeBase kb = OWLFactory.createKB();
        ((Model) kb.getImplementation()).add(inputContainerModel);
    
        // get process
        OWLClass Process = kb
                .createClass(new URI(
                        "http://localhost:8080/ontologies/ServiceAnchor.owl#Process"));
        OWLObjectProperty hasInput = kb.createObjectProperty(new URI(
                "http://localhost:8080/ontologies/ServiceAnchor.owl#hasInput"));
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
            if (label.equals(service.getProcess().getLabel())
                    || label.equals(service.getName() + "Process"))
                break;
        }
    
        // get inputs and store in HashMap
        HashMap inputs = new HashMap();
        InputList owlsInputList = service.getProcess().getInputs();
        OWLIndividualList containerInputList = process.getProperties(hasInput);
    
        // traverse all inputs in owl-s service description
        for (int i = 0; i < owlsInputList.size(); i++) {
            Input owlsInput = (Input) owlsInputList.get(i);
            String owlsInputName = owlsInput.getLabel();
    
            // traverse all inputs in container and match labels
            OWLIndividual containerInput = null;
            boolean match = false;
            for (int j = 0; j < containerInputList.size(); j++) {
                containerInput = (OWLIndividual) containerInputList.get(j);
                String inputLabel = containerInput.getLabel();
    
                if (inputLabel.equals(owlsInputName)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                OWLValue inputValue = containerInput.getProperty(parameterValue);
                
                inputs.put(owlsInputName, inputValue);
            }
        }
    
        long extractInputSWS1t2 = System.currentTimeMillis();
        logger.info("extractInputTimeConsumption: "
                + (extractInputSWS1t2 - extractInputSWS1t1));
    
        return inputs;
    }

    /**
     * Stores each output into a container the following way: 
     * - process hasOutput aOutput
     * - aOutput parameterValue aValue.
     * 
     * @param outputs the outputs to store
     * @param outputModel the container, where the outputs are stored
     * @param service the service that returned the outputs
     * @throws Exception
     */
    private static void storeOutput(HashMap outputs, OntModel outputModel, Service service)
            throws Exception {
    
        // add statements to OWL-S KB for data OWL-S API handling
        OWLKnowledgeBase kb = OWLFactory.createKB();
        ((Model) kb.getImplementation()).add(outputModel);
    
        // get process
        OWLClass Process = kb
                .createClass(new URI(
                        "http://localhost:8080/ontologies/ServiceAnchor.owl#Process"));
        OWLObjectProperty hasOutput = kb
                .createObjectProperty(new URI(
                        "http://localhost:8080/ontologies/ServiceAnchor.owl#hasOutput"));
        OWLObjectProperty parameterValue = kb
                .createObjectProperty(new URI(
                        "http://localhost:8080/ontologies/ServiceAnchor.owl#parameterObject"));
        OWLIndividualList processList = kb.getInstances(Process);
        OWLIndividual process = null;
        for (int i = 0; i < processList.size(); i++) {
            process = (OWLIndividual) processList.get(i);
            String label = process.getLabel();
            
            // Logger.info("process "+label);
            if (label.equals(service.getProcess().getLabel()))
                break;
        }
        // traverse each empty Output and set parameterValue
        OWLIndividualList containerOutputList = process
                .getProperties(hasOutput);
        for (int j = 0; j < containerOutputList.size(); j++) {
            OWLIndividual containerOutput = (OWLIndividual) containerOutputList
                    .get(j);
            String outputLabel = containerOutput.getLabel();
            OWLValue value = (OWLValue) outputs.get(outputLabel);    
            OWLIndividual outputParameterValue = (OWLIndividual) value;
            
            // workaround adding identifier to retrieve afterwards from kb
//            String namespace = process.getOntology().getFileURI().toString();
//            namespace = namespace.substring(0, namespace.indexOf("#"));
//            OWLClass ID = kb.createClass(new URI(namespace + "#xyz"));
            OWLClass ID = kb.createClass(
                    new URI("http://localhost:8080/ontologies/ServiceAnchor.owl#xyz"));
            
            // some types get lost, maybe does't matter, replaced
            // afterwards by classifiying through OWL DL reasoning
            Iterator types = outputParameterValue.getTypes().iterator();
            outputParameterValue.addType(ID);
            
            // add ParameterValue statements to KB
            String rdfString = outputParameterValue.toRDF();
            ((OntModel) kb.getImplementation()).read(new StringReader(
                    rdfString), null);
            
            // get ParameterValue by identifier
            OWLIndividual outputParameterValueInKB = (OWLIndividual) kb
                    .getInstances(ID).iterator().next();
            outputParameterValueInKB.removeTypes();
            
            while (types.hasNext())
            {
                OWLClass type = (OWLClass)types.next();
                outputParameterValueInKB.addType(type);
            }
            
            value = outputParameterValueInKB;
            
            // set parameterValue as property
            kb.addProperty(containerOutput, parameterValue, value);
        }
        
        // copy back to container
        outputModel.add((OntModel) kb.getImplementation());

        if (logger.isDebugEnabled())
        {
            logger.debug("############## Print SWS result as RDF/XML-ABBREV ##############");
            outputModel.writeAll(System.out,"RDF/XML-ABBREV", null);
        }
    }
    
}
