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

import java.util.Hashtable;

import javax.microedition.lcdui.Graphics;

import java.util.ArrayList;
import java.util.Arrays;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;

public class Graphics3D
{
	// Flag values for FJ2ME+ rendering overrides (bilinear, AA, dithering, etc)
	public static final int MODE_FORCE_DISABLE = 0;
	public static final int MODE_APP_CONTROLLED = 1;
	public static final int MODE_FORCE_ENABLE  = 2;

	// Dither pattern matrix (fast ordered dithering)
	private static final byte[] BAYER_PATTERN =
	{
		-8,  0, -6,  2,
		 4, -4,  6, -2,
		-5,  3, -7,  1,
		 7, -1,  5, -3
	};

	// Special blend modes for fog and AA coverage
	public static final int BLEND_FOG = -1;
	public static final int BLEND_COVERAGE = -2;

	public static final int ANTIALIAS = 2;
	public static final int DITHER = 4;
	public static final int OVERWRITE = 16; // This is unused here, as SW rasterization gives us direct control over pixels
	public static final int TRUE_COLOR = 8; // Also unused here, we always render at true color


	public static final boolean SUPPORT_ANTIALIASING = true;
	public static final boolean SUPPORT_TRUE_COLOR = true;
	public static final boolean SUPPORT_DITHERING = true;
	public static final boolean SUPPORT_MIPMAPPING = true;
	public static final boolean SUPPORT_PERSPECTIVE_CORRECTION = true;
	public static final boolean SUPPORT_LOCAL_CAMERA_LIGHTING = true;
	public static final int MAX_LIGHTS = 32;
	public static final int MAX_VIEWPORT_WIDTH = 1024;
	public static final int MAX_VIEWPORT_HEIGHT = 1024;
	public static final int MAX_VIEWPORT_DIMENSION = 1024;
	public static final int MAX_TEXTURE_DIMENSION = 2048;
	public static final int MAX_SPRITE_CROP_DIMENSION = 1024;
	public static final int MAX_TRANSFORMS_PER_VERTEX = 16;
	public static final int NUM_TEXTURE_UNITS = 4;
	private static Hashtable properties;

	// Render target
	private Object target;

	private static Graphics3D instance = null;

	// Viewport
	private int viewx;
	private int viewy;
	private int vieww;
	private int viewh;

	private boolean depthEnabled;
	private short[] depthBuffer;
	private float near;
	private float far;

	private int hints;

	private Camera currCam;
	private Transform currCamTrans;
	private Transform currCamTransInv;
	private ArrayList<Light> currLights;
	private ArrayList<Transform> currLightTrans;
	private Transform camTr;

	// Reusable rendering variables
	int canvasWidth, canvasHeight, paintPixel;
	int[] rasterData;
	final CompositingMode defaultCompositing;

	// Texturing
	final Transform texcomptr;
	boolean[] useBilinear = new boolean[NUM_TEXTURE_UNITS];
	final float[] texScaleBias = new float[4];
	final Transform[] textr = new Transform[NUM_TEXTURE_UNITS];
	final Texture2D[] textures = new Texture2D[NUM_TEXTURE_UNITS];
	final boolean[] texRepeatS = new boolean[NUM_TEXTURE_UNITS];
	final boolean[] texRepeatT = new boolean[NUM_TEXTURE_UNITS];
	final float[] sL = new float[NUM_TEXTURE_UNITS];
	final float[] sR = new float[NUM_TEXTURE_UNITS];
	final float[] tL = new float[NUM_TEXTURE_UNITS];
	final float[] tR = new float[NUM_TEXTURE_UNITS];
	final float[] sBot = new float[NUM_TEXTURE_UNITS];
	final float[] tBot = new float[NUM_TEXTURE_UNITS];
	final float[] sMidL = new float[NUM_TEXTURE_UNITS];
	final float[] tMidL = new float[NUM_TEXTURE_UNITS];
	final float[] sMidR = new float[NUM_TEXTURE_UNITS];
	final float[] tMidR = new float[NUM_TEXTURE_UNITS];
	final float[] sTop = new float[NUM_TEXTURE_UNITS];
	final float[] tTop = new float[NUM_TEXTURE_UNITS];
	final float[][] coS = new float[NUM_TEXTURE_UNITS][3];
	final float[][] coT = new float[NUM_TEXTURE_UNITS][3];
	float[] sStepX = new float[NUM_TEXTURE_UNITS];
	float[] sStepY = new float[NUM_TEXTURE_UNITS];
	float[] tStepX = new float[NUM_TEXTURE_UNITS];
	float[] tStepY = new float[NUM_TEXTURE_UNITS];
	float[][] texVerts = new float[NUM_TEXTURE_UNITS][];

	// Vertex color blending
	int alpha, r, g, b;

	// 3D rendering variables
	final Transform normalMatrix;
	final Transform posLocalToEye;
	final Transform tr;
	int yStart, yEnd;
	final int[] ord = new int[3];
	float[] vertClip = null;
	float[] eyePos = null;
	float[] lightEyePos = null;
	float[] lightEyeDir = null;
	final float[] lightVec = new float[4];
	final float[] coX = new float[3];
	final float[] coY = new float[3];
	final float[] coZ = new float[3];
	final float[] coW = new float[3];
	float xTop, yTop, zTop;
	float xMidL, yMid, zMidL;
	float xBot, yBot, zBot;
	float rHorizon, xMidR, zMidR;
	float pwTop, pwMidL, pwBot, pwMidR;
	float dwdx, dwdy;

	float rStepX = 0, gStepX = 0, bStepX = 0, aStepX = 0;
	float rStepY = 0, gStepY = 0, bStepY = 0, aStepY = 0;
	float deltaR = 0, deltaG = 0, deltaB = 0, deltaA = 0;

	final float[] scaleBias = new float[4];

	final Transform projectionMatrix = new Transform();
	final int[] renderableTriangles = {0}; // Counter for visible triangles

	// fog blending factor
	float fogFactor = 0.0f;

	public Graphics3D()
	{
		/*
		 * The default depth range used is that of window coordinates, so 0 to near, and 1 to far
		 * JSR-184 specifies that Normalized Device Coordinates (NDC) can also be used, which ranges from -1 to 1.
		 */
		this.defaultCompositing = new CompositingMode();
		this.near = 0f;
		this.far = 1f;
		this.currCam = null;
		this.currCamTrans = new Transform();
		this.currCamTransInv = new Transform();
		this.currLights = new ArrayList<Light>();
		this.currLightTrans = new ArrayList<Transform>();
		camTr = new Transform();
		tr = new Transform();
		normalMatrix = new Transform();
		posLocalToEye = new Transform();
		texcomptr = new Transform();
		for(int i = 0; i < NUM_TEXTURE_UNITS; i++) { textr[i] = new Transform(); }
	}


	public int addLight(Light light, Transform transform)
	{
		/* As per JSR-184, addLight() must throw a NullPointerException if no light is given */
		if (light == null) { throw new NullPointerException("addLight() was called but no light object was provided."); }

		// Are we going over the maximum allowed lights? Clear the light at
		// the first position to make room for the new one
		if (this.currLights.size() == MAX_LIGHTS)
		{
			this.currLights.remove(0);
			this.currLightTrans.remove(0);
		}

		this.currLights.add(light);
		this.currLightTrans.add(transform == null ? new Transform() : new Transform(transform));
		return this.currLights.size() - 1;
	}

	public void bindTarget(Object target)
	{
		/* Calls the method below specifying the depth buffer as enabled, and no render hints, as per JSR-184. */
		this.bindTarget(target, true, 0);
	}

	public void bindTarget(Object target, boolean depthBuffer, int hints)
	{
		/*
		 * As per JSR-184, this function returns:
		 * NullPointerException: If no render target is received as argument
		 * IllegalStateException: If the current Graphics3D Object already has a render target
		 */
		if (target == null) { throw new NullPointerException("bindTarget() was called but no render target was provided."); }
		if (this.target != null) { throw new IllegalStateException("This Graphics3D object already has a render target."); }

		/* The target can be an Image2D Object, or a Graphics Object. */
		if (target instanceof Image2D)
		{
			Image2D i2d = (Image2D) target;

			/* JSR-184 specifies that Image2D render targets can only have RGB or RGBA format. */
			if (i2d.getFormat() != Image2D.RGB && i2d.getFormat() != Image2D.RGBA)
			{ throw new IllegalArgumentException("Received a 2D render target with invalid internal format"); }

			/* It's a 2D image, so paint the canvas with it starting from the top-left corner */
			this.viewx = 0;
			this.viewy = 0;
			this.vieww = i2d.getWidth();
			this.viewh = i2d.getHeight();
		}
		else if (target instanceof Graphics)
		{
			Graphics pgrp = (Graphics) target;
			// We can get the framebuffer directly from PlatformGraphics for less memory pressure
			rasterData = ((PlatformGraphics) pgrp).getFrameBuffer();
			canvasWidth = pgrp.getCanvas().getWidth();
			canvasHeight = pgrp.getCanvas().getHeight();
			this.viewx = M3GMath.max(0, pgrp.getClipX() + pgrp.getTranslateX());
			this.viewy = M3GMath.max(0, pgrp.getClipY() + pgrp.getTranslateY());
			this.vieww = M3GMath.min(canvasWidth, pgrp.getClipWidth());
			this.viewh = M3GMath.min(canvasHeight, pgrp.getClipHeight());
		} else
		{
			/* If it is neither of those, throw an IllegalArgumentException as per JSR-184. */
			throw new IllegalArgumentException("Received render target is neither an instance of Image2D nor Graphics");
		}

		/*
		 * The final check performed before binding throws IllegalArgumentException if:
		 * 1 - The render target's width is larger than the max supported.
		 * 2 - The render target's height is taller than the max supported.
		 * 3 - The render hint is an OR bitmask that matches with one or more of [ANTIALIAS, DITHER, TRUE_COLOR, OVERWRITE], or not zero.
		 */
		if (this.vieww > MAX_VIEWPORT_WIDTH || this.viewh > MAX_VIEWPORT_HEIGHT || (hints & ~(ANTIALIAS | DITHER | TRUE_COLOR | OVERWRITE)) != 0)
			{ throw new IllegalArgumentException("Render target either has larger dimensions than supported, or the render hint is invalid"); }

		this.target = target;
		this.depthBuffer = new short[this.vieww * this.viewh];
		Arrays.fill(this.depthBuffer, (short) M3GMath.round((this.far * 65520.0f) - 32760.0f));
		this.depthEnabled = depthBuffer;
		this.hints = hints;
	}

