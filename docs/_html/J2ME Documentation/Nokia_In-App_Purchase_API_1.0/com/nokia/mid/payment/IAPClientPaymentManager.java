 /*======================================================================

 CLASS       : IAPClientPaymentManager.java
 SUMMARY     : Template Java source file for use in ThomasSCF
 COPYRIGHT   : (c) 2011 Nokia Corporation. All rights reserved.

  ========================================================================

 CCM DETAILS : %name:          IAPClientPaymentManager.java %
 %version:       1 %
 %date_modified: Fri Jun 01 18:22:55 2012 %
 
  ========================================================================

VERSION     : 19       Aug 8, 2011         Kathy Zhang
REASON      : KZ11081153738, javadoc update.
REFERENCE   : sd1iam3#69283
DESCRIPTION : Change comments per the javadoc publish request.
             
  ========================================================================

VERSION     : 18       Jun 8, 2011         Joe Xu
REASON      : Accept 360 ID - 1490274 test mode improvement
REFERENCE   : sd1iam3#74184
DESCRIPTION : update test mode for IAP  1) able to handle empty text file
              2) set default test type and mode if wrong text in the file
              also update restorable products function for the fail cases

  ========================================================================

VERSION     : 17       May 18, 2011         Joe Xu
REASON      : Accept 360 ID - 1490274
REFERENCE   : sd1iam3#72438
DESCRIPTION : Implement test mode for IAP

  ========================================================================
 
VERSION     : 16        May 16, 2011        Richard Navarro   
REASON      : Accept 360 ID - 1227926
REFERENCE   : sd1iam3#71602
DESCRIPTION : Merge versions 14 and 15, Add in changes for purchase to restore flows
 
  ========================================================================
 
VERSION     : 15        May 16, 2011        Guangjing Zhang       
REASON      : Initial version - Accept 360 ID - 1384755
REFERENCE   : sd1iam3#71755
DESCRIPTION : Merge versions 12.1.1 and 13

  ========================================================================
 
VERSION     : 13       06-May-2011         Jason Wang
REASON      : Accept 360 ID - 1384745
REFERENCE   : sd1iam3#71894
DESCRIPTION : File merge
 
  ========================================================================
 
VERSION     : 12.1.1        May 6, 2011        Guangjing Zhang       
REASON      : Initial version - Accept 360 ID - 1384755
REFERENCE   : sd1iam3#71755
DESCRIPTION : Add domain protection

  ========================================================================
 
VERSION     : 12          04-May-2011     Guangjing Zhang
REASON      : Accept 360 ID - 1338858
REFERENCE   : sd1iam3#67546
DESCRIPTION : Merge versions 10.2.1 and 10.1.1
 
  ========================================================================
 
VERSION     : 10.2.1          04-May-2011     Guangjing Zhang
REASON      : Accept 360 ID - 1338858
REFERENCE   : sd1iam3#67546
DESCRIPTION : Fix for the exception being thrown during NTH test when calling 
              getIAPClientPaymentManager 
 
  ========================================================================
VERSION     : 10.3.1       19-Apr-2011         Yufeng Wang
REASON      : Accept 360 ID - 1384745
REFERENCE   : sd1iam3#71894
DESCRIPTION : KNI implemenation for getRestorableProducts API.   
 
  ========================================================================

VERSION     : 10.1.1       Apr 25, 2011        Richard Navarro
REASON      : Accept 360 ID - 1227926
REFERENCE   : sd1iam3#70588
DESCRIPTION : Add in changes for restore product 
 
  ========================================================================

VERSION     : 10       Apr 04, 2011        Yufeng Wang
REASON      : Accept 360 ID - 1384734
REFERENCE   : sd1iam3#69702
DESCRIPTION : Cosmetics change based on the review comments

  ========================================================================

VERSION     : 9       Mar 31, 2011         Joe Xu
REASON      : Accept 360 ID - 1082785
REFERENCE   : sd1iam3#69253
DESCRIPTION : Uniform java api status code handling from OPE diff result status

  ========================================================================

VERSION     : 8       Mar 22, 2011         Joe Xu
REASON      : Accept 360 ID - 1082785
REFERENCE   : sd1iam3#67484
DESCRIPTION : Major revision of Improvements
              object exchanges with KNI, object created by KNI when there is data.
              new interval native function to read the object, and free grobal referenecs.
              java midlet state changes handling function to handle end key press.
              generated shared events between java and KNI.

  ========================================================================

VERSION     : 7          14-Mar-2011     Yufeng Wang
REASON      : Accept 360 ID - 1227607
REFERENCE   : sd1iam3#68162
DESCRIPTION : Read out Application ID from JAR package

VERSION     : 6          14-Mar-2011     Yufeng Wang
REASON      : Accept 360 ID - 1227607
REFERENCE   : sd1iam3#67426
DESCRIPTION : Java API(including KNI) implementation for getUserAndDeviceDataId.

VERSION     : 5.1        08-Mar-2011        Guangjing Zhang
REASON      : Initial version. Accept 360 ID: 1227661
REFERENCE   : sd1iam3#67496
DESCRIPTION : Implement getDRMResourceAsStream

  ========================================================================

VERSION     : 5       Mar 2, 2011         Joe Xu
REASON      : Accept 360 ID - 1082785
REFERENCE   : sd1iam3#65716
DESCRIPTION : Merge listener implementation 2.1.1 with new singleton class changes

  ========================================================================
VERSION     : 4       Feb 16, 2011         Sonia Sarkar
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#66028
DESCRIPTION : Update singleton comments

  ========================================================================

VERSION     : 3       Feb 14, 2011         Sonia Sarkar
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#65411
DESCRIPTION : Removed IAPClientPaymentException from all APIs, modified this class
              to be a singleton

  ========================================================================

VERSION     : 2.1.1        Feb 14, 2011        Joe Xu
REASON      : Accept 360 ID - 1082785
REFERENCE   : sd1iam3#63458
DESCRIPTION : Add Listener code handling native events from KNI
              add Java traces for integration test only, will remove the traces later.

  ========================================================================

VERSION     : 2        Feb 1, 2011        Sonia Sarkar
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#63909
DESCRIPTION : Updated comments, modified exception for getDRMResourceAsStream()

 ========================================================================

VERSION     : 1        Dec 8, 2010        Sonia Sarkar
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#61723
DESCRIPTION : API support for In Application Payment

 ======================================================================*/
