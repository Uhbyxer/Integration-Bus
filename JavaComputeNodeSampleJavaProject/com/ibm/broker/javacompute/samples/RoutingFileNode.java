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
import java.io.IOException;
import java.io.InputStream;
import java.util.ListResourceBundle;
import java.util.Properties;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.*;

/**
 * The RoutingFileNode sample demonstrates how a Java compute node can be used as
 * a filter node, with the filtering rules being loaded from an external source, in
 * this case a properties file.
 * 
 * <p>
 * The routing rules are loaded from routingtable.cfg that is deployed with the node.
 * The node extracts a "routingvalue" element value from the incoming message. This value is
 * then used to look up the terminal that the message should be routed to.
 * 
 * <p>
 * The incoming messages is of the following format:
 * 
 * <routingfilenode>
 *   <routingvalue> ... </routingvalue>   
 * </routingfilenode>
 * 
 */
public class RoutingFileNode extends MbJavaComputeNode
{
  // The properties file containing the routing rules.
  private final static String ROUTING_TABLE_FILE = "routingtable.cfg";
  
  // The Properties object that is used to lookup the routing rules.
  private static Properties routingTable;

  
  /** (non-Javadoc)
   * @see com.ibm.broker.javacompute.MbJavaComputeNode#evaluate(com.ibm.broker.plugin.MbMessageAssembly)
   */
  public void evaluate(MbMessageAssembly incomingAssembly) throws MbException
  {
    String methodName = "evaluate";
    
    // First use an XPath expression to extract the "routingvalue" element value.
    // The input message is of the following format:
    // <routingfilenode>
    //  <routingvalue>...</routingvalue>   
    // </routingfilenode>
    String routingValue = (String)incomingAssembly.getMessage().evaluateXPath("string(/routingfilenode/routingvalue)");
    
    // If the routing value has not been found or is empty then log an
    // error and throw a MbUserException so that the message will be routed to the
    // failure terminal.
    if(routingValue.length() == 0)
    {
      System.out.println("routing value is empty");
      logErrorGenerateException(this,
                                methodName,
                                RoutingFileNodeMessages.MESSAGE_SOURCE,
                                RoutingFileNodeMessages.NO_VALUE, 
                                "Routing value is empty",
                                new String[] {});
    }
    
    try
    {
      // Use the routing table to determine the terminal to route the message to.
      // Note, that we are not directly reference the members but instead are using
      // accessor methods. This allows the members to be instantiated on demand.
      String terminal = getRoutingTable().getProperty(routingValue);
      
      // If a routing rule was found then simply route the message.
      if(terminal != null)
      {
        MbOutputTerminal out = getOutputTerminal(terminal);
        
        // Check that the terminal exists before propagating the message.
        if(out != null)
        {
          out.propagate(incomingAssembly);
        }
        // The routing rule is using a terminal that doesn't exist so log an 
        // error and throw an MbUserException. The message will be routed to the 
        // failure terminal.
        else
        {
          logErrorGenerateException(this,
                                    methodName,
                                    RoutingFileNodeMessages.MESSAGE_SOURCE,
                                    RoutingFileNodeMessages.NO_TERMINAL,
                                    "Terminal not found",
                                    new String[] {terminal});
        }
      }
      // If no routing rule was found then log the error and throw an MbUserException. 
      //The message will be routed to the failure terminal.
      else
      {
        logErrorGenerateException(this,
                                  methodName,
                                  RoutingFileNodeMessages.MESSAGE_SOURCE,
                                  RoutingFileNodeMessages.NO_RULE, 
                                  "No rule was found for value",
                                  new String[] {routingValue});
      }
    }
    catch (IOException ioex)
    {
      // An IOException has been throw, log the exception, including stack trace and
      // throw an MbUserException, so the message is propagated to the failure terminal.
      logErrorGenerateException(this,
                                methodName,
                                RoutingFileNodeMessages.MESSAGE_SOURCE,
                                RoutingFileNodeMessages.IOEX, 
                                "IOException",
                                new String[] {ioex.getMessage(), stackTraceToString(ioex)});
    }
  }
  
