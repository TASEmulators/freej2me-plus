/*======================================================================

 CLASS       : IAPClientUserAndDeviceData.java
 SUMMARY     : Template Java source file for use in ThomasSCF
 COPYRIGHT   : (c) 2011 Nokia Corporation. All rights reserved.

 ========================================================================

 CCM DETAILS : %name:          IAPClientUserAndDeviceData.java %
 %version:       1 %
 %date_modified: Fri Jun 01 18:23:01 2012 %
 
  ========================================================================

VERSION     : 2        Feb 1, 2011        Sonia Sarkar       
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#63909
DESCRIPTION : Updated comments

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
 
 /**
 * Class for holding User and Device Data related information.
 * This class contains valid data if the status returned in the userAndDeviceDataReceived() callback has value OK.
 * This provides get methods to retrieve each of the  User and Device data related information.
 */
 
public class IAPClientUserAndDeviceData
{
     private String account; 
     private String imei; 
     private String imsi; 
     private String country; 
     private String language; 
     private String deviceModel; 

     /** This is a no specifier constructor for IAPClientUserAndDeviceData.
      *  This has default access which means this may be instantiated within this class or within the package.
      *  This may not be instantiated by any inherited classes if any or from outside the package.
      */
     IAPClientUserAndDeviceData()  
     {
     }
     
     /**
      * Returns the Nokia User Account information in hashed form.
      * 
      */   
      public String getAccount() 
      {
          return account;
      }
      
     /**
      * Returns the device IMEI code in hashed form.
      * 
      */   
      public String getImei() 
      {
          return imei;
      }
      
     /**
      * Returns the subscriber's IMSI code in hashed form.
      * 
      */   
      public String getImsi() 
      {
          return imsi;
      } 
                 
     /**
      * Returns the home network country according to the SIM card in the device.
      * 
      */   
      public String getCountry() 
      {
          return country;
      }      

     /**
      * Returns the device language according to the device settings.
      * 
      */   
      public String getLanguage() 
      {
          return language; 
      }
      
     /**
      * Returns the name of the device in use.
      * 
      */   
      public String getDeviceModel() 
      {
          return deviceModel;
      }
         
}  

/* End of class IAPClientUserAndDeviceData */

