package com.nokia.mid.location;

import javax.microedition.location.*;
import com.nokia.mid.impl.policy.*;

public final class LocationUtil
    {
    private LocationUtil()
        {
        }

    /**
     *  Get a LocationProvider matching defined location methods.
     *
     *  Preferred methods are defined by using definitions in
     *  javax.microedition.location.Location.
     *
     *  Possible methods:<ul>
     *    <li>(MTA_ASSISTED | MTE_CELLID | MTE_SHORTRANGE | MTY_NETWORKBASED) - Online CellID and/or WLAN</li>
     *    <li>(MTA_ASSISTED | MTE_SATELLTITE | MTY_TERMINALBASED) - Assisted GPS</li>
     *    <li>(MTA_UNASSISTED | MTE_SATELLITE | MTY_TERMINALBASED) - Standalone GPS</li>
     *    <li>(MTA_UNASSISTED | MTE_CELLID | MTY_TERMINALBASED) - Offline CellID</li>
     *</ul>
     *  @param preferredMethods
     *    The preferred location methods, if null behaves as LocationProvider.getInstance(null).
     *  @param parameters
     *    Reserved for future bearer selection.
     *
     *  @throws SecurityException, LocationException
     *
     *  @return
     *    A LocationProvider matching the preferred location methods and/or platform specific parameters.
     *    Null if a LocationProvider that meets the defined conditions can't be found but there are other available or
     *    temporarily unavailable providers that do not support the defined conditions.
     *
     */
    public static LocationProvider getLocationProvider(int[] preferredMethods,
                                                       java.lang.String parameters) throws SecurityException, LocationException
        {
        return com.nokia.mid.impl.isa.location.LocationProviderDefault.getProvider(preferredMethods, parameters);
        }
    }
