package io.github.emergentorganization.emergent2dcore.systems;

import com.artemis.BaseSystem;
import io.github.emergentorganization.cellrpg.core.Tags;
import io.github.emergentorganization.cellrpg.events.EntityEvent;
import io.github.emergentorganization.cellrpg.events.GameEvent;
import io.github.emergentorganization.cellrpg.managers.EventManager;
import io.github.emergentorganization.cellrpg.systems.CASystems.CAs.CA;
import io.github.emergentorganization.emergent2dcore.events.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;

/**
 * System which keeps track of the game's current "feel" to tailor musical or other aesthetic choices.
 */
public class MoodSystem extends BaseSystem {
    private final Logger logger = LogManager.getLogger(getClass());

    public int intensity = 0;  // how fast-paced and action packed the current moment is.
    public static final int MAX_INTENSITY = 10000;
    // min: 0, max: 1000  (NOTE: max is not enforced, just assumed. going a little over shouldn't break anything.)
    // intensity should be boosted by things like explosions and spawning enemies, intensity decreases over time.

    // map of intensity effect of various events
    private static final EnumMap<GameEvent, Integer> EVENT_INTENSITY_MAP = new EnumMap<GameEvent, Integer>(GameEvent.class);
    static {
        EVENT_INTENSITY_MAP.put(GameEvent.PLAYER_SHOOT, 50);
        EVENT_INTENSITY_MAP.put(GameEvent.PLAYER_HIT, 300);
        EVENT_INTENSITY_MAP.put(GameEvent.VYROID_KILL_GENETIC, 100);
        EVENT_INTENSITY_MAP.put(GameEvent.VYROID_KILL_STD, 100);
        EVENT_INTENSITY_MAP.put(GameEvent.COLLISION_BULLET, 50);
    }

    public static final HashMap<String, Integer> CA_INTENSITY_MAP = new HashMap<String, Integer>();
    static {
        CA_INTENSITY_MAP.put(Tags.CA_VYROIDS_STD,     1);
        CA_INTENSITY_MAP.put(Tags.CA_VYROIDS_GENETIC, 2);
    }

    public MoodSystem(EventManager eventManager){
        // add listener for event effects
        eventManager.addListener(new EventListener() {
            @Override
            public void notify(EntityEvent event) {
                if (EVENT_INTENSITY_MAP.get(event.event) != null) {
                    intensity += EVENT_INTENSITY_MAP.get(event.event);
                }
            }
        });
    }

    @Override
    public void processSystem() {
        logger.trace("mood intensity:" + intensity);
        if (intensity < 1){
            intensity = 0;
        } else if (intensity > MAX_INTENSITY){
            intensity = MAX_INTENSITY;
            intensityDecay();
        } else {
            intensityDecay();
        }

        // NOTE: could also add aspect and MoodEffect Component which could be handled here
        //       so that the presence of certain entities might influence the mood.
    }

    public int scoreIntensityLevelOutOf(final int subdivisions){
        // returns current intensity level scored out of subdivisions
        // lowest: 1, highest: subdivisions
        int res = (short) Math.round((float)intensity/(float)MAX_INTENSITY * (float)subdivisions);
        if (res > subdivisions){
            res = subdivisions;
        }
        logger.debug("intensity level " + intensity + " scored as " + res + "/" + subdivisions);
        return res;
    }

    private void intensityDecay(){
        int minDecay = MAX_INTENSITY/5000;
        int relativeDecay = intensity/50;
        intensity -= Math.max(minDecay, relativeDecay);
    }
}