package com.nokia.mid.payment;

import java.util.*;
import java.lang.*;
import java.io.*;
import com.nokia.mid.impl.isa.ui.EventConsumer;
import com.nokia.mid.impl.isa.ui.EventProducer;
import com.nokia.mid.impl.isa.ui.InitJALM;


/**
 * This is the main class for initiating and managing the purchase process for the purchaseable items in an application. Its methods
 * are asynchronous with responses received from the underlying implementation callbacks
 * All API methods except getProductData() will initiate the user authentication
 * by Single-Sign On (SSO).The IAPClientPaymentManager class handles a single API function call at a time. If a second request is
 * made while response is pending from a previous function call, a failure value is returned indicating that this request shall not
 * be processed and no callback can be expected for this request.
 *
 * <pre>
 * sample code snippet
 * package sample;
 *
 * import javax.microedition.midlet.*;
 * import javax.microedition.lcdui.*;
 * import com.nokia.mid.payment.*;
 * import java.io.*;
 *
 * public class SampleMIDlet extends MIDlet implements CommandListener, IAPClientPaymentListener
 * {
 *     private Display display;     // The display for this MIDlet
 *     private IAPClientPaymentManager manager;  //Singleton instance of IAPClientPaymentManager() class
 *
 *     public SampleMIDlet()
 *     {
 *         display = Display.getDisplay(this);
 *     }
 *
 *     public void startApp()
 *     {
 *         int status = 0;
 *
 *         try {
 *             // set IAPClientPaymentListener
 *             manager = IAPClientPaymentManager.getIAPClientPaymentManager();
 *             manager.setIAPClientPaymentListener(this);
 *
 *             // request metadata for product "game_level_3"
 *             status = manager.getProductData("123456");
 *             if (status != IAPClientPaymentManager.SUCCESS) {
 *                 // Do not expect a productDataReceived() callback with the requested metadata, handle the fail case
 *             }
 *         } catch (IAPClientPaymentException e) {
 *             // handle IAPClientPaymentException from getIAPClientPaymentManager()
 *         }
 *
 *         status = manager.purchaseProduct("123456", IAPClientPaymentManager.FORCED_AUTOMATIC_RESTORATION);
 *
 *         if(status != IAPClientPaymentManager.SUCCESS)
 *         {
 *             // Do not expect a purchaseCompleted() asynchronous callback, handle the fail case
 *         }
 *
 *         // restore product "game_level_5"
 *         status = manager.restoreProduct("112233", IAPClientPaymentManager.ONLY_IN_SILENT_AUTHENTICATION);
 *         if(status != IAPClientPaymentManager.SUCCESS)
 *         {
 *             // Do not expect a restoreCompleted() asynchronous callback, handle the fail case
 *         }
 *     }
 *
 *     public void pauseApp()
 *     {
 *     }
 *
 *     public void destroyApp(boolean unconditional)
 *     {
 *     }
 *
 *     public void commandAction(Command c, Displayable s)
 *     {
 *     }
 *
 *     public void productDataReceived(int status, IAPClientProductData pd)
 *     {
 *         if(status == OK)
 *         {
 *              String title = pd.getTitle();
 *              String price = pd.getPrice();
 *         }
 *
 *         //Update UI with information
 *     }
 *
 *     public void purchaseCompleted(int status, String purchaseTicket)
 *     {
 *         if(status == OK)
 *         {
 *              InputStream input = manager.getDRMResourceAsStream("/res/drm/data/ResourceId_123456");
 *              // Unlock game level 3, allow to be used
 *         }
 *     }
 *
 *     public  void restorableProductsReceived(int status, IAPClientProductData[] productDataList)
 *     {
 *         if(status == OK)
 *         {
 *              for (int i = 0; i < productDataList.length; i++) {
 *                  // Access each product data:productDataList[i] 
 *              }
 *         }
 *     }
 *
 *     public void restorationCompleted(int status, String purchaseTicket)
 *     {
 *         if(status == OK)
 *         {
 *              InputStream input = manager.getDRMResourceAsStream("/res/drm/data/ResourceId_112233");
 *              // Restore game level 5, allow to be used
 *         }
 *     }
 *
 *     public void userAndDeviceDataReceived(int status, IAPClientUserAndDeviceData ud)
 *     {
 *         if(status == OK)
 *         {
 *              // Access client user and device data
 *         }
 *     }
 * }
 *</pre>
 *
 */
