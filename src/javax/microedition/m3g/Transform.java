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

import java.util.Arrays;

public class Transform
{
	private final byte[] bVertices = new byte[3];
	private final short[] sVertices = new short[3];
	// This is a 4x4 matrix represented as a 16 item long array.
	// The items are in row major order:
	//   [  0,  1,  2,  3 ]
	//   [  4,  5,  6,  7 ]   Addressing in 2D vs in 1D:
	//   [  8,  9, 10, 11 ]     mat_2D[row][col] == mat_1D[4*row + col]
	//   [ 12, 13, 14, 15 ]
	private float[] matrix = new float[]
	{
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 1, 0,
		0, 0, 0, 1
	};

	private final float[] scratch = new float[]
	{
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 1, 0,
		0, 0, 0, 1
	};

	private final float[] manipulationMatrix = new float[]
	{
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 1, 0,
		0, 0, 0, 1
	};

	/* ------------------------- public methods ------------------------- */
	public Transform() { }

	public Transform(Transform transform)
	{
		/* As per JSR-184, throw NullPointerException if the given transform is null. */
		if(transform == null) { throw new NullPointerException("Cannot initialize with a null transform."); }

		System.arraycopy(transform.matrix, 0, this.matrix, 0, 16);
	}

	public void get(float[] matrix)
	{
		/* As per JSR-184, throw NullPointerException if the given matrix is null.*/
		if(matrix == null) { throw new NullPointerException("Cannot copy the matrix contents to a null object."); }

		/* Also per JSR-184, throw IllegalArgumentException if the matrix length is less than 16.*/
		if(matrix.length < 16) { throw new IllegalArgumentException("The received matrix is not a valid 4x4 transform matrix."); }

		System.arraycopy(this.matrix, 0, matrix, 0, 16);
	}

	public void invert()
	{
		/* The inverse matrix is calculated by using an adapted version of the Laplace Expansion Theorem. */

		/*
		 * Since the matrix is a linear array, the logic is akin to C's pointer arithmethic
		 * on matrices, where accesses to mat[row][col] becomes mat[4*row + col].
		 */
		float[] m = this.matrix;

		float s0 = m[0] * m[5] - m[4] * m[1];
		float s1 = m[0] * m[6] - m[4] * m[2];
		float s2 = m[0] * m[7] - m[4] * m[3];
		float s3 = m[1] * m[6] - m[5] * m[2];
		float s4 = m[1] * m[7] - m[5] * m[3];
		float s5 = m[2] * m[7] - m[6] * m[3];

		float c0 = m[8] * m[13] - m[12] * m[9];
		float c1 = m[8] * m[14] - m[12] * m[10];
		float c2 = m[8] * m[15] - m[12] * m[11];
		float c3 = m[9] * m[14] - m[13] * m[10];
		float c4 = m[9] * m[15] - m[13] * m[11];
		float c5 = m[10] * m[15] - m[14] * m[11];

		/*
		 * Check if the transform matrix can be inverted by calculating its determinant.
		 */

		float determDiv = (s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0);

		if(determDiv == 0.0f) { throw new ArithmeticException("This transform matrix cannot be inverted."); }

		float determinant = 1.0f / determDiv;

		/* If it can't, throw ArithmeticException as per JSR-184. */
		if(determinant == 0.0f) { throw new ArithmeticException("This transform matrix cannot be inverted."); }

		/* Calculate the inverse. */
		scratch[0]  = ( matrix[5]  * c5 - matrix[6]  * c4 + matrix[7]  * c3) * determinant;
		scratch[1]  = (-matrix[1]  * c5 + matrix[2]  * c4 - matrix[3]  * c3) * determinant;
		scratch[2]  = ( matrix[13] * s5 - matrix[14] * s4 + matrix[15] * s3) * determinant;
		scratch[3]  = (-matrix[9]  * s5 + matrix[10] * s4 - matrix[11] * s3) * determinant;

		scratch[4]  = (-matrix[4]  * c5 + matrix[6]  * c2 - matrix[7]  * c1) * determinant;
		scratch[5]  = ( matrix[0]  * c5 - matrix[2]  * c2 + matrix[3]  * c1) * determinant;
		scratch[6]  = (-matrix[12] * s5 + matrix[14] * s2 - matrix[15] * s1) * determinant;
		scratch[7]  = ( matrix[8]  * s5 - matrix[10] * s2 + matrix[11] * s1) * determinant;

		scratch[8]  = ( matrix[4]  * c4 - matrix[5]  * c2 + matrix[7]  * c0) * determinant;
		scratch[9]  = (-matrix[0]  * c4 + matrix[1]  * c2 - matrix[3]  * c0) * determinant;
		scratch[10] = ( matrix[12] * s4 - matrix[13] * s2 + matrix[15] * s0) * determinant;
		scratch[11] = (-matrix[8]  * s4 + matrix[9]  * s2 - matrix[11] * s0) * determinant;

		scratch[12] = (-matrix[4]  * c3 + matrix[5]  * c1 - matrix[6]  * c0) * determinant;
		scratch[13] = ( matrix[0]  * c3 - matrix[1]  * c1 + matrix[2]  * c0) * determinant;
		scratch[14] = (-matrix[12] * s3 + matrix[13] * s1 - matrix[14] * s0) * determinant;
		scratch[15] = ( matrix[8]  * s3 - matrix[9]  * s1 + matrix[10] * s0) * determinant;

		System.arraycopy(scratch, 0, this.matrix, 0, 16);
	}

