import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import javax.swing.*;


/**
 * Creates a GUI that takes in an exported iTunes playlist, number of
 * songs, and an optional genre to start the Markov chain with in order
 * to create a song shuffle based on the aforementioned Markov chain.
 * Allows users to play a song, or play multiple songs at the same time.
 *
 * @author Favian Contreras <fnc4@cornell.edu>
 *
 */
@SuppressWarnings("serial")
final public class PlaylistGUI extends JFrame {
    JList<String> myShuffle;
    List<String> songPaths;
    List<String> ogPlaylist;
    List<Integer> usedIndex;
    Map<String, Integer> genres;
    Map<String, Queue<MP3>> playingMP3s;    //bootleg multimap

    public PlaylistGUI() {
        initComponents();
    }

    final private void initComponents() {
        final JFileChooser fc = new JFileChooser();
        final JTextField listLength = new JTextField(5);
        final JTextField startGenre = new JTextField(10);
        final JTextField pathTextField = new JTextField(35);

        final JButton playButton = new JButton("Play");
        final JButton stopButton = new JButton("Stop");
        final JButton shuffleButton = new JButton("Shuffle");
        final JButton browseButton = new JButton("Browse...");
        final JButton blendButton = new JButton("Blend Play");
        final JButton stopAllButton = new JButton("Stop All");

        browseButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                int returnVal = fc.showDialog(PlaylistGUI.this, "Select");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fc.getSelectedFile();
                    pathTextField.setText(selectedFile.getPath());
                }
            }
        });
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
                shuffleSongs(num_songs, pathTextField.getText(), startGenre.getText());
            }
        });
        playButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                int index = myShuffle.getSelectedIndex();
                if (index == -1) {
                    String message = "No song has been selected. " +
                                     "Please select one before playing.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "No Song Selected",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }
                closeSongs();
                playSong(usedIndex.get(index));
            }
        });
        blendButton.addActionListener(new ActionListener() {
            final public void actionPerformed(ActionEvent e) {
                int index = myShuffle.getSelectedIndex();
                if (index == -1) {
                    String message = "No song has been selected. " +
                                     "Please select one before playing.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "No Song Selected",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }
                playSong(usedIndex.get(index));
            }
        });
        stopButton.addActionListener(new ActionListener() {
           final public void actionPerformed(ActionEvent e) {
                int index = myShuffle.getSelectedIndex();
                if (index == -1) {
                    String message = "No song has been selected. " +
                                     "Please select one before playing.";
                    JOptionPane.showMessageDialog(PlaylistGUI.this, message,
                                                  "No Song Selected",
                                                  JOptionPane.ERROR_MESSAGE);
                    return ;
                }
                closeSong(usedIndex.get(index));
            }
        });
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

        songPaths = new ArrayList<String>();
        ogPlaylist = new ArrayList<String>();
        usedIndex = new ArrayList<Integer>();
        genres = new HashMap<String, Integer>();
        playingMP3s = new HashMap<String, Queue<MP3>>();

        //For layout purposes, put the buttons in a separate panel
        JPanel panel1 = new JPanel(new GridLayout(4,1));
        panel1.add(new JLabel("  Instructions for formating input playlist"));
        panel1.add(new JLabel("  Step 1: Sort your playlist by genre."));
        panel1.add(new JLabel("  Step 2: Right-click playlist, then Export..."));
        panel1.add(new JLabel("  Step 3: Save as a text file."));

        JPanel Panel2 = new JPanel();
        Panel2.add(pathTextField);
        Panel2.add(browseButton);
        Panel2.add(new JLabel("Please enter a genre from your playlist."));
        Panel2.add(startGenre);
        Panel2.add(new JLabel("Number of songs"));
        Panel2.add(listLength);
        Panel2.add(shuffleButton);
        Panel2.add(playButton);
        Panel2.add(blendButton);
        Panel2.add(stopButton);
        Panel2.add(stopAllButton);

        //Add the buttons and the log to this panel.
        add(panel1, BorderLayout.PAGE_START);
        add(Panel2, BoxLayout.Y_AXIS);
        add(listScroller, BorderLayout.SOUTH);

        setTitle("Markov Music Shuffler");
        setPreferredSize(new Dimension(585, 480));
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
    }

    /* Plays one song. Runtime: O(1). */
    final public void playSong(int index) {
        String path = songPaths.get(index);
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
     * instances playing, stop the least recent. Runtime: O(1). */
    final public void closeSong(int index) {
        String path = songPaths.get(index);
        if (playingMP3s.containsKey(path)) {
            final Queue<MP3> sameSongs = playingMP3s.get(path);
            sameSongs.peek().close();   //close 1st, in case of system failure
            sameSongs.remove();
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
        // Way faster than clear(), that's OOP for you.
        playingMP3s = new HashMap<String, Queue<MP3>>();
    }

    /* Reads exported playlist file. */
    final public void read(String exportedPlaylist) throws IOException {
        if (!ogPlaylist.isEmpty()) {
            songPaths = new ArrayList<String>();
            ogPlaylist = new ArrayList<String>();
            usedIndex = new ArrayList<Integer>();
            genres = new HashMap<String, Integer>();
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                                    new FileInputStream(exportedPlaylist), "UTF-16"))) {
            int i = 1;
            br.readLine();                                                 //garbage line
            for (String line; (line = br.readLine()) != null; ++i) {
                int j = 0;
                final StringBuilder song = new StringBuilder();
                for (final String lineSplit : line.split("\\t")) {
                    switch (j++) {
                        case 0:  song.append(lineSplit).append(" by: ");   //song title
                                 break;
                        case 1:  song.append(lineSplit).append("      ");  //artist
                                 break;
                        case 5:  song.append(lineSplit);                   //genre
                                 genres.put(lineSplit, i);
                                 break;
                        case 26: songPaths.add(lineSplit);                 //filepath
                    }
                }
                //Compiles a list of all the songs in original playlist
                ogPlaylist.add(song.toString());
            }
        }
    }

    /* Reads exported playlist, applies Markov Chain, and displays in GUI. */
    final public void shuffleSongs(int num_songs,
                                       String pathTextField, String startG) {
        File exportedPlaylist = new File(pathTextField);
        if ((exportedPlaylist == null) || !exportedPlaylist.isFile()) {
            String message = "Invalid file type or no file selected.";
            JOptionPane.showMessageDialog(this, message, "Invalid Path",
                                          JOptionPane.ERROR_MESSAGE);
            return ;
        }
        try {
            read(pathTextField);
        } catch(IOException q) {
            q.printStackTrace();
        }
        if ((num_songs > ogPlaylist.size()) ||
                (num_songs <= 0)) {
            String message = "Invalid number! Must be equal or less " +
                             "than the number of songs in your " +
                             "playlist and greater than 0.";
            JOptionPane.showMessageDialog(this, message,
                                          "Invalid Number of Songs",
                                          JOptionPane.ERROR_MESSAGE);
            return ;
        }

        // TODO: take an optional markov chain input file 
        // Creates random markov chain based on genres from exported playlist
        final Random rand = new Random();
        final double markov[][] = new double[genres.size()][genres.size()];
        for (int row = 0; row < markov.length; ++row) {
            double max = 1.0;
            double random = 0.0;
            for (int col = 0; col < markov.length; ++col) {
                if ((col + 1 == markov.length) || (max == 0)) {
                    random = max;
                } else {
                    random = 0.1 + (max - 0.1) * rand.nextDouble();
                    max -= random;
                }
                markov[row][col] = random;
            }
        }

        int index = -1;
        final Iterator<String> genreItr = genres.keySet().iterator();
        if (startG.equals("")) {
            index = 0;
            startG = genreItr.next();   //Pick a genre, if one wasn't given
        }
        if (!genres.containsKey(startG)) {
            String message = "Genre not found in playlist, please " +
                             "enter a genre in your playlist.";
            JOptionPane.showMessageDialog(this, message,
                                          "Invalid Genre",
                                          JOptionPane.ERROR_MESSAGE);
            return ;
        }

        final Integer[] end = genres.values().toArray(new Integer[markov.length]);
        Arrays.sort(end);   // Assuming playlist is sorted by genre

        //find the index of user inputted genre
        if (index != 0) {
            for (index = 0; !startG.equals(genreItr.next()); ++index);
        }

        final DefaultListModel<String> listModel = new DefaultListModel<String>();
        // Creates the shuffle
        for (int q = 0; q < num_songs;) {
            double acc = 0.0;
            final double randDouble = rand.nextDouble();
            for (int i = 0; i < end.length; ++i) {
                acc += markov[index][i];
                if (randDouble <= acc) {
                    // Randomly selects an unselected song from the genre
                    final List<Integer> unusedIndex = new ArrayList<Integer>();
                    for (int j = ((i == 0) ? 0 : end[i-1]); j < end[i]; ++j) {
                        if (!usedIndex.contains(j)) {
                            unusedIndex.add(j);
                        }
                    }
                    int playlistIndex = unusedIndex.get(
                                            rand.nextInt(unusedIndex.size()));
                    // "Pick" song from genre (use index)
                    usedIndex.add(playlistIndex);
                    // Map GUI index to songPath structure index
                    listModel.addElement(ogPlaylist.get(playlistIndex));    

                    if (unusedIndex.size() == 1) {
                        // Distribute probability of genre with no songs left
                        for (int j = 0; j < markov.length; ++j) {
                            int emptyGenres = 1;
                            for (int k = 0; k < markov.length; ++k) {
                                if (markov[j][k] == 0) {
                                    ++emptyGenres;
                                }
                            }
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
                    index = i;
                    ++q;
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
