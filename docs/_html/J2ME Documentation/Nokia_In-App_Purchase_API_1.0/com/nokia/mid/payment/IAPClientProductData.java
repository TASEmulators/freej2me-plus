/*======================================================================

 CLASS       : IAPClientProductData.java
 SUMMARY     : Template Java source file for use in ThomasSCF
 COPYRIGHT   : (c) 2011 Nokia Corporation. All rights reserved.

 ========================================================================

 CCM DETAILS : %name:          IAPClientProductData.java %
 %version:       1 %
 %date_modified: Fri Jun 01 18:22:58 2012 %
 
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
 * Class for holding purchaseable or restorable product specific data.
 * This class contains valid data if the status returned in the productDataReceived() callback has value OK.
 * This provides get methods to retrieve each of the product related information.
 */
public class IAPClientProductData 
{
      /**
       * The DRM protection type is other than Nokia DRM or it is not available. The publisher has the responsibility 
       * for the access control and restriction mechanisms of use. 
       */    
     public static final int OTHER_DRM = 0;
     
      /**
       * The item is DRM protected by Nokia. The item can be restored using IAP API. 
       */  
     public static final int NOKIA_DRM = 1;
       
     private String productId; 
     private String title; 
     private String shortDescription; 
     private String longDescription; 
     private String price; 
     private int    drmProtection; 

     /** This is a no specifier constructor for IAPClientProductData. 
      *  This has default access which means this may be instantiated within this class or within the package.
      *  This may not be instantiated by any inherited classes if any or from outside the package.
      */
     IAPClientProductData()  
     {
     }
          
     /**
      * Returns the productID of the purchaseable item.
      * 
      */   
      public String getProductId() 
      {
          return productId;
      }
      
     /**
      * Returns the name of the product in 16 characters.
      * 
      */    
     
      public String getTitle()
      {
          return title;
      }  
      
     /**
      * Returns the short description of the product in 30 characters.
      * 
      */       
      
      public String getShortDescription() 
      {
          return shortDescription;
      }

     /**
      * Returns the long description of the product in 500 characters.
      * 
      */       
         
      public String getLongDescription() 
      {
          return longDescription;
      }  

     /**
      * Returns the price of the product including currency, string to be used as is.
      * 
      */            
      public String getPrice() 
      {
          return price;
      }

     /**
      * Returns the type of Drm protection.
      * @return 
      *     OTHER_DRM - Indicates non-Nokia DRM or not available.
      *     NOKIA_DRM - The item is DRM protected by Nokia.
      */       
      public int getDrmProtection() 
      {
          return drmProtection;
      }
} /* End of class IAPClientProductData */


