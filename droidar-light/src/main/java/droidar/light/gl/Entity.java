package droidar.light.world;

import droidar.light.gl.Renderable;
import droidar.light.gl.scenegraph.MeshComponent;
import droidar.light.gl.scenegraph.RenderList;
import droidar.light.gl.scenegraph.Shape;

/**
 * This is the basic interface for any object which hat to do with Rendering and
 * which also needs to be updated from time to time. <br>
 * <br>
 * 
 * The existing important subclasses are: <br>
 * 
 * - {@link RenderList}: It is a group of {@link Entity}s<br>
 * 
 * - {@link MeshComponent}: A basic {@link Shape} e.g. to draw OpenGL objects
 * 
 * @author Spobo
 * 
 */
public interface Entity extends Renderable, Updatable {


}
