package droidar.light.sensors;

public interface RotationMatrixListener {
	/**
	 * Called when the rotation matrix changed
	 * @param rotMatrix 4x4 rotation matrix, remapped to current screen orientation
     */
	void onRotationMatrixChanged(float[] rotMatrix);
}