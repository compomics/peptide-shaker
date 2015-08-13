package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.recalibration.SpectrumRecalibrator;
import eu.isas.peptideshaker.scoring.maps.PsmSpecificMap;
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
 * This class exports algorithm training files.
 *
 * @author Marc Vaudel
 */
public class TrainingExport {

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
     * @param identificationParameters the identification parameters
     * @param recalibrate boolean indicating whether the files shall be
     * recalibrated
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     */
    public static void exportPepnovoTrainingFiles(File destinationFolder, Identification identification, IdentificationParameters identificationParameters,
            boolean recalibrate, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {
        exportPepnovoTrainingFiles(destinationFolder, identification, identificationParameters, null, null, recalibrate, waitingHandler);
    }

    /**
     * Exports the PepNovo training files, eventually recalibrated with the
     * recalibrated mgf.
     *
     * @param destinationFolder the folder where to write the output files
     * @param identification the identification
     * @param identificationParameters the identification parameters
     * @param fdr the false discovery rate to use for the selection of "good"
     * spectra. Can be null, then the value used for validation is used.
     * @param fnr the false negative rate to use for the selection fo "bad"
     * spectra. Can be null, then the same value as for the fdr parameter is
     * used.
     * @param recalibrate boolean indicating whether the files shall be
     * recalibrated
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     */
    public static void exportPepnovoTrainingFiles(File destinationFolder, Identification identification, IdentificationParameters identificationParameters, Double fdr, Double fnr,
            boolean recalibrate, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        SpectrumRecalibrator spectrumRecalibrator = new SpectrumRecalibrator();

        PSMaps psMaps = new PSMaps();
        psMaps = (PSMaps) identification.getUrParam(psMaps);
        PsmSpecificMap psmTargetDecoyMap = psMaps.getPsmSpecificMap();

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

                spectrumRecalibrator.estimateErrors(fileName, identification, identificationParameters, waitingHandler);
            }

            PSParameter psParameter = new PSParameter();
            identification.loadSpectrumMatchParameters(fileName, psParameter, waitingHandler, true);

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
                    Integer charge = new Integer(psParameter.getSpecificMapKey());
                    String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                    Double fdrThreshold = getFdrThreshold(psmTargetDecoyMap, charge, spectrumTitle, fdr);
                    double confidenceLevel = getHighConfidenceThreshold(psmTargetDecoyMap, charge, spectrumFile, fdrThreshold);
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
            identification.loadSpectrumMatches(keys, waitingHandler, true);

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
                file = new File(destinationFolder, RecalibrationExporter.getRecalibratedFileName(fileName));
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
                        Integer charge = new Integer(psParameter.getSpecificMapKey());
                        String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                        Double fdrThreshold = getFdrThreshold(psmTargetDecoyMap, charge, spectrumTitle, fdr);
                        double confidenceLevel = getHighConfidenceThreshold(psmTargetDecoyMap, charge, spectrumFile, fdrThreshold);
                        if (psParameter.getPsmConfidence() >= confidenceLevel) {
                            if (spectrum == null) {
                                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                            }
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            if (spectrumMatch.getBestPeptideAssumption() != null) {
                                String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                                HashMap<String, String> tags = new HashMap<String, String>();
                                tags.put("SEQ", sequence);
                                spectrum.writeMgf(writerGood, tags);
                            }
                        }
                        confidenceLevel = getLowConfidenceThreshold(psmTargetDecoyMap, charge, spectrumFile, fnr, fdrThreshold);
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
     * Returns the FDR threshold to be used
     *
     * @param psmSpecificMap the PSM target/decoy scoring map
     * @param charge the charge of the inspected PSM
     * @param spectrumFileName the spectrum file name
     * @param fdr a user defined FDR, can be null
     *
     * @return the FDR threshold to be used
     */
    private static double getFdrThreshold(PsmSpecificMap psmSpecificMap, int charge, String spectrumFileName, Double fdr) {
        double fdrThreshold;
        TargetDecoyResults currentResults = psmSpecificMap.getTargetDecoyMap(charge, spectrumFileName).getTargetDecoyResults();
        if (fdr == null) {
            if (currentResults.getInputType() == 1) {
                fdrThreshold = currentResults.getUserInput();
            } else {
                fdrThreshold = currentResults.getFdrLimit();
            }
        } else {
            fdrThreshold = fdr;
        }
        return fdrThreshold;
    }

    /**
     * Returns the high confidence threshold
     *
     * @param psmSpecificMap the PSM target/decoy scoring map
     * @param charge the charge of the inspected PSM
     * @param spectrumFileName the spectrum file name
     * @param fdrThreshold the FDR threshold to be used
     *
     * @return the confidence threshold corresponding to this match at the
     * desired FDR
     */
    private static double getHighConfidenceThreshold(PsmSpecificMap psmSpecificMap, int charge, String spectrumFileName, Double fdrThreshold) {
        TargetDecoyResults trainingResults = new TargetDecoyResults();
        trainingResults.setClassicalEstimators(true);
        trainingResults.setClassicalValidation(true);
        trainingResults.setFdrLimit(fdrThreshold);
        TargetDecoySeries currentSeries = psmSpecificMap.getTargetDecoyMap(charge, spectrumFileName).getTargetDecoySeries();
        currentSeries.getFDRResults(trainingResults);
        return trainingResults.getConfidenceLimit();
    }

    /**
     * Returns the low confidence threshold
     *
     * @param psmSpecificMap the PSM target/decoy scoring map
     * @param charge the charge of the inspected PSM
     * @param spectrumFileName the spectrum file name
     * @param fnr a user defined FNR threshold, can be null
     * @param fdrThreshold the FDR threshold used
     *
     * @return the confidence threshold corresponding to this match at the
     * desired FNR
     */
    private static double getLowConfidenceThreshold(PsmSpecificMap psmSpecificMap, int charge, String spectrumFileName, Double fnr, double fdrThreshold) {
        double fnrThreshold;
        if (fnr == null) {
            fnrThreshold = fdrThreshold;
        } else {
            fnrThreshold = fnr;
        }
        TargetDecoyResults trainingResults = new TargetDecoyResults();
        trainingResults.setClassicalEstimators(true);
        trainingResults.setClassicalValidation(true);
        trainingResults.setFnrLimit(fnrThreshold);
        TargetDecoySeries currentSeries = psmSpecificMap.getTargetDecoyMap(charge, spectrumFileName).getTargetDecoySeries();
        currentSeries.getFNRResults(trainingResults);
        return trainingResults.getConfidenceLimit();
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
