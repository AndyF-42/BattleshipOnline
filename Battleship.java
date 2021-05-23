import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;

/*
 * Description
 * -----------
 * This class contains the functioning game.
 * One user will be the server and have
 * boolean isHost = true, for the client
 * isHost = false. Currently the host always
 * plays first.
 * 
 * Gameplay
 * --------
 * First, both users set up their ships, and
 * click the "Ready" button when finished. If
 * both users submit valid ships, the game begins.
 * Players take turns selecting a space to fire
 * at the opponent (hits are red, misses are
 * white). To receive the opponent's action,
 * a player must scan their radar until the
 * action comes through. The game ends once
 * one player destroys all the ships of their
 * opponent.
 * 
 * Authors
 * -------
 * Ally Delgado and Andy Fleischer
 * 
 * Date
 * ----
 * Last updated: June 8, 2020. 6:10 PM
 */

public class Battleship implements ActionListener {

	// UI setup
	JFrame frame = new JFrame();

	JPanel scorePanel = new JPanel();
	Font textFont = new Font("Consolas", Font.PLAIN, 15);
	int selfWins = 0;
	int oppWins = 0;
	JLabel selfWinsLabel = new JLabel();
	JLabel oppWinsLabel = new JLabel();

	JPanel panel = new JPanel();
	String playerName;
	String opponentName;
	JLabel selfName = new JLabel();
	JLabel oppName = new JLabel();
	JButton[] selfButtons = new JButton[100];
	JButton[] oppButtons = new JButton[100];
	JPanel selfPanel = new JPanel();
	JPanel oppPanel = new JPanel();
	JButton submitB = new JButton("Ready");
	JButton fireB = new JButton("Fire!");
	JButton radarB = new JButton("Scan Radar");

	// Tracks between the two phases
	final int PLACING_SHIPS = 0;
	final int BATTLING = 1;
	int phase = 0;

	// Used to tell the opponent when you won
	final int GAME_OVER = 101;

	// Variables that will be set with socket creation
	String hostName;
	int portNumber;
	boolean isHost;
	DataInputStream din;
	DataOutputStream dout;

	// Arrays to store where one's own and opponent's ships are
	int[] selfShips = new int[100];
	int[] oppShips = new int[100];
	// Keeps track of what space is selected to fire at
	int selectedSpace = -1;

	// Colors (set here because they are repeated often and to easily change
	// them/know what they mean)
	final Color WATER = new Color(50, 200, 250);
	final Color SHIP = new Color(150, 150, 150);
	final Color HIT = new Color(250, 10, 30);
	final Color MISS = Color.WHITE;
	final Border SELECTED = BorderFactory.createLineBorder(Color.YELLOW, 3);

	final int NOT_EDGE = 0;
	final int RIGHT_EDGE = 1;
	final int BOTTOM_EDGE = 2;
	final int RIGHT_BOTTOM_CORNER = 3;

	// finals to store squares with ships and w/o ships
	final int EMPTY_SQUARE = 0;
	final int SHIP_SQUARE = 1;
	final int HIT_SQUARE = 2;
	final int SUNKEN_SHIP = 3;
	final int MISS_SQUARE = 4;
	final int HORIZ = 0;
	final int VERT = 1;

	int[][] shipsCopy = new int[10][10];
	int[] validShipLengths = { 5, 4, 3, 3, 2 };