public final class IAPClientPaymentManager {
    //  NOTE: do not remove the following commented brace.
    // native_const(IAP_EVT)
    // {
    private final static int GET_PRODUCT_INFO_RESP = 1;
    private final static int PURCHASE_PRODUCT_RESP = 2;
    private final static int RESTORATION_RESP = 3;
    private final static int RESTORABEL_PRODUCT_RESP = 4;
    private final static int USER_DEVICE_DATA_RESP = 5;
    private final static int GET_PRODUCT_INFO_ARRAY_RESP = 6;
    //  NOTE: do not remove the following commented brace.
    //
    // }

    /**
     * The operation is executed as a normal one. The user authentication is requested when necessary.
     */
    public static final int DEFAULT_AUTHENTICATION = 0;

    /**.
     * The operation is executed silently without the user authentication. If this is not possible, the operation is aborted
     * If the user must enter account credentials, for example an account password, while ONLY_IN_SILENT_AUTHENTICATION
     * is passed, the operation is aborted. Otherwise the operation continues regardless of whether
     * the user has to enter account credentials or not.
     */
    public static final int ONLY_IN_SILENT_AUTHENTICATION = 1;

    /**
     *  The purchase request does not restore the item.
     */
    public static final int NO_FORCED_RESTORATION = 0;

    /**
     *  The purchase request is automatically transformed to a restoration if item restoration is available.
     */
    public static final int FORCED_AUTOMATIC_RESTORATION = 1;

    //  NOTE: do not remove the following commented brace.
    // native_const(IAP_CODE)
    // {
    
    /**
     *  The operation is successful, expect an asynchronous callback at a later time when requested information is available.
     */
    public static final int SUCCESS = 1;

