/* 
 * Sample program for use with Product          
 *  ProgIds: 5724-J06 5724-J05 5724-J04 5697-J09 5655-M74 5655-M75 5648-C63 
 *  (C) Copyright IBM Corporation 2005.                      
 * All Rights Reserved * Licensed Materials - Property of IBM 
 * 
 * This sample program is provided AS IS and may be used, executed, 
 * copied and modified without royalty payment by customer 
 * 
 * (a) for its own instruction and study, 
 * (b) in order to develop applications designed to run with an IBM 
 *     WebSphere product, either for customer's own internal use or for 
 *     redistribution by customer, as part of such an application, in 
 *     customer's own products. 
 */

package com.ibm.broker.javacompute.samples;

import java.util.List;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbXPath;

public class JavaComputeTransformXPath extends MbJavaComputeNode 
{

   // The message transformation involves a triple nested loop
   // Each loop is handled by an object that iterates over the result
   // of an XPath expression on the incoming message
   private final ArticleCreator articleCreator;
   private final StatementCreator statementCreator;
   private final SaleListCreator saleListCreator;
    
   /*
    * node constructor defined to do deploy time initialisation
    */
   public JavaComputeTransformXPath() throws MbException
   {
      // instantiate the output tree creation objects

      saleListCreator = new SaleListCreator();
      statementCreator = new StatementCreator();
      articleCreator = new ArticleCreator();
   }

   public void evaluate(MbMessageAssembly inAssembly) throws MbException 
   {
      MbOutputTerminal out = getOutputTerminal("out");

      MbMessage inMessage = inAssembly.getMessage();

      // create new message
      MbMessage outMessage = new MbMessage();
      MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly,
         outMessage);

      // optionally copy message headers
      copyMessageHeaders(inMessage, outMessage);

      // ----------------------------------------------------------
      // Add user code below

      MbElement inRoot = inMessage.getRootElement();   // reference to root of incoming message
      MbElement outRoot = outMessage.getRootElement(); // reference to root of outgoing message
      outRoot.createElementAsLastChild("XMLNSC");  // create the 'Body' XMLNSC element

      saleListCreator.setOutputElement(outRoot); // set the root of the tree to be build
      saleListCreator.evaluate(inRoot); // iterate over SaleList to build output message

      // End of user code
      // ----------------------------------------------------------

