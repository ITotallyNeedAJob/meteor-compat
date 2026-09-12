/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

public class CustomPlayerInput extends Input {
    @Override
    public void tick() {
        float f = this.playerInput.comp_3159() == this.playerInput.comp_3160() ? 0.0F : (this.playerInput.comp_3159() ? 1.0F : -1.0F);
        float g = this.playerInput.comp_3161() == this.playerInput.comp_3162() ? 0.0F : (this.playerInput.comp_3161() ? 1.0F : -1.0F);
        this.movementVector = new Vec2f(g, f).normalize();
    }

    public void stop() {
        this.playerInput = PlayerInput.DEFAULT;
    }

    public void forward(boolean bool) {
        this.playerInput = new PlayerInput(
            bool,
            this.playerInput.comp_3160(),
            this.playerInput.comp_3161(),
            this.playerInput.comp_3162(),
            this.playerInput.comp_3163(),
            this.playerInput.sneak(),
            this.playerInput.comp_3165()
        );
    }

    public void backward(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.comp_3159(),
            bool,
            this.playerInput.comp_3161(),
            this.playerInput.comp_3162(),
            this.playerInput.comp_3163(),
            this.playerInput.sneak(),
            this.playerInput.comp_3165()
        );
    }

    public void left(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.comp_3159(),
            this.playerInput.comp_3160(),
            bool,
            this.playerInput.comp_3162(),
            this.playerInput.comp_3163(),
            this.playerInput.sneak(),
            this.playerInput.comp_3165()
        );
    }

    public void right(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.comp_3159(),
            this.playerInput.comp_3160(),
            this.playerInput.comp_3161(),
            bool,
            this.playerInput.comp_3163(),
            this.playerInput.sneak(),
            this.playerInput.comp_3165()
        );
    }

    public void jump(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.comp_3159(),
            this.playerInput.comp_3160(),
            this.playerInput.comp_3161(),
            this.playerInput.comp_3162(),
            bool,
            this.playerInput.sneak(),
            this.playerInput.comp_3165()
        );
    }

    public void sneak(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.comp_3159(),
            this.playerInput.comp_3160(),
            this.playerInput.comp_3161(),
            this.playerInput.comp_3162(),
            this.playerInput.comp_3163(),
            bool,
            this.playerInput.comp_3165()
        );
    }

    public void sprint(boolean bool) {
        this.playerInput = new PlayerInput(
            this.playerInput.comp_3159(),
            this.playerInput.comp_3160(),
            this.playerInput.comp_3161(),
            this.playerInput.comp_3162(),
            this.playerInput.comp_3163(),
            this.playerInput.sneak(),
            bool
        );
    }
}
