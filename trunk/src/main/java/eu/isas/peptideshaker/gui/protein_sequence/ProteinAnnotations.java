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
    private HashMap<Integer, ArrayList<ResidueAnnotation>> proteinAnnotations;

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

        String tooltip = null;

        if (proteinAnnotations.containsKey(blockNumber)) {

            ArrayList<ResidueAnnotation> annotation = proteinAnnotations.get(blockNumber);

            tooltip = "<html>";

            if (annotation.size() == 1) {
                tooltip += annotation.get(0).getAnnotation();
            } else {
                for (int i = 0; i < annotation.size(); i++) {
                    tooltip += (i + 1) + ": " + annotation.get(i).getAnnotation() + "<br>";
                }
            }

            tooltip += "</html>";
        }

        return tooltip;
    }
}
