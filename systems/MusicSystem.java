package io.github.emergentorganization.emergent2dcore.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import io.github.emergentorganization.cellrpg.core.SoundEffect;
import io.github.emergentorganization.cellrpg.managers.AssetManager;
import io.github.emergentorganization.cellrpg.tools.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * System for handling music based on game events.
 * uses Gdx.audio.newSound() instead of newMusic() to avoid streaming overhead but sound files must be < 1MB
 *
 * ported from BgSoundController:
 * https://github.com/EmergentOrganization/cell-rpg/blob/audioLayers/core/src/com/emergentorganization/cellrpg/sound/BgSoundController.java)
 */
public class MusicSystem extends BaseSystem {
    public static long LOOP_DURATION = 30 * 1000; // loops must be this length!

    private final Logger logger = LogManager.getLogger(getClass());
    private MoodSystem moodSystem;
    private AssetManager assetManager;

    private final ArrayList<FileHandle> fileHandles = new ArrayList<FileHandle>();  // array of fileHandles for loops
    private Sound[] constantLoops;  // loops which play constantly
    private ArrayList<Sound> unusedLoops = new ArrayList<Sound>();
    private ArrayList<Sound> currentLoops = new ArrayList<Sound>();  // currently playing loops
    private ArrayList<Sound> loopsToRemove = new ArrayList<Sound>(); // loops queued for removal next round
    private long lastLoopTime;  // last time we looped around

    private boolean loaded = false;
    private boolean prepped = false;  // flag used to track if next set of loops has been queued yet
    private boolean scheduled = false;

