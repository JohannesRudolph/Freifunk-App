package droidar.light.util;

public class Calculus {
	public static float[] createIdentityMatrix() {
		float[] result = new float[16];
		result[0] = 1;
		result[5] = 1;
		result[10] = 1;
		result[15] = 1;
		return result;
	}
}
