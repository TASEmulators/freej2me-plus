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

public class ProductData 
{
	private String productId = "0";
	private String localizedPrice = "0.00";
	private String localCurrency = "USD";

	public ProductData(String productId, String localizedPrice, String localCurrency) 
	{ 
		this.productId = productId;
		this.localizedPrice = localizedPrice;
		this.localCurrency = localCurrency;
	}

	public String getCurrency() { return localCurrency; }

	public String getLocalizedPrice() { return localizedPrice; }

	public String getProductId() { return productId; }

	public boolean isValid() { return false; }
}
