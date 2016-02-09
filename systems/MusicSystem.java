package io.github.emergentorganization.emergent2dcore.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import io.github.emergentorganization.cellrpg.core.SoundEffect;
import io.github.emergentorganization.cellrpg.managers.AssetManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * System for handling music based on game events.
 *
 * ported from BgSoundController:
 * https://github.com/EmergentOrganization/cell-rpg/blob/audioLayers/core/src/com/emergentorganization/cellrpg/sound/BgSoundController.java)
 */
public class MusicSystem extends BaseSystem {
    public static float LOOP_DURATION = 30.0f; // loops must be this length!
    private final Logger logger = LogManager.getLogger(getClass());
    private Sound[] loops;
    private Sound lastLoopHandle;
    private Sound[] constantLoops;
    private Sound currentLoop;
    private boolean queued = false;  // flag used to track if next set of loops has been queued yet
    private float playTime = 0f; // in seconds
    private Map<SoundEffect, Sound> soundEffects;

    public MusicSystem(AssetManager assetManager) {
        soundEffects = assetManager.getSoundEffects();
        logger.info("MusicSystem init");
        start(assetManager);
    }

    @Override
    public void processSystem(){
        FileHandle dir = Gdx.files.getFileHandle("sounds/arcade_30s_loops", Files.FileType.Internal);
        final FileHandle[] fileHandles = dir.list();
        loops = new Sound[fileHandles.length];
        for (FileHandle fileHandle : fileHandles) {
            //manager.load(fileHandle.path(), Sound.class);
        }

        for (int i = 0; i < loops.length; i++) {
            FileHandle fileHandle = fileHandles[i];
            //loops[i] = manager.get(fileHandle.path(), Sound.class);
        }
    }

    public void start(AssetManager assetManager) {
        constantLoops = new Sound[2];
        constantLoops[0] = soundEffects.get(SoundEffect.MUSIC_LOOP_PAD);
        constantLoops[1] = soundEffects.get(SoundEffect.MUSIC_LOOP_KEYS);
        constantLoops[0].setLooping(constantLoops[0].play(), true);
        constantLoops[1].setLooping(constantLoops[1].play(), true);
    }

    /**
     * Stops the current song and future iterations
     */
    public void stop() {
        currentLoop.stop();
        currentLoop.dispose();
        currentLoop = null;
    }

    public void startRandomLoops(){
        // queues random loop to be added at next interval
        // TODO:
    }

    public void step(float deltaTime) {
        for (Sound constantLoop : constantLoops) {
            if (constantLoop != null) {
                playTime += deltaTime;
                // manually loop the constantLoop (so we can drop in triggered tracks at appropriate time
                // NOTE: this will break if deltaTime >= loop length (highly unlikely with long loops)
                //if( ! constantLoop.isPlaying()) {  // if has stopped playing
                if (!queued) {
                    // use Gdx.audio.newSound() instead of newMusic()? but files must be < 1MB
                    // TODO: load next loops
                    // TODO: set timed thread to start playing next loops @ end of these
                    float triggerTime = playTime % LOOP_DURATION;
                    System.out.println(triggerTime);
                    if (triggerTime <= 0.15f) { // Must be time to introduce a new loop layer?
                        //queued = true; // ??
                    }
                }
            }
        }
    }

    public void next() {
        if (currentLoop != null) {
            currentLoop.stop();
            currentLoop.dispose();
        }
        currentLoop = getRandomSound();
        currentLoop.play();
    }

    private Sound getRandomSound() {
        int index = (int) Math.max(0, Math.floor((Math.random() * loops.length) - 1));

        // Don't play the same song twice in a row
        if (lastLoopHandle == loops[index])
            index = (int) Math.max(0, Math.floor((Math.random() * loops.length) - 1));
        else
            lastLoopHandle = loops[index];

        return loops[index];
    }

    /**
     * Disposes at the end of the CellRPG lifecycle. Do not dispose on scene change -- see {@link MusicSystem#stop}
     */
    public void dispose() {
        if (currentLoop != null)
            currentLoop.dispose();
        for (Sound constantLoop : constantLoops) {
            if (constantLoop != null)
                constantLoop.dispose();
        }
    }
}