    /**
     *  The operation has failed, do not  expect an asynchronous callback later.
     */
    public static final int GENERAL_FAIL = -1;
    public static final int PENDING_REQUEST = -2;
    public static final int NULL_INPUT_PARAMETER = -3;
    public static final int KNI_INTERNAL_FAIL = -4;
    public static final int OUT_OF_MEMORY = -5;

    //  NOTE: do not remove the following commented brace.
    //
    // }
    
    //  NOTE: do not remove the following commented brace.
    // native_const(JAVA_IAP_TEST_TYPE)
    // {
    public static final int TEST_SERVER = 1;
    public static final int SIMULATION = 2;
    
    //  NOTE: do not remove the following commented brace.
    //
    // }
    
    //  NOTE: do not remove the following commented brace.
    // native_const(JAVA_IAP_TEST_MODE)
    // {
    public static final int PURCHASE = 101;
    public static final int RESTORE = 102;
    public static final int FAIL = 103;
    public static final int NORMAL = 104;
    
    //  NOTE: do not remove the following commented brace.
    //
    // }
    
    private static IAPClientPaymentListener listener;
    private static String appId = null;
    private static boolean testMode = false;
    private static final int TEXT_LINE_NUM = 2;
    private static String[] lineText = new String[TEXT_LINE_NUM];
    private static int test_type = TEST_SERVER;
    private static int mode_param = NORMAL;

    /**
     *  private reference to unique singleton instance of IAPClientPaymentManager
     */
    private static IAPClientPaymentManager singleton_instance = null;

    /**
     * private default constructor for IAPClientPaymentManager
     */
    private IAPClientPaymentManager() {
        try {
            InputStream res = getClass().getResourceAsStream("/IAP_VARIANTID.TXT");
            if (res == null) {
                res = getClass().getResourceAsStream("/Iap_variantid.txt");
            }
            if (res == null) {
                appId = null;
            } else {
                int chars = 0;
                StringBuffer sb = new StringBuffer();

                while ((chars = res.read()) != -1) {
                    sb.append((char) chars);
                }
                appId = new String(sb);
            }
        } catch (IOException e) {
            appId = null;
        }

        /* check if test mode */
        try {
            InputStream testFile = getClass().getResourceAsStream("/TEST_MODE.TXT");
            if (testFile == null) {
                testFile = getClass().getResourceAsStream("/TEST_MODE.txt");
            }
            if (testFile != null) {
                testMode = true;
		int readChar = testFile.read();
		if (readChar == -1) {
		    lineText[0] = "testserver";
		    lineText[1] = "testMode=normal";
		} else {
                    for (int i = 0; i < TEXT_LINE_NUM; i++) {
                        StringBuffer tempBuffer = new StringBuffer("");
                        while (readChar != '\n' && readChar != -1) {
                            /* for MS window text, ignore the CR */
                            if (readChar != '\r') {
                                tempBuffer.append((char)readChar);
                            }
                            readChar = testFile.read();
                        }
                        lineText[i] = new String(tempBuffer);
                        if (readChar == -1) {
                            break;
                        }
                    }
		}
            }
        } catch (IOException e) {
           lineText[0] = "testserver";
           lineText[1] = "testMode=normal"; 
        }
        
        /* process if it is test mode */
        if (testMode) {
            /* test mode type: 1=testserver; 2=simulatation(currently not supported in s40); */
            if (lineText[0].equalsIgnoreCase("testserver")) {
                test_type = TEST_SERVER;
	    } else if (lineText[0].equalsIgnoreCase("simulation")) {
                test_type = SIMULATION;
            } else {
                test_type = TEST_SERVER;
	    } 
            
            /* test mode:param 1-testMode=purchase; 2-testMode=restore; 3-testMode=fail; 4-testMode=normal; */
            if (lineText[1].equalsIgnoreCase("testMode=purchase")) {
                mode_param = PURCHASE;
            } else if (lineText[1].equalsIgnoreCase("testMode=restore")) {
                mode_param = RESTORE;
            } else if (lineText[1].equalsIgnoreCase("testMode=fail")) {
                mode_param = FAIL;
            } else if (lineText[1].equalsIgnoreCase("testMode=normal")) {
                mode_param = NORMAL;
            } else {
                mode_param = NORMAL;
	    }      
            
            nativeNotifyTestMode(test_type, mode_param);
        }

        /* register java midlet state change call back */
        nativeRegisterStateChangeCallback();
    }

