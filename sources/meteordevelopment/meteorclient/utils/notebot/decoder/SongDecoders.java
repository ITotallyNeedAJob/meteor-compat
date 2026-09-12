/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.decoder;

import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.meteorclient.utils.notebot.song.Song;
import net.minecraft.block.enums.NoteBlockInstrument;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SongDecoders {
    private static final Map<String, SongDecoder> decoders = new HashMap<>(); // file extension -> song decoder

    static {
        registerDecoder("nbs", new NBSSongDecoder());
        registerDecoder("txt", new TextSongDecoder());
        // TODO Maybe a midi decoder in the future
    }

    public static void registerDecoder(String extension, SongDecoder songDecoder) {
        decoders.put(extension, songDecoder);
    }

    public static SongDecoder getDecoder(File file) {
        return decoders.get(FilenameUtils.getExtension(file.getName()));
    }

    public static boolean hasDecoder(File file) {
        return decoders.containsKey(FilenameUtils.getExtension(file.getName()));
    }

    public static boolean hasDecoder(Path path) {
        return hasDecoder(path.toFile());
    }

    /**
     * Parse file to one of {@link SongDecoder}
     *
     * @param file A song file
     * @return A {@link Song} object
     */
    @NotNull
    public static Song parse(File file) throws Exception {
        if (!hasDecoder(file)) throw new IllegalStateException("Decoder for this file does not exists!");
        SongDecoder decoder = getDecoder(file);
        Song song = decoder.parse(file);

        fixSong(song);

        song.finishLoading();

        return song;
    }

    /**
     * COMPAT: Notebot module removed - song adaptation is a no-op.
     *
     * @param song A song
     */
    private static void fixSong(Song song) {
    }
}
