/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.decoder;

import meteordevelopment.meteorclient.utils.notebot.song.Song;

import java.io.File;

public abstract class SongDecoder {
    // COMPAT pure-host: no Notebot module. Decoders parse only; song adaptation
    // to Notebot settings (SongDecoders.fixSong) is skipped.

    /**
     * Parse file to a {@link Song} object
     *
     * @param file Song file
     * @return A {@link Song} object
     */
    public abstract Song parse(File file) throws Exception;
}
