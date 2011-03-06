package eu.isas.peptideshaker.utils;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * A cell renderer to use for the FragmentIonTable making it possible to
 * highlight certain cells.
 *
 * @author Harald Barsnes
 */
public class FragmentIonTableCellRenderer implements TableCellRenderer {

    /**
     * The background color to use.
     */
    private Color backgroundColor;
    /**
     * The foreground color to use.
     */
    private Color foregroundColor;
    /**
     * The row indices to hightlight.
     */
    private ArrayList<Integer> indices;
    /**
     * The number formatting to use for double values.
     */
    private DecimalFormat numberFormat;

    /**
     * Creates a new FragmentIonTableCellRenderer.
     *
     * @param indices      the indices to highlight
     * @param background   the highlight color
     * @param foreground   the foreground color
     */
    public FragmentIonTableCellRenderer(ArrayList<Integer> indices, Color background, Color foreground) {
        this.indices = indices;
        this.backgroundColor = background;
        this.foregroundColor = foreground;

        // make sure that floating numbers are always shown using four decimals
        numberFormat = new DecimalFormat("0.0000");
        numberFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
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

        label.setHorizontalAlignment(SwingConstants.RIGHT);

        if (indices.contains(new Integer(row)) && !isSelected) {
            label.setBackground(backgroundColor);
            label.setForeground(foregroundColor);
        }

        if (value instanceof Double) {
            label.setText(numberFormat.format(value));
        }

        return label;
    }
}
