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
import java.awt.image.DataBufferInt;

import org.recompile.mobile.Mobile;

public class Graphics3D
{

	public static final int ANTIALIAS = 2;
	public static final int DITHER = 4;
	public static final int OVERWRITE = 16; // This might be unused here, as SW rasterization gives us direct control over pixels
	public static final int TRUE_COLOR = 8;


	public static final boolean SUPPORT_ANTIALIASING = false;
	public static final boolean SUPPORT_TRUE_COLOR = false;
	public static final boolean SUPPORT_DITHERING = false;
	public static final boolean SUPPORT_MIPMAPPING = false;
	public static final boolean SUPPORT_PERSPECTIVE_CORRECTION = true;
	public static final boolean SUPPORT_LOCAL_CAMERA_LIGHTING = false;
	public static final int MAX_LIGHTS = 8;
	public static final int MAX_VIEWPORT_WIDTH = 1024;
	public static final int MAX_VIEWPORT_HEIGHT = 1024;
	public static final int MAX_VIEWPORT_DIMENSION = 1024;
	public static final int MAX_TEXTURE_DIMENSION = 512;
	public static final int MAX_SPRITE_CROP_DIMENSION = 256;
	public static final int MAX_TRANSFORMS_PER_VERTEX = 4;
	public static final int NUM_TEXTURE_UNITS = 1;
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
	private float[] depthBuffer;
	private float near;
	private float far;

	private int hints;

	private Camera currCam;
	private Transform currCamTrans;
	private Transform currCamTransInv;
	private ArrayList<Light> currLights;
	private ArrayList<Transform> currLightTrans;

	// Reusable rendering variables
	int yStart, yEnd, ixL, ixR, r, g, b;
	int canvasWidth, canvasHeight;
	int[] rasterData;
	final int[] ord = new int[3];
	
	float xTop, yTop, zTop, sTop, tTop;
	float xMidL, yMid, zMidL, sMidL, tMidL;
	float xBot, yBot, zBot, sBot, tBot;
	float rHorizon, xMidR, zMidR, sMidR, tMidR;
	float drawY, drawX, xL, xR, zL, zR, sL, sR, tL, tR;
	float z, s, t;
	final float[] scaleBias = new float[4];

	final Transform projectionMatrix = new Transform();
	final int[] renderableTriangles = {0}; // Counter for visible triangles
	
	// Vertex color blending variables
	final int[] colors = new int[3];
	final byte[] color_vertex = new byte[4];
	float totalArea, areaA, areaB, areaC, weightA, weightB, weightC, totalWeight;

	// fog blending factor
	float fogFactor = 0.0f;

	// Textured polygon variables
	final float[] coX = new float[3];
	final float[] coY = new float[3];
	final float[] coZ = new float[3];
	final float[] coS = new float[3];
	final float[] coT = new float[3];	


	public Graphics3D()
	{
		/* 
		 * The default depth range used is that of window coordinates, so 0 to near, and 1 to far
		 * JSR-184 specifies that Normalized Device Coordinates (NDC) can also be used, which ranges from -1 to 1.
		 */
		this.near = 0f;
		this.far = 1f;
		this.currCam = null;
		this.currCamTrans = null;
		this.currCamTransInv = null;
		this.currLights = new ArrayList<Light>();
		this.currLightTrans = new ArrayList<Transform>();
	}


	public int addLight(Light light, Transform transform)
	{
		/* As per JSR-184, addLight() must throw a NullPointerException if no light is given */
		if (light == null) { throw new NullPointerException("addLight() was called but no light object was provided."); }

		if (transform == null) { transform = new Transform(); }

		this.currLights.add(light);
		this.currLightTrans.add(transform);
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
			this.viewx = pgrp.getClipX();
			this.viewy = pgrp.getClipY();
			this.vieww = pgrp.getClipWidth();
			this.viewh = pgrp.getClipHeight();
			rasterData = ((DataBufferInt) pgrp.getCanvas().getRaster().getDataBuffer()).getData();
			canvasWidth = pgrp.getCanvas().getWidth();
			canvasHeight = pgrp.getCanvas().getHeight();
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
		this.depthBuffer = new float[this.vieww * this.viewh];
		Arrays.fill(this.depthBuffer, this.far);
		this.depthEnabled = depthBuffer;
		this.hints = hints;
	}

