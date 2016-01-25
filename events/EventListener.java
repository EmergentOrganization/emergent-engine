package io.github.emergentorganization.emergent2dcore.events;


import  io.github.emergentorganization.cellrpg.events.GameEvent;

public interface EventListener {

    void notify(GameEvent event);

}
