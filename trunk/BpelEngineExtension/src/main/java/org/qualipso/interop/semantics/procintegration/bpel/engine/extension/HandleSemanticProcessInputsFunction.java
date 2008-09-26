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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.w3c.dom.Node;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileUtils;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.NamespaceMap;
import edu.stanford.smi.protegex.owl.model.util.ImportHelper;

/**
 * Class representing a custom XPath function, used by the expression evaluator
 * to handle the semantic inputs sent to the process. An initiating message to
 * the process can consist of several parts. Each part represents an OWL
 * ontology containing individuals that represent the process input. In order to
 * be able to use these inputs in a dataflow, the process inputs have to be
 * hooked into a container variable. This is performed by this function.
 */
public class HandleSemanticProcessInputsFunction implements Function {

    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(HandleSemanticProcessInputsFunction.class);

    /**
     * Hooks the process inputs into the process container variable 
     * in order to be able to apply the semantic data flow approach to these inputs.
     * 
     * @param aContext
     *            the function context (currently ignored)
     * @param aArgs a list of arguments;
     *            the first argument is the name of the process
     *            all other arguments are process inputs;
     *            a process input consists of three parts:
     *            1. the name of the input parameter (String)
     *            2. the URI of the data type of the input (String)
     *            3. the rdf-serialisation of the semantic input (Node).
     *            
     *            So, a valid number of arguments must abide to
     *            the equation <code>1 + 3*x</code>
     *            with <code>x</code> the number of inputs.
     *            
     * 
     * @return the RDF/XML serialisation of the resulting model
     * 
     * @throws FunctionCallException
     */
    public Object call(Context aContext, List aArgs) throws FunctionCallException 
    {
        if (aArgs.size() < 4) {
            String errorMessage = "Custom XPath Error in executeSemanticWebService: "
                    + "requires at least 4 parameters, but provided where " + aArgs.size();
            logger.error(errorMessage);
            throw new FunctionCallException(errorMessage);
        } else if (!(aArgs.get(0) instanceof String)) {
            String errorMessage = "Custom XPath Error in executeSemanticWebService: "
                    + "first argument not of type java.lang.String, " + "it's a "
                    + aArgs.get(0).getClass().toString();
            logger.error(errorMessage);
            throw new FunctionCallException(errorMessage);
        }
        for (int i = 1; i < aArgs.size(); i = i + 3) {
            if (!(aArgs.get(i) instanceof String)) {
                String errorMessage = "Custom XPath Error in executeSemanticWebService: " + i
                        + ". argument not of type java.lang.String, " + "it's a "
                        + aArgs.get(i).getClass().toString();
                logger.error(errorMessage);
                throw new FunctionCallException(errorMessage);
            } else if (!(aArgs.get(i + 1) instanceof String)) {
                String errorMessage = "Custom XPath Error in executeSemanticWebService: "
                        + (i + 1) + ". argument not of type java.lang.String, " + "it's a "
                        + aArgs.get(i + 1).getClass().toString();
                logger.error(errorMessage);
                throw new FunctionCallException(errorMessage);
            } else if (!(aArgs.get(i + 2) instanceof Node)) {
                String errorMessage = "Custom XPath Error in executeSemanticWebService: "
                        + (i + 2) + ". argument not of type org.w3c.dom.Node, " + "it's a "
                        + aArgs.get(i + 2).getClass().toString();
                logger.error(errorMessage);
                throw new FunctionCallException(errorMessage);
            }
        }
        
        String output;
        try {
            // create container
            String processName = (String)aArgs.get(0);
            JenaOWLModel jenaOWLModel = ProtegeOWL.createJenaOWLModel();
            NamespaceMap nsm = jenaOWLModel.getNamespaceManager();
            //jenaOWLModel.getDefaultOWLOntology().setName(processName + "Input");
            String defaultNamespace = "http://process.inputs." + processName + "Input.owl#";
            nsm.setDefaultNamespace(defaultNamespace);
            jenaOWLModel.getTripleStoreModel().getTopTripleStore()
                .setDefaultNamespace(defaultNamespace);
            ImportHelper importHelper = new ImportHelper(jenaOWLModel);
            importHelper.addImport(new URI("http://localhost:8080/ontologies/ServiceAnchor.owl"));
            
            for (int i = 2; i < aArgs.size(); i = i + 3) {
                String namespace = (String)aArgs.get(i);
                namespace = namespace.substring(0, namespace.indexOf("#"));
                importHelper.addImport(new URI(namespace));
            }
            
            importHelper.importOntologies(false);
            OntModel compositeProcessInputContainerModel = jenaOWLModel.getOntModel();
            
            // add ProcessRoot to CompositeProcessInput container
            Individual compositeProcess = 
                addProcessRoot(compositeProcessInputContainerModel, processName + "Process");

            // for each input parameter (i) attach the value (i+1) to process
            // anchor
            for (int i = 1; i < aArgs.size(); i = i + 3) {
                // compositeProcess hasInput Input
                String parameterName = (String) aArgs.get(i);
                String compositeProcessInputType = (String) aArgs.get(i + 1);

                Individual compositeProcessInput = addProcessInput(
                        compositeProcessInputContainerModel, compositeProcess, parameterName);

                // read input-parameter into model
                Node inputParameterRoot = SemanticUtils.findRdfNode((Node) aArgs.get(i + 2));
                String inputParameterString = SemanticUtils.elementToXMLString(inputParameterRoot);

                inputParameterString = inputParameterString.replaceAll(
                        "<(.)xml version=\"1.0\" encoding=\"UTF-8\"(.)>", "");
                inputParameterString = inputParameterString.replaceAll("&lt;", "<");
                inputParameterString = inputParameterString.replaceAll("&gt;", ">");

                // if primitive datatype create new literal
                RDFNode inputParameterValue = null;
                if (SemanticUtils.isPrimitiveType(compositeProcessInputType)) {
                    OntModel model = compositeProcessInputContainerModel;

                    inputParameterValue = model.createLiteral(inputParameterString);
                } 
                else {
                    OntModel inputParameterModel = ModelFactory
                            .createOntologyModel(OntModelSpec.OWL_MEM);
                    inputParameterModel.read(new StringReader(inputParameterString), defaultNamespace);

                    // catch the right individual with type of input parameter
                    Resource indClass = inputParameterModel.getResource(compositeProcessInputType);

                    Iterator individualsIter = inputParameterModel.listIndividuals(indClass);
                    logger.debug("Model has individuals? " + individualsIter.hasNext());
                    inputParameterValue = (Individual) individualsIter.next();

                    compositeProcessInputContainerModel.add(inputParameterModel);

                    addProcessParameterObjectValue(compositeProcessInputContainerModel,
                            compositeProcessInput, inputParameterValue);
                }
            }

            // write the resulting container
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            compositeProcessInputContainerModel
                .write(bout, FileUtils.langXML, defaultNamespace);
            logger.debug("Handle Semantic Process Inputs Result: " + bout);

            output = bout.toString();
            bout.close();
        } catch (Exception ex) {
            throw new FunctionCallException("Could not handle semantic process inputs. ", ex);
        }

        // write the resulting document
        return SemanticUtils.xmlStringToElement(output, "WrapperElement4ProcessInputs");
    }

