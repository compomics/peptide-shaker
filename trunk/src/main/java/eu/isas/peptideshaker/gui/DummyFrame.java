package eu.isas.peptideshaker.gui;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;

/**
 * A simple class used to be able to show a JDialog without a parent frame in
 * the OS taskbar.
 */
public class DummyFrame extends JFrame {

    List<Image> ICONS = Arrays.asList(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

    public DummyFrame(String title) {
        super(title);
        setUndecorated(true);
        setVisible(true);
        setLocationRelativeTo(null);
        setIconImages(ICONS);
    }
}
