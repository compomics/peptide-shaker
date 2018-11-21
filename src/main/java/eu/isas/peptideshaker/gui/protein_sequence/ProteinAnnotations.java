package eu.isas.peptideshaker.gui.protein_sequence;

import java.util.ArrayList;
import java.util.HashMap;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * This class generates the tooltips for a ProteinSequencePanel.
 *
 * @author Harald Barsnes
 */
public class ProteinAnnotations implements CategoryToolTipGenerator {

    /**
     * The hashmap of protein annotations.
     */
    private final HashMap<Integer, ArrayList<ResidueAnnotation>> proteinAnnotations;

    /**
     * Create a new ProteinAnnotations object.
     *
     * @param proteinAnnotations a list of the protein residue annotations
     */
    public ProteinAnnotations(HashMap<Integer, ArrayList<ResidueAnnotation>> proteinAnnotations) {
        this.proteinAnnotations = proteinAnnotations;
    }

    @Override
    public String generateToolTip(CategoryDataset cd, int index, int i1) {

        int blockNumber = index;

        StringBuilder tooltip = new StringBuilder();

        if (proteinAnnotations.containsKey(blockNumber)) {

            ArrayList<ResidueAnnotation> annotation = proteinAnnotations.get(blockNumber);

            tooltip.append("<html>");

            if (annotation != null) {
                if (annotation.size() == 1) {
                    tooltip.append(annotation.get(0).annotation);
                } else {
                    for (int i = 0; i < annotation.size(); i++) {
                        tooltip.append((i + 1));
                        tooltip.append(": ");
                        tooltip.append(annotation.get(i).annotation);
                        tooltip.append("<br>");
                    }
                }
            }

            tooltip.append("</html>");
        }

        if (tooltip.length() == 0) {
            return null;
        } else {
            return tooltip.toString();
        }
    }
}
