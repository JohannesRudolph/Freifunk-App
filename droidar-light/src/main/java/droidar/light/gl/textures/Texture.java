package droidar.light.gl.textures;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;

public class Texture {

    private static final String LOG_TAG = Texture.class.getSimpleName();

    private Bitmap myImage;
    private String myName;
    private ArrayList<TexturedRenderData> myList;

    public Texture(TexturedRenderData target, Bitmap textureImage, String textureName) {
        myList = new ArrayList<TexturedRenderData>();
        myList.add(target);
        myImage = textureImage;
        myName = textureName;
    }

    public void idArrived(int id) {
        Log.d(LOG_TAG, String.format("id=%d arrived for %s(%d items use this texture)", id, myName, myList.size()));
        for (int i = 0; i < myList.size(); i++) {
            Log.d(LOG_TAG, "    -> Now setting id for: " + myList.get(i));
            myList.get(i).myTextureId = id;
        }
    }

    public void recycleImage() {
        if (TextureManager.recycleBitmapsToFreeMemory)
            myImage.recycle();
    }

    public Bitmap getImage() {
        return myImage;
    }

    public String getName() {
        return myName;
    }

    public void addRenderData(TexturedRenderData target) {
        if (!myList.contains(target)) {
            myList.add(target);
            checkIfTextureIdAlreadyAvailableFor(target);
        }
    }

    private void checkIfTextureIdAlreadyAvailableFor(TexturedRenderData target) {
        if (myList.get(0) != null) {
            if (myList.get(0).myTextureId != TexturedRenderData.NO_ID_SET) {
                Log.d(LOG_TAG, String.format("id=%d already loaded for %s(%d items use this texture)", myList.get(0).myTextureId, myName, myList.size()));
                target.myTextureId = myList.get(0).myTextureId;
            }
        }
    }
}
