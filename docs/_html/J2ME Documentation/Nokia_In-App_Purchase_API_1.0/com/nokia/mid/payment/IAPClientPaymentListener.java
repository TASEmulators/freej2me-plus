/*======================================================================

 CLASS       : IAPClientPaymentListener.java
 SUMMARY     : Template Java source file for use in ThomasSCF
 COPYRIGHT   : (c) 2011 Nokia Corporation. All rights reserved.

 ========================================================================

 CCM DETAILS : %name:          IAPClientPaymentListener.java %
 %version:       1 %
 %date_modified: Fri Jun 01 18:22:51 2012 %
 
 ========================================================================
 
VERSION     : 8       06-May-2011     Jason Wang
REASON      : Accept 360 ID - 1384745
REFERENCE   : sd1iam3#71894
DESCRIPTION : File merge

VERSION     : 6.1.1       Apr 25, 2011        Richard Navarro     
REASON      : Accept 360 ID - 1227926
REFERENCE   : sd1iam3#70588
DESCRIPTION : Re-enable product restoration
 
VERSION     : 6.2.1       19-Apr-2011     Yufeng Wang
REASON      : Accept 360 ID - 1384745
REFERENCE   : sd1iam3#71894
DESCRIPTION : KNI implemenation for getRestorableProducts API. 

VERSION     : 6        Mar 29, 2011        Joe Xu       
REASON      : Accept 360 ID - 1082785
REFERENCE   : sd1iam3#69253
DESCRIPTION : Map KNI and Java listener status codes
 
VERSION     : 5         22-Mar-2011     Yufeng Wang
REASON      : Accept 360 ID - 1227607
REFERENCE   : sd1iam3#68162
DESCRIPTION : Change the return object from Vector to array in restorableProductsReceiveRead
 
VERSION     : 4        14-Mar-2011     Yufeng Wang
REASON      : Accept 360 ID - 1227607
REFERENCE   : sd1iam3#67426
DESCRIPTION : Java API(including KNI) implementation for getUserAndDeviceDataId.

VERSION     : 3        Feb 14, 2011        Joe Xu       
REASON      : Accept 360 ID - 1082785
REFERENCE   : sd1iam3#65716
DESCRIPTION : KNI and Java listener code implementation and integration test
 
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
import java.util.*;
import java.lang.*;
import java.io.*;

    /**
     * The <code>IAPClientPaymentListener</code> represents a listener that receives
     * events associated with the <code>IAPClientPaymentProvider</code>. Applications implement this interface and
     * register it with the <code>IAPClientPaymentProvider</code> to obtain asynchronous callback responses to the API requests.
     * 
     * <p> The status parameter being returned in the callback functions below can have one of the values defined in the
     * final fields of the interface.
     *
     */

public interface IAPClientPaymentListener {
     
    //  NOTE: do not remove the following commented brace.
    // native_const(IAP_STATUS)
    // {
      /**
       * The status is OK which indicates no errors.
       */  
     public static final int OK  = 1; 
              
     /**
       * Http error status.
       */  
     public static final int BAD_REQUEST = -1; 

      /**
       * Http error status.
       */  
     public static final int AUTH_FAILED = -2;
     
      /**
       * Http error status.
       */   
     public static final int FORBIDDEN = -3;      
          
      /**
       * Http error status.
       */  
     public static final int NOT_FOUND = -4; 

      /**
       * Http error status.
       */   
     public static final int SERVER_ERROR = -5; 
   
      /**
       * Http error status.
       */   
     public static final int SERVICE_UNAVAILABLE = -6;  

      /**
       * Unknown service.
       */              
     public static final int UNKNOWN_SERVICE = -7;                     
      
      /**
       * Product Id is not valid.
       */         
     public static final int INVALID_PRODUCT_ID = -8; 
      
      /**
       * Product information failed.
       */        
     public static final int PRODUCT_INFO_FAILED= -9;

      /**
       * Price of the product is invalid.
       */  
     public static final int INVALID_PRICE = -10; 

      /**
       *  Customer information failed.
       */  
     public static final int CUST_INFO_FAILED = -11; 
     
      /**
       * Payment failed using the stored instrument id.
       */  
     public static final int PMT_INSTR_FAILED = -12;      
    
      /**
       *  No payment methods are set up in the user's Nokia account.
       */  
       
     public static final int NO_PMT_METHODS = -13;
      
      /**
       *Purchase session has failed.
       */  
     public static final int PURCHASE_SESSION_FAILED = -14;      
      
      /**
       * Unknown product.
       */  
     public static final int UNKNOWN_PRODUCT = -15; 

      /**
       * Invalid Product data.
       */  
     public static final int INVALID_PRODUCT_DATA = -16; 
     
      /**
       *  Delivery limit exceeded.
       */  
     public static final int DELIVERY_LIMIT_EXCEEDED = -17; 

      /**
       * The item is restorable.
       */  
     public static final int RESTORABLE = -18; 
      
      /**
       * Restoration is not allowed for this product.
       */  
     public static final int RESTORATION_NOT_ALLOWED = -19; 

      /**
       * Number of restorations alllowed for this item has exceeeded limit.
       */ 
     public static final int RESTORATION_LMT_EXCEEDED = -20; 
     
      /**
       * Number of restorations alllowed on the device has exceeeded limit.
       */  
     public static final int RESTORATION_DEVICE_LMT_EXCEEDED = -21;  

