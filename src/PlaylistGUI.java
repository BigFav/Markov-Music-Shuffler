import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


/**
 * Creates a GUI that takes in an exported iTunes playlist, number of
 * songs, and an optional genre to start the Markov chain with in order
 * to create a song shuffle based on the aforementioned Markov chain.
 * Allows users to play a song, or play multiple songs at the same time.
 *
 * @author Favian Contreras <fnc4@cornell.edu>
 */
@SuppressWarnings("serial")
final public class PlaylistGUI extends JFrame {
    private JList<String> myShuffle;
    private List<String> songPaths;
    private List<String> ogPlaylist;                // List of all songs in playlist
    private List<Integer> usedIndices;
    private Map<String, Integer> genres;
    private Map<String, Queue<MP3>> playingMP3s;    // Bootleg multimap
    private AtomicInteger multimap_size;
    private final JButton next_button = new JButton("Next");
    private final JButton prev_button = new JButton("Previous");
    private final JCheckBox repeat = new JCheckBox("Repeat");
    private final JCheckBox cont_play = new JCheckBox("Continuous Play");

    public PlaylistGUI() {
        songPaths = new ArrayList<String>();
        ogPlaylist = new ArrayList<String>();
        usedIndices = new ArrayList<Integer>();
        genres = new HashMap<String, Integer>();
        playingMP3s = Collections.synchronizedMap(new HashMap<String, Queue<MP3>>());
        multimap_size = new AtomicInteger();

        initComponents();
    }