  /**
   * Takes a Throwable object and writes the stacktrace to a String.
   * This is used to provide that stacktrace as an insert to error
   * messages.
   * @param thr
   * @return the stacktrace.
   */
  private String stackTraceToString(Throwable thr)
  {
    StackTraceElement[] elements = thr.getStackTrace();
    StringBuffer buf = new StringBuffer();
    
    for (int i = 0; i < elements.length; i++)
    {
      StackTraceElement element = elements[i];
      buf.append(element);
      
      if(i != elements.length-1)
        buf.append(System.getProperty("line.separator"));
    }
    
    return buf.toString();
  }

  /**
   * Returns the properties object used as the routing table. Loading the
   * table if this is the first call.
   * 
   * @return Returns the properties object used as the routing table.
   * @throws IOException
   * @throws MbException
   */
  private static Properties getRoutingTable() throws IOException, MbException
  {
    if(routingTable == null)
    { 
      routingTable = loadRoutingTable();
    }
    
    return routingTable;
  }
  
  
  /**
   * Loads the routing rules from the config file into a Properties object. 
   * This method will be called the first time the routing table is requested.
   * 
   * @return The Properties object containing the routing rules.
   * @throws IOException
   * @throws MbException
   */
  private static Properties loadRoutingTable() throws IOException, MbException
  {
    // The config file is deployed with the node code, so use the nodes classloader
    // to locate the file.
    InputStream is = null;
    try{
    	is = RoutingFileNode.class.getResourceAsStream(ROUTING_TABLE_FILE);
        routingTable = new Properties();
        routingTable.load(is);
    }
    finally{
    	if(is != null){
    		is.close();
    	}
    }
    return routingTable;
  }
  
  /**
   * Logs an error message to the platforms syslog/eventlog and throws a
   * MbUserException contain the information. Throwing this exception will
   * cause the broker to propagate the current message to the failure terminal
   * 
   * @param obj The source instance.
   * @param methodName The method name.
   * @param messageSrc The message source.
   * @param key The message key.
   * @param traceText The trace text, that will be written to user trace.
   * @param inserts The inserts for the message.
   * 
   * @throws MbException
   */
  private void logErrorGenerateException(Object obj,
                                         String methodName,
                                         String messageSrc,
                                         String key,
                                         String traceText, 
                                         String[] inserts) throws MbException
  {
    MbService.logError(obj,
                       methodName,
                       messageSrc,
                       key, 
                       traceText,
                       inserts);

    throw new MbUserException(obj,
                              methodName,
                              messageSrc,
                              key, 
                              traceText,
                              inserts);
  }
  
  /**
   * The class is the ResourceBundle containg all the messages for this example.
   */
  public static class RoutingFileNodeMessages extends ListResourceBundle
  {
    public static final String MESSAGE_SOURCE = RoutingFileNodeMessages.class.getName();                                                
    public static final String IOEX = "IOEX";
    public static final String NO_VALUE = "NO_VALUE";
    public static final String NO_RULE = "NO_RULE";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String NO_TERMINAL = "NO_TERMINAL";
    
    private Object[][] messages  = {{IOEX, "An IOException has been thrown by RoutingFileNode: message: {0} stackTrace: {1}" },
                                    {NO_VALUE, "The routing value doesn't exist or was empty."},
                                    {NO_RULE, "No routing rule was found for value {0}."},
                                    {FILE_NOT_FOUND, "Unable to find routing table file in classpath"},
                                    {NO_TERMINAL, "Terminal {0} doesn't exist."}};

    /* (non-Javadoc)
     * @see java.util.ListResourceBundle#getContents()
     */
    public Object[][] getContents()
    {
      return messages;
    }
  }
  
  
}