      // The following should only be changed
      // if not propagating message to the 'out' terminal
      out.propagate(outAssembly);
   }

   public void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
      throws MbException 
   {
      MbElement outRoot = outMessage.getRootElement();

      // iterate though the headers starting with the first child of the root element
      MbElement header = inMessage.getRootElement().getFirstChild();
      while (header != null && header.getNextSibling() != null) // stop before the last child (body)
      {
         // copy the header and add it to the out message
         outRoot.addAsLastChild(header.copy());
         // move along to next header
         header = header.getNextSibling();
      }
   }

   /**
    * Iterates over the incoming message's SaleList elements and builds the output
    * message's SaleList
    */
   private final class SaleListCreator extends XPathOperation 
   {
      private SaleListCreator() throws MbException 
      {
         super("/SaleEnvelope/SaleList"); // expression evaluated on the incoming message
      }

      /**
       * Called once for each SaleList element
       */
      @SuppressWarnings("unchecked")
	protected void forEachElement(MbElement saleList) throws MbException
      {
         List <MbElement> nodeset = (List <MbElement>)getOutputElement().evaluateXPath("/?SaleEnvelope/?$SaleList");
         MbElement outSaleList = nodeset.get(0);
        	
         // create the statements for each SaleList
         statementCreator.setOutputElement(outSaleList);
         statementCreator.evaluate(saleList);
      }
   }

   /**
    * Iterates over the incoming message's Invoice elements and builds the output
    * message's Statement sub-tree
    */
   private final class StatementCreator extends XPathOperation 
   {
      private final MbXPath setCustomer = new MbXPath(
         "?$Statement[?@Type[set-value('Monthly')]]" +
         "           [?@Style[set-value('Full')]]" +
         "           [?Customer[?Initials[set-value(concat($invoice/Initial[1], $invoice/Initial[2]))]]" +
         "                     [?Name[set-value($invoice/Surname)]]" +
         "                     [?Balance[set-value($invoice/Balance)]]]" +
         "           /?Purchases");

      private StatementCreator() throws MbException 
      {
         super("Invoice"); // expression evaluated on the incoming SaleList sub-tree
      }

      /**
       * Called once for each Invoice element
       */
      @SuppressWarnings("unchecked")
	protected void forEachElement(MbElement invoice) throws MbException
      {
         MbElement outSaleList = getOutputElement();

         setCustomer.assignVariable("invoice", invoice);
         List <MbElement> nodeset = (List <MbElement>)outSaleList.evaluateXPath(setCustomer);
         MbElement purchases = nodeset.get(0);

         // Now create the articles for each invoice
         articleCreator.setOutputElement(purchases);
         articleCreator.evaluate(invoice);
      }
   }

   /**
    * Iterates over the incoming message's Item elements and builds the output
    * message's Article sub-tree
    */
   private final class ArticleCreator extends XPathOperation 
   {
      private double total;

      private final MbXPath setArticle = new MbXPath(
         "?$Article[?Desc[set-value($item/Description)]]" +
         "         [?Cost[set-value($item/Price * 1.6)]]" +
         "         [?Qty[set-value($item/Quantity)]]");

      private final MbXPath setTotal = new MbXPath("?>Amount[set-value($total)]");

      private ArticleCreator() throws MbException 
      {
         super("Item"); // expression evaluated on the incoming Invoice sub-tree
      }

      /**
       * Initialisation of total before iteration
       */
      protected void before()
      {
         total = 0;
      }

      /**
       * Called once for each Item element
       */
      @SuppressWarnings("unchecked")
	protected void forEachElement(MbElement item) throws MbException
      {
         MbElement purchases = getOutputElement(); // build the output tree from this point
         setArticle.assignVariable("item", item);
         List <MbElement> nodeset = (List <MbElement>)purchases.evaluateXPath(setArticle);
         MbElement article = nodeset.get(0);
         total += ((Double)article.evaluateXPath("Cost * Qty")).doubleValue();
      }

      /**
       * After iteration, set the total
       */
      protected void after() throws MbException
      {
         setTotal.assignVariable("total", total);
         getOutputElement().evaluateXPath(setTotal);
      }
   }

   /**
    * XPathOperation is an abstract helper class allowing a method to be repeatedly applied to 
    * results of an XPath expression.  The user can optionally access the output message tree
    * allowing message transformations to be defined based on XPath evaluations of the input
    * tree.
    */
   abstract class XPathOperation
   {
      private MbXPath xpath = null;
      private MbElement outputElement = null;

      /**
       * Constructor taking the XPath expression to evaluate.  The expression must evaluate to 
       * a nodeset.
       */
      public XPathOperation(String expression) throws MbException
      {
         xpath = new MbXPath(expression);
      }

      /**
       * Must be called prior to the evaluate method if the user wishes to work with the output
       * tree (eg for message transformation).
       */
      public void setOutputElement(MbElement out)
      {
         outputElement = out;
      }

      /**
       * Allows the user to access the output tree in the forEachElement() method
       */
      public MbElement getOutputElement()
      {
         return outputElement;
      }

      /** 
       * Can be overridden by the user to do initialisation processing before iterating over the
       * XPath nodeset results
       */
      protected void before() throws MbException { }

      /**
       * This gets called once for each element in the XPath nodeset results. The current element
       * is passed in as the only argument.  This method is abstract and must be implemented
       * in the derived class.
       */
      protected abstract void forEachElement(MbElement element) throws MbException;

      /** 
       * Can be overridden by the user to do post-processing after iterating over the
       * XPath nodeset results
       */
      protected void after() throws MbException { }

      /**
       * Called by the user to iterate over the XPath nodeset.
       *
       * @param element The context element to which the XPath expression will be applied.
       */
      @SuppressWarnings("unchecked")
	public void evaluate(MbElement element) throws MbException
      {
         Object result = element.evaluateXPath(xpath);
         if(!(result instanceof List))
         {
            // error
            return;
         }

         before();

         List<MbElement> nodeset = (List<MbElement>)result;
                 
         for(MbElement node : nodeset)
         {
            forEachElement(node);
         }

         after();
      }

   }
}
