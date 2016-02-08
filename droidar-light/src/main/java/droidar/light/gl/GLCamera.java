package droidar.light.gl;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.util.Calculus;
import droidar.light.util.Vec;

/**
 * This is the virtual camera needed to display a virtual world. The 3 important
 * properties you might want to change manually are its position, rotation and
 * offset.
 * 
 * @author Spobo
 * 
 */
public class GLCamera implements Renderable, HasPosition, HasRotation {

	private Vec myPosition = new Vec(0, 0, 0);

	/**
	 * y is always the angle from floor to top (rotation around green achsis
	 * counterclockwise) and x always the clockwise rotation (like when you lean
	 * to the side on a motorcycle) angle of the camera.
	 * 
	 * to move from the green to the red axis (clockwise) you would have to add
	 * 90 degree.
	 */
	private Vec myRotationVec = new Vec(0, 0, 0);

	private float[] rotationMatrix = Calculus.createIdentityMatrix();
	private final Object rotMatrLock = new Object();

	public GLCamera() {
	}
	@Override
	public Vec getRotation() {
		return myRotationVec;
	}

	@Override
	@Deprecated
	public void setRotation(Vec rotation) {
		if (myRotationVec == null) {
			myRotationVec = rotation;
		} else {
			myRotationVec.setToVec(rotation);
		}
	}

	/**
	 * This method will be called by the virtual world to load the camera
	 * parameters like the position and the rotation
	 */
	@Override
	public synchronized void render(GL10 gl) {

		synchronized (rotMatrLock) {
			// load rotation matrix:
			gl.glMultMatrixf(rotationMatrix, 0);
		}

		// rotate Camera TODO use for manual rotation:
		glLoadRotation(gl, myRotationVec);

		// set the point where to rotate around
		glLoadPosition(gl, myPosition);
	}

	public void setRotationMatrix(float[] rotMatrix) {
		synchronized (rotMatrLock) {
			rotationMatrix = rotMatrix;
		}
	}

	private void glLoadPosition(GL10 gl, Vec vec) {
		if (vec != null) {
			// if you want to set the center to 0 0 5 you have to move the
			// camera -5 units OUT of the screen
			gl.glTranslatef(-vec.x, -vec.y, -vec.z);
		}
	}

	private void glLoadRotation(GL10 gl, Vec vec) {
		/*
		 * a very important point is that its something completely different
		 * when you change the rotation order to x y z ! the order y x z is
		 * needed to use extract the angles from the rotation matrix with:
		 * 
		 * SensorManager.getOrientation(rotationMatrix, anglesInRadians);
		 * 
		 * so remember this oder when doing own rotations.
		 * 
		 * y is always the angle from floor to top and x always the clockwise
		 * rotation (like when you lean to the side on a motorcycle) angle of
		 * the camera.
		 */
		if (vec != null) {
			gl.glRotatef(vec.y, 0, 1, 0);
			gl.glRotatef(vec.x, 1, 0, 0);
			gl.glRotatef(vec.z, 0, 0, 1);
		}
	}

	/**
	 * @return the position in the virtual world. This vec could be used as the
	 *         users postion e.g. <br>
	 * <br>
	 *         x positive means east of zero pos (latitude direction) <br>
	 *         y positive means north of zero pos (longitude direction) <br>
	 *         z the height of the camera
	 */
	@Override
	public Vec getPosition() {
		return myPosition;
	}

	@Override
	public void setPosition(Vec position) {
		if (myPosition == null) {
			myPosition = position;
		} else {
			myPosition.setToVec(position);
		}
	}

}
