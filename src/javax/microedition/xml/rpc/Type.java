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
package javax.microedition.xml.rpc;

public class Type
{
    public static final Type BOOLEAN = new Type(0);
    public static final Type BYTE = new Type(1);
    public static final Type SHORT = new Type(2);
    public static final Type INT = new Type(3);
    public static final Type LONG = new Type(4);
    public static final Type FLOAT = new Type(5);
    public static final Type DOUBLE = new Type(6);
    public static final Type STRING = new Type(7);

    public final int value;

    protected Type() { value = 0; }

    private Type(int value)
    {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) { return true; }
        if (!(obj instanceof Type)) { return false; }
        Type type = (Type) obj;

        return this.value == type.value;
    }

    @Override
    public int hashCode() { return value; }

    @Override
    public String toString()
    {
        switch (value)
        {
            case 0: return "BOOLEAN";
            case 1: return "BYTE";
            case 2: return "SHORT";
            case 3: return "INT";
            case 4: return "LONG";
            case 5: return "FLOAT";
            case 6: return "DOUBLE";
            case 7: return "STRING";
            default: return "UNKNOWN";
        }
    }
}
