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


@SuppressWarnings("serial")
public class PlaylistGUI extends JFrame implements ActionListener {
    JFileChooser fc;
    JTextField startGenre;
    JTextField listLength;
    JTextField pathTextField;
    JButton playButton;
    JButton stopButton;
    JButton blendButton;
    JButton browseButton;
    JButton shuffleButton;
    JButton stopAllButton;
    JList<String> myShuffle;
    DefaultListModel<String> songs;
    List<String> songPaths;
    List<String> ogPlaylist;
    List<Integer> usedIndex;
    Map<String, Integer> genres;
    Map<String, Queue<MP3>> playingMP3s;    //bootleg multimap
    double markov[][];

    public PlaylistGUI() {
        initComponents();
    }

    private void initComponents() {
        fc = new JFileChooser();
        listLength = new JTextField(5);
        startGenre = new JTextField(10);
        pathTextField = new JTextField(35);       
        browseButton = new JButton("Browse...");
        browseButton.addActionListener(this);
        shuffleButton = new JButton("Shuffle");
        shuffleButton.addActionListener(this);
        playButton = new JButton("Play");
        playButton.addActionListener(this);
        blendButton = new JButton("Blend Play");
        blendButton.addActionListener(this);
        stopButton = new JButton("Stop");
        stopButton.addActionListener(this);
        stopAllButton = new JButton("Stop All");
        stopAllButton.addActionListener(this);
        myShuffle = new JList<String>();
        myShuffle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myShuffle.setLayoutOrientation(JList.VERTICAL);
        myShuffle.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(myShuffle);
        listScroller.setPreferredSize(new Dimension(250, 295));
        songs = new DefaultListModel<String>();
        songPaths = new ArrayList<String>();
        ogPlaylist = new ArrayList<String>();
        usedIndex = new ArrayList<Integer>();
        genres = new HashMap<String, Integer>();
        playingMP3s = new HashMap<String, Queue<MP3>>();

        //For layout purposes, put the buttons in a separate panel
        JPanel panel1 = new JPanel();
        JLabel instructions1 = new JLabel();
        JLabel instructions2 = new JLabel();
        JLabel instructions3 = new JLabel();
        JLabel instructions4 = new JLabel();
        panel1.setLayout(new GridLayout(4,1));
        instructions1.setText("  Instructions for formating input playlist");
        instructions2.setText("  Step 1: Sort your playlist by genre.");
        instructions3.setText("  Step 2: Click File->Library->Export Playlist...");
        instructions4.setText("  Step 3: For playlist, save type as text file."); 
        panel1.add(instructions1);
        panel1.add(instructions2);
        panel1.add(instructions3);
        panel1.add(instructions4);
        JPanel Panel2 = new JPanel();
        Panel2.add(pathTextField);
        Panel2.add(browseButton);
        JLabel genre = new JLabel();
        genre.setText("Please enter a genre from your playlist.");
        Panel2.add(genre);
        Panel2.add(startGenre);
        JLabel num = new JLabel();
        num.setText("Number of songs");
        Panel2.add(num);
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

    public void actionPerformed(ActionEvent e) {
        //Handle browse button action.
        if (e.getSource() == browseButton) {
            int returnVal = fc.showDialog(PlaylistGUI.this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();
                pathTextField.setText(selectedFile.getPath());
            }
        } else if (e.getSource() == shuffleButton) {
            shuffleSongs();
        } else if (e.getSource() == playButton) {
            int index = myShuffle.getSelectedIndex();
            if (index == -1) {
                String message = "No song has been selected. Please select one before playing.";
                JOptionPane.showMessageDialog(this, message, "No Song Selected", JOptionPane.ERROR_MESSAGE);
                return ;
            }
            //plays songs, closes all other open songs
            closeSongs();
            playSong(usedIndex.get(index));
        } else if (e.getSource() == blendButton) {
            int index = myShuffle.getSelectedIndex();
            if (index == -1) {
                String message = "No song has been selected. Please select one before playing.";
                JOptionPane.showMessageDialog(this, message, "No Song Selected", JOptionPane.ERROR_MESSAGE);
                return ;
            }
            //plays song without closing other songs
            playSong(usedIndex.get(index));
        } else if (e.getSource() == stopButton) {
            int index = myShuffle.getSelectedIndex();
            if (index == -1) {
                String message = "No song has been selected. Please select one before playing.";
                JOptionPane.showMessageDialog(this, message, "No Song Selected", JOptionPane.ERROR_MESSAGE);
                return ;
            }
            closeSong(usedIndex.get(index));
        } else if (e.getSource() == stopAllButton) {
            closeSongs();
        }
    }

    /* Plays one song. Runtime: O(1). */
    public void playSong(int index) {
        String path = songPaths.get(index);
        MP3 newmp3 = new MP3(path);
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
    public void closeSong(int index) {
        String path = songPaths.get(index);
        if (playingMP3s.containsKey(path)) {
            Queue<MP3> sameSongs = playingMP3s.get(path);
            sameSongs.peek().close();    //close 1st, in case of system failure
            sameSongs.remove();
            if (sameSongs.isEmpty()) {
                playingMP3s.remove(path);
            }
        }
    }

    /* Stops all songs. Runtime: O(n). */
    public void closeSongs() {
        for (Map.Entry<String, Queue<MP3>> entry : playingMP3s.entrySet()) {
            for (MP3 song : entry.getValue()) {
                song.close();
            }
        }
        playingMP3s = new HashMap<String, Queue<MP3>>();    // Way faster than clear(), that's OOP for you.
    }

    /* Reads exported playlist file. */
    public void read(String exportedPlaylist) throws IOException {
        if (!ogPlaylist.isEmpty()) {
            songPaths = new ArrayList<String>();
            ogPlaylist = new ArrayList<String>();
            usedIndex = new ArrayList<Integer>();
            genres = new HashMap<String, Integer>();
        }
        int i = 1;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(exportedPlaylist), "UTF-16"))) {
            br.readLine();                                              //remove top line w/ categories
            for (String line; (line = br.readLine()) != null; ++i) {
                int j = 0;
                StringBuilder song = new StringBuilder();
                for (String lineSplit : line.split("\\t")) {
                    switch (j++) {
                        case 0:  song.append(lineSplit).append(" ");    //song title
                                 song.append("by: ");
                                 break;
                        case 1:  song.append(lineSplit).append(" ");    //artist
                                 song.append("     ");
                                 break;
                        case 5:  song.append(lineSplit).append(" ");    //genre
                                 genres.put(lineSplit, i);
                                 break;
                        case 26: songPaths.add(lineSplit);              //filepath
                    }
                }
                //Compiles a list of all the songs in original playlist
                ogPlaylist.add(song.toString());
            }
        }
        myShuffle.setModel(songs);
    }

    /* Searches the spreadsheet, applies Markov Chain, and displays in GUI. */
    public void shuffleSongs() {
        int num_songs;
        try {
            num_songs = Integer.valueOf(listLength.getText());
        } catch (NumberFormatException j) {
            String message = "Please insert a numerical value in the \"number of songs\" text field.";
            JOptionPane.showMessageDialog(this, message, "Invalid Number of Songs", JOptionPane.ERROR_MESSAGE);
            return ;
        }
        File exportedPlaylist = new File(pathTextField.getText());
        if ((exportedPlaylist != null) && exportedPlaylist.isFile()) {
            try {
                //reads exported playlist file
                read(pathTextField.getText());
                if ((num_songs > ogPlaylist.size()) ||
                        (num_songs <= 0)) {
                    String message = "Invalid number! Must be equal or less than the number of songs in your playlist and greater than 0.";
                    JOptionPane.showMessageDialog(this, message, "Invalid Number of Songs", JOptionPane.ERROR_MESSAGE);
                    return ;
                }

                // TODO: take an optional markov chain input file 
                // Creates random markov chain based on genres from exported playlist
                markov = new double[genres.size()][genres.size()];
                Random rand = new Random();
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
                String startG = startGenre.getText();
                Iterator<String> genreItr = genres.keySet().iterator();
                if (startG.equals("")) {
                    index = 0;
                    startG = genreItr.next();                                           //Pick a genre, if one wasn't given
                }
                if (genres.containsKey(startG)) {
                    Integer[] end = (Integer[])(genres.values().toArray(new Integer[markov.length]));
                    Arrays.sort(end);                                                   //Assuming playlist was sorted by genre

                    //find the index of user inputted genre
                    if (index != 0) {
                        for (index = 0; !startG.equals(genreItr.next()); ++index);
                    }

                    DefaultListModel<String> listModel = new DefaultListModel<String>();
                    // Creates the shuffle
                    for (int q = 0; q < num_songs;) {
                        double acc = 0.0;
                        double randDouble = rand.nextDouble();
                        for (int i = 0; i < end.length; ++i) {
                            acc += markov[index][i];
                            if (randDouble <= acc) {
                                // Randomly selects a previously unselected song from the genre
                                List<Integer> unusedIndex = new ArrayList<Integer>();
                                for (int j = ((i == 0) ? 0 : end[i-1]); j < end[i]; ++j) {
                                    if (!usedIndex.contains(j)) {
                                        unusedIndex.add(j);
                                    }
                                }
                                int playlistIndex = unusedIndex.get(rand.nextInt(unusedIndex.size()));
                                usedIndex.add(playlistIndex);                           //"pick" song from genre (use index)
                                listModel.addElement(ogPlaylist.get(playlistIndex));    //map GUI index to data structure index
                                if (unusedIndex.size() == 1) {
                                    // Distribute probability of genre with no songs left
                                    for (int j = 0; j < markov.length; ++j) {
                                        int numEmptyGenres = 1;
                                        for (int k = 0; k < markov.length; ++k) {
                                            if (markov[j][k] == 0) {
                                                ++numEmptyGenres;
                                            }
                                        }
                                        double probSplit = markov[j][i] / (markov.length - numEmptyGenres);
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
                } else {
                    String message = "Genre not found in playlist/library, please enter a genre in your playlist/library.";
                    JOptionPane.showMessageDialog(this, message, "Invalid Genre", JOptionPane.ERROR_MESSAGE);
                }
            } catch(IOException q) {
                q.printStackTrace();
            }
        } else {
            String message = "Invalid file type or no file selected.";
            JOptionPane.showMessageDialog(this, message, "Invalid Path", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PlaylistGUI().setVisible(true);
            }
        });
    }
}
