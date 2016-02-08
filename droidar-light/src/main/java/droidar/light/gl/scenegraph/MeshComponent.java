package droidar.light.gl.scenegraph;

import android.util.Log;

import droidar.light.gl.Color;
import droidar.light.gl.HasColor;
import droidar.light.gl.HasPosition;
import droidar.light.gl.HasRotation;
import droidar.light.gl.HasScale;
import droidar.light.gl.Renderable;
import droidar.light.gl.animations.GLAnimation;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.util.Vec;
import droidar.light.world.Entity;

/**
 * This is a subclass of {@link Entity} and it can be used for any
 * type of World Object which has a position ( {@link HasPosition} ), a
 * {@link Color}, a rotation ( {@link HasRotation} ) and a scale (
 * {@link HasScale} ). <br>
 * It can have children {@link MeshComponent#addChild(Entity)} so also
 * a {@link Shape} or can have direct children if required.
 * A special type of child is the {@link GLAnimation} (which is a
 * {@link Entity} as well).
 * 
 * @author Spobo
 * 
 */
public abstract class MeshComponent implements Entity, HasPosition, HasColor, HasRotation, HasScale {

	private static final String LOG_TAG = "MeshComp";
	/**
	 * positive x value is in east direction (along red axis) positive y value
	 * is i north direction (along green axis) positive z value is in sky
	 * direction
	 */
	private Vec position;
	/**
	 * a vector that describes how the MeshComp is rotated. For example:
	 * Vec(90,0,0) would rotate it 90 degree around the x axis
	 */
	private Vec rotation;
	private Vec scale;
	private Color color;
	private RenderList children;
	private MeshComponent parent;


	public void setParent(MeshComponent parent) {
		this.parent = parent;
	}

	public Vec calculateAbsolutePosition() {
		// recurse up the hierarchy
		Vec parentPos = parent!= null ? parent.calculateAbsolutePosition() : new Vec();
		return parentPos.add(getPosition());
	}

	@Override
	public Vec getScale() {
		return scale;
	}

	@Override
	public void setScale(Vec scale) {
		if (this.scale == null)
			this.scale = scale.copy();
		else
			this.scale.setToVec(scale);
	}

	@Override
	public Vec getRotation() {
		return rotation;
	}

	@Override
	public void setRotation(Vec rotation) {
		if (this.rotation == null)
			this.rotation = rotation.copy();
		else
			this.rotation.setToVec(rotation);
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setColor(Color c) {
		if (color == null)
			color = c.copy();
		else
			color.setTo(c);
	}

	@Override
	public Vec getPosition() {
		if (position == null)
			position = new Vec();
		return position;
	}

	@Override
	public void setPosition(Vec position) {
		if (this.position == null)
			this.position = position.copy();
		else
			this.position.setToVec(position);
	}

	private void loadPosition(GL10 gl) {
		if (position != null)
			gl.glTranslatef(position.x, position.y, position.z);
	}

	private void loadRotation(GL10 gl) {
		if (rotation != null) {
			/*
			 * this order is important. first rotate around the blue-z-axis
			 * (like a compass) then the the green-y-axis and red-x-axis. the
			 * order of the x and y axis rotations normaly is not important but
			 * first x and then y is better in this case because of
			 * Vec.calcRotationVec which may be extendet to add also a y
			 * rotation which then would have to be rotated last to not make the
			 * x-axis rotation wrong. so z x y is the best rotation order but
			 * normaly z y x would work too:
			 */
			gl.glRotatef(rotation.z, 0, 0, 1);
			gl.glRotatef(rotation.x, 1, 0, 0);
			gl.glRotatef(rotation.y, 0, 1, 0);
		}

	}

	private void setScale(GL10 gl) {
		if (scale != null)
			gl.glScalef(scale.x, scale.y, scale.z);
	}

	@Override
	public synchronized void render(GL10 gl) {

		// store current matrix and then modify it:
		gl.glPushMatrix();
		loadPosition(gl);
		setScale(gl);
		loadRotation(gl);

		if (color != null) {
			gl.glColor4f(color.red, color.green, color.blue, color.alpha);
		}

		if (children != null) {
			children.render(gl);
		}

		draw(gl);
		// restore old matrix:
		gl.glPopMatrix();
	}

	/**
	 * Don't override the {@link Renderable#render(GL10)} method if
	 * you are creating a subclass of {@link MeshComponent}. Instead implement
	 * this method and all the translation and rotation abilities of the
	 * {@link MeshComponent} will be applied automatically
	 * 
	 * @param gl
	 */
	public abstract void draw(GL10 gl);

	@Override
	public boolean update(float timeDelta) {
		if ((children != null)) {

			// if the animation does not need to be animated anymore..
			if (!children.update(timeDelta)) {
				// ..remove it:
				Log.d(LOG_TAG, children
						+ " will now be removed from mesh because it "
						+ "is finished (returned false on update())");
				children = null;
			}
		}
		return true;
	}

	/**
	 * @param child
	 */
	public void addChild(Entity child) {
		addChildEntity(child, false);
	}

	/**
	 * An animation will be inserted at the BEGINNING of the children list. So
	 * the last animation added will be executed first by the renderer!
	 * 
	 * @param animation
	 */
	public void addAnimation(GLAnimation animation) {
		animation.setTarget(this);
		addChildEntity(animation, true);
	}

	private void addChildEntity(Entity a, boolean insertAtBeginnung) {
		if (children == null) {
			children = new RenderList();
		}

		if (insertAtBeginnung) {
			children.insert(0, a);
		} else {
			children.add(a);
		}

		if (a instanceof MeshComponent){
			MeshComponent mc = (MeshComponent) a;
			mc.setParent(this);
		}
	}

	public void removeChild(Entity c) {
		if (children == null) {
			children = new RenderList();
		}

		children.remove(c);

		if (c instanceof MeshComponent){
			MeshComponent mc = (MeshComponent) c;
			mc.setParent(null);
		}
	}
}
