/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.text.ClickEvent;

/**
 * This class does nothing except ensure that {@link ClickEvent}'s containing Meteor Client commands can only be executed if they come from the client.
 * COMPAT Phase 0: GUI mixin hook (ScreenMixin) removed; the marker type is kept for ABI.
 */
public class MeteorClickEvent implements ClickEvent {
    public final String value;

    public MeteorClickEvent(String value) {
        this.value = value;
    }

    @Override
    public Action getAction() {
        return Action.RUN_COMMAND;
    }
}
