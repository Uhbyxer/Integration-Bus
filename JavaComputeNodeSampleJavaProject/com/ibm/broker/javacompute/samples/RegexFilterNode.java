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
import java.util.ListResourceBundle;
import java.util.regex.*;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.*;

/**
 * 
 * The RegexFilterNode sample demonstrates how a Java compute node can be used as
 * a filter node and the use of user defined attributes.
 * 
 * <p>
 * The node has two user defined attributes "filterField" and "filterRegex". The node 
 * extract the element value of the first field in the message with name held by the user attribute
 * "filterField". If the value matches the regular expression held by the user defined attribute
 * "filterRegex" the message is propagated to the "out" terminal, otherwise it is propagated to the "alternate" terminal.
 */
public class RegexFilterNode extends MbJavaComputeNode
{  
  // The user defined attribute that holds the regular expression.
  private final static String FILTER_REGEX_ATTRIBUTE_NAME = "filterRegex";
  // The user defined attribute that holds the field to match on.
  private final static String FILTER_FIELD_ATTRIBUTE_NAME = "filterField";
  
  // The regular expression pattern
  private Pattern regex;
  // The XPath expression used to extract the element value.
  private String xpathExpression;
  
  /* (non-Javadoc)
   * @see com.ibm.broker.javacompute.MbJavaComputeNode#evaluate(com.ibm.broker.plugin.MbMessageAssembly)
   */
  public void evaluate(MbMessageAssembly incomingAssembly) throws MbException
  {
    final String methodName = "evaluate";
    
    try
    {
      // First use the XPath expression to extract the field value to match on.
      String fieldValue = (String)incomingAssembly.getMessage().evaluateXPath(getXPathExpression());
      
      // Create the matcher from the regex pattern and the field value.
      Matcher matcher = getRegexPattern().matcher(fieldValue);
      
      // If the field value matches the regex then propagate to "out"
      if(matcher.matches())
      {
        getOutputTerminal("out").propagate(incomingAssembly);
      }
      // Otherwise propagate to "alternate"
      else
      {
        getOutputTerminal("alternate").propagate(incomingAssembly);
      }
    }
    catch (PatternSyntaxException pse) 
    {
      // The regex provided by the user is invalid so log the error.
      MbService.logError(this,
                         methodName,
                         RegexFilterNodeMessages.MESSAGE_SOURCE,
                         RegexFilterNodeMessages.INVALID_REGEX,
                         "Invalid regex",
                         new String[] { getRegexPattern().toString() });
    }
  }

  
  /**
   * Returns the XPath expression to extract the fields value. The expression is
   * created the first time it is required, based on the value of user defined 
   * attribute "filterField".
   * 
   * @return The XPath expression
   */
  private String getXPathExpression()
  {

    //  Only create is necessary
    if(xpathExpression == null)
    {
      // First get the value of user defined attribute.
      String fieldValue = (String)getUserDefinedAttribute(FILTER_FIELD_ATTRIBUTE_NAME);
      // The XPath string function automatically convert the value to a string.
      xpathExpression = "string(//"+fieldValue+")";
    }
    return xpathExpression;
  }

  /**
   * Returns a Pattern object instance for regular expression returned by user defined
   * attribute "filterRegex". The object is create the first time the pattern is required.
   * 
   * @return The Pattern object
   */
  private Pattern getRegexPattern()
  {
    // Only create is necessary
    if(regex == null)
    {
      // Compile the user defined attribute into a Pattern object.
      regex = Pattern.compile((String)getUserDefinedAttribute(FILTER_REGEX_ATTRIBUTE_NAME));
    }
    
    return regex;
  }

  
  /**
   * The class is the ResourceBundle containg all the messages for this example.
   */
  public static class RegexFilterNodeMessages extends ListResourceBundle
  {
    public static final String MESSAGE_SOURCE = RegexFilterNodeMessages.class.getName();                                                
    public static final String INVALID_REGEX = "INVALID_REGEX";
   
    private Object[][] messages  = {{INVALID_REGEX, "{0} is not a valid regular expression." }};

    /* (non-Javadoc)
     * @see java.util.ListResourceBundle#getContents()
     */
    public Object[][] getContents()
    {
      return messages;
    }
  }
}



