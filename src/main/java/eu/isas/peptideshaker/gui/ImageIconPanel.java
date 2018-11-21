package eu.isas.peptideshaker.gui;

import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * This class extends JPanel to be able to display an ImageIcon object inside a
 * JPanel.
 * 
 * @author Harald Barsnes
 */
public class ImageIconPanel extends JPanel {

    /**
     * The image icon to display.
     */
    private final ImageIcon imageIcon;

    /**
     * Create a new ImageIconPanel.
     *
     * @param imageIcon the image icon to display
     */
    public ImageIconPanel(ImageIcon imageIcon) {
        this.imageIcon = imageIcon;
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getIconWidth(), imageIcon.getIconHeight(), null);
    }
}
