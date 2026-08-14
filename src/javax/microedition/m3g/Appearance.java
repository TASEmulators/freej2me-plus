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
package javax.microedition.m3g;

public class Appearance extends Object3D
{

	private int layer;
	private CompositingMode compositingMode;
	private Fog fog;
	private PolygonMode polygonMode;
	private Material material;
	private Texture2D[] textures;

	public Appearance()
	{
		this.layer = 0;
		this.polygonMode = null;
		this.compositingMode = null;
		this.textures = new Texture2D[Graphics3D.NUM_TEXTURE_UNITS];
		this.material = null;
		this.fog = null;
	}

	protected Object3D duplicateImpl()
	{
		Appearance copy = (Appearance) super.duplicateImpl();

		copy.layer = this.layer;

		copy.setCompositingMode(this.compositingMode);
		copy.setFog(this.fog);
		copy.setMaterial(this.material);
		copy.setPolygonMode(this.polygonMode);

		copy.textures = new Texture2D[this.textures.length];
		for (int i = 0; i < this.textures.length; i++)
		{
			copy.setTexture(i, this.textures[i]);
		}

		return copy;
	}

	public void setLayer(int layer)
	{
		if (layer < -63 || layer > 63)
		{
			throw new IllegalArgumentException("Layer must be in range of [-63, 63]");
		}
		this.layer = layer;
	}

	public int getLayer() { return layer; }

	public void setFog(Fog fog)
	{
		this.removeReference(this.fog);
		this.fog = fog;
		this.addReference(this.fog);
	}

	public Fog getFog() { return fog; }

	public void setPolygonMode(PolygonMode polygonMode)
	{
		this.removeReference(this.polygonMode);
		this.polygonMode = polygonMode;
		this.addReference(this.polygonMode);
	}

	public PolygonMode getPolygonMode() { return polygonMode; }

	public void setMaterial(Material material)
	{
		this.removeReference(this.material);
		this.material = material;
		this.addReference(this.material);
	}

	public Material getMaterial() { return material; }

	public void setCompositingMode(CompositingMode comp)
	{
		this.removeReference(this.compositingMode);
		this.compositingMode = comp;
		this.addReference(this.compositingMode);
	}

	public CompositingMode getCompositingMode() { return this.compositingMode; }

	public void setTexture(int index, Texture2D texture)
	{
		if (index < 0 || index >= textures.length)
		{
			throw new IndexOutOfBoundsException("index must be in [0," + textures.length + "]");
		}

		this.removeReference(textures[index]);
		textures[index] = texture;
		this.addReference(textures[index]);
	}

	public Texture2D getTexture(int index)
	{
		if (index < 0 || index >= textures.length)
		{
			throw new IndexOutOfBoundsException("index must be in [0," + textures.length + "]");
		}

		return textures[index];
	}
}
