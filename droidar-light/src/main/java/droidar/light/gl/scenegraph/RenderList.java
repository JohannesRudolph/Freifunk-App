package droidar.light.gl.scenegraph;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.util.EfficientList;
import droidar.light.util.Vec;
import droidar.light.world.Entity;

public class RenderList implements Entity {

	private static final String LOG_TAG = "RenderList";
	EfficientList<Entity> myItems = new EfficientList<Entity>();

	@Override
	public void render(GL10 gl) {
		for (int i = 0; i < myItems.myLength; i++) {
			myItems.get(i).render(gl);
		}
	}

	@Override
	public boolean update(float timeDelta) {

		for (int i = 0; i < myItems.myLength; i++) {
			if (!myItems.get(i).update(timeDelta)) {
				Log.d(LOG_TAG, String.format("Item %s will now be removed from RenderList because it is finished (returned false on update())", myItems.get(i)));
				myItems.remove(myItems.get(i));
			}
		}
		if (myItems.myLength == 0)
			return false;
		return true;
	}

	public boolean add(Entity child) {
		if (child == this) {
			Log.e(LOG_TAG, "Not allowed to add object to itself!");
			return false;
		}

		return myItems.add(child);
	}

	public boolean insert(int pos, Entity item) {
		return myItems.insert(pos, item);
	}

	public boolean remove(Entity child) {
		return myItems.remove(child);
	}

	@Override
	public String toString() {
		return LOG_TAG + " (" + myItems.myLength + " items)";
	}

}