	public void clear(Background background)
	{
		/* As per JSR-184, throw IllegalStateException if this Graphics3D object does not have a render target. */
		if (this.target == null) { throw new IllegalStateException("Cannot clear Background on a Graphics3D without a render target."); }

		int color = 0;
		int x = viewx;
		int y = viewy;
		int w = vieww;
		int h = viewh;
		boolean clearColor = true;
		boolean clearDepth = true;

		if (background != null)
		{
			color = background.getColor();
			x = background.getCropX();
			y = background.getCropY();
			w = background.getCropWidth();
			h = background.getCropHeight();
			clearColor = background.isColorClearEnabled();
			clearDepth = background.isDepthClearEnabled();
		}
		else { color = 0x00000000; }

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

				// Fill the background with the background color
				grp.setColor(color);
				grp.fillRect(x, y, w, h);

				// Draw the background's image if any (and there's a background)
				if(background != null && background.getImage() != null) 
				{
					Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Clear with Background Image Untested");
					for(; y < h; y++) 
					{
						for(; x < w; x++) 
						{
							rasterData[y * canvasWidth + x] = background.getImage().getConvertedPixel(x, y);
						}
					}
				}
			}
		}

		if (clearDepth) { Arrays.fill(this.depthBuffer, this.far); }
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
			/* 
			 * TODO: Flush the rendered 3D image to this target before releasing it 
			 * in order to ensure that the 3D image becomes visible.
			 */
			
			/* If there is a render target, release it */ 
			this.target = null;
		}
	}

	public void render(World world)
	{
		/* Clear the background first */
		clear(world.getBackground());

		/* As per JSR-184, throw NullPointerException if the received world is null. */
		if (world == null) { throw new NullPointerException("render(world) was called but no world was provided."); }
		
		/* Also per JSR-184, throw IllegalStateException this object has no render target yet. */
		if (this.target == null) { throw new IllegalStateException("render(world) was called but there is no render target."); }

		Transform tr = new Transform();

		Camera worldCamera = world.getActiveCamera();

		if(worldCamera == null) { throw new IllegalStateException("Cannot render a world that has no active camera."); }

		if(!worldCamera.getTransformTo(world, tr)) { throw new IllegalStateException("Active camera is not in world."); }
		
		/* 
		 * if the bg-img of `world` is not the same format as `this.target`:
		 * throw new IllegalStateException();
		 */

		setCamera(worldCamera, tr);
		resetLights();
		positionLights(world, world);

		render((Group) world, new Transform());
	}

	public void render(Node node, Transform transform)
	{
		/* As per JSR-184, throw NullPointerException if no node is received. */
		if(node == null) { throw new NullPointerException("render() was called but no node was provided."); }
	
		/* Also per JSR-184, throw IllegalStateException if this method is called but there's no camera or render target available. */ 
		if (this.target == null || this.currCam == null) { throw new IllegalStateException("render() was called but there is no camera or render target."); }

		/* Also per JSR-184, throw IllegalStateException if if node is not a Sprite3D, Mesh, or Group Object. */
		if (!(node instanceof Mesh || node instanceof Sprite3D || node instanceof Group)) { throw new IllegalArgumentException("Node is not an instance of any of the following: Sprite3D, Mesh, Group"); }

		if(transform == null) { transform = new Transform(); } // If transform is null, it indicates an identity matrix is to be used
		// if any Mesh that is rendered violates the constraints defined in
		//    Mesh, MorphingMesh, SkinnedMesh, VertexBuffer, or IndexBuffer
		//    throw new java.lang.IllegalStateException();

		if (node instanceof Mesh) 
		{
			if(!node.isRenderingEnabled()) { return; }
			Mesh mesh = (Mesh) node;
			int subMeshes = mesh.getSubmeshCount();
			VertexBuffer vertices = mesh.getVertexBuffer();
			for (int i = 0; i < subMeshes; i++) 
			{
				if (mesh.getAppearance(i) != null) { render(vertices, mesh.getIndexBuffer(i), mesh.getAppearance(i), transform, node.getScope()); }
			}
		}
		else if (node instanceof Sprite3D) 
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Graphics3D.render Node: Sprite3D Not Implemented!");
			if(!node.isRenderingEnabled()) { return; }
		}
		else if (node instanceof Group) 
		{
			Node child = ((Group) node).firstChild;
			if (child != null) 
			{
				do 
				{
					if (child != (Object3D) node) 
					{
						if(child instanceof Sprite3D || child instanceof Mesh || child instanceof Group) 
						{
							Transform t = new Transform();
							child.getCompositeTransform(t);
							t.preMultiply(transform);
							render(child, t); 
						}
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
		/* TODO: Check the scope used by the submesh to find out which lights need to be applied, if it needs to be rendered, etc. */

		/* As per JSR-184, if vertices, triangles or appearence are null, throw a NullPointerException. */
		if (vertices == null || triangles == null || appearance == null) { throw new NullPointerException("Tried to render a submesh with incomplete info."); }
		
		/* Also per JSR-184, throw IllegalStateException if the application tries to render without having set up a render target or camera beforehand. */
		if (this.target == null || this.currCam == null) { throw new IllegalStateException("Tried to render a submesh without having a render target or camera first."); }
		
		// if `vertices` or `triangles` violates the constraints
		//    defined in VertexBuffer or IndexBuffer
		//    throw new java.lang.IllegalStateException();

		/* Receiving a null transform indicates that the identity matrix must be used. */
		if (transform == null) { transform = new Transform(); }

		final CompositingMode compositingMode = appearance.getCompositingMode() != null ? appearance.getCompositingMode() : new CompositingMode();
		
		// TODO: Shading mode is not implemented
		final int shadingMode = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getShading() : PolygonMode.SHADE_SMOOTH;
		
		final int cullingMode = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getCulling() : PolygonMode.CULL_BACK;
		final int windingOrder = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getWinding() : PolygonMode.WINDING_CCW;
		final boolean perspectiveCorrectionEnabled = appearance.getPolygonMode() != null ? appearance.getPolygonMode().isPerspectiveCorrectionEnabled() : false;

		// Handle winding order first and foremost
		if (windingOrder == PolygonMode.WINDING_CW) 
		{
			Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Polygon Winding is Clockwise! Untested, might render incorrectly");
			ord[0] = 0;
			ord[1] = 2;
			ord[2] = 1;
		}
		else 
		{
			ord[0] = 0;
			ord[1] = 1;
			ord[2] = 2;
		}

		// Set up fog properties
		final Fog fog = appearance.getFog();

		final Transform tr = new Transform();
		final Transform textr = new Transform();
		final Transform texcomptr = new Transform();

		final VertexArray vertPos = vertices.getPositions(scaleBias);
		final Texture2D tex = appearance.getTexture(0);
		final Image2D teximg = tex == null ? null : tex.getImage();

		// Scale and translate mesh
		tr.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
		tr.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);

		final VertexArray texCoords = vertices.getTexCoords(0, scaleBias); // get Texture coordinates

		if (tex != null) { tex.getCompositeTransform(texcomptr); }

		// Scale and translate texture coordinates (same scaleBias)
		textr.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
		textr.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);
		
		textr.preMultiply(texcomptr);

		// -> Local space
		this.currCam.getProjection(projectionMatrix);

		// Transform mesh from local coords to world coords
		tr.preMultiply(transform);
		// -> World space

		// Apply the inverse of the camera's transform to the mesh
		tr.preMultiply(this.currCamTransInv);
		// -> View space

		// Apply projection matrix
		tr.preMultiply(projectionMatrix);
		// -> Clip space

		// Do the transformation
		final float[] vertClip = new float[4 * vertPos.getVertexCount()];
		tr.transform(vertPos, vertClip, true);

		final float[] texVert = new float[4 * vertPos.getVertexCount()];
		if (texCoords != null) { textr.transform(texCoords, texVert, true); }

		// Create Triangle objects (fromVertsAndTris already does culling and clipping)
		final Triangle[] trisScreen = Triangle.fromVertAndTris(vertClip, texVert, triangles.getIndexArray(), renderableTriangles, near, cullingMode);

		// At this point the triangles in `trisScreen` are actually
		// projected to Normalized Device Coordinates, but they will be tranformed
		// to Screen space in-place, hence the name.

		// Reset transform
		tr.setIdentity();
		textr.setIdentity();

		// Fit to viewport
		if (teximg != null) { textr.postScale(teximg.getWidth(), teximg.getHeight(), 1); }
		tr.postScale((float) vieww / 2f, (float) viewh / 2f, 1f);
		tr.postTranslate(1, 1, 0);
		tr.postScale(1, -1, 1);

		// -> Screen space

		// Perform viewport transform only on renderable triangles (saves an Arrays.copyOf call)
		Triangle.transform(trisScreen, renderableTriangles[0], tr, textr);

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
				coS[0] = trisScreen[tri_id].sA(); coS[1] = trisScreen[tri_id].sB(); coS[2] = trisScreen[tri_id].sC();
				coT[0] = trisScreen[tri_id].tA(); coT[1] = trisScreen[tri_id].tB(); coT[2] = trisScreen[tri_id].tC();

				// x and y coordinates are special cases where the resulting top, mid and bot values should be in decreasing order (top > mid > bot)
				if (coY[ord[1]] < coY[ord[0]]) 
				{
					int temp = ord[0];
					ord[0] = ord[1];
					ord[1] = temp;
				}
				if (coY[ord[2]] < coY[ord[0]]) 
				{
					int temp = ord[0];
					ord[0] = ord[2];
					ord[2] = temp;
				}
				if (coY[ord[2]] < coY[ord[1]]) 
				{
					int temp = ord[1];
					ord[1] = ord[2];
					ord[2] = temp;
				}

				// Assign ordered vertex attributes based on their determined order
				xTop = coX[ord[0]]; xMidL = coX[ord[1]]; xBot = coX[ord[2]];
				yTop = coY[ord[0]]; yMid = coY[ord[1]]; yBot = coY[ord[2]];
				zTop = coZ[ord[0]]; zMidL = coZ[ord[1]]; zBot = coZ[ord[2]];
				sTop = coS[ord[0]]; sMidL = coS[ord[1]]; sBot = coS[ord[2]];
				tTop = coT[ord[0]]; tMidL = coT[ord[1]]; tBot = coT[ord[2]];

				// Calculate the right horizon
				rHorizon = (yMid - yTop) / (yBot - yTop);
				xMidR = xTop + rHorizon * (xBot - xTop);
				zMidR = zTop + rHorizon * (zBot - zTop);
				sMidR = sTop + rHorizon * (sBot - sTop);
				tMidR = tTop + rHorizon * (tBot - tTop);

				// Swap midpoints if necessary
				if (xMidL > xMidR) 
				{
					float temp;

					// Swap values between left and right midpoints
					temp = xMidL; xMidL = xMidR; xMidR = temp;
					temp = zMidL; zMidL = zMidR; zMidR = temp;
					temp = sMidL; sMidL = sMidR; sMidR = temp;
					temp = tMidL; tMidL = tMidR; tMidR = temp;
				}

				// Draw both halves of the triangle
				for (int half = 0; half < 2; half++) 
				{
					// Determine the range for the y-coordinate
					yStart = half == 0 ? M3GMath.max(M3GMath.roundPositive(yTop), 0) : M3GMath.max(M3GMath.roundPositive(yMid), 0);
					yEnd = half == 0 ? M3GMath.min(M3GMath.roundPositive(yMid), viewh) : M3GMath.min(M3GMath.roundPositive(yBot), viewh);
					
					// Adjust drawY calculation based on half
					for (int y = yStart; y < yEnd; y += Mobile.halfResM3GRaster ? 2 : 1) 
					{
						drawY = half == 0
							? (y - yTop) / (yMid - yTop)  // Upper half
							: 1f - (y - yMid) / (yBot - yMid); // Lower half
						drawY = M3GMath.max(0f, M3GMath.min(drawY, 1f));

						// Calculate interpolated values
						
						xL = half == 0
							? xTop + drawY * (xMidL - xTop)
							: xBot + drawY * (xMidL - xBot);
						xR = half == 0
							? xTop + drawY * (xMidR - xTop)
							: xBot + drawY * (xMidR - xBot);
						zL = half == 0
							? zTop + drawY * (zMidL - zTop)
							: zBot + drawY * (zMidL - zBot);
						zR = half == 0
							? zTop + drawY * (zMidR - zTop)
							: zBot + drawY * (zMidR - zBot);
							
						sL = half == 0
							? sTop + drawY * (sMidL - sTop)
							: sBot + drawY * (sMidL - sBot);
						sR = half == 0
							? sTop + drawY * (sMidR - sTop)
							: sBot + drawY * (sMidR - sBot);
						tL = half == 0
							? tTop + drawY * (tMidL - tTop)
							: tBot + drawY * (tMidL - tBot);
						tR = half == 0
							? tTop + drawY * (tMidR - tTop)
							: tBot + drawY * (tMidR - tBot);

						// TODO: Proper texture perspective correction
						if (perspectiveCorrectionEnabled) 
						{			
							
						}

						ixL = M3GMath.max(M3GMath.roundPositive(xL), 0);
						ixR = M3GMath.min(M3GMath.roundPositive(xR), vieww);

						// Draw the pixels for the current y-coordinate
						for (int x = ixL; x < ixR; x += Mobile.halfResM3GRaster ? 2 : 1) 
						{
							// This check is really only used for wireframe debugging, and it's not a perfect wireframe rendering
							if(Mobile.M3GRenderWireframe && x > ixL && x < ixR) { continue; }

							try 
							{
								drawX = (x - xL) / (xR - xL);
								drawX = M3GMath.max(0f, M3GMath.min(drawX, 1f));
								z = (zL + drawX * (zR - zL));
								
								// Only depth test if the compositingMode has the feature enabled. If compositingMode is not set, check if this target has depthBuffer enabled
								if(compositingMode.isDepthTestEnabled() && isDepthBufferEnabled())
								{
									// Depth testing and depth buffer updates don't need to match against the pixel's translated viewport coordinates, if they are translated
									if (this.depthBuffer[this.vieww * y + x] < z) { continue; } // Skip if this pixel is not visible

									// Update the depth buffer if depth write is enabled
									if (compositingMode.isDepthWriteEnabled()) { this.depthBuffer[this.vieww * y + x] = z; }
								}
								s = sL + drawX * (sR - sL);
								t = tL + drawX * (tR - tL);

								// If there's no texture coords or a texture image, we default to rendering with vertex colors. (also used for debug render modes)
								int paintPixel = 0xFF000000 | vertices.getDefaultColor(); // It's forced to opaque, maybe that shouldn't be done for untextured polygons, but helps some games like Brick Breaker Revolution
								if(tex != null && texCoords != null && !Mobile.M3GRenderUntexturedPolygons && !Mobile.M3GRenderWireframe) { paintPixel = teximg.getConvertedPixel(M3GMath.roundPositive(s), M3GMath.roundPositive(t)); }
								
								if (((paintPixel >> 24) & 0xFF) < (int) (compositingMode.getAlphaThreshold() * 255)) { continue; } // Skip transparent pixels below the alpha threshold

								int alpha; // Image2D converts to ARGB format

								if (vertices.getColors() != null) // We have to do texture blending, as we have vertex colors and any available texture goes on top of them
								{
									// Get vertex index color TODO: This doesn't yet result in proper blending
									for (int i = 0; i < 3; i++) 
									{
										vertices.getColors().get(trisScreen[tri_id].getIndex(ord[i]), 1, color_vertex);
										colors[i] = (vertices.getColors().getComponentCount() == 3)
											? (0xFF << 24) | (Byte.toUnsignedInt(color_vertex[0]) << 16) |
											(Byte.toUnsignedInt(color_vertex[1]) << 8) | Byte.toUnsignedInt(color_vertex[2])
											: (Byte.toUnsignedInt(color_vertex[3]) << 24) |
											(Byte.toUnsignedInt(color_vertex[0]) << 16) |
											(Byte.toUnsignedInt(color_vertex[1]) << 8) | Byte.toUnsignedInt(color_vertex[2]);
									}

									// Calculate weights based on pixel position in relation to the triangle's area
									areaA = M3GMath.abs((xBot - xTop) * (y - yTop) - (xMidL - xTop) * (yBot - yTop)) * 0.5f;
									areaB = M3GMath.abs((xMidL - xTop) * (y - yTop) - (x - xTop) * (yMid - yTop)) * 0.5f;
									areaC = M3GMath.abs((x - xBot) * (yMid - yTop) - (xBot - xMidL) * (y - yTop)) * 0.5f;
									totalArea = areaA + areaB + areaC;

									weightA = areaA / totalArea;
									weightB = areaB / totalArea;
									weightC = areaC / totalArea;

									// Normalize weights
									totalWeight = weightA + weightB + weightC;
									weightA /= totalWeight;
									weightB /= totalWeight;
									weightC /= totalWeight;

									// Interpolate color based on weights
									alpha = (int) ((weightA * ((colors[0] >> 24) & 0xFF)) + (weightB * ((colors[1] >> 24) & 0xFF)) + (weightC * ((colors[2] >> 24) & 0xFF)));
									r = (int) ((weightA * ((colors[0] >> 16) & 0xFF)) + (weightB * ((colors[1] >> 16) & 0xFF)) + (weightC * ((colors[2] >> 16) & 0xFF)));
									g = (int) ((weightA * ((colors[0] >> 8) & 0xFF)) + (weightB * ((colors[1] >> 8) & 0xFF)) + (weightC * ((colors[2] >> 8) & 0xFF)));
									b = (int) ((weightA * (colors[0] & 0xFF)) + (weightB * (colors[1] & 0xFF)) + (weightC * (colors[2] & 0xFF)));

									// Blend with texture pixel if there's one, otherwise, just use the interpolated vertex color directly
									if(tex == null && texCoords == null) { paintPixel = (alpha << 24) | (r << 16) | (g << 8) | b; }
									else { paintPixel = blendPixels(paintPixel, (alpha << 24) | (r << 16) | (g << 8) | b, alpha, tex.getBlending()); }
								}

								alpha = (paintPixel >> 24) & 0xFF; // Image2D converts to ARGB format

								// To blend the fog value here, we have to take the current pixel's z value into consideration
								if(fog != null) 
								{
									// TODO: This multiplication by 250 is not correct, it's just a workaround that helps games with actual fog usage to show geometry
									// There's probably some kind of issue with how triangles' final z-coordinate is calculated
									if (fog.getMode() == Fog.LINEAR) 
									{
										fogFactor = M3GMath.max(0, M3GMath.min(1, (fog.getFarDistance() - z) / (fog.getFarDistance() - fog.getNearDistance()) * 250));
									} 
									else 
									{
										fogFactor = M3GMath.abs(M3GMath.exp(-fog.getDensity() * z));
										fogFactor = M3GMath.max(0, M3GMath.min(1, fogFactor));
									}

									paintPixel = blendFog(paintPixel, fog.getColor());
								}

								// Handle compositing mode with background pixel [rasterData] AFTER the fog calculation, otherwise alpha values won't be correct.
								// If blending is REPLACE and the pixel to paint is fully opaque, we can skip blending altogether and just paint it directly.
								final int finalPixel = (compositingMode.getBlending() == CompositingMode.REPLACE && alpha == 255) ? paintPixel :
									blendPixels(rasterData[(y+viewy) * canvasWidth + (x+viewx)], paintPixel, alpha, compositingMode.getBlending());

								if(!Mobile.halfResM3GRaster) // If we're rendering at native res, just blend each pixel and update the depth buffer normally
								{
									rasterData[(y+viewy) * canvasWidth + (x+viewx)] = finalPixel;
								} 
								else // Else, we have to copy the same pixel over in a 2x2 basis, and update the depth buffer in the same manner
								{
									for(int fx = x; fx < x + 2; fx++) 
									{
										for(int fy = y; fy < y + 2; fy++) 
										{
											rasterData[(fy+viewy) * canvasWidth + (fx+viewx)] = finalPixel;
										}
									}
								}
							} 
							catch (Exception e) { Mobile.log(Mobile.LOG_WARNING, Graphics3D.class.getPackage().getName() + "." + Graphics3D.class.getSimpleName() + ": " + "Error drawing triangle:" + e.getMessage()); e.printStackTrace(); }
						}
					}
				}
			}
		}
	}

	private void positionLights(World world, Object3D obj) 
	{
		int numReferences = obj.getReferences(null);
		if (numReferences > 0) 
		{
			Object3D[] objArray = new Object3D[numReferences];
			obj.getReferences(objArray);
			for (int i = 0; i < numReferences; ++i) 
			{
				if (objArray[i] instanceof Light) 
				{
					Transform t = new Transform();
					Light light = (Light) objArray[i];
					if (light.isRenderingEnabled() && light.getTransformTo(world, t)) { addLight(light, t); }
				}
				positionLights(world, objArray[i]);
			}
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
			this.currCamTrans = new Transform();
			this.currCamTransInv = new Transform();
		}
		else /* Else, set the transform and its inverse accordingly. */
		{
			this.currCamTrans = new Transform(transform);
			this.currCamTransInv = new Transform(transform);
		}
		this.currCamTransInv.invert(); /* This one will execute regardless of the given transform above. */
	}

	public void setDepthRange(float near, float far)
	{
		/* As per JSR-184, throw IllegalArgumentException if the received near and/or far planes have unsupported values. */
		if (near < 0 || far < 0 || 1 < near || 1 < far) { throw new IllegalArgumentException("The requested Depth Range values are invalid."); }
		else 
		{ 
			this.near=near; this.far=far;
			Arrays.fill(this.depthBuffer, this.far);
		}	
	}

	public void setLight(int index, Light light, Transform transform)
	{
		/* As per JSR-184, throw IndexOutOfBoundsException if index < 0 or index > CurrentAmountOfLights. */
		if (index < 0 || index > this.currLights.size()) { throw new IndexOutOfBoundsException("Tried to modify a Light on an out-of-bounds index."); }

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

	// This one is used for texture/background blending, and also pixel blending when rendering to the screen
	private int blendPixels(int background, int foreground, int alpha, int blendMode) 
	{
		final int bgA = (background >> 24) & 0xFF;
		final int bgR = (background >> 16) & 0xFF;
		final int bgG = (background >> 8) & 0xFF;
		final int bgB = background & 0xFF;

		final int fgA = (foreground >> 24) & 0xFF;
		final int fgR = (foreground >> 16) & 0xFF;
		final int fgG = (foreground >> 8) & 0xFF;
		final int fgB = foreground & 0xFF;

		final int outR, outG, outB, outA;

		final float alphaNorm = alpha / 255f;

		switch (blendMode)
		{
			case Texture2D.FUNC_REPLACE:
			case CompositingMode.REPLACE:
				outA = (int) (fgA + (bgA * (1 - (fgA / 255f))));
				outR = (int) (fgR * (fgA / 255f) + bgR * (1 - (fgA / 255f)));
				outG = (int) (fgG * (fgA / 255f) + bgG * (1 - (fgA / 255f)));
				outB = (int) (fgB * (fgA / 255f) + bgB * (1 - (fgA / 255f)));
				return (M3GMath.max(0, M3GMath.min(outA, 255)) << 24) | 
					(M3GMath.max(0, M3GMath.min(outR, 255)) << 16) | 
					(M3GMath.max(0, M3GMath.min(outG, 255)) << 8) | 
					M3GMath.max(0, M3GMath.min(outB, 255));

			case CompositingMode.ALPHA_ADD:
				outR = (int) M3GMath.min(255, (fgR * alphaNorm) + bgR);
				outG = (int) M3GMath.min(255, (fgG * alphaNorm) + bgG);
				outB = (int) M3GMath.min(255, (fgB * alphaNorm) + bgB);
				outA = (int) M3GMath.min(255, bgA + (int)(alpha * (1 - (bgA / 255f))));
				return (outA << 24) | (outR << 16) | (outG << 8) | outB;

			case Texture2D.FUNC_BLEND:
			case CompositingMode.ALPHA:
				outR = (int) ((fgR * alphaNorm) + (bgR * (1 - alphaNorm)));
				outG = (int) ((fgG * alphaNorm) + (bgG * (1 - alphaNorm)));
				outB = (int) ((fgB * alphaNorm) + (bgB * (1 - alphaNorm)));
				outA = (int) (bgA * (1 - alphaNorm) + fgA * alphaNorm);
				return (M3GMath.max(0, M3GMath.min(outA, 255)) << 24) | 
					(M3GMath.max(0, M3GMath.min(outR, 255)) << 16) | 
					(M3GMath.max(0, M3GMath.min(outG, 255)) << 8) | 
					M3GMath.max(0, M3GMath.min(outB, 255));

			case Texture2D.FUNC_MODULATE:
			case CompositingMode.MODULATE:
				outR = (int) ((fgR * bgR) / 255);
				outG = (int) ((fgG * bgG) / 255);
				outB = (int) ((fgB * bgB) / 255);
				outA = M3GMath.max(bgA, fgA);
				return (M3GMath.max(0, M3GMath.min(outA, 255)) << 24) | 
					(M3GMath.max(0, M3GMath.min(outR, 255)) << 16) | 
					(M3GMath.max(0, M3GMath.min(outG, 255)) << 8) | 
					M3GMath.max(0, M3GMath.min(outB, 255));

			case CompositingMode.MODULATE_X2:
				outR = (int) (((2 * fgR) * bgR) / 255);
				outG = (int) (((2 * fgG) * bgG) / 255);
				outB = (int) (((2 * fgB) * bgB) / 255);
				outA = M3GMath.max(bgA, fgA);
				return (M3GMath.max(0, M3GMath.min(outA, 255)) << 24) | 
					(M3GMath.max(0, M3GMath.min(outR, 255)) << 16) | 
					(M3GMath.max(0, M3GMath.min(outG, 255)) << 8) | 
					M3GMath.max(0, M3GMath.min(outB, 255));

			case Texture2D.FUNC_DECAL:
				outR = (fgR * fgA / 255) + (bgR * (255 - fgA) / 255);
				outG = (fgG * fgA / 255) + (bgG * (255 - fgA) / 255);
				outB = (fgB * fgA / 255) + (bgB * (255 - fgA) / 255);
				outA = fgA; // Use foreground's alpha
				return (M3GMath.min(M3GMath.max(outA, 0), 255) << 24) | 
					(M3GMath.min(M3GMath.max(outR, 0), 255) << 16) | 
					(M3GMath.min(M3GMath.max(outG, 0), 255) << 8) | 
					M3GMath.min(M3GMath.max(outB, 0), 255);

			case Texture2D.FUNC_ADD:
				outR = M3GMath.min(bgR + fgR, 255);
				outG = M3GMath.min(bgG + fgG, 255);
				outB = M3GMath.min(bgB + fgB, 255);
				outA = M3GMath.max(bgA, fgA); // Use maximum alpha
				return (M3GMath.min(M3GMath.max(outA, 0), 255) << 24) | 
					(outR << 16) | 
					(outG << 8) | 
					outB;

			default:
				return background; // Fallback
		}
	}

	private int blendFog(int pixelColor, int fogColor) 
	{
		/*
		 * M3G specifies that, the smaller the fogFactor value, the more we
		 * should blend the fog color into the received color... which means
		 * that the fog's contribution to the resulting color should be
		 * 1 - fogFactor;
		 */
		final int r = (int) (((pixelColor >> 16) & 0xFF) * fogFactor + ((fogColor >> 16) & 0xFF) * (1 - fogFactor));
		final int g = (int) (((pixelColor >> 8) & 0xFF) * fogFactor + ((fogColor >> 8) & 0xFF) * (1 - fogFactor));
		final int b = (int) ((pixelColor & 0xFF) * fogFactor + (fogColor & 0xFF) * (1 - fogFactor));
	
		// Fog only has RGB channels, so it's always fully opaque
		return (255 << 24) | (r << 16) | (g << 8) | b;
	}
}
