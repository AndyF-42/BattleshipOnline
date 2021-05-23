import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/*
 * Description
 * -----------
 * This class contains the main method, and
 * is where users will start. One will make
 * up a port number and click "Host" while
 * the other will use the name of the host's
 * computer name and port number to join.
 * Leads both users to BattleshipGame class,
 * but with different parameters.
 * 
 * Authors
 * -------
 * Ally Delgado and Andy Fleischer
 * 
 * Date
 * ----
 * Last updated: June 7, 2020
 */

public class GameSetup implements ActionListener {

	JFrame frame = new JFrame("Welcome to Battleship!");
	JPanel panel = new JPanel();
	ImageIcon logoIcon = new ImageIcon(getClass().getResource("battleshipLogo.PNG"));
	JLabel logo = new JLabel(logoIcon);
	JButton hostB = new JButton("Host");
	JButton joinB = new JButton("Join");
	JTextField hostPortTF = new HintTextField("Port Number");
	JTextField joinPortTF = new HintTextField("Port Number");
	JTextField joinNameTF = new HintTextField("Host Name");
	ImageIcon helpIcon = new ImageIcon(getClass().getResource("questionMark.png"));
	JButton helpB = new JButton(helpIcon);
	Font buttonFont = new Font("Verdana", Font.PLAIN, 20);

	// When devMode is true, simply clicking "host" or "join" will automatically
	// connect the socket.
	// No port number or host name required, but it only works when on the same
	// computer.
	boolean devMode = false;

	public GameSetup() {
		frame.setSize(780, 713);
		frame.setLayout(new BorderLayout());

		panel.setBackground(new Color(16, 31, 55)); // dark blue background
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Battleship logo
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		c.insets = new Insets(0, 0, 0, 0);
		panel.add(logo, c);

		// "Host" button
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.insets = new Insets(0, 0, 0, 0);
		hostB.setFont(buttonFont);
		hostB.setPreferredSize(new Dimension(200, 100));
		hostB.addActionListener(this);
		panel.add(hostB, c);

		// Port textField for host
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		hostPortTF.setHorizontalAlignment(JTextField.CENTER);
		hostPortTF.setPreferredSize(new Dimension(150, 100));
		panel.add(hostPortTF, c);

		// Help (?) button
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.insets = new Insets(50, 33, 50, 0);
		helpB.setPreferredSize(new Dimension(50, 50));
		helpB.addActionListener(this);
		panel.add(helpB, c);

		// "Join" button
		c.gridx = 0;
		c.gridy = 3;
		c.insets = new Insets(0, 0, 50, 0);
		joinB.setFont(buttonFont);
		joinB.setPreferredSize(new Dimension(200, 100));
		joinB.addActionListener(this);
		panel.add(joinB, c);

		// Host name textField for client
		c.gridx = 1;
		c.gridy = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 50, 0);
		joinNameTF.setHorizontalAlignment(JTextField.CENTER);
		joinNameTF.setPreferredSize(new Dimension(231, 100));
		panel.add(joinNameTF, c);

		// Port textField for client
		c.gridx = 2;
		c.gridy = 3;
		c.insets = new Insets(0, 0, 50, 0);
		joinPortTF.setHorizontalAlignment(JTextField.CENTER);
		joinPortTF.setPreferredSize(new Dimension(231, 100));
		panel.add(joinPortTF, c);

		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new GameSetup();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Host a game
		if (e.getSource().equals(hostB)) {
			if (devMode) {
				String name = JOptionPane.showInputDialog(frame, "What is your name?");
				new Battleship(findHostName(), "1", true, name);
			} else {
				// Get the supplied port, make sure it's an integer
				String port = hostPortTF.getText();
				boolean valid = true;
				for (int i = 0; i < port.length(); i++) {
					if (!Character.isDigit(port.charAt(i))) {
						valid = false;
					}
				}
				if (valid) {
					if (findHostName().equals(null)) {
						JOptionPane.showMessageDialog(frame, "Could not find host name.");
					} else {
						// Make sure the port is only 4 digits, then supply the host name and create the
						// server
						if (Integer.parseInt(port) < 10000) {
							String[] options = { "Copy", "OK" };
							// If they want to copy, add their computer name to clipboard
							if (JOptionPane.showOptionDialog(frame, "Host name: " + findHostName(), "Message",
									JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options,
									null) == 0) {
								StringSelection stringSelection = new StringSelection(findHostName());
								Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
								clipboard.setContents(stringSelection, null);
							}
							String name = JOptionPane.showInputDialog(frame, "What is your name?");
							new Battleship(findHostName(), port, true, name); // the "true" means it is a server
							// TODO - names
						} else {
							JOptionPane.showMessageDialog(frame, "Port number must less than 10000");
						}
					}
				} else {
					JOptionPane.showMessageDialog(frame, "Port number must be an integer.");
				}
			}
		}
		// Join a game
		if (e.getSource().equals(joinB)) {
			String name = JOptionPane.showInputDialog(frame, "What is your name?");
			if (devMode) {
				new Battleship(findHostName(), "1", false, name);
			} else {
				// Get the host name and port, and try to connect to the server
				String hostName = joinNameTF.getText();
				String port = joinPortTF.getText();
				new Battleship(hostName, port, false, name); // the "false" means it is a client
			}
		}
		// Tutorial for how to set up a game
		if (e.getSource().equals(helpB)) {
			if (JOptionPane.showOptionDialog(frame, "Would you like a quick tutorial for how to set up a game?",
					"Tutorial", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null,
					null) == JOptionPane.YES_OPTION) {
				final JOptionPane pane = new JOptionPane();
				final JDialog d = pane.createDialog(frame, "Tutorial");

				// Five messages:
				pane.setMessage("If you want to be the host, start by entering\nany number (up to 4 digits) here.");
				d.setLocation(getRelativeX(hostPortTF) + 100, getRelativeY(hostPortTF) - 100);
				d.setVisible(true);

				pane.setMessage("Then click this host button, and you will\nbe given your host name.");
				d.setLocation(getRelativeX(hostB) - 30, getRelativeY(hostB) - 130);
				d.setVisible(true);

				pane.setMessage("Communicate that host name and the port\nnumber you chose to the second player.");
				d.setLocation(getRelativeX(helpB) - 110, getRelativeY(helpB) - 40);
				d.setVisible(true);

				pane.setMessage("The second player will then place the port\nnumber and host name in these boxes.");
				d.setLocation(getRelativeX(joinNameTF) + 97, getRelativeY(joinNameTF) - 100);
				d.setVisible(true);

				pane.setMessage("Once they click this Join button, you're\nall set!");
				d.setLocation(getRelativeX(joinB) - 30, getRelativeY(joinB) - 130);
				d.setVisible(true);
			}
		}
	}

	// Finds and returns the name of the computer (used to connect the socket to the
	// server)
	public String findHostName() {
		InetAddress ip;
		String hostName;
		try {
			ip = InetAddress.getLocalHost();
			hostName = ip.getHostName();
			return hostName;

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	// Gets the x value of component c relative to where the window is on the
	// computer
	public int getRelativeX(Component c) {
		return (int) c.getLocationOnScreen().getX();
	}

	// Gets the y value of component c relative to where the window is on the
	// computer
	public int getRelativeY(Component c) {
		return (int) c.getLocationOnScreen().getY();
	}
}
