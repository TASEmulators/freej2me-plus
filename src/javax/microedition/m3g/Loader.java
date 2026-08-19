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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.Vector;
import java.util.zip.Inflater;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;
import org.recompile.mobile.Mobile;

public class Loader
{
	private DataInputStream dis;
	private Vector<Object3D> objs;
	private Vector<String> roots;
	private String resName;
	private static String resDir;
	private int bytesRead = 0;

	private static final byte[] M3G_FILE_IDENTIFIER = { -85, 74, 83, 82, 49, 56, 52, -69, 13, 10, 26, 10 };
	private static final byte[] PNG_FILE_IDENTIFIER = { -119, 80, 78, 71, 13, 10, 26, 10 };
	private static final byte[] JPEG_FILE_IDENTIFIER = { -1, -40 };

	private static final int INVALID_HEADER_TYPE = -1;
	private static final int M3G_TYPE = 0;
	private static final int PNG_TYPE = 1;
	private static final int JPEG_TYPE = 2;

	private static final int PNG_IHDR = ((73 << 24) + (72 << 16) + (68 << 8) + 82);
	private static final int PNG_tRNS = ((116 << 24) + (82 << 16) + (78 << 8) + 83);
	private static final int PNG_IDAT = ((73 << 24) + (68 << 16) + (65 << 8) + 84);

	private static final int JPEG_JFIF = ((74 << 24) + (70 << 16) + (73 << 8) + 70);
	private static final int JPEG_SOFn_DELTA = 7;
	private static final int JPEG_INVALID_COLOUR_FORMAT = -1;

	private static Vector<String> activeRefs = new Vector<String>();

	private Loader(byte[] data, Vector<Object3D> objects, Vector<String> roots, String resDir, Vector<String> activeRefs)
	{
		this.dis = new DataInputStream(new ByteArrayInputStream(data));
		this.objs = objects;
		this.roots = roots;
		this.resDir = resDir;
		this.activeRefs = activeRefs;
	}

	private Loader(byte[] data, int offset, Vector<String> activeRefs) throws IOException
	{
		if (data == null) { throw new NullPointerException("Cannot load M3G object from null data"); }
		if (offset >= data.length) { throw new IllegalArgumentException("Invalid offset for m3g data"); }

		this.dis = new DataInputStream(new ByteArrayInputStream(data));
		if (offset > 0) { this.dis.skipBytes(offset); }
		this.resDir = "/";
		this.activeRefs = activeRefs != null ? activeRefs : new Vector<String>();
	}

	private Loader(String name, Vector<String> activeRefs) throws IOException
	{
		if (name == null) { throw new NullPointerException("Cannot load M3G object from null path"); }

		// Standardize path (ensure leading slash for MIDlet resource stream)
		String canonicalPath = name.startsWith("/") ? name : "/" + name;

		this.activeRefs = activeRefs != null ? activeRefs : new Vector<String>();

		if (this.activeRefs.contains(canonicalPath))
		{
			throw new IOException("Detected a cyclic reference loop: " + canonicalPath);
		}

		// Push path to active stack
		this.activeRefs.addElement(canonicalPath);

		InputStream is = Mobile.getMIDletResourceAsStream(canonicalPath);
		if (is == null)
		{
			// Remove the path from active references before throwing an
			// exception. Some apps expecti this and catch, so we must keep
			// the active references array valid
			this.activeRefs.removeElement(canonicalPath);
			throw new IOException("Can't load resource: " + canonicalPath);
		}

		this.resName = canonicalPath;
		// Compute base directory (e.g. "/models/car.m3g" -> "/models/")
		int lastSlash = canonicalPath.lastIndexOf('/');
		this.resDir = (lastSlash >= 0) ? canonicalPath.substring(0, lastSlash + 1) : "/";

		this.dis = new DataInputStream(new BufferedInputStream(is));
	}

	public static Object3D[] load(byte[] data, int offset) throws IOException
	{
		Mobile.log(Mobile.LOG_DEBUG, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Jar requested to load M3G object from byte data of size " + data.length + " starting at offset " + offset);

		try { return new Loader(data, offset, new Vector<String>()).load(); }
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Loader(byte[], int) could not load data.");
			e.printStackTrace();
			throw new IOException("Invalid M3G data.");
		}
	}

	public static Object3D[] load(String name) throws IOException
	{
		return new Loader(name, new Vector<String>()).load();
	}