    /**
     *  Retrieve the singleton IAPClientPaymentManager instance
     *  @return  The return is the instance of IAPClientPaymentManager singleton: The value is null if the singleton has already been initialized
     *  @throws  IAPClientPaymentException if the Application ID can't be read out from JAR package
     *  @throws  SecurityException if the caller does not have security permission to use In-App Purchase API
     */
    public static IAPClientPaymentManager getIAPClientPaymentManager() throws IAPClientPaymentException {
        if (singleton_instance == null) {
            singleton_instance = new IAPClientPaymentManager();
        }

        if (appId == null) {
            throw new IAPClientPaymentException("Application ID initialization fails");
        }

        if (testMode == false) {
            // Check current midlet domain and Throw SecurityException if the domain is not qualified.
            boolean validDomain = nativeCheckCurrentMidletDomain();
            if (validDomain == false) {
                // Add a wait to avoid assertion due to trying to resume an unsuspended thread that relates
                // to copying bg image if this function is called in atartApp.
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                throw new SecurityException("Permission Denied");
            }
        }

        return singleton_instance;
    }

    /**
     * Set up a listener for IAP related events from the observed class IAPClientPaymentManager.
     * @param iapListener of type IAPClientPaymentListener
     */
    public static void setIAPClientPaymentListener(IAPClientPaymentListener iapListener) {
        if (listener != null) {
            if (listener == iapListener) {
                return;
            }
        }

        listener = iapListener;
        InstallIapServerRespListener();

        return;
    }

    /**
     * Provides information about purchasable items for the publisher application for building a catalog list. The productDataReceived() callback returns a ProductData structure
     * for each of the productIDs.
     * The callback is sent only if the return value is SUCCESS.
     * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
     * @param productId  The productId of the purchaseable items. Publishers register sub-products(IAP) with Nokia store to receive product IDs
     * @return  The return is an integer value. The value is one of the following:
     *  <li> SUCCESS: The method call succeeded. IAP API sends the productDataReceived() callback to the publisher application. .
     *  <li> FAIL: The method call failed. IAP API does not send the productDataReceived() callback to the publisher application.
     */
    public int getProductData(String productId) {
        return nativeGetProductData(productId, appId);
    }
    
    /**
     * Provides information about purchasable items for the publisher application for building a catalog list. The productDataListReceived() callback returns ProductData structure
     * for a list of requested productIDs.
     * The callback is sent only if the return value is SUCCESS.
     * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
     * @param productIdList  The productIds of a list of purchaseable items. Publishers register sub-products(IAP) with Nokia store to receive product IDs
     * @return  The return is an integer value. The value is one of the following:
     *  <li> SUCCESS: The method call succeeded. IAP API sends the productDataListReceived() callback to the publisher application. .
     *  <li> FAIL: The method call failed. IAP API does not send the productDataListReceived() callback to the publisher application.
     */
    public int getProductData(String[] productIdList) {
        return nativeGetProductDataList(appId, productIdList);
    }    

    /**
     *<p>Initiates the purchase flow. If forceRestorationFlag is set and the item to be purchased is restorable, the purchase flow is automatically
     * transformed to a restoration flow. Only Nokia DRM encrypted items can be restored using IAPClient API.
     *
     * <p>The function call results in the  purchaseCompleted() callback during execution if the return value is positive:
     *    This provides the purchase result, when the Nokia Store back end has completed the purchase
     *    and the purchase data is available for the client application.
     * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
     *
     *
     * @param productId  The ID of the product to be purchased. Publishers register sub-products(IAP) with Nokia store to receive product IDs
     * @param forceRestorationFlag the flag for selecting forcedRestoration when available
     *        Possible values are one of the constants defined in this class, NO_FORCED_RESTORATION or FORCED_AUTOMATIC_RESTORATION
     * @return  The return is an integer value. The value is one of the following:
     *  <li> SUCCESS: The method call succeeded. IAP API sends the purchaseCompleted() callback to the publisher application.
     *  <li> FAIL: The method call failed. IAP API does not send the purchaseCompleted() callback to the publisher application.
     */
    public int purchaseProduct(String productId, int forceRestorationFlag) {
        return nativePurchaseProduct(productId, appId, forceRestorationFlag);
    }

