/*************************************************************************
 *  Compilation:  javac -classpath .:jl1.0.jar MP3.java         (OS X)
 *                javac -classpath .;jl1.0.jar MP3.java         (Windows)
 *  Execution:    java -classpath .:jl1.0.jar MP3 filename.mp3  (OS X / Linux)
 *                java -classpath .;jl1.0.jar MP3 filename.mp3  (Windows)
 *  
 *  Plays an MP3 file using the JLayer MP3 library.
 *
 *  Reference:  http://www.javazoom.net/javalayer/sources.html
 *
 *
 *  To execute, get the file jl1.0.jar from the website above or from
 *
 *      http://www.cs.princeton.edu/introcs/24inout/jl1.0.jar
 *
 *  and put it in your working directory with this file MP3.java.
 *
 *************************************************************************/

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javazoom.jl.player.Player;


public class MP3 {
    private String filename;
    private Player player;
    int shuffle_index;

    public MP3(String filename, int shuffle_index) {
        this.filename = filename;
        this.shuffle_index = shuffle_index;
    }

    public void close() { if (player != null) player.close(); }

    // play the MP3 file to the sound card
    public void play(final PlaylistGUI gui) {
        try {
            FileInputStream fis     = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            player = new Player(bis);
        } catch (Exception e) {
            System.out.println("Problem playing file " + filename);
            System.out.println(e);
        }

        // run in new thread to play in background
        new Thread() {
            public void run() {
                try {
                    player.play();
                } catch (Exception e) {
                    System.out.println(e);
                } finally {
                    // If song wasn't closed before finishing
                    if (player.isComplete()) {
                        if (gui.isRepeatSelected()) {
                            play(gui);
                        } else {
                            gui.closeSong(filename);

                            // If cont_play is checked, play and highlight next song
                            final int num_shuffle_songs = gui.numShuffleSongs() - 1;
                            if ((shuffle_index++ < num_shuffle_songs) &&
                                    gui.isContPlaySelected()) {
                                gui.setNextButtonEnabled(shuffle_index < num_shuffle_songs);
                                gui.playSong(gui.getSongPath(shuffle_index), shuffle_index);
                                gui.setShuffleSelect(shuffle_index);
                            }
                        }
                    }
                }
            }
        }.start();
    }
}