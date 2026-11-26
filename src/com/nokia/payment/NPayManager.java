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

import org.recompile.mobile.Mobile;
import javax.microedition.midlet.MIDlet;

public class NPayManager 
{
	// This class tries to bypass purchases from the Nokia In-App Payment API

	NPayListener listener;

	public NPayManager(MIDlet midlet) throws NPayException 
	{ 
		if(midlet == null) { throw new NPayException("NPayManager received null MIDlet"); }
		Mobile.log(Mobile.LOG_INFO, NPayManager.class.getPackage().getName() + "." + NPayManager.class.getSimpleName() + ": " + "NPayManager init."); 
	}

	public void getProductData(String[] productIdList) throws NPayException
	{ 
		for(int i = 0; i < productIdList.length; i++)
		{
			listener.productDataReceived(new ProductData[] { new ProductData("0", "0.00", "$") } );
		}
	}

	public boolean isNPayAvailable() { return true; }

	public void launchNPaySetup() { }

	public void purchaseProduct(String productId) throws NPayException { purchaseProduct("0", productId);}

	public void purchaseProduct(String contentId, String productId) throws NPayException 
	{ 
		PurchaseData data = new PurchaseData(contentId, productId, PurchaseData.PURCHASE_SUCCESS, "RequestPurchase", "AllowStub");
		listener.purchaseCompleted(data);
	}

	public void setNPayListener(NPayListener listener) { this.listener = listener; }
}
