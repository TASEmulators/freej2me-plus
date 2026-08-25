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

public class Group extends Node {
    public Node firstChild = null;
    public int numNonCullables = 0, numRenderables = 0;

    private final Transform t1 = new Transform();
    private final Transform t2 = new Transform();
    private final float[] cameraPoints = new float[8];
    private final float[] ray = new float[6];
    private final float[] localRay = new float[8];
    private final float[] scaleBias = new float[4];
    private final float[] spritePoints = new float[12];
    private final float[] texCoord = new float[4];
    private final PickResult result = new PickResult();

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

        camera.getProjection(t1);
        t1.invert();

        cameraPoints[0] = 2.0f * x - 1.0f; cameraPoints[1] = 1.0f - 2.0f * y; cameraPoints[2] = -1.0f; cameraPoints[3] = 1.0f;
        cameraPoints[4] = 2.0f * x - 1.0f; cameraPoints[5] = 1.0f - 2.0f * y; cameraPoints[6] =  1.0f; cameraPoints[7] = 1.0f;

        t1.transform(cameraPoints);

        for (int offset = 0; offset <= 4; offset += 4)
        {
            float invW = 1.0f / cameraPoints[offset + 3];
            cameraPoints[offset]     *= invW;
            cameraPoints[offset + 1] *= invW;
            cameraPoints[offset + 2] *= invW;
        }

        if (!camera.getTransformTo(this, t2))
        {
            throw new IllegalStateException("Camera and Group are not in the same scene graph");
        }
        t2.transform(cameraPoints);

        ray[0] = cameraPoints[0]; ray[1] = cameraPoints[1]; ray[2] = cameraPoints[2];
        ray[3] = cameraPoints[4] - cameraPoints[0]; ray[4] = cameraPoints[5] - cameraPoints[1];
        ray[5] = cameraPoints[6] - cameraPoints[2];

