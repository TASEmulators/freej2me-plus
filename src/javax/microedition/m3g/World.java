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

public class World extends Group
{

	private Camera camera;
	private Background background;

	public World() { }

	protected Object3D duplicateImpl()
	{
		World copy = (World) super.duplicateImpl();

		/*
		 * The camera and background fields were shallow-copied by clone().
		 * Per JSR-184, if the active camera is part of the duplicated set of
		 * nodes, the duplicate must reference the corresponding duplicated
		 * camera instead; any other references are left as they are.
		 */
		Node copyCamera = (this.camera != null) ? Node.matchingNode(this, this.camera, copy) : null;
		copy.setActiveCamera((copyCamera instanceof Camera) ? (Camera) copyCamera : this.camera);
		copy.setBackground(this.background);

		return copy;
	}

	public Camera getActiveCamera() { return camera; }

	public void setActiveCamera(Camera camera)
	{
		removeReference(this.camera);
		this.camera = camera;
		addReference(this.camera);
	}

	public Background getBackground() { return background; }

	public void setBackground(Background background)
	{
		removeReference(this.background);
		this.background = background;
		addReference(this.background);
	}

}
