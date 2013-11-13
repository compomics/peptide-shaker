package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import static eu.isas.peptideshaker.followup.RecalibrationExporter.getRecalibratedFileName;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.recalibration.SpectrumRecalibrator;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoySeries;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class exports de novo training files.
 *
 * @author Marc Vaudel
 */
public class PepnovoTrainingExport {

    /**
     * Suffix for the mgf file containing annotated spectra making the "good
     * training" set.
     */
    public static final String goodTrainingSetSuffix = "_good_training";
    /**
     * Suffix for the mgf file containing spectra making the "bad training" set.
     */
    public static final String badTrainingSetSuffix = "_bad_training";

    /**
     * Exports the PepNovo training files using the validation settings,
     * eventually recalibrated with the recalibrated mgf.
     *
     * @param destinationFolder the folder where to write the output files
     * @param identification the identification
     * @param annotationPreferences the annotation preferences. Only necessary
     * in recalibration mode.
     * @param recalibrate boolean indicating whether the files shall be
     * recalibrated
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportPepnovoTrainingFiles(File destinationFolder, Identification identification, AnnotationPreferences annotationPreferences,
            boolean recalibrate, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {
        exportPepnovoTrainingFiles(destinationFolder, identification, annotationPreferences, null, null, recalibrate, waitingHandler);
    }

    /**
     * Exports the PepNovo training files, eventually recalibrated with the
     * recalibrated mgf.
     *
     * @param destinationFolder the folder where to write the output files
     * @param identification the identification
     * @param annotationPreferences the annotation preferences. Only necessary
     * in recalibration mode.
     * @param fdr the false discovery rate to use for the selection of "good"
     * spectra. Can be null, then the value used for validation is used.
     * @param fnr the false negative rate to use for the selection fo "bad"
     * spectra. Can be null, then the same value as for the fdr parameter is
     * used.
     * @param recalibrate boolean indicating whether the files shall be
     * recalibrated
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportPepnovoTrainingFiles(File destinationFolder, Identification identification, AnnotationPreferences annotationPreferences, Double fdr, Double fnr,
            boolean recalibrate, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        SpectrumRecalibrator spectrumRecalibrator = new SpectrumRecalibrator();

        PSMaps psMaps = new PSMaps();
        psMaps = (PSMaps) identification.getUrParam(psMaps);
        PsmSpecificMap psmTargetDecoyMap = psMaps.getPsmSpecificMap();
        HashMap<Integer, Double> highConfidenceThresholds = new HashMap<Integer, Double>();
        HashMap<Integer, Double> lowConfidenceThresholds = new HashMap<Integer, Double>();
        
        for (Integer key : psmTargetDecoyMap.getKeys().keySet()) {
            double fdrThreshold, fnrThreshold;
            TargetDecoyResults currentResults = psmTargetDecoyMap.getTargetDecoyMap(key).getTargetDecoyResults();
            if (fdr == null) {
                if (currentResults.getInputType() == 1) {
                    fdrThreshold = currentResults.getUserInput();
                } else {
                    fdrThreshold = currentResults.getFdrLimit();
                }
            } else {
                fdrThreshold = fdr;
                currentResults = new TargetDecoyResults();
                currentResults.setClassicalEstimators(true);
                currentResults.setClassicalValidation(true);
                currentResults.setFdrLimit(fdrThreshold);
                TargetDecoySeries currentSeries = psmTargetDecoyMap.getTargetDecoyMap(key).getTargetDecoySeries();
                currentSeries.getFDRResults(currentResults);
            }
            highConfidenceThresholds.put(key, currentResults.getConfidenceLimit());
            if (fnr == null) {
                fnrThreshold = fdrThreshold;
            } else {
                fnrThreshold = fnr;
            }
            currentResults = new TargetDecoyResults();
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFnrLimit(fnrThreshold);
            TargetDecoySeries currentSeries = psmTargetDecoyMap.getTargetDecoyMap(key).getTargetDecoySeries();
            currentSeries.getFNRResults(currentResults);
            lowConfidenceThresholds.put(key, currentResults.getConfidenceLimit());
        }

        int progress = 1;

        for (String fileName : spectrumFactory.getMgfFileNames()) {

            if (recalibrate) {
                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        break;
                    }

                    waitingHandler.setWaitingText("Recalibrating Spectra. Please Wait... (" + progress + "/" + spectrumFactory.getMgfFileNames().size() + ")");
                    waitingHandler.setSecondaryProgressCounter(0);
                    waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                    waitingHandler.setMaxSecondaryProgressCounter(2 * spectrumFactory.getNSpectra(fileName));
                }

                spectrumRecalibrator.estimateErrors(fileName, identification, annotationPreferences, waitingHandler);
            }

            PSParameter psParameter = new PSParameter();
            identification.loadSpectrumMatchParameters(fileName, psParameter, waitingHandler);

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.setWaitingText("Selecting Good PSMs. Please Wait... (" + progress + "/" + spectrumFactory.getMgfFileNames().size() + ")");
                // reset the progress bar
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(spectrumFactory.getSpectrumTitles(fileName).size());
            }

            ArrayList<String> keys = new ArrayList<String>();
            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(fileName)) {

                String spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                if (identification.matchExists(spectrumKey)) {
                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                    double confidenceLevel = highConfidenceThresholds.get(psmTargetDecoyMap.getCorrectedKey(psParameter.getSpecificMapKey()));
                    if (psParameter.getPsmConfidence() >= confidenceLevel) {
                        keys.add(spectrumKey);
                    }
                }
                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }
            }
            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.setWaitingText("Loading PSMs. Please Wait... (" + progress + "/"
                        + spectrumFactory.getMgfFileNames().size() + ")");
            }
            identification.loadSpectrumMatches(keys, waitingHandler);

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.setWaitingText("Exporting PepNovo Training Files. Please Wait... (" + progress + "/" + spectrumFactory.getMgfFileNames().size() + ").");
                // reset the progress bar
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(spectrumFactory.getSpectrumTitles(fileName).size());
            }

            File file = new File(destinationFolder, getGoodSetFileName(fileName));
            BufferedWriter writerGood = new BufferedWriter(new FileWriter(file));
            file = new File(destinationFolder, getBadSetFileName(fileName));
            BufferedWriter writerBad = new BufferedWriter(new FileWriter(file));
            BufferedWriter writerRecalibration = null;
            if (recalibrate) {
                file = new File(destinationFolder, getRecalibratedFileName(fileName));
                writerRecalibration = new BufferedWriter(new FileWriter(file));
            }

            try {
                for (String spectrumTitle : spectrumFactory.getSpectrumTitles(fileName)) {

                    String spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);

                    MSnSpectrum spectrum = null;
                    if (recalibrate) {
                        spectrum = spectrumRecalibrator.recalibrateSpectrum(fileName, spectrumTitle, true, true);
                        spectrum.writeMgf(writerRecalibration);
                    }
                    if (identification.matchExists(spectrumKey)) {
                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                        double confidenceLevel = highConfidenceThresholds.get(psmTargetDecoyMap.getCorrectedKey(psParameter.getSpecificMapKey()));
                        if (psParameter.getPsmConfidence() >= confidenceLevel) {
                            if (spectrum == null) {
                                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                            }
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                            HashMap<String, String> tags = new HashMap<String, String>();
                            tags.put("SEQ", sequence);
                            spectrum.writeMgf(writerGood, tags);
                        }
                        confidenceLevel = lowConfidenceThresholds.get(psmTargetDecoyMap.getCorrectedKey(psParameter.getSpecificMapKey()));
                        if (psParameter.getPsmConfidence() <= confidenceLevel) {
                            if (spectrum == null) {
                                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                            }
                            spectrum.writeMgf(writerBad);
                        }
                    }

                    if (waitingHandler != null) {
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                        waitingHandler.increaseSecondaryProgressCounter();
                    }
                }
            } finally {
                writerBad.close();
                writerGood.close();
                if (writerRecalibration != null) {
                    writerRecalibration.close();
                }
            }

            spectrumRecalibrator.clearErrors(fileName);
            progress++;
        }
    }

    /**
     * Returns the name of the good training set file.
     *
     * @param fileName the original file name
     * @return the name of the good training set file
     */
    public static String getGoodSetFileName(String fileName) {
        return Util.appendSuffix(fileName, goodTrainingSetSuffix);
    }

    /**
     * Returns the name of the bad training set file.
     *
     * @param fileName the original file name
     * @return the name of the bad training set file
     */
    public static String getBadSetFileName(String fileName) {
        return Util.appendSuffix(fileName, badTrainingSetSuffix);
    }
}
