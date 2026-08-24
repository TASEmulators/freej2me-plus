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

public class Group extends Node
{
	public Node firstChild = null;
	public int numNonCullables = 0, numRenderables = 0;

	public Group() { }

	protected Object3D duplicateImpl()
	{
		Group copy = (Group) super.duplicateImpl();
		copy.firstChild = null;

		// We must Duplicate each child in the circular doubly-linked list
		if (this.firstChild != null)
		{
			Node curr = this.firstChild;
			do
			{
				Node childCopy = (Node) curr.duplicateImpl();
				copy.addChild(childCopy);
				curr = curr.right;
			}
			while (curr != this.firstChild);
		}

		return copy;
	}

	public void addChild(Node child)
	{
		if (child == null) { throw new NullPointerException("child cannot be null"); }
		if (child == this) { throw new IllegalArgumentException("cannot add self as child"); }
		if (child.getParent() != null) { throw new IllegalArgumentException("child already has parent"); }
		if (isAncestor(child)) { throw new IllegalArgumentException("Cannot add an ancestor as a child"); }

		if (firstChild == null)
		{
			firstChild = child;
			child.left = child;
			child.right = child;
		}
		else
		{
			Node lastChild = firstChild.left;

			lastChild.right = child;
			child.left = lastChild;

			child.right = firstChild;
			firstChild.left = child;
		}

		child.setParent(this);
		addReference(child);
	}

	public Node getChild(int idx)
	{
		if (idx < 0 || idx >= getChildCount())
			{ throw new IndexOutOfBoundsException("Negative child index"); }

		if (firstChild == null)
		{
			throw new IndexOutOfBoundsException("Group has no children");
		}

		Node n = firstChild;
		int count = 0;
		do
		{
			if (count == idx)
			{
				return n;
			}
			count++;
			n = n.right;
		}
		while (n != firstChild);

		throw new IndexOutOfBoundsException("Index " + idx + " out of bounds (child count is: " + count + ")");
	}

	public int getChildCount()
	{
		if (firstChild == null)
		{
			return 0;
		}

		int count = 0;
		Node child = firstChild;
		do
		{
			count++;
			child = child.right;
		}
		while (child != firstChild);

		return count;
	}

	public void removeChild(Node child)
	{
		if (child != null && firstChild != null)
		{
			Node n = firstChild;
			do
			{
				if (n == child)
				{
					if (n.right == n) // Only child in the list
					{
						firstChild = null;
					}
					else
					{
						n.right.left = n.left;
						n.left.right = n.right;

						if (firstChild == n)
						{
							firstChild = n.right;
						}
					}

					n.left = null;
					n.right = null;
					n.setParent(null);
					removeReference(child);
					return;
				}
				n = n.right;
			}
			while (n != firstChild);
		}
	}

	@Override
	boolean doAlign(Node ref)
	{
		if (!super.doAlign(ref))
		{
			return false;
		}

		Node child = firstChild;
		if (child != null)
		{
			do
			{
				if (!child.doAlign(ref))
				{
					return false;
				}
				child = child.right;
			}
			while (child != firstChild);
		}
		return true;
	}

