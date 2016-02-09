package io.github.emergentorganization.emergent2dcore.systems;

import com.artemis.BaseSystem;

/**
 * System which keeps track of the game's current "feel" to tailor musical or other aesthetic choices.
 */
public class MoodSystem extends BaseSystem {
    public short intensity = 0;  // how fast-paced and action packed the current moment is.
    private final short MAX_INTENSITY = 1000;
    // min: 0, max: 1000
    // intensity should be boosted by things like explosions and spawning enemies, intensity decreases over time.

    // TODO: add listeners for mood-boosting events

    @Override
    public void processSystem() {
        if (intensity < 1){
            intensity = 0;
        } else {
            intensityDecay();
        }
    }   

    public int scoreIntensityLevelOutOf(final short subdivisions){
        // returns current intensity level scored out of subdivisions
        // lowest: 1, highest: subdivisions
        return (short) Math.round((float)intensity/MAX_INTENSITY * subdivisions);
    }

    private void intensityDecay(){
        short decay = 1;
        intensity -= decay;
    }
}
