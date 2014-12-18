package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class contains the method for PSM best hit selection.
 *
 * @author Marc Vaudel
 */
public class BestMatchSelection {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * Map indicating how often a protein was found in a search engine first hit
     * whenever this protein was found more than one time.
     */
    private HashMap<String, Integer> proteinCount = new HashMap<String, Integer>();
    /**
     * The identification object.
     */
    private Identification identification;
    /**
     * The validator which will take care of the matches validation.
     */
    private MatchesValidator matchesValidator;
    /**
     * Metrics to be picked when loading the identification.
     */
    private Metrics metrics = new Metrics();

    /**
     * Constructor.
     *
     * @param identification the identification object where to get the matches
     * from
     * @param proteinCount a map of proteins found multiple times
     * @param matchesValidator the matches validator
     * @param metrics the object where to store dataset metrics
     */
    public BestMatchSelection(Identification identification, HashMap<String, Integer> proteinCount, MatchesValidator matchesValidator, Metrics metrics) {
        this.identification = identification;
        this.proteinCount = proteinCount;
        this.matchesValidator = matchesValidator;
        this.metrics = metrics;
    }

    /**
     * Fills the PSM specific map.
     *
     * @param inputMap The input map
     * @param waitingHandler the handler displaying feedback to the user
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the back-end database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading an external file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     */
    public void selectBestHitAndFillPsmMap(InputMap inputMap, WaitingHandler waitingHandler, ShotgunProtocol shotgunProtocol, 
            IdentificationParameters identificationParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
        boolean multiSE = inputMap.isMultipleAlgorithms();

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        AnnotationPreferences annotationPreferences = identificationParameters.getAnnotationPreferences();

        IdFilter idFilter = identificationParameters.getIdFilter();

        // Keep a map of the spectrum keys grouped by peptide
        HashMap<String, ArrayList<String>> orderedPsmMap = null;
        if (MemoryConsumptionStatus.memoryUsed() < 0.8) {
            orderedPsmMap = new HashMap<String, ArrayList<String>>(identification.getSpectrumIdentificationMap().size());
        }

        PSParameter psParameter = new PSParameter();

        for (String spectrumFileName : identification.getSpectrumFiles()) {

            HashMap<String, ArrayList<String>> keysMap = null;
            if (orderedPsmMap != null) {
                keysMap = new HashMap<String, ArrayList<String>>();
            }

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, null, true);

            while (psmIterator.hasNext()) {

                SpectrumMatch advocateMatch = psmIterator.next();
                String spectrumKey = advocateMatch.getKey();

                // map of the peptide first hits for this spectrum: score -> max protein count -> max search engine votes -> amino acids annotated -> min mass deviation -> peptide sequence
                HashMap<Double, HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>>>> peptideAssumptions
                        = new HashMap<Double, HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>>>>();

                // map of the tag first hits: score -> assumptions
                HashMap<Double, ArrayList<TagAssumption>> tagAssumptions = new HashMap<Double, ArrayList<TagAssumption>>();

                ArrayList<String> identifications = new ArrayList<String>();

                HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);

                for (int searchEngine1 : assumptions.keySet()) {

                    HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocate1Map = assumptions.get(searchEngine1);

                    double bestEvalue = Collections.min(advocate1Map.keySet());

                    for (SpectrumIdentificationAssumption assumption1 : advocate1Map.get(bestEvalue)) {

                        if (assumption1 instanceof PeptideAssumption) {

                            PeptideAssumption peptideAssumption1 = (PeptideAssumption) assumption1;
                            String id = peptideAssumption1.getPeptide().getKey();

                            if (!identifications.contains(id)) {

                                psParameter = (PSParameter) peptideAssumption1.getUrParam(psParameter);
                                double p;

                                if (multiSE && sequenceFactory.concatenatedTargetDecoy()) {
                                    p = psParameter.getSearchEngineProbability();
                                } else {
                                    p = peptideAssumption1.getScore();
                                }

                                int nSE = 1;
                                int proteinMax = 1;
                                for (String protein : peptideAssumption1.getPeptide().getParentProteins(sequenceMatchingPreferences)) {
                                    Integer tempCount = proteinCount.get(protein);
                                    if (tempCount != null && tempCount > proteinMax) {
                                        proteinMax = tempCount;
                                    }
                                }

                                for (int searchEngine2 : assumptions.keySet()) {

                                    if (searchEngine1 != searchEngine2) {

                                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocate2Map = assumptions.get(searchEngine2);

                                        boolean found = false;
                                        ArrayList<Double> eValues2 = new ArrayList<Double>(advocate2Map.keySet());
                                        Collections.sort(eValues2);

                                        for (double eValue2 : eValues2) {
                                            for (SpectrumIdentificationAssumption assumption2 : advocate2Map.get(eValue2)) {

                                                if (assumption2 instanceof PeptideAssumption) {

                                                    PeptideAssumption peptideAssumption2 = (PeptideAssumption) assumption2;

                                                    if (peptideAssumption1.getPeptide().isSameSequenceAndModificationStatus(peptideAssumption2.getPeptide(),
                                                            sequenceMatchingPreferences)) {
                                                        PSParameter psParameter2 = (PSParameter) peptideAssumption2.getUrParam(psParameter);
                                                        p = p * psParameter2.getSearchEngineProbability();
                                                        nSE++;
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (found) {
                                                break;
                                            }
                                        }
                                    }
                                }

                                identifications.add(id);

                                HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>>> pMap = peptideAssumptions.get(p);
                                if (pMap == null) {
                                    pMap = new HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>>>(advocate1Map.size());
                                    peptideAssumptions.put(p, pMap);
                                }

                                HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>> proteinMaxMap = pMap.get(proteinMax);
                                if (proteinMaxMap == null) {
                                    proteinMaxMap = new HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>>(1);
                                    pMap.put(proteinMax, proteinMaxMap);
                                }

                                HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>> nSeMap = proteinMaxMap.get(nSE);
                                if (nSeMap == null) {
                                    nSeMap = new HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>(1);
                                    proteinMaxMap.put(nSE, nSeMap);
                                    HashMap<Double, HashMap<String, PeptideAssumption>> coverageMap = new HashMap<Double, HashMap<String, PeptideAssumption>>(1);
                                    nSeMap.put(-1, coverageMap);
                                    HashMap<String, PeptideAssumption> assumptionMap = new HashMap<String, PeptideAssumption>(1);
                                    coverageMap.put(-1.0, assumptionMap);
                                    assumptionMap.put(peptideAssumption1.getPeptide().getSequenceWithLowerCasePtms(), peptideAssumption1);
                                } else {
                                    MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                                    double mzTolerance = shotgunProtocol.getMs2Resolution();
                                    boolean isPpm = false; //@TODO change this as soon as search engine support fragment ion tolerance in ppm

                                    HashMap<Double, HashMap<String, PeptideAssumption>> coverageMap = nSeMap.get(-1);
                                    if (coverageMap != null) {
                                        HashMap<String, PeptideAssumption> assumptionMap = coverageMap.get(-1.0);
                                        for (PeptideAssumption tempAssumption : assumptionMap.values()) { // There should be only one
                                            Peptide peptide = tempAssumption.getPeptide();
                                            int precursorCharge = tempAssumption.getIdentificationCharge().value;
                                            annotationPreferences.setCurrentSettings(tempAssumption, true, sequenceMatchingPreferences);
                                            HashMap<Integer, ArrayList<IonMatch>> coveredAminoAcids = 
                                                    spectrumAnnotator.getCoveredAminoAcids(annotationPreferences.getIonTypes(), 
                                                            annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(), precursorCharge,
                                                    spectrum, peptide, 0, mzTolerance, isPpm, annotationPreferences.isHighResolutionAnnotation());
                                            int nIons = coveredAminoAcids.size();
                                            nSeMap.put(nIons, coverageMap);
                                        }
                                        nSeMap.remove(-1);
                                    }

                                    Peptide peptide = peptideAssumption1.getPeptide();
                                    int precursorCharge = peptideAssumption1.getIdentificationCharge().value;
                                    annotationPreferences.setCurrentSettings(peptideAssumption1, true, sequenceMatchingPreferences);
                                    HashMap<Integer, ArrayList<IonMatch>> coveredAminoAcids = 
                                            spectrumAnnotator.getCoveredAminoAcids(annotationPreferences.getIonTypes(), 
                                                    annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(), precursorCharge,
                                            spectrum, peptide, 0, mzTolerance, isPpm, annotationPreferences.isHighResolutionAnnotation());
                                    int nIons = coveredAminoAcids.size();

                                    coverageMap = nSeMap.get(nIons);
                                    if (coverageMap == null) {
                                        coverageMap = new HashMap<Double, HashMap<String, PeptideAssumption>>(1);
                                        HashMap<String, PeptideAssumption> assumptionMap = new HashMap<String, PeptideAssumption>(1);
                                        assumptionMap.put(peptideAssumption1.getPeptide().getSequenceWithLowerCasePtms(), peptideAssumption1);
                                        coverageMap.put(-1.0, assumptionMap);
                                        nSeMap.put(nIons, coverageMap);
                                    } else {
                                        HashMap<String, PeptideAssumption> assumptionMap = coverageMap.get(-1.0);
                                        if (assumptionMap != null) {
                                            for (PeptideAssumption tempAssumption : assumptionMap.values()) { // There should be only one
                                                double massError = Math.abs(tempAssumption.getDeltaMass(spectrum.getPrecursor().getMz(), shotgunProtocol.isMs1ResolutionPpm()));
                                                coverageMap.put(massError, assumptionMap);
                                            }
                                            coverageMap.remove(-1.0);
                                        }

                                        double massError = Math.abs(peptideAssumption1.getDeltaMass(spectrum.getPrecursor().getMz(), shotgunProtocol.isMs1ResolutionPpm()));
                                        assumptionMap = coverageMap.get(massError);
                                        if (assumptionMap == null) {
                                            assumptionMap = new HashMap<String, PeptideAssumption>(1);
                                            coverageMap.put(massError, assumptionMap);
                                        }
                                        assumptionMap.put(peptideAssumption1.getPeptide().getSequenceWithLowerCasePtms(), peptideAssumption1);
                                    }
                                }
                            }
                        } else if (assumption1 instanceof TagAssumption) {
                            TagAssumption tagAssumption = (TagAssumption) assumption1;
                            ArrayList<TagAssumption> assumptionList = tagAssumptions.get(bestEvalue);
                            if (assumptionList == null) {
                                assumptionList = new ArrayList<TagAssumption>();
                                tagAssumptions.put(bestEvalue, assumptionList);
                            }
                            assumptionList.add(tagAssumption);
                        }
                    }
                }

                SpectrumMatch spectrumMatch = new SpectrumMatch(spectrumKey);
                if (!peptideAssumptions.isEmpty()) {

                    PeptideAssumption bestPeptideAssumption = null;
                    ArrayList<Double> ps = new ArrayList<Double>(peptideAssumptions.keySet());
                    Collections.sort(ps);
                    double retainedP = 0;

                    for (double p : ps) {

                        retainedP = p;
                        HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>>> pMap = peptideAssumptions.get(p);
                        ArrayList<Integer> proteinMaxs = new ArrayList<Integer>(pMap.keySet());
                        Collections.sort(proteinMaxs, Collections.reverseOrder());

                        for (int proteinMax : proteinMaxs) {

                            HashMap<Integer, HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>>> proteinMaxMap = pMap.get(proteinMax);
                            ArrayList<Integer> nSEs = new ArrayList<Integer>(proteinMaxMap.keySet());
                            Collections.sort(nSEs, Collections.reverseOrder());

                            for (int nSE : nSEs) {

                                HashMap<Integer, HashMap<Double, HashMap<String, PeptideAssumption>>> nSeMap = proteinMaxMap.get(nSE);
                                ArrayList<Integer> coverages = new ArrayList<Integer>(nSeMap.keySet());
                                Collections.sort(coverages, Collections.reverseOrder());

                                for (Integer coverage : coverages) {

                                    HashMap<Double, HashMap<String, PeptideAssumption>> coverageMap = nSeMap.get(coverage);
                                    ArrayList<Double> minErrors = new ArrayList<Double>(coverageMap.keySet());
                                    Collections.sort(minErrors);

                                    for (double minError : minErrors) {

                                        HashMap<String, PeptideAssumption> bestPeptideAssumptions = coverageMap.get(minError);
                                        ArrayList<String> sequences = new ArrayList<String>(bestPeptideAssumptions.keySet());

                                        for (String sequence : sequences) {
                                            PeptideAssumption peptideAssumption = bestPeptideAssumptions.get(sequence);
                                            if (idFilter.validateProteins(peptideAssumption.getPeptide(), sequenceMatchingPreferences)) {
                                                bestPeptideAssumption = peptideAssumption;
                                                break;
                                            }
                                        }
                                        if (bestPeptideAssumption != null) {
                                            break;
                                        }
                                    }
                                    if (bestPeptideAssumption != null) {
                                        break;
                                    }
                                }
                                if (bestPeptideAssumption != null) {
                                    break;
                                }
                            }
                            if (bestPeptideAssumption != null) {
                                break;
                            }
                        }
                        if (bestPeptideAssumption != null) {
                            break;
                        }
                    }
                    if (bestPeptideAssumption != null) {

                        if (multiSE) {

                            // try to find the most likely modification localization based on the search engine results
                            HashMap<PeptideAssumption, ArrayList<Double>> assumptionPEPs = new HashMap<PeptideAssumption, ArrayList<Double>>();
                            String bestAssumptionKey = bestPeptideAssumption.getPeptide().getMatchingKey(sequenceMatchingPreferences);

                            for (int searchEngine : assumptions.keySet()) {

                                boolean found = false;
                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateMap = assumptions.get(searchEngine);
                                ArrayList<Double> eValues = new ArrayList<Double>(advocateMap.keySet());
                                Collections.sort(eValues);

                                for (double eValue : eValues) {
                                    for (SpectrumIdentificationAssumption assumption : advocateMap.get(eValue)) {

                                        if (assumption instanceof PeptideAssumption) {

                                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;

                                            if (peptideAssumption.getPeptide().getMatchingKey(sequenceMatchingPreferences).equals(bestAssumptionKey)) {

                                                found = true;
                                                boolean found2 = false;

                                                for (PeptideAssumption assumption1 : assumptionPEPs.keySet()) {
                                                    if (assumption1.getPeptide().sameModificationsAs(peptideAssumption.getPeptide())) {
                                                        found2 = true;
                                                        psParameter = (PSParameter) assumption.getUrParam(psParameter);
                                                        ArrayList<Double> peps = assumptionPEPs.get(assumption1);
                                                        peps.add(psParameter.getSearchEngineProbability());
                                                        break;
                                                    }
                                                }

                                                if (!found2) {
                                                    ArrayList<Double> peps = new ArrayList<Double>(1);
                                                    assumptionPEPs.put(peptideAssumption, peps);
                                                    psParameter = (PSParameter) assumption.getUrParam(psParameter);
                                                    peps.add(psParameter.getSearchEngineProbability());
                                                }
                                            }
                                        }
                                    }

                                    if (found) {
                                        break;
                                    }
                                }
                            }

                            Double bestSeP = null;
                            int nSe = -1;

                            for (PeptideAssumption peptideAssumption : assumptionPEPs.keySet()) {

                                ArrayList<Double> peps = assumptionPEPs.get(peptideAssumption);
                                Double sep = Collections.min(peps);

                                if (bestSeP == null || bestSeP > sep) {
                                    bestSeP = sep;
                                    nSe = peps.size();
                                    bestPeptideAssumption = peptideAssumption;
                                } else if (peps.size() > nSe) {
                                    if (sep != null && (Math.abs(sep - bestSeP) <= 1e-10)) {
                                        nSe = peps.size();
                                        bestPeptideAssumption = peptideAssumption;
                                    }
                                }
                            }
                        }

                        // create a PeptideShaker match based on the best search engine match
                        Peptide sePeptide = bestPeptideAssumption.getPeptide();
                        ArrayList<String> psProteins = new ArrayList<String>(sePeptide.getParentProteins(sequenceMatchingPreferences));
                        ArrayList<ModificationMatch> psModificationMatches = new ArrayList<ModificationMatch>();

                        for (ModificationMatch seModMatch : sePeptide.getModificationMatches()) {
                            psModificationMatches.add(new ModificationMatch(seModMatch.getTheoreticPtm(), seModMatch.isVariable(), seModMatch.getModificationSite()));
                        }

                        Peptide psPeptide = new Peptide(sePeptide.getSequence(), psModificationMatches);
                        psPeptide.setParentProteins(psProteins);
                        PeptideAssumption psAssumption = new PeptideAssumption(psPeptide, 1, Advocate.peptideShaker.getIndex(), bestPeptideAssumption.getIdentificationCharge(), retainedP);

                        spectrumMatch.setBestPeptideAssumption(psAssumption);

                        if (orderedPsmMap != null) {
                            String peptideKey = psPeptide.getMatchingKey(sequenceMatchingPreferences);
                            ArrayList<String> spectrumKeys = keysMap.get(peptideKey);
                            if (spectrumKeys == null) {
                                spectrumKeys = new ArrayList<String>();
                                keysMap.put(peptideKey, spectrumKeys);
                            }
                            spectrumKeys.add(spectrumKey);
                        }

                        psParameter = new PSParameter();
                        psParameter.setSpectrumProbabilityScore(retainedP);

                        PSParameter matchParameter = (PSParameter) bestPeptideAssumption.getUrParam(psParameter);
                        psParameter.setSearchEngineProbability(matchParameter.getSearchEngineProbability());
                        psParameter.setAlgorithmDeltaPEP(matchParameter.getAlgorithmDeltaPEP());
                        psParameter.setDeltaPEP(matchParameter.getDeltaPEP());

                        matchesValidator.getPsmMap().addPoint(psParameter.getPsmProbabilityScore(), spectrumMatch, sequenceMatchingPreferences);
                        psParameter.setSpecificMapKey(spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value + "");
                        identification.addSpectrumMatchParameter(spectrumKey, psParameter);
                        identification.updateSpectrumMatch(spectrumMatch);

                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + spectrumKey + ".");
                    }
                }
                if (!tagAssumptions.isEmpty()) {
                    ArrayList<Double> evalues = new ArrayList<Double>(tagAssumptions.keySet());
                    Double bestEvalue = Collections.min(evalues);
                    TagAssumption bestAssumption = tagAssumptions.get(bestEvalue).get(0);
                    spectrumMatch.setBestTagAssumption(bestAssumption);
                    identification.updateSpectrumMatch(spectrumMatch);
                    if (spectrumMatch.getBestPeptideAssumption() == null) {
                        psParameter = new PSParameter();
                        if (!multiSE) {
                            psParameter.setSpectrumProbabilityScore(bestEvalue);
                        }
                        PSParameter matchParameter = (PSParameter) bestAssumption.getUrParam(psParameter);
                        psParameter.setSearchEngineProbability(matchParameter.getSearchEngineProbability());
                        psParameter.setAlgorithmDeltaPEP(matchParameter.getAlgorithmDeltaPEP());
                        psParameter.setDeltaPEP(matchParameter.getDeltaPEP());
                        psParameter.setSpecificMapKey(spectrumMatch.getBestTagAssumption().getIdentificationCharge().value + "");
                        identification.addSpectrumMatchParameter(spectrumKey, psParameter);
                    }
                }
                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }

            if (orderedPsmMap != null) {
                ArrayList<String> orderedKeys = new ArrayList<String>(identification.getSpectrumIdentification(spectrumFileName).size());
                for (ArrayList<String> keys : keysMap.values()) {
                    orderedKeys.addAll(keys);
                }
                orderedPsmMap.put(spectrumFileName, orderedKeys);

                if (MemoryConsumptionStatus.memoryUsed() > 0.9) {
                    orderedPsmMap = null;
                }
            }
        }

        if (orderedPsmMap != null) {
            metrics.setGroupedSpectrumKeys(orderedPsmMap);
        }

        // the protein count map is no longer needed
        proteinCount.clear();

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }
}