	private Object3D[] loadPNG() throws IOException
	{
		int format = Image2D.RGB;
		dis.mark(1024 * 1024);
		// Scan chunks that have effect on Image2D format
		dis.skipBytes(PNG_FILE_IDENTIFIER.length);
		try
		{
			while (true)
			{
				int length = dis.readInt();
				int type = dis.readInt();
				// IHDR
				if (type == PNG_IHDR)
				{
					dis.skipBytes(9);
					int colourType = dis.readUnsignedByte();
					length -= 10;
					switch (colourType)
					{
						case 0:
							format = Image2D.LUMINANCE;
							break;
						case 2:
							format = Image2D.RGB;
							break;
						case 3:
							format = Image2D.RGB;
							break;
						case 4:
							format = Image2D.LUMINANCE_ALPHA;
							break;
						case 6:
							format = Image2D.RGBA;
							break;
					}

					// Is it luminance or alpha? They don't use tRNS.
					if (format == Image2D.LUMINANCE_ALPHA
						|| format == Image2D.RGBA) { break; }
				}
				// tRNS
				if (type == PNG_tRNS)
				{
					switch (format)
					{
						case Image2D.LUMINANCE:
							format = Image2D.LUMINANCE_ALPHA;
							break;
						case Image2D.RGB:
							format = Image2D.RGBA;
							break;
					}
					break;
				}
				// IDAT
				if (type == PNG_IDAT) { break; }
				dis.skipBytes(length + 4);
			}
		} // EOF
		catch (Exception e) { throw new IOException("M3G Loader: Failed to load PNG Image"); }
		dis.reset();
		return buildImage2D(format);
	}


