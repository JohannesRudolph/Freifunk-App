package droidar.light.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.hardware.SensorManager;

public class GLUtilityClass {

	/**
	 * TODO not efficient to create a new buffer object every time, pass an old
	 * one if available and use it instead?
	 * 
	 * @param source
	 * @return
	 */
	public static FloatBuffer createAndInitFloatBuffer(float[] source) {
		if (source == null)
			return null;
		/*
		 * a float is 4 bytes, therefore the number of elements in the array has
		 * to be multiplied with 4:
		 */
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(source.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		FloatBuffer targetBuffer = byteBuffer.asFloatBuffer();
		targetBuffer.put(source);
		targetBuffer.position(0);
		return targetBuffer;
	}
}