    /**
     * Initiates the restoration flow. Only Nokia DRM encrypted items can be restored using IAPClient API.
     *
     * The function call results in the restorationCompleted() callback during the execution if the return value is positive
     *     This provides the result of the restoration, when it is completed by the Nokia Store back end
     *    and the restoration data is available for the client application.
     * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
     *
     *
     *
     * @param productId  The ID of the product to be restored.  Publishers register sub-products(IAP) with Nokia store to receive product IDs
     * @param authenticationMode  Defines whether product is being restored silently without asking the user authentication or not
     *        Possible values are one of the constants defined in this class, DEFAULT_AUTHENTICATION or ONLY_IN_SILENT_AUTHENTICATION
     * @return  The return is an integer value. The value is one of the following:
     *  <li> SUCCESS: The method call succeeded. IAP API sends the restorationCompleted() callback to the publisher application.
     *  <li> FAIL: The method call failed. IAP API does not send the restorationCompleted() callback to the publisher application.
     */
    public int restoreProduct(String productId, int authenticationMode) {
        return nativeRestoreProduct(productId, appId, authenticationMode);
    }

    /**
     * Provides a list of Nokia Store items related to the given application ID that are restorable via IAPClient API. The restorableProductsReceived() callback
     * function returns the list of restorable products.
     * The callback function is called only if the return value is positive.
     * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
     *
     *
     * @param authenticationMode  Defines whether restorable products are fetched silently without asking the user authentication or not
     * @return  The return is an integer value. The value is one of the following:
     *  <li> SUCCESS: The method call succeeded. IAP API sends the restorableProductsReceived() callback to the publisher application. .
     *  <li> FAIL: The method call failed. IAP API does not send the restorableProductsReceived() callback to the publisher application.
     */
    public int getRestorableProducts(int authenticationMode) {
        return nativeGetRestorableProducts(appId, authenticationMode); 
    }

    /**
     * Provides all necessary information about the current user and the device for the publisher service to restore non-Nokia DRM encrypted data
     * to the device. The userAndDeviceDataReceived() callback returns the data in hashed form. The callback is sent only if the return value is positive.
     * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
     *
     *
     * @param authenticationMode  Defines whether the user and the device data are fetched silently without asking user authentication or not
     *        Possible values are one of the constants defined in this class, DEFAULT_AUTHENTICATION or ONLY_IN_SILENT_AUTHENTICATION
     * @return  The return is an integer value. The value is one of the following:
     *  <li> SUCCESS: The method call succeeded. IAP API sends the userAndDeviceDataReceived() callback to the publisher application. .
     *  <li> FAIL: The method call failed. IAP API does not send the userAndDeviceDataReceived() callback to the publisher application.
     */
    public int getUserAndDeviceId(int authenticationMode) {
        return nativeGetUserAndDeviceId(authenticationMode);
    }

    /**
     * This provides the client application access to DRM encrypted resource files inside JAR.
     * eg. Possible location Jar-root/res/drm/data/level_1
     *                                            /level_2
     * This is used after receiving the PurchaseTicket from a successful
     * purchase flow or restoration flow.
     *
     * Finds a drm resource with a given name inside the application's
     * JAR file. This method returns
     * <code>null</code> if no resource with this name is found
     * in the application's JAR file.
     * <p>
     * The resource names can be represented in two
     * different formats: absolute or relative.
     * <p>
     * Absolute format:
     * <ul><code>/packagePathName/resourceName</code></ul>
     * <p>
     * Relative format:
     * <ul><code>resourceName</code></ul>
     * <p>
     * In the absolute format, the programmer provides a fully
     * qualified name that includes both the full path and the
     * name of the resource inside the JAR file.  In the path names,
     * the character "/" is used as the separator.
     * <p>
     * In the relative format, the programmer provides only
     * the name of the actual resource.  Relative names are
     * converted to absolute names by the system by prepending
     * the resource name with the fully qualified package name
     * of class upon which the <code>geDRMResourceAsStream</code>
     * method was called.
     *
     * @param name  name of the desired resource
     * @return      a <code>java.io.InputStream</code> object.
     */
    public java.io.InputStream getDRMResourceAsStream(String name) {
        try {
            return new DrmResourceInputStream(name);
        } catch (java.io.IOException x) {
            return null;
        }
    }

