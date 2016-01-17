package io.github.emergentorganization.engine.scenes;

import io.github.emergentorganization.engine.EmergentEngine;
import io.github.emergentorganization.engine.scenes.game.Arcade;
import io.github.emergentorganization.engine.scenes.game.LifeGeneLab;
import io.github.emergentorganization.engine.scenes.game.Story;
import io.github.emergentorganization.engine.scenes.menu.MainMenu;
import io.github.emergentorganization.engine.tools.mapeditor.MapEditor;


public enum Scene {

    MAIN_MENU {
        @Override
        public BaseScene getScene(EmergentEngine pt) {
            return new MainMenu(pt);
        }
    },
    ARCADE {
        @Override
        public BaseScene getScene(EmergentEngine pt) {
            return new Arcade(pt);
        }
    },
    LAB {
        @Override
        public BaseScene getScene(EmergentEngine pt) {
            return new LifeGeneLab(pt);
        }
    },
    EDITOR {
        @Override
        public BaseScene getScene(EmergentEngine pt) {
            return new MapEditor(pt);
        }
    },
    STORY {
        @Override
        public BaseScene getScene(EmergentEngine pt) {
            return new Story(pt);
        }
    };

    public abstract BaseScene getScene(EmergentEngine pt);
}
