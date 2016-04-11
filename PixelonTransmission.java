package io.github.emergentorganization.emergent2dcore;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.GdxNativesLoader;
import io.github.emergentorganization.cellrpg.scenes.Scene;
import io.github.emergentorganization.cellrpg.scenes.SceneManager;
import io.github.emergentorganization.cellrpg.scenes.game.HUD.DebugDisplay;
import io.github.emergentorganization.cellrpg.scenes.game.menu.pause.GraphicsSettingsMenu;
import io.github.emergentorganization.cellrpg.tools.FileStructure;
import io.github.emergentorganization.cellrpg.tools.GameSettings;
import io.github.emergentorganization.cellrpg.tools.physics.BodyEditorLoader;
import com.kotcrab.vis.ui.VisUI;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


public class PixelonTransmission extends Game {
    public static final float PHYSICS_TIMESTEP = 1 / 45f;
    private static final String VERSION = "0.3";
    private static final String ATLAS_PATH = FileStructure.RESOURCE_DIR + "textures/TexturePack.atlas";
    private static final String COLLIDER_PATH = FileStructure.RESOURCE_DIR + "/data/colliderProject";

    static {
        GdxNativesLoader.load();
    }

    private final Logger logger;
    private AssetManager assetManager;
    private SceneManager sceneManager;
    private TextureAtlas textureAtlas;
    private FileStructure fileStructure;
    private Skin skin;
    private BodyEditorLoader bodyLoader;

    public PixelonTransmission() {
        String logFile = "log4j2.xml";
        System.setProperty("log4j.configurationFile", FileStructure.RESOURCE_DIR + logFile);
        logger = LogManager.getLogger(getClass());
    }

    @Override
    public void create() {

        // init graphics settings
        Preferences prefs = GameSettings.getPreferences();
        int w,h;
        try {
            w = prefs.getInteger(GameSettings.KEY_GRAPHICS_WIDTH, GraphicsSettingsMenu.getDefaultW());
            h = prefs.getInteger(GameSettings.KEY_GRAPHICS_HEIGHT, GraphicsSettingsMenu.getDefaultH());
        } catch(NumberFormatException ex){  // TODO: libgdx fix: LwjglPreferences should do this automatically, right?
            logger.error("corrupted screen size preferences. cannot parse integers", ex);
            w = GraphicsSettingsMenu.getDefaultW();
            h = GraphicsSettingsMenu.getDefaultH();
            prefs.putInteger(GameSettings.KEY_GRAPHICS_WIDTH, w);
            prefs.putInteger(GameSettings.KEY_GRAPHICS_HEIGHT, h);
        }
        boolean fs = prefs.getBoolean(GameSettings.KEY_GRAPHICS_FULLSCREEN, GraphicsSettingsMenu.FULLSCREEN_DEFAULT);
        logger.debug("Resizing: " + w + ", " + h + ". Fullscreen: " + fs);
        Gdx.graphics.setDisplayMode(w, h, fs);

        // init file structure
        this.fileStructure = new FileStructure();
        if (fileStructure.isJar()) {
            fileStructure.unpackAssets();
        }

        //version = getVersion();
        VisUI.load();

        assetManager = new AssetManager(new InternalFileHandleResolver());
        assetManager.load(ATLAS_PATH, TextureAtlas.class);
        loadSounds();
        loadMusic();
        assetManager.finishLoading();
        textureAtlas = assetManager.get(ATLAS_PATH, TextureAtlas.class);

        bodyLoader = new BodyEditorLoader(Gdx.files.internal(COLLIDER_PATH));

        skin = new Skin(
                Gdx.files.internal("resources/uiskin/skin.json"),
                new TextureAtlas(Gdx.files.internal("resources/uiskin/skin.atlas"))
        );

        sceneManager = new SceneManager(this);
        sceneManager.setScene(Scene.MAIN_MENU);

        logger.info("Game started");
    }

    public String loadVersion() {
        Properties props = new Properties();
        File propsFile = Gdx.files.internal(FileStructure.RESOURCE_DIR + "property.settings").file();
        try {
            FileReader reader = new FileReader(propsFile);
            props.load(reader);
            String major = props.getProperty("majorVersion");
            String minor = props.getProperty("minorVersion");
            String revision = props.getProperty("revision");
            reader.close();
            return major + "." + minor + "." + revision;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void loadMusic(){
        String prefix = FileStructure.RESOURCE_DIR + "sounds/music/";
        String ext = ".ogg";
        String[] musics = {
                "always_on/pad",
                "always_on/keys"
        };

        for (String sound : musics) {
            assetManager.load(prefix + sound + ext, Sound.class);
        }
    }

    private void loadSounds() {
        String prefix = FileStructure.RESOURCE_DIR + "sounds/";
        String ext = ".wav";
        String[] sounds = {
                "Hit",
                "PlayerHurt",
                "Explosion",
                "Select",
                "ShieldDown",
                "LaserShot",
                "ShootBlank",
                "UIBack",
                "UIBackLong",
                "UIConfirm"
        };

        for (String sound : sounds) {
            assetManager.load(prefix + sound + ext, Sound.class);
        }
    }

    @Override
    public void dispose() {
        assetManager.dispose();
        sceneManager.dispose();
        logger.info("Game shutdown");
    }

    public String getVersion() {
        return VERSION;
    }

    public AssetManager getGdxAssetManager() {
        return assetManager;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }

    public Skin getUISkin() {
        return skin;
    }

    public BodyEditorLoader getBodyLoader() {
        return bodyLoader;
    }

    public FileStructure getFileStructure() {
        return fileStructure;
    }
}
