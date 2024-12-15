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

public class RangeCondition implements Condition
{
    private double lowerLimit;
    private String lowerOp;
    private double upperLimit;
    private String upperOp;

    public RangeCondition(double lowerLimit, String lowerOp, double upperLimit, String upperOp) 
    {
        Mobile.log(Mobile.LOG_WARNING, RangeCondition.class.getPackage().getName() + "." + RangeCondition.class.getSimpleName() + ": " + "Created new RangeCondition.");
        this.lowerLimit = lowerLimit;
        this.lowerOp = lowerOp;
        this.upperLimit = upperLimit;
        this.upperOp = upperOp;
    }

    public double getLowerLimit() { return lowerLimit; }

    public String getLowerOp() { return lowerOp; }

    public double getUpperLimit() { return upperLimit; }

    public String getUpperOp() { return upperOp; }

    @Override
    public boolean isMet(double doubleValue) { return false; }

    @Override
    public boolean isMet(Object value) { return false; }
}