	public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri)
	{
		if (camera == null) { throw new NullPointerException("Camera cannot be null"); }

        Transform inverseProjection = new Transform();
        camera.getProjection(inverseProjection);
        inverseProjection.invert();
        float[] cameraPoints = {
                2.0f*x - 1.0f, 1.0f - 2.0f*y, -1.0f, 1.0f,
                2.0f*x - 1.0f, 1.0f - 2.0f*y,  1.0f, 1.0f
        };
        inverseProjection.transform(cameraPoints);
        for (int offset = 0; offset <= 4; offset += 4)
        {
            float inverseW = 1.0f / cameraPoints[offset + 3];
            cameraPoints[offset] *= inverseW;
            cameraPoints[offset + 1] *= inverseW;
            cameraPoints[offset + 2] *= inverseW;
            cameraPoints[offset + 3] = 1.0f;
        }

        Transform cameraToGroup = new Transform();
        if (!camera.getTransformTo(this, cameraToGroup))
        { throw new IllegalStateException("Camera and Group are not in the same scene graph"); }
        float[] groupPoints = (float[]) cameraPoints.clone();
        cameraToGroup.transform(groupPoints);
        float[] ray = {
                groupPoints[0], groupPoints[1], groupPoints[2],
                groupPoints[4] - groupPoints[0],
                groupPoints[5] - groupPoints[1],
                groupPoints[6] - groupPoints[2]
        };

        PickResult result = pickInternal(scope, ray, camera, cameraPoints, true);
        if (result.node == null) { return false; }
        if (ri != null)
        { ri.set(result.node, result.distance, result.submesh, ray, result.normal, result.texS, result.texT); }
        return true;
	}

	public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri)
	{
		if (dx == 0.0f && dy == 0.0f && dz == 0.0f)
			{ throw new IllegalArgumentException("Ray direction vector cannot be zero"); }
        float[] ray = { ox, oy, oz, dx, dy, dz };
        PickResult result = pickInternal(scope, ray, null, null, false);
        if (result.node == null) { return false; }
        if (ri != null)
        { ri.set(result.node, result.distance, result.submesh, ray, result.normal, result.texS, result.texT); }
        return true;
    }

    private PickResult pickInternal(int scope, float[] ray, Camera camera,
                                    float[] cameraPoints, boolean pickSprites)
    {
        PickResult result = new PickResult();
        /* JSR-184 states that ancestors of the Group on which pick is invoked are
         * ignored, while picking disable remains inherited within this subtree. */
        traverseForPick(this, this.isPickingEnabled(), scope, ray, camera,
                cameraPoints, pickSprites, result);
        return result;
    }

    private void traverseForPick(Node node, boolean parentPicking, int scope, float[] ray,
                                 Camera camera, float[] cameraPoints, boolean pickSprites, PickResult result)
    {
        boolean picking = parentPicking && node.isPickingEnabled();
        if (!picking) { return; }

        if (node instanceof Mesh)
        {
            pickMesh((Mesh) node, scope, ray, result);
            if (node instanceof SkinnedMesh)
            {
                traverseForPick(((SkinnedMesh) node).getSkeleton(), picking, scope,
                        ray, camera, cameraPoints, pickSprites, result);
            }
        }
        else if (pickSprites && node instanceof Sprite3D)
        {
            pickSprite((Sprite3D) node, scope, camera, cameraPoints, result);
        }
        else if (node instanceof Group)
        {
            Node child = ((Group) node).firstChild;
            if (child != null)
            {
                do
                {
                    traverseForPick(child, picking, scope, ray, camera,
                            cameraPoints, pickSprites, result);
                    child = child.right;
                }
                while (child != ((Group) node).firstChild);
            }
        }
    }

    private void pickMesh(Mesh mesh, int scope, float[] groupRay, PickResult result)
    {
        if ((scope & mesh.getScope()) == 0) { return; }
        boolean hasEnabledSubmesh = false;
        for (int i = 0; i < mesh.getSubmeshCount(); i++)
        { if (mesh.getAppearance(i) != null) { hasEnabledSubmesh = true; break; } }
        if (!hasEnabledSubmesh) { return; }
        VertexBuffer vertices = mesh.getVertexBuffer();
        float[] scaleBias = new float[4];
        VertexArray positions = vertices.getPositions(scaleBias);
        if (positions == null) { throw new IllegalStateException("Pickable Mesh has no positions"); }
        float[] localPositions = readVertexArray(positions, false);
        for (int vertex = 0; vertex < positions.getVertexCount(); vertex++)
        {
            int offset = 3 * vertex;
            localPositions[offset] = localPositions[offset] * scaleBias[0] + scaleBias[1];
            localPositions[offset + 1] = localPositions[offset + 1] * scaleBias[0] + scaleBias[2];
            localPositions[offset + 2] = localPositions[offset + 2] * scaleBias[0] + scaleBias[3];
        }

        Transform groupToMesh = new Transform();
        if (!this.getTransformTo(mesh, groupToMesh))
        { throw new ArithmeticException("Mesh transform cannot be computed"); }
        float[] localRay4 = {
                groupRay[0], groupRay[1], groupRay[2], 1.0f,
                groupRay[3], groupRay[4], groupRay[5], 0.0f
        };
        groupToMesh.transform(localRay4);

        for (int submesh = 0; submesh < mesh.getSubmeshCount(); submesh++)
        {
            Appearance appearance = mesh.getAppearance(submesh);
            if (appearance == null) { continue; }
            IndexBuffer buffer = mesh.getIndexBuffer(submesh);
            int[] indices = buffer.getIndexArray();
            if (indices == null || indices.length != buffer.getIndexCount() || indices.length % 3 != 0)
            { throw new IllegalStateException("Invalid triangle index buffer"); }
            PolygonMode polygonMode = appearance.getPolygonMode();
            int culling = polygonMode != null ? polygonMode.getCulling() : PolygonMode.CULL_BACK;
            int winding = polygonMode != null ? polygonMode.getWinding() : PolygonMode.WINDING_CCW;

            for (int triangle = 0; triangle < indices.length; triangle += 3)
            {
                int ia = indices[triangle], ib = indices[triangle + 1], ic = indices[triangle + 2];
                if (ia < 0 || ib < 0 || ic < 0 || ia >= positions.getVertexCount() ||
                        ib >= positions.getVertexCount() || ic >= positions.getVertexCount())
                { throw new IllegalStateException("Triangle index exceeds VertexBuffer"); }
                intersectTriangle(mesh, vertices, appearance, submesh, winding, culling,
                        localRay4, localPositions, ia, ib, ic, result);
            }
        }
    }

    private static void intersectTriangle(Mesh mesh, VertexBuffer vertices, Appearance appearance,
                                          int submesh, int winding, int culling, float[] ray, float[] positions,
                                          int ia, int ib, int ic, PickResult result)
    {
        int a = 3*ia, b = 3*ib, c = 3*ic;
        double e1x = positions[b] - positions[a], e1y = positions[b+1] - positions[a+1];
        double e1z = positions[b+2] - positions[a+2];
        double e2x = positions[c] - positions[a], e2y = positions[c+1] - positions[a+1];
        double e2z = positions[c+2] - positions[a+2];
        double px = ray[5]*e2z - ray[6]*e2y;
        double py = ray[6]*e2x - ray[4]*e2z;
        double pz = ray[4]*e2y - ray[5]*e2x;
        double determinant = e1x*px + e1y*py + e1z*pz;
        if (determinant == 0.0) { return; }

        /* JSR-184 states that picking applies each submesh's winding and culling
         * modes to facing as observed along the pick ray. */
        boolean front = determinant > 0.0;
        if (winding == PolygonMode.WINDING_CW) { front = !front; }
        if ((culling == PolygonMode.CULL_BACK && !front) ||
                (culling == PolygonMode.CULL_FRONT && front)) { return; }

        double tx = ray[0] - positions[a], ty = ray[1] - positions[a+1];
        double tz = ray[2] - positions[a+2];
        double inverseDeterminant = 1.0 / determinant;
        double u = (tx*px + ty*py + tz*pz) * inverseDeterminant;
        if (u < 0.0 || u > 1.0) { return; }
        double qx = ty*e1z - tz*e1y;
        double qy = tz*e1x - tx*e1z;
        double qz = tx*e1y - ty*e1x;
        double v = (ray[4]*qx + ray[5]*qy + ray[6]*qz) * inverseDeterminant;
        if (v < 0.0 || u + v > 1.0) { return; }
        double distance = (e2x*qx + e2y*qy + e2z*qz) * inverseDeterminant;
        if (distance < 0.0 || distance >= result.distance) { return; }

        float wa = (float) (1.0 - u - v), wb = (float) u, wc = (float) v;
        result.node = mesh;
        result.distance = (float) distance;
        result.submesh = submesh;
        computeNormal(vertices, ia, ib, ic, wa, wb, wc, positions, result.normal);
        computeTextureCoordinates(vertices, appearance, ia, ib, ic, wa, wb, wc,
                result.texS, result.texT);
    }

    private static void computeNormal(VertexBuffer vertices, int ia, int ib, int ic,
                                      float wa, float wb, float wc, float[] positions, float[] normal)
    {
        VertexArray normals = vertices.getNormals();
        if (normals != null)
        {
            float[] values = readVertexArray(normals, false);
            normal[0] = wa*values[3*ia] + wb*values[3*ib] + wc*values[3*ic];
            normal[1] = wa*values[3*ia+1] + wb*values[3*ib+1] + wc*values[3*ic+1];
            normal[2] = wa*values[3*ia+2] + wb*values[3*ib+2] + wc*values[3*ic+2];
        }
        else
        {
            int a = 3*ia, b = 3*ib, c = 3*ic;
            float e1x = positions[b]-positions[a], e1y = positions[b+1]-positions[a+1];
            float e1z = positions[b+2]-positions[a+2];
            float e2x = positions[c]-positions[a], e2y = positions[c+1]-positions[a+1];
            float e2z = positions[c+2]-positions[a+2];
            normal[0] = e1y*e2z - e1z*e2y;
            normal[1] = e1z*e2x - e1x*e2z;
            normal[2] = e1x*e2y - e1y*e2x;
        }
        float length = M3GMath.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (length != 0.0f)
        {
            normal[0] /= length; normal[1] /= length; normal[2] /= length;
        }
    }

    private static void computeTextureCoordinates(VertexBuffer vertices, Appearance appearance,
                                                  int ia, int ib, int ic, float wa, float wb, float wc, float[] texS, float[] texT)
    {
        for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
        {
            texS[unit] = texT[unit] = 0.0f;
            Texture2D texture = appearance.getTexture(unit);
            float[] scaleBias = new float[4];
            VertexArray coords = vertices.getTexCoords(unit, scaleBias);
            if (texture == null || coords == null) { continue; }
            float[] values = readVertexArray(coords, false);
            int components = coords.getComponentCount();
            float[] coordinate = { 0.0f, 0.0f, 0.0f, 1.0f };
            for (int component = 0; component < components; component++)
            {
                coordinate[component] = scaleBias[0] *
                        (wa*values[components*ia + component] +
                                wb*values[components*ib + component] +
                                wc*values[components*ic + component]) + scaleBias[component + 1];
            }
            /* JSR-184 returns texture coordinates after texture transformation and
             * homogeneous projection, but before wrapping or clamping. */
            Transform textureTransform = new Transform();
            texture.getCompositeTransform(textureTransform);
            textureTransform.transform(coordinate);
            if (coordinate[3] != 0.0f)
            {
                texS[unit] = coordinate[0] / coordinate[3];
                texT[unit] = coordinate[1] / coordinate[3];
            }
        }
    }

    private void pickSprite(Sprite3D sprite, int scope, Camera camera,
                            float[] cameraPoints, PickResult result)
    {
        if (!sprite.isScaled() || sprite.getAppearance() == null ||
                (scope & sprite.getScope()) == 0) { return; }
        Transform spriteToCamera = new Transform();
        if (!sprite.getTransformTo(camera, spriteToCamera))
        { throw new ArithmeticException("Sprite transform cannot be computed"); }
        float[] points = { 0,0,0,1, 0.5f,0,0,1, 0,0.5f,0,1 };
        spriteToCamera.transform(points);
        float cx = points[0]/points[3], cy = points[1]/points[3], cz = points[2]/points[3];
        float x1 = points[4]/points[7], y1 = points[5]/points[7], z1 = points[6]/points[7];
        float x2 = points[8]/points[11], y2 = points[9]/points[11], z2 = points[10]/points[11];
        float halfWidth = M3GMath.sqrt((x1-cx)*(x1-cx) + (y1-cy)*(y1-cy) + (z1-cz)*(z1-cz));
        float halfHeight = M3GMath.sqrt((x2-cx)*(x2-cx) + (y2-cy)*(y2-cy) + (z2-cz)*(z2-cz));
        float rayOZ = cameraPoints[2], rayDZ = cameraPoints[6] - cameraPoints[2];
        if (rayDZ == 0.0f || halfWidth == 0.0f || halfHeight == 0.0f) { return; }
        float distance = (cz - rayOZ) / rayDZ;
        if (distance < 0.0f || distance >= result.distance) { return; }
        float hitX = cameraPoints[0] + distance * (cameraPoints[4] - cameraPoints[0]);
        float hitY = cameraPoints[1] + distance * (cameraPoints[5] - cameraPoints[1]);
        if (hitX < cx-halfWidth || hitX > cx+halfWidth ||
                hitY < cy-halfHeight || hitY > cy+halfHeight) { return; }

        result.node = sprite;
        result.distance = distance;
        result.submesh = 0;
        result.normal[0] = result.normal[1] = 0.0f; result.normal[2] = 1.0f;
        for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
        { result.texS[unit] = result.texT[unit] = 0.0f; }
        // JSR-184 states that sprite pick coordinates span [0,1] and ignore cropping.
        result.texS[0] = (hitX - (cx-halfWidth)) / (2.0f*halfWidth);
        result.texT[0] = ((cy+halfHeight) - hitY) / (2.0f*halfHeight);
    }

    private static float[] readVertexArray(VertexArray array, boolean unsigned)
    {
        int vertices = array.getVertexCount();
        int elements = vertices * array.getComponentCount();
        float[] result = new float[elements];
        if (array.getComponentType() == 1)
        {
            byte[] values = new byte[elements];
            array.get(0, vertices, values);
            for (int i = 0; i < elements; i++)
            { result[i] = unsigned ? Byte.toUnsignedInt(values[i]) : values[i]; }
        }
        else
        {
            short[] values = new short[elements];
            array.get(0, vertices, values);
            for (int i = 0; i < elements; i++) { result[i] = values[i]; }
        }
        return result;
    }

    private static class PickResult {
        Node node;
        float distance = Float.POSITIVE_INFINITY;
        int submesh;
        float[] normal = {0.0f, 0.0f, 1.0f};
        float[] texS = new float[Graphics3D.NUM_TEXTURE_UNITS];
        float[] texT = new float[Graphics3D.NUM_TEXTURE_UNITS];
    }

	private boolean isAncestor(Node potentialChild)
	{
		Node p = this.getParent();
		while (p != null)
		{
			if (p == potentialChild)
			{
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	int getRenderableCount() { return this.numRenderables; }
	int getNonCullableCount() { return this.numNonCullables; }
}
