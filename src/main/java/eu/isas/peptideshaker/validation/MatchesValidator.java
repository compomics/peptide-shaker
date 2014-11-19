package eu.isas.peptideshaker.validation;

import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.myparameters.PSParameter;
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
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator providing information about the matches
     * @param inputMap the input target/decoy map
     */
    public void validateIdentifications(Identification identification, Metrics metrics, WaitingHandler waitingHandler, 
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator, InputMap inputMap) {

        IdMatchValidationPreferences validationPreferences = identificationParameters.getIdValidationPreferences();
        
        waitingHandler.setWaitingText("Finding FDR thresholds. Please Wait...");

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

        try {
            validateIdentifications(identification, metrics, inputMap, waitingHandler, 
                    identificationFeaturesGenerator, shotgunProtocol, identificationParameters);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while validating the results.", true, true);
            waitingHandler.setRunCanceled();
            e.printStackTrace();
        }

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
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     * @throws InterruptedException
     */
    public void validateIdentifications(Identification identification, Metrics metrics, InputMap inputMap, 
            WaitingHandler waitingHandler, IdentificationFeaturesGenerator identificationFeaturesGenerator, 
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) 
            throws SQLException, IOException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        PSParameter psParameter = new PSParameter();
        PSParameter psParameter2 = new PSParameter();

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Match Validation. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size()
                    + identification.getPeptideIdentification().size()
                    + 2 * identification.getSpectrumIdentificationSize());
        }

        psmMap.resetDoubtfulMatchesFilters();
        peptideMap.resetDoubtfulMatchesFilters();
        proteinMap.resetDoubtfulMatchesFilters();
        
        PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
        HashMap<String, ArrayList<String>> spectrumKeysMap = identification.getSpectrumIdentificationMap();
        if (metrics != null && metrics.getGroupedSpectrumKeys() != null) {
            spectrumKeysMap = metrics.getGroupedSpectrumKeys();
        }

        // validate the spectrum matches
        if (inputMap != null) {
            inputMap.resetAdvocateContributions();
        }
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFileName, null);
            identification.loadSpectrumMatchParameters(spectrumFileName, new PSParameter(), null);
            ArrayList<Double> precursorMzDeviations = new ArrayList<Double>();
            ArrayList<Integer> charges = new ArrayList<Integer>();

            for (String spectrumKey : spectrumKeysMap.get(spectrumFileName)) {

                updateSpectrumMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, psmMap, spectrumKey);
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (peptideAssumption != null) {

                        Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);
                        double precursorMzError = peptideAssumption.getDeltaMass(precursor.getMz(), shotgunProtocol.isMs1ResolutionPpm());
                        precursorMzDeviations.add(precursorMzError);
                        Integer charge = peptideAssumption.getIdentificationCharge().value;

                        if (!charges.contains(charge)) {
                            charges.add(charge);
                            PsmFilter psmFilter = new PsmFilter(">30% Fragment Ion Sequence Coverage");
                            psmFilter.setDescription("<30% sequence coverage by fragment ions");
                            psmFilter.setSequenceCoverage(30.0); // @TODO: make the doubtfulThreshold editable by the user!
                            psmFilter.setSequenceCoverageComparison(RowFilter.ComparisonType.AFTER);
                            psmMap.addDoubtfulMatchesFilter(charge, spectrumFileName, psmFilter);
                        }

                        if (inputMap != null) {

                            Peptide bestPeptide = peptideAssumption.getPeptide();
                            ArrayList<Integer> agreementAdvocates = new ArrayList<Integer>();

                            for (int advocateId : spectrumMatch.getAdvocates()) {
                                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : spectrumMatch.getFirstHits(advocateId)) {
                                    if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                                        Peptide advocatePeptide = ((PeptideAssumption) spectrumIdentificationAssumption).getPeptide();
                                        if (bestPeptide.isSameSequenceAndModificationStatus(advocatePeptide, identificationParameters.getSequenceMatchingPreferences())) {
                                            agreementAdvocates.add(advocateId);
                                            break;
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
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);

                    for (Integer advocateId : spectrumMatch.getAdvocates()) {

                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptions = spectrumMatch.getAllAssumptions(advocateId);

                        for (double eValue : assumptions.keySet()) {
                            for (SpectrumIdentificationAssumption spectrumIdAssumption : assumptions.get(eValue)) {
                                if (spectrumIdAssumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdAssumption;
                                    updatePeptideAssumptionValidationLevel(identificationFeaturesGenerator, shotgunProtocol, identificationParameters, inputMap, spectrumKey, peptideAssumption, peptideSpectrumAnnotator);
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

            // check if we should narrow the mass accuracy window, if yes, do a second pass validation
            if (!precursorMzDeviations.isEmpty()) {

                NonSymmetricalNormalDistribution precDeviationDistribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistribution(precursorMzDeviations);
                Double minDeviation = precDeviationDistribution.getMinValueForProbability(0.0001);
                Double maxDeviation = precDeviationDistribution.getMaxValueForProbability(0.0001);
                boolean needSecondPass = false;

                if (minDeviation < maxDeviation) {
                    String unit = "ppm";
                    if (!shotgunProtocol.isMs1ResolutionPpm()) {
                        unit = "Da";
                    }
                    if (minDeviation != Double.NaN && minDeviation > -shotgunProtocol.getMs1Resolution()) {
                        needSecondPass = true;
                        PsmFilter psmFilter = new PsmFilter("Precursor m/z deviation > " + Util.roundDouble(minDeviation, 2) + " " + unit);
                        psmFilter.setDescription("Precursor m/z deviation < " + Util.roundDouble(minDeviation, 2) + " " + unit);
                        psmFilter.setMinPrecursorMzError(minDeviation);
                        psmFilter.setPrecursorMinMzErrorComparison(RowFilter.ComparisonType.AFTER);
                        for (int charge : charges) {
                            psmMap.addDoubtfulMatchesFilter(charge, spectrumFileName, psmFilter);
                        }
                    }
                    if (minDeviation != Double.NaN && maxDeviation < shotgunProtocol.getMs1Resolution()) {
                        needSecondPass = true;
                        PsmFilter psmFilter = new PsmFilter("Precursor m/z deviation < " + Util.roundDouble(maxDeviation, 2) + " " + unit);
                        psmFilter.setDescription("Precursor m/z deviation > " + Util.roundDouble(maxDeviation, 2) + " " + unit);
                        psmFilter.setMaxPrecursorMzError(maxDeviation);
                        psmFilter.setPrecursorMaxMzErrorComparison(RowFilter.ComparisonType.BEFORE);
                        for (int charge : charges) {
                            psmMap.addDoubtfulMatchesFilter(charge, spectrumFileName, psmFilter);
                        }
                    }
                }

                if (needSecondPass) {

                    if (inputMap != null) {
                        inputMap.resetAdvocateContributions(spectrumFileName);
                    }

                    for (String spectrumKey : identification.getSpectrumIdentification(spectrumFileName)) {

                        updateSpectrumMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, psmMap, spectrumKey);
                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                        if (psParameter.getMatchValidationLevel().isValidated()) {

                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                            if (peptideAssumption != null) {
                                if (inputMap != null) {
                                    Peptide bestPeptide = peptideAssumption.getPeptide();
                                    ArrayList<Integer> agreementAdvocates = new ArrayList<Integer>();
                                    for (int advocateId : spectrumMatch.getAdvocates()) {
                                        for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : spectrumMatch.getFirstHits(advocateId)) {
                                            if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                                                Peptide advocatePeptide = ((PeptideAssumption) spectrumIdentificationAssumption).getPeptide();
                                                if (bestPeptide.isSameSequenceAndModificationStatus(advocatePeptide, identificationParameters.getSequenceMatchingPreferences())) {
                                                    agreementAdvocates.add(advocateId);
                                                    break;
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
                } else if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressCounter(identification.getSpectrumIdentification(spectrumFileName).size());
                }
            }
        }

        HashMap<String, Integer> validatedTotalPeptidesPerFraction = new HashMap<String, Integer>();

        identification.loadPeptideMatches(null);
        identification.loadPeptideMatchParameters(new PSParameter(), null);
        ArrayList<Double> validatedPeptideLengths = new ArrayList<Double>();

        // validate the peptides
        for (String peptideKey : identification.getPeptideIdentification()) {

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

                            if (SpectrumFactory.getInstance().getPrecursor(spectrumKeys.get(k)).getIntensity() > 0) {
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
        double proteinThreshold = targetDecoyResults.getScoreLimit();
        double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + targetDecoyMap.getResolution();
        if (proteinConfidentThreshold > 100) {
            proteinConfidentThreshold = 100;
        }
        boolean noValidated = proteinMap.getTargetDecoyMap().getTargetDecoyResults().noValidated();

        int maxValidatedSpectraFractionLevel = 0;
        int maxValidatedPeptidesFractionLevel = 0;
        double maxProteinAveragePrecursorIntensity = 0;
        double maxProteinSummedPrecursorIntensity = 0;

        identification.loadProteinMatches(null);
        identification.loadProteinMatchParameters(new PSParameter(), null);

        for (String proteinKey : identification.getProteinIdentification()) {

            updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters,
                    targetDecoyMap, proteinThreshold, proteinConfidentThreshold, noValidated, proteinMap.getDoubtfulMatchesFilters(), proteinKey);

            // set the fraction details
            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

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

        if (metrics != null) {
            // set the max values in the metrics
            metrics.setMaxValidatedPeptidesPerFraction(maxValidatedPeptidesFractionLevel);
            metrics.setMaxValidatedSpectraPerFraction(maxValidatedSpectraFractionLevel);
            metrics.setMaxProteinAveragePrecursorIntensity(maxProteinAveragePrecursorIntensity);
            metrics.setMaxProteinSummedPrecursorIntensity(maxProteinSummedPrecursorIntensity);
            metrics.setTotalPeptidesPerFraction(validatedTotalPeptidesPerFraction);
        }
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
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ProteinMap proteinMap, String proteinKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
        double proteinThreshold = targetDecoyResults.getScoreLimit();
        double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + targetDecoyMap.getResolution();

        if (proteinConfidentThreshold > 100) {
            proteinConfidentThreshold = 100;
        }

        boolean noValidated = proteinMap.getTargetDecoyMap().getTargetDecoyResults().noValidated();
        updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters,
                targetDecoyMap, proteinThreshold, proteinConfidentThreshold, noValidated, proteinMap.getDoubtfulMatchesFilters(), proteinKey);
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
     * @param doubtfulMatchFilters the filters to use for quality filtering
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinKey the key of the protein match of interest
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, TargetDecoyMap targetDecoyMap, double scoreThreshold,
            double confidenceThreshold, boolean noValidated, ArrayList<ProteinFilter> doubtfulMatchFilters,
            String proteinKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
        psParameter.resetQcResults();

        if (!psParameter.isManualValidation()) {

            if (sequenceFactory.concatenatedTargetDecoy()) {

                if (!noValidated && psParameter.getProteinProbabilityScore() <= scoreThreshold) {
                    String reasonDoubtful = null;
                    boolean filterPassed = true;
                    for (ProteinFilter filter : doubtfulMatchFilters) {
                        boolean validation = filter.isValidated(proteinKey, identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters);
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
                    boolean enoughHits = targetDecoyMap.getnTargetOnly() > 100;
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
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void updatePeptideMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpecificMap peptideMap, String peptideKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
        psParameter.resetQcResults();

        if (sequenceFactory.concatenatedTargetDecoy()) {
            TargetDecoyMap targetDecoyMap = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSpecificMapKey()));
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double peptideThreshold = targetDecoyResults.getScoreLimit();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + targetDecoyMap.getResolution();
            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }
            boolean noValidated = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSpecificMapKey())).getTargetDecoyResults().noValidated();
            if (!noValidated && psParameter.getPeptideProbabilityScore() <= peptideThreshold) {
                String reasonDoubtful = null;
                boolean filterPassed = true;
                for (PeptideFilter filter : peptideMap.getDoubtfulMatchesFilters()) {
                    boolean validation = filter.isValidated(peptideKey, identification, identificationFeaturesGenerator);
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
                boolean enoughHits = targetDecoyMap.getnTargetOnly() > 100;
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
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void updateSpectrumMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator, 
            PsmSpecificMap psmMap, String spectrumKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
        psParameter.resetQcResults();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            Integer charge = new Integer(psParameter.getSpecificMapKey());
            String fileName = Spectrum.getSpectrumFile(spectrumKey);
            TargetDecoyMap targetDecoyMap = psmMap.getTargetDecoyMap(charge, fileName);
            double psmThreshold = 0;
            double confidenceThreshold = 100;
            boolean noValidated = true;

            if (targetDecoyMap != null) {
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                psmThreshold = targetDecoyResults.getScoreLimit();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + targetDecoyMap.getResolution();
                if (confidenceThreshold > 100) {
                    confidenceThreshold = 100;
                }
                noValidated = targetDecoyResults.noValidated();
            }

            if (!noValidated && psParameter.getPsmProbabilityScore() <= psmThreshold) {

                AnnotationPreferences annotationPreferences = identificationParameters.getAnnotationPreferences();
                SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    // update the annotation preferences for the new psm, mainly the charge
                    annotationPreferences.setCurrentSettings(spectrumMatch.getBestPeptideAssumption(), true, sequenceMatchingPreferences);
                } else if (spectrumMatch.getBestTagAssumption() != null) {
                    charge = spectrumMatch.getBestTagAssumption().getIdentificationCharge().value;
                } else {
                    throw new IllegalArgumentException("No best tag or peptide found for spectrum " + spectrumKey);
                }

                String reasonDoubtful = null;
                boolean filterPassed = true;

                for (PsmFilter filter : psmMap.getDoubtfulMatchesFilters(charge, spectrumFile)) {
                    boolean validated = filter.isValidated(spectrumKey, identification, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator);
                    psParameter.setQcResult(filter.getName(), validated);
                    if (!validated) {
                        if (filter.getName().toLowerCase().contains("deviation")) {
                            filter.isValidated(spectrumKey, identification, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator);
                        } else if (filter.getName().toLowerCase().contains("coverage")) {
                            filter.isValidated(spectrumKey, identification, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator);
                        }
                        filterPassed = false;
                        if (reasonDoubtful == null) {
                            reasonDoubtful = "";
                        } else {
                            reasonDoubtful += ", ";
                        }
                        reasonDoubtful += filter.getDescription();
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

                boolean enoughHits = targetDecoyMap.getnTargetOnly() > 100;

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
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void updateTagAssumptionValidationLevel(IdentificationFeaturesGenerator identificationFeaturesGenerator, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, InputMap inputMap, String spectrumKey, TagAssumption tagAssumption)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) tagAssumption.getUrParam(psParameter);

        if (sequenceFactory.concatenatedTargetDecoy()) {

            TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(tagAssumption.getAdvocate());
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double seThreshold = targetDecoyResults.getScoreLimit();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + targetDecoyMap.getResolution();

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

                boolean enoughHits = targetDecoyMap.getnTargetOnly() > 100;

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
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void updatePeptideAssumptionValidationLevel(IdentificationFeaturesGenerator identificationFeaturesGenerator, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, InputMap inputMap, String spectrumKey, PeptideAssumption peptideAssumption, PeptideSpectrumAnnotator peptideSpectrumAnnotator)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
        psParameter.resetQcResults();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(peptideAssumption.getAdvocate());
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double seThreshold = targetDecoyResults.getScoreLimit();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + targetDecoyMap.getResolution();

            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }

            boolean noValidated = targetDecoyResults.noValidated();

            if (!noValidated && peptideAssumption.getScore() <= seThreshold) { //@TODO: include ascending/descending scores

                String reasonDoubtful = null;
                boolean filterPassed = true;

                for (AssumptionFilter filter : inputMap.getDoubtfulMatchesFilters()) {
                    boolean validated = filter.isValidated(spectrumKey, peptideAssumption, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator);
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

                boolean enoughHits = targetDecoyMap.getnTargetOnly() > 100;

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
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public void fillPeptideMaps(Identification identification, Metrics metrics, WaitingHandler waitingHandler, 
            IdentificationParameters identificationParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Filling Peptide Maps. Please Wait...");

        PSParameter psParameter = new PSParameter();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size() * 2);

        ArrayList<String> foundModifications = new ArrayList<String>();
        HashMap<String, ArrayList<String>> fractionPsmMatches = new HashMap<String, ArrayList<String>>();

        // load the peptides into memory
        identification.loadPeptideMatches(identification.getPeptideIdentification(), waitingHandler);

        for (String peptideKey : identification.getPeptideIdentification()) {
            for (String modification : Peptide.getModificationFamily(peptideKey)) {
                if (!foundModifications.contains(modification)) {
                    foundModifications.add(modification);
                }
            }

            double probaScore = 1;
            HashMap<String, Double> fractionScores = new HashMap<String, Double>();
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

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
            peptideMap.addPoint(probaScore, peptideMatch, identificationParameters.getSequenceMatchingPreferences());

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
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public void attachPeptideProbabilities(Identification identification, WaitingHandler waitingHandler) 
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Attaching Peptide Probabilities. Please Wait...");

        PSParameter psParameter = new PSParameter();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size());

        identification.loadPeptideMatchParameters(psParameter, null);

        for (String peptideKey : identification.getPeptideIdentification()) {

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
     * @throws java.lang.Exception
     */
    public void fillProteinMap(Identification identification, WaitingHandler waitingHandler) throws Exception {

        waitingHandler.setWaitingText("Filling Protein Map. Please Wait...");

        PSParameter psParameter = new PSParameter();

        int max = identification.getProteinIdentification().size();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        identification.loadPeptideMatchParameters(psParameter, null);
        identification.loadProteinMatches(null);

        for (String proteinKey : identification.getProteinIdentification()) {

            waitingHandler.increaseSecondaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }

            HashMap<String, Double> fractionScores = new HashMap<String, Double>();
            double probaScore = 1;
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

            if (proteinMatch == null) {
                throw new IllegalArgumentException("Protein match " + proteinKey + " not found.");
            }

            // get the fraction scores
            identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null); // @TODO: already covered by the loadPeptideMatchParameters call above?
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

            identification.addProteinMatchParameter(proteinKey, psParameter); // @TODO: batch insertion?
            proteinMap.addPoint(probaScore, proteinMatch.isDecoy());
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
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public void attachProteinProbabilities(Identification identification, Metrics metrics, WaitingHandler waitingHandler, 
            ProcessingPreferences processingPreferences) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Attaching Protein Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());

        PSParameter psParameter = new PSParameter();
        HashMap<String, ArrayList<Double>> fractionMW = new HashMap<String, ArrayList<Double>>();

        for (String proteinKey : identification.getProteinIdentification()) {

            //@TODO: this molecular weigth stuff should not be done here!
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
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
}
