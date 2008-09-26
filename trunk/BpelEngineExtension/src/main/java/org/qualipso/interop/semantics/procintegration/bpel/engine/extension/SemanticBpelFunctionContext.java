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

import java.io.File;

import org.jaxen.Function;
import org.jaxen.FunctionContext;
import org.jaxen.UnresolvableException;


/**
 * A function context provider for custom semantic XPath functions.
 */
public class SemanticBpelFunctionContext implements FunctionContext {
    /**
     * Get a function to be used by the XPath expression
     * evaluator given a qualified function name (e.g."executeSemanticBridge").
     * 
     * @param functionName
     *            the local name of the function (e.g. "executeSemanticBridge").
     * @return the function
     * @throws UnresolvableException
     */
    private Function getFunction(String functionName) throws UnresolvableException {
        if ("executeSemanticBridge".equals(functionName)) {
            return new SemanticBridgeFunction();
        }
        else if ("executeSemanticDataflow".equals(functionName)) {
            return new SemanticDataflowFunction();
        }
        else if ("executeSemanticWebService".equals(functionName)) {
            return new SemanticWebServiceFunction();
        }
        else if ("handleSemanticProcessInputs".equals(functionName)) {
            return new HandleSemanticProcessInputsFunction();
        }
        else if ("handleSemanticProcessOutputs".equals(functionName)) {
            return new HandleSemanticProcessOutputsFunction();
        }

        throw new UnresolvableException(functionName);
    }

    /**
     * Get a function to be used by the XPath expression
     * evaluator given a qualified function name (e.g."executeSemanticBridge").
     * 
     * @param namespaceURI
     *            this parameter is currently ignored
     * @param prefix
     *            this parameter is currently ignored
     * @param functionName
     *            the local name of the function (e.g. "executeSemanticBridge").
     * @return the function
     */
    public Function getFunction(String namespaceURI, String prefix, String functionName)
            throws UnresolvableException {
        return getFunction(functionName);
    }


    /**
     * Make the given URI absolute. The given URI is only modified, if it starts with "project:/",
     * otherwise it is supposed to be already absolute and is returned unchanged. 
     * Hint: This method is ActiveBpel-Engine-specific!
     * 
     * @param uri the URI to check/modify
     * @return the absolute URI
     */
    public static String convertProjectRelativeUriToAbsoluteUri(String uri) {
        if (!uri.startsWith("project:/"))
        {
            return uri;
        }
        
        String projectName = uri.substring(9, uri.indexOf("/", 10));        
        String tomcatDir   = new File(".").getAbsolutePath().replace("\\", "/");
        
        uri = "file:/" + tomcatDir + "/bpr/work/ae_temp_" + projectName + "_bpr/wsdl/" 
            + projectName + uri.substring(9 + projectName.length());
        
        return uri;
    }
}



