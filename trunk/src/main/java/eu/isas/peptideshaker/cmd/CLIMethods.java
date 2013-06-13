package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.export.RecalibrationExporter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class groups standard methods used by the different command line
 * interfaces.
 *
 * @author Marc Vaudel
 */
public class CLIMethods {

    /**
     * Recalibrates spectra if required by the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param annotationPreferences the annotation preferences
     * @param waitingHandler a waiting handler to display progress
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void recalibrateSpectra(FollowUpCLIInputBean followUpCLIInputBean, Identification identification,
            AnnotationPreferences annotationPreferences, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException {

        File recalibrationFolder = followUpCLIInputBean.getRecalibrationFolder();

        if (recalibrationFolder != null) {
            boolean ms1 = true;
            boolean ms2 = true;
            if (followUpCLIInputBean.getRecalibrationMode() == 1) {
                ms2 = false;
            } else if (followUpCLIInputBean.getRecalibrationMode() == 2) {
                ms1 = false;
            }
            RecalibrationExporter.writeRecalibratedSpectra(ms1, ms2, recalibrationFolder, identification, annotationPreferences, waitingHandler);
        }
    }
}
