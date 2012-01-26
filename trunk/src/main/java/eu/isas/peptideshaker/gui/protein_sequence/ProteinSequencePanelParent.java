package eu.isas.peptideshaker.gui.protein_sequence;

import java.util.ArrayList;
import org.jfree.chart.ChartMouseEvent;

/**
 * An interface implemented by parents of ProteinSequencePanel plots/charts.
 * Makes sure that the parent can make a global/external action when the user
 * clicks on a given annotation in the plot/chart.
 *
 * @author Harald Barsnes
 */
public interface ProteinSequencePanelParent {

    public void annotationClicked(ArrayList<ResidueAnnotation> allAnnotation, ChartMouseEvent cme);
}
