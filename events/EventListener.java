package io.github.emergentorganization.emergent2dcore.events;

import io.github.emergentorganization.cellrpg.events.EntityEvent;

public interface EventListener {

    void notify(EntityEvent event);
    // notifies listening object of an event

}
