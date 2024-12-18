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
package com.samsung.util;

public class SM
{
	private String destAddress;
	private String callbackAddress;
	private String data;

	public SM() {  }

	public SM(String dest, String callback, String textMessage) 
	{  
		setDestAddress(dest);
		setCallbackAddress(callback);
		setData(textMessage);
	}


	public String getCallbackAddress() { return callbackAddress; }

	public String getData() { return data; }

	public String getDestAddress() { return destAddress; }

	public void setCallbackAddress(java.lang.String address) { this.callbackAddress = address; }

	public void setData(java.lang.String textMessage) { this.data = textMessage; }

	public void setDestAddress(java.lang.String address) { this.destAddress = address; }
}
