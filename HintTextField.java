import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

/*
 * Description
 * -----------
 * This class is for the JTextFields in the
 * GameSetup class so the text in them will
 * disappear when a user clicks into the
 * text field.
 * 
 * Authors
 * -------
 * Ally Delgado and Andy Fleischer
 * 
 * Date
 * ----
 * Last updated: June 7, 2020
 */

public class HintTextField extends JTextField implements FocusListener {

	private static final long serialVersionUID = 1L;
	private final String hint;
	private boolean showingHint;
	private Font hintFont = new Font("Verdana", Font.ITALIC, 20);
	private Font mainFont = new Font("Verdana", Font.PLAIN, 20);

	//On creation, set font and color, and show the hint
	public HintTextField(final String hint) {
		super(hint);
		super.setFont(hintFont);
		super.setForeground(Color.GRAY);
		this.hint = hint;
		this.showingHint = true;
		super.addFocusListener(this);
	}

	//If you click into the textField, remove the hint and change font style + color
	@Override
	public void focusGained(FocusEvent e) {
		if (this.getText().isEmpty()) {
			super.setFont(mainFont);
			super.setForeground(Color.BLACK);
			super.setText("");
			showingHint = false;
		}
	}
	
	//If you click out of the textField, if there is no text, add the hint back with the proper font style + color
	@Override
	public void focusLost(FocusEvent e) {
		if (this.getText().isEmpty()) {
			super.setFont(hintFont);
			super.setForeground(Color.GRAY);
			super.setText(hint);
			showingHint = true;
		}
	}

	//Override the getText function, to add that if we have been showing the hint (meaning there is no user-given text), return nothing
	@Override
	public String getText() {
		return showingHint ? "" : super.getText();
	}
}