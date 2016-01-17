package io.github.emergentorganization.engine.events;


import  io.github.emergentorganization.cellrpg.events.GameEvent;

public interface EventListener {

    void notify(GameEvent event);

}