	private Object3D[] loadJPEG() throws IOException
	{
		int format = JPEG_INVALID_COLOUR_FORMAT;
		dis.mark(1024 * 1024);
		// Skip file identifier
		dis.skipBytes(JPEG_FILE_IDENTIFIER.length);
		try
		{
			int marker;
			do
			{
				// Find marker
				while (dis.readUnsignedByte() != 0xff);
				do { marker = dis.readUnsignedByte(); }
				while (marker == 0xff);

				// Parse marker
				switch (marker)
				{
					// 'SOFn' (Start Of Frame n)
					case 0xC0: case 0xC1: case 0xC2: case 0xC3:
					case 0xC5: case 0xC6: case 0xC7: case 0xC9:
					case 0xCA: case 0xCB: case 0xCD: case 0xCE: case 0xCF:
						// Skip length(2), precision(1), width(2), height(2)
						dis.skipBytes(JPEG_SOFn_DELTA);
						switch (dis.readUnsignedByte())
						{
							case 1:
								format = Image2D.LUMINANCE;
								break;
							case 3:
								format = Image2D.RGB;
								break;
							default:
								Mobile.log(Mobile.LOG_ERROR, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Unknown jpeg format. ");
								throw new IOException("Unknown JPG format.");
						}
						break;
					// APP0 (0xe0) marker segments and constrains certain parameters in the frame.
					case 0xe0:
						int length = dis.readUnsignedShort();
						if (JPEG_JFIF != dis.readInt())
						{
							Mobile.log(Mobile.LOG_ERROR, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Unknown jpeg format. ");
							throw new IOException("Not a valid JPG file.");
						}
						dis.skipBytes(length - 4 - 2);
						break;
					default:
						// Skip variable data
						dis.skipBytes(dis.readUnsignedShort() - 2);
						break;
				}
			}
			while (format == JPEG_INVALID_COLOUR_FORMAT);
		} catch (Exception e) { throw new IOException("M3G Loader: Failed to load JPG image"); }
		dis.reset();
		return buildImage2D(format);
	}

	private Object3D[] buildImage2D(int aColourFormat) throws IOException
	{
		return new Object3D[]{new Image2D(aColourFormat, Image.createImage(dis))};
	}

	private int getIdentifierType(byte[] aData, int aOffset)
	{
		if (parseIdentifier(aData, aOffset, JPEG_FILE_IDENTIFIER)) { return JPEG_TYPE; }
		else if (parseIdentifier(aData, aOffset, PNG_FILE_IDENTIFIER)) { return PNG_TYPE; }
		else if (parseIdentifier(aData, aOffset, M3G_FILE_IDENTIFIER)) { return M3G_TYPE; }
		return INVALID_HEADER_TYPE;
	}

	private boolean parseIdentifier(byte[] aData, int aOffset, byte[] aIdentifier)
	{
		if ((aData.length - aOffset) < aIdentifier.length) { return false; }

		for (int index = 0; index < aIdentifier.length; index++)
		{
			if (aData[index + aOffset] != aIdentifier[index]) { return false; }
		}
		return true;
	}

	private void loadM3GSectionData() throws IOException
	{
		while (dis.available() > 0)
		{
			int objectType = readByte();
			int length = readInt();
			bytesRead = 0;

			if (objectType == 0) // M3G Header
			{
				int versionHigh = readByte();
				int versionLow = readByte();
				boolean hasExternalReferences = readBoolean();
				int totalFileSize = readInt();
				int approximateContentSize = readInt();
				String authoringField = readString();
			}
			else if (objectType == 1) // AnimationController
			{
				SuperObject sObj = loadObject3D();
				AnimationController cont = new AnimationController();
				sObj.applyToObject3D(cont);

				float speed = readFloat();
				float weight = readFloat();
				int start = readInt();
				int end = readInt();
				cont.setActiveInterval(start, end);
				float referenceSeqTime = readFloat();
				int referenceWorldTime = readInt();
				cont.setPosition(referenceSeqTime, referenceWorldTime);
				cont.setSpeed(speed, referenceWorldTime);
				cont.setWeight(weight);
				objs.addElement(cont);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 2) // AnimationTrack
			{
				SuperObject sObj = loadObject3D();
				KeyframeSequence ks = (KeyframeSequence) getObject(readInt());
				AnimationController cont = (AnimationController) getObject(readInt());
				int property = readInt();
				AnimationTrack track = new AnimationTrack(ks, property);
				track.setController(cont);

				sObj.applyToObject3D(track);

				objs.addElement(track);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 3) // Appearance
			{
				SuperObject sObj = loadObject3D();
				Appearance appearance = new Appearance();
				sObj.applyToObject3D(appearance);

				appearance.setLayer(readByte());
				appearance.setCompositingMode((CompositingMode) getObject(readInt()));
				appearance.setFog((Fog) getObject(readInt()));
				appearance.setPolygonMode((PolygonMode) getObject(readInt()));
				appearance.setMaterial((Material) getObject(readInt()));
				int numTextures = readInt();
				Object tex;

				for (int i = 0; i < numTextures; ++i)
				{
					tex = getObject(readInt());
					appearance.setTexture(i, tex != null ? (Texture2D) tex : null);
				}
				objs.addElement(appearance);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 4) // Background
			{
				SuperObject sObj = loadObject3D();
				Background background = new Background();
				sObj.applyToObject3D(background);

				background.setColor(readRGBA());
				Object bgImage = getObject(readInt());
				if (bgImage != null) { background.setImage((Image2D) bgImage); }

				int modeX = readByte();
				int modeY = readByte();
				background.setImageMode(modeX, modeY);
				int cropX = readInt();
				int cropY = readInt();
				int cropWidth = readInt();
				int cropHeight = readInt();
				background.setCrop(cropX, cropY, cropWidth, cropHeight);
				background.setDepthClearEnable(readBoolean());
				background.setColorClearEnable(readBoolean());
				objs.addElement(background);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 5) // Camera
			{
				SuperObject sObj = loadNode();
				Camera camera = new Camera();
				sObj.applyToNode(camera);

				int projectionType = readByte();
				if (projectionType == Camera.GENERIC)
				{
					Transform t = new Transform();
					t.set(readMatrix());
					camera.setGeneric(t);
				}
				else
				{
					float fovy = readFloat();
					float aspect = readFloat();
					float near = readFloat();
					float far = readFloat();
					if (projectionType == Camera.PARALLEL) { camera.setParallel(fovy, aspect, near, far); }
					else { camera.setPerspective(fovy, aspect, near, far); }
				}
				objs.addElement(camera);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 6) // CompositingMode
			{
				SuperObject sObj = loadObject3D();
				CompositingMode compositingMode = new CompositingMode();
				sObj.applyToObject3D(compositingMode);

				compositingMode.setDepthTestEnable(readBoolean());
				compositingMode.setDepthWriteEnable(readBoolean());
				compositingMode.setColorWriteEnable(readBoolean());
				compositingMode.setAlphaWriteEnable(readBoolean());
				compositingMode.setBlending(readByte());
				compositingMode.setAlphaThreshold((float) readByte() / 255.0f);
				compositingMode.setDepthOffset(readFloat(), readFloat());
				objs.addElement(compositingMode);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 7) // Fog
			{
				SuperObject sObj = loadObject3D();
				Fog fog = new Fog();
				sObj.applyToObject3D(fog);

				fog.setColor(readRGB());
				fog.setMode(readByte());
				if (fog.getMode() == Fog.EXPONENTIAL) { fog.setDensity(readFloat()); }
				else { fog.setLinear(readFloat(), readFloat()); }
				objs.addElement(fog);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 8) // PolygonMode
			{
				SuperObject sObj = loadObject3D();
				PolygonMode polygonMode = new PolygonMode();
				sObj.applyToObject3D(polygonMode);

				polygonMode.setCulling(readByte());
				polygonMode.setShading(readByte());
				polygonMode.setWinding(readByte());
				polygonMode.setTwoSidedLightingEnable(readBoolean());
				polygonMode.setLocalCameraLightingEnable(readBoolean());
				polygonMode.setPerspectiveCorrectionEnable(readBoolean());
				objs.addElement(polygonMode);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 9) // Group
			{
				SuperObject sObj = loadGroup();
				Group group = new Group();
				sObj.applyToGroup(group, this);

				objs.addElement(group);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 10) // Image2D
			{
				SuperObject sObj = loadObject3D();
				Image2D image = null;
				int format = readByte();
				boolean isMutable = readBoolean();
				int width = readInt();
				int height = readInt();
				if (!isMutable)
				{
					int paletteSize = readInt();
					byte[] palette = null;
					if (paletteSize > 0)
					{
						palette = new byte[paletteSize];
						dis.readFully(palette);
						bytesRead += paletteSize;
					}

					int pixelSize = readInt();
					byte[] pixel = new byte[pixelSize];
					dis.readFully(pixel);
					bytesRead += pixelSize;
					if (palette != null) { image = new Image2D(format, width, height, pixel, palette); }
					else { image = new Image2D(format, width, height, pixel); }
				}
				else { image = new Image2D(format, width, height); }

				sObj.applyToObject3D(image);

				objs.addElement(image);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 11) // TriangleStripArray
			{
				SuperObject sObj = loadObject3D();

				int encoding = readByte();
				int firstIndex = 0;
				int[] indices = null;
				if (encoding == 0) { firstIndex = readInt(); }
				else if (encoding == 1) { firstIndex = readByte(); }
				else if (encoding == 2) { firstIndex = readShort(); }
				else if (encoding == 128)
				{
					int numIndices = readInt();
					indices = new int[numIndices];
					for (int i = 0; i < numIndices; ++i) { indices[i] = readInt(); }
				}
				else if (encoding == 129)
				{
					int numIndices = readInt();
					indices = new int[numIndices];
					for (int i = 0; i < numIndices; ++i) { indices[i] = readByte(); }
				}
				else if (encoding == 130)
				{
					int numIndices = readInt();
					indices = new int[numIndices];
					for (int i = 0; i < numIndices; ++i) { indices[i] = readShort(); }
				}

				int numStripLengths = readInt();
				int[] stripLengths = new int[numStripLengths];
				for (int i = 0; i < numStripLengths; i++) { stripLengths[i] = readInt(); }

				TriangleStripArray triStrip = null;
				if (indices == null) { triStrip = new TriangleStripArray(firstIndex, stripLengths); }
				else { triStrip = new TriangleStripArray(indices, stripLengths); }

				sObj.applyToObject3D(triStrip);

				objs.addElement(triStrip);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 12) // Light
			{
				SuperObject sObj = loadNode();
				Light light = new Light();
				sObj.applyToNode(light);

				float constant = readFloat();
				float linear = readFloat();
				float quadratic = readFloat();
				light.setAttenuation(constant, linear, quadratic);
				light.setColor(readRGB());
				light.setMode(readByte());
				light.setIntensity(readFloat());
				light.setSpotAngle(readFloat());
				light.setSpotExponent(readFloat());
				objs.addElement(light);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 13) // Material
			{
				SuperObject sObj = loadObject3D();
				Material material = new Material();
				sObj.applyToObject3D(material);

				material.setColor(Material.AMBIENT, readRGB());
				material.setColor(Material.DIFFUSE, readRGBA());
				material.setColor(Material.EMISSIVE, readRGB());
				material.setColor(Material.SPECULAR, readRGB());
				material.setShininess(readFloat());
				material.setVertexColorTrackingEnable(readBoolean());
				objs.addElement(material);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 14) // Mesh
			{
				SuperObject sObj = loadNode();

				VertexBuffer vertices = (VertexBuffer) getObject(readInt());
				int subMeshCount = readInt();

				IndexBuffer[] submeshes = new IndexBuffer[subMeshCount];
				Appearance[] appearances = new Appearance[subMeshCount];
				Object ap = null;

				for (int i = 0; i < subMeshCount; ++i)
				{
					submeshes[i] = (IndexBuffer) getObject(readInt());
					ap = getObject(readInt());
					appearances[i] = ap != null ? (Appearance) ap : null;
				}
				Mesh mesh = new Mesh(vertices, submeshes, appearances);

				sObj.applyToNode(mesh);

				objs.addElement(mesh);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 15) // MorphingMesh
			{
				SuperObject sObj = loadNode();
				VertexBuffer vb = (VertexBuffer) getObject(readInt());
				int subMeshCount = readInt();
				IndexBuffer[] ib = new IndexBuffer[subMeshCount];
				Appearance[] aps = new Appearance[subMeshCount];
				Object ap = null;

				for (int i = 0; i < subMeshCount; i++)
				{
					ib[i] = (IndexBuffer) getObject(readInt());
					ap = getObject(readInt());
					aps[i] = ap != null ? (Appearance) ap : null;
				}

				int targetCount = readInt();
				float[] weights = new float[targetCount];
				VertexBuffer[] targets = new VertexBuffer[targetCount];

				for (int i = 0; i < targetCount; i++)
				{
					targets[i] = (VertexBuffer) getObject(readInt());
					weights[i] = readFloat();
				}

				MorphingMesh mesh = new MorphingMesh(vb, targets, ib, aps);
				mesh.setWeights(weights);

				sObj.applyToNode(mesh);

				objs.addElement(mesh);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 16) // SkinnedMesh
			{
				SuperObject sObj = loadNode();
				VertexBuffer vb = (VertexBuffer) getObject(readInt());
				int subMeshCount = readInt();
				IndexBuffer[] ib = new IndexBuffer[subMeshCount];
				Appearance[] aps = new Appearance[subMeshCount];
				Object ap = null;

				for (int i = 0; i < subMeshCount; i++)
				{
					ib[i] = (IndexBuffer) getObject(readInt());
					ap = getObject(readInt());
					aps[i] = ap != null ? (Appearance) ap : null;
				}

				Group skeleton = (Group) getObject(readInt());

				SkinnedMesh mesh = new SkinnedMesh(vb, ib, aps, skeleton);
				int transformReferenceCount = readInt();

				for (int i = 0; i < transformReferenceCount; i++)
				{
					Node bone = (Node) getObject(readInt());
					int firstVertex = readInt();
					int vertexCount = readInt();
					int weight = readInt();
					mesh.addTransform(bone, weight, firstVertex, vertexCount);
				}

				sObj.applyToNode(mesh);

				objs.addElement(mesh);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 17) // Texture2D
			{
				SuperObject sObj = loadTransformable();
				Texture2D texture = new Texture2D((Image2D) getObject(readInt()));
				texture.setBlendColor(readRGB());
				texture.setBlending(readByte());
				int wrapS = readByte();
				int wrapT = readByte();
				texture.setWrapping(wrapS, wrapT);
				int levelFilter = readByte();
				int imageFilter = readByte();
				texture.setFiltering(levelFilter, imageFilter);

				sObj.applyToTransformable(texture);

				objs.addElement(texture);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 18) // Sprite3D
			{
				SuperObject sObj = loadNode();
				Image2D image = (Image2D) getObject(readInt()); // Image cannot be null
				Object ap = getObject(readInt());
				Sprite3D sprite = new Sprite3D(readBoolean(), image, ap != null ? (Appearance) ap : null);
				int x = readInt();
				int y = readInt();
				int width = readInt();
				int height = readInt();
				sprite.setCrop(x, y, width, height);

				sObj.applyToNode(sprite);

				objs.addElement(sprite);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 19) // KeyframeSequence
			{
				SuperObject sObj = loadObject3D();
				int interpolation = readByte();
				int repeatMode = readByte();
				int encoding = readByte();
				int duration = readInt();
				int rangeFirst = readInt();
				int rangeLast = readInt();
				int components = readInt();
				int keyFrames = readInt();

				KeyframeSequence seq = new KeyframeSequence(keyFrames, components, interpolation);
				seq.setRepeatMode(repeatMode);
				seq.setDuration(duration);
				seq.setValidRange(rangeFirst, rangeLast);
				float[] values = new float[components];
				if (encoding == 0)
				{
					for (int i = 0; i < keyFrames; i++)
					{
						int time = readInt();

						for (int j = 0; j < components; j++) { values[j] = readFloat(); }

						seq.setKeyframe(i, time, values);
					}
				}
				else
				{
					float[] vectorBiasScale = new float[components * 2];
					for (int i = 0; i < components; i++) { vectorBiasScale[i] = readFloat(); }

					for (int i = 0; i < components; i++) { vectorBiasScale[i + components] = readFloat(); }

					for (int i = 0; i < keyFrames; i++)
					{
						int time = readInt();
						if (encoding == 1)
						{
							for (int j = 0; j < components; j++)
							{
								int v = readByte();
								values[j] = vectorBiasScale[j] + ((vectorBiasScale[j + components] * v) / 255.0f);
							}
						}
						else
						{
							for (int j = 0; j < components; j++)
							{
								int v = readShort();
								values[j] = vectorBiasScale[j] + ((vectorBiasScale[j + components] * v) / 65535.0f);
							}
						}
						seq.setKeyframe(i, time, values);
					}
				}

				sObj.applyToObject3D(seq);

				objs.addElement(seq);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 20) // VertexArray
			{
				SuperObject sObj = loadObject3D();

				int componentSize = readByte();
				int components = readByte();
				int encoding = readByte();
				int vertices = readShort();

				VertexArray va = new VertexArray(vertices, components, componentSize);
				int size = vertices * components;

				if (componentSize == 1)
				{
					byte[] values = new byte[size];
					if (encoding == 0)
					{
						dis.readFully(values);
						bytesRead += size;
					}
					else
					{
						byte last = 0;
						for (int i = 0; i < size; ++i)
						{
							last += (byte) readByte();
							values[i] = last;
						}
					}
					va.set(0, vertices, values);
				}
				else
				{
					short last = 0;
					short[] values = new short[size];
					for (int i = 0; i < size; ++i)
					{
						if (encoding == 0) { values[i] = (short) readShort(); }
						else
						{
							last += (short) readShort();
							values[i] = last;
						}
					}
					va.set(0, vertices, values);
				}

				sObj.applyToObject3D(va);

				objs.addElement(va);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 21) // VertexBuffer
			{
				SuperObject sObj = loadObject3D();
				VertexBuffer vertices = new VertexBuffer();
				sObj.applyToObject3D(vertices);

				vertices.setDefaultColor(readRGBA());

				VertexArray positions = (VertexArray) getObject(readInt());
				float[] bias = new float[3];
				bias[0] = readFloat();
				bias[1] = readFloat();
				bias[2] = readFloat();
				float scale = readFloat();
				vertices.setPositions(positions, scale, bias);

				vertices.setNormals((VertexArray) getObject(readInt()));
				vertices.setColors((VertexArray) getObject(readInt()));

				int texCoordArrayCount = readInt();
				for (int i = 0; i < texCoordArrayCount; ++i)
				{
					VertexArray texcoords = (VertexArray) getObject(readInt());
					bias[0] = readFloat();
					bias[1] = readFloat();
					bias[2] = readFloat();
					scale = readFloat();
					vertices.setTexCoords(i, texcoords, scale, bias);
				}

				objs.addElement(vertices);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 22) // World
			{
				SuperObject sObj = loadGroup();
				World world = new World();

				sObj.applyToGroup(world, this);
				world.setActiveCamera((Camera) getObject(readInt()));
				Object bg = getObject(readInt());
				world.setBackground(bg != null ? (Background) bg : null);
				objs.addElement(world);
				roots.addElement("" + (objs.size()-1));
			}
			else if (objectType == 255) // External reference
				{ loadExternalReference(); }
			else { Mobile.log(Mobile.LOG_WARNING, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Unsupported objectType " + objectType + "."); }

			if (bytesRead != length) { Mobile.log(Mobile.LOG_WARNING, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Length mismatch, expected: " + length + ", bytesRead: " + bytesRead + ", objectType: " + objectType); }
		}
	}

	private Object3D[] loadM3G() throws IOException
	{
		this.objs = new Vector<Object3D>();
		this.roots = new Vector<String>();
		this.objs.addElement(null); // Index 0 always means a null object
		this.objs.addElement(null); // Index 1 is the header, which is not a valid object.

		this.bytesRead = 0;

		// First section must be header
		int compressionScheme = readByte();
		int totalSectionLength = readInt();
		int uncompressedLength = readInt();

		int objectType = readByte();
		int length = readInt();

		int versionHigh = readByte();
		int versionLow = readByte();
		boolean hasExternalReferences = readBoolean();
		int totalFileSize = readInt();
		int approximateContentSize = readInt();
		String authoringField = readString();
		int checkSum = readInt();

		int read = bytesRead + M3G_FILE_IDENTIFIER.length;

		while (read < totalFileSize)
		{
		//while (dis.available() > 0) {
			compressionScheme = readByte();
			totalSectionLength = readInt();
			uncompressedLength = readInt();

			byte[] uncompressedData = new byte[uncompressedLength];

			if (compressionScheme == 0) { dis.readFully(uncompressedData); }
			else if (compressionScheme == 1)
			{
				int compressedLength = totalSectionLength - 13;
				byte[] compressedData = new byte[compressedLength];
				dis.readFully(compressedData);

				Inflater decompresser = new Inflater();
				decompresser.setInput(compressedData, 0, compressedLength);
				int resultLength = 0;
				try { resultLength = decompresser.inflate(uncompressedData); }
				catch (Exception e) { e.printStackTrace(); }
				decompresser.end();

				if (resultLength != uncompressedLength) { throw new IOException("Unable to decompress data."); }
			}
			else { throw new IOException("Unknown compression scheme."); }

			checkSum = readInt();

			new Loader(uncompressedData, objs, roots, this.resDir, this.activeRefs).loadM3GSectionData();

			read += totalSectionLength;
		}

		dis.close();

		// Return only the root level objects, that is, those not referenced by any other objects.
		Object3D[] rootObjs = new Object3D[roots.size()];
		for(int i = 0; i < rootObjs.length; i++) { rootObjs[i] = objs.get(Integer.parseInt(roots.get(i))); }

		return rootObjs;
	}


	private Object3D[] load() throws IOException
	{
		try
		{
			// Check header
			dis.mark(12);
			byte[] identifier = new byte[12];
			int read = dis.read(identifier, 0, 12);
			int type = getIdentifierType(identifier, 0);
			dis.reset();

			if (type == M3G_TYPE)
			{
				dis.skipBytes(M3G_FILE_IDENTIFIER.length);
				return loadM3G();
			}
			else if (type == PNG_TYPE) { return loadPNG(); }
			else if (type == JPEG_TYPE) { return loadJPEG(); }
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "Exception: " + e.getMessage());
			e.printStackTrace();
			throw new IOException("Invalid M3G data.");
		}
		finally
		{
			// Remove the path from active references before throwing an
			// exception. Some apps expecti this and catch, so we must keep
			// the active references array valid
			if (this.resName != null && this.activeRefs != null)
				{ this.activeRefs.removeElement(this.resName); }
		}

		return null;
	}

	private int readByte() throws IOException
	{
		bytesRead++;
		return dis.readUnsignedByte();
	}

	private int readShort() throws IOException
	{
		int a = readByte();
		int b = readByte();
		return (b << 8) | a;
	}

	private int readRGB() throws IOException
	{
		byte r = dis.readByte();
		byte g = dis.readByte();
		byte b = dis.readByte();
		bytesRead += 3;

		return (r << 16) | (g << 8) | b;
	}

	// Reads RGBA, returns ARGB for methods that use it (they expect ARGB)
	private int readRGBA() throws IOException
	{
		byte r = dis.readByte();
		byte g = dis.readByte();
		byte b = dis.readByte();
		byte a = dis.readByte();
		bytesRead += 4;

		return a | (r << 24) | (g << 16) | (b << 8);
	}

	private float readFloat() throws IOException
	{
		return Float.intBitsToFloat(readInt());
	}

	private int readInt() throws IOException
	{
		int a = readByte();
		int b = readByte();
		int c = readByte();
		int d = readByte();
		int i = (d << 24) | (c << 16) | (b << 8) | a;
		return i;
	}

	private boolean readBoolean() throws IOException
	{
		return readByte() == 1;
	}

	private String readString() throws IOException
	{
		StringBuffer result = new StringBuffer();
		int i = 0;
		for (int c = readByte(); c != 0; c = readByte())
		{
			if ((c & 0x80) == 0) { result.append((char)(c & 0x00FF)); }
			else if ((c & 0xE0) == 0xC0)
			{
				int c2 = readByte();
				if ((c2 & 0xC0) != 0x80) { throw new IOException("Invalid UTF-8 string."); }
				else { result.append((char)(((c & 0x1F) << 6) | (c2 & 0x3F))); }
			}
			else if ((c & 0xF0) == 0xE0)
			{
				int c2 = readByte();
				int c3 = readByte();
				if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80)) { throw new IOException("Invalid UTF-8 string."); }
				else { result.append((char)(((c & 0x0F) << 12) | ((c2 & 0x3F) <<6) | (c3 & 0x3F))); }
			}
			else if ((c & 0xF8) == 0xF0)
			{
				int c2 = readByte();
				int c3 = readByte();
				int c4 = readByte();
				if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80) || ((c4 & 0xC0) != 0x80)) { throw new IOException("Invalid UTF-8 string."); }
				int codePoint = ((c & 0x07) << 18) | ((c2 & 0x3F) << 12) | ((c3 & 0x3F) << 6) | (c4 & 0x3F);

				// Convert code point to UTF-16 surrogate pair
				codePoint -= 0x10000;
				result.append((char) (0xD800 | (codePoint >> 10)));
				result.append((char) (0xDC00 | (codePoint & 0x3FF)));
			}
			else { throw new IOException("Invalid UTF-8 string."); }
		}

		String ret = result.toString();
		Mobile.log(Mobile.LOG_DEBUG, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "String: " + ret);
		return ret;
	}

	private float[] readMatrix() throws IOException
	{
		float[] m = new float[16];
		for (int i = 0; i < 16; ++i) { m[i] = readFloat(); }
		return m;
	}

	private Object getObject(int index)
	{
		// getObject is pretty much only used to get a reference for the object that's currently at the last index of objs, so we don't need to track each object's index
		if(index >= objs.size())
		{
			Mobile.log(Mobile.LOG_ERROR, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": " + "App tried to get an Object3D array that's higher than the currently parsed Object3D's index. Idx:" + index + " maxIdx:" + objs.size());
			throw new IllegalArgumentException("Cannot get an M3G Object3D index that's higher than the current object's index");
		}
		if(index == 1) { throw new IllegalArgumentException("Header index is not a valid object!"); }
		else
		{
			// The referenced object is no longer root-level.
			roots.remove("" + index);
			return objs.get(index);
		}
	}

	private void loadExternalReference() throws IOException
	{
		String uri = readString();
		Object3D[] extObjects = null;

		// Resolve URI against current resDir context
		String targetPath;
		if (uri.startsWith("/")) { targetPath = uri; }
		else
		{
			if (resDir == null || resDir.length() == 0)
				{ targetPath = uri.startsWith("/") ? uri : "/" + uri; }

			targetPath = resDir + uri;
		}

		Mobile.log(Mobile.LOG_DEBUG, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": Loading External Reference from: " + targetPath);

		try { extObjects = new Loader(targetPath, this.activeRefs).load(); }
		catch (IOException e)
		{
			// Fallback attempt: if relative lookup failed, try root-relative
			if (!uri.startsWith("/"))
			{
				Mobile.log(Mobile.LOG_WARNING, Loader.class.getPackage().getName() + "." + Loader.class.getSimpleName() + ": Relative load failed. Trying fallback /" + uri);
				extObjects = new Loader("/" + uri, this.activeRefs).load();
			}
			else { throw new IOException("Failed to load resource:" + e.getMessage()); }
		}

		if (extObjects != null && extObjects.length > 0)
		{
			// Place primary root external object into reserved objs index
			Object3D mainExtObj = extObjects[0];
			objs.addElement(mainExtObj);

			int extIdx = objs.size() - 1;
			roots.addElement(String.valueOf(extIdx));
		}
		else
		{
			throw new IOException("Could not load external resource: " + uri);
		}
	}

	// Those two vectors will always house AnimationTracks and Object3Ds
	@SuppressWarnings("unchecked")
	private SuperObject loadObject3D() throws IOException
	{
		SuperObject sObj = new SuperObject();
		sObj.userID = readInt();

		int animationTracksCount = readInt();
		for (int i = 0; i < animationTracksCount; ++i)
		{
			sObj.animationTracks.addElement((AnimationTrack) getObject(readInt()));
		}

		int userParams = readInt();
		if (userParams != 0) {
			Hashtable hashtable = new Hashtable();
			for (int i = 0; i < userParams; ++i) {
				int parameterID = readInt();
				int numBytes = readInt();
				byte[] parameterBytes = new byte[numBytes];
				dis.readFully(parameterBytes);
				bytesRead += numBytes;

				hashtable.put(new Integer(parameterID), parameterBytes);
			}
			sObj.userObjects = hashtable;
		}
		return sObj;
	}

	private SuperObject loadTransformable() throws IOException
	{
		SuperObject sObj = loadObject3D();
		sObj.hasComponentTransform = readBoolean();
		if (sObj.hasComponentTransform)
		{
			sObj.tx = readFloat();
			sObj.ty = readFloat();
			sObj.tz = readFloat();
			sObj.sx = readFloat();
			sObj.sy = readFloat();
			sObj.sz = readFloat();
			sObj.angle = readFloat();
			sObj.ax = readFloat();
			sObj.ay = readFloat();
			sObj.az = readFloat();
		}
		sObj.hasGeneralTransform = readBoolean();
		if (sObj.hasGeneralTransform)
		{
			sObj.transform = new Transform();
			sObj.transform.set(readMatrix());
		}
		return sObj;
	}

	private SuperObject loadNode() throws IOException
	{
		SuperObject sObj = loadTransformable();
		sObj.renderingEnable = readBoolean();
		sObj.pickingEnable = readBoolean();
		int alpha = readByte();
		sObj.alphaFactor = (float) alpha / 255.0f;
		sObj.scope = readInt();
		sObj.hasAlignment = readBoolean();
		if (sObj.hasAlignment)
		{
			sObj.zTarget = readByte();
			sObj.yTarget = readByte();
			sObj.zReference = (Node) getObject(readInt());
			sObj.yReference = (Node) getObject(readInt());
		}
		return sObj;
	}

	private SuperObject loadGroup() throws IOException
	{
		SuperObject sObj = loadNode();
		int count = readInt();
		sObj.childIDs = new int[count];
		for (int i = 0; i < count; ++i)
		{
			sObj.childIDs[i] = readInt();
		}
		return sObj;
	}

	// This is a superclass (in the literal sense) that holds all fields of
	// Object3D, Transformable, Node, Group all at once. Used for composite objects
	// like MorphingMesh, Camera, etc. so we don't have to backtrack and check for
	// animationTrack compatibility in a different way.
	private static class SuperObject
	{
		// Object3D fields
		int userID;
		Vector animationTracks = new Vector();
		Hashtable userObjects;

		// Transformable fields
		boolean hasComponentTransform;
		float tx, ty, tz;
		float sx = 1.0f, sy = 1.0f, sz = 1.0f;
		float angle, ax, ay, az = 1.0f;
		boolean hasGeneralTransform;
		Transform transform;

		// Node fields
		boolean renderingEnable = true;
		boolean pickingEnable = true;
		float alphaFactor = 1.0f;
		int scope = -1;
		boolean hasAlignment;
		int zTarget, yTarget;
		Node zReference, yReference;

		// Group fields
		int[] childIDs;

		public void applyToObject3D(Object3D obj)
		{
			obj.setUserID(this.userID);
			for (int i = 0; i < this.animationTracks.size(); i++)
			{
				obj.addAnimationTrack((AnimationTrack) this.animationTracks.elementAt(i));
			}
			if (this.userObjects != null)
			{
				obj.setUserObject(this.userObjects);
			}
		}

		public void applyToTransformable(Transformable t)
		{
			applyToObject3D(t);
			if (this.hasComponentTransform)
			{
				t.setTranslation(tx, ty, tz);
				t.setScale(sx, sy, sz);
				t.setOrientation(angle, ax, ay, az);
			}
			if (this.hasGeneralTransform && this.transform != null)
			{
				t.setTransform(this.transform);
			}
		}

		public void applyToNode(Node node)
		{
			applyToTransformable(node);
			node.setRenderingEnable(this.renderingEnable);
			node.setPickingEnable(this.pickingEnable);
			node.setAlphaFactor(this.alphaFactor);
			node.setScope(this.scope);
			if (this.hasAlignment)
			{
				node.setAlignment(this.zReference, this.zTarget, this.yReference, this.yTarget);
			}
		}

		public void applyToGroup(Group group, Loader loader)
		{
			applyToNode(group);
			if (this.childIDs != null)
			{
				for (int i = 0; i < this.childIDs.length; i++)
				{
					Node child = (Node) loader.getObject(this.childIDs[i]);
					if (child != null)
					{
						group.addChild(child);
					}
				}
			}
		}
	}
}
