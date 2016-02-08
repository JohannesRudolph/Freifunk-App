package droidar.light.gl.textures;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import java8.util.stream.StreamSupport;

public class TextureManager {
    private static final String LOG_TAG = "Texture Manager";
    private static final int INIT_TEXTURE_MAP_SIZE = 40;
    public static boolean recycleBitmapsToFreeMemory = false;

    private int textureArrayOffset;
    private int[] textureArray;
    private HashMap<String, Texture> textureMap;
    private ArrayList<Texture> newTexturesToLoad;

    public TextureManager() {
        resetState();
    }

    private void resetState() {
        textureArray = new int[INIT_TEXTURE_MAP_SIZE];
        textureArrayOffset = 0;
        textureMap = new HashMap<String, Texture>();

        newTexturesToLoad = new ArrayList<Texture>();
    }

    /**
     * @param target      The target mesh where the texture will be set to
     * @param bitmap      The bitmap that should be used as the texture
     * @param textureName An unique name for the texture. Textures with the same name
     *                    will have the same OpenGL textures!
     */
    public void addTexture(TexturedRenderData target, Bitmap bitmap, String textureName) {

        Texture t = loadTextureFromMap(textureName);

        if (t == null) {
            addTexture(new Texture(target, bitmap, textureName));
        } else {
            Log.d(LOG_TAG, "Texture for " + textureName
                    + " already added, so it will get the same texture id");
            t.addRenderData(target);
        }

    }

    private void addTexture(Texture t) {
        Log.d(LOG_TAG, "   > Texture for " + t.getName() + " not yet added, so it will get a new texture id");

        textureMap.put(t.getName(), t);


        newTexturesToLoad.add(t);
    }

    private Texture loadTextureFromMap(String textureName) {
        if (textureMap == null)
            return null;

        return textureMap.get(textureName);
    }

    public void updateTextures(GL10 gl) {
        if (newTexturesToLoad != null && newTexturesToLoad.size() > 0) {
            try {
                while (textureArray.length - textureArrayOffset < newTexturesToLoad
                        .size()) {
                    Log.d(LOG_TAG, "Resizing textureArray!");
                    textureArray = doubleTheArraySize(textureArray);
                }

                // generate and store id numbers in textureArray:
                gl.glGenTextures(newTexturesToLoad.size(), textureArray,
                        textureArrayOffset);
                int newtextureArrayOffset = newTexturesToLoad.size();

                for (int i = 0; i < newTexturesToLoad.size(); i++) {

                    Texture t = newTexturesToLoad.get(i);
                    int newTextureId = textureArray[textureArrayOffset + i];

                    t.idArrived(newTextureId);

                    gl.glBindTexture(GL10.GL_TEXTURE_2D, newTextureId);

                    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
                    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

                    gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

                    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, t.getImage(), 0);

                    int[] mCropWorkspace = new int[4];
                    mCropWorkspace[0] = 0;
                    mCropWorkspace[1] = t.getImage().getHeight();
                    mCropWorkspace[2] = t.getImage().getWidth();
                    mCropWorkspace[3] = -t.getImage().getHeight();

                    // TODO maybe not working on any phone because using GL11?
                    ((GL11) gl)
                            .glTexParameteriv(GL10.GL_TEXTURE_2D,
                                    GL11Ext.GL_TEXTURE_CROP_RECT_OES,
                                    mCropWorkspace, 0);

                    t.recycleImage();

                    int error = gl.glGetError();
                    if (error != GL10.GL_NO_ERROR) {
                        Log.e("SpriteMethodTest", "Texture Load GLError: "
                                + error);
                    }

                }
                textureArrayOffset = newtextureArrayOffset;
                newTexturesToLoad.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int[] doubleTheArraySize(int[] a) {
        int[] b = new int[a.length * 2];
        // copy old values:
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    /**
     * its important that the used textures have a size powered 2 (2,4,8,16,32..
     * x 2,4,8..) so resize the bitmap if it has not the correct size
     *
     * @param b
     * @return
     */
    public Bitmap resizeBitmapIfNecessary(Bitmap b) {
        int height = b.getHeight();
        int width = b.getWidth();
        int newHeight = getNextPowerOfTwoValue(height);
        int newWidth = getNextPowerOfTwoValue(width);
        if ((height != newHeight) || (width != newWidth)) {
            Log.v(LOG_TAG, "   > Need to resize bitmap: old height=" + height
                    + ", old width=" + width + ", new height=" + newHeight
                    + ", new width=" + newWidth);
            return resizeBitmap(b, newHeight, newWidth);
        }
        return b;
    }


    private static Bitmap resizeBitmap(Bitmap bitmap, float newHeight, float newWidth) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public static int getNextPowerOfTwoValue(double x) {
        /*
         * calc log2(x) (log2(x) can be calculated with log(x)/log(2)) and get
		 * the next bigger integer value. then calc 2^this value
		 */
        double x2 = Math.pow(2, Math.floor(Math.log(x) / Math.log(2)) + 1);
        if (x2 != x) {
            return (int) x2;
        }
        return (int) x;
    }


    public void reloadTexturesIfNeeded() {

        try {
            Collection<Texture> all = new ArrayList<>(textureMap.values());

            textureMap = new HashMap<>(all.size());

            Log.d(LOG_TAG, "Restoring " + all.size() + " textures");

            StreamSupport.stream(all).forEach((Texture t) -> addTexture(t));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while restoring textures");
            e.printStackTrace();
        }
    }


}

