package eu.isas.peptideshaker.gui;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;

/**
 * A simple class used to be able to show a JDialog without a parent frame in
 * the OS task bar.
 * 
 * @author Harald Barsnes
 */
public class DummyFrame extends JFrame {

    /**
     * The list of icons to use.
     */
    List<Image> ICONS = Arrays.asList(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

    /**
     * Constructor.
     * 
     * @param title the frame title
     */
    public DummyFrame(String title) {
        super(title);
        setUndecorated(true);
        setVisible(true);
        setLocationRelativeTo(null);
        setIconImages(ICONS);
    }
    
    /**
     * Update the frame title and return the frame.
     * 
     * @param title the new title
     * @return the updated dummy frame
     */
    public DummyFrame setNewTitle(String title) {
        this.setTitle(title);
        return this;
    }
}
