package droidar.light.gl.scenegraph;

import droidar.light.gl.Color;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.util.Vec;

public class Shape extends MeshComponent {

	private ArrayList<Vec> myShapeArray;
	protected RenderData myRenderData;
	private boolean singeSide = false;


	public ArrayList<Vec> getMyShapeArray() {
		if (myShapeArray == null)
			myShapeArray = new ArrayList<Vec>();
		return myShapeArray;
	}

	@Override
	public void draw(GL10 gl) {
		if (myRenderData != null) {
			if (singeSide) {
				// which is the front? the one which is drawn counter clockwise
				gl.glFrontFace(GL10.GL_CCW);
				// enable the differentiation of which side may be visible
				gl.glEnable(GL10.GL_CULL_FACE);
				// which one should NOT be drawn
				gl.glCullFace(GL10.GL_BACK);
				gl.glLightModelf(GL10.GL_LIGHT_MODEL_TWO_SIDE, 0);
				myRenderData.draw(gl);

				// Disable face culling.
				gl.glDisable(GL10.GL_CULL_FACE);
			} else {
				/*
				 * The GL_LIGHT_MODEL_TWO_SIDE can be used to use the same
				 * normal vector and light for both sides of the mesh
				 */
				gl.glLightModelf(GL10.GL_LIGHT_MODEL_TWO_SIDE, 1);
				myRenderData.draw(gl);
			}
		}
	}

	@Override
	public String toString() {
		return "Shape " + super.toString();
	}

}
