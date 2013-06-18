package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.recalibration.RunMzDeviation;
import eu.isas.peptideshaker.recalibration.SpectrumRecalibrator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class exports recalibrated spectra.
 *
 * @author Marc Vaudel
 */
public class RecalibrationExporter {

    /**
     * Boolean indicating whether the exporter shall be used in debug mode.
     *
     * The debug mode exports the ion distributions and the titles of the
     * processed spectra
     */
    private static boolean debug = true;
    /**
     * Suffix for the mgf file containing all recalibrated spectra.
     */
    public static final String recalibrated = "_recalibrated";

    /**
     * Writes the recalibrated spectra in files named according to
     * getRecalibratedFileName in the given folder.
     *
     * @param recalibratePrecursors boolean indicating whether precursor ions
     * shall be recalibrated
     * @param recalibrateFragmentIons boolean indicating whether fragment ions
     * shall be recalibrated
     * @param folder folder where recalibrated files shall be written
     * @param identification identification of the project
     * @param annotationPreferences the spectrum annotation preferences
     * @param waitingHandler waiting handler displaying progress and used to
     * cancel the process. Can be null. The method does not call RunFinished.
     *
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void writeRecalibratedSpectra(boolean recalibratePrecursors, boolean recalibrateFragmentIons, File folder,
            Identification identification, AnnotationPreferences annotationPreferences, WaitingHandler waitingHandler)
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        SpectrumRecalibrator spectrumRecalibrator = new SpectrumRecalibrator();

        int progress = 1;

        for (String fileName : spectrumFactory.getMgfFileNames()) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    break;
                }

                waitingHandler.setWaitingText("Recalibrating Spectra. Inspecting Mass Deviations. Please Wait... (" + progress + "/" + spectrumFactory.getMgfFileNames().size() + ")");
                waitingHandler.resetSecondaryProgressBar();
                waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                waitingHandler.setMaxSecondaryProgressValue(2 * spectrumFactory.getNSpectra(fileName));
            }

            spectrumRecalibrator.estimateErrors(fileName, identification, annotationPreferences, waitingHandler);

            // Debug part
            if (debug) {

                RunMzDeviation runMzDeviation = spectrumRecalibrator.getRunMzDeviations(fileName);

                File debugFile = new File(folder, "debug" + getRecalibratedFileName(fileName) + "_precursors.txt");
                BufferedWriter debugWriter = new BufferedWriter(new FileWriter(debugFile));
                debugWriter.write("rt\tgrade\toffset");
                debugWriter.newLine();

                for (double key : runMzDeviation.getPrecursorRTList()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    debugWriter.write(key + "\t");
                    debugWriter.write(runMzDeviation.getSlope(key) + "\t");
                    debugWriter.write(runMzDeviation.getOffset(key) + "\t");
                    debugWriter.newLine();
                }

                debugWriter.flush();
                debugWriter.close();

                debugFile = new File(folder, getRecalibratedFileName(fileName) + "_fragments.txt");
                debugWriter = new BufferedWriter(new FileWriter(debugFile));

                for (double rtKey : runMzDeviation.getPrecursorRTList()) {

                    debugWriter.write(rtKey + "\nm/z");

                    for (double mzKey : runMzDeviation.getFragmentMZList(rtKey)) {

                        debugWriter.write("\t" + mzKey);

                    }
                    debugWriter.newLine();
                    debugWriter.write("Error");

                    for (double mzKey : runMzDeviation.getFragmentMZList(rtKey)) {

                        debugWriter.write("\t" + runMzDeviation.getFragmentMzError(rtKey, mzKey));

                    }

                    debugWriter.newLine();
                }
                debugWriter.flush();
                debugWriter.close();
                // End of debug part
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            File file = new File(folder, getRecalibratedFileName(fileName));
            BufferedWriter writer1 = new BufferedWriter(new FileWriter(file));
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Recalibrating Spectra. Writing Spectra. Please Wait... (" + progress + "/" + spectrumFactory.getMgfFileNames().size() + ")");
            }

            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(fileName)) {

                if (debug) {
                    //System.out.println(new Date() + " recalibrating " + spectrumTitle + "\n");
                }

                MSnSpectrum recalibratedSpectrum = spectrumRecalibrator.recalibrateSpectrum(fileName, spectrumTitle, recalibratePrecursors, recalibrateFragmentIons);
                recalibratedSpectrum.writeMgf(writer1);
                writer1.flush();

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        break;
                    }
                    waitingHandler.increaseProgressValue();
                }
            }

            spectrumRecalibrator.clearErrors(fileName);
            writer1.close();
        }
    }

    /**
     * Returns the name of the recalibrated file.
     *
     * @param fileName the original file name
     * @return the name of the recalibrated file
     */
    public static String getRecalibratedFileName(String fileName) {
        return Util.appendSuffix(fileName, recalibrated);
    }
}