      /**
       * General Product Error
       */  
     public static final int GENERAL_PRODUCT_ERROR = -22;      
      
      /**
       * Error in connecting to DRM server.
       */         
     public static final int DRM_SERVER_ERROR = -23; 

      /**
       * Error in activating license.
       */         
     public static final int LICENSE_ACTIVATION_ERROR = -24; 

      /**
       * Silent flag is on the request, but authorization is required.
       */       
     public static final int SILENT_OPER_FAILED = -25; 

      /**
       * User has given wrong credentials 3 times.
       */         
     public static final int OVI_SIGN_IN_FAILED = -26; 

      /**
       * Sending SMS has failed in operator payment.
       */         
     public static final int SMS_PMT_FAILED = -27; 
      
      /**
       * Operator payment failed.
       */         
     public static final int OPERATOR_BILLING_FAILED = -28;      
      
      /**
       * Unauthorized payment instrument.
       */  
     public static final int PMT_INSTR_UNAUTHORIZED = -29; 
     
      /**
       * Unknown transaction id.
       */  
     public static final int UNKNOWN_TRANSACTION_ID = -30;  

      /**
       * Timeout.
       */  
     public static final int TIMEOUT = -31; 

      /**
       * Timeout deliverd.
       */  
     public static final int TIMEOUT_DELIVERED = -32;
 
      /**
       *  General Purchase Error
       */  
     public static final int GENERAL_PURCHASE_ERROR = -33;   

      /**
       *  Operation Failed
       */  
     public static final int OPERATION_FAILED = -34;   
 
      /**
       *  General HTTP Error
       */  
     public static final int GENERAL_HTTP_ERROR = -35;   
 
      /**
       *  General User and Device Info Error
       */  
     public static final int USER_AND_DEVICE_INFO_FAILED = -36;   

     /**
       *  General Restore Product Error
       */  
     public static final int RESTORATION_FAILED = -37;

    //  NOTE: do not remove the following commented brace.
    //
    // }
       
    /**   
     * This callback function is executed when the product data request for the Nokia Store back end 
     * is complete. Product data is returned as a IAPClientProductData class that contains information 
     * about the requested product ID. The product is requested using getProductData().
     *
     * The results returned by the IAPClientProductData object will be valid only during the scope of the callback method.
     * The application will receive unpredictable results if it attempts to use the object at any other time.
     *
     * @param status  The status of the getProductData() request.  
     * @param pd  The IAPClientProductData class containing the meta data of the requested product.  
     *
     */
     void productDataReceived( int status, IAPClientProductData pd);
     
    /**   
     * This callback function is executed when the request for a list of product data to the Nokia Store back end 
     * is complete. The list of product data is returned as a IAPClientProductData array that contains information 
     * about the requested product IDs. The list of product is requested using getProductData().
     *
     * The results returned by the IAPClientProductData object array will be valid only during the scope of the callback method.
     * The application will receive unpredictable results if it attempts to use these objects at any other time.
     *
     * @param status  The status of the getProductData() request.  
     * @param productDataList  The IAPClientProductData array containing the meta data of the requested products.  
     *
     */
     void productDataListReceived( int status, IAPClientProductData[] productDataList );     
     
    /**   
     * This callback function is executed when the IAPClient gets information from the Nokia Store
     * back end indicating that the purchase has been completed. 
     *
     * @param status  The status of the purchase flow.  
     * @param purchaseTicket  The purchase ticket in base64 encoded form. It is SHA1 (160 bit) hash. 
     *  Hash is calculated over the string, which is derived by concatenating the attribute values. 
     *  The attribute values are concatenated in order transactionId, transactionTime, productId, applicationId, accountId, imei and imsi  
     *
     */        
     void purchaseCompleted( int status, String purchaseTicket );

    /**   
     * This callback function is executed when the IAPClientPaymentProvider gets information from the Nokia Store
     * back end indicating that the item's restoration is possible. 
     *
     * @param status  The status of the restoration flow.   
     * @param purchaseTicket  The purchase ticket in base64 encoded form. It is SHA1 (160 bit) hash. 
     *  Hash is calculated over the string, which is derived by concatenating the attribute values. 
     *  The attribute values are concatenated in order transactionId, transactionTime, productId, applicationId, accountId, imei and imsi.
     *
     */   
     void restorationCompleted( int status, String purchaseTicket );
    
    /**   
     * This callback function is executed when the request for products available for restoration is complete.
     *
     * The results returned by the list of IAPClientProductData objects will be valid only during the scope of the callback method.
     * The application will receive unpredictable results if it attempts to use the objects at any other time.
     *
     * @param status  The status of the getRestorableProducts() request.  
     * @param productDataList  The list of IAPClientProductData objects containing the meta data of the requested product.  
     *
     */     
     void restorableProductsReceived( int status, IAPClientProductData[] productDataList ); 
     
    /**   
     * This callback function is executed when the user and device data request is completed.
     *
     * The results returned by the IAPClientUserAndDeviceData object will be valid only during the scope of the callback method.
     * The application will receive unpredictable results if it attempts to use the object at any other time.
     *
     * @param status  The status of the getUserAndDeviceId() request.  
     * @param ud The IAPClientUserAndDeviceData class containing User and Device Data received from Nokia Store backend.
     *
     */ 
     void userAndDeviceDataReceived( int status, IAPClientUserAndDeviceData ud);    

}
/* End of interface IAPClientPaymentListener */
