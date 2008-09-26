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


import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.w3c.dom.Node;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.inference.reasoner.exception.ProtegeReasonerException;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.util.ImportHelper;
import edu.stanford.smi.protegex.owl.swrl.bridge.exceptions.SWRLRuleEngineBridgeException;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLFactory;

/**
 * Class representing a custom XPath function, used by the expression evaluator
 * to transform an OWL individual of a certain OWL-class-definition to conform
 * to another OWL-class-definition.
 */
public class SemanticBridgeFunction implements Function {
    
    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(SemanticBridgeFunction.class);

    /**
     * Execution a semantic bridge call represented by a custom XPath function.
     * 
     * @param aContext the function context (currently ignored)
     * @param aArgs the two function arguments:
     *          1. bridge URI (String)
     *          2. input model (RDF/XML serialization String)
     *         
     * @return the RDF/XML serialisation of the resulting model  
     * 
     * @throws FunctionCallException
     */
    public Object call(Context aContext, List aArgs) throws FunctionCallException 
    {
        long t1 = System.currentTimeMillis();
        
        // check all parameters
        Object arg0 = aArgs.get(0); // bridge URI (String)
        Object arg1 = aArgs.get(1); // input model (RDF/XML serialization String)
        
        if (aArgs.size() != 2) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: requires 2 parameters, " +
                "but provided where " + aArgs.size());
            logger.error(exception);
            throw new FunctionCallException(exception);
        } else if (!(arg0 instanceof String)) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: " +
                "first argument not of type java.lang.String, " +
                "it's a " + arg0.getClass().toString());
            logger.error(exception);
            throw new FunctionCallException(exception);
        } else if (!(arg1 instanceof Node)) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: " +
                "second argument not of type org.w3c.dom.Node, " +
                "it's a " + arg1.getClass().toString());
            logger.error(exception);
            throw new FunctionCallException(exception);
        }

        String           bridgeURI          = null;        
        JenaOWLModel     bridgingModel      = null;
        String           output             = null;

        // execute the semantic bridge
        try {

            // parse the incoming ABox model
            Node rdfRootNode = SemanticUtils.findRdfNode((Node) arg1);
            String owlIndividualString = SemanticUtils.elementToXMLString(rdfRootNode);
            
            // repair the xml:base attribute (which is necessary 
            // for parsing the model into protege)
            owlIndividualString = SemanticUtils.repairBaseUriInRDFSerialisation(
                    owlIndividualString);

            // load the parsed model into bridging model
            bridgingModel = ProtegeOWL.createJenaOWLModelFromReader(
                    new StringReader(owlIndividualString));            
            
            // get the root individual in the model (the Process individual)
            // (this is used to write the minimal inferred model after performing reasoning)
            RDFResource rootIndividual = null;
            String processClassName = bridgingModel.getResourceNameForURI(
                    "http://localhost:8080/ontologies/ServiceAnchor.owl#Process");
            RDFResource process = bridgingModel.getRDFResource(processClassName);
            Collection individuals = bridgingModel.getUserDefinedRDFIndividuals(false);
            for (Iterator iterator = individuals.iterator(); iterator.hasNext();) {
                rootIndividual = (RDFResource) iterator.next();
                
                if (rootIndividual.getDirectTypes().contains(process))
                {
                    break;
                }
            }
            
            // make the given URL for the bridge ontology absolute if necessary
            bridgeURI = SemanticBpelFunctionContext.convertProjectRelativeUriToAbsoluteUri(
                    (String)arg0);
            
            // Import the bridge ontology containing the mapping rules 
            // from the specified URI            
            ImportHelper importHelper = new ImportHelper(bridgingModel);
            
            importHelper.addImport(new URI(bridgeURI));
            importHelper.importOntologies();

            // TODO this is a workaround for removing "bad" namespace declarations
            // caused by creating objects that do not have a namespace declaration
            bridgingModel = SemanticUtils.removeBadNamespaceDeclarations(bridgingModel);

            // perform all necessary steps for OWL and SWRL reasoning 
            // (which performs the actual dataflow)
            SemanticUtils.performOWLandSWRLreasoning(bridgingModel);

            // create SWRLFactory for manipulation of rule model
            SWRLFactory swrlFactory = new SWRLFactory(bridgingModel);

            // delete the rules, since we do not want to safe them into the
            // resulting ontology
//            swrlFactory.deleteImps();

            // remove all swrl related facts (like e.g. swrl-variables, imports)
//            SemanticUtils.removeRemainingSWRLFacts(bridgingModel);
        
            // write the resulting ontology model
            output = SemanticUtils.writeMinimalInferredModel(bridgingModel, rootIndividual);        
            logger.debug("Semantic Bridge Result: " + output);

            // make sure the memory is deallocated
            bridgingModel.dispose();
//            System.gc();
//            System.runFinalization();
//            System.gc();

            // debug output
            long t2 = System.currentTimeMillis();
            logger.info("Semantic Bridge Time Consumption: " + (t2 - t1));
        } catch (InvalidOWLModelSerialzationException ex) {
            ex.printStackTrace();

            FunctionCallException exception = new FunctionCallException(
                    "Custom XPath Error in executeSemanticBridge: "
                    + "Failed to import individuals ontology" 
                    + " into bridge OWL model. This could be due to an" 
                    + " invalid RDF/XML serialization of inputContainer " +
                    		"(e.g. missing namespace declaration for 'rdf:base' attribute)."
                    + " or due to a missing file,"
                    + " which could have been caused by a lack of write permissings " +
                    		"for the specified temp-folder.", ex);              
            logger.error(exception);
            throw exception;      
        } catch (SWRLRuleEngineBridgeException ex) {
            ex.printStackTrace();

            FunctionCallException exception = new FunctionCallException(
                    "Custom XPath Error in executeSemanticBridge: "
                    + " Failed to create and execute the SWRL bridge.", ex);
            logger.error(exception);
            throw exception;        
        } catch (ProtegeReasonerException ex) {
            ex.printStackTrace();

            FunctionCallException exception = new FunctionCallException(
                    "Custom XPath Error in executeSemanticBridge: "
                    + " Failed to perform DL-reasoning.", ex);
            logger.error(exception);
            throw exception;        
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
  
            FunctionCallException exception = new FunctionCallException(
                    "Custom XPath Error in executeSemanticBridge: "
                    + " Could not execute semantic bridge "
                    + bridgeURI, ex);
            logger.error(exception);
            throw exception;        
        } catch (Exception ex) {
            ex.printStackTrace();          
            
            FunctionCallException exception = new FunctionCallException(
                    "Custom XPath Error in executeSemanticBridge: "
                    + " Failed to import an ontology. ", ex);
            logger.error(exception, ex);
            throw exception;
        } 
        
        // return the resulting ontology model as a dom with one wrapper element "out0"
        return SemanticUtils.xmlStringToElement(output, "out0");
    }
}

