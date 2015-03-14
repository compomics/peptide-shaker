package eu.isas.peptideshaker.validation;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.ValidationQCPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.RowFilter;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class validates the quality of identification matches.
 *
 * @author Marc Vaudel
 */
public class MatchesValidator {

    /**
     * The PSM target decoy map.
     */
    private PsmSpecificMap psmMap;
    /**
     * The peptide target decoy map.
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The protein target decoy map.
     */
    private ProteinMap proteinMap;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The protein sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * Constructor.
     *
     * @param psmMap the PSM target decoy map
     * @param peptideMap the peptide target decoy map
     * @param proteinMap the protein target decoy map
     */
    public MatchesValidator(PsmSpecificMap psmMap, PeptideSpecificMap peptideMap, ProteinMap proteinMap) {
        this.psmMap = psmMap;
        this.peptideMap = peptideMap;
        this.proteinMap = proteinMap;
    }

    /**
     * Validates the identification matches comprised in an identification
     * object based on the target/decoy strategy and quality control metrics
     * based on given FDR thresholds.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided, metrics on fractions will be saved while
     * iterating the matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler handler for exceptions
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator providing information about the matches
     * @param inputMap the input target/decoy map
     * @param spectrumCountingPreferences the spectrum counting preferences
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void validateIdentifications(Identification identification, Metrics metrics, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator, InputMap inputMap,
            SpectrumCountingPreferences spectrumCountingPreferences) throws SQLException, IOException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        IdMatchValidationPreferences validationPreferences = identificationParameters.getIdValidationPreferences();

        waitingHandler.setWaitingText("Finding FDR Thresholds. Please Wait...");

        TargetDecoyMap currentMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults currentResults = currentMap.getTargetDecoyResults();
        currentResults.setInputType(1);
        currentResults.setUserInput(validationPreferences.getDefaultProteinFDR());
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(validationPreferences.getDefaultProteinFDR());
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        ArrayList<TargetDecoyMap> psmMaps = psmMap.getTargetDecoyMaps(),
                inputMaps = inputMap.getTargetDecoyMaps();

        int max = peptideMap.getKeys().size() + psmMaps.size() + inputMap.getNalgorithms();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        for (String mapKey : peptideMap.getKeys()) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.increaseSecondaryProgressCounter();
            currentMap = peptideMap.getTargetDecoyMap(mapKey);
            currentResults = currentMap.getTargetDecoyResults();
            currentResults.setInputType(1);
            currentResults.setUserInput(validationPreferences.getDefaultPeptideFDR());
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(validationPreferences.getDefaultPeptideFDR());
            currentMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        for (TargetDecoyMap targetDecoyMap : psmMaps) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.increaseSecondaryProgressCounter();
            currentResults = targetDecoyMap.getTargetDecoyResults();
            currentResults.setInputType(1);
            currentResults.setUserInput(validationPreferences.getDefaultPsmFDR());
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(validationPreferences.getDefaultPsmFDR());
            targetDecoyMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        for (TargetDecoyMap targetDecoyMap : inputMaps) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.increaseSecondaryProgressCounter();
            currentResults = targetDecoyMap.getTargetDecoyResults();
            currentResults.setInputType(1);
            currentResults.setUserInput(validationPreferences.getDefaultPsmFDR());
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(validationPreferences.getDefaultPsmFDR());
            targetDecoyMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        validateIdentifications(identification, metrics, inputMap, waitingHandler,
                identificationFeaturesGenerator, shotgunProtocol, identificationParameters, spectrumCountingPreferences);

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * This method validates the identification matches of an identification
     * object. Target Decoy thresholds must be set.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided, metrics on fractions will be saved while
     * iterating the matches
     * @param inputMap the target decoy map of all search engine scores
     * @param waitingHandler the progress bar
     * @param identificationFeaturesGenerator an identification features
     * generator computing information about the identification matches
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void validateIdentifications(Identification identification, Metrics metrics, InputMap inputMap,
            WaitingHandler waitingHandler, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            SpectrumCountingPreferences spectrumCountingPreferences)
            throws SQLException, IOException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        PSParameter psParameter = new PSParameter();
        PSParameter psParameter2 = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Match Validation and Quality Control. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size()
                    + identification.getPeptideIdentification().size()
                    + 2 * identification.getSpectrumIdentificationSize());
        }

        PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
        HashMap<String, ArrayList<String>> spectrumKeysMap = identification.getSpectrumIdentificationMap();
        if (metrics.getGroupedSpectrumKeys() != null) {
            spectrumKeysMap = metrics.getGroupedSpectrumKeys();
        }

        // validate the spectrum matches
        if (inputMap != null) {
            inputMap.resetAdvocateContributions();
        }
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            ArrayList<Double> precursorMzDeviations = new ArrayList<Double>();

            ArrayList<String> spectrumKeys = spectrumKeysMap.get(spectrumFileName);

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, spectrumKeys, parameters, false, waitingHandler);

            while (psmIterator.hasNext()) {

                if (waitingHandler != null) {
                    waitingHandler.setDisplayProgress(false);
                }
                SpectrumMatch spectrumMatch = psmIterator.next();
                if (waitingHandler != null) {
                    waitingHandler.setDisplayProgress(true);
                }

                String spectrumKey = spectrumMatch.getKey();

                updateSpectrumMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, psmMap, spectrumKey, null, false);
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (peptideAssumption != null) {

                        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
                        double precursorMzError = peptideAssumption.getDeltaMass(precursorMz, shotgunProtocol.isMs1ResolutionPpm());
                        precursorMzDeviations.add(precursorMzError);

                        if (inputMap != null) {

                            Peptide bestPeptide = peptideAssumption.getPeptide();
                            ArrayList<Integer> agreementAdvocates = new ArrayList<Integer>();

                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                            for (int advocateId : assumptions.keySet()) {
                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateId);
                                if (advocateAssumptions != null) {
                                    ArrayList<Double> eValues = new ArrayList<Double>(advocateAssumptions.keySet());
                                    Collections.sort(eValues);
                                    for (SpectrumIdentificationAssumption firstHit : advocateAssumptions.get(eValues.get(0))) {
                                        if (firstHit instanceof PeptideAssumption) {
                                            Peptide advocatePeptide = ((PeptideAssumption) firstHit).getPeptide();
                                            if (bestPeptide.isSameSequenceAndModificationStatus(advocatePeptide, identificationParameters.getSequenceMatchingPreferences())) {
                                                agreementAdvocates.add(advocateId);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            boolean unique = agreementAdvocates.size() == 1;

                            for (int advocateId : agreementAdvocates) {
                                inputMap.addAdvocateContribution(advocateId, spectrumFileName, unique);
                            }

                            inputMap.addAdvocateContribution(Advocate.peptideShaker.getIndex(), spectrumFileName, agreementAdvocates.isEmpty());
                        }
                    }
                }

                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressCounter();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }

            if (inputMap != null) {
                inputMap.resetAdvocateContributions(spectrumFileName);
            }

            psmIterator = identification.getPsmIterator(spectrumFileName, spectrumKeys, parameters, false, waitingHandler);

            while (psmIterator.hasNext()) {

                if (waitingHandler != null) {
                    waitingHandler.setDisplayProgress(false);
                }
                SpectrumMatch spectrumMatch = psmIterator.next();
                if (waitingHandler != null) {
                    waitingHandler.setDisplayProgress(true);
                }

                String spectrumKey = spectrumMatch.getKey();

                updateSpectrumMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, psmMap, spectrumKey, precursorMzDeviations, true);
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (peptideAssumption != null) {
                        if (inputMap != null) {
                            Peptide bestPeptide = peptideAssumption.getPeptide();
                            ArrayList<Integer> agreementAdvocates = new ArrayList<Integer>();
                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                            for (int advocateId : assumptions.keySet()) {
                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateId);
                                for (double eValue : advocateAssumptions.keySet()) {
                                    for (SpectrumIdentificationAssumption spectrumIdAssumption : advocateAssumptions.get(eValue)) {
                                        if (spectrumIdAssumption instanceof PeptideAssumption) {
                                            Peptide advocatePeptide = ((PeptideAssumption) spectrumIdAssumption).getPeptide();
                                            if (bestPeptide.isSameSequenceAndModificationStatus(advocatePeptide, identificationParameters.getSequenceMatchingPreferences())) {
                                                agreementAdvocates.add(advocateId);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            boolean unique = agreementAdvocates.size() == 1;
                            for (int advocateId : agreementAdvocates) {
                                inputMap.addAdvocateContribution(advocateId, spectrumFileName, unique);
                            }
                            inputMap.addAdvocateContribution(Advocate.peptideShaker.getIndex(), spectrumFileName, agreementAdvocates.isEmpty());
                        }
                    }
                }

                // go through the peptide assumptions
                if (inputMap != null) { //backward compatibility check
                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                    for (int advocateId : assumptions.keySet()) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateId);
                        for (double eValue : advocateAssumptions.keySet()) {
                            for (SpectrumIdentificationAssumption spectrumIdAssumption : advocateAssumptions.get(eValue)) {
                                if (spectrumIdAssumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdAssumption;
                                    updatePeptideAssumptionValidationLevel(identificationFeaturesGenerator, shotgunProtocol, identificationParameters, inputMap, spectrumKey, peptideAssumption, peptideSpectrumAnnotator, precursorMzDeviations);
                                } else if (spectrumIdAssumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) spectrumIdAssumption;
                                    updateTagAssumptionValidationLevel(identificationFeaturesGenerator, shotgunProtocol, identificationParameters, inputMap, spectrumKey, tagAssumption);
                                }
                            }
                        }
                    }
                }

                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressCounter();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }
        }

        HashMap<String, Integer> validatedTotalPeptidesPerFraction = new HashMap<String, Integer>();
        ArrayList<Double> validatedPeptideLengths = new ArrayList<Double>();

        // validate the peptides
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, false, parameters, waitingHandler);
        while (peptideMatchesIterator.hasNext()) {

            if (waitingHandler != null) {
                waitingHandler.setDisplayProgress(false);
            }
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            if (waitingHandler != null) {
                waitingHandler.setDisplayProgress(true);
            }

            String peptideKey = peptideMatch.getKey();

            updatePeptideMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideMap, peptideKey);

            // set the fraction details
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (psParameter.getMatchValidationLevel().isValidated()) {
                double length = Peptide.getSequence(peptideKey).length();
                validatedPeptideLengths.add(length);
            }

            // @TODO: could be a better more elegant way of doing this?
            HashMap<String, Integer> validatedPsmsPerFraction = new HashMap<String, Integer>();
            HashMap<String, ArrayList<Double>> precursorIntensitesPerFractionPeptideLevel = new HashMap<String, ArrayList<Double>>();

            for (String fraction : psParameter.getFractions()) {

                ArrayList<Double> precursorIntensities = new ArrayList<Double>();

                if (metrics.getFractionPsmMatches().get(fraction + "_" + peptideKey) != null) {
                    ArrayList<String> spectrumKeys = metrics.getFractionPsmMatches().get(fraction + "_" + peptideKey);

                    for (int k = 0; k < spectrumKeys.size(); k++) {

                        psParameter2 = (PSParameter) identification.getSpectrumMatchParameter(spectrumKeys.get(k), psParameter2);

                        if (psParameter2.getMatchValidationLevel().isValidated()) {
                            if (validatedPsmsPerFraction.containsKey(fraction)) {
                                Integer value = validatedPsmsPerFraction.get(fraction);
                                validatedPsmsPerFraction.put(fraction, value + 1);
                            } else {
                                validatedPsmsPerFraction.put(fraction, 1);
                            }

                            if (SpectrumFactory.getInstance().getPrecursor(spectrumKeys.get(k)).getIntensity() > 0) { // @TODO: replace by an mgf index map? (have to add intensity map to the index first...)
                                precursorIntensities.add(SpectrumFactory.getInstance().getPrecursor(spectrumKeys.get(k)).getIntensity());
                            }
                        }

                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                        }
                    }
                }

                precursorIntensitesPerFractionPeptideLevel.put(fraction, precursorIntensities);

                // save the total number of peptides per fraction
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    if (validatedTotalPeptidesPerFraction.containsKey(fraction)) {
                        Integer value = validatedTotalPeptidesPerFraction.get(fraction);
                        validatedTotalPeptidesPerFraction.put(fraction, value + 1);
                    } else {
                        validatedTotalPeptidesPerFraction.put(fraction, 1);
                    }
                }
            }

            // set the number of validated spectra per fraction for each peptide
            psParameter.setFractionValidatedSpectra(validatedPsmsPerFraction);
            psParameter.setPrecursorIntensityPerFraction(precursorIntensitesPerFractionPeptideLevel);

            identification.updatePeptideMatchParameter(peptideKey, psParameter);
            if (waitingHandler != null) {
                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }
        }

        if (validatedPeptideLengths.size() >= 100) {
            NonSymmetricalNormalDistribution lengthDistribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistribution(validatedPeptideLengths);
            metrics.setPeptideLengthDistribution(lengthDistribution);
        }

        // validate the proteins
        TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
        double fdrLimit = targetDecoyResults.getFdrLimit();
        double nTargetLimit = 100.0 / fdrLimit;
        double proteinThreshold = targetDecoyResults.getScoreLimit();
        double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
        double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + margin;
        if (proteinConfidentThreshold > 100) {
            proteinConfidentThreshold = 100;
        }
        boolean noValidated = proteinMap.getTargetDecoyMap().getTargetDecoyResults().noValidated();

        int maxValidatedSpectraFractionLevel = 0;
        int maxValidatedPeptidesFractionLevel = 0;
        double maxProteinAveragePrecursorIntensity = 0.0, maxProteinSummedPrecursorIntensity = 0.0;
        double totalSpectrumCountingMass = 0.0;

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, false, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            if (waitingHandler != null) {
                waitingHandler.setDisplayProgress(false);
            }
            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            if (waitingHandler != null) {
                waitingHandler.setDisplayProgress(true);
            }

            String proteinKey = proteinMatch.getKey();
            updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters,
                    targetDecoyMap, proteinThreshold, nTargetLimit, proteinConfidentThreshold, noValidated, proteinKey);

            // set the fraction details
            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

            if (!proteinMatch.isDecoy() && psParameter.getMatchValidationLevel().isValidated()) {
                double tempSpectrumCounting = identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                double molecularWeight = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                double massContribution = molecularWeight * tempSpectrumCounting;
                totalSpectrumCountingMass += massContribution;
            }

            // @TODO: could be a better more elegant way of doing this?
            HashMap<String, Integer> validatedPsmsPerFraction = new HashMap<String, Integer>();
            HashMap<String, Integer> validatedPeptidesPerFraction = new HashMap<String, Integer>();
            HashMap<String, ArrayList<Double>> precursorIntensitesPerFractionProteinLevel = new HashMap<String, ArrayList<Double>>();
            ArrayList<String> peptideKeys = identification.getProteinMatch(proteinKey).getPeptideMatchesKeys();
            identification.loadPeptideMatchParameters(peptideKeys, psParameter, null);

            for (String currentPeptideKey : peptideKeys) {

                psParameter2 = (PSParameter) identification.getPeptideMatchParameter(currentPeptideKey, psParameter2);

                for (String fraction : psParameter2.getFractions()) {

                    if (psParameter2.getFractionValidatedSpectra(fraction) != null) {
                        if (validatedPsmsPerFraction.containsKey(fraction)) {
                            Integer value = validatedPsmsPerFraction.get(fraction);
                            validatedPsmsPerFraction.put(fraction, value + psParameter2.getFractionValidatedSpectra(fraction));
                        } else {
                            validatedPsmsPerFraction.put(fraction, psParameter2.getFractionValidatedSpectra(fraction));
                        }

                        if (validatedPsmsPerFraction.get(fraction) > maxValidatedSpectraFractionLevel) {
                            maxValidatedSpectraFractionLevel = validatedPsmsPerFraction.get(fraction);
                        }
                    }

                    if (psParameter2.getPrecursorIntensityPerFraction(fraction) != null) {
                        if (precursorIntensitesPerFractionProteinLevel.containsKey(fraction)) {
                            for (int i = 0; i < psParameter2.getPrecursorIntensityPerFraction(fraction).size(); i++) {
                                precursorIntensitesPerFractionProteinLevel.get(fraction).add(psParameter2.getPrecursorIntensityPerFraction(fraction).get(i));
                            }
                        } else {
                            precursorIntensitesPerFractionProteinLevel.put(fraction, psParameter2.getPrecursorIntensityPerFraction(fraction));
                        }
                    }

                    if (psParameter2.getMatchValidationLevel().isValidated()) {
                        if (validatedPeptidesPerFraction.containsKey(fraction)) {
                            Integer value = validatedPeptidesPerFraction.get(fraction);
                            validatedPeptidesPerFraction.put(fraction, value + 1);
                        } else {
                            validatedPeptidesPerFraction.put(fraction, 1);
                        }

                        if (validatedPeptidesPerFraction.get(fraction) > maxValidatedPeptidesFractionLevel) {
                            maxValidatedPeptidesFractionLevel = validatedPeptidesPerFraction.get(fraction);
                        }
                    }
                }

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }

            // set the number of validated spectra per fraction for each peptide
            psParameter.setFractionValidatedSpectra(validatedPsmsPerFraction);
            psParameter.setFractionValidatedPeptides(validatedPeptidesPerFraction);
            psParameter.setPrecursorIntensityPerFraction(precursorIntensitesPerFractionProteinLevel);

            for (String fraction : psParameter.getFractions()) {
                if (psParameter.getPrecursorIntensityAveragePerFraction(fraction) != null) {
                    if (psParameter.getPrecursorIntensityAveragePerFraction(fraction) > maxProteinAveragePrecursorIntensity) {
                        maxProteinAveragePrecursorIntensity = psParameter.getPrecursorIntensityAveragePerFraction(fraction);
                    }
                    if (psParameter.getPrecursorIntensityAveragePerFraction(fraction) > maxProteinSummedPrecursorIntensity) {
                        maxProteinAveragePrecursorIntensity = psParameter.getPrecursorIntensitySummedPerFraction(fraction);
                    }
                }
            }

            identification.updateProteinMatchParameter(proteinKey, psParameter);

            if (waitingHandler != null) {
                waitingHandler.increaseSecondaryProgressCounter();
            }
        }

        // set the max values in the metrics
        metrics.setMaxValidatedPeptidesPerFraction(maxValidatedPeptidesFractionLevel);
        metrics.setMaxValidatedSpectraPerFraction(maxValidatedSpectraFractionLevel);
        metrics.setMaxProteinAveragePrecursorIntensity(maxProteinAveragePrecursorIntensity);
        metrics.setMaxProteinSummedPrecursorIntensity(maxProteinSummedPrecursorIntensity);
        metrics.setTotalPeptidesPerFraction(validatedTotalPeptidesPerFraction);
        metrics.setTotalSpectrumCountingMass(totalSpectrumCountingMass);
    }

    /**
     * Updates the validation status of a protein match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param proteinMap the protein level target/decoy scoring map
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinKey the key of the protein match of interest
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ProteinMap proteinMap, String proteinKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();
        TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
        double fdrLimit = targetDecoyResults.getFdrLimit();
        double nTargetLimit = 100.0 / fdrLimit;
        double proteinThreshold = targetDecoyResults.getScoreLimit();
        double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
        double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + margin;

        if (proteinConfidentThreshold > 100) {
            proteinConfidentThreshold = 100;
        }

        boolean noValidated = proteinMap.getTargetDecoyMap().getTargetDecoyResults().noValidated();
        updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters,
                targetDecoyMap, proteinThreshold, nTargetLimit, proteinConfidentThreshold, noValidated, proteinKey);
    }

    /**
     * Updates the validation status of a protein match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param targetDecoyMap the protein level target/decoy map
     * @param scoreThreshold the validation score doubtfulThreshold
     * @param confidenceThreshold the confidence doubtfulThreshold after which a
     * match should be considered as confident
     * @param noValidated boolean indicating whether no validation was actually
     * conducted
     * @param nTargetLimit the limit in number of target hits before the first
     * decoy hit
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinKey the key of the protein match of interest
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, TargetDecoyMap targetDecoyMap, double scoreThreshold, double nTargetLimit,
            double confidenceThreshold, boolean noValidated,
            String proteinKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (!psParameter.isManualValidation()) {

            if (sequenceFactory.concatenatedTargetDecoy()) {

                if (!noValidated && psParameter.getProteinProbabilityScore() <= scoreThreshold) {
                    String reasonDoubtful = null;
                    boolean filterPassed = true;
                    for (Filter filter : validationQCPreferences.getProteinFilters()) {
                        ProteinFilter proteinFilter = (ProteinFilter) filter;
                        boolean validation = proteinFilter.isValidated(proteinKey, identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters);
                        psParameter.setQcResult(filter.getName(), validation);
                        if (!validation) {
                            filterPassed = false;
                            if (reasonDoubtful == null) {
                                reasonDoubtful = "";
                            } else {
                                reasonDoubtful += ", ";
                            }
                            reasonDoubtful += filter.getDescription();
                        }
                    }
                    boolean confidenceThresholdPassed = psParameter.getProteinConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?
                    if (!confidenceThresholdPassed) {
                        if (reasonDoubtful == null) {
                            reasonDoubtful = "";
                        } else {
                            reasonDoubtful += ", ";
                        }
                        reasonDoubtful += "Low confidence";
                    }
                    boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;
                    if (!enoughHits) {
                        if (reasonDoubtful == null) {
                            reasonDoubtful = "";
                        } else {
                            reasonDoubtful += ", ";
                        }
                        reasonDoubtful += "Low number of hits";
                    }
                    boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();
                    if (!enoughSequences) {
                        if (reasonDoubtful == null) {
                            reasonDoubtful = "";
                        } else {
                            reasonDoubtful += ", ";
                        }
                        reasonDoubtful += "Database too small";
                    }
                    if (filterPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
                        psParameter.setMatchValidationLevel(MatchValidationLevel.confident);
                    } else {
                        psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);
                        if (reasonDoubtful != null) {
                            psParameter.setReasonDoubtful(reasonDoubtful);
                        }
                    }
                } else {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);
                }
            } else {
                psParameter.setMatchValidationLevel(MatchValidationLevel.none);
            }

            identification.updateProteinMatchParameter(proteinKey, psParameter);
        }
    }

    /**
     * Updates the validation status of a peptide match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param peptideMap the peptide level target/decoy scoring map
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param peptideKey the key of the peptide match of interest
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updatePeptideMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpecificMap peptideMap, String peptideKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {
            TargetDecoyMap targetDecoyMap = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSpecificMapKey()));
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double fdrLimit = targetDecoyResults.getFdrLimit();
            double nTargetLimit = 100.0 / fdrLimit;
            double peptideThreshold = targetDecoyResults.getScoreLimit();
            double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;
            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }
            boolean noValidated = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSpecificMapKey())).getTargetDecoyResults().noValidated();
            if (!noValidated && psParameter.getPeptideProbabilityScore() <= peptideThreshold) {
                String reasonDoubtful = null;
                boolean filterPassed = true;
                for (Filter filter : validationQCPreferences.getPeptideFilters()) {
                    PeptideFilter peptideFilter = (PeptideFilter) filter;
                    boolean validation = peptideFilter.isValidated(peptideKey, identification, identificationFeaturesGenerator);
                    psParameter.setQcResult(filter.getName(), validation);
                    if (!validation) {
                        filterPassed = false;
                        if (reasonDoubtful == null) {
                            reasonDoubtful = "";
                        } else {
                            reasonDoubtful += ", ";
                        }
                        reasonDoubtful += filter.getDescription();
                    }
                }
                boolean confidenceThresholdPassed = psParameter.getPeptideConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?
                if (!confidenceThresholdPassed) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low confidence";
                }
                boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;
                if (!enoughHits) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low number of hits";
                }
                boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();
                if (!enoughSequences) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Database too small";
                }
                if (filterPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);
                } else {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);
                    if (reasonDoubtful != null) {
                        psParameter.setReasonDoubtful(reasonDoubtful);
                    }
                }
            } else {
                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);
            }
        } else {
            psParameter.setMatchValidationLevel(MatchValidationLevel.none);
        }

        identification.updatePeptideMatchParameter(peptideKey, psParameter);
    }

    /**
     * Updates the validation status of a spectrum match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param psmMap the PSM level target/decoy scoring map
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param spectrumKey the key of the spectrum match of interest
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this psm to
     * @param applyQCFilters if true quality control filters will be used
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updateSpectrumMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            PsmSpecificMap psmMap, String spectrumKey, ArrayList<Double> precursorMzDeviations, boolean applyQCFilters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            Integer charge = new Integer(psParameter.getSpecificMapKey());
            String fileName = Spectrum.getSpectrumFile(spectrumKey);
            TargetDecoyMap targetDecoyMap = psmMap.getTargetDecoyMap(charge, fileName);
            double psmThreshold = 0;
            double confidenceThreshold = 100;
            boolean noValidated = true;
            double nTargetLimit = 100;

            if (targetDecoyMap != null) {
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double fdrLimit = targetDecoyResults.getFdrLimit();
                nTargetLimit = 100.0 / fdrLimit;
                psmThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;
                if (confidenceThreshold > 100) {
                    confidenceThreshold = 100;
                }

                noValidated = targetDecoyResults.noValidated();
            }

            if (!noValidated && psParameter.getPsmProbabilityScore() <= psmThreshold) {

                String reasonDoubtful = null;
                boolean filterPassed = true;

                if (applyQCFilters) {

                    for (Filter filter : validationQCPreferences.getPsmFilters()) {
                        PsmFilter psmFilter = (PsmFilter) filter;
                        boolean validated = psmFilter.isValidated(spectrumKey, identification, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, precursorMzDeviations);
                        psParameter.setQcResult(psmFilter.getName(), validated);
                        if (!validated) {
                            filterPassed = false;
                            if (reasonDoubtful == null) {
                                reasonDoubtful = "";
                            } else {
                                reasonDoubtful += ", ";
                            }
                            reasonDoubtful += filter.getDescription();
                        }
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getPsmConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                if (!confidenceThresholdPassed) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low confidence";
                }

                boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;

                if (!enoughHits) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low number of hits";
                }

                boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();

                if (!enoughSequences) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Database too small";
                }

                if (filterPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);
                } else {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);
                    if (reasonDoubtful != null) {
                        psParameter.setReasonDoubtful(reasonDoubtful);
                    }
                }
            } else {
                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);
            }
        } else {
            psParameter.setMatchValidationLevel(MatchValidationLevel.none);
        }

        identification.updateSpectrumMatchParameter(spectrumKey, psParameter);
    }

    /**
     * Updates the validation status of a tag assumption. If the match was
     * manually validated nothing will be changed.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param inputMap the target decoy map of all search engine scores
     * @param spectrumKey the key of the inspected spectrum
     * @param tagAssumption the tag assumption of interest
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updateTagAssumptionValidationLevel(IdentificationFeaturesGenerator identificationFeaturesGenerator, ShotgunProtocol shotgunProtocol,
            IdentificationParameters identificationParameters, InputMap inputMap, String spectrumKey, TagAssumption tagAssumption)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) tagAssumption.getUrParam(psParameter);
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(tagAssumption.getAdvocate());
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double fdrLimit = targetDecoyResults.getFdrLimit();
            double nTargetLimit = 100.0 / fdrLimit;
            double seThreshold = targetDecoyResults.getScoreLimit();
            double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;

            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }

            boolean noValidated = targetDecoyResults.noValidated();

            if (!noValidated && tagAssumption.getScore() <= seThreshold) { //@TODO: include ascending/descending scores

                String reasonDoubtful = null;
                boolean filterPassed = true;

                //TODO: implement tag quality filters
//                for (AssumptionFilter filter : inputMap.getDoubtfulMatchesFilters()) {
//                    boolean validated = filter.isValidated(spectrumKey, peptideAssumption, searchParameters, annotationPreferences);
//                    psParameter.setQcResult(filter.getName(), validated);
//                    if (!validated) {
//                        filterPassed = false;
//                        if (reasonDoubtful == null) {
//                            reasonDoubtful = "";
//                        } else {
//                            reasonDoubtful += ", ";
//                        }
//                        reasonDoubtful += filter.getDescription();
//                    }
//                }
                boolean confidenceThresholdPassed = psParameter.getSearchEngineConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                if (!confidenceThresholdPassed) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low confidence";
                }

                boolean enoughHits = targetDecoyMap.getnTargetOnly() > nTargetLimit;

                if (!enoughHits) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low number of hits";
                }

                if (!sequenceFactory.hasEnoughSequences()) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Database too small";
                }

                if (filterPassed && confidenceThresholdPassed && enoughHits && sequenceFactory.hasEnoughSequences()) {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);
                } else {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);
                    if (reasonDoubtful != null) {
                        psParameter.setReasonDoubtful(reasonDoubtful);
                    }
                }
            } else {
                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);
            }
        } else {
            psParameter.setMatchValidationLevel(MatchValidationLevel.none);
        }
    }

    /**
     * Updates the validation status of a peptide assumption. If the match was
     * manually validated nothing will be changed.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param inputMap the target decoy map of all search engine scores
     * @param spectrumKey the key of the inspected spectrum
     * @param peptideAssumption the peptide assumption of interest
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this assumption to
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updatePeptideAssumptionValidationLevel(IdentificationFeaturesGenerator identificationFeaturesGenerator, ShotgunProtocol shotgunProtocol,
            IdentificationParameters identificationParameters, InputMap inputMap, String spectrumKey, PeptideAssumption peptideAssumption,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator, ArrayList<Double> precursorMzDeviations)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(peptideAssumption.getAdvocate());
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double fdrLimit = targetDecoyResults.getFdrLimit();
            double nTargetLimit = 100.0 / fdrLimit;
            double seThreshold = targetDecoyResults.getScoreLimit();
            double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;

            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }

            boolean noValidated = targetDecoyResults.noValidated();

            if (!noValidated && peptideAssumption.getScore() <= seThreshold) { //@TODO: include ascending/descending scores

                String reasonDoubtful = null;
                boolean filterPassed = true;

                for (Filter filter : validationQCPreferences.getPsmFilters()) {
                    PsmFilter psmFilter = (PsmFilter) filter;
                    AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
                    boolean validated = assumptionFilter.isValidated(spectrumKey, peptideAssumption, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, precursorMzDeviations);
                    psParameter.setQcResult(filter.getName(), validated);
                    if (!validated) {
                        filterPassed = false;
                        if (reasonDoubtful == null) {
                            reasonDoubtful = "";
                        } else {
                            reasonDoubtful += ", ";
                        }
                        reasonDoubtful += filter.getDescription();
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getSearchEngineConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                if (!confidenceThresholdPassed) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low confidence";
                }

                boolean enoughHits = targetDecoyMap.getnTargetOnly() > nTargetLimit;

                if (!enoughHits) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Low number of hits";
                }

                if (!sequenceFactory.hasEnoughSequences()) {
                    if (reasonDoubtful == null) {
                        reasonDoubtful = "";
                    } else {
                        reasonDoubtful += ", ";
                    }
                    reasonDoubtful += "Database too small";
                }

                if (filterPassed && confidenceThresholdPassed && enoughHits && sequenceFactory.hasEnoughSequences()) {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);
                } else {
                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);
                    if (reasonDoubtful != null) {
                        psParameter.setReasonDoubtful(reasonDoubtful);
                    }
                }
            } else {
                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);
            }
        } else {
            psParameter.setMatchValidationLevel(MatchValidationLevel.none);
        }
    }

    /**
     * Fills the peptide specific map.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided fraction information and found modifications
     * will be saved while iterating the matches
     * @param identificationParameters the identification parameters
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void fillPeptideMaps(Identification identification, Metrics metrics, WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Filling Peptide Maps. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size() * 2);

        ArrayList<String> foundModifications = new ArrayList<String>();
        HashMap<String, ArrayList<String>> fractionPsmMatches = new HashMap<String, ArrayList<String>>();

        PSParameter psParameter = new PSParameter();
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(null, false, null, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            waitingHandler.setDisplayProgress(false);
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            waitingHandler.setDisplayProgress(true);

            String peptideKey = peptideMatch.getKey();
            for (String modification : Peptide.getModificationFamily(peptideKey)) {
                if (!foundModifications.contains(modification)) {
                    foundModifications.add(modification);
                }
            }

            double probaScore = 1;
            HashMap<String, Double> fractionScores = new HashMap<String, Double>();

            // get the fraction scores
            identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                probaScore = probaScore * psParameter.getPsmProbability();
                String fraction = Spectrum.getSpectrumFile(spectrumKey);

                if (!fractionScores.containsKey(fraction)) {
                    fractionScores.put(fraction, 1.0);
                }

                fractionScores.put(fraction, fractionScores.get(fraction) * psParameter.getPsmProbability());

                if (!fractionPsmMatches.containsKey(fraction + "_" + peptideKey)) {
                    ArrayList<String> spectrumMatches = new ArrayList<String>(1);
                    spectrumMatches.add(spectrumKey);
                    fractionPsmMatches.put(fraction + "_" + peptideKey, spectrumMatches);
                } else {
                    fractionPsmMatches.get(fraction + "_" + peptideKey).add(spectrumKey);
                }
            }

            psParameter = new PSParameter();
            psParameter.setPeptideProbabilityScore(probaScore);
            psParameter.setSpecificMapKey(peptideMap.getKey(peptideMatch));

            // set the fraction scores
            for (String fractionName : fractionScores.keySet()) {
                psParameter.setFractionScore(fractionName, fractionScores.get(fractionName));
            }

            identification.addPeptideMatchParameter(peptideKey, psParameter);
            peptideMap.addPoint(psParameter.getPeptideProbabilityScore(), peptideMatch, identificationParameters.getSequenceMatchingPreferences());

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        if (metrics != null) {
            // set the fraction psm matches
            metrics.setFractionPsmMatches(fractionPsmMatches);
            // set the ptms
            metrics.setFoundModifications(foundModifications);
        }
    }

    /**
     * Attaches the peptide posterior error probabilities to the peptide
     * matches.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void attachPeptideProbabilities(Identification identification, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Attaching Peptide Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size());

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, false, parameters, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            waitingHandler.setDisplayProgress(false);
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            waitingHandler.setDisplayProgress(true);

            String peptideKey = peptideMatch.getKey();

            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (sequenceFactory.concatenatedTargetDecoy()) {
                psParameter.setPeptideProbability(peptideMap.getProbability(psParameter.getSpecificMapKey(), psParameter.getPeptideProbabilityScore()));
            } else {
                psParameter.setPeptideProbability(1.0);
            }
            for (String fraction : psParameter.getFractions()) {
                if (sequenceFactory.concatenatedTargetDecoy()) {
                    psParameter.setFractionPEP(fraction, peptideMap.getProbability(psParameter.getSpecificMapKey(), psParameter.getFractionScore(fraction)));
                } else {
                    psParameter.setFractionPEP(fraction, 1.0);
                }
            }

            identification.updatePeptideMatchParameter(peptideKey, psParameter);
            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Fills the protein map.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     */
    public void fillProteinMap(Identification identification, WaitingHandler waitingHandler) throws Exception {

        waitingHandler.setWaitingText("Filling Protein Map. Please Wait...");

        int max = identification.getProteinIdentification().size();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(null, true, parameters, false, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            waitingHandler.setDisplayProgress(false);
            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            waitingHandler.setDisplayProgress(true);

            String proteinKey = proteinMatch.getKey();

            waitingHandler.increaseSecondaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }

            HashMap<String, Double> fractionScores = new HashMap<String, Double>();
            double probaScore = 1;

            if (proteinMatch == null) {
                throw new IllegalArgumentException("Protein match " + proteinKey + " not found.");
            }

            // get the fraction scores
            for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                probaScore = probaScore * psParameter.getPeptideProbability();

                for (String fraction : psParameter.getFractions()) {
                    if (!fractionScores.containsKey(fraction)) {
                        fractionScores.put(fraction, 1.0);
                    }

                    fractionScores.put(fraction, fractionScores.get(fraction) * psParameter.getFractionPEP(fraction));
                }
            }