	public void clear(Background background)
	{
		/* As per JSR-184, throw IllegalStateException if this Graphics3D object does not have a render target. */
		if (this.target == null) { throw new IllegalStateException("Cannot clear Background on a Graphics3D without a render target."); }

		final int color = (background != null) ? background.getColor() : 0x00000000;
		final boolean clearColor = (background == null) || background.isColorClearEnabled();
		final boolean clearDepth = (background == null) || background.isDepthClearEnabled();

		/*
		 * If the background object is null:
		 * Color buffer is cleared to transparent black
		 * Depth buffer is cleared to the max depth value, 1.0.
		 */

		if (clearColor)
		{
			if (this.target instanceof Image2D)
			{
				Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Clear to Image2D not Implemented");
				Image2D i2d = (Image2D) this.target;

				// CHECK is the bg image used only if clearColor is true?

				if (background.getImage() == null || background.getImage().getFormat() != i2d.getFormat())
				{ throw new IllegalArgumentException("The background image to be cleared does not have the same format as the render target."); }

				// TODO support clearing Image2D
			}
			else if (this.target instanceof Graphics)
			{
				Graphics grp = (Graphics) this.target;

				/*
				 * As per JSR-184, clear() always affects the whole viewport: fill it with the
				 * background color first. The Background crop rectangle is a sampling window
				 * into the background image, NOT the destination rectangle.
				 */
				grp.setColor(color);
				grp.fillRect(viewx, viewy, vieww, viewh);

				// Draw the background's image if any (and there's a background)
				if(background != null && background.getImage() != null && false)
				{
					final Image2D bgImg = background.getImage();

					/* The crop rectangle (defaulting to the whole image) is mapped onto the
					 * viewport so that it fills it completely; the image mode governs sampling
					 * outside the image bounds (BORDER = background color, REPEAT = tile). */
					final int cropX = background.getCropX(), cropY = background.getCropY();
					int cropW = background.getCropWidth(), cropH = background.getCropHeight();
					if (cropW <= 0) { cropW = bgImg.getWidth(); }
					if (cropH <= 0) { cropH = bgImg.getHeight(); }
					final boolean repeatX = background.getImageModeX() == Background.REPEAT;
					final boolean repeatY = background.getImageModeY() == Background.REPEAT;
					final boolean doDither = (Mobile.m3gDitheringMode == MODE_FORCE_ENABLE)
						|| (Mobile.m3gDitheringMode == MODE_APP_CONTROLLED && (this.hints & DITHER) != 0);

					for (int py = 0; py < viewh; py++)
					{
						int sy = cropY + (int) (py * cropH / viewh);
						sy = wrapY(sy, bgImg.getHeight(), repeatY, bgImg.isPowerOfTwo(bgImg.getHeight()));

						for (int px = 0; px < vieww; px++)
						{
							int sx = cropX + (int) (px * cropW / vieww);
							sx = wrapY(sx, bgImg.getWidth(), repeatX, bgImg.isPowerOfTwo(bgImg.getWidth()));

							int paintPixel = bgImg.getPixel(sx, sy);

							// Dither available? Apply it to the BG Image.
							if (doDither)
							{
								int ditherOffset = BAYER_PATTERN[((sy & 3) << 2) | (sx & 3)];

								int r = (paintPixel >> 16) & 0xFF;
								int g = (paintPixel >>  8) & 0xFF;
								int b =  paintPixel & 0xFF;

								r += ditherOffset;
								g += ditherOffset;
								b += ditherOffset;

								if ((r & ~0xFF) != 0) r = (r < 0) ? 0 : 255;
								if ((g & ~0xFF) != 0) g = (g < 0) ? 0 : 255;
								if ((b & ~0xFF) != 0) b = (b < 0) ? 0 : 255;

								paintPixel = (paintPixel & 0xFF000000) | (r << 16) | (g << 8) | b;
							}

							// Image format argument shouldn't matter here
							rasterData[(py + viewy) * canvasWidth + (px + viewx)] =
								blendPixels(rasterData[(py + viewy) * canvasWidth + (px + viewx)], paintPixel,
									(paintPixel >>> 24), CompositingMode.ALPHA, 0, 0);
						}
					}
				}
			}
		}

		if (clearDepth) { Arrays.fill(this.depthBuffer, (short) M3GMath.round((this.far * 65520.0f) - 32760.0f)); }
	}

	public Camera getCamera(Transform transform)
	{
		if (transform != null) { transform.set(this.currCamTrans); }
		return this.currCam;
	}

	public float getDepthRangeFar() { return far; }

	public float getDepthRangeNear() { return near;}

	public int getHints() { return hints; }

	public static Graphics3D getInstance()
	{
		if( instance == null) { instance = new Graphics3D(); }
		return instance;
	}

	public Light getLight(int index, Transform transform)
	{
		/* As per JSR-184, throw IndexOutOfBoundsException if the requested light index is out of bounds. */
		if (index < 0 || index > this.currLights.size()) { throw new IndexOutOfBoundsException("The received light index is out of bounds."); }

		/* If a transform variable is received, use it to store the requested light's transform. */
		if (transform != null) { transform.set(this.currLightTrans.get(index)); }

		return this.currLights.get(index);
	}

	/* This is supposed to include nulls, so just return the size */
	public int getLightCount() { return this.currLights.size(); }

	public static Hashtable getProperties()
	{
		if (Graphics3D.properties != null)
			return Graphics3D.properties;

		Hashtable<String, Object> p = new Hashtable<String, Object>();
		p.put("supportAntialiasing", SUPPORT_ANTIALIASING);
		p.put("supportTrueColor", SUPPORT_TRUE_COLOR);
		p.put("supportDithering", SUPPORT_DITHERING);
		p.put("supportMipmapping", SUPPORT_MIPMAPPING);
		p.put("supportPerspectiveCorrection", SUPPORT_PERSPECTIVE_CORRECTION);
		p.put("supportLocalCameraLighting", SUPPORT_LOCAL_CAMERA_LIGHTING);
		p.put("maxLights", MAX_LIGHTS);
		p.put("maxViewportWidth", MAX_VIEWPORT_WIDTH);
		p.put("maxViewportHeight", MAX_VIEWPORT_HEIGHT);
		p.put("maxViewportDimension", MAX_VIEWPORT_DIMENSION);
		p.put("maxTextureDimension", MAX_TEXTURE_DIMENSION);
		p.put("maxSpriteCropDimension", MAX_SPRITE_CROP_DIMENSION);
		p.put("maxTransformsPerVertex", MAX_TRANSFORMS_PER_VERTEX);
		p.put("numTextureUnits", NUM_TEXTURE_UNITS);
		Graphics3D.properties = p;

		return Graphics3D.properties;
	}

	public static int getTextureUnitCount() { return NUM_TEXTURE_UNITS; }

	public Object getTarget() { return this.target; }

	public int getViewportHeight() { return viewh; }

	public int getViewportWidth() { return vieww; }

	public int getViewportX() { return viewx; }

	public int getViewportY() { return viewy; }

	public boolean isDepthBufferEnabled() { return this.depthEnabled; }

	public void releaseTarget()
	{
		/* Ignore the call if no render target is bound. */
		if(this.target != null)
		{
			/* If there is a render target, release it */
			this.target = null;
		}
	}

	public void render(World world)
	{
		/* As per JSR-184, throw NullPointerException if the received world is null. */
		if (world == null) { throw new NullPointerException("render(world) was called but no world was provided."); }

		/* Also per JSR-184, throw IllegalStateException this object has no render target yet. */
		if (this.target == null) { throw new IllegalStateException("render(world) was called but there is no render target."); }

		Camera worldCamera = world.getActiveCamera();

		if(worldCamera == null) { throw new IllegalStateException("Cannot render a world that has no active camera."); }

		camTr.setIdentity();
		if(!worldCamera.getTransformTo(world, camTr)) { throw new IllegalStateException("Active camera is not in world."); }

		/* Clear the background first */
		clear(world.getBackground());

		setCamera(worldCamera, camTr);
		resetLights();
		positionLights(world, world);

		render((Group) world, null);
	}

	public void render(Node node, Transform transform)
	{
		/* As per JSR-184, throw NullPointerException if no node is received. */
		if(node == null) { throw new NullPointerException("render() was called but no node was provided."); }

		/* Also per JSR-184, throw IllegalStateException if this method is called but there's no camera or render target available. */
		if (this.target == null || this.currCam == null) { throw new IllegalStateException("render() was called but there is no camera or render target."); }

		/* Also per JSR-184, throw IllegalStateException if if node is not a Sprite3D, Mesh, or Group Object. */
		if (!(node instanceof Mesh || node instanceof Sprite3D || node instanceof Group)) { throw new IllegalArgumentException("Node is not an instance of any of the following: Sprite3D, Mesh, Group"); }

		// Node not renderable? Skip it and its children.
		if(!node.isRenderingEnabled()) { return; }

		if (node instanceof Mesh)
		{
			Mesh mesh = (Mesh) node;
			int subMeshes = mesh.getSubmeshCount();
			VertexBuffer vertices = mesh.getVertexBuffer();
			for (int i = 0; i < subMeshes; i++)
			{
				if (mesh.getAppearance(i) != null) { render(vertices, mesh.getIndexBuffer(i), mesh.getAppearance(i), transform, node.getScope()); }
			}

			/*
			 * Per JSR-184, the skeleton group of a SkinnedMesh is a regular
			 * scene graph branch: it is traversed just like any other branch
			 * during rendering. This is what allows, for example, a character
			 * to render a separate weapon mesh attached to its hand bone.
			 */
			if (node instanceof SkinnedMesh)
			{
				Group skeleton = ((SkinnedMesh) node).getSkeleton();
				Transform sktr = new Transform();
				skeleton.getCompositeTransform(sktr);
				if (transform != null) { sktr.preMultiply(transform); }
				render(skeleton, sktr);
			}
		}
		else if (node instanceof Sprite3D) { renderSprite((Sprite3D) node, transform); }
		else if (node instanceof Group)
		{
			Node child = ((Group) node).firstChild;
			if (child != null)
			{
				do
				{
					if(child instanceof Sprite3D || child instanceof Mesh || child instanceof Group)
					{
						Transform nodetr = new Transform();
						child.getCompositeTransform(nodetr);
						if(transform != null) { nodetr.preMultiply(transform); }

						render(child, nodetr);
					}
					child = child.right;
				} while (child != ((Group) node).firstChild);
			}
		}
	}