	public void postMultiply(Transform transform)
	{
		if (transform == null) { throw new NullPointerException("Cannot multiply by receiving a null transform."); }
		multiply(this.matrix, transform.matrix, this.matrix);
	}

	public void postRotate(float angle, float ax, float ay, float az)
	{
		computeRotationMatrix(angle, ax, ay, az);
		multiply(this.matrix, this.manipulationMatrix, this.matrix);
	}

	public void postRotateQuat(float qx, float qy, float qz, float qw)
	{
		computeRotationQuatMatrix(qx, qy, qz, qw);
		multiply(this.matrix, this.manipulationMatrix, this.matrix);
	}

	public void postScale(float sx, float sy, float sz)
	{
		float[] m = this.matrix;
		m[0] *= sx;  m[1] *= sy;  m[2] *= sz;
		m[4] *= sx;  m[5] *= sy;  m[6] *= sz;
		m[8] *= sx;  m[9] *= sy;  m[10] *= sz;
		m[12] *= sx; m[13] *= sy; m[14] *= sz;
	}

	public void postTranslate(float tx, float ty, float tz)
	{
		float[] m = this.matrix;
		m[3]  += m[0] * tx + m[1] * ty + m[2] * tz;
		m[7]  += m[4] * tx + m[5] * ty + m[6] * tz;
		m[11] += m[8] * tx + m[9] * ty + m[10] * tz;
		m[15] += m[12] * tx + m[13] * ty + m[14] * tz;
	}

	public void set(float[] matrix)
	{
		/* As per JSR-184, throw NullPointerException if the given matrix is null. */
		if(matrix == null) { throw new NullPointerException("Tried setting the transform with a null matrix."); }

		/* Also per JSR-184, IllegalArgumentException if matrix.length < 16 (not a 4x4 matrix). */
		if(matrix.length < 16) { throw new IllegalArgumentException("Cannot copy data from a matrix with less than 16 elements."); }

		System.arraycopy(matrix, 0, this.matrix, 0, 16);
	}

	public void set(Transform transform)
	{
		/* As per JSR-184, throw NullPointerException if the given transform is null. */
		if(transform == null) { throw new NullPointerException("Tried to set a null transform."); }

		System.arraycopy(transform.matrix, 0, this.matrix, 0, 16);
	}

	public void setIdentity()
	{
		float[] m = this.matrix;
		m[0] = 1; m[1] = 0; m[2] = 0; m[3] = 0;
		m[4] = 0; m[5] = 1; m[6] = 0; m[7] = 0;
		m[8] = 0; m[9] = 0; m[10] = 1; m[11] = 0;
		m[12] = 0; m[13] = 0; m[14] = 0; m[15] = 1;
	}

