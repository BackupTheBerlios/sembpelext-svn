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
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.w3c.dom.Node;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.NamespaceManager;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLAtom;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLAtomList;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLFactory;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLImp;
import edu.stanford.smi.protegex.owl.swrl.model.SWRLIndividualPropertyAtom;

/**
 * Class representing a custom XPath function, used by the expression evaluator
 * to perform semantic dataflow from one or more to one target based on rules.
 */
public class SemanticDataflowFunction implements Function {

    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(SemanticDataflowFunction.class);

    /**
     * Execution of a semantic dataflow call represented by a custom XPath function.
     * depending of the third parameter (individual URI) there are two different dataflow-modes.
     * If the third parameter contains an URI then it is expected that a special dataflow
     * has to be performed. This dataflow transfers the specified individual from 
     * the (one and only) source model container into the target model container as 
     * specified in the dataflow rule mode.
     * It no URI is specified then a normal dataflow will be 
     * performed and one or more source model
     * containers can be specified for the dataflow.
     * 
     * @param aContext the function context (currently ignored)
     * @param aArgs the two function arguments:
     *          1. dataflow-rule-model (RDF/XML as DOM),
     *          2. target-model-container (RDF/XML as DOM), 
     *          3. source individual URI or <code>empty string</code> for normal dataflow, 
     *          x. zero or more source-model-containers (as RDF/XML DOM-Node) 
     *             that define the inputs for the dataflow
     *         
     * @return the RDF/XML serialisation of the resulting model  
     * 
     * @throws FunctionCallException 
     */
    public Object call(Context aContext, List aArgs) throws FunctionCallException {
        long t1 = System.currentTimeMillis();
        
        // check all parameters
        Object arg0 = aArgs.get(0); // dataflow-rule-model (RDF/XML as DOM)
        Object arg1 = aArgs.get(1); // target-model-container (RDF/XML as DOM)
        Object arg2 = aArgs.get(2); // source-individual-uri or an empty string for normal dataflow
        
        if (aArgs.size() < 3) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: requires 4 or more parameters, " +
                "but provided where " + aArgs.size());
            logger.error(exception);
            throw new FunctionCallException(exception);
        } 
        if (!((arg0 instanceof String) && "".equals(arg0)) && !(arg0 instanceof Node)) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: first argument an empty string " +
                "and not of type org.w3c.dom.Node, " +
                "it's a " + arg0.getClass().toString());
            logger.error(exception);
            throw new FunctionCallException(exception);
        } 
        if (!(arg1 instanceof Node)) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: second argument " +
                "not of type org.w3c.dom.Node, " +
                "it's a " + arg1.getClass().toString());
            logger.error(exception);
            throw new FunctionCallException(exception);
        }      
        if (arg2 != null && !(arg2 instanceof String)) {
            Exception exception = new Exception(
                "Custom XPath Error in executeSemanticBridge: third argument " +
                "not of type java.lang.String, " +
                "it's a " + arg2.getClass().toString());
            logger.error(exception);
            throw new FunctionCallException(exception);
        }
                
        Object output = null;
        
        List sourceModelContainers = new Vector();
        
        for (int i = 3; i < aArgs.size(); i++) 
        {
            Object sourceModelContainer = aArgs.get(i);

            if (!(sourceModelContainer instanceof Node)) {
                Exception exception = new Exception(
                    "Custom XPath Error in executeSemanticBridge: " + i + ". argument " +
                    		"not of type org.w3c.dom.Node, " +
                    "it's a " + aArgs.get(i).getClass().toString());
                logger.error(exception);
                throw new FunctionCallException(exception);
            }
            
            sourceModelContainers.add(sourceModelContainer);
        }
        
        output = executeDataflow((Node)arg0, (Node)arg1, (String)arg2, sourceModelContainers);
        
        long t2 = System.currentTimeMillis();
        logger.info("Dataflow Time Consumption: " + (t2 - t1));
        
        return SemanticUtils.xmlStringToElement(output, "out0");
    }

    /**
     * Replaces the variable "placeholder" in the rules rules 
     * of the given model with the specified individual-URI.
     * 
     * RESTRICTIONS: There can be more then one rule that contains the variable 
     * called "placeholder", however this variable can only occur exactly one times 
     * in the rule body and one times in the one and only atom of the rule head. 
     * It can be placed anywhere in the body but has to be the first
     * atom in the head (if it occurs in the head at all). 
     * 
     * @param individualURI
     * @param dataflowModel
     * @return
     * @throws URISyntaxException
     */
    private SWRLFactory replacePlaceholderInDataflowRule(String individualURI,
            JenaOWLModel dataflowModel) throws URISyntaxException {
        // workarround:
        // fix Protege prefix querying (prefixes are not resolved in protege) 
        // get actual prefix
        NamespaceManager namespaceManager = dataflowModel.getNamespaceManager();
        String prefix = namespaceManager.getPrefix((individualURI).substring(0, 
                (individualURI).indexOf("#") + 1));
        
        // load model into swrl factory to manipulate rule
        // set source individual URI in swrl rule (replace placeholder in rule)
        SWRLFactory swrlFactory = new SWRLFactory(dataflowModel);
        
        // get the rules to replace placeholder in
        Iterator ruleIt = swrlFactory.getImps().iterator();
        
        while (ruleIt.hasNext()) {
            SWRLImp rule = (SWRLImp) ruleIt.next();
            
            // debug output
            logger.debug("original rule: " + rule.getBrowserText());

            // get the placeholder atom from body 
            SWRLAtomList bodyAtomIterator = rule.getBody();
            SWRLIndividualPropertyAtom placeholderAtom = null;
            
            boolean foundPlaceholderInRule = false;
            int size = bodyAtomIterator.size();
            
            for (int i = 1; i < size; i++)
            {
                SWRLAtom currentAtom = (SWRLAtom)bodyAtomIterator.getFirst();
                
                if (currentAtom instanceof SWRLIndividualPropertyAtom)
                {
                    placeholderAtom = (SWRLIndividualPropertyAtom)currentAtom;
                    
                    if ((placeholderAtom.getArgument2().getName()).contains("placeholder"))
                    {
                        foundPlaceholderInRule = true;
                        break;
                    }                    
                }
                bodyAtomIterator = (SWRLAtomList)bodyAtomIterator.getRest();
            }
            logger.debug("  foundPlaceholderInRule: " + foundPlaceholderInRule);
            
            if (foundPlaceholderInRule)
            {              
                // get the parts of the atom that are necessary to construct a new body atom
                OWLObjectProperty bodyProperty = placeholderAtom.getPropertyPredicate();
                RDFResource bodyAnkerVariable = placeholderAtom.getArgument1();
                
                // get owl individual from protege model using prepared individual name 
                // (BUG: namespace is not mapped to prefix) 
                OWLIndividual individual = dataflowModel.getOWLIndividual(prefix + ":" 
                        + new URI((individualURI)).getFragment());
                
                // create new rule atom with the given individual  
                SWRLIndividualPropertyAtom bodyIndividualPropertyAtom = 
                    swrlFactory.createIndividualPropertyAtom(bodyProperty, bodyAnkerVariable, 
                            individual);

                // get original body list to append and delete (replace) the atom
                SWRLAtomList body = rule.getBody();

                // append the new atom to the body
                body.append(bodyIndividualPropertyAtom);
                
                // remove the old atom from the body (order of append and remove is important!)
                placeholderAtom.delete();
            }
            else
            {
                logger.debug("Did not find placeholder-variable in rule body!");
            }
            
            // PROCESS HEAD
            foundPlaceholderInRule = false;
           
            
            // get the atoms of the head
            SWRLAtomList headAtomIterator = rule.getHead();
            
            int headSize = headAtomIterator.size();
            
            for (int i = 1; i < headSize; i++)
            {
                SWRLAtom currentAtom = (SWRLAtom)headAtomIterator.getFirst();
                
                if (currentAtom instanceof SWRLIndividualPropertyAtom)
                {
                    placeholderAtom = (SWRLIndividualPropertyAtom)currentAtom;
                    
                    if ((placeholderAtom.getArgument2().getName()).contains("placeholder"))
                    {
                        foundPlaceholderInRule = true;
                        break;
                    }                    
                }
                headAtomIterator = (SWRLAtomList)headAtomIterator.getRest();
            }
            
            if (foundPlaceholderInRule)
            {              
                // get the parts of the atom that are necessary to construct a new atom
                OWLObjectProperty property = placeholderAtom.getPropertyPredicate();
                RDFResource ankerVariable = placeholderAtom.getArgument1();
                
                // get owl individual from protege model using prepared individual name 
                //(BUG: namespace is not mapped to prefix) 
                OWLIndividual individual = dataflowModel.getOWLIndividual(prefix + ":" 
                        + new URI((individualURI)).getFragment());

                // create new rule atom with the given individual  
                SWRLIndividualPropertyAtom headIndividualPropertyAtom = 
                    swrlFactory.createIndividualPropertyAtom(property, ankerVariable, individual);

                // get original head list to append and delete (replace) the atom
                SWRLAtomList head = rule.getHead();

                // append the new atom to the head
                head.append(headIndividualPropertyAtom);
                
                // remove the old atom from the head (order of append and remove is important!)
                placeholderAtom.delete();                
            }
            else
            {
                logger.debug("Did not find placeholder-variable in rule head!");
            }
            
            // debug output
            logger.debug("final rule: " + rule.getBrowserText());
        }   
        
        return swrlFactory;
    }

    /**
     * Perform a data flow defined in the rule model that transfers an individuals 
     * (specified via its URI) into the given target container model.
     * 
     * @param ruleContainer the data flow-rule-model (as RDF/XML DOM-Node) 
     *                      that contains one rule to copy the individual
     * @param targetContainer the target-model-container (as RDF/XML DOM-Node) 
     *                      that contains an anchor for the individual to be placed
     * @param sourceContainers a list of source-model-containers (as RDF/XML DOM-Node) 
     *                       that define the inputs for the data flow
     *                       
     * @return the target container with the transfered individual
     * 
     * @throws FunctionCallException
     */
    private Object executeDataflow(Node ruleContainer, Node targetContainer, String individualURI,
            List sourceContainers) throws FunctionCallException 
    {
                String output = null;

                // execute the semantic data flow for one specified individual
                try {
                    // load the target container model (ABox only, TBox is imported 
                    // within ABox model) into knowledge base
                    Node targetModelRoot = SemanticUtils.findRdfNode(targetContainer);
                    String targetModelString = SemanticUtils.elementToXMLString(targetModelRoot);
                    JenaOWLModel dataflowModel = ProtegeOWL
                        .createJenaOWLModelFromReader(new StringReader(targetModelString));     
                    
                    // get the root individual in the model (the Process individual)
                    // (this is used to write the minimal inferred model after reasoning)
                    RDFResource rootIndividual = null;
                    String processClassName = dataflowModel.getResourceNameForURI(
                            "http://localhost:8080/ontologies/ServiceAnchor.owl#Process");
                    RDFResource process = dataflowModel.getRDFResource(processClassName);
                    Collection individuals = dataflowModel.getUserDefinedRDFIndividuals(false);
                    for (Iterator iterator = individuals.iterator(); iterator.hasNext();) {
                        rootIndividual = (RDFResource) iterator.next();
                        
                        if (rootIndividual.getDirectTypes().contains(process))
                        {
                            break;
                        }
                    }

                    // import the rule model next, so that all necessary imports
                    // are already in the model before the source containers are imported.
                    // If the source containers don´t define their imports, then
                    // loading the rule model first no problems with "untyped" resources occur.
                    SemanticUtils semanticUtils = new SemanticUtils();                    
                    if (ruleContainer != null)
                    {
                        semanticUtils.importIntoModel(ruleContainer, dataflowModel);
                    }

                    // import all source containers
                    for (Iterator sourceContainerIt = sourceContainers.iterator(); 
                            sourceContainerIt.hasNext();) {
                        Node sourceContainer = (Node) sourceContainerIt.next();
                        
                        semanticUtils.importIntoModel(sourceContainer, dataflowModel);  

                        // TODO this is a workaround for removing "bad" namespace declarations
                        // caused by creating objects that do not have a namespace declaration
                        //dataflowModel = SemanticUtils
                        //    .removeBadNamespaceDeclarations(dataflowModel);
                    }


                    // TODO this is a workaround for removing "bad" namespace declarations
                    // caused by creating objects that do not have a namespace declaration
                    //dataflowModel = SemanticUtils.removeBadNamespaceDeclarations(dataflowModel);

                    // SWRLFactory for manipulation of rule model
                    SWRLFactory swrlFactory = null;                  
                    
                    if (individualURI != null && !"".equals(individualURI))
                    {
                        // replace the variable named "placeholder" in the one and only rule of 
                        // the imported rule model
                        swrlFactory = replacePlaceholderInDataflowRule(individualURI, 
                                dataflowModel);
                    }

                    // perform all necessary steps for OWL and SWRL reasoning 
                    // (which performs the actual dataflow)
                    SemanticUtils.performOWLandSWRLreasoning(dataflowModel);

                    if (swrlFactory != null)
                    {
                        swrlFactory = new SWRLFactory(dataflowModel);
                    }
                    
                    // delete the rules, since we do not want to safe them 
                    // into the resulting ontology
//                    swrlFactory.deleteImps();

                    // TODO this is a bugfix for the "swrlFactory.deleteImps()"-method, 
                    // which does not delete everything
                    // remove all swrl related facts (like e.g. swrl-variables)
//                    SemanticUtils.removeRemainingSWRLFacts(dataflowModel);
                
                    output = SemanticUtils.writeMinimalInferredModel(dataflowModel, 
                            rootIndividual);

                    // make sure the memory is deallocated
                    dataflowModel.dispose();
//                    System.gc();
//                    System.runFinalization();
//                    System.gc();
                    
                    logger.debug("Semantic Dataflow Result: " + output);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    
                    FunctionCallException exception = new FunctionCallException(
                            "Custom XPath Error in executeSemanticDataflow: "
                            + " Execution of dataflow failed. Details: " + ex.getMessage());
                    logger.error(exception);
                    throw exception;
                } 
                
                // return the resulting ontology model as string
                return output;       
          }
}