	public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform)
	{ this.render(vertices, triangles, appearance, transform, -1); }

	public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform, int scope)
	{
		/* As per JSR-184, if vertices, triangles or appearence are null, throw a NullPointerException. */
		if (vertices == null || triangles == null || appearance == null) { throw new NullPointerException("Tried to render a submesh with incomplete info."); }

		/* Also per JSR-184, throw IllegalStateException if the application tries to render without having set up a render target or camera beforehand. */
		if (this.target == null || this.currCam == null) { throw new IllegalStateException("Tried to render a submesh without having a render target or camera first."); }

		/*
		 * JSR-184 scope culling: geometry is only rendered if its scope intersects the
		 * camera's scope. Games hide nodes by calling setScope(0) on them (e.g. pooled
		 * objects parked inside a Group), so ignoring this draws them all at the origin.
		 */
		if ((scope & this.currCam.getScope()) == 0) { return; }

		final int projType = this.currCam.getProjection((float []) null);

		final CompositingMode compositingMode = appearance.getCompositingMode() != null ? appearance.getCompositingMode() : this.defaultCompositing;

		final int shadingMode = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getShading() : PolygonMode.SHADE_SMOOTH;
		final Material material = appearance.getMaterial();
		final int cullingMode = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getCulling() : PolygonMode.CULL_BACK;
		final int windingOrder = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getWinding() : PolygonMode.WINDING_CCW;
		final boolean twoSidedLighting = appearance.getPolygonMode() != null ? appearance.getPolygonMode().isTwoSidedLightingEnabled() : false;
		final boolean localCameraLight = appearance.getPolygonMode() != null ? appearance.getPolygonMode().isLocalCameraLightingEnabled() : false;

		// This one can be overridden by FJ2ME+
		boolean perspectiveCorrection = appearance.getPolygonMode() != null ? appearance.getPolygonMode().isPerspectiveCorrectionEnabled() : false;
		perspectiveCorrection = perspectiveCorrection && (projType == Camera.PERSPECTIVE);
		perspectiveCorrection = (Mobile.m3gPerspectiveCorrectionMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gPerspectiveCorrectionMode == MODE_APP_CONTROLLED && perspectiveCorrection);

		ord[0] = 0; ord[1] = 1; ord[2] = 2;

		// Set up fog properties
		final Fog fog = appearance.getFog();
		final float invFogDiv = fog != null ? M3GMath.fastReciprocal(fog.getFarDistance() - fog.getNearDistance()) : 0.0f;

		// We'll need the projection matrix for the next transformations
		this.currCam.getProjection(projectionMatrix);

		// This one is also used by all position calculations
		final VertexArray vertPos = vertices.getPositions(scaleBias);

		// Setup texture units first, if we have to use any. Texturing is done
		// by layer, with each texture unit blending on top of another.
		boolean hasTexture = false;

		if(!Mobile.M3GRenderUntexturedPolygons && !Mobile.M3GRenderWireframe)
		{
			for (int i = 0; i < NUM_TEXTURE_UNITS; i++)
			{
				Texture2D t = appearance.getTexture(i);
				VertexArray texCoords = (t != null) ? vertices.getTexCoords(i, texScaleBias) : null;

				if (t != null && texCoords != null)
				{
					// We have at least one texture, so texturing must be done.
					hasTexture = true;

					textures[i] = t;
					texRepeatS[i] = (t.getWrappingS() == Texture2D.WRAP_REPEAT);
					texRepeatT[i] = (t.getWrappingT() == Texture2D.WRAP_REPEAT);

					textr[i].setIdentity();
					if (t.getImage() != null)
					{
						textr[i].postScale(t.getImage().getWidth(), t.getImage().getHeight(), 1.0f);
					}

					texcomptr.setIdentity();
					t.getCompositeTransform(texcomptr);
					textr[i].postMultiply(texcomptr);

					textr[i].postTranslate(texScaleBias[1], texScaleBias[2], texScaleBias[3]);
					textr[i].postScale(texScaleBias[0], texScaleBias[0], texScaleBias[0]);

					if (texVerts[i] == null || 4 * vertPos.getVertexCount() > texVerts[i].length)
						{ texVerts[i] = new float[4 * vertPos.getVertexCount()]; }

					// Transform texture coordinates into NDC
					textr[i].transform(texCoords, texVerts[i], true);
				}
				else
				{
					// Clear the references if this unit is unused or was disabled.
					textures[i] = null;
					texVerts[i] = null;
				}
			}
		}

		// Done with texture transforms, next up is preparing normals for
		// lighting calculations

		final VertexArray vertNorms = vertices.getNormals();

		tr.setIdentity();

		// Transform mesh from local space to world space
		// Receiving a null "transform" indicates that the identity matrix must
		// be used, which just means we don't need to postMultiply.
		if (transform != null) { tr.postMultiply(transform); }

		// Apply the inverse of the camera's transform to the mesh (Eye/View Space)
		if (this.currCamTransInv != null) { tr.postMultiply(this.currCamTransInv); }

		if (vertNorms != null && material != null)
		{
			normalMatrix.set(tr);
			normalMatrix.invert();
			normalMatrix.transpose();

			posLocalToEye.set(tr);
			posLocalToEye.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
			posLocalToEye.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);

			if (eyePos == null || 4 * vertPos.getVertexCount() > eyePos.length)
				{ eyePos = new float[4 * vertPos.getVertexCount()]; }

			posLocalToEye.transform(vertPos, eyePos, true);
		}

		// Normals done, so set up the lights.
		final int numLights = (this.currLights != null) ? this.currLights.size() : 0;

			if (lightEyePos == null || lightEyePos.length < numLights * 4)
			{
				lightEyePos = new float[numLights * 4];
				lightEyeDir = new float[numLights * 4];
			}

		for (int i = 0; i < numLights; i++)
		{
			Light light = this.currLights.get(i);
			Transform lightTrans = this.currLightTrans.get(i);

			// Compute Light World-to-Eye Transform
			tr.setIdentity();
			if (lightTrans != null) { tr.postMultiply(lightTrans); }
			if (this.currCamTransInv != null) { tr.postMultiply(this.currCamTransInv); }

			// Light Position in Eye Space
			lightVec[0] = 0.0f;
			lightVec[1] = 0.0f;
			lightVec[2] = 0.0f;
			lightVec[3] = 1.0f;
			tr.transform(lightVec);
			System.arraycopy(lightVec, 0, lightEyePos, i * 4, 4);

			// Light Direction in Eye Space (M3G's default direction is
			// [0, 0, -1, 0] due to negative Z)
			lightVec[0] = 0.0f;
			lightVec[1] = 0.0f;
			lightVec[2] = -1.0f;
			lightVec[3] = 0.0f;
			tr.transform(lightVec);
			lightVec[3] = 0.0f;
			System.arraycopy(lightVec, 0, lightEyeDir, i * 4, 4);
		}


		// Now that we're done with textures, normals, and lights we transform
		// the vertices and build the triangles themselves. Follows most of the
		// same transforms as the normal step, except this time we go all the
		// way to screen space and account for scaling.

		tr.setIdentity();

		// Apply projection matrix (Clip space)
		tr.postMultiply(projectionMatrix);

		// Apply the inverse of the camera's transform to the mesh (Eye/View Space)
		if (this.currCamTransInv != null) { tr.postMultiply(this.currCamTransInv); }

		// Transform mesh from local space to world space
		// Receiving a null "transform" indicates that the identity matrix must
		// be used, which just means we don't need to postMultiply.
		if (transform != null) { tr.postMultiply(transform); }

		// Scale and translate mesh (P = (S * V) + B) in local space
		tr.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
		tr.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);

		// Transform vertex positions
		if(vertClip == null || 4 * vertPos.getVertexCount() > vertClip.length)
			{ vertClip = new float[4 * vertPos.getVertexCount()]; }
		tr.transform(vertPos, vertClip, true);

		// Now with texture and vertex coordinates transformed, we generate the
		// actual geometry, clip/cull it, and move it to NDC.


		// Create Triangle objects (fromVertAndTris already does culling and clipping)
		final Triangle[] trisScreen = Triangle.fromVertAndTris(
			// Position and texture vertex data
			vertClip, texVerts,
			// Material and shading
			material, shadingMode, twoSidedLighting, localCameraLight,
			// Normal data
			eyePos, vertNorms, normalMatrix,
			// Lights
			this.currLights, lightEyePos, lightEyeDir,
			// IndexArray, clipping, winding order and perspectiveCorrection
			triangles.getIndexArray(), renderableTriangles, cullingMode, vertices,
			windingOrder == PolygonMode.WINDING_CW, perspectiveCorrection);

		// At this point the triangles in `trisScreen` are actually
		// projected to Normalized Device Coordinates, but they will be tranformed
		// to Screen space in-place, hence the name.

		// Reset transform
		tr.setIdentity();

		for (int i = 0; i < NUM_TEXTURE_UNITS; i++)
		{
			if (textures[i] != null) { textr[i].setIdentity(); }
		}


		// Fit to viewport. Notice that Z is scaled slightly below the max limits
		// for shorts (which is -32768, 32767), this is to make sure the
		// multiplied Z values will always be in range and never overflow,
		// saving us the need to clamp it for every pixel draw
		tr.postScale(vieww / 2f, -viewh / 2f, (far - near) * 32759.5f);
		tr.postTranslate(1f, -1f, 0);

		// -> Screen space

		// Perform viewport transform only on renderable triangles (saves an Arrays.copyOf call)
		Triangle.transform(trisScreen, renderableTriangles[0], tr, textr);

		final boolean depthEnabled = compositingMode.isDepthTestEnabled() && isDepthBufferEnabled();
		final float depthUnits = compositingMode.getDepthOffsetUnits();
		final float depthFactor = compositingMode.getDepthOffsetFactor();
		final boolean hasDepthOffset = depthEnabled && (depthFactor != 0.0f || depthUnits != 0.0f);
		float depthOffset = 0.0f;

		final boolean colorEnabled = compositingMode.isColorWriteEnabled();
		final int alphaThreshold = (int) (compositingMode.getAlphaThreshold() * 255);

		if (this.target instanceof Image2D)
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Render Target is instance of Image2D!");
			Image2D i2d = (Image2D) this.target;
			// TODO support rendering to Image2D
		}
		else if (this.target instanceof Graphics)
		{
			final Graphics pgrp = (Graphics) this.target;

			for (int tri_id = 0; tri_id < renderableTriangles[0]; tri_id++)
			{
				// Collect vertex attributes
				coX[0] = trisScreen[tri_id].xA(); coX[1] = trisScreen[tri_id].xB(); coX[2] = trisScreen[tri_id].xC();
				coY[0] = trisScreen[tri_id].yA(); coY[1] = trisScreen[tri_id].yB(); coY[2] = trisScreen[tri_id].yC();
				coZ[0] = trisScreen[tri_id].zA(); coZ[1] = trisScreen[tri_id].zB(); coZ[2] = trisScreen[tri_id].zC();
				coW[0] = trisScreen[tri_id].iwA(); coW[1] = trisScreen[tri_id].iwB(); coW[2] = trisScreen[tri_id].iwC();

				if(hasDepthOffset)
				{
					final float dx10 = coX[1] - coX[0];
					final float dy10 = coY[1] - coY[0];
					final float dz10 = coZ[1] - coZ[0];

					final float dx20 = coX[2] - coX[0];
					final float dy20 = coY[2] - coY[0];
					final float dz20 = coZ[2] - coZ[0];

					final float det = dx10 * dy20 - dx20 * dy10;

					if (M3GMath.abs(det) > M3GMath.EPSILON)
					{
						final float invDet = M3GMath.fastReciprocal(det);
						final float dzdx = (dz10 * dy20 - dz20 * dy10) * invDet;
						final float dzdy = (dx10 * dz20 - dx20 * dz10) * invDet;

						final float m = M3GMath.sqrt(dzdx * dzdx + dzdy * dzdy);

						// 1e-7f is the minimum Z step for a float depth buffer
						depthOffset = (depthFactor * m) + (depthUnits * 1e-7f);
					}
				}

				if(hasTexture)
				{
					for(int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
					{
						if(textures[i] == null) { continue; }
						coS[i][0] = trisScreen[tri_id].sA(i); coS[i][1] = trisScreen[tri_id].sB(i); coS[i][2] = trisScreen[tri_id].sC(i);
						coT[i][0] = trisScreen[tri_id].tA(i); coT[i][1] = trisScreen[tri_id].tB(i); coT[i][2] = trisScreen[tri_id].tC(i);
					}
				}

				boolean hasColors = trisScreen[tri_id].hasVertexColors();

				// Calculate the triangle area denominator, used by texturing
				// and vertex color blending.
				float xA = trisScreen[tri_id].xA();
				float yA = trisScreen[tri_id].yA();
				float xB = trisScreen[tri_id].xB();
				float yB = trisScreen[tri_id].yB();
				float xC = trisScreen[tri_id].xC();
				float yC = trisScreen[tri_id].yC();

				float denominator = (xB - xA) * (yC - yA) - (xC - xA) * (yB - yA);

				// Let's precalculate uv derivatives for mipmapping. Skips having
				// to do expensive calculations inside the inner render loops.
				if (hasTexture && M3GMath.abs(denominator) > M3GMath.EPSILON)
				{
					float invDet = M3GMath.fastReciprocal(denominator);

					float dxB = xB - xA, dyB = yB - yA;
					float dxC = xC - xA, dyC = yC - yA;

					if (perspectiveCorrection)
					{
						float dwB = coW[1] - coW[0];
						float dwC = coW[2] - coW[0];

						dwdx = (dwB * dyC - dwC * dyB) * invDet;
						dwdy = (dwC * dxB - dwB * dxC) * invDet;
					}

					for (int i = 0; i < NUM_TEXTURE_UNITS; i++)
					{
						if (textures[i] == null) { continue; }

						// For perspective correction, we need the actual W of
						// each vertex as well.
						if (perspectiveCorrection)
						{
							float swA = coS[i][0] * trisScreen[tri_id].wA();
							float swB = coS[i][1] * trisScreen[tri_id].wB();
							float swC = coS[i][2] * trisScreen[tri_id].wC();

							float twA = coT[i][0] * trisScreen[tri_id].wA();
							float twB = coT[i][1] * trisScreen[tri_id].wB();
							float twC = coT[i][2] * trisScreen[tri_id].wC();

							float dswB = swB - swA, dswC = swC - swA;
							float dtwB = twB - twA, dtwC = twC - twA;

							sStepX[i] = (dswB * dyC - dswC * dyB) * invDet; // d(s/w)/dx
							tStepX[i] = (dtwB * dyC - dtwC * dyB) * invDet; // d(t/w)/dx

							sStepY[i] = (dswC * dxB - dswB * dxC) * invDet; // d(s/w)/dy
							tStepY[i] = (dtwC * dxB - dtwB * dxC) * invDet; // d(t/w)/dy
						}
						else
						{
							float dsB = coS[i][1] - coS[i][0];
							float dsC = coS[i][2] - coS[i][0];
							float dtB = coT[i][1] - coT[i][0];
							float dtC = coT[i][2] - coT[i][0];

							sStepX[i] = (dsB * dyC - dsC * dyB) * invDet; // ds/dx
							tStepX[i] = (dtB * dyC - dtC * dyB) * invDet; // dt/dx

							sStepY[i] = (dsC * dxB - dsB * dxC) * invDet; // ds/dy
							tStepY[i] = (dtC * dxB - dtB * dxC) * invDet; // dt/dy
						}
					}
				}

				// Calculate the starting vertex color with the barycentric of the
				// triangle. Then at each scanline we only need to determine the
				// left and right color spans with quick add and mult operations, and
				// at the inner pixel loop, all we need is a simple addition.
				if (hasColors)
				{
					if (M3GMath.abs(denominator) > M3GMath.EPSILON)
					{
						float invDet = M3GMath.fastReciprocal(denominator);

						int colorA = trisScreen[tri_id].colorA();
						int colorB = trisScreen[tri_id].colorB();
						int colorC = trisScreen[tri_id].colorC();

						float aA = (colorA >> 24) & 0xFF, rA = (colorA >> 16) & 0xFF, gA = (colorA >> 8) & 0xFF, bA = colorA & 0xFF;
						float aB = (colorB >> 24) & 0xFF, rB = (colorB >> 16) & 0xFF, gB = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
						float aC = (colorC >> 24) & 0xFF, rC = (colorC >> 16) & 0xFF, gC = (colorC >> 8) & 0xFF, bC = colorC & 0xFF;

						// To properly use additions in the triangle render loops
						// below, we need to calculate the derivatives for each
						// color channel, on each axis.
						float dR_B = rB - rA, dR_C = rC - rA;
						float dG_B = gB - gA, dG_C = gC - gA;
						float dB_B = bB - bA, dB_C = bC - bA;
						float dA_B = aB - aA, dA_C = aC - aA;

						rStepX = (dR_B * (yC - yA) - dR_C * (yB - yA)) * invDet;
						gStepX = (dG_B * (yC - yA) - dG_C * (yB - yA)) * invDet;
						bStepX = (dB_B * (yC - yA) - dB_C * (yB - yA)) * invDet;
						aStepX = (dA_B * (yC - yA) - dA_C * (yB - yA)) * invDet;

						rStepY = (dR_C * (xB - xA) - dR_B * (xC - xA)) * invDet;
						gStepY = (dG_C * (xB - xA) - dG_B * (xC - xA)) * invDet;
						bStepY = (dB_C * (xB - xA) - dB_B * (xC - xA)) * invDet;
						aStepY = (dA_C * (xB - xA) - dA_B * (xC - xA)) * invDet;
					}
				}

				// x and y coordinates are special cases where the resulting top, mid and bot values should be in decreasing order (top > mid > bot)
				if (coY[ord[1]] < coY[ord[0]]) { int temp = ord[0]; ord[0] = ord[1]; ord[1] = temp; }
				if (coY[ord[2]] < coY[ord[0]]) { int temp = ord[0]; ord[0] = ord[2]; ord[2] = temp; }
				if (coY[ord[2]] < coY[ord[1]]) { int temp = ord[1]; ord[1] = ord[2]; ord[2] = temp; }

				// Degenerate triangle? Skip it.
				if (M3GMath.abs(coY[ord[2]] - coY[ord[0]]) < M3GMath.EPSILON) { continue; }

				// Assign ordered vertex attributes based on their determined order
				xTop = coX[ord[0]]; xMidL = coX[ord[1]]; xBot = coX[ord[2]];
				yTop = coY[ord[0]]; yMid = coY[ord[1]]; yBot = coY[ord[2]];
				zTop = coZ[ord[0]]; zMidL = coZ[ord[1]]; zBot = coZ[ord[2]];
				pwTop = coW[ord[0]]; pwMidL = coW[ord[1]]; pwBot = coW[ord[2]];

				if(hasTexture)
				{
					for(int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
					{
						if(textures[i] == null) { continue; }
						sTop[i] = coS[i][ord[0]]; sMidL[i] = coS[i][ord[1]]; sBot[i] = coS[i][ord[2]];
						tTop[i] = coT[i][ord[0]]; tMidL[i] = coT[i][ord[1]]; tBot[i] = coT[i][ord[2]];
					}
				}

				// Calculate the right horizon
				rHorizon = (yMid - yTop) * M3GMath.fastReciprocal(yBot - yTop);
				xMidR = xTop + rHorizon * (xBot - xTop);
				zMidR = zTop + rHorizon * (zBot - zTop);
				pwMidR = pwTop + rHorizon * (pwBot - pwTop);

				if(hasTexture)
				{
					for(int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
					{
						if(textures[i] == null) { continue; }
						sMidR[i] = sTop[i] + rHorizon * (sBot[i] - sTop[i]);
						tMidR[i] = tTop[i] + rHorizon * (tBot[i] - tTop[i]);
					}
				}

				// Swap midpoints if necessary
				if (xMidL > xMidR)
				{
					float temp;

					// Swap values between left and right midpoints
					temp = xMidL; xMidL = xMidR; xMidR = temp;
					temp = zMidL; zMidL = zMidR; zMidR = temp;
					temp = pwMidL; pwMidL = pwMidR; pwMidR = temp;

					if(hasTexture)
					{
						for(int i = 0; i < Graphics3D.NUM_TEXTURE_UNITS; i++)
						{
							if(textures[i] == null) { continue; }
							temp = sMidL[i]; sMidL[i] = sMidR[i]; sMidR[i] = temp;
							temp = tMidL[i]; tMidL[i] = tMidR[i]; tMidR[i] = temp;
						}
					}
				}

				// Draw both halves of the triangle
				for (int half = 0; half < 2; half++)
				{
					// Determine the range for the y-coordinate
					yStart = half == 0 ? M3GMath.max(M3GMath.roundPositive(yTop), 0) : M3GMath.max(M3GMath.roundPositive(yMid), 0);
					yEnd = half == 0 ? M3GMath.min(M3GMath.roundPositive(yMid), viewh) : M3GMath.min(M3GMath.roundPositive(yBot), viewh);

					renderTriangleHalf(vertices, half, yStart, yEnd, trisScreen, tri_id, hasColors, hasTexture, compositingMode,
						fog, invFogDiv, alphaThreshold, depthEnabled, colorEnabled, depthOffset, perspectiveCorrection);
				}
			}
		}
	}

	private void positionLights(World world, Group group)
	{
		for (int i = 0; i < group.getChildCount(); ++i)
		{
			Transform t = new Transform();
			Node node = group.getChild(i);

			if (node instanceof Light && node.getTransformTo(world, t))
				{ addLight((Light) node, t); }
			else if (node instanceof Group)
				{ positionLights(world, (Group) node);}
			else if (node instanceof SkinnedMesh)
				/* A SkinnedMesh skeleton can hold lights in its own branch. */
				{ positionLights(world, ((SkinnedMesh) node).getSkeleton()); }
		}
	}

	public void resetLights()
	{
		this.currLights.clear();
		this.currLightTrans.clear();
	}

	public void setCamera(Camera camera, Transform transform)
	{
		this.currCam = camera;

		/* If no transform is given, the identity matrix is used as per JSR-184. */
		if (transform == null)
		{
			this.currCamTrans.setIdentity();
			this.currCamTransInv.setIdentity();
		}
		else /* Else, set the transform and its inverse accordingly. */
		{
			this.currCamTrans.set(transform);
			this.currCamTransInv.set(transform);
		}
		this.currCamTransInv.invert(); /* This one will execute regardless of the given transform above. */
	}

	public void setDepthRange(float near, float far)
	{
		/* As per JSR-184, throw IllegalArgumentException if the received near and/or far planes have unsupported values. */
		if (near < 0 || far < 0 || 1 < near || 1 < far) { throw new IllegalArgumentException("The requested Depth Range values are invalid."); }
		else
		{
			this.near=near;
			this.far=far;
			if (this.depthBuffer != null) { Arrays.fill(this.depthBuffer, (short) M3GMath.round((this.far * 65520.0f) - 32760.0f)); }
		}
	}

	public void setLight(int index, Light light, Transform transform)
	{
		/* As per JSR-184, throw IndexOutOfBoundsException if index < 0 or index > CurrentAmountOfLights. */
		if (index < 0 || index >= MAX_LIGHTS) { throw new IndexOutOfBoundsException("Tried to modify a Light on an out-of-bounds index."); }

		/* If no transform is received, use the identity matrix. */
		if (transform == null) { transform = new Transform(); }

		// Indices are NOT supposed to change here,
		// so we're simply updating the arrays at the index,
		// even if any new value is null.
		this.currLights.set(index, light);
		this.currLightTrans.set(index, transform);
	}

	public void setViewport(int x, int y, int width, int height)
	{
		/* As per JSR-184, throw IllegalArgumentException if the received width and height are < 0, or beyond the max allowed. */
		if (width <= 0 || height <= 0 || width > MAX_VIEWPORT_WIDTH || height > MAX_VIEWPORT_HEIGHT)
			{ throw new IllegalArgumentException("Tried to set a viewport of unsupported size."); }

		this.viewx = x;
		this.viewy = y;
		this.vieww = width;
		this.viewh = height;
	}


	/* Helper Methods */

	/*
	 * Renders a Sprite3D as a screen-aligned textured rectangle, following the same
	 * math as the JSR-184 Reference Implementation (m3g_sprite.c, m3gGetSpriteCoordinates):
	 * the node origin and half-unit axis vectors are measured in eye space, re-aligned
	 * to the screen axes, projected, and the resulting NDC quad is rasterized directly
	 * with the sprite's crop as texture source.
	 */
	private void renderSprite(Sprite3D sprite, Transform transform)
	{
		final Image2D img = sprite.getImage();
		final Appearance appearance = sprite.getAppearance();

		// As per JSR-184, a Sprite3D with no appearance (or no image) is not rendered.
		if (img == null || appearance == null) { return; }
		if (!(this.target instanceof Graphics)) { return; }

		// JSR-184 scope culling, same rule as for meshes.
		if ((sprite.getScope() & this.currCam.getScope()) == 0) { return; }

		// The crop rectangle keeps its sign; negative dimensions flip the image on that axis.
		final int cropX = sprite.getCropX(), cropY = sprite.getCropY();
		int cropW = sprite.getCropWidth(), cropH = sprite.getCropHeight();
		final boolean flipX = cropW < 0, flipY = cropH < 0;
		if (flipX) { cropW = -cropW; }
		if (flipY) { cropH = -cropH; }
		if (cropW == 0 || cropH == 0) { return; }

		// Intersect the crop rectangle with the image rectangle; nothing to render without overlap.
		final int isectX = M3GMath.max(cropX, 0), isectY = M3GMath.max(cropY, 0);
		final int isectW = M3GMath.min(cropX + cropW, img.getWidth()) - isectX;
		final int isectH = M3GMath.min(cropY + cropH, img.getHeight()) - isectY;
		if (isectW <= 0 || isectH <= 0) { return; }

		// Model-view: the sprite's rotation/scale only affect its size, never its screen alignment.
		tr.set(this.currCamTransInv);
		if (transform != null) { tr.postMultiply(transform); }

		// Origin and half-unit axis points in eye space (affine transform, w stays 1).
		final float[] eye = { 0,0,0,1,  0.5f,0,0,1,  0,0.5f,0,1 };
		tr.transform(eye);
		final float ox = eye[0]/eye[3], oy = eye[1]/eye[3], oz = eye[2]/eye[3];
		final float dx0 = eye[4]/eye[7] - ox, dy0 = eye[5]/eye[7] - oy, dz0 = eye[6]/eye[7] - oz;
		final float dx1 = eye[8]/eye[11] - ox, dy1 = eye[9]/eye[11] - oy, dz1 = eye[10]/eye[11] - oz;
		final float halfUnitX = M3GMath.sqrt(dx0*dx0 + dy0*dy0 + dz0*dz0);
		final float halfUnitY = M3GMath.sqrt(dx1*dx1 + dy1*dy1 + dz1*dz1);

		// Project the origin plus screen-aligned extent points.
		this.currCam.getProjection(projectionMatrix);
		final float[] clip = { ox,oy,oz,1,  ox+halfUnitX,oy,oz,1,  ox,oy+halfUnitY,oz,1 };
		projectionMatrix.transform(clip);
		if (clip[3] <= 0f || clip[7] <= 0f || clip[11] <= 0f) { return; }

		float ndcX = clip[0]/clip[3], ndcY = clip[1]/clip[3];
		float ndcZ = clip[2]/clip[3];
		if (ndcZ < -1f || ndcZ > 1f) { return; }

		// Our depth buffer is now comprised of short values, so ndcZ has to be
		// multiplied by the same factor used by the buffer.
		ndcZ = ndcZ * (this.far - this.near) * 32759.5f;
		short z = (short) ndcZ;

		float halfW = M3GMath.abs(clip[4]/clip[7] - ndcX);
		float halfH = M3GMath.abs(clip[9]/clip[11] - ndcY);

		if (sprite.isScaled())
		{
			// Adjust the position and size according to the (possibly partly outside) crop rectangle.
			final float unitX = halfW / (float) cropW, unitY = halfH / (float) cropH;
			ndcX -= (2*cropX + cropW - 2*isectX - isectW) * unitX;
			ndcY += (2*cropY + cropH - 2*isectY - isectH) * unitY;
			halfW = unitX * isectW;
			halfH = unitY * isectH;
		}
		else
		{
			// Non-scaled sprites take their size in pixels from the crop rectangle.
			ndcX -= (float)(2*cropX + cropW - 2*isectX - isectW) / (float) vieww;
			ndcY += (float)(2*cropY + cropH - 2*isectY - isectH) / (float) viewh;
			halfW = (float) isectW / (float) vieww;
			halfH = (float) isectH / (float) viewh;
		}

		// NDC -> viewport-relative pixels (same mapping as the triangle rasterizer).
		final float sx0 = (ndcX - halfW + 1f) * vieww / 2f;
		final float sx1 = (ndcX + halfW + 1f) * vieww / 2f;
		final float sy0 = (1f - (ndcY + halfH)) * viewh / 2f;
		final float sy1 = (1f - (ndcY - halfH)) * viewh / 2f;
		final float spanX = sx1 - sx0, spanY = sy1 - sy0;
		if (spanX <= 0f || spanY <= 0f) { return; }

		final int pixL = M3GMath.max(M3GMath.roundPositive(sx0), 0);
		final int pixR = M3GMath.min(M3GMath.roundPositive(sx1), vieww);
		final int pixT = M3GMath.max(M3GMath.roundPositive(sy0), 0);
		final int pixB = M3GMath.min(M3GMath.roundPositive(sy1), viewh);
		if (pixL >= pixR || pixT >= pixB) { return; }

		final CompositingMode compositingMode = appearance.getCompositingMode() != null ? appearance.getCompositingMode() : new CompositingMode();
		final Fog fog = appearance.getFog();
		final int alphaThreshold = (int) (compositingMode.getAlphaThreshold() * 255);
		final float alphaFactor = sprite.getAlphaFactor();
		final boolean depthTest = compositingMode.isDepthTestEnabled() && isDepthBufferEnabled();
		final boolean depthWrite = depthTest && compositingMode.isDepthWriteEnabled();

		// The Sprite3D has the same depth for its entire area, so we only need
		// to calculate fog once.
		if (fog != null)
		{
			// Distance in eye space along the camera's viewing axis
			final float zEye = -oz;
			final float invFogDiv = M3GMath.fastReciprocal(fog.getFarDistance() - fog.getNearDistance());

			if (fog.getMode() == Fog.LINEAR)
			{
				fogFactor = M3GMath.max(0, M3GMath.min(1, (fog.getFarDistance() - zEye) * invFogDiv));
			}
			else { fogFactor = M3GMath.exp(-fog.getDensity() * zEye); }

			fogFactor = M3GMath.min(255.0f, fogFactor * 256.0f);
		}

		for (int y = pixT; y < pixB; y++)
		{
			// Skip odd scanlines when in half res. The even scanlines repeat on the lower one as well
			if(Mobile.halfResM3GRaster && (y & 1) != 0) { continue; }

			final float v = (y + 0.5f - sy0) / spanY;
			int texY = isectY + (int) ((flipY ? 1f - v : v) * isectH);
			if (texY < isectY) { texY = isectY; } else if (texY >= isectY + isectH) { texY = isectY + isectH - 1; }

			int rasterIdxY = (y + viewy) * canvasWidth + viewx;
			for (int x = pixL; x < pixR; x++)
			{
				// Depth test against the same buffer and convention used by triangles.
				if (depthTest && this.depthBuffer[this.vieww * y + x] <= z) { continue; }

				final float u = (x + 0.5f - sx0) / spanX;
				int texX = isectX + (int) ((flipX ? 1f - u : u) * isectW);
				if (texX < isectX) { texX = isectX; } else if (texX >= isectX + isectW) { texX = isectX + isectW - 1; }

				paintPixel = img.getPixel(texX, texY);
				alpha = (int) (((paintPixel >> 24) & 0xFF) * alphaFactor);

				if (alpha < alphaThreshold || alpha == 0) { continue; }

				if (fog != null && fogFactor < 255.0f)
					{ paintPixel = blendPixels(paintPixel, fog.getColor(), (int) fogFactor, Graphics3D.BLEND_FOG, 0, 0); }

				rasterData[rasterIdxY + x] = blendPixels(rasterData[rasterIdxY + x],
					paintPixel, alpha, compositingMode.getBlending(), 0, 0);

				// Rendering at half res?
				if (Mobile.halfResM3GRaster && y+viewy < canvasHeight) { rasterData[rasterIdxY + canvasWidth + x] = rasterData[rasterIdxY + x]; }

				if (depthWrite && (paintPixel >>> 24) >= 255) { this.depthBuffer[this.vieww * y + x] = z; }
			}
		}
	}

	private void renderTriangleHalf(VertexBuffer vertices, int half, int yStart, int yEnd,
		Triangle[] trisScreen, int tri_id, boolean hasColors, boolean hasTexture, CompositingMode compositingMode,
		Fog fog, float invFogDiv, int alphaThreshold, boolean depthEnabled, boolean colorEnabled,
		float depthOffset, boolean doPerspective)
	{
		// Prepare the flags that can be overridden by the UI.
		boolean doDither = (Mobile.m3gDitheringMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gDitheringMode == MODE_APP_CONTROLLED && (this.hints & DITHER) != 0);

		boolean doAntiAlias = (Mobile.m3gAntiAliasingMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gAntiAliasingMode == MODE_APP_CONTROLLED && (this.hints & ANTIALIAS) != 0);

		if (hasTexture)
		{
			for (int i = 0; i < NUM_TEXTURE_UNITS; i++)
			{
				if (textures[i] == null) { continue; }
				useBilinear[i] = (Mobile.m3gBilinearFilterMode == MODE_FORCE_ENABLE)
					|| (Mobile.m3gBilinearFilterMode == MODE_APP_CONTROLLED &&
					((textures[i].getImageFilter() == Texture2D.FILTER_LINEAR)));
			}
		}

		// Get into the render loop proper.

		float yDiv = half == 0 ? M3GMath.fastReciprocal(yMid - yTop) : M3GMath.fastReciprocal(yBot - yMid);
		for (int y = yStart; y < yEnd; y++)
		{
			// Skip odd scanlines when in half res. The even scanlines repeat on the lower one as well
			if(Mobile.halfResM3GRaster && (y & 1) != 0) { continue; }

			float drawY = half == 0
				? (y - yTop) * yDiv  // Upper half
				: 1f - (y - yMid) * yDiv; // Lower half
			drawY = M3GMath.min(drawY, 1.0f);

			// Calculate interpolated values (xL and xR allow us to skip early, so do them first)
			float xL = half == 0
				? xTop + drawY * (xMidL - xTop)
				: xBot + drawY * (xMidL - xBot);
			float xR = half == 0
				? xTop + drawY * (xMidR - xTop)
				: xBot + drawY * (xMidR - xBot);

			int ixL = M3GMath.max(M3GMath.roundPositive(xL), 0);
			int ixR = M3GMath.min(M3GMath.roundPositive(xR), vieww);

			final int spanWidth = ixR - ixL;

			if (spanWidth <= 0) { continue; }

			// Saves a division for each x step.
			final float invDrawSpanWidth = M3GMath.fastReciprocal(xR - xL);

			// Do we have vertex colors? If so, get the span edges' colors here,
			// that way, the inner loop only needs to do a simple addition.
			if (hasColors)
			{
				float xA = trisScreen[tri_id].xA();
				float yA = trisScreen[tri_id].yA();
				int colorA = trisScreen[tri_id].colorA();

				float dx = ixL - xA;
				float dy = y - yA;

				deltaA = ((colorA >> 24) & 0xFF) + dx * aStepX + dy * aStepY;
				deltaR = ((colorA >> 16) & 0xFF) + dx * rStepX + dy * rStepY;
				deltaG = ((colorA >> 8) & 0xFF)  + dx * gStepX + dy * gStepY;
				deltaB = (colorA & 0xFF)         + dx * bStepX + dy * bStepY;
			}

			float zL = half == 0
				? zTop + drawY * (zMidL - zTop)
				: zBot + drawY * (zMidL - zBot);
			float zR = half == 0
				? zTop + drawY * (zMidR - zTop)
				: zBot + drawY * (zMidR - zBot);

			if(hasTexture)
			{
				for (int i = 0; i < NUM_TEXTURE_UNITS; i++)
				{
					if (textures[i] == null) { continue; }

					sL[i] = half == 0 ? sTop[i] + drawY * (sMidL[i] - sTop[i]) : sBot[i] + drawY * (sMidL[i] - sBot[i]);
					sR[i] = half == 0 ? sTop[i] + drawY * (sMidR[i] - sTop[i]) : sBot[i] + drawY * (sMidR[i] - sBot[i]);
					tL[i] = half == 0 ? tTop[i] + drawY * (tMidL[i] - tTop[i]) : tBot[i] + drawY * (tMidL[i] - tBot[i]);
					tR[i] = half == 0 ? tTop[i] + drawY * (tMidR[i] - tTop[i]) : tBot[i] + drawY * (tMidR[i] - tBot[i]);
				}
			}

			float pwL = half == 0
				? pwTop + drawY * (pwMidL - pwTop)
				: pwBot + drawY * (pwMidL - pwBot);
			float pwR = half == 0
				? pwTop + drawY * (pwMidR - pwTop)
				: pwBot + drawY * (pwMidR - pwBot);

			int depthIdx = (y * this.vieww) + ixL;
			int rasterIdx = ((y + viewy) * canvasWidth + viewx) + ixL;

			final float zStep = (zR - zL) * invDrawSpanWidth;
			final float pwStep = (pwR - pwL) * invDrawSpanWidth;

			float pw = pwL + (ixL - xL) * pwStep;
			float z  = zL + (ixL - xL) * zStep + depthOffset;
			float drawX = (ixL - xL) * invDrawSpanWidth;

			// Draw the pixels for the current y-coordinate
			for (int x = ixL; x < ixR; x++, z += zStep, pw += pwStep, drawX += invDrawSpanWidth, depthIdx++, rasterIdx++)
			{
				// This check is really only used for wireframe debugging, and it's not a perfect wireframe rendering
				if(Mobile.M3GRenderWireframe && x > ixL && x < ixR) { continue; }

				// Only depth test if the compositingMode has the feature enabled. If
				// compositingMode is not set, check if this target has depthBuffer enabled.
				if(depthEnabled && this.depthBuffer[depthIdx] <= (short) z)
				{
					// We need to increment the color deltas even when discarding by depth,
					// otherwise vertex color spans on objects partially occluded by others
					// won't be correct.
					if (hasColors) { deltaA += aStepX; deltaR += rStepX; deltaG += gStepX; deltaB += bStepX; }
					continue;
				}

				// We have to do texture blending if we have vertex colors, as any available texture goes on top of them
				if (hasColors)
				{
					// Interpolate from xL to xR based on current pixel xy coordinate..
					// No need to calculate barycentric coords on every pixel.

					// We could call M3GMath.max/min here, but a simple ternary to
					// clamp these to [0,255] range is likely faster...
					int r = deltaR < 0.0f ? 0 : (deltaR > 255.0f ? 255 : (int) deltaR);
					int g = deltaG < 0.0f ? 0 : (deltaG > 255.0f ? 255 : (int) deltaG);
					int b = deltaB < 0.0f ? 0 : (deltaB > 255.0f ? 255 : (int) deltaB);
					int a = deltaA < 0.0f ? 0 : (deltaA > 255.0f ? 255 : (int) deltaA);

					paintPixel = (a << 24) | (r << 16) | (g << 8) | b;

					deltaA += aStepX;
					deltaR += rStepX;
					deltaG += gStepX;
					deltaB += bStepX;
				}
				else
				{
					// If there's no texture coords or a texture image, we default to rendering with vertex colors. (also used for debug render modes)
					// It's forced to opaque when blending mode is set to REPLACE.
					paintPixel = compositingMode.getBlending() == CompositingMode.REPLACE ?
						0xFF000000 | vertices.getDefaultColor() : vertices.getDefaultColor();
				}

				if(hasTexture)
				{
					float invPw = M3GMath.fastReciprocal(pw);

					for(int i = 0; i < NUM_TEXTURE_UNITS; i++)
					{
						// Skip this texture unit right away if it is disabled/unused
						Texture2D tex = textures[i];
						if (tex == null) { continue; }

						Image2D targetImage = tex.getImage();
						int baseWidth = targetImage.getWidth();
						int baseHeight = targetImage.getHeight();
						final int levelFilter = tex.getLevelFilter();

						float s = sL[i] + drawX * (sR[i] - sL[i]);
						float t = tL[i] + drawX * (tR[i] - tL[i]);

						if(doPerspective)
						{
							s *= invPw;
							t *= invPw;
						}

						// Mipmapping support requested.
						if (levelFilter != Texture2D.FILTER_BASE_LEVEL)
						{
							float dsdx, dtdx, dsdy, dtdy;

							if (doPerspective)
							{
								dsdx = (sStepX[i] - s * dwdx) * invPw;
								dtdx = (tStepX[i] - t * dwdx) * invPw;
								dsdy = (sStepY[i] - s * dwdy) * invPw;
								dtdy = (tStepY[i] - t * dwdy) * invPw;
							}
							else
							{
								dsdx = sStepX[i];
								dtdx = tStepX[i];
								dsdy = sStepY[i];
								dtdy = tStepY[i];
							}

							int maxLevel = textures[i].getMipmapLevelCount() - 1;

							float lod = calculateLOD(dsdx, dtdx, dsdy, dtdy, levelFilter == Texture2D.FILTER_LINEAR);
							int targetLevel = (int) lod;

							// Apply LOD Dithering ONLY when FILTER_LINEAR (Trilinear) is requested
							// This saves us the need to do much slower trilinear filtering, while
							// retaining most of the looks.
							if (levelFilter == Texture2D.FILTER_LINEAR)
							{
								float lodFraction = lod - targetLevel;

								// Blend on half of the transition band between each mip level
								if (lodFraction > 0.25f && lodFraction < 0.75f)
								{
									// Remap lodFraction from the [0.25, 0.75] range to the expected
									// [0.0, 1.0] for the dither threshold
									float normalizedFraction = (lodFraction - 0.25f) * 2.0f;

									// Manually tuned bayer coefficient. Looks good enough in
									// Super Taxi Driver.
									int bayerIndex = ((y & 3) << 2) | (x & 3);
									float threshold = (BAYER_PATTERN[bayerIndex] + 4) * 0.0625f;

									if (normalizedFraction > threshold)
									{
										targetLevel = Math.min(targetLevel + 1, maxLevel);
									}
								}
								else if (lodFraction >= 0.75f)
								{
									// Unconditionally step to next MIP level once past the transition band
									targetLevel = Math.min(targetLevel + 1, maxLevel);
								}
							}

							targetImage = tex.getImageForLOD(targetLevel);

							// POT textures coming in with another fast path: Just shift
							// right by the targetLevel!
							if (!textures[i].isNPOT())
							{
								s = (float) ((int) s >> targetLevel);
								t = (float) ((int) t >> targetLevel);
							}
							else
							{
								float scaleX = (float) targetImage.getWidth() / baseWidth;
								float scaleY = (float) targetImage.getHeight() / baseHeight;
								s *= scaleX;
								t *= scaleY;
							}
						}

						if (useBilinear[i])
						{
							paintPixel = blendPixels(paintPixel,
								sampleBilinear(targetImage, s, t, targetImage.getWidth(), targetImage.getHeight(),
									texRepeatS[i], texRepeatT[i], textures[i].isNPOT()),
									255, textures[i].getBlending(), textures[i].getBlendColor(),
									targetImage.getFormat());
						}
						else
						{
							// This minor EPSILON decrement fixes UV bounds in a number of games,
							// such as Speed Spirit and 4x4 Extreme Rally 3D.
							int texX = M3GMath.floor(s);
							int texY = M3GMath.floor(t);

							texX = wrapX(texX, targetImage.getWidth(), texRepeatS[i], textures[i].isNPOT());
							texY = wrapY(texY, targetImage.getHeight(), texRepeatT[i], textures[i].isNPOT());

							paintPixel = blendPixels(paintPixel, targetImage.getPixel(texX, texY),
								255, textures[i].getBlending(), textures[i].getBlendColor(),
								targetImage.getFormat());
						}
					}
				}

				/*
				 * Alpha test BEFORE any depth write: transparent fragments must not
				 * occlude geometry drawn later (games rely on this — e.g. tree canopies
				 * with alpha cutouts drawn before the ground). The depth buffer is only
				 * updated by fragments that survive this test.
				 *
				 * TODO: Spec says that a threshold of 0 should make ALL fragments go through,
				 * but doing so evidently breaks transparency in apps like Speed Spirit, Coast Racer
				 * and a few others on vegetation when the texel alpha is also 0. So what gives?
				 */
				if ((paintPixel >>> 24) == 0 || (paintPixel >>> 24) < alphaThreshold) { continue; }

				// Update the depth buffer if depth write is enabled (alpha pixels do not write Z)
				if (depthEnabled && compositingMode.isDepthWriteEnabled() &&
					(paintPixel >>> 24) >= 255) { this.depthBuffer[depthIdx] = (short) z; }

				// To blend the fog value here, we have to take the current pixel's z value into consideration
				if (fog != null)
				{
					// Fog is always perspective-correct
					final float zEye = M3GMath.fastReciprocal(pw);

					if (fog.getMode() == Fog.LINEAR)
					{
						fogFactor = M3GMath.max(0, M3GMath.min(1, (fog.getFarDistance() - zEye) * invFogDiv));
					}
					else { fogFactor = M3GMath.exp(-fog.getDensity() * zEye); }

					fogFactor = M3GMath.min(255.0f, fogFactor * 256.0f);

					if (fogFactor < 255.0f) { paintPixel = blendPixels(paintPixel, fog.getColor(), (int) fogFactor, Graphics3D.BLEND_FOG, 0, 0); }
				}

				// Only write to the screen if color write is enabled.
				if(colorEnabled)
				{
					int compBlending = compositingMode.getBlending();

					if (doDither)
					{
						byte ditherOffset = BAYER_PATTERN[((y & 3) << 2) | (x & 3)];

						int r = (paintPixel >> 16) & 0xFF;
						int g = (paintPixel >>  8) & 0xFF;
						int b =  paintPixel & 0xFF;

						// Apply dither offset
						r += ditherOffset;
						g += ditherOffset;
						b += ditherOffset;

						// Fast-path branchless check for [0, 255] bounds.
						// (c & ~0xFF) is non-zero ONLY if c < 0 or c > 255
						if ((r & ~0xFF) != 0) r = (r < 0) ? 0 : 255;
						if ((g & ~0xFF) != 0) g = (g < 0) ? 0 : 255;
						if ((b & ~0xFF) != 0) b = (b < 0) ? 0 : 255;

						// Repack keeping original Alpha intact
						paintPixel = (paintPixel & 0xFF000000) | (r << 16) | (g << 8) | b;
					}

					// Apply basic edge coverage Anti-Aliasing, if the flag is enabled.
					if (doAntiAlias && (x == ixL || x == ixR - 1) && (paintPixel >>> 24) >= 255)
					{
						// The way this works is that we "extend" the geometry size a bit
						// for the antialiased output, that way triangles don't get smoothed
						// inwards, causing transparent edges between them to manifest.
						if (x == ixL)
						{
							int distFx = (int) (((ixL + 1.0f) - xL) * 65536.0f);
							int scaledDist = (distFx * 84) >> 16;

							applyEdgeAA(x - 1, rasterIdx - 1, canvasWidth, paintPixel, rasterData, compBlending, 84 + scaledDist);
							applyEdgeAA(x - 2, rasterIdx - 2, canvasWidth, paintPixel, rasterData, compBlending, scaledDist);
						}
						else
						{
							int distFx = (int) ((xR - (ixR - 1)) * 65536.0f);
							int scaledDist = (distFx * 84) >> 16;

							applyEdgeAA(x + 1, rasterIdx + 1, canvasWidth, paintPixel, rasterData, compBlending, 84 + scaledDist);
							applyEdgeAA(x + 2, rasterIdx + 2, canvasWidth, paintPixel, rasterData, compBlending, scaledDist);
						}
					}

					rasterData[rasterIdx] = blendPixels(rasterData[rasterIdx],
						paintPixel, (paintPixel >>> 24), compBlending, 0, 0);

					// Rendering at half res? Next scanline gets painted too.
					if (Mobile.halfResM3GRaster) { rasterData[rasterIdx + canvasWidth] = rasterData[rasterIdx]; }
				}
			}
		}
	}

	private static final void applyEdgeAA(int targetX, int targetIdx, int canvasWidth, int paintPixel,
							int[] rasterData, int compBlending, int coverageAlpha)
	{
		if (coverageAlpha > 0 && targetX >= 0 && targetX < canvasWidth)
		{
			int aaPixel = blendPixels(rasterData[targetIdx], paintPixel, coverageAlpha, Graphics3D.BLEND_COVERAGE, 0, 0);

			rasterData[targetIdx] = blendPixels(rasterData[targetIdx], aaPixel, (aaPixel >> 24) & 0xFF, compBlending, 0, 0);

			if (Mobile.halfResM3GRaster && (targetIdx + canvasWidth) < rasterData.length)
			{
				rasterData[targetIdx + canvasWidth] = rasterData[targetIdx];
			}
		}
	}

	// This one is used for texture/background blending, and also pixel blending when rendering to the screen
	private static final int blendPixels(int bg, int fg, int alpha, int blendMode, int texBlendColor, int texFormat)
	{
		switch (blendMode)
		{
			case CompositingMode.REPLACE:
				return fg;

			case CompositingMode.ALPHA:
			{
				if (alpha == 0)   { return bg; }
				if (alpha >= 255) { return fg; }

				int bgRB = bg & 0x00FF00FF;
				int fgRB = fg & 0x00FF00FF;

				int bgR = (bg >> 16) & 0xFF, fgR = (fg >> 16) & 0xFF;
				int bgB = bg & 0xFF,         fgB = fg & 0xFF;

				int bgA = bg >>> 24,         fgA = fg >>> 24;
				int bgG = (bg >> 8) & 0xFF,  fgG = (fg >> 8) & 0xFF;

				int outR = bgR + (((fgR - bgR) * alpha) >> 8);
				int outG = bgG + (((fgG - bgG) * alpha) >> 8);
				int outB = bgB + (((fgB - bgB) * alpha) >> 8);
				int outA = bgA + (((fgA - bgA) * alpha) >> 8);

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case CompositingMode.ALPHA_ADD:
			{
				if (alpha == 0) { return bg; }

				int bgA = bg >>> 24, bgR = (bg >> 16) & 0xFF, bgG = (bg >> 8) & 0xFF, bgB = bg & 0xFF;
				int fgR = (fg >> 16) & 0xFF, fgG = (fg >> 8) & 0xFF, fgB = fg & 0xFF;

				int outR = bgR + ((fgR * alpha) >> 8); if (outR > 255) outR = 255;
				int outG = bgG + ((fgG * alpha) >> 8); if (outG > 255) outG = 255;
				int outB = bgB + ((fgB * alpha) >> 8); if (outB > 255) outB = 255;
				int outA = bgA + ((alpha * (255 - bgA)) >> 8);
				if (outA > 255) { outA = 255; }

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case CompositingMode.MODULATE:
			{
				int bgRB = bg & 0x00FF00FF;
				int fgRB = fg & 0x00FF00FF;

				int r = (((bgRB >> 16) * (fgRB >> 16)) >> 8) & 0xFF;
				int b = (((bgRB & 0xFF) * (fgRB & 0xFF)) >> 8) & 0xFF;

				int bgAG = (bg >>> 8) & 0x00FF00FF;
				int fgAG = (fg >>> 8) & 0x00FF00FF;
				int a = (((bgAG >> 16) * (fgAG >> 16)) >> 8) & 0xFF;
				int g = (((bgAG & 0xFF) * (fgAG & 0xFF)) >> 8) & 0xFF;

				return (a << 24) | (r << 16) | (g << 8) | b;
			}

			case CompositingMode.MODULATE_X2:
			{
				int bgA = bg >>> 24, bgR = (bg >> 16) & 0xFF, bgG = (bg >> 8) & 0xFF, bgB = bg & 0xFF;
				int fgA = fg >>> 24, fgR = (fg >> 16) & 0xFF, fgG = (fg >> 8) & 0xFF, fgB = fg & 0xFF;

				int outR = (fgR * bgR) >> 7;
				int outG = (fgG * bgG) >> 7;
				int outB = (fgB * bgB) >> 7;
				int outA = (fgA * bgA) >> 7;

				outR = (outR | -(outR >> 8)) & 0xFF;
				outG = (outG | -(outG >> 8)) & 0xFF;
				outB = (outB | -(outB >> 8)) & 0xFF;
				outA = (outA | -(outA >> 8)) & 0xFF;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case Texture2D.FUNC_ADD:
			{
				int fR = (bg >> 16) & 0xFF, fG = (bg >> 8) & 0xFF, fB = bg & 0xFF, fA = bg >>> 24;
				int tR = (fg >> 16) & 0xFF, tG = (fg >> 8) & 0xFF, tB = fg & 0xFF, tA = fg >>> 24;

				int outR = fR, outG = fG, outB = fB;
				if (texFormat != Image2D.ALPHA)
				{
					outR = fR + tR; if (outR > 255) outR = 255;
					outG = fG + tG; if (outG > 255) outG = 255;
					outB = fB + tB; if (outB > 255) outB = 255;
				}

				boolean hasAlpha = (texFormat == Image2D.ALPHA
					|| texFormat == Image2D.LUMINANCE_ALPHA || texFormat == Image2D.RGBA);
				int outA = hasAlpha ? (fA * tA) >> 8 : fA;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case Texture2D.FUNC_BLEND:
			{
				int fA = bg >>> 24, tA = fg >>> 24;
				boolean hasAlpha = (texFormat == Image2D.ALPHA || texFormat == Image2D.LUMINANCE_ALPHA || texFormat == Image2D.RGBA);
				int outA = hasAlpha ? (fA * tA) >> 8 : fA;

				if (texFormat == Image2D.ALPHA) { return (outA << 24) | (bg & 0x00FFFFFF); }

				int tR = (fg >> 16) & 0xFF, tG = (fg >> 8) & 0xFF, tB = fg & 0xFF;
				int factor = (tR + tG + tB) / 3;
				if (factor == 0) { return (outA << 24) | (bg & 0x00FFFFFF); }

				// Blend is the only one that uses the texture's blend color
				int fRB = bg & 0x00FF00FF, cRB = texBlendColor & 0x00FF00FF;
				int outRB = (fRB + ((((cRB - fRB) * factor) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				int fAG = (bg >>> 8) & 0x00FF00FF, cAG = (texBlendColor >>> 8) & 0x00FF00FF;
				int outAG = (fAG + ((((cAG - fAG) * factor) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				return (outA << 24) | ((outRB | (outAG << 8)) & 0x00FFFFFF);
			}

			case Texture2D.FUNC_DECAL:
			{
				if (texFormat == Image2D.RGB) { return (bg & 0xFF000000) | (fg & 0x00FFFFFF); }
				else if (texFormat == Image2D.RGBA)
				{
					int tA = fg >>> 24;
					if (tA == 0)   { return bg; }
					if (tA == 255) { return (bg & 0xFF000000) | (fg & 0x00FFFFFF); }

					int fRB = bg & 0x00FF00FF, tRB = fg & 0x00FF00FF;
					int outRB = (fRB + ((((tRB - fRB) * tA) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

					int fAG = (bg >>> 8) & 0x00FF00FF, tAG = (fg >>> 8) & 0x00FF00FF;
					int outAG = (fAG + ((((tAG - fAG) * tA) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

					return (bg & 0xFF000000) | ((outRB | (outAG << 8)) & 0x00FFFFFF);
				}

				// TODO: DECAL is undefined for ALPHA, LUMINANCE, and LUMINANCE_ALPHA, so we just
				// don't do any blending. Is this the same on vendor implementations? No idea.
				return bg;
			}

			case Texture2D.FUNC_MODULATE:
			{
				int fR = (bg >> 16) & 0xFF, fG = (bg >> 8) & 0xFF, fB = bg & 0xFF, fA = bg >>> 24;
				int tR = (fg >> 16) & 0xFF, tG = (fg >> 8) & 0xFF, tB = fg & 0xFF, tA = fg >>> 24;

				int outR, outG, outB, outA;

				if (texFormat == Image2D.ALPHA)
				{
					outR = fR;
					outG = fG;
					outB = fB;
				}
				else
				{
					int pR = fR * tR + 1; outR = (pR + (pR >> 8)) >> 8;
					int pG = fG * tG + 1; outG = (pG + (pG >> 8)) >> 8;
					int pB = fB * tB + 1; outB = (pB + (pB >> 8)) >> 8;
				}

				if (texFormat == Image2D.ALPHA || texFormat == Image2D.LUMINANCE_ALPHA
						|| texFormat == Image2D.RGBA)
				{
					int pA = fA * tA + 1;
					outA = (pA + (pA >> 8)) >> 8;
				}
				else { outA = fA; }

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case Texture2D.FUNC_REPLACE:
				// RGB & LUMINANCE don't carry an alpha channel, so we use the bg alpha
				if (texFormat == Image2D.RGB || texFormat == Image2D.LUMINANCE)
					{ return (bg & 0xFF000000) | (fg & 0x00FFFFFF); }

				// ALPHA format only carries alpha, so we use the bg color
				if (texFormat == Image2D.ALPHA) { return (fg & 0xFF000000) | (bg & 0x00FFFFFF); }

				// RGBA and LUMINANCE_ALPHA just replace bg completely.
				return fg;

			// Special case for fog blending
			case Graphics3D.BLEND_FOG:
			{
				/*
				 * M3G specifies that, the smaller the fogFactor value, the more we
				 * should blend the fog color into the received color... which means
				 * that the fog's contribution to the resulting color should be
				 * 1 - fogFactor;
				 */
				final int bgRB = bg & 0x00FF00FF;
				final int bgG  = (bg >> 8) & 0xFF;

				final int fgRB = fg & 0x00FF00FF;
				final int fgG  = (fg >> 8) & 0xFF;

				final int r = ((fgRB >> 16) + ((((bgRB >> 16) - (fgRB >> 16)) * alpha) >> 8)) & 0xFF;
				final int g = (fgG          + ((((bgG)          - (fgG))          * alpha) >> 8)) & 0xFF;
				final int b = ((fgRB & 0xFF)+ ((((bgRB & 0xFF)  - (fgRB & 0xFF))  * alpha) >> 8)) & 0xFF;

				return (bg & 0xFF000000) | (r << 16) | (g << 8) | b;
			}

			// Special case for AA coverage blending
			case Graphics3D.BLEND_COVERAGE:
			{
				if (alpha <= 0)   { return bg; }
				if (alpha >= 255) { return fg; }

				int bgRB = bg & 0x00FF00FF;
				int fgRB = fg & 0x00FF00FF;
				int outRB = (bgRB + ((((fgRB - bgRB) * alpha) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				int bgAG = (bg >>> 8) & 0x00FF00FF;
				int fgAG = (fg >>> 8) & 0x00FF00FF;
				int outAG = (bgAG + ((((fgAG - bgAG) * alpha) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				return outRB | (outAG << 8);
			}

			default:
				return bg;
		}
	}

	// Calculates the Mipmap LOD for a given pixel.
	private float calculateLOD(float dsdx, float dtdx, float dsdy, float dtdy, boolean linear)
	{
		if (linear)
		{
			// Area-based geometric footprint (Determinant magnitude: |ds/dx * dt/dy - dt/dx * ds/dy|)
			// Keeps elongated polygons (like ground planes/roads) much sharper along their primary axis,
			// and seems to align with OpenGL's linear filter slope
			float area = M3GMath.abs(dsdx * dtdy - dtdx * dsdy);

			if (area <= 1.0f) { return 0.0f; }

			// M3GMath.log(area) already returns log2(area).
			// For area = scale^2, log2(scale^2) * 0.5 == 0.5 * log2(area).
			return M3GMath.log(area) * 0.5f;
		}
		else
		{
			// Simpler max of squared distance, seems to align with OpenGL's nearest
			// mipmap filter slope.
			float lengthXSq = dsdx * dsdx + dtdx * dtdx;
			float lengthYSq = dsdy * dsdy + dtdy * dtdy;

			float maxSq = (lengthXSq > lengthYSq) ? lengthXSq : lengthYSq;

			if (maxSq <= 1.0f) { return 0.0f; }

			// Similar optimization to above.
			// Since log2(x^2) = 2 * log2(x), taking 0.5 * log2(maxSq) simplifies
			// this directly to M3GMath.log(maxSq) * 0.5f!
			return M3GMath.log(maxSq) * 0.5f;
		}
	}

	// For bilinear filtering support
	private static final int sampleBilinear(Image2D teximg, float s, float t, int texW, int texH, boolean texRepeatS, boolean texRepeatT, boolean isNPOT)
	{
		// Shift s and t by 0.5 for OpenGL-like filtering,
		int uFixed = M3GMath.floor((s - 0.5f) * 256.0f);
		int vFixed = M3GMath.floor((t - 0.5f) * 256.0f);

		int x0 = uFixed >> 8;
		int y0 = vFixed >> 8;
		int x1 = x0 + 1;
		int y1 = y0 + 1;

		int fx = uFixed & 0xFF;
		int fy = vFixed & 0xFF;

		x0 = wrapX(x0, texW, texRepeatS, isNPOT);
		x1 = wrapX(x1, texW, texRepeatS, isNPOT);
		y0 = wrapY(y0, texH, texRepeatT, isNPOT);
		y1 = wrapY(y1, texH, texRepeatT, isNPOT);

		int c00 = teximg.getPixel(x0, y0);
		int c10 = teximg.getPixel(x1, y0);
		int c01 = teximg.getPixel(x0, y1);
		int c11 = teximg.getPixel(x1, y1);

		int rb0 = (c00 & 0x00FF00FF) + ((((c10 & 0x00FF00FF) - (c00 & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;
		int ag0 = ((c00 >>> 8) & 0x00FF00FF) + (((((c10 >>> 8) & 0x00FF00FF) - ((c00 >>> 8) & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;

		int rb1 = (c01 & 0x00FF00FF) + ((((c11 & 0x00FF00FF) - (c01 & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;
		int ag1 = ((c01 >>> 8) & 0x00FF00FF) + (((((c11 >>> 8) & 0x00FF00FF) - ((c01 >>> 8) & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;

		int rb = rb0 + ((((rb1 - rb0) * fy) >> 8) & 0x00FF00FF);
		int ag = ag0 + ((((ag1 - ag0) * fy) >> 8) & 0x00FF00FF);

		return (ag << 8) | rb;
	}

	// Helpers for texture wrapping/clamping
	// JSR-184 texture wrapping: REPEAT tiles the image, CLAMP samples the edge.
	// Out-of-range coordinates must never index outside the image.
	private static final int wrapX(int x, int width, boolean repeat, boolean isNPOT)
	{
		if (repeat)
		{
			// If the texture is Power-Of-Two, repeat wrapping can be done
			// quickly as just an AND of the coordinate with the the edge
			// mask (which is width - 1). Why is that? A POT texture has
			// the following property: (2 - 1 = 1 = `0b1`, 4 - 1 = 3 = `0b11`,
			// 8 - 1 = 7 = `0b111`, and so on), so we always wrap around to the
			// correct coordinate with an AND of size - 1, as overflowing data
			// will naturally wrap back to the start.
			if(!isNPOT) { return x & (width - 1); }

			// If it is NPOT we must fallback to modulo, as an AND would not
			// result in the proper coordinate.
			int r = x % width;
			return r < 0 ? r + width : r;
		}

		// CLAMP is fast for both POT and NPOT
		if (x < 0) { return 0; }
		if (x >= width) { return (width - 1); }

		return x;
	}

	private static final int wrapY(int y, int height, boolean repeat, boolean isNPOT)
	{
		if (repeat)
		{
			if(!isNPOT) { return y & (height - 1); }

			int r = y % height;
			return r < 0 ? r + height : r;
		}

		if (y < 0) { return 0; }
		if (y >= height) { return (height - 1); }

		return y;
	}
}
