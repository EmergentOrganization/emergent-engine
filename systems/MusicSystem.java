package io.github.emergentorganization.emergent2dcore.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.audio.Sound;
import io.github.emergentorganization.cellrpg.core.SoundEffect;
import io.github.emergentorganization.cellrpg.managers.AssetManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * System for handling music based on game events.
 *
 * ported from BgSoundController:
 * https://github.com/EmergentOrganization/cell-rpg/blob/audioLayers/core/src/com/emergentorganization/cellrpg/sound/BgSoundController.java)
 */
public class MusicSystem extends BaseSystem {
    public static long LOOP_DURATION = 30 * 1000; // loops must be this length!
    private final Logger logger = LogManager.getLogger(getClass());
    private Sound[] loops;
    private Sound lastLoopHandle;
    private Sound[] constantLoops;
    private Sound currentLoop;
    private boolean prepped = false;  // flag used to track if next set of loops has been queued yet
    private boolean scheduled = false;
    private Map<SoundEffect, Sound> soundEffects;
    private long lastLoopTime;  // last time we looped around

    public MusicSystem(AssetManager assetManager) {
        soundEffects = assetManager.getSoundEffects();
        logger.info("MusicSystem init");

//        FileHandle dir = Gdx.files.getFileHandle("sounds/arcade_30s_loops", Files.FileType.Internal);
//        final FileHandle[] fileHandles = dir.list();
//        loops = new Sound[fileHandles.length];
//        for (FileHandle fileHandle : fileHandles) {
//            //manager.load(fileHandle.path(), Sound.class);
//        }
//
//        for (int i = 0; i < loops.length; i++) {
//            FileHandle fileHandle = fileHandles[i];
//            //loops[i] = manager.get(fileHandle.path(), Sound.class);
//        }

        start(assetManager);
    }

    @Override
    public void processSystem(){
        long deltaTime = System.currentTimeMillis() - lastLoopTime;
        if (!scheduled && deltaTime > 25*1000){
            // almost time to loop back around, schedule the reloop
            scheduleNextLoop();
        } else if (!prepped && deltaTime > 15*1000){
            // halfway through the loop, prep next loop(s)
            prepNextLoopRound();
        } else {

        }
    }

    public void start(AssetManager assetManager) {
        constantLoops = new Sound[2];
        constantLoops[0] = soundEffects.get(SoundEffect.MUSIC_LOOP_PAD);
        constantLoops[1] = soundEffects.get(SoundEffect.MUSIC_LOOP_KEYS);
        constantLoops[0].setLooping(constantLoops[0].play(), true);  // setLooping doesn't seem to work here...
        constantLoops[1].setLooping(constantLoops[1].play(), true);
        lastLoopTime = System.currentTimeMillis();
    }

    /**
     * Stops the current song and future iterations
     */
    public void stop() {
        currentLoop.stop();
        currentLoop.dispose();
        currentLoop = null;
    }

    public void next() {
        if (currentLoop != null) {
            currentLoop.stop();
            currentLoop.dispose();
        }
        currentLoop = getRandomSound();
        currentLoop.play();
    }

    private void scheduleNextLoop() {
        // schedules a new loop play
        logger.info("scheduling next loop");
        Timer time = new Timer();
        time.schedule(new ReLoop(), lastLoopTime + LOOP_DURATION*1000);
        scheduled = true;
    }

    class ReLoop extends TimerTask {
        // used to manually loop the constantLoop (so we can drop in triggered tracks at appropriate time)
        // NOTE: this will break if deltaTime >= loop length (highly unlikely with long loops)
        public ReLoop() {

        }

        public void run() {
            // loop constant loops
            logger.info("looping da loops");
            for (Sound constantLoop : constantLoops) {
                constantLoop.play();
            }

            // TODO: remove loops queued for removal
            // TODO: add loops queued for addition

            // play other loops
            for (Sound loop : loops){
                if (loop != null) {
                    // use Gdx.audio.newSound() instead of newMusic()? but files must be < 1MB
                    logger.info("playing new loop");
                }
            }

            lastLoopTime = System.currentTimeMillis();
            prepped = false;
            scheduled = false;
        }
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

    private void prepNextLoopRound(){
        // preps next round of loops
        // TODO: load new loops
        // TODO: queue loops for removal (if desired)
        prepped = true;
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