	public void transform(float[] vectors)
	{
		/* As per JSR-184, throw NullPointerException if the given vector is null. */
		if(vectors == null) { throw new NullPointerException("Cannot transform a null vector."); }

		/* Also per JSR-184, throw IllegalArgumentException if the given vector is not a flat array of quadruplets. */
		if(vectors.length % 4 != 0) { throw new IllegalArgumentException("Cannot transform a vector array that's not multiple of 4."); }

		/* Multiply each 4D vector with this transform's matrix by quadruplets, hence the vector offset of 4. */
		float x, y, z, w;
		float[] m = this.matrix;
		for (int offset = 0; offset < vectors.length; offset += 4)
		{
			x = vectors[offset];
			y = vectors[offset + 1];
			z = vectors[offset + 2];
			w = vectors[offset + 3];

			vectors[offset]     = m[0] * x + m[1] * y + m[2] * z + m[3] * w;
			vectors[offset + 1] = m[4] * x + m[5] * y + m[6] * z + m[7] * w;
			vectors[offset + 2] = m[8] * x + m[9] * y + m[10] * z + m[11] * w;
			vectors[offset + 3] = m[12] * x + m[13] * y + m[14] * z + m[15] * w;
		}
	}

	public void transform(VertexArray in, float[] out, boolean W)
	{
		/* As per JSR-184, throw NullPointerException if either 'in' or 'out' are null. */
		if(in == null || out == null) { throw new NullPointerException("Cannot transform since input vertex array or output array are null."); }

		int vertexCount = in.getVertexCount();
		int vertexDims = in.getComponentCount();

		/* Also per JSR-184, throw IllegalArgumentException if numComponents < 2 || > 3, or out.length < (4 * vertexCount). */
		if (vertexDims < 2 || vertexDims == 4 || out.length < 4 * vertexCount) // Vertex position data has either 2 or 3 components
		{
			throw new IllegalArgumentException("Tried to transform an invalid vertex array.");
		}

		// Fill the `out` array with raw data
		float wVal = W ? 1.0f : 0.0f;

		if (in.getComponentType() == 1)
		{
			if (vertexDims < 3) { bVertices[2] = 0; }

			for (int i = 0; i < vertexCount; i++)
			{
				in.get(i, 1, bVertices);
				int outIdx = i * 4;

				out[outIdx]     = bVertices[0];
				out[outIdx + 1] = bVertices[1];
				out[outIdx + 2] = bVertices[2];
				out[outIdx + 3] = wVal;
			}
		}
		else
		{
			if (vertexDims < 3) { sVertices[2] = 0; }
			for (int i = 0; i < vertexCount; i++)
			{
				in.get(i, 1, sVertices);
				int outIdx = i * 4;

				out[outIdx]     = sVertices[0];
				out[outIdx + 1] = sVertices[1];
				out[outIdx + 2] = sVertices[2];
				out[outIdx + 3] = wVal;
			}
		}

		// Do the transformation on the raw data that is currently in `out`
		this.transform(out);
	}

	public void transpose()
	{
		float[] m = this.matrix;
		float tmp;

		tmp = m[1];  m[1] = m[4];   m[4] = tmp;
		tmp = m[2];  m[2] = m[8];   m[8] = tmp;
		tmp = m[3];  m[3] = m[12];  m[12] = tmp;
		tmp = m[6];  m[6] = m[9];   m[9] = tmp;
		tmp = m[7];  m[7] = m[13];  m[13] = tmp;
		tmp = m[11]; m[11] = m[14]; m[14] = tmp;
	}

	/* ------------------------- package methods ------------------------- */

	// The pre* methods exist to facilitate chaining transformations.
	// They are mainly used in rendering.

	// package-private
	void preMultiply(Transform transform)
	{
		if (transform == null) { throw new NullPointerException("preMultiply() called with null transform."); }
		multiply(transform.matrix, this.matrix, this.matrix);
	}

	void preRotate(float angle, float ax, float ay, float az)
	{
		computeRotationMatrix(angle, ax, ay, az);
		multiply(this.manipulationMatrix, this.matrix, this.matrix);
	}

	void preRotateQuat(float qx, float qy, float qz, float qw)
	{
		computeRotationQuatMatrix(qx, qy, qz, qw);
		multiply(this.manipulationMatrix, this.matrix, this.matrix);
	}