    public MusicSystem(AssetManager assetManager) {
        logger.trace("MusicSystem init");

        String loopDir = Resources.DIR_SOUNDS + "music/arcade_30s_loops";
        FileHandle dirs = Gdx.files.getFileHandle(loopDir, Files.FileType.Internal);
        for (FileHandle dir : dirs.list()) {
            fileHandles.add(dir);
            assetManager.gdxAssetManager.load(dir.path(), Sound.class);
        }

        logger.info("music loops loading from " + loopDir);
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
        } else if (!loaded && deltaTime > 2*1000){  // hopefully this is about enough time for loops to load
            ArrayList<FileHandle> delQueue = new ArrayList<FileHandle>();  // delQueue to avoid concurrentMod
            for (FileHandle fileHandle : fileHandles) {
                if (assetManager.gdxAssetManager.isLoaded(fileHandle.path(), Sound.class)) {
                    logger.debug("sound @" + fileHandle.path() + " loaded");
                    unusedLoops.add(assetManager.gdxAssetManager.get(fileHandle.path(), Sound.class));
                    delQueue.add(fileHandle);
                } else {
                    logger.trace("loading " + fileHandle.path().split("/")[fileHandle.path().split("/").length-1]);
                    assetManager.gdxAssetManager.update();
                }
            }
            for (FileHandle fileHandle : delQueue) {  // process the delQueue
                fileHandles.remove(fileHandle);
            }

            if (fileHandles.size() == 0) { // if  all have loaded
                loaded = true;
                logger.info(unusedLoops.size() + " music loops loaded");
            }
        }
    }

    public void start(AssetManager assetManager) {
        constantLoops = new Sound[2];
        constantLoops[0] = assetManager.getSoundEffects().get(SoundEffect.MUSIC_LOOP_PAD);
        constantLoops[1] = assetManager.getSoundEffects().get(SoundEffect.MUSIC_LOOP_KEYS);
        constantLoops[0].setLooping(constantLoops[0].play(), false);
        constantLoops[1].setLooping(constantLoops[1].play(), false);
        lastLoopTime = System.currentTimeMillis();
    }

    /**
     * Stops the current song and future iterations
     */
    public void stop() {
        for (Sound currentLoop : currentLoops) {
            currentLoop.stop();
            currentLoop.dispose();
            currentLoops.remove(currentLoop);
        }
    }

    private void scheduleNextLoop() {
        // schedules a new loop play
        logger.debug("scheduling next music loop");
        Timer time = new Timer();
        time.schedule(new ReLoop(), getTimeOfNextMeasure());
        logger.debug("schedule in " + getTimeOfNextMeasure());
        scheduled = true;
    }

    class ReLoop extends TimerTask {
        // used to manually loop the constantLoop (so we can drop in triggered tracks at appropriate time)
        // NOTE: this will break if deltaTime >= loop length (highly unlikely with long loops)
        public ReLoop() {

        }

        public void run() {
            // loop constant loops
            logger.debug("looping da loops");
            if (prepped) {
                try {
                    for (Sound constantLoop : constantLoops) {
                        if (constantLoop != null)
                            constantLoop.play();
                    }

                    updateCurrentLoops();

                    // play other loops
                    for (Sound loop : currentLoops) {
                        if (loop != null) {
                            loop.play();
                        }
                    }
                } finally {
                    lastLoopTime = System.currentTimeMillis();
                    prepped = false;
                    scheduled = false;
                }
            }
        }
    }

    private Sound getRandomSound() {
        int index = (int) Math.max(0, Math.floor((Math.random() * unusedLoops.size()) - 1));
        return unusedLoops.get(index);
    }

    private void updateCurrentLoops(){
        // executes the planned changes to currentLoops
        // remove loops queued for removal
        for (Sound loop : loopsToRemove){
            currentLoops.remove(loop);
            unusedLoops.add(loop);
        }
        // loops are not queued for addition, they are added (but not played)immediately
    }

    private void prepNextLoopRound(){
        // preps next round of loops
        logger.debug("prepping next round of loops");
        // number of loops desired:
        int numberOfLoops = moodSystem.scoreIntensityLevelOutOf((short) (currentLoops.size() + unusedLoops.size()));
        numberOfLoops += 1; // we want 1 optional loop at minimum
        logger.debug("current # loops:" + currentLoops.size());
        logger.debug("desired # loops:" + numberOfLoops);

        // swap out a loops to add random variation
        if (currentLoops.size() > 0 && unusedLoops.size() > 0) {
            int randomCurrentLoopIndex = (int) Math.max(0, Math.floor((Math.random() * currentLoops.size()) - 1));
            loopsToRemove.add(currentLoops.get(randomCurrentLoopIndex));
            int randomUnusedLoopIndex = (int) Math.max(0, Math.floor((Math.random() * unusedLoops.size()) - 1));
            currentLoops.add(unusedLoops.get(randomUnusedLoopIndex));
        }

        while (numberOfLoops < currentLoops.size() - loopsToRemove.size()
                && loopsToRemove.size() != currentLoops.size()){
            // queue loops for removal
            loopsToRemove.add(currentLoops.get(loopsToRemove.size()));
        }

        while(numberOfLoops > currentLoops.size() && unusedLoops.size() > 0){
            // add loops
            Sound newSound = getRandomSound();
            currentLoops.add(newSound);
            unusedLoops.remove(newSound);
        }
        prepped = true;
    }

    /**
     * Disposes at the end of the CellRPG lifecycle. Do not dispose on scene change -- see {@link MusicSystem#stop}
     */
    public void dispose() {
        // set these to try to help other threads stop:
        scheduled = true;
        prepped = false;
        loaded = false;

        for (Sound currentLoop : currentLoops) {
            currentLoop.dispose();
        }
        for (Sound unusedLoop : unusedLoops){
            unusedLoop.dispose();
        }
        for (Sound constantLoop : constantLoops) {
            if (constantLoop != null)
                constantLoop.dispose();
        }

        for (Sound loop : loopsToRemove){
            loop.dispose();
        }
    }

    private long getTimeOfNextMeasure(){
        return (lastLoopTime + LOOP_DURATION) - System.currentTimeMillis();
    }
}
