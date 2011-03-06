package eu.isas.peptideshaker.utils;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * TableCellRenderer with alignment functionality.
 *
 * @author Harald Barsnes
 */
public class AlignedTableCellRenderer implements TableCellRenderer {

    /**
     * One of the following constants defined in SwingConstants: LEFT, CENTER
     * (the default for image-only labels), RIGHT, LEADING (the default for
     * text-only labels) or TRAILING.
     */
    private int align;
    /**
     * Background color to use. If not set the default background color will 
     * be used.
     */
    private Color backgroundColor;

    /**
     * Creates a new AlignedTableCellRenderer
     *
     * @param align SwingConstant: LEFT, CENTER, RIGHT, LEADING or TRAILING.
     */
    public AlignedTableCellRenderer(int align) {
        this.align = align;
    }

    /**
     * Creates a new AlignedTableCellRenderer
     *
     * @param align         SwingConstant: LEFT, CENTER, RIGHT, LEADING or TRAILING.
     * @param background    The background color to use
     */
    public AlignedTableCellRenderer(int align, Color background) {
        this.align = align;
        this.backgroundColor = background;
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

        JLabel label = (JLabel) new DefaultTableCellRenderer().getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        Color bg = label.getBackground();
        // We have to create a new color object because Nimbus returns
        // a color of type DerivedColor, which behaves strange, not sure why.
        label.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue()));

        if (backgroundColor != null  && !isSelected) {
            label.setBackground(backgroundColor);
        }

        label.setHorizontalAlignment(align);

        return label;
    }
}