    private static class IapClientEventConsumer implements EventConsumer {
        private IapClientEventConsumer() {
        }

        protected final static void IapEventConsumerInitializer() {
            /** Singleton instance of the IapClientEventConsumer. */
            IapClientEventConsumer singletonInstance = new IapClientEventConsumer();

            EventProducer eventProducer = InitJALM.s_getEventProducer();

            eventProducer.attachEventConsumer(EventProducer.CATEGORY_IAP_API,
                singletonInstance);
        }

        public void consumeEvent(int category, int type, int param) {
            if (category != EventProducer.CATEGORY_IAP_API) {
                return;
            }

            IAPClientPaymentManager.processIapClientEvent(type, param);

            return;
        }
    }  
    
    private static void InstallIapServerRespListener() {
        IapClientEventConsumer.IapEventConsumerInitializer();
        return;
    }

    private static void processIapClientEvent(int type, int param) {
        if (listener == null) {
            // Do not process events.
            return;
        }

        switch (type) {
        case GET_PRODUCT_INFO_RESP:
            if (param == SUCCESS) {
                listener.productDataReceived(param, nativeReadProductDataObject());
            } else {
                listener.productDataReceived(param, null);
            }
            nativeFreeProductDataGlobalRef();
            break;
            
        case GET_PRODUCT_INFO_ARRAY_RESP:
            if (param == SUCCESS) {
                listener.productDataListReceived(param, nativeReadProductDataListObject());
            } else {
                listener.productDataListReceived(param, null);
            }
            nativeFreeProductDataArrayGlobalRef();
            break;            

        case PURCHASE_PRODUCT_RESP:
            if (param == SUCCESS) {
                listener.purchaseCompleted(param, nativeReadPurchaseTicketString());
            } else {
                listener.purchaseCompleted(param, null);
            }
            nativeFreePurchaseTicketGlobalRef();
            break;

        case USER_DEVICE_DATA_RESP:
            if (param == SUCCESS) {
                listener.userAndDeviceDataReceived(param, nativeReadUserAndDeviceDDataObject());
            } else {
                listener.userAndDeviceDataReceived(param, null);
            }
            nativeFreeUserAndDeviceDDataGlobalRef();
            break;

        case RESTORATION_RESP:
            if (param == SUCCESS) {
                listener.restorationCompleted(param, nativeReadPurchaseTicketString());
            } else {
                listener.restorationCompleted(param,  null);
            }
            nativeFreePurchaseTicketGlobalRef();
            break;
        
        case RESTORABEL_PRODUCT_RESP:
            if (param == SUCCESS) {
                listener.restorableProductsReceived(param, nativeReadRestorablelProductDataListObject());
            } else {
                listener.restorableProductsReceived(param, null);
            }
            nativeFreeProductDataArrayGlobalRef();
            break;

        default:
            break;
        }

        return;
    }
    
    private native int nativeGetProductData(String productId, String appId);
    
    private native int nativeGetProductDataList(String appId, String[] productIdList);

    private native int nativePurchaseProduct(String productId, String appId, int forceRestorationFlag);

    private native int nativeGetUserAndDeviceId(int authenticationMode);

    private native int nativeRestoreProduct(String productId, String appId, int authenticationMode);

    private native int nativeGetRestorableProducts(String appId, int authenticationMode);   
     
    private native static void nativeFreeProductDataGlobalRef();

    private native static void nativeFreePurchaseTicketGlobalRef();
    
    private native static void nativeFreeUserAndDeviceDDataGlobalRef();
    
    private native static void nativeFreeProductDataArrayGlobalRef();

    private native static void nativeRegisterStateChangeCallback();

    private native static boolean nativeCheckCurrentMidletDomain();
    
    private native static String nativeReadPurchaseTicketString();
    
    private native static IAPClientProductData nativeReadProductDataObject();
    
    private native static IAPClientProductData[] nativeReadProductDataListObject();    
    
    private native static IAPClientUserAndDeviceData nativeReadUserAndDeviceDDataObject();
    
    private native static IAPClientProductData[] nativeReadRestorablelProductDataListObject();

    private native void nativeNotifyTestMode(int test_type, int mode_param);
}
