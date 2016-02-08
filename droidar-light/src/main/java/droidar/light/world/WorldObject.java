package droidar.light.world;

import droidar.light.world.Entity;

public class WorldObject {
    private Entity renderable;

    public WorldObject(Entity renderable) {
        this.renderable = renderable;
    }

    public void onAddedToWorld(World world) {
    }

    public void update(float timeDelta, World world) {
        renderable.update(timeDelta);
    }

    public Entity getRenderable() {
        return renderable;
    }
}
