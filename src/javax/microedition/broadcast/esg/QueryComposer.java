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
package javax.microedition.broadcast.esg;

import java.util.Date;

public class QueryComposer
{

    public static Query after(DateAttribute attribute, Date value)
    {
        if (attribute == null || value == null) { throw new NullPointerException("Arguments cannot be null"); }

        return null;
    }

    public static Query and(Query a, Query b)
    {
        if (a == null || b == null) { throw new NullPointerException("Arguments cannot be null"); }

        return null;
    }

    public static Query before(DateAttribute attribute, Date value)
    {
        if (attribute == null || value == null) { throw new NullPointerException("Arguments cannot be null"); }

        return null;
    }

    public static Query contains(StringAttribute attribute, String value)
    {
        if (attribute == null || value == null) { throw new NullPointerException("Arguments cannot be null"); }
        if (value.length() == 0) { throw new IllegalArgumentException("Value cannot be empty"); }

        return null;
    }

    public static Query currentProgram()
    {
        Date now = new Date();
        Query q1 = before(CommonMetadataSet.PROGRAM_START_TIME, now);
        Query q1e = equivalent(CommonMetadataSet.PROGRAM_START_TIME, now);
        Query q2 = after(CommonMetadataSet.PROGRAM_END_TIME, now);
        return and(or(q1, q1e), q2);
    }

    public static Query equivalent(Attribute attribute, Object value)
    {
        if (attribute == null || value == null) { throw new NullPointerException("Arguments cannot be null"); }

        return null;
    }

    public static Query equivalent(NumericAttribute attribute, double value)
    {
        if (attribute == null) { throw new NullPointerException("NumericAttribute cannot be null"); }

        return null;
    }

    public static Query greaterThan(NumericAttribute attribute, double value)
    {
        if (attribute == null) { throw new NullPointerException("NumericAttribute cannot be null"); }

        return null;
    }

    public static Query isTrue(BooleanAttribute attribute)
    {
        if (attribute == null) { throw new NullPointerException("BooleanAttribute cannot be null"); }

        return null;
    }

    public static Query lessThan(NumericAttribute attribute, double value)
    {
        if (attribute == null) { throw new NullPointerException("NumericAttribute cannot be null"); }

        return null;
    }

    public static Query not(Query a)
    {
        if (a == null) { throw new NullPointerException("Query cannot be null"); }

        return null;
    }

    public static Query or(Query a, Query b)
    {
        if (a == null || b == null) { throw new NullPointerException("Arguments cannot be null"); }

        return null;
    }
}
