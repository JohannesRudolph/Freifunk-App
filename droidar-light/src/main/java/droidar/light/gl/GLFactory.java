package droidar.light.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import droidar.light.gl.animations.AnimationFaceToCamera;
import droidar.light.gl.animations.AnimationRotate;
import droidar.light.gl.scenegraph.MeshComponent;
import droidar.light.gl.scenegraph.MultiColoredShape;
import droidar.light.gl.scenegraph.Shape;
import droidar.light.gl.textures.TextureManager;
import droidar.light.gl.textures.TexturedShape;
import droidar.light.util.Vec;


public class GLFactory {
	private final Context context;
	private final GLCamera camera;
	private final TextureManager textureManager;

	public GLFactory(Context context, GLCamera camera, TextureManager textureManager) {
		this.context = context;
		this.camera = camera;
		this.textureManager = textureManager;
	}

	public MeshComponent newArrow() {
		Color top = Color.blue();
		Color bottom = Color.red();
		Color edge1 = Color.red();
		Color edge2 = Color.redTransparent();
		float height = 4f;
		float x = 0.7f;
		float y = 0f;
		return newArrow(x, y, height, top, edge1, bottom, edge2);
	}

	private MeshComponent newArrow(float x, float y, float height, Color top,
			Color edge1, Color bottom, Color edge2) {

		MeshComponent pyr = new Shape();

		MultiColoredShape s = new MultiColoredShape();

		s.add(new Vec(-x, 0, height), top);
		s.add(new Vec(1, 0, 0), edge1);
		s.add(new Vec(-y, 0, -height), bottom);

		MultiColoredShape s2 = new MultiColoredShape();
		s2.add(new Vec(0, -x, height), top);
		s2.add(new Vec(0, 1, 0), edge2);
		s2.add(new Vec(0, -y, -height), bottom);

		MultiColoredShape s3 = new MultiColoredShape();
		s3.add(new Vec(x, 0, height), top);
		s3.add(new Vec(-1, 0, 0), edge1);
		s3.add(new Vec(y, 0, -height), bottom);

		MultiColoredShape s4 = new MultiColoredShape();
		s4.add(new Vec(0, x, height), top);
		s4.add(new Vec(0, -1, 0), edge2);
		s4.add(new Vec(0, y, -height), bottom);

		pyr.addChild(s);
		pyr.addChild(s2);
		pyr.addChild(s3);
		pyr.addChild(s4);

		addRotateAnimation(pyr, 120, new Vec(0, 0, 1));

		return pyr;
	}

	private void addRotateAnimation(MeshComponent target, int speed,
			Vec rotationVec) {
		AnimationRotate a = new AnimationRotate(speed, rotationVec);
		target.addAnimation(a);
	}

	public MeshComponent newTextObject(String textToDisplay, float textSize) {
		TextView v = new TextView(context);

		v.setGravity(Gravity.CENTER);
		v.setTypeface(null, Typeface.BOLD);
		v.setTextColor(android.graphics.Color.WHITE);
		v.setShadowLayer(5.0f, 0.0f, 1.0f, android.graphics.Color.DKGRAY);

		// Set textcolor to black:
		// v.setTextColor(new Color(0, 0, 0, 1).toIntARGB());
		v.setText(textToDisplay);

		MeshComponent mesh = this.newTexturedSquare("textBitmap" + textToDisplay, loadBitmapFromView(v), textSize);
		mesh.addAnimation(new AnimationFaceToCamera(camera));

		return mesh;
	}

	public MeshComponent newTexturedSquare(String bitmapName, Bitmap bitmap, float heightInMeters) {
		TexturedShape s = new TexturedShape(bitmapName, bitmap, textureManager);
		float f = (float) bitmap.getHeight() / (float) bitmap.getWidth();
		float x = heightInMeters / f;

		float w2 = -x / 2;
		float h2 = -heightInMeters / 2;

		s.add(new Vec(-w2, 0, -h2), 0, 0);
		s.add(new Vec(-w2, 0, h2), 0, 1);
		s.add(new Vec(w2, 0, -h2), 1, 0);

		s.add(new Vec(w2, 0, h2), 1, 1);
		s.add(new Vec(-w2, 0, h2), 0, 1);
		s.add(new Vec(w2, 0, -h2), 1, 0);

		return s;
	}


	/**
	 * turns any view in a bitmap to load it to openGL eg
	 *
	 * @param v
	 *            the view to transform into the bitmap
	 * @return the bitmap with the correct size of the view
	 */
	public static Bitmap loadBitmapFromView(View v) {
		int width = ViewGroup.LayoutParams.WRAP_CONTENT;
		int heigth = ViewGroup.LayoutParams.WRAP_CONTENT;
		return loadBitmapFromView(v, width, heigth);
	}

	/**
	 * turns any view in a bitmap to load it to openGL eg
	 *
	 * @param v
	 *            the view to convert to the bitmap
	 * @param width
	 *            e.g. LayoutParams.WRAP_CONTENT or
	 *            MeasureSpec.makeMeasureSpec(*some width*, MeasureSpec.AT_MOST)
	 * @param heigth
	 * @return
	 */
	public static Bitmap loadBitmapFromView(View v, int width, int heigth) {
		// first calc the size the view will need:
		v.measure(width, heigth);
		// then create a bitmap to store the views drawings:
		Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(),
				v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		// wrap the bitmap:
		Canvas c = new Canvas(b);
		// set the view size to the mesured values:
		v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
		// and draw the view onto the bitmap contained in the canvas:
		v.draw(c);
		return b;
	}
}