            psParameter = new PSParameter();
            psParameter.setProteinProbabilityScore(probaScore);

            // set the fraction scores
            for (String fractionName : fractionScores.keySet()) {
                psParameter.setFractionScore(fractionName, fractionScores.get(fractionName));
            }

            identification.addProteinMatchParameter(proteinKey, psParameter);
            proteinMap.addPoint(psParameter.getProteinProbabilityScore(), proteinMatch.isDecoy());
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Attaches the protein posterior error probability to the protein matches.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided fraction information
     * @param waitingHandler the handler displaying feedback to the user
     * @param processingPreferences the processing preferences
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the matches database
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with the matches database or when an error is encountered
     * while reading the FASTA file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing a match from the database
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while retrieving the match or when an error is encountered while
     * reading the FASTA file
     */
    public void attachProteinProbabilities(Identification identification, Metrics metrics, WaitingHandler waitingHandler,
            ProcessingPreferences processingPreferences) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Attaching Protein Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());

        HashMap<String, ArrayList<Double>> fractionMW = new HashMap<String, ArrayList<Double>>();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            waitingHandler.setDisplayProgress(false);
            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            waitingHandler.setDisplayProgress(true);

            String proteinKey = proteinMatch.getKey();
            Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());

            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
            if (sequenceFactory.concatenatedTargetDecoy()) {
                double proteinProbability = proteinMap.getProbability(psParameter.getProteinProbabilityScore());
                psParameter.setProteinProbability(proteinProbability);
            } else {
                psParameter.setProteinProbability(1.0);
            }

            for (String fraction : psParameter.getFractions()) {
                if (sequenceFactory.concatenatedTargetDecoy()) {
                    psParameter.setFractionPEP(fraction, proteinMap.getProbability(psParameter.getFractionScore(fraction)));
                } else {
                    psParameter.setFractionPEP(fraction, 1.0);
                }

                // set the fraction molecular weights
                if (!proteinMatch.isDecoy() && psParameter.getFractionConfidence(fraction) > processingPreferences.getProteinConfidenceMwPlots()) {
                    if (fractionMW.containsKey(fraction)) {
                        fractionMW.get(fraction).add(proteinMW);
                    } else {
                        ArrayList<Double> mw = new ArrayList<Double>();
                        mw.add(proteinMW);
                        fractionMW.put(fraction, mw);
                    }
                }
            }

            identification.updateProteinMatchParameter(proteinKey, psParameter);
            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        if (metrics != null) {
            // set the observed fractional molecular weights per fraction
            metrics.setObservedFractionalMassesAll(fractionMW);
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Returns the PSM scoring specific map.
     *
     * @return the PSM scoring specific map
     */
    public PsmSpecificMap getPsmMap() {
        return psmMap;
    }

    /**
     * Sets the PSM scoring specific map.
     *
     * @param psmMap the PSM scoring specific map
     */
    public void setPsmMap(PsmSpecificMap psmMap) {
        this.psmMap = psmMap;
    }

    /**
     * Returns the peptide scoring specific map.
     *
     * @return the peptide scoring specific map
     */
    public PeptideSpecificMap getPeptideMap() {
        return peptideMap;
    }

    /**
     * Sets the peptide scoring specific map.
     *
     * @param peptideMap the peptide scoring specific map
     */
    public void setPeptideMap(PeptideSpecificMap peptideMap) {
        this.peptideMap = peptideMap;
    }

    /**
     * Returns the protein scoring map.
     *
     * @return the protein scoring map
     */
    public ProteinMap getProteinMap() {
        return proteinMap;
    }

    /**
     * Sets the protein scoring map.
     *
     * @param proteinMap the protein scoring map
     */
    public void setProteinMap(ProteinMap proteinMap) {
        this.proteinMap = proteinMap;
    }

    /**
     * Sets the default matches quality control filters.
     *
     * @param validationQCPreferences the default matches quality control
     * filters
     */
    public static void setDefaultMatchesQCFilters(ValidationQCPreferences validationQCPreferences) {

        ArrayList<Filter> psmFilters = new ArrayList<Filter>(3);
        PsmFilter psmFilter = new PsmFilter("Fragment Ion Sequence Coverage");
        psmFilter.setDescription("Sequence coverage filter by fragment ions");
        psmFilter.getAssumptionFilter().setSequenceCoverage(30.0);
        psmFilter.getAssumptionFilter().setSequenceCoverageComparison(RowFilter.ComparisonType.AFTER);
        psmFilters.add(psmFilter);
        psmFilter = new PsmFilter("Mass deviation");
        psmFilter.setDescription("Precursor m/z deviation filter");
        psmFilter.getAssumptionFilter().setPrecursorMzError(0.0001);
        psmFilter.getAssumptionFilter().setPrecursorMzErrorComparison(RowFilter.ComparisonType.BEFORE);
        psmFilter.getAssumptionFilter().setPrecursorMzErrorType(IonMatch.MzErrorType.Statistical);
        psmFilters.add(psmFilter);
        validationQCPreferences.setPsmFilters(psmFilters);

        ArrayList<Filter> peptideFilters = new ArrayList<Filter>(1);
        PeptideFilter peptideFilter = new PeptideFilter("One confident PSM");
        peptideFilter.setDescription("Number of confident PSMs filter");
        peptideFilter.setNConfidentSpectra(0);
        peptideFilter.setnConfidentSpectraComparison(RowFilter.ComparisonType.AFTER);
        peptideFilters.add(peptideFilter);
        validationQCPreferences.setPeptideFilters(peptideFilters);

        ArrayList<Filter> proteinFilters = new ArrayList<Filter>(1);
        ProteinFilter proteinFilter = new ProteinFilter(">=2 confident peptides");
        proteinFilter.setDescription("Number of confident peptides filter");
        proteinFilter.setnConfidentPeptides(1);
        proteinFilter.setnConfidentPeptidesComparison(RowFilter.ComparisonType.AFTER);
        proteinFilters.add(proteinFilter);
        proteinFilter = new ProteinFilter(">=2 confident spectra");
        proteinFilter.setDescription("Number of confident spectra filter");
        proteinFilter.setProteinNConfidentSpectra(1);
        proteinFilter.setnConfidentSpectraComparison(RowFilter.ComparisonType.AFTER);
        proteinFilters.add(proteinFilter);
        validationQCPreferences.setProteinFilters(proteinFilters);

    }
}
