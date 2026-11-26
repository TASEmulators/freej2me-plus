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

public class PurchaseData 
{
	public static final int PURCHASE_FAILED = 1;
	public static final int PURCHASE_RESTORE_SUCCESS = 2;
	public static final int PURCHASE_SUCCESS = 0;

	int status;
	String contentId, productId, requestMessage, purchaseTicket;
	
	public PurchaseData(String contentId, String productId, int status, String requestMessage, String purchaseTicketInformation) 
	{
		this.contentId = contentId;
		this.productId = productId;
		this.status = status;
		this.requestMessage = requestMessage;
		this.purchaseTicket = purchaseTicketInformation;
	}

	public String getContentId() { return contentId; }

	public String getMessage() { return requestMessage; }

	public String getProductId() { return productId; }

	public String getPurchaseTicket() { return purchaseTicket; }

	public int getStatus() { return status; }
}