    /**
     * Creates a process individual to use as parameter root.
     * 
     * @param model
     * @param processLabel
     * @return process root individual
     */
    private Individual addProcessRoot(OntModel model, String processLabel) {
        OntClass Process = model
                .createClass("http://localhost:8080/ontologies/ServiceAnchor.owl#Process");
        Individual aProcess = model.createIndividual(Process);
        aProcess.addLabel(processLabel, null);
        return aProcess;
    }

    /**
     * Attaches an input individual to a given process individual
     * 
     * @param model
     *            the ontology model representing the container with all the
     *            process inputs
     * @param process
     *            the process where the input should be added
     * @param inputName
     *            the name of the input
     * 
     * @return ProcessInput individual
     */
    private Individual addProcessInput(OntModel model, Individual process, String inputName) {
        OntClass Input = model
                .createClass("http://localhost:8080/ontologies/ServiceAnchor.owl#Input");
        OntProperty hasInput = model
                .createOntProperty("http://localhost:8080/ontologies/ServiceAnchor.owl#hasInput");
        Individual anInput = model.createIndividual(Input);
        process.addProperty(hasInput, anInput);
        anInput.addLabel(inputName, null);
        return anInput;
    }

    /**
     * Proces --> hasInput --> Input --> parameterObject --> parameterValue
     * 
     * @param model
     *            the ontology model representing the container with all the
     *            process inputs
     * @param parameter
     *            the parameter where to add the value
     * @param parameterValue
     *            the value to add
     */
    private void addProcessParameterObjectValue(OntModel model, Individual parameter,
            RDFNode parameterValue) {
        OntProperty parametervalue = model
                .createOntProperty(
                     "http://localhost:8080/ontologies/ServiceAnchor.owl#parameterObject");
        parameter.addProperty(parametervalue, parameterValue);
    }

}
