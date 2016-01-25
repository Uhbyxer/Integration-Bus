package com.ibm.broker.filenodes.samples;

/*
Sample program for use with IBM WebSphere Message Broker
� Copyright International Business Machines Corporation 2007, 2010 
Licensed Materials - Property of IBM
*/
import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.*;
import java.util.List;

// This class updates some of the item and price details in a sales invoice contained within the incoming namespaced SOAP message.
public class FileOutputNodeSample_JavaCompute extends MbJavaComputeNode {

	@SuppressWarnings("unchecked")
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {

	// Define the output terminal.
	MbOutputTerminal out = getOutputTerminal("out");

	// Get the input message. 
	MbMessage inMessage = inAssembly.getMessage();

	// Create a new message.
	MbMessage outMessage = new MbMessage(inMessage);
	MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly, outMessage);

	try {
		// Use an XPath query to select the first element of the message body ('SaleListMessage').
		// As the input message is namespaced, an MbXPath object is created. A mapping is then added between each
		// namespace prefix and its corresponding full namespace URI. 
		MbXPath xp = new MbXPath("/soap:Envelope/soap:Body/samp:SaleListMessage");
		xp.addNamespacePrefix("soap", "http://schemas.xmlsoap.org/soap/envelope/");
		xp.addNamespacePrefix("samp", "http://www.samplemessage.broker.hursley.ibm.com");
		
		// This returns a list of all SaleListMessage elements in the message.
		List<MbElement> saleListMessages = (List<MbElement>) outMessage.evaluateXPath(xp); 
		// Get the first element in the list (Note that there is only ever one such element in the expected incoming message).
		MbElement saleListMessage = (MbElement) saleListMessages.get(0); 

		// Update the name of the 'SaleListMessage' element of the XML message body to 'SaleListReplyMessage'
		// to indicate that this message is the reply.
		saleListMessage.setName("SaleListReplyMessage");
		
		// The XPath expression below updates any description of "Microscope" with that of "Deluxe Microscope".
		// The 'SaleListReplyMessage' element is used as the context node from which to begin the query.
		saleListMessage.evaluateXPath("SaleEnvelope/SaleList/Invoice/Item[Description='Microscope']/Description[set-value('Deluxe Microscope')]");

		// The XPath expression below finds any item with a description of "Deluxe Microscope" and updates it's price to "44.00".
		// The 'SaleListReplyMessage' element is used as the context node from which to begin the query.
		saleListMessage.evaluateXPath("SaleEnvelope/SaleList/Invoice/Item[Description='Deluxe Microscope']/Price[set-value('44.00')]");

		// Propagate the message to the output terminal.
		out.propagate(outAssembly);
		} finally {
			// Clear the output message after it has been propagated.
			outMessage.clearMessage();
		}
	}
}
