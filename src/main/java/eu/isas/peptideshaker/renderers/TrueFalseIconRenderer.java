package eu.isas.peptideshaker.renderers;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * A render that displays icons instead of true or false, Assumes that
 * the cell values are of type Boolean.
 *
 * @author Harald Barsnes
 */
public class TrueFalseIconRenderer implements TableCellRenderer {

    /**
     * A reference to a standard table cell renderer.
     */
    private TableCellRenderer delegate;
    /**
     * The icon to use for the true values.
     */
    private ImageIcon trueIcon;
    /**
     * The icon to use for the false values.
     */
    private ImageIcon falseIcon;

    /**
     * Creates a new IconRenderer.
     *
     * @param trueIcon the icon to use for cells containing TRUE, can be null
     * @param falseIcon the icon to use for cells containing FALSE, can be null
     */
    public TrueFalseIconRenderer(ImageIcon trueIcon, ImageIcon falseIcon) {
        this.delegate = new DefaultTableCellRenderer();
        this.trueIcon = trueIcon;
        this.falseIcon = falseIcon;
    }

    /**
     * Sets up the cell renderer for the given component.
     *
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return the rendered cell
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = delegate.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        
        if (c instanceof JLabel && (Boolean) value == true) {
            ((JLabel) c).setIcon(trueIcon);
            ((JLabel) c).setText(null);
        } else {
            ((JLabel) c).setIcon(falseIcon);
            ((JLabel) c).setText(null);
        }

        ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);

        return c;
    }
}
