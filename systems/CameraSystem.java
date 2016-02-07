package io.github.emergentorganization.emergent2dcore.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Profile;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import io.github.emergentorganization.cellrpg.tools.GameSettings;
import io.github.emergentorganization.cellrpg.tools.profiling.EmergentProfiler;
import io.github.emergentorganization.emergent2dcore.components.Bounds;
import io.github.emergentorganization.emergent2dcore.components.CameraFollow;
import io.github.emergentorganization.emergent2dcore.components.Position;
import io.github.emergentorganization.cellrpg.core.entityfactory.EntityFactory;
import io.github.emergentorganization.emergent2dcore.components.Velocity;
import io.github.emergentorganization.emergent2dcore.events.EventListener;
import io.github.emergentorganization.cellrpg.events.GameEvent;
import io.github.emergentorganization.cellrpg.managers.EventManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Wire
@Profile(using=EmergentProfiler.class, enabled=true)
public class CameraSystem extends IteratingSystem {
    private final Logger logger = LogManager.getLogger(getClass());

    private ComponentMapper<Position> pm;
    private ComponentMapper<Bounds> bm;
    private ComponentMapper<Velocity> velocity_m;
    private EventManager eventMan;

    private OrthographicCamera gameCamera;
    private boolean shouldFollow = true;
    private long lastUpdate;

    public CameraSystem() {
        super(Aspect.all(CameraFollow.class, Position.class, Bounds.class, Velocity.class));
        gameCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        gameCamera.zoom = EntityFactory.SCALE_WORLD_TO_BOX;
        gameCamera.lookAt(0, 0, 0);
        gameCamera.update();
    }

    @Override
    public void initialize() {
        super.initialize();
        eventMan.addListener(new EventListener() {
            @Override
            public void notify(GameEvent event) {
                switch (event) {
                    case PLAYER_HIT:
                        camShake();
                        break;
                }
            }
        });
        lastUpdate = System.currentTimeMillis();
    }

    private void camShake() {
        logger.info("camShake!");
        final float offset = .5f;  // TODO: randomize directions
        gameCamera.translate(offset, offset);
//        shouldFollow = !shouldFollow;  // this was just temporary to test that it works
    }

    public Camera getGameCamera() {
        return gameCamera;
    }

    public void setCamFollow(boolean enabled) {
        shouldFollow = enabled;
    }

    private void camFollow(int followEntity) {
        if (shouldFollow) {
            Position pc = pm.get(followEntity);
            Bounds b = bm.get(followEntity);
            Velocity velocity = velocity_m.get(followEntity);

            final long deltaTime = System.currentTimeMillis() - lastUpdate;
            lastUpdate = System.currentTimeMillis();

            // get camera settings:
            Preferences prefs = GameSettings.getPreferences();
            final float EDGE_MARGIN = prefs.getFloat(GameSettings.KEY_CAM_EDGE_MARGIN, 10)
                    *EntityFactory.SCALE_WORLD_TO_BOX;
            final float CLOSE_ENOUGH = prefs.getFloat(GameSettings.KEY_CAM_NEARNESS_CUTOFF, 4)
                    *EntityFactory.SCALE_WORLD_TO_BOX;
            final float CAMERA_LEAD = prefs.getFloat(GameSettings.KEY_CAM_LEAD, 20)
                    *EntityFactory.SCALE_WORLD_TO_BOX;

            // compute camera positioning:
            final float MAX_OFFSET = Math.min(gameCamera.viewportWidth, gameCamera.viewportHeight)/2-EDGE_MARGIN;  // max player-camera dist
            final float PROPORTIONAL_GAIN = deltaTime * velocity.velocity.len() / MAX_OFFSET;

            Vector2 pos = pc.getCenter(b,0);
            Vector2 cameraLoc = new Vector2(gameCamera.position.x, gameCamera.position.y);

            Vector2 offset = new Vector2(pos);
            offset.sub(gameCamera.position.x, gameCamera.position.y);

            offset.add(velocity.velocity.nor().scl(CAMERA_LEAD));

            if (Math.abs(offset.x) > CLOSE_ENOUGH || Math.abs(offset.y) > CLOSE_ENOUGH) {
                cameraLoc.add(offset.scl(PROPORTIONAL_GAIN));
                gameCamera.position.set(cameraLoc, 0);
//                logger.info("new camera pos:" + cameraLoc);
            }
        }
    }

    @Override
    protected void process(int entityId) {
        camFollow(entityId);
    }

    @Override
    protected void end() {
        gameCamera.update();
    }
}
