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
import com.ibm.broker.plugin.*;

public class SampleUtils
{

  /**
   * Adds a minimal MQMD to a message, allowing the message to be propagated
   * to a MQOutputNode.
   * 
   * @param msg the message to add the MQMD to.
   * @param format the format of the body or the next header e.g. "XML" or "MQHRF".
   * @return the message passed in with the added MQMD.
   * @throws MbException
   */
  public static MbMessage addMqmd(MbMessage msg,
                                  String    format) throws MbException
  {
    MbElement root = msg.getRootElement();

    MbElement body = root.getLastChild();
    MbElement mqmd = body.createElementBefore("MQHMD");

    mqmd.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "Format", format);

    // Other fields can be added here
    
    return msg;
  }

  /**
   * Adds a minimal RFH2 to a message.
   * 
   * @param msg the message to add the RFH2 to.
   * @param format the format of the body or the next header. 
   * @return the message passed in with the added RFH2.
   * @throws MbException
   * 
   */
  public static MbMessage addRfh2(MbMessage msg,
                                  String    format) throws MbException
  {
    MbElement root = msg.getRootElement();

    MbElement body = root.getLastChild();
    MbElement rfh2 = body.createElementBefore("MQHRF2");

    rfh2.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "Format", format);

    // Other fields can be added here
    
    return msg;
  }


  
}



