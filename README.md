# Markov Music Shuffler

This creates a music shuffle based on a Markov chain over the genres.
I utilize the
<a href="http://www.javazoom.net/javalayer/sources.html">JLayer MP3 library</a>
in order to play the MP3s; more on this can be seen in the MP3.java file.
Requires iTunes of some sort; the instructions pertain to an iTunes playlist,
however if you know the file format you can recreate a playlist file.

## What it Should Look Like

<img src="playlist_screencap.png" alt="Example Invocation" />

## Buttons and Fields

* Browse: Allows you to find/use exported playlist, or Markov chain file.
* Enter genre (optional): Allows you to input genre to begin Markov chain.
* Number of songs: Number of songs to display in the shuffle.
* Shuffle: Creates a shuffle based on a Markov chain.
* Play: Plays the selected song. If other songs are currently playing, they are stopped.
* Blend Play: Same as play except, any playing songs are not stopped.
* Stop: Stops the selected song.
* Stop All: Stops all currently playing songs.
* Previous: Plays the previous song in the shuffle.
* Next: Plays the next song in the shuffle.
* Continuous Play: Plays the shuffle songs in sequence, one after the other.
* Repeat: Repeats the song.

## Markov Chain

The file format for the Markov chain should simply align the genres in alphabetical
order; columns and rows should be in alphabetical ascending order for genres. As
shown in the picture, the file expects probabilities ranging from 0.0 to 1.0, and
the probabilities should sum to 1.0.