    final private void initComponents() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
                            "Normal text file (*.txt)", "txt"));

        final JTextField listLength = new JTextField(5);
        final JTextField startGenre = new JTextField(10);
        final JTextField markovPathField = new JTextField(31);
        final JTextField playlistPathField = new JTextField(35);

        final JButton playlistBrowseButton = new JButton("Browse...");
        final JButton markovBrowseButton = new JButton("Browse...");
        final JButton shuffleButton = new JButton("Shuffle");
        final JButton playButton = new JButton("Play");
        final JButton blendButton = new JButton("Blend Play");
        final JButton stopButton = new JButton("Stop");
        final JButton stopAllButton = new JButton("Stop All");

        playlistBrowseButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final int returnVal = fc.showDialog(PlaylistGUI.this, "Select");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    playlistPathField.setText(fc.getSelectedFile().getPath());
                }
            }
        });

        markovBrowseButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final int returnVal = fc.showDialog(PlaylistGUI.this, "Select");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    markovPathField.setText(fc.getSelectedFile().getPath());
                }
            }   
        });

        shuffleButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                int num_songs;
                try {
                    num_songs = Integer.parseInt(listLength.getText());
                } catch (NumberFormatException j) {
                    String message = "Please insert a numerical value in the " +
                                     "\"number of songs\" text field.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "Invalid Number of Songs",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }

                synchronized (this) {
                    playButton.setEnabled(true);
                    blendButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    stopAllButton.setEnabled(true);
                    if (multimap_size.get() == 0) {
                        cont_play.setEnabled(true);
                        repeat.setEnabled(true);
                    } else {
                        cont_play.setEnabled(false);
                        cont_play.setSelected(false);
                        repeat.setEnabled(false);
                        repeat.setSelected(false);
                    }
                    prev_button.setEnabled(false);
                    next_button.setEnabled(false);
                    shuffleSongs(playlistPathField.getText(), startGenre.getText(),
                                 markovPathField.getText(), num_songs);                
                }
            }
        });

        playButton.setEnabled(false);
        playButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final int index = myShuffle.getSelectedIndex();
                if (index == -1) {
                    String message = "No song has been selected. " +
                                     "Please select one before playing.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "No Song Selected",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }

                closeSongs();
                cont_play.setEnabled(true);
                prev_button.setEnabled(index > 0);
                next_button.setEnabled(index < usedIndices.size() - 1);
                playSong(songPaths.get(usedIndices.get(index)), index);
            }
        });

        blendButton.setEnabled(false);
        blendButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final int index = myShuffle.getSelectedIndex();
                if (index == -1) {
                    String message = "No song has been selected. " +
                                     "Please select one before playing.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "No Song Selected",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }

                synchronized (this) {
                    if (multimap_size.get() > 0) {
                        repeat.setEnabled(false);
                        repeat.setSelected(false);
                        cont_play.setEnabled(false);
                        cont_play.setSelected(false);
                        prev_button.setEnabled(false);
                        next_button.setEnabled(false);
                    } else {
                        prev_button.setEnabled(index > 0);
                        next_button.setEnabled(index < usedIndices.size() - 1);
                    }
                    playSong(songPaths.get(usedIndices.get(index)), index);
                }                
            }
        });

        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                 final int index = myShuffle.getSelectedIndex();
                 if (index == -1) {
                     String message = "No song has been selected. " +
                                      "Please select one before stopping.";
                     JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                   "No Song Selected",
                                                   JOptionPane.ERROR_MESSAGE);
                     return ;
                 }

                 closeSong(songPaths.get(usedIndices.get(index)));
             }
         });

        stopAllButton.setEnabled(false);
        stopAllButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                closeSongs();
                repeat.setEnabled(true);
                cont_play.setEnabled(true);
                prev_button.setEnabled(false);
                next_button.setEnabled(false);
            }
        });

        prev_button.setEnabled(false);
        prev_button.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final MP3 current_song = playingMP3s.values().iterator().next().peek();
                final int prev_index = current_song.shuffle_index - 1;
                current_song.close();
                playingMP3s = Collections.synchronizedMap(
                                  new HashMap<String, Queue<MP3>>());
                multimap_size = new AtomicInteger();
                prev_button.setEnabled(prev_index > 0);
                next_button.setEnabled(prev_index < usedIndices.size() - 1);
                playSong(songPaths.get(usedIndices.get(prev_index)), prev_index);
                myShuffle.setSelectedIndex(prev_index);
            }
        });

        next_button.setEnabled(false);
        next_button.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final MP3 current_song = playingMP3s.values().iterator().next().peek();
                final int next_index = current_song.shuffle_index + 1;
                current_song.close();
                playingMP3s = Collections.synchronizedMap(
                                  new HashMap<String, Queue<MP3>>());
                multimap_size = new AtomicInteger();
                prev_button.setEnabled(next_index > 0);
                next_button.setEnabled(next_index < usedIndices.size() - 1);
                playSong(songPaths.get(usedIndices.get(next_index)), next_index);
                myShuffle.setSelectedIndex(next_index);
            }
        });

        repeat.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                cont_play.setSelected(false);
            }
        });

        cont_play.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                repeat.setSelected(false);
            }
        });

        myShuffle = new JList<String>();
        myShuffle.setVisibleRowCount(-1);
        myShuffle.setLayoutOrientation(JList.VERTICAL);
        myShuffle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(myShuffle);
        listScroller.setPreferredSize(new Dimension(250, 295));

        // For layout purposes, put the buttons in a separate panel
        JPanel topPanel = new JPanel(new GridLayout(6,1));
        topPanel.add(new JLabel("  Instructions for formating input playlist"));
        topPanel.add(new JLabel("  Step 1: Sort your playlist by genre."));
        topPanel.add(new JLabel("  Step 2: Right-click the playlist, then Export..."));
        topPanel.add(new JLabel("  Step 3: Save as a text file."));
        topPanel.add(new JLabel("  Step 4: Save Markov chain with the genre " +
                                "probabilities in alphabetical order, of the genre,"));
        topPanel.add(new JLabel(String.format("%-16s", "") +
                                "separated by whitespace (optional)."));

        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Playlist File"));
        mainPanel.add(playlistPathField);
        mainPanel.add(playlistBrowseButton);
        mainPanel.add(new JLabel("Markov Chain File"));
        mainPanel.add(markovPathField);
        mainPanel.add(markovBrowseButton);
        mainPanel.add(new JLabel("Genre from your playlist (optional)"));
        mainPanel.add(startGenre);
        mainPanel.add(new JLabel("Number of songs"));
        mainPanel.add(listLength);
        mainPanel.add(shuffleButton);
        mainPanel.add(playButton);
        mainPanel.add(blendButton);
        mainPanel.add(stopButton);
        mainPanel.add(stopAllButton);
        mainPanel.add(prev_button);
        mainPanel.add(next_button);
        mainPanel.add(repeat);
        mainPanel.add(cont_play);

        add(topPanel, BorderLayout.PAGE_START);
        add(mainPanel, BoxLayout.Y_AXIS);
        add(listScroller, BorderLayout.SOUTH);

        setTitle("Markov Music Shuffler");
        setPreferredSize(new Dimension(565, 573));
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
    }

    /* Series of getters and setters. */
    final public synchronized boolean isRepeatSelected() {
        return repeat.isSelected();
    }

    final public boolean isContPlaySelected() {
        return cont_play.isSelected();
    }

    final public int numShuffleSongs() {
        return usedIndices.size();
    }

    final public String getSongPath(int shuffle_index) {
        return songPaths.get(usedIndices.get(shuffle_index));
    }

    final void setNextButtonEnabled(boolean bool) {
        next_button.setEnabled(bool);
    }

    final void setShuffleSelect(int shuffle_index) {
        myShuffle.setSelectedIndex(shuffle_index);
    }

    /* Plays one song. Runtime: O(1). */
    final public void playSong(final String path, final int shuffle_index) {
        final MP3 newmp3 = new MP3(path, shuffle_index);
        Queue<MP3> sameSong = playingMP3s.get(path);
        if (sameSong == null) {
            sameSong = new ConcurrentLinkedQueue<MP3>();
            playingMP3s.put(path, sameSong);
        }
        sameSong.add(newmp3);
        multimap_size.getAndIncrement();
        newmp3.play(this);
    }

    /* Stops one song. If the selected song to stop has multiple
     * instances playing, stop the least recent. Runtime: O(1). 
     */
    final public synchronized void closeSong(final String path) {
        if (playingMP3s.containsKey(path)) {
            final Queue<MP3> sameSongs = playingMP3s.get(path);
            MP3 song = sameSongs.remove();
            song.close();
            if (sameSongs.isEmpty()) {
                playingMP3s.remove(path);
            }

            switch (multimap_size.getAndDecrement()) {
                case 1: prev_button.setEnabled(false);
                        next_button.setEnabled(false);
                        repeat.setEnabled(true);
                        cont_play.setEnabled(true);
                        return ;
                case 2: repeat.setEnabled(true);
                        cont_play.setEnabled(true);
                        prev_button.setEnabled(song.shuffle_index > 0);
                        next_button.setEnabled(song.shuffle_index < usedIndices.size() - 1);
            }
        }
    }

    /* Stops all songs. Runtime: O(n). */
    final public void closeSongs() {
        for (final Map.Entry<String, Queue<MP3>> entry : playingMP3s.entrySet()) {
            for (final MP3 song : entry.getValue()) {
                song.close();
            }
        }
        playingMP3s = Collections.synchronizedMap(new HashMap<String, Queue<MP3>>());
        multimap_size = new AtomicInteger();
    }

    /* Reads exported playlist file. */
    final public boolean readPlaylist(String playlist) {
        if (!ogPlaylist.isEmpty()) {
            songPaths = new ArrayList<String>();
            ogPlaylist = new ArrayList<String>();
            usedIndices = new ArrayList<Integer>();
            genres = new HashMap<String, Integer>();
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                                    new FileInputStream(playlist), "UTF-16"))) {
            String line = br.readLine();
            if (line == null) {
                String message = "The given file is empty. " +
                                 "Please enter a different file.";
                JOptionPane.showMessageDialog(this, message,
                                              "Empty File",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Ensure the categories are correct
            String[] lineSplits = line.split("\\t");
            if (!lineSplits[0].equals("Name") ||
                    !lineSplits[1].equals("Artist") ||
                    !lineSplits[5].equals("Genre") ||
                    !lineSplits[26].equals("Location")) {
                String message = "Did you properly save/select the playlist? " +
                                 "Try again. Maybe iTunes has updated.";
                JOptionPane.showMessageDialog(this, message,
                                              "Bad File Format",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }

            for (int i = 1; (line = br.readLine()) != null; ++i) {
                lineSplits = line.split("\\t");
                lineSplits[1] = lineSplits[1].trim();
                lineSplits[5] = lineSplits[5].trim();

                ogPlaylist.add(lineSplits[0].trim() +  // Compiler opts ftw
                               (lineSplits[1].equals("") ? "" : " by : ") +
                               lineSplits[1] +         // Artist exists?
                               "         " +
                               lineSplits[5]);
                genres.put(lineSplits[5], i);
                songPaths.add(lineSplits[26]);
            }
        } catch (UnsupportedEncodingException e) {
            String message = "Did you properly save/select the playlist? " +
                             "Try again. Maybe iTunes has updated.";
            JOptionPane.showMessageDialog(this, message,
                                          "Bad File Format",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (FileNotFoundException e) {
            String message = "Chosen file does not exist.";
            JOptionPane.showMessageDialog(this, message, "Invalid Path",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IOException e) {
            // Not really sure what else to do, doesn't cause a crash
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /* Reads exported playlist, applies Markov Chain, and displays in GUI. */
    final public void shuffleSongs(String playlistFilename, String startG,
                                       String markovFilename, int num_songs) {
        if (!readPlaylist(playlistFilename)) {
            return ;
        }
        if ((num_songs > ogPlaylist.size()) ||
                num_songs <= 0) {
            String message = "Invalid number! Must be equal or less than " +
                             "the number of songs in your playlist and " +
                             "greater than 0. May also be a file format " +
                             "problem, try re-exporting playlist if " +
                             "confident that the number is correct.";
            JOptionPane.showMessageDialog(this, message,
                                          "Invalid Number of Songs",
                                          JOptionPane.ERROR_MESSAGE);
            return ;
        }

        final Random rand = new Random();
        final double markov[][] = new double[genres.size()][genres.size()];
        if (markovFilename.equals("")) {
            /* Creates random markov chain based on genres from playlist;
             * very unfair to later genres if there are too many. */
            for (int row = 0; row < markov.length; ++row) {
                double max = 1.0;
                double random = 0.0;
                for (int col = 0; col < markov.length; ++col) {
                    if ((col + 1 == markov.length) || (max == 0)) {
                        random = max;
                    } else {
                        random = max * rand.nextDouble();
                        max -= random;
                    }
                    markov[row][col] = random;
                }
            }
        } else {
            // Process Markov chain input file
            try (BufferedReader br =
                    new BufferedReader(new FileReader(markovFilename))) {
                for (int i = 0; i < markov.length; ++i) {
                    double total_prob = 0.0;
                    final String[] chainProbs = br.readLine().split("\\s");
                    for (int j = 0, k = 0; j < markov.length; ++k) {
                        if (!chainProbs[k].equals("")) {
                            final double prob = Double.parseDouble(chainProbs[k]);
                            markov[i][j++] = prob;
                            total_prob += prob;
                        }
                    }
                    if ((total_prob < 0.99999) || (total_prob > 1.0)) {
                        String message = "All probabilities do not sum to 1.";
                        JOptionPane.showMessageDialog(this, message,
                                                      "Bad File Format",
                                                      JOptionPane.ERROR_MESSAGE);
                        return ;
                    }
                }
            } catch (NumberFormatException e) {
                String message = "Markov chain file must be " +
                                 "expressed in double.";
                JOptionPane.showMessageDialog(this, message, "Bad File Format",
                                              JOptionPane.ERROR_MESSAGE);
                return ;
            } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                String message = "Not enough rows/columns in your Markov " +
                                 "chain (should be equal).";
                JOptionPane.showMessageDialog(this, message, "Bad File Format",
                                              JOptionPane.ERROR_MESSAGE);
               return ;
            } catch (FileNotFoundException e) {
                String message = "Chosen file does not exist.";
                JOptionPane.showMessageDialog(this, message, "Invalid Path",
                                              JOptionPane.ERROR_MESSAGE);
                return ;
            } catch (IOException e) {
                // Not really sure what else to do, doesn't cause a crash
                e.printStackTrace();
                return ;
            }
        }

        int index = -1;
        final Iterator<String> genreItr = genres.keySet().iterator();
        if (startG.equals("")) {
            index = 0;
            startG = genreItr.next();  // Pick a genre, if one wasn't given
        } else if (!genres.containsKey(startG)) {
            String message = "Genre not found in playlist, please " +
                             "enter a genre in your playlist.";
            JOptionPane.showMessageDialog(this, message,
                                          "Invalid Genre",
                                          JOptionPane.ERROR_MESSAGE);
            return ;
        }

        // Find the index of user inputted genre
        if (index != 0) {
            for (index = 0; !startG.equals(genreItr.next()); ++index);
        }

        final Integer[] end = genres.values().toArray(new Integer[markov.length]);
        Arrays.sort(end);              // Assuming playlist is sorted by genre

        // Create array of arrays for each genre's range of indices
        final int[][] unusedIndices = new int[end.length][];
        final int[] lengths = new int[end.length];
        for (int i = 0, start = 0; i < end.length; ++i) {
            final int length = end[i] - start;
            unusedIndices[i] = new int[length];
            for (int j = 0; start < end[i]; ++j) {
                unusedIndices[i][j] = start++;
            }
            lengths[i] = length;
        }

        // Creates shuffle
        final DefaultListModel<String> listModel = new DefaultListModel<String>();
        for (int q = 0, emptyGenres = 0; q < num_songs;) {
            double acc = 0.0;
            final double randDouble = rand.nextDouble();
            for (int i = 0; i < end.length; ++i) {
                acc += markov[index][i];
                if (randDouble <= acc) {
                    /* Pick random element in array to prevent the same
                     * shuffle order of songs for each genre. */
                    final int randIndex = rand.nextInt(lengths[i]);
                    final int length = --lengths[i];
                    final int playlistIndex = unusedIndices[i][randIndex];
                    unusedIndices[i][randIndex] = unusedIndices[i][length];

                    // "Pick" song from genre
                    usedIndices.add(playlistIndex);

                    // Map GUI index to songPath structure index
                    listModel.addElement(ogPlaylist.get(playlistIndex));
                    index = i;
                    ++q;

                    // Distribute probability of genre with no songs left
                    if (length == 0) {
                        ++emptyGenres;
                        for (int j = 0; j < markov.length; ++j) {
                            final double probSplit = markov[j][i] /
                                                      (markov.length-emptyGenres);
                            for (int k = 0; k < markov.length; ++k) {
                                if (k == i) {
                                    markov[j][k] = 0.0;
                                } else if (markov[j][k] != 0.0) {
                                    markov[j][k] += probSplit;
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        myShuffle.setModel(listModel);
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PlaylistGUI().setVisible(true);
            }
        });
    }
}
