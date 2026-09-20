/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILTY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package net.rim.device.api.system;

public interface RadioStatusListener extends RadioListener
{
	public static final int CAUSE_NOT_APPLICABLE = 28672;
	public static final int GMM_ATTACH_ACCEPT = 2050;
	public static final int GMM_ATTACH_REJECT = 2052;
	public static final int GMM_AUTH_CIPHER_REJECT = 2068;
	public static final int GMM_AUTH_CIPHER_REQUEST = 2066;
	public static final int GMM_GMM_INFORMATION = 2081;
	public static final int GMM_IDENTITY_REQUEST = 2069;
	public static final int GMM_MO_DETACH_ACCEPT = 2054;
	public static final int GMM_MT_DETACH_REQUEST = 2053;
	public static final int GMM_PTMSI_REALLOC_CMD = 2064;
	public static final int GMM_RA_UPDATE_ACCEPT = 2057;
	public static final int GMM_RA_UPDATE_REJECT = 2059;
	public static final int MM_ABORT = 1321;
	public static final int MM_AUTHENTICATION_REJECT = 1297;
	public static final int MM_AUTHENTICATION_REQUEST = 1298;
	public static final int MM_CM_SERVICE_ACCEPT = 1313;
	public static final int MM_CM_SERVICE_REJECT = 1314;
	public static final int MM_IDENTITY_REQUEST = 1304;
	public static final int MM_LOCATION_UPDATE_ACCEPT = 1282;
	public static final int MM_LOCATION_UPDATE_REJECT = 1284;
	public static final int MM_MM_INFORMATION = 1330;
	public static final int MM_MM_STATUS = 1329;
	public static final int MM_TMSI_REALLOC_CMD = 1307;
	public static final int PDP_CAUSE_AUTHENTICATION_FAILED = 29;
	public static final int PDP_CAUSE_INSUFFICIENT_RESOURCES = 26;
	public static final int PDP_CAUSE_LLC_FAILURE = 25;
	public static final int PDP_CAUSE_NETWORK_FAILURED = 38;
	public static final int PDP_CAUSE_NORMAL_DEACTIVATION = 36;
	public static final int PDP_CAUSE_NOT_SUPPORTED = 40;
	public static final int PDP_CAUSE_NSAPI_ALREADY_USED = 35;
	public static final int PDP_CAUSE_QOS_NOT_ACCEPTED = 37;
	public static final int PDP_CAUSE_REACTIVATION_REQUESTED = 39;
	public static final int PDP_CAUSE_REJECTED = 31;
	public static final int PDP_CAUSE_REJECTED_BY_GGSN = 30;
	public static final int PDP_CAUSE_SERVICE_NOT_SUBSCRIBED = 33;
	public static final int PDP_CAUSE_SERVICE_NOT_SUPPORTED = 32;
	public static final int PDP_CAUSE_SERVICE_OUT_OF_ORDER = 34;
	public static final int PDP_CAUSE_TFT_ALREADY_USED = 41;
	public static final int PDP_CAUSE_TFT_INVALID = 42;
	public static final int PDP_CAUSE_UNKNOWN = 0;
	public static final int PDP_CAUSE_UNKNOWN_APN = 27;
	public static final int PDP_CAUSE_UNKNOWN_PDP = 28;
	public static final int PDP_CAUSE_UNKNOWN_PDP_CONTEXT = 43;
	public static final int PDP_STATE_ACTIVE = 0;
	public static final int PDP_STATE_INACTIVE = 1;
	public static final int PDP_STATE_IP_ADDRESS_CHANGED = 3;
	public static final int PDP_STATE_REJECTED = 2;
	public static final int SM_DEACTIVATE_PDP_ACCEPT = 2631;
	public static final int SM_DEACTIVATE_PDP_REQUEST = 2630;
	public static final int SM_PDP_ACTIVATE_ACCEPT = 2626;
	public static final int SM_PDP_ACTIVATE_REJECT = 2627;

	void baseStationChange();

	void networkScanComplete(boolean success);

	void networkServiceChange(int networkId, int service);

	void networkStarted(int networkId, int service);

	void networkStateChange(int state);

	void pdpStateChange(int apn, int state, int cause);

	void radioTurnedOff();

	void signalLevel(int level);
}
