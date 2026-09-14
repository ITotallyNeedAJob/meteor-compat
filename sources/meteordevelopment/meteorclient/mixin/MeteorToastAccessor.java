/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MeteorToast.class)
public interface MeteorToastAccessor {
    @Accessor("title")
    Text getTitle();

    @Accessor("text")
    Text getText();
}
