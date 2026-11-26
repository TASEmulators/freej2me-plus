/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package com.nokia.payment;

public class NPayException extends Exception 
{
	public static final int NO_ERROR_CODE = 0;
	public static final int ERR_PARAM_IS_NULL = 1;
	public static final int ERR_NIAP_LIBRARY_INIT = 2;
	public static final int ERR_NO_IAP_RESPONSE_LISTENER = 3;
	public static final int ERR_NPAY_INVOKE = 4;
	public static final int ERR_NO_IAP_MIDLET_AUTHORITY = 5;
	public static final int ERR_IAP_MIDLET_AUTHORITY_NOT_TRUSTED = 6;
	public static final int ERR_PARAM_IS_EMPTY = 7;

	int code = NO_ERROR_CODE;

	public NPayException(String message) { super(message); }

	public NPayException(String message, int code) { super(message); this.code = code; }

	public int getErrorCode() { return code; }
}
