/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.decoder;

import meteordevelopment.meteorclient.utils.notebot.song.Song;

import java.io.File;

public abstract class SongDecoder {
    // COMPAT: Notebot module removed - subclasses no longer get a module reference.

    /**
     * Parse file to a {@link Song} object
     *
     * @param file Song file
     * @return A {@link Song} object
     */
    public abstract Song parse(File file) throws Exception;
}
