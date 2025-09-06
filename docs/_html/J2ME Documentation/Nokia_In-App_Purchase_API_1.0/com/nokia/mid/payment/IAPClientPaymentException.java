/*======================================================================
 
CLASS       : com.nokia.mid.payment.IAPClientPaymentException
SUMMARY     : Exception class for IAPClientPaymentManager
COPYRIGHT   : (c) 2011 Nokia Mobile Phones. All rights reserved.
 
========================================================================
 
CCM DETAILS : %name:          IAPClientPaymentException.java %
              %version:       1 %
              %date_modified: Fri Jun 01 18:22:48 2012 %
              %status:        working %
========================================================================

VERSION     : 2        Feb 1, 2011        Sonia Sarkar       
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#63909
DESCRIPTION : Updated comments
  
========================================================================

VERSION     : 1        Dec 8, 2010        Sonia Sarkar       
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#61723
DESCRIPTION : API support for In Application Payment.

 ======================================================================*/
package com.nokia.mid.payment;

/**
 * The <code>IAPClientPaymentException</code> is thrown when any unexpected system error occurs.
 * 
 */
public class IAPClientPaymentException extends Exception {
    /** 
     * Constructs a <code>IAPClientPaymentException</code> with no detail message. 
     */
    public IAPClientPaymentException() {
        super();
    }

    /** 
     * Constructs a <code>IAPClientPaymentException</code> with the specified 
     * detail message. 
     * @param s the detail message
     */
    public IAPClientPaymentException(String s) {
        super(s);
    }  
}
