package droidar.light.gl.textures;


import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import droidar.light.gl.scenegraph.Shape;
import droidar.light.util.Vec;

public class TexturedShape extends Shape {

    /**
     * this values are corresponding to the shape edges
     */
    ArrayList<Vec> myTexturePositions = new ArrayList<Vec>();

    /**
     * Please read
     * {@link TextureManager#addTexture(TexturedRenderData, Bitmap, String)} for
     * information about the parameters
     *
     * @param textureName
     * @param texture
     */
    public TexturedShape(String textureName, Bitmap texture, TextureManager textureManager) {
        super();
        myRenderData = new TexturedRenderData();
		/*
		 * TODO redesign this so that the input texture is projected on the mesh
		 * correctly
		 */
        if (texture != null) {
            texture = textureManager.resizeBitmapIfNecessary(texture);
            textureManager.addTexture((TexturedRenderData) myRenderData, texture, textureName);
        } else {
            Log.e("TexturedShape", "got null-bitmap! check bitmap creation process");
        }
    }

    public void add(Vec vec, int x, int y) {
        getMyShapeArray().add(vec);
        // z coordinate not needed for 2d textures:
        myTexturePositions.add(new Vec(x, y, 0));
        myRenderData.updateShape(getMyShapeArray());
        ((TexturedRenderData) myRenderData).updateTextureBuffer(myTexturePositions);
    }

}