        result.reset();
        traverseForPick(this, this.isPickingEnabled(), scope, ray, camera, cameraPoints, true, result);
        return populateRayIntersection(result, ri, ray);
    }

    public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri)
    {
        if (dx == 0.0f && dy == 0.0f && dz == 0.0f)
        {
            throw new IllegalArgumentException("Ray direction vector cannot be zero");
        }

        ray[0] = ox; ray[1] = oy; ray[2] = oz;
        ray[3] = dx; ray[4] = dy; ray[5] = dz;

        result.reset();
        traverseForPick(this, this.isPickingEnabled(), scope, ray, null, null, false, result);
        return populateRayIntersection(result, ri, ray);
    }

    private boolean populateRayIntersection(PickResult result, RayIntersection ri, float[] ray)
    {
        if (result.node == null) { return false; }
        if (ri != null)
        {
            ri.set(result.node, result.distance, result.submesh, ray, result.normal, result.texS, result.texT);
        }
        return true;
    }

    private boolean isAncestor(Node potentialChild)
    {
        for (Node p = this.getParent(); p != null; p = p.getParent())
        {
            if (p == potentialChild) { return true; }
        }
        return false;
    }

    int getRenderableCount() { return this.numRenderables; }
    int getNonCullableCount() { return this.numNonCullables; }

    private void traverseForPick(Node node, boolean parentPicking, int scope, float[] ray,
        Camera camera, float[] cameraPoints, boolean pickSprites,
        PickResult result)
    {
        if (!parentPicking || !node.isPickingEnabled()) { return; }

        if (node instanceof Mesh)
        {
            pickMesh((Mesh) node, scope, ray, result);
            if (node instanceof SkinnedMesh)
            {
                traverseForPick(((SkinnedMesh) node).getSkeleton(), true, scope, ray, camera, cameraPoints, pickSprites, result);
            }
        }
        else if (pickSprites && node instanceof Sprite3D)
        {
            pickSprite((Sprite3D) node, scope, camera, cameraPoints, result);
        }
        else if (node instanceof Group)
        {
            Group g = (Group) node;
            Node child = g.firstChild;
            if (child != null)
            {
                do
                {
                    traverseForPick(child, true, scope, ray, camera, cameraPoints, pickSprites, result);
                    child = child.right;
                } while (child != g.firstChild);
            }
        }
    }

    private void pickMesh(Mesh mesh, int scope, float[] groupRay, PickResult result)
    {
        if ((scope & mesh.getScope()) == 0) { return; }

        VertexBuffer vertices = mesh.getVertexBuffer();
        VertexArray positions = vertices.getPositions(scaleBias);
        if (positions == null) { throw new IllegalStateException("Pickable Mesh has no positions"); }

        float[] localPositions = readVertexArray(positions);
        for (int vertex = 0; vertex < positions.getVertexCount(); vertex++)
        {
            int offset = 3 * vertex;
            localPositions[offset]     = localPositions[offset]     * scaleBias[0] + scaleBias[1];
            localPositions[offset + 1] = localPositions[offset + 1] * scaleBias[0] + scaleBias[2];
            localPositions[offset + 2] = localPositions[offset + 2] * scaleBias[0] + scaleBias[3];
        }

        if (!this.getTransformTo(mesh, t1))
        {
            throw new ArithmeticException("Mesh transform cannot be computed");
        }

        localRay[0] = groupRay[0]; localRay[1] = groupRay[1]; localRay[2] = groupRay[2]; localRay[3] = 1.0f;
        localRay[4] = groupRay[3]; localRay[5] = groupRay[4]; localRay[6] = groupRay[5]; localRay[7] = 0.0f;
        t1.transform(localRay);

        for (int submesh = 0; submesh < mesh.getSubmeshCount(); submesh++)
        {
            Appearance appearance = mesh.getAppearance(submesh);
            if (appearance == null) { continue; }

            IndexBuffer buffer = mesh.getIndexBuffer(submesh);
            int[] indices = buffer.getIndexArray();
            if (indices == null || indices.length != buffer.getIndexCount() || indices.length % 3 != 0)
            {
                throw new IllegalStateException("Invalid triangle index buffer");
            }

            PolygonMode mode = appearance.getPolygonMode();
            int culling = mode != null ? mode.getCulling() : PolygonMode.CULL_BACK;
            int winding = mode != null ? mode.getWinding() : PolygonMode.WINDING_CCW;

            for (int t = 0; t < indices.length; t += 3)
            {
                intersectTriangle(mesh, vertices, appearance, submesh, winding, culling,
                    localRay, localPositions, indices[t], indices[t + 1], indices[t + 2], result);
            }
        }
    }

    private void intersectTriangle(Mesh mesh, VertexBuffer vertices, Appearance appearance,
        int submesh, int winding, int culling, float[] ray, float[] positions,
        int ia, int ib, int ic, PickResult result)
    {
        int a = 3 * ia, b = 3 * ib, c = 3 * ic;

        double e1x = positions[b] - positions[a],     e1y = positions[b + 1] - positions[a + 1],     e1z = positions[b + 2] - positions[a + 2];
        double e2x = positions[c] - positions[a],     e2y = positions[c + 1] - positions[a + 1],     e2z = positions[c + 2] - positions[a + 2];

        double px = ray[5] * e2z - ray[6] * e2y;
        double py = ray[6] * e2x - ray[4] * e2z;
        double pz = ray[4] * e2y - ray[5] * e2x;

        double det = e1x * px + e1y * py + e1z * pz;
        if (det == 0.0) { return; }

        boolean front = det > 0.0;
        if (winding == PolygonMode.WINDING_CW) front = !front;
        if ((culling == PolygonMode.CULL_BACK && !front) || (culling == PolygonMode.CULL_FRONT && front)) { return; }

        double invDet = 1.0 / det;
        double tx = ray[0] - positions[a], ty = ray[1] - positions[a + 1], tz = ray[2] - positions[a + 2];

        double u = (tx * px + ty * py + tz * pz) * invDet;
        if (u < 0.0 || u > 1.0) { return; }

        double qx = ty * e1z - tz * e1y;
        double qy = tz * e1x - tx * e1z;
        double qz = tx * e1y - ty * e1x;

        double v = (ray[4] * qx + ray[5] * qy + ray[6] * qz) * invDet;
        if (v < 0.0 || u + v > 1.0) { return; }

        double distance = (e2x * qx + e2y * qy + e2z * qz) * invDet;
        if (distance < 0.0 || distance >= result.distance) return;

        float wa = (float) (1.0 - u - v), wb = (float) u, wc = (float) v;
        result.node = mesh;
        result.distance = (float) distance;
        result.submesh = submesh;

        computeNormal(vertices, ia, ib, ic, wa, wb, wc, positions, result.normal);
        computeTextureCoordinates(vertices, appearance, ia, ib, ic, wa, wb, wc, result.texS, result.texT);
    }

    private void computeNormal(VertexBuffer vertices, int ia, int ib, int ic,
        float wa, float wb, float wc, float[] positions, float[] normal)
    {
        VertexArray normals = vertices.getNormals();
        if (normals != null)
        {
            float[] values = readVertexArray(normals);
            normal[0] = wa * values[3 * ia]     + wb * values[3 * ib]     + wc * values[3 * ic];
            normal[1] = wa * values[3 * ia + 1] + wb * values[3 * ib + 1] + wc * values[3 * ic + 1];
            normal[2] = wa * values[3 * ia + 2] + wb * values[3 * ib + 2] + wc * values[3 * ic + 2];
        }
        else
        {
            int a = 3 * ia, b = 3 * ib, c = 3 * ic;
            float e1x = positions[b] - positions[a],     e1y = positions[b + 1] - positions[a + 1],     e1z = positions[b + 2] - positions[a + 2];
            float e2x = positions[c] - positions[a],     e2y = positions[c + 1] - positions[a + 1],     e2z = positions[c + 2] - positions[a + 2];
            normal[0] = e1y * e2z - e1z * e2y;
            normal[1] = e1z * e2x - e1x * e2z;
            normal[2] = e1x * e2y - e1y * e2x;
        }

        float len = M3GMath.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
        if (len != 0.0f)
        {
            normal[0] /= len; normal[1] /= len; normal[2] /= len;
        }
    }

    private void computeTextureCoordinates(VertexBuffer vertices, Appearance appearance,
        int ia, int ib, int ic, float wa, float wb, float wc,
        float[] texS, float[] texT)
    {
        float[] coord = texCoord;

        for (int unit = 0; unit < Graphics3D.NUM_TEXTURE_UNITS; unit++)
        {
            texS[unit] = texT[unit] = 0.0f;
            Texture2D texture = appearance.getTexture(unit);
            VertexArray coords = vertices.getTexCoords(unit, scaleBias);
            if (texture == null || coords == null) continue;

            float[] values = readVertexArray(coords);
            int dim = coords.getComponentCount();

            coord[0] = 0.0f; coord[1] = 0.0f; coord[2] = 0.0f; coord[3] = 1.0f;
            for (int k = 0; k < dim; k++)
            {
                coord[k] = scaleBias[0] * (wa * values[dim * ia + k] + wb *
                	values[dim * ib + k] + wc * values[dim * ic + k]) +
                	scaleBias[k + 1];
            }

            texture.getCompositeTransform(t2);
            t2.transform(coord);

            if (coord[3] != 0.0f)
            {
                texS[unit] = coord[0] / coord[3];
                texT[unit] = coord[1] / coord[3];
            }
        }
    }

    private void pickSprite(Sprite3D sprite, int scope, Camera camera, float[] cameraPoints, PickResult result)
    {
        if (!sprite.isScaled() || sprite.getAppearance() == null || (scope & sprite.getScope()) == 0) { return; }

        if (!sprite.getTransformTo(camera, t1))
        {
            throw new ArithmeticException("Sprite transform cannot be computed");
        }

        float[] pts = spritePoints;
        pts[0] = 0; pts[1] = 0; pts[2] = 0; pts[3] = 1;
        pts[4] = 0.5f; pts[5] = 0; pts[6] = 0; pts[7] = 1;
        pts[8] = 0; pts[9] = 0.5f; pts[10] = 0; pts[11] = 1;

        t1.transform(pts);

        float cx = pts[0] / pts[3], cy = pts[1] / pts[3], cz = pts[2] / pts[3];
        float halfW = M3GMath.sqrt((pts[4]/pts[7] - cx)*(pts[4]/pts[7] - cx) + (pts[5]/pts[7] - cy)*(pts[5]/pts[7] - cy) + (pts[6]/pts[7] - cz)*(pts[6]/pts[7] - cz));
        float halfH = M3GMath.sqrt((pts[8]/pts[11] - cx)*(pts[8]/pts[11] - cx) + (pts[9]/pts[11] - cy)*(pts[9]/pts[11] - cy) + (pts[10]/pts[11] - cz)*(pts[10]/pts[11] - cz));

        float rayOZ = cameraPoints[2], rayDZ = cameraPoints[6] - cameraPoints[2];
        if (rayDZ == 0.0f || halfW == 0.0f || halfH == 0.0f) { return; }

        float distance = (cz - rayOZ) / rayDZ;
        if (distance < 0.0f || distance >= result.distance) { return; }

        float hitX = cameraPoints[0] + distance * (cameraPoints[4] - cameraPoints[0]);
        float hitY = cameraPoints[1] + distance * (cameraPoints[5] - cameraPoints[1]);

        if (hitX < cx - halfW || hitX > cx + halfW || hitY < cy - halfH || hitY > cy + halfH) { return; }

        result.node = sprite;
        result.distance = distance;
        result.submesh = 0;
        result.normal[0] = 0.0f; result.normal[1] = 0.0f; result.normal[2] = 1.0f;

        for (int u = 0; u < Graphics3D.NUM_TEXTURE_UNITS; u++)
        {
            result.texS[u] = result.texT[u] = 0.0f;
        }

        result.texS[0] = (hitX - (cx - halfW)) / (2.0f * halfW);
        result.texT[0] = ((cy + halfH) - hitY) / (2.0f * halfH);
    }

    private static float[] readVertexArray(VertexArray array)
    {
        int vertices = array.getVertexCount();
        int elements = vertices * array.getComponentCount();
        float[] result = new float[elements];

        if (array.getComponentType() == 1)
        {
            byte[] values = new byte[elements];
            array.get(0, vertices, values);
            for (int i = 0; i < elements; i++) { result[i] = values[i]; }
        }
        else
        {
            short[] values = new short[elements];
            array.get(0, vertices, values);
            for (int i = 0; i < elements; i++) { result[i] = values[i]; }
        }
        return result;
    }

    private static class PickResult
    {
        Node node;
        float distance;
        int submesh;
        final float[] normal = new float[3];
        final float[] texS = new float[Graphics3D.NUM_TEXTURE_UNITS];
        final float[] texT = new float[Graphics3D.NUM_TEXTURE_UNITS];

        void reset()
        {
            node = null;
            distance = Float.POSITIVE_INFINITY;
            submesh = 0;
            normal[0] = 0.0f; normal[0] = 0.0f; normal[2] = 1.0f;
        }
    }
}
