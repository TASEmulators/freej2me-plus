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
package javax.microedition.sensor;

import org.recompile.mobile.Mobile;

public final class LimitCondition implements Condition 
{
    double limit;
    String operator;

    LimitCondition(double limit, String operator) 
    {
        Mobile.log(Mobile.LOG_WARNING, LimitCondition.class.getPackage().getName() + "." + LimitCondition.class.getSimpleName() + ": " + "Created new LimitCondition.");
        this.limit = limit;
        this.operator = operator;
    }

	public final double getLimit() { return limit; }

	public final String getOperator() { return operator; }

	@Override
	public boolean isMet(double value) { return false; }

	@Override
	public boolean isMet(Object value) { return false;}
}