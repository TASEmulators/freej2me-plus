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
		0, 8, 2, 10,
		12, 4, 14, 6,
		3, 11, 1, 9,
		15, 7, 13, 5
	};

	// Pre-computed 1/N lookup table for span sizes 1 to 32. FreeJ2ME+ will
	// allow configurable span sizes for piecewise linear perspective correction
	// from 8 to 32 pixels in the future, so by precalculating these here at
	// the start, we don't need to keep throwing fastReciprocals on the inner
	// render loop.
	private static final float[] INV_SPAN_TABLE = new float[33];

	static
	{
	    INV_SPAN_TABLE[0] = 0.0f;
	    for (int i = 1; i <= 32; i++)
		{
	        INV_SPAN_TABLE[i] = M3GMath.fastReciprocal((float) i);
	    }
	}

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
	final boolean[] useBilinear = new boolean[NUM_TEXTURE_UNITS];
	final boolean[] texRepeatS = new boolean[NUM_TEXTURE_UNITS];
	final boolean[] texRepeatT = new boolean[NUM_TEXTURE_UNITS];
	final float[] curS = new float[NUM_TEXTURE_UNITS];
	final float[] curT = new float[NUM_TEXTURE_UNITS];
	final float[] stepS = new float[NUM_TEXTURE_UNITS];
	final float[] stepT = new float[NUM_TEXTURE_UNITS];
	final float[] texScaleBias = new float[4];
	final float[] dsL_dy = new float[NUM_TEXTURE_UNITS];
	final float[] dtL_dy = new float[NUM_TEXTURE_UNITS];
	final float[] sL = new float[NUM_TEXTURE_UNITS];
	final float[] tL = new float[NUM_TEXTURE_UNITS];
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
	final float[] sStepX = new float[NUM_TEXTURE_UNITS];
	final float[] sStepY = new float[NUM_TEXTURE_UNITS];
	final float[] tStepX = new float[NUM_TEXTURE_UNITS];
	final float[] tStepY = new float[NUM_TEXTURE_UNITS];
	final int[] texblendMode = new int[NUM_TEXTURE_UNITS];
	final int[] levelFilters = new int[NUM_TEXTURE_UNITS];
	final float[][] texVerts = new float[NUM_TEXTURE_UNITS][];
	final Transform[] textr = new Transform[NUM_TEXTURE_UNITS];
	final Texture2D[] textures = new Texture2D[NUM_TEXTURE_UNITS];

	// 3D rendering variables
	static byte ACTIVE_TEXTURE_UNITS;
	final Transform normalMatrix;
	final Transform tr;
	int yStart, yEnd;
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
	int deltaR = 0, deltaG = 0, deltaB = 0, deltaA = 0;
	int stepA = 0, stepR = 0, stepG = 0, stepB = 0;

	final float[] scaleBias = new float[4];

	final Transform projectionMatrix = new Transform();
	final int[] renderableTriangles = {0}; // Counter for visible triangles


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
		texcomptr = new Transform();
		for(int i = 0; i < NUM_TEXTURE_UNITS; i++) { textr[i] = new Transform(); }
	}


	public int addLight(Light light, Transform transform)
	{
		/* As per JSR-184, addLight() must throw a NullPointerException if no light is given */
		if (light == null) { throw new NullPointerException("addLight() was called but no light object was provided."); }

		// We specify a limit, but only because its required,
		// and i really doubt any app will use more than 32
		// lights per mesh. SO even if they do, just accept
		// silently as it doesn't result in such a massive
		// increase to runtime due to when they're calculated.
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
		Arrays.fill(this.depthBuffer, (short) M3GMath.round(this.far * 32767.0f));
		this.depthEnabled = depthBuffer;
		this.hints = hints;
	}

	public void clear(Background background)
	{
		/*
		 * As per JSR-184, this should throw IllegalStateException if this Graphics3D object does not
		 * have a render target. However, some games and demos were written against lenient phone
		 * implementations that treated clear() without a bound target as a silent no-op, and calling
		 * it mid-paint would abort the whole frame here, so just warn and ignore the call instead.
		 */
		if (this.target == null)
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "clear() called without a render target, ignoring.");
			return;
		}

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
			final Image2D bgImg = (background != null) ? background.getImage() : null;

			if (this.target instanceof Graphics)
			{
				Graphics grp = (Graphics) this.target;

				/*
				 * As per JSR-184, clear() always affects the whole viewport: fill it with the
				 * background color first. The Background crop rectangle is a sampling window
				 * into the background image, NOT the destination rectangle.
				 */
				grp.setColor(color);
				grp.fillRect(viewx, viewy, vieww, viewh);

				if (bgImg != null) { clearToTarget(background, bgImg, false); }
			}
			else if (this.target instanceof Image2D)
			{
				Image2D i2d = (Image2D) this.target;

				if(bgImg != null && bgImg.getFormat() != i2d.getFormat())
					{ throw new IllegalArgumentException("The background image to be cleared does not have the same format as the render target."); }

				// Clear with color first
				for (int py = 0; py < viewh; py++)
				{
					int screenY = py + viewy;
					for (int px = 0; px < vieww; px++)
					{
						i2d.setPixel(px + viewx, screenY, color);
					}
				}

				if (bgImg != null) { clearToTarget(background, bgImg, true); }
			}
		}

		if (clearDepth) { Arrays.fill(this.depthBuffer, (short) M3GMath.round(this.far * 32767.0f)); }
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

		/*
		 * Also per JSR-184, this should throw IllegalStateException when there's no render target
		 * yet. Lenient no-op instead (see the comment in clear() for the rationale).
		 */
		if (this.target == null)
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "render(world) called without a render target, ignoring.");
			return;
		}

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

		/*
		 * Also per JSR-184, this should throw IllegalStateException when there's no camera or render
		 * target available. Lenient no-op instead (see the comment in clear() for the rationale).
		 */
		if (this.target == null || this.currCam == null)
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "render(node) called without a camera or render target, ignoring.");
			return;
		}

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

		/*
		 * Also per JSR-184, this should throw IllegalStateException if the application tries to render
		 * without having set up a render target or camera beforehand. Lenient no-op instead (see the
		 * comment in clear() for the rationale).
		 */
		if (this.target == null || this.currCam == null)
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "render(submesh) called without a camera or render target, ignoring.");
			return;
		}

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

		// Set up fog properties
		final Fog fog = Mobile.m3gDisableFog ? null : appearance.getFog();
		final float invFogDiv = fog != null ? M3GMath.fastReciprocal(fog.getFarDistance() - fog.getNearDistance()) : 0.0f;

		// This one can be overridden by FJ2ME+
		boolean perspectiveCorrection = appearance.getPolygonMode() != null ? appearance.getPolygonMode().isPerspectiveCorrectionEnabled() : false;
		perspectiveCorrection = fog != null || (perspectiveCorrection && (projType == Camera.PERSPECTIVE)); // fog usage enables it
		perspectiveCorrection = (Mobile.m3gPerspectiveCorrectionMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gPerspectiveCorrectionMode == MODE_APP_CONTROLLED && perspectiveCorrection);


		// We'll need the projection matrix for the next transformations
		this.currCam.getProjection(projectionMatrix);

		// This one is also used by all position calculations
		final VertexArray vertPos = vertices.getPositions(scaleBias);

		// Setup texture units first, if we have to use any. Texturing is done
		// by layer, with each texture unit blending on top of another.
		boolean hasTexture = false;

		ACTIVE_TEXTURE_UNITS = 0;
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

					// joint-increment ACTIVE_TEXTURE_UNITS alongside assignment.
					textures[ACTIVE_TEXTURE_UNITS++] = t;
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

			/*
			 * JSR-184 states that lighting is undefined for a non-invertible
			 * local-to-camera transform, and since undefined means we can treat
			 * this any way we want, we'll set the matrix as the identity and
			 * soldier onwards.
			 */
			try
			{
				normalMatrix.invert();
				normalMatrix.transpose();
			}
			catch (ArithmeticException ae)
			{
				Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Normal matrix not invertible. Using identity...");
				normalMatrix.setIdentity();
			}

			tr.setIdentity();
			if (this.currCamTransInv != null) { tr.postMultiply(this.currCamTransInv); }
			if (transform != null) { tr.postMultiply(transform); }
			tr.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
			tr.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);

			if (eyePos == null || 4 * vertPos.getVertexCount() > eyePos.length)
				{ eyePos = new float[4 * vertPos.getVertexCount()]; }

			tr.transform(vertPos, eyePos, true);
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
			if (this.currCamTransInv != null) { tr.postMultiply(this.currCamTransInv); }
			if (lightTrans != null) { tr.postMultiply(lightTrans); }

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

			// We also need to normalize the light direction vector.
			float dirLen = M3GMath.sqrt(lightVec[0]*lightVec[0] + lightVec[1]*lightVec[1] +
				lightVec[2]*lightVec[2]);
			if (dirLen > 0.0f)
			{
				lightVec[0] /= dirLen;
				lightVec[1] /= dirLen;
				lightVec[2] /= dirLen;
			}
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
			this.currLights, lightEyePos, lightEyeDir, scope,
			// IndexArray, clipping, winding order and perspectiveCorrection
			triangles.getIndexArray(), renderableTriangles, cullingMode, vertices,
			windingOrder == PolygonMode.WINDING_CW, perspectiveCorrection);

		// At this point the triangles in `trisScreen` are actually
		// projected to Normalized Device Coordinates, but they will be tranformed
		// to Screen space in-place, hence the name.

		// Reset transform
		tr.setIdentity();

		for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++) { textr[i].setIdentity(); }


		// Fit to viewport. Notice that Z is scaled slightly below the max limits
		// for shorts (which is -32768, 32767), this is to make sure the
		// multiplied Z values will always be in range and never overflow,
		// saving us the need to clamp it for every pixel draw.
		//
		// NOTE: that last mult by 2.0f and negative translation by 0.5 is
		// just a hack to improve depth buffer range usage.
		tr.postScale(vieww / 2f, -viewh / 2f, (this.far - this.near) * 32767.0f);
		tr.postTranslate(1f, -1f, 0f);

		// -> Screen space

		// Perform viewport transform only on renderable triangles (saves an Arrays.copyOf call)
		Triangle.transform(trisScreen, renderableTriangles[0], tr, textr);

		final boolean usesDepth = this.depthEnabled && compositingMode.isDepthTestEnabled() && isDepthBufferEnabled();
		final float depthUnits = compositingMode.getDepthOffsetUnits();
		final float depthFactor = compositingMode.getDepthOffsetFactor();
		final boolean hasDepthOffset = usesDepth && (depthFactor != 0.0f || depthUnits != 0.0f);
		float depthOffset = 0.0f;

		final boolean colorEnabled = compositingMode.isColorWriteEnabled();
		final int alphaThreshold = (int) (compositingMode.getAlphaThreshold() * 255);

		if (hasTexture)
		{
			for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
			{
				useBilinear[i] = (Mobile.m3gBilinearFilterMode == MODE_FORCE_ENABLE)
					|| (Mobile.m3gBilinearFilterMode == MODE_APP_CONTROLLED &&
					((textures[i].getImageFilter() == Texture2D.FILTER_LINEAR)));
			}
		}

		for (int tri_id = 0; tri_id < renderableTriangles[0]; tri_id++)
		{
			final Triangle tri = trisScreen[tri_id];

			final float xA = tri.xA(), xB = tri.xB(), xC = tri.xC();
			final float yA = tri.yA(), yB = tri.yB(), yC = tri.yC();

			final float dxB = xB - xA, dyB = yB - yA;
			final float dxC = xC - xA, dyC = yC - yA;
			final float denominator = dxB * dyC - dxC * dyB;

			// Degenerate triangle? Skip it.
			if (M3GMath.abs(denominator) <= M3GMath.EPSILON) { continue; }

			// We don't draw wireframes to Image2Ds
			if (Mobile.M3GRenderWireframe && !(this.target instanceof Image2D))
			{
				final PlatformGraphics pgrp = (PlatformGraphics) this.target;
				int tempcolor = pgrp.getColor();
				pgrp.setColor(0xFF000000 | tri.colorA());
				pgrp.drawTriangle((int) xA, (int) yA, (int) xB, (int) yB, (int) xC, (int) yC);
				pgrp.setColor(tempcolor);
				continue;
			}

			coX[0] = xA; coX[1] = xB; coX[2] = xC;
			coY[0] = yA; coY[1] = yB; coY[2] = yC;
			coZ[0] = tri.zA();  coZ[1] = tri.zB();  coZ[2] = tri.zC();
			coW[0] = tri.iwA(); coW[1] = tri.iwB(); coW[2] = tri.iwC();

			final float invDet = M3GMath.fastReciprocal(denominator);

			if (hasDepthOffset)
			{
				final float dz10 = coZ[1] - coZ[0];
				final float dz20 = coZ[2] - coZ[0];

				final float dzdx = (dz10 * dyC - dz20 * dyB) * invDet;
				final float dzdy = (dxB * dz20 - dxC * dz10) * invDet;

				final float m = M3GMath.sqrt(dzdx * dzdx + dzdy * dzdy);
				depthOffset = (depthFactor * m) + (depthUnits * 1e-7f);
			}

			// Let's precalculate uv derivatives for mipmapping. Skips having
			// to do expensive calculations inside the inner render loops.
			if (hasTexture)
			{
				for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
				{
					coS[i][0] = tri.sA(i); coS[i][1] = tri.sB(i); coS[i][2] = tri.sC(i);
					coT[i][0] = tri.tA(i); coT[i][1] = tri.tB(i); coT[i][2] = tri.tC(i);
				}

				dwdx = 0.0f;
				dwdy = 0.0f;

				if (perspectiveCorrection)
				{
					final float dwB = coW[1] - coW[0];
					final float dwC = coW[2] - coW[0];

					dwdx = (dwB * dyC - dwC * dyB) * invDet;
					dwdy = (dwC * dxB - dwB * dxC) * invDet;
				}

				// For perspective correction, we need the actual W of
				// each vertex as well.
				for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
				{
					if (perspectiveCorrection)
					{
						final float wA = tri.wA(), wB = tri.wB(), wC = tri.wC();
						final float swA = coS[i][0] * wA, swB = coS[i][1] * wB, swC = coS[i][2] * wC;
						final float twA = coT[i][0] * wA, twB = coT[i][1] * wB, twC = coT[i][2] * wC;

						final float dswB = swB - swA, dswC = swC - swA;
						final float dtwB = twB - twA, dtwC = twC - twA;

						sStepX[i] = (dswB * dyC - dswC * dyB) * invDet; // d(s/w)/dx
						tStepX[i] = (dtwB * dyC - dtwC * dyB) * invDet; // d(t/w)/dx

						sStepY[i] = (dswC * dxB - dswB * dxC) * invDet; // d(s/w)/dy
						tStepY[i] = (dtwC * dxB - dtwB * dxC) * invDet; // d(t/w)/dy
					}
					else
					{
						final float dsB = coS[i][1] - coS[i][0], dsC = coS[i][2] - coS[i][0];
						final float dtB = coT[i][1] - coT[i][0], dtC = coT[i][2] - coT[i][0];

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
			final boolean hasColors = tri.hasVertexColors();
			if (hasColors)
			{
				final int colorA = tri.colorA();
				final int colorB = tri.colorB();
				final int colorC = tri.colorC();

				final float aA = (colorA >> 24) & 0xFF, rA = (colorA >> 16) & 0xFF, gA = (colorA >> 8) & 0xFF, bA = colorA & 0xFF;
				final float aB = (colorB >> 24) & 0xFF, rB = (colorB >> 16) & 0xFF, gB = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
				final float aC = (colorC >> 24) & 0xFF, rC = (colorC >> 16) & 0xFF, gC = (colorC >> 8) & 0xFF, bC = colorC & 0xFF;

				// To properly use additions in the triangle render loops
				// below, we need to calculate the derivatives for each
				// color channel, on each axis.
				final float dR_B = rB - rA, dR_C = rC - rA;
				final float dG_B = gB - gA, dG_C = gC - gA;
				final float dB_B = bB - bA, dB_C = bC - bA;
				final float dA_B = aB - aA, dA_C = aC - aA;

				rStepX = (dR_B * dyC - dR_C * dyB) * invDet;
				gStepX = (dG_B * dyC - dG_C * dyB) * invDet;
				bStepX = (dB_B * dyC - dB_C * dyB) * invDet;
				aStepX = (dA_B * dyC - dA_C * dyB) * invDet;

				rStepY = (dR_C * dxB - dR_B * dxC) * invDet;
				gStepY = (dG_C * dxB - dG_B * dxC) * invDet;
				bStepY = (dB_C * dxB - dB_B * dxC) * invDet;
				aStepY = (dA_C * dxB - dA_B * dxC) * invDet;

				stepA = (int) (aStepX * 65536.0f);
				stepR = (int) (rStepX * 65536.0f);
				stepG = (int) (gStepX * 65536.0f);
				stepB = (int) (bStepX * 65536.0f);
			}

			// x and y coordinates are special cases where the resulting top, mid and bot values should be in decreasing order (top > mid > bot)
			coY[0] = yA; coY[1] = yB; coY[2] = yC;
			int top = 0, mid = 1, bot = 2;
			if (coY[mid] < coY[top]) { int t = top; top = mid; mid = t; }
			if (coY[bot] < coY[top]) { int t = top; top = bot; bot = t; }
			if (coY[bot] < coY[mid]) { int t = mid; mid = bot; bot = t; }

			// Degenerate triangle? Skip it.
			if (M3GMath.abs(coY[bot] - coY[top]) < M3GMath.EPSILON) { continue; }

			// Assign ordered vertex attributes based on their determined order
			xTop = coX[top]; xMidL = coX[mid]; xBot = coX[bot];
			yTop = coY[top]; yMid = coY[mid]; yBot = coY[bot];
			zTop = coZ[top]; zMidL = coZ[mid]; zBot = coZ[bot];
			pwTop = coW[top]; pwMidL = coW[mid]; pwBot = coW[bot];

			if (hasTexture)
			{
				for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
				{
					sTop[i] = coS[i][top]; sMidL[i] = coS[i][mid]; sBot[i] = coS[i][bot];
					tTop[i] = coT[i][top]; tMidL[i] = coT[i][mid]; tBot[i] = coT[i][bot];
				}
			}

			// Calculate Triangle's right horizon midpoints
			rHorizon = (yMid - yTop) * M3GMath.fastReciprocal(yBot - yTop);
			xMidR = xTop + rHorizon * (xBot - xTop);
			zMidR = zTop + rHorizon * (zBot - zTop);
			pwMidR = pwTop + rHorizon * (pwBot - pwTop);

			if (hasTexture)
			{
				for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
				{
					sMidR[i] = sTop[i] + rHorizon * (sBot[i] - sTop[i]);
					tMidR[i] = tTop[i] + rHorizon * (tBot[i] - tTop[i]);
					texblendMode[i] = ((textures[i].getBlending() & 7) << 3) |
						(textures[i].getImage().getFormat() & 7);

					switch(Mobile.m3gMipmapMode)
					{
						case MODE_FORCE_DISABLE:
							levelFilters[i] = Texture2D.FILTER_BASE_LEVEL;
							break;
						case MODE_APP_CONTROLLED:
							levelFilters[i] = textures[i].getLevelFilter();
							break;
						case MODE_FORCE_ENABLE:
							levelFilters[i] = Texture2D.FILTER_NEAREST;
							break;
						case 3: // FORCE_LINEAR
							levelFilters[i] = Texture2D.FILTER_LINEAR;
							break;
					}
				}
			}

			// Swap Midpoints if triangle left > triangle right
			if (xMidL > xMidR)
			{
				float temp;
				temp = xMidL; xMidL = xMidR; xMidR = temp;
				temp = zMidL; zMidL = zMidR; zMidR = temp;
				temp = pwMidL; pwMidL = pwMidR; pwMidR = temp;

				if (hasTexture)
				{
					for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
					{
						temp = sMidL[i]; sMidL[i] = sMidR[i]; sMidR[i] = temp;
						temp = tMidL[i]; tMidL[i] = tMidR[i]; tMidR[i] = temp;
					}
				}
			}

			// 0 width triangles get skipped entirely too.
			if (xMidR - xMidL < M3GMath.EPSILON) { continue; }

			float invMidSpan = M3GMath.fastReciprocal(xMidR - xMidL);

			// Draw both halves of the triangle, starting with the top one.
			yStart = M3GMath.max(M3GMath.ceil(yTop), 0);
			yEnd = M3GMath.min(M3GMath.ceil(yMid), viewh);

			if (yStart < yEnd)
			{
				renderTriangleHalf(vertices.getDefaultColor(), 0, yStart, yEnd, tri, hasColors, hasTexture, compositingMode,
					fog, invFogDiv, alphaThreshold, usesDepth, colorEnabled, depthOffset, perspectiveCorrection,
					invMidSpan);
			}

			yStart = M3GMath.max(M3GMath.ceil(yMid), 0);
			yEnd = M3GMath.min(M3GMath.ceil(yBot), viewh);

			if (yStart < yEnd)
			{
				renderTriangleHalf(vertices.getDefaultColor(), 1, yStart, yEnd, tri, hasColors, hasTexture, compositingMode,
					fog, invFogDiv, alphaThreshold, usesDepth, colorEnabled, depthOffset, perspectiveCorrection,
					invMidSpan);
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
			if (this.depthBuffer != null) { Arrays.fill(this.depthBuffer, (short) M3GMath.round(this.far * 32767.0f)); }
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

		/*
		 * The depth buffer is indexed in viewport-relative coordinates (y * vieww + x),
		 * and bindTarget sizes it for the viewport in effect at bind time. JSR-184 allows
		 * setViewport to be called at any point between bind and release with a viewport
		 * larger than the bind-time one (e.g. binding with a small clip rect, then setting
		 * a fullscreen viewport), so grow the buffer here or the rasterizer will index
		 * out of bounds. Its previous contents are laid out for the old width and thus
		 * meaningless for the new viewport either way, so reset it to the far plane.
		 */
		if (this.depthBuffer != null && this.depthBuffer.length < width * height)
		{
			this.depthBuffer = new short[width * height];
			Arrays.fill(this.depthBuffer, (short) M3GMath.round(this.far * 32767.0f));
		}
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
		boolean renderToImage = false;
		Image2D imageData = null;

		if((this.target instanceof Image2D))
		{
			renderToImage = true;
			imageData = (Image2D) this.target;
		}

		final Image2D spr = sprite.getImage();
		final Appearance appearance = sprite.getAppearance();

		// As per JSR-184, a Sprite3D with no appearance (or no image) is not rendered.
		if (spr == null || appearance == null) { return; }
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
		final int isectW = M3GMath.min(cropX + cropW, spr.getWidth()) - isectX;
		final int isectH = M3GMath.min(cropY + cropH, spr.getHeight()) - isectY;
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


		// Our depth buffer is now comprised of short values, so ndcZ has to be
		// multiplied by the same factor used by the buffer.
		// NOTE: that last mult by 2.0f and negative translation by 0.5 is
		// just a hack to improve depth buffer range usage, mirroring the triangle
		// path. Clamp to short range so near depths don't wrap around when cast.
		short ndcZ = (short) (clip[2]/clip[3] * 32767.0f);

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
		final Fog fog = Mobile.m3gDisableFog ? null : appearance.getFog();
		final int alphaThreshold = (int) (compositingMode.getAlphaThreshold() * 255);
		final float alphaFactor = sprite.getAlphaFactor();
		final boolean depthTest = compositingMode.isDepthTestEnabled() && isDepthBufferEnabled();
		final boolean depthWrite = depthTest && compositingMode.isDepthWriteEnabled();
		float fogFactor = 255.0f;
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
			// Odd scanlines just copy from even ones in half res mode.
			if(Mobile.halfResM3GRaster && (y & 1) != 0 && !(this.target instanceof Image2D))
			{
				System.arraycopy(rasterData, ((y + viewy - 1) * canvasWidth + viewx),
					rasterData, ((y + viewy) * canvasWidth + viewx), canvasWidth);
				continue;
			}

			final float v = (y + 0.5f - sy0) / spanY;
			int texY = isectY + (int) ((flipY ? 1f - v : v) * isectH);
			if (texY < isectY) { texY = isectY; } else if (texY >= isectY + isectH) { texY = isectY + isectH - 1; }

			int rasterIdxY = (y + viewy) * canvasWidth + viewx;
			for (int x = pixL; x < pixR; x++)
			{
				// Depth test against the same buffer and convention used by triangles.
				if (depthTest && this.depthBuffer[this.vieww * y + x] < ndcZ) { continue; }

				final float u = (x + 0.5f - sx0) / spanX;
				int texX = isectX + (int) ((flipX ? 1f - u : u) * isectW);
				if (texX < isectX) { texX = isectX; } else if (texX >= isectX + isectW) { texX = isectX + isectW - 1; }

				paintPixel = spr.getPixel(texX, texY);
				int alpha = (int) (((paintPixel >> 24) & 0xFF) * alphaFactor);

				if (alpha < alphaThreshold || alpha == 0) { continue; }

				if (fog != null && fogFactor < 255.0f)
					{ paintPixel = blendFog(paintPixel, fog.getColor(), (int) fogFactor); }

				if(!renderToImage)
				{
					rasterData[rasterIdxY + x] =
						compositingMode.getBlending() == CompositingMode.REPLACE ? paintPixel :
						blendCompositing(rasterData[rasterIdxY + x], paintPixel, alpha,
							compositingMode.getBlending());
				}
				else
				{
					imageData.setPixel((x+viewx), (y + viewy),
						compositingMode.getBlending() == CompositingMode.REPLACE ? paintPixel :
						blendCompositing(imageData.getPixel((x+viewx), (y + viewy)), paintPixel, alpha,
							compositingMode.getBlending()));
				}

				if (depthWrite && (paintPixel >>> 24) >= 255) { this.depthBuffer[this.vieww * y + x] = ndcZ; }
			}
		}
	}

	private void renderTriangleHalf(int defVertColor, int half, int yStart, int yEnd,
		Triangle triScreen, boolean hasColors, boolean hasTexture, CompositingMode compositingMode,
		Fog fog, float invFogDiv, int alphaThreshold, boolean usesDepth, boolean colorEnabled,
		float depthOffset, boolean doPerspective, float invMidSpan)
	{
		// Prepare the flags that can be overridden by the UI.
		boolean doDither = (Mobile.m3gDitheringMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gDitheringMode == MODE_APP_CONTROLLED && (this.hints & DITHER) != 0);

		boolean doAntiAlias = (Mobile.m3gAntiAliasingMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gAntiAliasingMode == MODE_APP_CONTROLLED && (this.hints & ANTIALIAS) != 0);

		final boolean hasFog = fog != null;

		final float fogFarNorm = hasFog ? fog.getFarDistance() * invFogDiv : 0.0f;
		final float fogNearNorm = hasFog ? fog.getNearDistance() * invFogDiv : 0.0f;
		final float fogDensity = hasFog ? fog.getDensity() : 0.0f;
		final int fogMode = hasFog ? fog.getMode() : 0;

		float fogFactor = 255.0f;
		float stepFogFactor = 0.0f;

		boolean renderToImage = false;
		Image2D imageData = null;

		if((this.target instanceof Image2D))
		{
			renderToImage = true;
			imageData = (Image2D) this.target;
		}

		int compBlending = compositingMode.getBlending();
		final boolean usesReplace = (compBlending == CompositingMode.REPLACE);
		final boolean usesDepthWrite = usesDepth &&
			compositingMode.isDepthWriteEnabled();
		final int opaqueAlpha = usesReplace ? 0xFF000000 : 0x00000000;

		float xA = triScreen.xA();
		float yA = triScreen.yA();
		int colorA = 0;
		if (hasColors) { colorA = triScreen.colorA(); }
		final float cA = (colorA >> 24) & 0xFF;
		final float cR = (colorA >> 16) & 0xFF;
		final float cG = (colorA >> 8) & 0xFF;
		final float cB = colorA & 0xFF;

		// Get into the render loop proper.

		float zStep  = (zMidR - zMidL) * invMidSpan;
		float pwStep = (pwMidR - pwMidL) * invMidSpan;

		if (hasTexture)
		{
			for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
			{
				stepS[i] = (sMidR[i] - sMidL[i]) * invMidSpan;
				stepT[i] = (tMidR[i] - tMidL[i]) * invMidSpan;
			}
		}

		float yDiv = half == 0 ? M3GMath.fastReciprocal(yMid - yTop) : M3GMath.fastReciprocal(yBot - yMid);

		float subY = yStart - (half == 0 ? yTop : yMid);

		float dxL_dy  = (half == 0 ? xMidL - xTop : xBot - xMidL) * yDiv;
		float dxR_dy  = (half == 0 ? xMidR - xTop : xBot - xMidR) * yDiv;
		float dzL_dy  = (half == 0 ? zMidL - zTop  : zBot - zMidL)  * yDiv;
		float dpwL_dy = (half == 0 ? pwMidL - pwTop : pwBot - pwMidL) * yDiv;

		if (hasTexture)
		{
			for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
			{
				dsL_dy[i] = (half == 0 ? sMidL[i] - sTop[i] : sBot[i] - sMidL[i]) * yDiv;
				dtL_dy[i] = (half == 0 ? tMidL[i] - tTop[i] : tBot[i] - tMidL[i]) * yDiv;

				sL[i] = (half == 0 ? sTop[i] : sMidL[i]) + subY * dsL_dy[i];
				tL[i] = (half == 0 ? tTop[i] : tMidL[i]) + subY * dtL_dy[i];
			}
		}

		float xL  = (half == 0 ? xTop  : xMidL)  + subY * dxL_dy;
		float xR  = (half == 0 ? xTop  : xMidR)  + subY * dxR_dy;
		float zL  = (half == 0 ? zTop  : zMidL)  + subY * dzL_dy;
		float pwL = (half == 0 ? pwTop : pwMidL) + subY * dpwL_dy;

		for (int y = yStart; y < yEnd; y++, xL += dxL_dy, xR += dxR_dy, zL += dzL_dy, pwL += dpwL_dy)
		{
			// Odd scanlines just copy from even ones in half res mode.
			if(!renderToImage && Mobile.halfResM3GRaster && (y & 1) != 0)
			{
				System.arraycopy(rasterData, ((y + viewy - 1) * canvasWidth + viewx),
					rasterData, ((y + viewy) * canvasWidth + viewx), canvasWidth);

				if (hasTexture)
				{
					for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
					{
						sL[i] += dsL_dy[i];
						tL[i] += dtL_dy[i];
					}
				}
				continue;
			}

			int ixL = (int) (xL + 0.999999f);
			int ixR = (int) (xR + 0.999999f);

			if (ixL < 0) { ixL = 0; }
			if (ixR > vieww) { ixR = vieww; }

			if (ixL >= ixR)
			{
				if (hasTexture)
				{
					for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
					{
						sL[i] += dsL_dy[i];
						tL[i] += dtL_dy[i];
					}
				}
				continue;
			}

			// Do we have vertex colors? If so, get the span edges' colors here,
			// that way, the inner loop only needs to do a simple addition.
			if (hasColors)
			{
				float dx = ixL - xA;
				float dy = y - yA;

				// Everyone goes to 16.16 fixed point, innermost X loop can get colors right away
				// with this.
				deltaA = (int) ((((colorA >> 24) & 0xFF) + dx * aStepX + dy * aStepY) * 65536.0f);
				deltaR = (int) ((((colorA >> 16) & 0xFF) + dx * rStepX + dy * rStepY) * 65536.0f);
				deltaG = (int) ((((colorA >> 8)  & 0xFF) + dx * gStepX + dy * gStepY) * 65536.0f);
				deltaB = (int) (((colorA & 0xFF)         + dx * bStepX + dy * bStepY) * 65536.0f);
			}

			int depthIdx = (y * this.vieww) + ixL;
			int rasterIdx = ((y + viewy) * canvasWidth + viewx) + ixL;

			float pw = pwL + (ixL - xL) * pwStep;
			float invPw = doPerspective ? M3GMath.fastReciprocal(pw) : 1.0f;
			float stepInvPw = 0.0f;
			float z  = zL  + (ixL - xL) * zStep + depthOffset;

			if (hasTexture)
			{
				// We'll use DDA for texturing as well, saves many multiply and
				// add operations for each textured pixel.
				final float subpixelOffset = ixL - xL;

				for (byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
				{
					curS[i] = (sL[i]) + (subpixelOffset * stepS[i]);
					curT[i] = (tL[i]) + (subpixelOffset * stepT[i]);

					sL[i] += dsL_dy[i];
					tL[i] += dtL_dy[i];
				}
			}

			// Draw the pixels for the current y-coordinate
			for (int x = ixL; x < ixR; x++, z += zStep, pw += pwStep, invPw += stepInvPw, fogFactor += stepFogFactor, depthIdx++, rasterIdx++)
			{
				// Subsampling block. A.K.A, where we calculate anything that
				// is too expensive to run per-pixel but cannot be done only once
				// for the whole triangle Y scanline due to large precision loss.
				if (doPerspective && ((x & Mobile.m3gPerspCorrSubFactor) == 0 || x == ixL))
				{
					int maxSpan = (Mobile.m3gPerspCorrSubFactor + 1) -
						(x & Mobile.m3gPerspCorrSubFactor);
					int spanLen = (ixR - x < maxSpan) ? ixR - x : maxSpan;
					float invSpanLen = INV_SPAN_TABLE[spanLen];

					float nextPw = pw + pwStep * spanLen;

					// Calling M3GMath.fastReciprocal() in here was deemed
					// too expensive by the profiler, so we'll be inlining an
					// even simpler alternative in here instead
					// (single Newton-Raphson step)).
					float denom = pw * nextPw;
					int bits = Float.floatToRawIntBits(denom);
					float invDenom = Float.intBitsToFloat(0x7EF127EA - bits);
					invDenom = invDenom * (2.0f - denom * invDenom);

					stepInvPw = -pwStep * invDenom;

					// Compute start and end fog factors for this 16-pixel span
					if (hasFog)
					{
						float zEyeStart = invPw;
						float zEyeEnd = invPw + stepInvPw * spanLen;

						float fStart, fEnd;
						if (fogMode == Fog.LINEAR)
						{
							fStart = (fogFarNorm - zEyeStart * invFogDiv) * 256.0f;
							fEnd   = (fogFarNorm - zEyeEnd   * invFogDiv) * 256.0f;
						}
						else
						{
							fStart = M3GMath.exp(-fogDensity * zEyeStart) * 256.0f;
							fEnd   = M3GMath.exp(-fogDensity * zEyeEnd)   * 256.0f;
						}

						fogFactor = M3GMath.min(255.0f, M3GMath.max(0.0f, fStart));
						float targetFogEnd = M3GMath.min(255.0f, M3GMath.max(0.0f, fEnd));
						stepFogFactor = (targetFogEnd - fogFactor) * invSpanLen;
					}
				}

				// Only depth test if the compositingMode has the feature enabled. If
				// compositingMode is not set, check if this target has depthBuffer enabled.
				if(usesDepth && this.depthBuffer[depthIdx] < (short) z)
				{
					// We need to increment the color and texture deltas even when discarding
					// by depth, otherwise color and texturing spans on objects partially
					// occluded by others won't be correct.
					if (hasColors) { deltaA += stepA; deltaR += stepR; deltaG += stepG; deltaB += stepB; }
					if(hasTexture)
					{
						for(byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
						{
							curS[i] += stepS[i];
							curT[i] += stepT[i];
						}
					}
					continue;
				}

				// We have to do texture blending if we have vertex colors, as any available texture goes on top of them
				if (hasColors)
				{
					// Interpolate from xL to xR based on current pixel xy coordinate.
					// No need to calculate barycentric coords on every pixel.

					// We could call M3GMath.max/min here, but a simple & to
					// clamp these to [0,255] range is faster and should not
					// cause overflow
					paintPixel = ((deltaA >> 16 & 0xFF) << 24) |
					 ((deltaR >> 16 & 0xFF) << 16) |
					 ((deltaG >> 16 & 0xFF) << 8)  |
					  (deltaB >> 16 & 0xFF);

					deltaA += stepA;
					deltaR += stepR;
					deltaG += stepG;
					deltaB += stepB;
				}
				// Otherwise, we just use the default vertex color for this triangle
				else { paintPixel = opaqueAlpha | defVertColor; }

				if(hasTexture)
				{
					int bayerThreshold = BAYER_PATTERN[((y & 3) << 2) | (x & 3)] + 3;
					for(byte i = 0; i < ACTIVE_TEXTURE_UNITS; i++)
					{
						Image2D targetImage = textures[i].getImage();

						float s = curS[i] * invPw;
						float t = curT[i] * invPw;

						// Mipmapping support requested.
						if (levelFilters[i] != Texture2D.FILTER_BASE_LEVEL)
						{
							float dsdx, dtdx, dsdy, dtdy;
							int targetLevel = 0;

							dsdx = (sStepX[i] - s * dwdx) * invPw;
							dtdx = (tStepX[i] - t * dwdx) * invPw;
							dsdy = (sStepY[i] - s * dwdy) * invPw;
							dtdy = (tStepY[i] - t * dwdy) * invPw;

							if (levelFilters[i] == Texture2D.FILTER_NEAREST)
							{
								float lengthXSq = dsdx * dsdx + dtdx * dtdx;
								float lengthYSq = dsdy * dsdy + dtdy * dtdy;
								float maxSq = (lengthXSq > lengthYSq) ? lengthXSq : lengthYSq;

								int rawBits = Float.floatToRawIntBits(maxSq) - 0x3F800000;
								rawBits = rawBits & ~(rawBits >> 31);
								targetLevel = rawBits>> 24;
							}
							else // Trilinear
							{
								float area = dsdx * dtdy - dtdx * dsdy;
								int rawBits = (Float.floatToRawIntBits(area) & 0x7FFFFFFF) - 0x3F000000;

								// Apply LOD Dithering ONLY when FILTER_LINEAR (Trilinear) is requested
								// This saves us the need to do much slower trilinear filtering, while
								// retaining most of the looks.
								int lodFract = ((rawBits >> 19) & 0x1F) & ~(rawBits >> 31);

								rawBits = rawBits & ~(rawBits >> 31);
								targetLevel = rawBits >> 24;

								targetLevel -= (bayerThreshold - lodFract) >> 31;
							}

							targetImage = textures[i].getImageForLOD(targetLevel);

							// POT textures coming in with another fast path: Just shift
							// right by the targetLevel! TODO: NPOT textures SHOULD be able
							// to benefit from this as well although it's untested and
							// none of the NPOT test cases so far use mipmaps.
							s = (float) ((int) s >> targetLevel);
							t = (float) ((int) t >> targetLevel);
						}

						if (useBilinear[i])
						{
							paintPixel = blendTexture(paintPixel,
								sampleBilinear(targetImage, s, t, targetImage.getWidth(),
									targetImage.getHeight(), texRepeatS[i], texRepeatT[i],
									textures[i].isNPOT()), texblendMode[i], textures[i].getBlendColor());
						}
						else
						{
							int texCoord = wrapCoords(M3GMath.floor(s), M3GMath.floor(t), targetImage.getWidth(),
								targetImage.getHeight(), texRepeatS[i], texRepeatT[i], textures[i].isNPOT());

							paintPixel = blendTexture(paintPixel, targetImage.getPixel(texCoord & 0xFFFF, texCoord >>> 16),
								texblendMode[i], textures[i].getBlendColor());
						}

						curS[i] += stepS[i];
						curT[i] += stepT[i];
					}
				}

				/*
				 * Alpha test BEFORE any depth write: transparent fragments must not
				 * occlude geometry drawn later (games rely on this — e.g. tree canopies
				 * with alpha cutouts drawn before the ground). The depth buffer is only
				 * updated by fragments that survive this test.
				 */
				final int alpha = paintPixel >>> 24;

				if (alpha == 0 || alpha < alphaThreshold) { continue; }

				// Update the depth buffer if depth write is enabled (alpha pixels do not write Z)
				if (usesDepthWrite) { this.depthBuffer[depthIdx] = (short) z; }

				// Only write to the screen if color write is enabled.
				if(!colorEnabled) { continue; }

				// Blend the fog, all important calculations were done prior.
				if (hasFog && fogFactor < 255.0f)
				{
					paintPixel = blendFog(paintPixel, fog.getColor(), (int) fogFactor);
				}

				if (doDither)
				{
					int ditherOffset = BAYER_PATTERN[((y & 3) << 2) | (x & 3)];

					int r = ((paintPixel >> 16) & 0xFF) + ditherOffset;
					int g = ((paintPixel >>  8) & 0xFF) + ditherOffset;
					int b =  (paintPixel        & 0xFF) + ditherOffset;

					r |= ((255 - r) >> 31); r &= 0xFF;
					g |= ((255 - g) >> 31); g &= 0xFF;
					b |= ((255 - b) >> 31); b &= 0xFF;

					// Repack keeping Alpha intact
					paintPixel = (paintPixel & 0xFF000000) | (r << 16) | (g << 8) | b;
				}

				// Apply basic edge coverage Anti-Aliasing, if the flag is enabled.
				if (!renderToImage && doAntiAlias && (x == ixL || x == ixR - 1) &&
					compBlending == CompositingMode.REPLACE && alpha >= 255)
				{
					// The way this works is that we "extend" the geometry size a bit
					// for the antialiased output, that way triangles don't get smoothed
					// inwards, causing transparent edges between them to manifest.
					if (x == ixL)
					{
						int distFx = (int) (((ixL + 1.0f) - xL) * 65536.0f);
						int scaledDist = (distFx * 85) >> 16;

						applyEdgeAA(x - 1, rasterIdx - 1, canvasWidth, paintPixel, rasterData, 85 + scaledDist);
						applyEdgeAA(x, rasterIdx, canvasWidth, paintPixel, rasterData, 164 + scaledDist);
					}
					else
					{
						int distFx = (int) ((xR - (ixR - 1)) * 65536.0f);
						int scaledDist = (distFx * 85) >> 16;

						applyEdgeAA(x, rasterIdx, canvasWidth, paintPixel, rasterData, 164 + scaledDist);
						applyEdgeAA(x + 1, rasterIdx + 1, canvasWidth, paintPixel, rasterData, 85 + scaledDist);
					}

					continue;
				}

				if(!renderToImage)
				{
					rasterData[rasterIdx] = compBlending == CompositingMode.REPLACE ? paintPixel : blendCompositing(rasterData[rasterIdx],
						paintPixel, alpha, compBlending);
				}
				else
				{
					imageData.setPixel((x+viewx), (y+viewy), compBlending == CompositingMode.REPLACE ? paintPixel :
						blendCompositing(imageData.getPixel((x+viewx), (y+viewy)), paintPixel,
							alpha, compBlending));
				}
			}
		}
	}

	private static final void applyEdgeAA(int targetX, int targetIdx, int canvasWidth, int paintPixel,
							int[] rasterData, int coverageAlpha)
	{
		if (coverageAlpha > 0 && targetX >= 0 && targetX < canvasWidth)
		{
			if (coverageAlpha > 255) { coverageAlpha = 255; }

			int bgRB = rasterData[targetIdx] & 0x00FF00FF, fgRB = paintPixel & 0x00FF00FF;
			int outRB = (bgRB + ((((fgRB - bgRB) * coverageAlpha) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

			int bgAG = (rasterData[targetIdx] >>> 8) & 0x00FF00FF, fgAG = (paintPixel >>> 8) & 0x00FF00FF;
			int outAG = (bgAG + ((((fgAG - bgAG) * coverageAlpha) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

			int aaPixel = outRB | (outAG << 8);

			rasterData[targetIdx] = aaPixel;
		}
	}

	// This is basically the pixel blending to use when rendering to the screen
	private static final int blendCompositing(int bg, int fg, int alpha, int blendMode)
	{
		switch (blendMode)
		{
			case CompositingMode.REPLACE:
				return fg;

			case CompositingMode.ALPHA:
			{
				if (alpha <= 0)   { return bg; }
				if (alpha >= 255) { return fg; }

				int invA = 255 - alpha;

				int bgRB = bg & 0x00FF00FF;
				int fgRB = fg & 0x00FF00FF;
				int outRB = ((fgRB * alpha + bgRB * invA) >> 8) & 0x00FF00FF;

				int bgAG = (bg >>> 8) & 0x00FF00FF;
				int fgAG = (fg >>> 8) & 0x00FF00FF;
				int outAG = ((fgAG * alpha + bgAG * invA) >> 8) & 0x00FF00FF;

				return outRB | (outAG << 8);
			}

			case CompositingMode.ALPHA_ADD:
			{
				if (alpha == 0) { return bg; }

				int bgA = bg >>> 24,          bgR = (bg >> 16) & 0xFF;
				int bgG = (bg >> 8) & 0xFF,   bgB = bg & 0xFF;
				int fgR = (fg >> 16) & 0xFF,  fgG = (fg >> 8) & 0xFF,  fgB = fg & 0xFF;

				int addR = (fgR * alpha) >> 8;
				int addG = (fgG * alpha) >> 8;
				int addB = (fgB * alpha) >> 8;
				int addA = (alpha * (255 - bgA)) >> 8;

				int sumR = bgR + addR;
				int sumG = bgG + addG;
				int sumB = bgB + addB;
				int sumA = bgA + addA;

				int outR = sumR | -(sumR >> 8);
				int outG = sumG | -(sumG >> 8);
				int outB = sumB | -(sumB >> 8);
				int outA = sumA | -(sumA >> 8);

				return ((outA & 0xFF) << 24) | ((outR & 0xFF) << 16) | ((outG & 0xFF) << 8) | (outB & 0xFF);
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

			default:
				return bg;
		}
	}

	private static int blendFog(int color, int fogColor, int fogAmount)
	{
		/*
		 * M3G specifies that, the smaller the fogFactor value, the more we
		 * should blend the fog color into the received color... which means
		 * that the fog's contribution to the resulting color should be
		 * 1 - fogFactor;
		 */
		final int pixRB = color & 0x00FF00FF;
		final int pixG  = (color >> 8) & 0xFF;

		final int fogRB = fogColor & 0x00FF00FF;
		final int fogG  = (fogColor >> 8) & 0xFF;

		final int outRB = (fogRB + ((((pixRB - fogRB) * fogAmount) >> 8) & 0x00FF00FF)) & 0x00FF00FF;
		final int outG = fogG + (((pixG - fogG) * fogAmount) >> 8);

		return (color & 0xFF000000) | outRB | (outG << 8);
	}

	private static final int blendTexture(int bg, int fg, int funcMode, int texBlendColor)
	{
		switch (funcMode)
		{
			// RGB and LUMINANCE are opaque by default, so REPLACE and
			// DECAL may also return them outright.
			case ((Texture2D.FUNC_REPLACE & 7) << 3) | (Image2D.RGB & 7):
			case ((Texture2D.FUNC_DECAL & 7) << 3)   | (Image2D.RGB & 7):
			case ((Texture2D.FUNC_REPLACE & 7) << 3) | (Image2D.LUMINANCE & 7):
			case ((Texture2D.FUNC_REPLACE & 7) << 3) | (Image2D.RGBA & 7):
			case ((Texture2D.FUNC_REPLACE & 7) << 3) | (Image2D.LUMINANCE_ALPHA & 7):
				return fg;

			case ((Texture2D.FUNC_REPLACE & 7) << 3) | (Image2D.ALPHA & 7):
				return (fg & 0xFF000000) | (bg & 0x00FFFFFF);

			case ((Texture2D.FUNC_ADD & 7) << 3) | (Image2D.RGB & 7):
			case ((Texture2D.FUNC_ADD & 7) << 3) | (Image2D.LUMINANCE & 7):
			{
				int sumRB = (bg & 0x00FF00FF) + (fg & 0x00FF00FF);
				int overRB = (sumRB & 0x01000100) - ((sumRB & 0x01000100) >>> 8);
				int outRB = (sumRB | overRB) & 0x00FF00FF;

				int sumG = (bg & 0x0000FF00) + (fg & 0x0000FF00);
				int overG = (sumG & 0x00010000) - ((sumG & 0x00010000) >>> 8);
				int outG = (sumG | overG) & 0x0000FF00;

				return (bg & 0xFF000000) | outG | outRB;
			}

			case ((Texture2D.FUNC_ADD & 7) << 3) | (Image2D.RGBA & 7):
			case ((Texture2D.FUNC_ADD & 7) << 3) | (Image2D.LUMINANCE_ALPHA & 7):
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;

				int sumRB = (bg & 0x00FF00FF) + (fg & 0x00FF00FF);
				int overRB = (sumRB & 0x01000100) - ((sumRB & 0x01000100) >>> 8);
				int outRB = (sumRB | overRB) & 0x00FF00FF;

				int sumG = (bg & 0x0000FF00) + (fg & 0x0000FF00);
				int overG = (sumG & 0x00010000) - ((sumG & 0x00010000) >>> 8);
				int outG = (sumG | overG) & 0x0000FF00;

				return (outA << 24) | outG | outRB;
			}

			case ((Texture2D.FUNC_ADD & 7) << 3) | (Image2D.ALPHA & 7):
			{
				// Cv = Cf, Av = Af * At
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;
				return (outA << 24) | (bg & 0x00FFFFFF);
			}

			case ((Texture2D.FUNC_BLEND & 7) << 3) | (Image2D.RGBA & 7):
			case ((Texture2D.FUNC_BLEND & 7) << 3) | (Image2D.LUMINANCE_ALPHA & 7):
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;

				int fR = (bg >> 16) & 0xFF, cR = (texBlendColor >> 16) & 0xFF, tR = (fg >> 16) & 0xFF;
				int fG = (bg >>  8) & 0xFF, cG = (texBlendColor >>  8) & 0xFF, tG = (fg >>  8) & 0xFF;
				int fB =  bg        & 0xFF, cB =  texBlendColor        & 0xFF, tB =  fg        & 0xFF;

				int outR = (fR + (((cR - fR) * tR + 128) >> 8)) & 0xFF;
				int outG = (fG + (((cG - fG) * tG + 128) >> 8)) & 0xFF;
				int outB = (fB + (((cB - fB) * tB + 128) >> 8)) & 0xFF;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case ((Texture2D.FUNC_BLEND & 7) << 3) | (Image2D.RGB & 7):
			case ((Texture2D.FUNC_BLEND & 7) << 3) | (Image2D.LUMINANCE & 7):
			{
				int fR = (bg >> 16) & 0xFF, cR = (texBlendColor >> 16) & 0xFF, tR = (fg >> 16) & 0xFF;
				int fG = (bg >>  8) & 0xFF, cG = (texBlendColor >>  8) & 0xFF, tG = (fg >>  8) & 0xFF;
				int fB =  bg        & 0xFF, cB =  texBlendColor        & 0xFF, tB =  fg        & 0xFF;

				int outR = fR + (((cR - fR) * tR + 128) >> 8);
				int outG = fG + (((cG - fG) * tG + 128) >> 8);
				int outB = fB + (((cB - fB) * tB + 128) >> 8);

				return (bg & 0xFF000000) | (outR << 16) | (outG << 8) | outB;
			}

			case ((Texture2D.FUNC_BLEND & 7) << 3) | (Image2D.ALPHA & 7):
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;
				return (outA << 24) | (bg & 0x00FFFFFF);
			}

			case ((Texture2D.FUNC_DECAL & 7) << 3) | (Image2D.RGBA & 7):
			{
				int tA = fg >>> 24;

				int fRB = bg & 0x00FF00FF, tRB = fg & 0x00FF00FF;
				int outRB = (fRB + ((((tRB - fRB) * tA) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				int fAG = (bg >>> 8) & 0x00FF00FF, tAG = (fg >>> 8) & 0x00FF00FF;
				int outAG = (fAG + ((((tAG - fAG) * tA) >> 8) & 0x00FF00FF)) & 0x00FF00FF;

				return (bg & 0xFF000000) | ((outRB | (outAG << 8)) & 0x00FFFFFF);
			}

			case ((Texture2D.FUNC_MODULATE & 7) << 3) | (Image2D.RGBA & 7):
			case ((Texture2D.FUNC_MODULATE & 7) << 3) | (Image2D.LUMINANCE_ALPHA & 7):
			{
				int outR = (((bg >> 16) & 0xFF) * ((fg >> 16) & 0xFF) + 128) >> 8;
				int outG = (((bg >>  8) & 0xFF) * ((fg >>  8) & 0xFF) + 128) >> 8;
				int outB = (( bg        & 0xFF) * ( fg        & 0xFF) + 128) >> 8;
				int outA = ((bg >>> 24) * (fg >>> 24) + (bg >>> 24)) >> 8;

				return (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			case ((Texture2D.FUNC_MODULATE & 7) << 3) | (Image2D.RGB & 7):
			case ((Texture2D.FUNC_MODULATE & 7) << 3) | (Image2D.LUMINANCE & 7):
			{
				int outR = ((bg >> 16) & 0xFF) * ((fg >> 16) & 0xFF);
				int outG = ((bg >>  8) & 0xFF) * ((fg >>  8) & 0xFF);
				int outB = ( bg        & 0xFF) * ( fg        & 0xFF);

				return (bg & 0xFF000000)
					 | ((outR & 0xFF00) << 8)
					 |  (outG & 0xFF00)
					 |  (outB >> 8);
			}

			case ((Texture2D.FUNC_MODULATE & 7) << 3) | (Image2D.ALPHA & 7):
			{
				int outA = (((bg >>> 24) * (fg >>> 24)) + 128) >> 8;
				return (outA << 24) | (bg & 0x00FFFFFF);
			}

			default:
				return bg;
		}
	}

	private void clearToTarget(Background background, Image2D bgImg, boolean isImageTarget)
	{
		final int cropX = background.getCropX(), cropY = background.getCropY();
		int cropW = background.getCropWidth(), cropH = background.getCropHeight();
		if (cropW <= 0) { cropW = bgImg.getWidth(); }
		if (cropH <= 0) { cropH = bgImg.getHeight(); }

		final boolean repeatX = background.getImageModeX() == Background.REPEAT;
		final boolean repeatY = background.getImageModeY() == Background.REPEAT;
		final boolean doDither = (Mobile.m3gDitheringMode == MODE_FORCE_ENABLE)
			|| (Mobile.m3gDitheringMode == MODE_APP_CONTROLLED && (this.hints & DITHER) != 0);

		final int bgW = bgImg.getWidth();
		final int bgH = bgImg.getHeight();
		final boolean isNPOT = !(bgImg.isPowerOfTwo(bgW) && bgImg.isPowerOfTwo(bgH));

		int stepX = (cropW << 16) / vieww;
		int stepY = (cropH << 16) / viewh;
		int currY = cropY << 16;

		Image2D destImg = isImageTarget ? (Image2D) this.target : null;

		for (int py = 0; py < viewh; py++)
		{
			int currX = cropX << 16;
			int screenY = py + viewy;
			int rowOffset = screenY * canvasWidth + viewx;

			for (int px = 0; px < vieww; px++)
			{
				final int texCoord = wrapCoords(currX >> 16, currY >> 16, bgW, bgH,
					repeatX, repeatY, isNPOT);
				int paintPixel = bgImg.getPixel(texCoord & 0xFFFF, texCoord >>> 16);

				if (doDither)
				{
					int ditherOffset = BAYER_PATTERN[((py & 3) << 2) | (px & 3)];
					int r = ((paintPixel >> 16) & 0xFF) + ditherOffset;
					int g = ((paintPixel >>  8) & 0xFF) + ditherOffset;
					int b =  (paintPixel        & 0xFF) + ditherOffset;

					r |= ((255 - r) >> 31); r &= 0xFF;
					g |= ((255 - g) >> 31); g &= 0xFF;
					b |= ((255 - b) >> 31); b &= 0xFF;

					paintPixel = (paintPixel & 0xFF000000) | (r << 16) | (g << 8) | b;
				}

				int screenX = px + viewx;

				// Both are actually rather similar due to how FreeJ2ME+ implements these
				if (isImageTarget) { destImg.setPixel(screenX, screenY, paintPixel); }
				else { rasterData[rowOffset + px] = paintPixel; }
				currX += stepX;
			}
			currY += stepY;
		}
	}

	// For bilinear filtering support
	private static final int sampleBilinear(Image2D teximg, float s, float t, int texW, int texH, boolean texRepeatS, boolean texRepeatT, boolean isNPOT)
	{
		// Shift s and t by 0.5 on the texel center for OpenGL-like filtering,
		int sFixed = (int) ((s - 0.5f) * 256.0f);
		int tFixed = (int) ((t - 0.5f) * 256.0f);

		// Fractional components
		int fx = sFixed & 0xFF;
		int fy = tFixed & 0xFF;

		int xy0 = wrapCoords(sFixed >> 8, tFixed >> 8, texW, texH,
			texRepeatS, texRepeatT, isNPOT);

		int x1 = ((xy0 & 0xFFFF) + 1 < texW) ? (xy0 & 0xFFFF) + 1 : (texRepeatS ? 0 : (xy0 & 0xFFFF));
		int y1 = ((xy0 >>> 16) + 1 < texH) ? (xy0 >>> 16) + 1 : (texRepeatT ? 0 : (xy0 >>> 16));

		int c00 = teximg.getPixel(xy0 & 0xFFFF, xy0 >>> 16);
		int c10 = teximg.getPixel(x1, xy0 >>> 16);
		int c01 = teximg.getPixel(xy0 & 0xFFFF, y1);
		int c11 = teximg.getPixel(x1, y1);

		int rb0 = (c00 & 0x00FF00FF) + ((((c10 & 0x00FF00FF) - (c00 & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;
		int ag0 = ((c00 >>> 8) & 0x00FF00FF) + (((((c10 >>> 8) & 0x00FF00FF) - ((c00 >>> 8) & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;

		int rb1 = (c01 & 0x00FF00FF) + ((((c11 & 0x00FF00FF) - (c01 & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;
		int ag1 = ((c01 >>> 8) & 0x00FF00FF) + (((((c11 >>> 8) & 0x00FF00FF) - ((c01 >>> 8) & 0x00FF00FF)) * fx) >> 8) & 0x00FF00FF;

		int rb = rb0 + ((((rb1 - rb0) * fy) >> 8) & 0x00FF00FF);
		int ag = ag0 + ((((ag1 - ag0) * fy) >> 8) & 0x00FF00FF);

		return (ag << 8) | rb;
	}

	// Helper for texture wrapping/clamping
	// JSR-184 texture wrapping: REPEAT tiles the image, CLAMP samples the edge.
	// Out-of-range coordinates must never index outside the image.
	//
	// This method assumes that no texture larger than 32Kx32K will be used,
	// and this, it processes both X and Y coordinates in one go and returns
	// them packed in an integer as follows: XY = (texY << 16) | (texX & 0xFFFF).
	private static final int wrapCoords(int s, int t, int boundW, int boundH,
		boolean repeatS, boolean repeatT, boolean isNPOT)
	{
		int texX, texY;

		if (repeatS)
		{
			// If the texture is Power-Of-Two, repeat wrapping can be done
			// quickly as just an AND of the coordinate with the the edge
			// mask (which is width - 1). Why is that? A POT texture has
			// the following property: (2 - 1 = 1 = `0b1`, 4 - 1 = 3 = `0b11`,
			// 8 - 1 = 7 = `0b111`, and so on), so we always wrap around to the
			// correct coordinate with an AND of size - 1, as overflowing data
			// will naturally wrap back to the start.
			if (!isNPOT) { texX = s & (boundW - 1); }

			// Go to the slower NPOT path... try to make it a bit faster by
			// returning outright if the texture is within bounds.
			else if (s >= 0 && s < boundW) { texX = s; }

			// Not within bounds? Escape the usage of modulo by using Lemire's
			// fast reduction. We are hardly ever going to get coordinates over
			// the short range (-32768,32767), so we also do not cast to long,
			// remaining entirely within 32-bit range.
			else
			{
				int mask = s >> 31;
				int absT = (s ^ mask) - mask;

				// 32-bit int multiplication to replace need for 64-bit math
				texX = (absT * boundW) >> 16;

				// Wrap any negative coordinates back into [0, bound - 1] range
				texX = (mask != 0 && texX != 0) ? (boundW - texX) : texX;
			}
		}
		else // CLAMP mode
		{
			texX = s < 0 ? 0 : (s >= boundW ? boundW - 1 : s);
		}

		// This one just repeats the above, but for T/Y
		if (repeatT)
		{
			if (!isNPOT) { texY = t & (boundH - 1); }
			else if (t >= 0 && t < boundH) { texY = t; }
			else
			{
				int mask = t >> 31;
				int absT = (t ^ mask) - mask;
				texY = (absT * boundH) >> 16;
				texY = (mask != 0 && texY != 0) ? (boundH - texY) : texY;
			}
		}
		else
		{
			texY = t < 0 ? 0 : (t >= boundH ? boundH - 1 : t);
		}

		return (texY << 16) | (texX & 0xFFFF);
	}
}