	public Battleship(String hostName, String port, boolean isHost, String playerName) {

		this.hostName = hostName;
		int portNumber = Integer.parseInt(port);
		this.isHost = isHost;
		this.playerName = playerName;
		selfName.setText(playerName);
		selfWinsLabel.setText(playerName + "'s Wins: " + selfWins);

		// If this instance is the host, set up a server and accept client, then set up
		// data streams
		if (isHost) {
			System.out.println("SERVER");
			frame.setTitle("Server (" + playerName + ")");
			try {
				@SuppressWarnings("resource")
				ServerSocket ss = new ServerSocket(portNumber);
				Socket s = ss.accept();
				din = new DataInputStream(s.getInputStream());
				dout = new DataOutputStream(s.getOutputStream());

			} catch (IOException e) {
				System.out.println("Exception caught when trying to listen on port " + portNumber
						+ " or listening for a connection");
				System.out.println(e.getMessage());
			}
		}
		// If this instance is the client, just set up the socket and data streams
		else {
			frame.setTitle("Client (" + playerName + ")");
			System.out.println("CLIENT");
			try {
				@SuppressWarnings("resource")
				Socket s = new Socket(hostName, portNumber);
				din = new DataInputStream(s.getInputStream());
				dout = new DataOutputStream(s.getOutputStream());

			} catch (UnknownHostException e) {
				System.err.println("Don't know about host " + hostName);
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Couldn't get I/O for the connection to " + hostName);
				System.out.println(e.getMessage());
				System.exit(1);
			}
		}

		// Share names with opponent, set up opponent wins label
		try {
			dout.writeUTF(playerName);
			opponentName = din.readUTF();
			oppName.setText(opponentName);
			oppWinsLabel.setText(opponentName + "'s Wins: " + oppWins);
		} catch (IOException e) {
			e.printStackTrace();
		}

		frame.setSize(1450, 700);
		frame.setLayout(new BorderLayout());

		// Initializing the 10x10 button grids
		selfPanel.setLayout(new GridLayout(10, 10));
		oppPanel.setLayout(new GridLayout(10, 10));
		for (int i = 0; i < selfButtons.length; i++) {
			selfButtons[i] = new JButton();
			selfButtons[i].addActionListener(this);
			selfButtons[i].setBackground(WATER);
			selfButtons[i].setPreferredSize(new Dimension(50, 50));
			selfPanel.add(selfButtons[i]);

			oppButtons[i] = new JButton();
			oppButtons[i].addActionListener(this);
			oppButtons[i].setBackground(WATER);
			oppButtons[i].setPreferredSize(new Dimension(50, 50));
			oppPanel.add(oppButtons[i]);
		}

		// Score panel (each player's wins)
		scorePanel.setLayout(new GridLayout(2, 1));
		scorePanel.add(selfWinsLabel);
		scorePanel.add(oppWinsLabel);
		selfWinsLabel.setFont(textFont);
		oppWinsLabel.setFont(textFont);

		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Opponent's name
		c.gridx = 0;
		c.gridy = 0;
		panel.add(oppName, c);
		oppName.setFont(textFont);
		oppName.setVisible(false);

		// Self name
		c.gridx = 2;
		c.gridy = 0;
		c.insets = new Insets(0, 50, 0, 0);
		panel.add(selfName, c);
		selfName.setFont(textFont);

		// Button grid for guessing on opponent
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(0, 0, 50, 0);
		panel.add(oppPanel, c);
		oppPanel.setVisible(false);

		// "Ready" button
		submitB.addActionListener(this);
		submitB.setPreferredSize(new Dimension(200, 100));
		c.insets = new Insets(0, 150, 50, 150);
		panel.add(submitB, c);
		submitB.setFont(textFont);

		// Fire weapon button
		c.gridx = 1;
		c.gridy = 1;
		c.insets = new Insets(0, 50, 200, 0);
		fireB.addActionListener(this);
		fireB.setPreferredSize(new Dimension(200, 100));
		panel.add(fireB, c);
		fireB.setVisible(false);
		fireB.setFont(textFont);

		// Radar button
		c.gridx = 1;
		c.gridy = 1;
		c.insets = new Insets(150, 50, 50, 0);
		radarB.addActionListener(this);
		radarB.setPreferredSize(new Dimension(200, 100));
		panel.add(radarB, c);
		radarB.setVisible(false);
		radarB.setFont(textFont);

		// Button grid of own ships
		c.gridx = 2;
		c.gridy = 1;
		c.insets = new Insets(0, 50, 50, 0);
		panel.add(selfPanel, c);

		frame.add(scorePanel, BorderLayout.NORTH);
		frame.add(panel, BorderLayout.SOUTH);
		// frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// If you clicked on the grid for your ships
		for (int i = 0; i < selfButtons.length; i++) {
			if (phase == PLACING_SHIPS) {
				if (e.getSource().equals(selfButtons[i])) {
					// If you clicked on water, change to ship, or vice versa
					if (selfButtons[i].getBackground().equals(WATER)) {
						selfButtons[i].setBackground(SHIP);
					} else if (selfButtons[i].getBackground().equals(SHIP)) {
						selfButtons[i].setBackground(WATER);
					}
				}
			}
		}
		// If you clicked on the grid of your opponent's ships
		for (int i = 0; i < oppButtons.length; i++) {
			if (e.getSource().equals(oppButtons[i])) {
				// If you clicked on the space you have selected, remove border and reset
				// selectedSpace
				if (selectedSpace == i) {
					oppButtons[i].setBorder(UIManager.getBorder("Button.border"));
					selectedSpace = -1;
				}
				// If you clicked on a new space, set its border to SELECTED and reset the old
				// selected space if needed
				else {
					if (selectedSpace != -1) {
						oppButtons[selectedSpace].setBorder(UIManager.getBorder("Button.border"));
					}
					oppButtons[i].setBorder(SELECTED);
					selectedSpace = i;
				}
			}
		}
		// Fire button
		if (e.getSource().equals(fireB)) {
			if (selectedSpace == -1) {
				JOptionPane.showMessageDialog(frame, "No space selected!");
			} else {
				// See if it is hit or miss
				if (oppShips[selectedSpace] == 0) {
					oppButtons[selectedSpace].setBackground(MISS);
				} else {
					oppButtons[selectedSpace].setBackground(HIT);
				}
				// Remove border and disable button
				oppButtons[selectedSpace].setBorder(UIManager.getBorder("Button.border"));
				oppButtons[selectedSpace].setEnabled(false);
				// oppShips[selectedSpace] = 0;

				int numSunk = checkSunkenShips();

				// If you sunk the final ship:
				if (gameOver(numSunk)) {
					try {
						// Tell the opponent it's game over and where you finally shot
						dout.write(GAME_OVER);
						dout.write(selectedSpace);
						selfWins++;
						selfWinsLabel.setText(playerName + "'s Wins: " + selfWins);
						playAgain(true); // check if playing again, "true" because this player won
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				} else {
					// Tell opponent where you hit
					try {
						dout.write(selectedSpace);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					selectedSpace = -1;
					// Start reloading weapon and ready the radar
					fireB.setEnabled(false);
					fireB.setText("Reloading...");

					radarB.setEnabled(true);
					radarB.setText("Scan Radar");
				}
			}
		}
		// Radar button, scans for opponent's move
		if (e.getSource().equals(radarB)) {
			// Wait for opponent to say their move
			try {
				int oppMove = din.read();
				// If they tell you you lost, ouch
				if (oppMove == GAME_OVER) {
					selfButtons[din.read()].setBackground(HIT);
					radarB.setText("Our ship was hit!");
					oppWins++;
					oppWinsLabel.setText(opponentName + "'s Wins: " + oppWins);
					playAgain(false); // check if playing again, "false" because this player did not win
				}
				// Otherwise check if it was a hit or a miss, adjust backgrounds accordingly
				else {
					if (selfShips[oppMove] == 0) {
						selfButtons[oppMove].setBackground(MISS);
						radarB.setText("Missed! Phew");
					} else {
						selfButtons[oppMove].setBackground(HIT);
						radarB.setText("Our ship was hit!");
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			// After radar use, disable it and ready the weapon
			radarB.setEnabled(false);

			fireB.setEnabled(true);
			fireB.setText("Fire!");
		}
		// Submit button (for after ship placement
		if (e.getSource().equals(submitB)) {
			if (shipsValid()) {
				phase = BATTLING;
				// Fill out selfShips array with the player's ships
				for (int i = 0; i < selfButtons.length; i++) {
					if (selfButtons[i].getBackground().equals(SHIP)) {
						selfShips[i] = 1;
					}
				}

				try {
					// Tell opponent you are ready, wait for them to say the same
					dout.writeUTF("READY");
					if (din.readUTF().equals("READY")) {
						// Tell opponent where your ships are, get theirs, and fill out oppShips array
						String msgout = "";
						for (int i = 0; i < selfShips.length; i++) {
							msgout += Integer.toString(selfShips[i]);
						}
						dout.writeUTF(msgout);
						String[] msgin = din.readUTF().split("");
						for (int i = 0; i < msgin.length; i++) {
							oppShips[i] = Integer.parseInt(msgin[i]);
						}

						// Set up the new UI
						oppPanel.setVisible(true);
						submitB.setVisible(false);
						fireB.setVisible(true);
						radarB.setVisible(true);
						oppName.setVisible(true);
						// If you are the host, you start first with your weapon loaded. Client goes
						// second with radar ready
						if (isHost) {
							radarB.setEnabled(false);
							radarB.setText("Booting up radar...");
							fireB.setEnabled(true);
							fireB.setText("Fire!");
						} else {
							fireB.setText("Loading up weapon...");
							fireB.setEnabled(false);
							radarB.setEnabled(true);
							radarB.setText("Scan Radar");
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	// Checks if the selected spaces for ships is valid, meaning:
	// 1) Contains 5 ships of lengths 2, 3, 3, 4, and 5
	// 2) No ships are touching--even diagonally
	public boolean shipsValid() {
		// set up 2D array, reset valid ships

		shipsCopy = new int[10][10];
		for (int i = 0; i < shipsCopy.length; i++) {
			for (int j = 0; j < shipsCopy[0].length; j++) {
				if (selfButtons[10 * i + j].getBackground().equals(SHIP)) {
					shipsCopy[i][j] = 1;
				}
				System.out.print(shipsCopy[i][j]);
			}
		}
		validShipLengths[0] = 5;
		validShipLengths[1] = 4;
		validShipLengths[2] = 3;
		validShipLengths[3] = 3;
		validShipLengths[4] = 2;

		int shipLength = 0;
		// loop through entire board horizontally
		for (int i = 0; i < shipsCopy.length; i++) {
			for (int j = 0; j < shipsCopy[0].length; j++) {
				if (shipsCopy[i][j] == 1) {
					shipLength = horizontal(i, j, shipsCopy);
					if (shipLength > 1) {
						if (checkSurrounding(shipLength, i, j, "horizontal")) {
							if (!saveShipLength(shipLength)) {
								return false;
							}
						} else {
							JOptionPane.showMessageDialog(frame, "Those ships are not 6 feet apart. Invalid.");
							return false;
						}
					}
				}
			}
		}
		// loop through entire board vertically
		for (int j = 0; j < shipsCopy[0].length; j++) {
			for (int i = 0; i < shipsCopy.length; i++) {
				if (shipsCopy[i][j] == 1) {
					shipLength = vertical(i, j, shipsCopy);
					if (shipLength > 1) {
						if (checkSurrounding(shipLength, i, j, "vertical")) {
							if (!saveShipLength(shipLength)) {
								return false;
							}
						} else {
							JOptionPane.showMessageDialog(frame, "Those ships are not 6 feet apart. Invalid.");
							return false;
						}
					}
					// check for lone one square ships
					else {
						JOptionPane.showMessageDialog(frame, "Invalid ship length: 1");
						return false;
					}
				}
			}
		}
		// check if all the lengths are met
		for (int j = 0; j < validShipLengths.length; j++) {
			if (validShipLengths[j] != 20) {
				JOptionPane.showMessageDialog(frame, "Missing length " + validShipLengths[j]);
				return false;
			}
		}
		JOptionPane.showMessageDialog(frame, "Ships are valid.");

		return true;
	}

	// Returns the length of a ship going horizontally
	public int horizontal(int row, int col, int[][] TempBoard) {
		int shipLength = 0;

		while (TempBoard[row][col] == SHIP_SQUARE) {
			shipLength++;
			TempBoard[row][col] = EMPTY_SQUARE;
			col++;
			if (col == 10) { // if you hit the edge, break before out of bounds exception
				break;
			}
		}

		if (shipLength == 1) { // if it is length 1, replace the ship because it might be vertical
			TempBoard[row][col - 1] = SHIP_SQUARE;
		}

		return shipLength;
	}

	// Returns the length of a ship going horizontally
	public int vertical(int row, int col, int[][] TempBoard) {
		int shipLength = 0;

		while (TempBoard[row][col] == SHIP_SQUARE) {
			shipLength++;
			TempBoard[row][col] = EMPTY_SQUARE;
			row++;
			if (row == 10) { // if you hit the edge, break before out of bounds exception
				break;
			}
		}
		return shipLength;
	}

	// Saves a length of ship by replacing it from the validShipLengths array
	public boolean saveShipLength(int shipLength) {
		for (int i = 0; i < validShipLengths.length; i++) {
			if (validShipLengths[i] == shipLength) {
				validShipLengths[i] = 20;
				return true;
			}
		}
		JOptionPane.showMessageDialog(frame, "Invalid ship length: " + shipLength);
		return false;
	}

	// Checks all the spaces around a ship and returns true if nothing is around it
	// (good)
	public boolean checkSurrounding(int shipLength, int row, int col, String direction) {
		if (direction.equals("vertical")) { // vertical
			// above
			if ((row > 0) && ((col > 0 && shipsCopy[row - 1][col - 1] == 1)
					|| (col < 9 && shipsCopy[row - 1][col + 1] == 1))) {
				return false;
			}
			// left and right
			for (int i = 0; i < shipLength; i++) {
				if ((col > 0 && shipsCopy[row + i][col - 1] == 1) || (col < 9 && shipsCopy[row + i][col + 1] == 1)) {
					return false;
				}
			}
			// below
			if ((row + shipLength < 10) && ((col > 0 && shipsCopy[row + shipLength][col - 1] == 1)
					|| (col < 9 && shipsCopy[row + shipLength][col + 1] == 1))) {
				return false;
			}
		} else { // horizontal
					// left
			if ((col > 0) && ((row > 0 && shipsCopy[row - 1][col - 1] == 1)
					|| (row < 9 && shipsCopy[row + 1][col - 1] == 1))) {
				return false;
			}
			// above and below
			for (int i = 0; i < shipLength; i++) {
				if ((row > 0 && shipsCopy[row - 1][col + i] == 1) || (row < 9 && shipsCopy[row + 1][col + i] == 1)) {
					return false;
				}
			}
			// right
			if ((col + shipLength < 10) && ((row > 0 && shipsCopy[row - 1][col + shipLength] == 1)
					|| (row < 9 && shipsCopy[row + 1][col + shipLength] == 1))) {
				return false;
			}
		}
		return true;
	}

	// Checks to see if a ship was sunk, and if so, places misses all around it
	public int checkSunkenShips() {
		int vertLength = 0;
		int horizLength = 0;
		int tempShips[][] = new int[10][10];
		int i = 0;
		int j = 0;
		int k = 0;
		int numHits = 0;
		int numSunk = 0;
		// Create tempShips 2D array
		for (i = 0; i < 10; i++) {
			for (j = 0; j < 10; j++) {
				tempShips[i][j] = oppShips[(10 * i) + j];
			}

		}

		// check for a fully sunken ship
		for (i = 0; i < 10; i++) { // Rows
			for (j = 0; j < 10; j++) { // Columns

				numHits = 0;

				if (oppShips[(10 * i) + j] == SHIP_SQUARE) {
					horizLength = horizontal(i, j, tempShips); // Call with 2D representation of oppShips
					vertLength = vertical(i, j, tempShips);
					if (horizLength > vertLength) { // found horiz ship!
						for (k = j; k < horizLength + j; k++) {
							if (oppButtons[(10 * i) + k].getBackground().equals(HIT)) {
								numHits++;
							}
						}

						if (numHits == horizLength && numHits > 0) { // found complete sunken ship
							numSunk++;
							addMisses(i, j, HORIZ, horizLength, tempShips);

						}
					} else if (vertLength > horizLength) { // found vert ship!
						for (k = i; k < vertLength + i; k++) {
							if (oppButtons[(10 * k) + j].getBackground().equals(HIT)) {
								numHits++;
							}
						}
						if (numHits == vertLength && numHits > 0) { // found complete sunken ship
							numSunk++;
							addMisses(i, j, VERT, vertLength, tempShips);

						}
					} else if (vertLength == 0 && horizLength == 0) {
						// empty spot, not a ship
					} else {
						System.out.println("Unexpected ship found. i = " + i + " j= " + j + "horizLength = "
								+ horizLength + " vertLength = " + vertLength); // error
					}

				}
			}
		}
		return numSunk;
	}

	public void addMisses(int i, int j, int direction, int length, int[][] tempShips) {
		if (direction == VERT) {
			if (i > 0) {
				if (j > 0) {
					oppButtons[(10 * (i - 1) + (j - 1))].setBackground(MISS);
					// set miss (i-1,j-1)
				}
				oppButtons[(10 * (i - 1) + j)].setBackground(MISS);
				// set miss(i-1,j)
				if (j < 9) {
					oppButtons[(10 * (i - 1) + (j + 1))].setBackground(MISS);
					// setMiss(i-1, j+1)
				}
			}
			// left and right
			for (int k = 0; k < length; k++) {
				if (j > 0) {
					oppButtons[(10 * (i + k) + (j - 1))].setBackground(MISS);
					// setMiss(i+k, j-1)
				}
				if (j < 9) {
					oppButtons[10 * (i + k) + (j + 1)].setBackground(MISS);
					// setMiss(i+k,j+1)
				}
			}
			// below
			if (i + length < 10) {
				if (j > 0) {
					oppButtons[(10 * (i + length) + (j - 1))].setBackground(MISS);
					// setMiss(i+vertLength, j-1)
				}
				oppButtons[(10 * (i + length) + j)].setBackground(MISS);
				// setMiss(i+length, j)
				if (j < 9) {
					oppButtons[(10 * (i + length) + (j + 1))].setBackground(MISS);
					// setMiss(i+vertLength, j+1)
				}
			}
		} else {// horizontal
				// left
			if (j > 0) {
				if (i > 0) {
					oppButtons[(10 * (i - 1) + (j - 1))].setBackground(MISS);
					// setMiss(i-1,j-1)
				}
				oppButtons[(10 * i) + (j - 1)].setBackground(MISS);
				// setMiss(i,j-1)
				if (i < 9) {
					oppButtons[(10 * (i + 1) + (j - 1))].setBackground(MISS);
					// setMiss(i+1,j-1)
				}
			}
			// above and below
			for (int k = 0; k < length; k++) {
				if (i > 0) {
					oppButtons[(10 * (i - 1) + (j + k))].setBackground(MISS);
					// setMiss(i - 1, j + k);
				}
				if (i < 9) {
					oppButtons[(10 * (i + 1) + (j + k))].setBackground(MISS);
					// setMiss(i + 1, j + k);
				}
			}
			// right
			if (j + length < 10) {
				if (i > 0) {

					oppButtons[(10 * (i - 1) + (j + length))].setBackground(MISS);
					// setMiss(i-1, j+horizLength);
				}
				oppButtons[(10 * i) + (j + length)].setBackground(MISS);
				// setMiss(i, j+horizLength);
				if (i < 9) {
					oppButtons[(10 * (i + 1) + (j + length))].setBackground(MISS);
					// setMiss(i+1, j+horizLength);
				}
			}
		}
	}

	public int findStart(int i, int j, int direction, int length, int[][] tempShips) {
		int startLocation = 0;
		if (direction == HORIZ) {
			while (tempShips[i][j] == SHIP_SQUARE) {
				j--;
				if (j < 0) { // don't go out of bounds
					break;
				}
			}
			startLocation = j; // Returns left-most 2D column
		} else if (direction == VERT) {
			while (tempShips[i][j] == SHIP_SQUARE) {
				i--;
				if (i < 0) { // don't go out of bounds
					break;
				}
			}
			startLocation = i; // Returns top-most 2D row
		}
		return startLocation;

	}

	// Checks if someone has won
	public boolean gameOver(int numSunk) {
		if (numSunk == 5) {
			System.out.println("Game is over!");
			return true;
		}
		return false;
	}

	// Reset the game for another round
	public void resetGame() {
		// Reset the boards visually (enable all buttons and change color to water)
		for (int i = 0; i < oppButtons.length; i++) {
			oppButtons[i].setEnabled(true);
			oppButtons[i].setBackground(WATER);
			selfButtons[i].setEnabled(true);
			selfButtons[i].setBackground(WATER);
		}
		// Return to placing ships phase
		phase = PLACING_SHIPS;
		oppPanel.setVisible(false);
		submitB.setVisible(true);
		fireB.setVisible(false);
		radarB.setVisible(false);
		oppName.setVisible(false);

		// Reset the arrays that stored self and opponent's ships
		selfShips = new int[100];
		oppShips = new int[100];
		selectedSpace = -1;
	}

	// Checks to see if the players want to play again
	public void playAgain(boolean isWinner) {
		try {
			int playAgain;
			// Tell if they won or lost, ask to play again
			if (isWinner) {
				playAgain = JOptionPane.showOptionDialog(frame, "You win! Play again?", "", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, null, null);
			} else {
				playAgain = JOptionPane.showOptionDialog(frame, "You lose! Play again?", "", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, null, null);
			}
			dout.write(playAgain);
			// If playing again, check what opponent said
			if (playAgain == JOptionPane.YES_OPTION) {
				if (din.read() == JOptionPane.YES_OPTION) {
					resetGame();
				} else {
					JOptionPane.showMessageDialog(frame,
							"Unfortunately, your opponent is lame and\ndoes not want to play again. Goodbye!");
					System.exit(0);
				}
			}
			// If not playing again, check what opponent said and always exit
			else {
				if (din.read() == JOptionPane.NO_OPTION) {
					JOptionPane.showMessageDialog(frame, "Looks like you both don't want to play. Goodbye!");
				} else {
					JOptionPane.showMessageDialog(frame, "I'll let your opponent know that you are lame. Goodbye!");
				}
				System.exit(0);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}