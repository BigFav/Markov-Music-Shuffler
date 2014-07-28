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
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// TODO add instruction line about Markov chain
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
    JList<String> myShuffle;
    List<String> songPaths;
    List<String> ogPlaylist;                // List of all songs in playlist
    List<Integer> usedIndices;
    Map<String, Integer> genres;
    Map<String, Queue<MP3>> playingMP3s;    // Bootleg multimap

    public PlaylistGUI() {
        songPaths = new ArrayList<String>();
        ogPlaylist = new ArrayList<String>();
        usedIndices = new ArrayList<Integer>();
        genres = new HashMap<String, Integer>();
        playingMP3s = new HashMap<String, Queue<MP3>>();

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
        playlistBrowseButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final int returnVal = fc.showDialog(PlaylistGUI.this, "Select");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    playlistPathField.setText(fc.getSelectedFile().getPath());
                }
            }
        });

        final JButton markovBrowseButton = new JButton("Browse...");
        markovBrowseButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                final int returnVal = fc.showDialog(PlaylistGUI.this, "Select");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    markovPathField.setText(fc.getSelectedFile().getPath());
                }
            }   
        });

        final JButton shuffleButton = new JButton("Shuffle");
        shuffleButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                int num_songs;
                try {
                    num_songs = Integer.valueOf(listLength.getText());
                } catch (NumberFormatException j) {
                    String message = "Please insert a numerical value in the " +
                                     "\"number of songs\" text field.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "Invalid Number of Songs",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }
                shuffleSongs(playlistPathField.getText(), startGenre.getText(),
                             markovPathField.getText(), num_songs);
            }
        });

        final JButton playButton = new JButton("Play");
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
                playSong(usedIndices.get(index));
            }
        });

        final JButton blendButton = new JButton("Blend Play");
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
                playSong(usedIndices.get(index));
            }
        });

        final JButton stopButton = new JButton("Stop");
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
                 closeSong(usedIndices.get(index));
             }
         });

        final JButton stopAllButton = new JButton("Stop All");
        stopAllButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                closeSongs();
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

        add(topPanel, BorderLayout.PAGE_START);
        add(mainPanel, BoxLayout.Y_AXIS);
        add(listScroller, BorderLayout.SOUTH);

        setTitle("Markov Music Shuffler");
        setPreferredSize(new Dimension(565, 543));
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
    }

    /* Plays one song. Runtime: O(1). */
    final public void playSong(int index) {
        final String path = songPaths.get(index);
        final MP3 newmp3 = new MP3(path);
        Queue<MP3> sameSong = playingMP3s.get(path);
        if (sameSong == null) {
            sameSong = new LinkedList<MP3>();
            playingMP3s.put(path, sameSong);
        }
        sameSong.add(newmp3);
        newmp3.play();
    }

    /* Stops one song. If the selected song to stop has multiple
     * instances playing, stop the least recent. Runtime: O(1). 
     */
    final public void closeSong(int index) {
        final String path = songPaths.get(index);
        if (playingMP3s.containsKey(path)) {
            final Queue<MP3> sameSongs = playingMP3s.get(path);
            sameSongs.remove().close();
            if (sameSongs.isEmpty()) {
                playingMP3s.remove(path);
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
        playingMP3s = new HashMap<String, Queue<MP3>>();
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
                (num_songs <= 0)) {
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
                    if ((total_prob < 0.9999999999999999) || (total_prob > 1.0)) {
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
        }
        if (!genres.containsKey(startG)) {
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
        int start = 0;
        for (int i = 0; i < end.length; ++i) {
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
                    final int randInt = rand.nextInt(lengths[i]);
                    final int length = --lengths[i];
                    final int playlistIndex = unusedIndices[i][randInt];
                    unusedIndices[i][randInt] = unusedIndices[i][length];

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