	private void computeRotationMatrix(float angle, float ax, float ay, float az)
	{
		float axisLen = (ax * ax) + (ay * ay) + (az * az);

		// Angle or axis length as zero means no/invalid rotation. Return right away
		if (angle == 0.0f || axisLen < M3GMath.EPSILON) { return; }

		resetManipulationMatrix();
		if (angle == 0) { return; }

		float rad = M3GMath.toRadians(angle);
		float s = M3GMath.sin(rad);
		float c = M3GMath.cos(rad);
		float d = 1.0f - c;

		float l = M3GMath.sqrt(axisLen);
		float x = ax / l;
		float y = ay / l;
		float z = az / l;

		manipulationMatrix[0] = x*x*d + c;   manipulationMatrix[1] = y*x*d - z*s; manipulationMatrix[2] = z*x*d + y*s;
		manipulationMatrix[4] = x*y*d + z*s; manipulationMatrix[5] = y*y*d + c;   manipulationMatrix[6] = z*y*d - x*s;
		manipulationMatrix[8] = x*z*d - y*s; manipulationMatrix[9] = y*z*d + x*s; manipulationMatrix[10]= z*z*d + c;
	}

	private void computeRotationQuatMatrix(float qx, float qy, float qz, float qw)
	{
		if (qx == 0 && qy == 0 && qz == 0 && qw == 0) {
			throw new IllegalArgumentException("Cannot rotate when all quaternion components are zero.");
		}

		float l = M3GMath.sqrt((qx * qx) + (qy * qy) + (qz * qz) + (qw * qw));
		float x = qx / l;
		float y = qy / l;
		float z = qz / l;
		float w = qw / l;

		resetManipulationMatrix();
		manipulationMatrix[0] = 1 - 2*y*y - 2*z*z;
		manipulationMatrix[1] = 2*x*y - 2*z*w;
		manipulationMatrix[2] = 2*x*z + 2*y*w;

		manipulationMatrix[4] = 2*x*y + 2*z*w;
		manipulationMatrix[5] = 1 - 2*x*x - 2*z*z;
		manipulationMatrix[6] = 2*y*z - 2*x*w;

		manipulationMatrix[8] = 2*x*z - 2*y*w;
		manipulationMatrix[9] = 2*y*z + 2*x*w;
		manipulationMatrix[10]= 1 - 2*x*x - 2*y*y;
	}

	private void resetManipulationMatrix()
	{
		manipulationMatrix[0] = 1.0f; manipulationMatrix[1] = 0.0f; manipulationMatrix[2] = 0.0f; manipulationMatrix[3] = 0.0f;
		manipulationMatrix[4] = 0.0f; manipulationMatrix[5] = 1.0f; manipulationMatrix[6] = 0.0f; manipulationMatrix[7] = 0.0f;
		manipulationMatrix[8] = 0.0f; manipulationMatrix[9] = 0.0f; manipulationMatrix[10]= 1.0f; manipulationMatrix[11]= 0.0f;
		manipulationMatrix[12]= 0.0f; manipulationMatrix[13]= 0.0f; manipulationMatrix[14]= 0.0f; manipulationMatrix[15]= 1.0f;
	}

	/* ------------------------- private methods ------------------------- */

	private void multiply(float[] left, float[] right, float[] target)
	{
		float[] out = (target == left || target == right) ? scratch : target;

		for (int r = 0; r < 16; r += 4)
		{
			float l0 = left[r], l1 = left[r+1], l2 = left[r+2], l3 = left[r+3];

			out[r]   = l0 * right[0] + l1 * right[4] + l2 * right[8]  + l3 * right[12];
			out[r+1] = l0 * right[1] + l1 * right[5] + l2 * right[9]  + l3 * right[13];
			out[r+2] = l0 * right[2] + l1 * right[6] + l2 * right[10] + l3 * right[14];
			out[r+3] = l0 * right[3] + l1 * right[7] + l2 * right[11] + l3 * right[15];
		}

		if (out == scratch)
		{
			System.arraycopy(scratch, 0, target, 0, 16);
		}
	}
}
