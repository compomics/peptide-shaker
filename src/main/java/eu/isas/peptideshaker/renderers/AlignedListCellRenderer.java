package eu.isas.peptideshaker.renderers;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * ListCellRenderer with alignment functionality.
 *
 * @author Harald Barsnes
 */
public class AlignedListCellRenderer extends DefaultListCellRenderer {

    /**
     * One of the following constants defined in SwingConstants: LEFT, CENTER
     * (the default for image-only labels), RIGHT, LEADING (the default for
     * text-only labels) or TRAILING.
     */
    private int align;

    /**
     * Creates a new AlignedListCellRenderer
     *
     * @param align SwingConstant: LEFT, CENTER, RIGHT, LEADING or TRAILING.
     */
    public AlignedListCellRenderer(int align) {
        this.align = align;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (value == null) {
            value = "- not used -";
        }

        JLabel lbl = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        // set the standard horizontal alignment
        lbl.setHorizontalAlignment(align);

        return lbl;
    }
}
