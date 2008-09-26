package org.qualipso.interop.semantics.procintegration.bpel.engine.extension;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.mindswap.owl.OWLFactory;
import org.mindswap.owl.OWLKnowledgeBase;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.util.FileUtils;

import edu.stanford.smi.protegex.owl.ProtegeOWL;

public class Test {

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        URI outputContainerUri = null;
        OntModel outputContainer = null;
        OWLKnowledgeBase outputContainerKb = OWLFactory.createKB();
        // read output container model
        //String outputContainerString = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns=\"http://service.GetResidentInfo.Input#\" xmlns:anchor=\"http://localhost:8080/ontologies/ServiceAnchor.owl#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xml:base=\"http://service.GetResidentInfo.Input\">  <owl:Ontology rdf:about=\"\">    <owl:imports rdf:resource=\"http://localhost:8080/ontologies/ServiceAnchor.owl\"/>    <owl:imports rdf:resource=\"http://localhost:8080/PublicServices/ResidentRegistry/ResidentRegistryOntology.owl\"/>  </owl:Ontology>  <anchor:Process rdf:ID=\"GetResidentInfoProcess\">    <anchor:hasInput>      <anchor:Input rdf:ID=\"ResidentSearchProfileInput\">        <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">ResidentSearchProfile</rdfs:label>      </anchor:Input>    </anchor:hasInput>    <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">GetResidentInfoProcess</rdfs:label>  </anchor:Process></rdf:RDF>";
        //String outputContainerString = "<rdf:RDF  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"  xmlns=\"http://service.GetResidentInfo.Input#\"  xmlns:anchor=\"http://localhost:8080/ontologies/ServiceAnchor.owl#\"  xmlns:owl=\"http://www.w3.org/2002/07/owl#\"  xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"  xml:base=\"http://service.GetResidentInfo.Input\">  <owl:Ontology rdf:about=\"\">  <owl:imports rdf:resource=\"http://localhost:8080/ontologies/ServiceAnchor.owl\"/>  </owl:Ontology>  <anchor:Process rdf:ID=\"GetResidentInfoProcess\">  <anchor:hasInput>  <anchor:Input rdf:ID=\"ResidentSearchProfileInput\">  <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">ResidentSearchProfile</rdfs:label>  </anchor:Input>  </anchor:hasInput>  <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">GetResidentInfoProcess</rdfs:label>  </anchor:Process> </rdf:RDF>";
        String outputContainerString = //"<rdf:RDF   xmlns:moon=\"http://localhost:8080/ontologies/MoonOntology.owl#\"   xmlns:protege=\"http://protege.stanford.edu/plugins/owl/protege#\"  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"    xmlns:owl=\"http://www.w3.org/2002/07/owl#\"    xmlns=\"http://process.MediationProcess.MoonOMSCloseOrderServiceOutput.owl#\"  xml:base=\"http://process.MediationProcess.MoonOMSCloseOrderServiceOutput.owl\">  <owl:Ontology rdf:about=\"\">  <owl:imports rdf:resource=\"http://localhost:8080/ontologies/ServiceAnchor.owl\"/>  <owl:imports rdf:resource=\"http://localhost:8080/ontologies/MoonOntology.owl\"/>  </owl:Ontology>  <moon:CloseOrderResponse rdf:ID=\"CloseOrderResponse_1\">  <moon:hasOrder>  <moon:Order rdf:ID=\"Order_2\">  <moon:hasOrderID rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">2345</moon:hasOrderID>  </moon:Order>  </moon:hasOrder>  </moon:CloseOrderResponse> </rdf:RDF>";
                "<rdf:RDF  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"  " +
                "xmlns=\"http://process.MediationProcess.MoonCRMServiceOutputContainer.owl#\"  " +
                "xmlns:protege=\"http://protege.stanford.edu/plugins/owl/protege#\" " + 
                "xmlns:anchor=\"http://localhost:8080/ontologies/ServiceAnchor.owl#\"  " +
                "xmlns:daml=\"http://www.daml.org/2001/03/daml+oil#\" " + 
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +  
                "xmlns:owl=\"http://www.w3.org/2002/07/owl#\"  " +
                "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"  " +
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"  " +
                "xml:base=\"http://process.MediationProcess.MoonCRMServiceOutputContainer.owl\">  " +
                "<anchor:Output rdf:ID=\"Output\">  " +
                "<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">CustomerLookupResponse</rdfs:label>  " +
                "</anchor:Output>  <anchor:Process rdf:ID=\"MoonCRMService\">  " +
                "<anchor:hasOutput rdf:resource=\"#Output\"/>  " +
                "<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">MoonCRMServiceProcess</rdfs:label>  " +
                "</anchor:Process> " +
                "</rdf:RDF>";

        outputContainerUri =
                SemanticUtils.extractOntologyNameUriFromRDF(outputContainerString, true);
        
        System.out.println(outputContainerUri);
        // outputContainerKb.setAutoConsistency(true);
        StringReader stringReader = new StringReader(outputContainerString);
        outputContainer = (OntModel) outputContainerKb.read(stringReader, outputContainerUri).getImplementation();        
        //outputContainer = ProtegeOWL.createJenaOWLModelFromReader(stringReader).getOntModel();

        outputContainer.write(System.out, FileUtils.langXML, outputContainerUri.toString());

    }

}
