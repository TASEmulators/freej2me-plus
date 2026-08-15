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
	// Special blend mode for fog
	public static final int BLEND_FOG = -1;

	public static final int ANTIALIAS = 2;
	public static final int DITHER = 4;
	public static final int OVERWRITE = 16; // This might be unused here, as SW rasterization gives us direct control over pixels
	public static final int TRUE_COLOR = 8;


	public static final boolean SUPPORT_ANTIALIASING = false;
	public static final boolean SUPPORT_TRUE_COLOR = true;
	public static final boolean SUPPORT_DITHERING = false;
	public static final boolean SUPPORT_MIPMAPPING = true;
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
	int canvasWidth, canvasHeight;
	int[] rasterData;

	// Vertex color blending
	int alpha, r, g, b;

	// 3D rendering variables
	int yStart, yEnd, ixL, ixR;
	final int[] ord = new int[3];
	float[] vertClip = null;
	float[] texVert = null;
	final float[] projParams = new float[4];
	float xTop, yTop, zTop, sTop, tTop;
	float xMidL, yMid, zMidL, sMidL, tMidL;
	float xBot, yBot, zBot, sBot, tBot;
	float rHorizon, xMidR, zMidR, sMidR, tMidR;
	float drawY, drawX, xL, xR, zL, zR, sL, sR, tL, tR;
	float pwTop, pwMidL, pwBot, pwMidR, pwL, pwR;

	float z, s, t;
	final float[] scaleBias = new float[4];

	final Transform projectionMatrix = new Transform();
	final int[] renderableTriangles = {0}; // Counter for visible triangles

	// fog blending factor
	float fogFactor = 0.0f;

	// Textured polygon variables
	final float[] coX = new float[3];
	final float[] coY = new float[3];
	final float[] coZ = new float[3];
	final float[] coS = new float[3];
	final float[] coT = new float[3];
	final float[] coW = new float[3];

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

					for (int py = 0; py < viewh; py++)
					{
						int sy = cropY + (int) (py * cropH / viewh);
						sy = wrapY(sy, bgImg.getHeight(), repeatY, bgImg.isPowerOfTwo(bgImg.getHeight()));

						for (int px = 0; px < vieww; px++)
						{
							int sx = cropX + (int) (px * cropW / vieww);
							sx = wrapY(sx, bgImg.getWidth(), repeatX, bgImg.isPowerOfTwo(bgImg.getWidth()));

							// Image format argument shouldn't matter here
							rasterData[(py + viewy) * canvasWidth + (px + viewx)] =
								blendPixels(rasterData[(py + viewy) * canvasWidth + (px + viewx)], bgImg.getPixel(sx, sy),
									(bgImg.getPixel(sx, sy) >> 24) & 0xFF, CompositingMode.ALPHA, 0, 0);
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
			if(!node.isRenderingEnabled()) { return; }
			renderSprite((Sprite3D) node, transform);
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

							if (child instanceof Sprite3D) { renderSprite((Sprite3D) child, t); }
							else { render(child, t); }
						}
					}
					child = child.right;
				} while (child != ((Group) node).firstChild);
			}
		}
	}

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

		/* As per JSR-184, a Sprite3D with no appearance (or no image) is not rendered. */
		if (img == null || appearance == null) { return; }
		if (!(this.target instanceof Graphics)) { return; }
		/* JSR-184 scope culling, same rule as for meshes. */
		if ((sprite.getScope() & this.currCam.getScope()) == 0) { return; }

		/* The crop rectangle keeps its sign; negative dimensions flip the image on that axis. */
		final int cropX = sprite.getCropX(), cropY = sprite.getCropY();
		int cropW = sprite.getCropWidth(), cropH = sprite.getCropHeight();
		final boolean flipX = cropW < 0, flipY = cropH < 0;
		if (flipX) { cropW = -cropW; }
		if (flipY) { cropH = -cropH; }
		if (cropW == 0 || cropH == 0) { return; }

		/* Intersect the crop rectangle with the image rectangle; nothing to render without overlap. */
		final int isectX = M3GMath.max(cropX, 0), isectY = M3GMath.max(cropY, 0);
		final int isectW = M3GMath.min(cropX + cropW, img.getWidth()) - isectX;
		final int isectH = M3GMath.min(cropY + cropH, img.getHeight()) - isectY;
		if (isectW <= 0 || isectH <= 0) { return; }

		if (transform == null) { transform = new Transform(); }

		/* Model-view: the sprite's rotation/scale only affect its size, never its screen alignment. */
		final Transform modelView = new Transform(transform);
		modelView.preMultiply(this.currCamTransInv);

		/* Origin and half-unit axis points in eye space (affine transform, w stays 1). */
		final float[] eye = { 0,0,0,1,  0.5f,0,0,1,  0,0.5f,0,1 };
		modelView.transform(eye);
		final float ox = eye[0]/eye[3], oy = eye[1]/eye[3], oz = eye[2]/eye[3];
		final float dx0 = eye[4]/eye[7] - ox, dy0 = eye[5]/eye[7] - oy, dz0 = eye[6]/eye[7] - oz;
		final float dx1 = eye[8]/eye[11] - ox, dy1 = eye[9]/eye[11] - oy, dz1 = eye[10]/eye[11] - oz;
		final float halfUnitX = M3GMath.sqrt(dx0*dx0 + dy0*dy0 + dz0*dz0);
		final float halfUnitY = M3GMath.sqrt(dx1*dx1 + dy1*dy1 + dz1*dz1);

		/* Project the origin plus screen-aligned extent points. */
		this.currCam.getProjection(projectionMatrix);
		final float[] clip = { ox,oy,oz,1,  ox+halfUnitX,oy,oz,1,  ox,oy+halfUnitY,oz,1 };
		projectionMatrix.transform(clip);
		if (clip[3] <= 0f || clip[7] <= 0f || clip[11] <= 0f) { return; } /* Behind the camera */

		float ndcX = clip[0]/clip[3], ndcY = clip[1]/clip[3];
		final float ndcZ = clip[2]/clip[3];
		if (ndcZ < -1f || ndcZ > 1f) { return; } /* Outside the depth range */
		float halfW = M3GMath.abs(clip[4]/clip[7] - ndcX);
		float halfH = M3GMath.abs(clip[9]/clip[11] - ndcY);

		if (sprite.isScaled())
		{
			/* Adjust the position and size according to the (possibly partly outside) crop rectangle. */
			final float unitX = halfW / (float) cropW, unitY = halfH / (float) cropH;
			ndcX -= (2*cropX + cropW - 2*isectX - isectW) * unitX;
			ndcY += (2*cropY + cropH - 2*isectY - isectH) * unitY;
			halfW = unitX * isectW;
			halfH = unitY * isectH;
		}
		else
		{
			/* Non-scaled sprites take their size in pixels from the crop rectangle. */
			ndcX -= (float)(2*cropX + cropW - 2*isectX - isectW) / (float) vieww;
			ndcY += (float)(2*cropY + cropH - 2*isectY - isectH) / (float) viewh;
			halfW = (float) isectW / (float) vieww;
			halfH = (float) isectH / (float) viewh;
		}

		/* NDC -> viewport-relative pixels (same mapping as the triangle rasterizer). */
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

		// The Sprite3D has the same dapth for its entire area, so we only need
		// to calculate fog once.
		if (fog != null)
		{
			// Distance in eye space along the camera's viewing axis
			final float zEye = -oz;

			float fogFactor;
			if (fog.getMode() == Fog.LINEAR)
			{
				fogFactor = (fog.getFarDistance() - zEye) / (fog.getFarDistance() - fog.getNearDistance());
			}
			else
			{
				fogFactor = M3GMath.exp(-fog.getDensity() * zEye);
			}

			fogFactor = M3GMath.max(0.0f, M3GMath.min(255.0f, fogFactor * 256.0f));
		}

		for (int y = pixT; y < pixB; y++)
		{
			final float v = (y + 0.5f - sy0) / spanY;
			int texY = isectY + (int) ((flipY ? 1f - v : v) * isectH);
			if (texY < isectY) { texY = isectY; } else if (texY >= isectY + isectH) { texY = isectY + isectH - 1; }

			for (int x = pixL; x < pixR; x++)
			{
				/* Depth test against the same buffer and convention used by triangles. */
				if (depthTest && this.depthBuffer[this.vieww * y + x] < ndcZ) { continue; }

				final float u = (x + 0.5f - sx0) / spanX;
				int texX = isectX + (int) ((flipX ? 1f - u : u) * isectW);
				if (texX < isectX) { texX = isectX; } else if (texX >= isectX + isectW) { texX = isectX + isectW - 1; }

				int paintPixel = img.getPixel(texX, texY);
				alpha = (int) (((paintPixel >> 24) & 0xFF) * alphaFactor);
				if (alpha < alphaThreshold || alpha == 0) { continue; } /* Alpha test discards the fragment before any writes */

				if (fog != null && fogFactor < 255.0f)
					{ paintPixel = blendPixels(paintPixel, fog.getColor(), (int) fogFactor, Graphics3D.BLEND_FOG, 0, 0); }

				final int finalPixel = blendPixels(rasterData[(y+viewy) * canvasWidth + (x+viewx)],
					paintPixel, alpha, compositingMode.getBlending(), 0, 0);

				rasterData[(y+viewy) * canvasWidth + (x+viewx)] = finalPixel;

				if (depthWrite) { this.depthBuffer[this.vieww * y + x] = ndcZ; }
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

		// if `vertices` or `triangles` violates the constraints
		//    defined in VertexBuffer or IndexBuffer
		//    throw new java.lang.IllegalStateException();

		final int projType = this.currCam.getProjection(projParams);

		final CompositingMode compositingMode = appearance.getCompositingMode() != null ? appearance.getCompositingMode() : new CompositingMode();

		// TODO: Shading mode is not implemented
		final int shadingMode = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getShading() : PolygonMode.SHADE_SMOOTH;

		final int cullingMode = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getCulling() : PolygonMode.CULL_BACK;
		final int windingOrder = appearance.getPolygonMode() != null ? appearance.getPolygonMode().getWinding() : PolygonMode.WINDING_CCW;
		boolean perspectiveCorrection = appearance.getPolygonMode() != null ? appearance.getPolygonMode().isPerspectiveCorrectionEnabled() : false;
		perspectiveCorrection = perspectiveCorrection && (projType == Camera.PERSPECTIVE);

		ord[0] = 0;
		ord[1] = 1;
		ord[2] = 2;

		// Set up fog properties
		final Fog fog = appearance.getFog();
		final float invFogDiv = fog != null ? M3GMath.fastReciprocal(fog.getFarDistance() - fog.getNearDistance()) : 0.0f;

		final VertexArray vertPos = vertices.getPositions(scaleBias);
		final Texture2D tex = appearance.getTexture(0);
		final Image2D teximg = tex == null ? null : tex.getImage();

		final Transform tr = new Transform();
		final Transform textr = new Transform();
		final Transform texcomptr = new Transform();

		/* Texture wrapping mode and dimensions, applied per-pixel while sampling */
		final boolean texRepeatS = (tex != null) && tex.getWrappingS() == Texture2D.WRAP_REPEAT;
		final boolean texRepeatT = (tex != null) && tex.getWrappingT() == Texture2D.WRAP_REPEAT;
		final int texW = (teximg != null) ? teximg.getWidth() : 0;
		final int texH = (teximg != null) ? teximg.getHeight() : 0;

		if (tex != null) { tex.getCompositeTransform(texcomptr); }

		/* Receiving a null transform indicates that the identity matrix must be used. */
		if (transform == null) { transform = new Transform(); }

		// -> Local space

		// Scale and translate mesh (P = (S * V) + B)
		tr.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
		tr.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);

		// Get Texture coordinates
		final VertexArray texCoords = vertices.getTexCoords(0, scaleBias);

		// Scale and translate texture coordinates (same scaleBias)
		textr.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
		textr.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);

		textr.preMultiply(texcomptr);

		// Transform mesh from local coords to world coords
		tr.preMultiply(transform);
		// -> World space

		// Apply the inverse of the camera's transform to the mesh
		tr.preMultiply(this.currCamTransInv);
		// -> Eye/View space

		// Apply projection matrix
		this.currCam.getProjection(projectionMatrix);
		tr.preMultiply(projectionMatrix);
		// -> Clip space

		// Do the transformation
		if(vertClip == null || 4 * vertPos.getVertexCount() > vertClip.length)
			vertClip = new float[4 * vertPos.getVertexCount()];
		tr.transform(vertPos, vertClip, true);

		if(texVert == null || 4 * vertPos.getVertexCount() > texVert.length)
			texVert = new float[4 * vertPos.getVertexCount()];
		if (texCoords != null) { textr.transform(texCoords, texVert, true); }

		/*
		 * Near-plane distance for clipping: the camera's actual near plane (where
		 * w_clip == -z_eye == near), NOT the depth-range near (which defaults to 0).
		 * Clipping against w >= 0 leaves vertices at w == 0 that blow up to infinity
		 * in the perspective division, dropping every triangle that crosses the plane.
		 */
		final float clipNear = (projType == Camera.PERSPECTIVE) ? M3GMath.max(projParams[2], 1e-4f) : 1e-4f;

		// Create Triangle objects (fromVertsAndTris already does culling and clipping)
		final Triangle[] trisScreen = Triangle.fromVertAndTris(vertClip, texVert, triangles.getIndexArray(),
			renderableTriangles, clipNear, cullingMode, vertices, windingOrder == PolygonMode.WINDING_CW,
			perspectiveCorrection);

		/*
		 * Per-triangle flat lighting (JSR-184 lighting requires a Material on the
		 * Appearance). Lights and vertices are brought to camera space once per
		 * render; each triangle then gets a diffuse+ambient factor from its
		 * geometric normal, applied to the rasterized color below.
		 */
		final Material material = appearance.getMaterial();
		float[] litVerts = null;
		/* per light: [mode, r, g, b, x, y, z] with color premultiplied by intensity */
		float[][] litLights = null;

		if (material != null && !this.currLights.isEmpty())
		{
			litVerts = new float[4 * vertPos.getVertexCount()];
			final Transform mv = new Transform();
			mv.postTranslate(scaleBias[1], scaleBias[2], scaleBias[3]);
			mv.postScale(scaleBias[0], scaleBias[0], scaleBias[0]);
			mv.preMultiply(transform);
			mv.preMultiply(this.currCamTransInv);
			mv.transform(vertPos, litVerts, true);

			litLights = new float[this.currLights.size()][7];
			final float[] lv = new float[16];
			for (int li = 0; li < this.currLights.size(); li++)
			{
				final Light light = this.currLights.get(li);
				if (light == null) { litLights[li][0] = -1; continue; }
				final Transform lt = new Transform(this.currLightTrans.get(li));
				lt.preMultiply(this.currCamTransInv);
				lt.get(lv);
				litLights[li][0] = light.getMode();
				litLights[li][1] = ((light.getColor() >> 16) & 0xFF) / 255f * light.getIntensity();
				litLights[li][2] = ((light.getColor() >> 8) & 0xFF) / 255f * light.getIntensity();
				litLights[li][3] = (light.getColor() & 0xFF) / 255f * light.getIntensity();
				if (light.getMode() == Light.DIRECTIONAL)
				{
					/* Light direction: -Z axis of the light's transform, in camera space. */
					litLights[li][4] = -lv[2]; litLights[li][5] = -lv[6]; litLights[li][6] = -lv[10];
				}
				else /* OMNI and SPOT (treated as OMNI): light position in camera space. */
				{
					litLights[li][4] = lv[3]; litLights[li][5] = lv[7]; litLights[li][6] = lv[11];
				}
			}
		}
		float litR = 1f, litG = 1f, litB = 1f;

		// At this point the triangles in `trisScreen` are actually
		// projected to Normalized Device Coordinates, but they will be tranformed
		// to Screen space in-place, hence the name.

		// Reset transform
		tr.setIdentity();
		textr.setIdentity();

		// Fit to viewport
		if (teximg != null) { textr.postScale(teximg.getWidth(), teximg.getHeight(), 1); }
		tr.postScale(vieww / 2f, -viewh / 2f, 1f);
		tr.postTranslate(1, -1, 0);

		// -> Screen space

		// Perform viewport transform only on renderable triangles (saves an Arrays.copyOf call)
		Triangle.transform(trisScreen, renderableTriangles[0], tr, textr);

		final boolean depthEnabled = compositingMode.isDepthTestEnabled() && isDepthBufferEnabled();
		final boolean hasTexture = tex != null && texCoords != null && !Mobile.M3GRenderUntexturedPolygons && !Mobile.M3GRenderWireframe;
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
				coS[0] = trisScreen[tri_id].sA(); coS[1] = trisScreen[tri_id].sB(); coS[2] = trisScreen[tri_id].sC();
				coT[0] = trisScreen[tri_id].tA(); coT[1] = trisScreen[tri_id].tB(); coT[2] = trisScreen[tri_id].tC();
				coW[0] = trisScreen[tri_id].iwA(); coW[1] = trisScreen[tri_id].iwB(); coW[2] = trisScreen[tri_id].iwC();

				if (litVerts != null)
				{
					/* Flat lighting factor for this triangle from its geometric normal in camera space. */
					final int liA = trisScreen[tri_id].getIndex(0) * 4;
					final int liB = trisScreen[tri_id].getIndex(1) * 4;
					final int liC = trisScreen[tri_id].getIndex(2) * 4;
					final float e1x = litVerts[liB] - litVerts[liA], e1y = litVerts[liB+1] - litVerts[liA+1], e1z = litVerts[liB+2] - litVerts[liA+2];
					final float e2x = litVerts[liC] - litVerts[liA], e2y = litVerts[liC+1] - litVerts[liA+1], e2z = litVerts[liC+2] - litVerts[liA+2];
					float nx = e1y*e2z - e1z*e2y, ny = e1z*e2x - e1x*e2z, nz = e1x*e2y - e1y*e2x;
					final float nlen = M3GMath.sqrt(nx*nx + ny*ny + nz*nz);
					litR = 0f; litG = 0f; litB = 0f;
					if (nlen > 0f)
					{
						nx /= nlen; ny /= nlen; nz /= nlen;
						for (int li = 0; li < litLights.length; li++)
						{
							final float mode = litLights[li][0];
							if (mode == Light.AMBIENT)
							{
								litR += litLights[li][1]; litG += litLights[li][2]; litB += litLights[li][3];
							}
							else if (mode == Light.DIRECTIONAL || mode == Light.OMNI || mode == Light.SPOT)
							{
								float lx, ly, lz;
								if (mode == Light.DIRECTIONAL) { lx = -litLights[li][4]; ly = -litLights[li][5]; lz = -litLights[li][6]; }
								else
								{
									/* Direction from the triangle towards the light position. */
									lx = litLights[li][4] - litVerts[liA]; ly = litLights[li][5] - litVerts[liA+1]; lz = litLights[li][6] - litVerts[liA+2];
								}
								final float llen = M3GMath.sqrt(lx*lx + ly*ly + lz*lz);
								if (llen <= 0f) { continue; }
								/* Two-sided diffuse term, so winding/normal direction doesn't black out faces. */
								final float ndl = M3GMath.abs((nx*lx + ny*ly + nz*lz) / llen);
								litR += litLights[li][1] * ndl; litG += litLights[li][2] * ndl; litB += litLights[li][3] * ndl;
							}
						}
					}
					else { litR = 1f; litG = 1f; litB = 1f; }
					if (litR > 1f) { litR = 1f; } if (litG > 1f) { litG = 1f; } if (litB > 1f) { litB = 1f; }
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
				sTop = coS[ord[0]]; sMidL = coS[ord[1]]; sBot = coS[ord[2]];
				tTop = coT[ord[0]]; tMidL = coT[ord[1]]; tBot = coT[ord[2]];
				pwTop = coW[ord[0]]; pwMidL = coW[ord[1]]; pwBot = coW[ord[2]];

				// Calculate the right horizon
				rHorizon = (yMid - yTop) / (yBot - yTop);
				xMidR = xTop + rHorizon * (xBot - xTop);
				zMidR = zTop + rHorizon * (zBot - zTop);
				sMidR = sTop + rHorizon * (sBot - sTop);
				tMidR = tTop + rHorizon * (tBot - tTop);
				pwMidR = pwTop + rHorizon * (pwBot - pwTop);

				// Swap midpoints if necessary
				if (xMidL > xMidR)
				{
					float temp;

					// Swap values between left and right midpoints
					temp = xMidL; xMidL = xMidR; xMidR = temp;
					temp = zMidL; zMidL = zMidR; zMidR = temp;
					temp = sMidL; sMidL = sMidR; sMidR = temp;
					temp = tMidL; tMidL = tMidR; tMidR = temp;
					temp = pwMidL; pwMidL = pwMidR; pwMidR = temp;
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

						// Calculate interpolated values (xL and xR allow us to skip early, so do them first)
						xL = half == 0
							? xTop + drawY * (xMidL - xTop)
							: xBot + drawY * (xMidL - xBot);
						xR = half == 0
							? xTop + drawY * (xMidR - xTop)
							: xBot + drawY * (xMidR - xBot);

						ixL = M3GMath.max(M3GMath.roundPositive(xL), 0);
						ixR = M3GMath.min(M3GMath.roundPositive(xR), vieww);

						final int spanWidth = ixR - ixL;

						if (spanWidth <= 0) { continue; }

						// Used for vertex color blending and nothing else.
						final float invSpanWidth = M3GMath.fastReciprocal(spanWidth);

						// Do we have vertex colors? If so, get the span edges' colors here,
						// that way, the inner loop only needs to do a simple interpolation.
						int aL = 0, rL = 0, gL = 0, bL = 0, aR = 0, rR = 0, gR = 0, bR = 0;
						int deltaA = 0, deltaR = 0, deltaG = 0, deltaB = 0;
						if (trisScreen[tri_id].hasVertexColors())
						{
							float xA = trisScreen[tri_id].xA();
							float xB = trisScreen[tri_id].xB();
							float xC = trisScreen[tri_id].xC();
							float yA = trisScreen[tri_id].yA();
							float yB = trisScreen[tri_id].yB();
							float yC = trisScreen[tri_id].yC();

							float denominator = (xB - xA) * (yC - yA) - (xC - xA) * (yB - yA);

							if (M3GMath.abs(denominator) > M3GMath.EPSILON)
							{
								int colorA = trisScreen[tri_id].colorA();
								int colorB = trisScreen[tri_id].colorB();
								int colorC = trisScreen[tri_id].colorC();

								// Calculate the left edge's color
								float c1 = M3GMath.min(1.0f, M3GMath.max(0.0f, ((xB - ixL) * (yC - y) - (xC - ixL) * (yB - y)) / denominator));
								float c2 = M3GMath.min(1.0f, M3GMath.max(0.0f, ((xC - ixL) * (yA - y) - (xA - ixL) * (yC - y)) / denominator));
								float c3 = 1.0f - c1 - c2;

								aL = (int) (c1 * ((colorA >> 24) & 0xFF) + c2 * ((colorB >> 24) & 0xFF) + c3 * ((colorC >> 24) & 0xFF));
								rL = (int) (c1 * ((colorA >> 16) & 0xFF) + c2 * ((colorB >> 16) & 0xFF) + c3 * ((colorC >> 16) & 0xFF));
								gL = (int) (c1 * ((colorA >> 8) & 0xFF) + c2 * ((colorB >> 8) & 0xFF) + c3 * ((colorC >> 8) & 0xFF));
								bL = (int) (c1 * (colorA & 0xFF) + c2 * (colorB & 0xFF) + c3 * (colorC & 0xFF));

								// Now the right edge's color
								c1 = M3GMath.min(1.0f, M3GMath.max(0.0f, ((xB - ixR) * (yC - y) - (xC - ixR) * (yB - y)) / denominator));
								c2 = M3GMath.min(1.0f, M3GMath.max(0.0f, ((xC - ixR) * (yA - y) - (xA - ixR) * (yC - y)) / denominator));
								c3 = 1.0f - c1 - c2;

								aR = (int) (c1 * ((colorA >> 24) & 0xFF) + c2 * ((colorB >> 24) & 0xFF) + c3 * ((colorC >> 24) & 0xFF));
								rR = (int) (c1 * ((colorA >> 16) & 0xFF) + c2 * ((colorB >> 16) & 0xFF) + c3 * ((colorC >> 16) & 0xFF));
								gR = (int) (c1 * ((colorA >> 8) & 0xFF) + c2 * ((colorB >> 8) & 0xFF) + c3 * ((colorC >> 8) & 0xFF));
								bR = (int) (c1 * (colorA & 0xFF) + c2 * (colorB & 0xFF) + c3 * (colorC & 0xFF));

								deltaA = aR - aL;
								deltaR = rR - rL;
								deltaG = gR - gL;
								deltaB = bR - bL;
							}
						}

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
						pwL = half == 0
							? pwTop + drawY * (pwMidL - pwTop)
							: pwBot + drawY * (pwMidL - pwBot);
						pwR = half == 0
							? pwTop + drawY * (pwMidR - pwTop)
							: pwBot + drawY * (pwMidR - pwBot);

						int depthIdxY = this.vieww * y;
						int rasterIdxY = (y + viewy) * canvasWidth + viewx;

						// Draw the pixels for the current y-coordinate
						for (int x = ixL; x < ixR; x++)
						{
							// This check is really only used for wireframe debugging, and it's not a perfect wireframe rendering
							if(Mobile.M3GRenderWireframe && x > ixL && x < ixR) { continue; }

							drawX = M3GMath.max(0f, M3GMath.min((x - xL) / (xR - xL), 1f));
							z = (zL + drawX * (zR - zL));

							// Only depth test if the compositingMode has the feature enabled. If
							// compositingMode is not set, check if this target has depthBuffer enabled.
							if(depthEnabled && this.depthBuffer[depthIdxY + x] < z) { continue; }

							s = sL + drawX * (sR - sL);
							t = tL + drawX * (tR - tL);

							int paintPixel;

							// We have to do texture blending if we have vertex colors, as any available texture goes on top of them
							if (trisScreen[tri_id].hasVertexColors())
							{
								// Interpolate from xL to xR based on current X.
								// No need to calculate barycentric coords on every pixel.
								float xPos = (x - ixL) * invSpanWidth;

								alpha = (int) (aL + xPos * deltaA);
								r     = (int) (rL + xPos * deltaR);
								g     = (int) (gL + xPos * deltaG);
								b     = (int) (bL + xPos * deltaB);

								paintPixel = (alpha << 24) | (r << 16) | (g << 8) | b;
							}
							else
							{
								// If there's no texture coords or a texture image, we default to rendering with vertex colors. (also used for debug render modes)
								// It's forced to opaque when blending mode is set to REPLACE.
								paintPixel = compositingMode.getBlending() == CompositingMode.REPLACE ?
									0xFF000000 | vertices.getDefaultColor() : vertices.getDefaultColor();
							}

							/*
							 * Alpha test BEFORE any depth write: transparent fragments must not
							 * occlude geometry drawn later (games rely on this — e.g. tree canopies
							 * with alpha cutouts drawn before the ground). The depth buffer is only
							 * updated by fragments that survive this test.
							 */
							if (((paintPixel >> 24) & 0xFF) <= alphaThreshold) { continue; }

							if(hasTexture)
							{
								// TODO: Allow perspective correction force-disable
								if(perspectiveCorrection)
								{
									float pw = pwL + drawX * (pwR - pwL);
									if (pw > 1e-9f || pw < -1e-9f) { s /= pw; t /= pw; }
								}

								// We can force-disable bilinear filter
								if (tex.getImageFilter() == Texture2D.FILTER_LINEAR && !Mobile.m3gDisableBilinearFilter)
								{
									paintPixel = blendPixels(paintPixel,
										sampleBilinear(teximg, s, t, texW, texH, texRepeatS, texRepeatT, tex.isNPOT()),
										255, tex.getBlending(), tex.getBlendColor(), teximg.getFormat());
								}
								else
								{
									int texX = M3GMath.roundPositive(s), texY = M3GMath.roundPositive(t);
									texX = wrapX(texX, texW, texRepeatS, tex.isNPOT());
									texY = wrapY(texY, texH, texRepeatT, tex.isNPOT());
									paintPixel = blendPixels(paintPixel, teximg.getPixel(texX, texY), 255,
										tex.getBlending(), tex.getBlendColor(), teximg.getFormat());
								}

								if (((paintPixel >> 24) & 0xFF) <= alphaThreshold) { continue; }
							}

							if (litVerts != null)
							{
								/* Modulate the rasterized color with this triangle's flat lighting factor. */
								paintPixel = (paintPixel & 0xFF000000)
									| (((int) (((paintPixel >> 16) & 0xFF) * litR)) << 16)
									| (((int) (((paintPixel >> 8) & 0xFF) * litG)) << 8)
									| ((int) ((paintPixel & 0xFF) * litB));
							}

							// Update the depth buffer if depth write is enabled
							if (depthEnabled && compositingMode.isDepthWriteEnabled()) { this.depthBuffer[depthIdxY + x] = z; }

							// To blend the fog value here, we have to take the current pixel's z value into consideration
							if (fog != null)
							{
								// Fog is always perspective-correct
								final float zEye = M3GMath.fastReciprocal(pwL + drawX * (pwR - pwL));

								if (fog.getMode() == Fog.LINEAR)
								{
									fogFactor = M3GMath.max(0, M3GMath.min(1, (fog.getFarDistance() - zEye) * invFogDiv));
								}
								else { fogFactor = M3GMath.abs(M3GMath.exp(-fog.getDensity() * zEye)); }

								fogFactor = M3GMath.max(0.0f, M3GMath.min(255.0f, fogFactor * 256.0f));

								if (fogFactor < 255.0f) { paintPixel = blendPixels(paintPixel, fog.getColor(), (int) fogFactor, Graphics3D.BLEND_FOG, 0, 0); }
							}

							// Handle compositing mode with background pixel [rasterData] AFTER the fog calculation, otherwise alpha values won't be correct.
							final int finalPixel = blendPixels(rasterData[rasterIdxY + x],
								paintPixel, (paintPixel >> 24) & 0xFF, compositingMode.getBlending(), 0, 0);

							rasterData[rasterIdxY + x] = finalPixel;

							// Rendering at half res?
							if (Mobile.halfResM3GRaster && y+viewy < canvasHeight) { rasterData[rasterIdxY + canvasWidth + x] = finalPixel; }
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
	private final int blendPixels(int bg, int fg, int alpha, int blendMode, int texBlendColor, int texFormat)
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

				int outR = (texFormat == Image2D.ALPHA) ? fR : (fR * tR) >> 8;
				int outG = (texFormat == Image2D.ALPHA) ? fG : (fG * tG) >> 8;
				int outB = (texFormat == Image2D.ALPHA) ? fB : (fB * tB) >> 8;

				boolean hasAlpha = (texFormat == Image2D.ALPHA ||
					texFormat == Image2D.LUMINANCE_ALPHA || texFormat == Image2D.RGBA);
				int outA = hasAlpha ? (fA * tA) >> 8 : fA;

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

			default:
				return bg;
		}
	}

	// For bilinear filtering support
	private final int sampleBilinear(Image2D teximg, float s, float t, int texW, int texH, boolean texRepeatS, boolean texRepeatT, boolean isNPOT)
	{
		// Shift by 0.5 for OpenGL-like filtering,
		int uFixed = (int) ((s - 0.5f) * 256.0f);
		int vFixed = (int) ((t - 0.5f) * 256.0f);

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

		// Are all colors the same? Don't waste time blending.
		if (c00 == c10 && c00 == c01 && c00 == c11) { return c00; }

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
	private final int wrapX(int x, int width, boolean repeat, boolean isNPOT)
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

	private final int wrapY(int y, int height, boolean repeat, boolean isNPOT)
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
