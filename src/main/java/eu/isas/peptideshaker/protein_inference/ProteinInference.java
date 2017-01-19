package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProteinInferencePreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class groups the methods for protein inference.
 *
 * @author Marc Vaudel
 */
public class ProteinInference {

    /**
     * Number of groups deleted because of protein evidence issues.
     */
    private int evidenceIssue = 0;
    /**
     * Number of groups deleted because of enzymatic issues.
     */
    private int enzymaticIssue = 0;
    /**
     * Number of groups deleted because of protein characterization issues.
     */
    private int uncharacterizedIssue = 0;
    /**
     * Number of groups deleted because explained by a simpler group.
     */
    private int explainedGroup = 0;
    /**
     * The protein sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * Map of the most complex groups: key | proteins
     */
    private HashMap<String, ArrayList<String>> proteinGroupCache = new HashMap<String, ArrayList<String>>(100);
    /**
     * Size of the protein groups cahce
     */
    private int cacheSize = 100;
    /**
     * The minimal group size to include a protein in the cache
     */
    private int sizeOfProteinsInCache = 10;

    /**
     * Reduce artifact groups which can be explained by a simpler group.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification feature
     * generator
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public void removeRedundantGroups(Identification identification, IdentificationParameters identificationParameters,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, WaitingHandler waitingHandler)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        int max = identification.getProteinIdentification().size();

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Symplifying Protein Groups. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(max);
        }

        HashSet<String> toDelete = new HashSet<String>();
        HashMap<String, String> processedKeys = new HashMap<String, String>();

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(null, false, null, false, null, waitingHandler);
        while (proteinMatchesIterator.hasNext()) {
            ProteinMatch proteinSharedGroup = proteinMatchesIterator.next();
            if (proteinSharedGroup.getNProteins() > 1) {
                String proteinSharedKey = proteinSharedGroup.getKey();
                if (!processedKeys.containsKey(proteinSharedKey)) {
                    String uniqueKey = getSubgroup(identification, proteinSharedKey, proteinSharedGroup, processedKeys, toDelete, identificationParameters, identificationFeaturesGenerator);
                    if (uniqueKey != null) {
                        mergeProteinGroups(identification, proteinSharedKey, uniqueKey, toDelete);
                        processedKeys.put(proteinSharedKey, uniqueKey);
                    } else {
                        processedKeys.put(proteinSharedKey, proteinSharedKey);
                    }
                }
                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }
            }
        }

        if (enzymaticIssue + evidenceIssue + uncharacterizedIssue + explainedGroup > 0) { // special case to not divide by zero

            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Removing Mapping Artifacts. Please Wait...");
                waitingHandler.appendReport(toDelete.size() + " unlikely protein mappings found:", true, true);

                String padding = "    ";

                if (enzymaticIssue > 0) {
                    waitingHandler.appendReport(padding + "- " + enzymaticIssue + " protein groups supported by non-enzymatic shared peptides.", true, true);
                }
                if (evidenceIssue > 0) {
                    waitingHandler.appendReport(padding + "- " + evidenceIssue + " protein groups explained by peptides shared to less confident mappings.", true, true);
                }
                if (uncharacterizedIssue > 0) {
                    waitingHandler.appendReport(padding + "- " + uncharacterizedIssue + " protein groups supported by peptides shared to uncharacterized proteins.", true, true);
                }
                if (explainedGroup > 0) {
                    waitingHandler.appendReport(padding + "- " + explainedGroup + " groups explained by a simpler group.", true, true);
                }
                waitingHandler.appendReport(padding + "Note: a group can present combinations of these criteria.", true, true);
                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxSecondaryProgressCounter(toDelete.size());
            }

            for (String proteinKey : toDelete) {
                identification.removeProteinMatch(proteinKey);
                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }
            }
        }
    }

    /**
     * Returns the best subgroup of a protein key, null if none found. If
     * intermediate groups are found they will be processed. Processed keys are
     * stored in processedKeys. Keys to delete are stored in keysToDelete.
     * Returns null if no simpler group is found.
     *
     * @param identification the identification where to get the matches from.
     * @param sharedKey the key of the group to inspect
     * @param processedKeys map of already processed keys and their best smaller
     * key
     * @param keysToDelete list of keys to delete
     * @param shotgunProtocol the protocol containing the enzyme used
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinInferencePreferences the protein inference preferences
     *
     * @return the best smaller group, null if none found.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever an threading error
     * occurred
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the back-end database
     */
    private String getSubgroup(Identification identification, String sharedKey, ProteinMatch sharedProteinMatch, HashMap<String, String> processedKeys,
            HashSet<String> keysToDelete, IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ArrayList<String> sharedAccessions;
        if (sharedProteinMatch == null) {
            sharedAccessions = proteinGroupCache.get(sharedKey);
            if (sharedAccessions == null) {
                sharedProteinMatch = identification.getProteinMatch(sharedKey, false);
            }
        }
        if (sharedProteinMatch != null) {
            sharedAccessions = sharedProteinMatch.getTheoreticProteinsAccessions();
        } else {
            sharedAccessions = getProteins(sharedKey);
        }
        HashSet<String> candidateUnique = new HashSet<String>(1);
        HashSet<String> sharedAccessionsAsSet = null;

        for (String accession : sharedAccessions) {
            HashSet<String> otherGroups = identification.getProteinMap().get(accession);
            for (String uniqueKey : otherGroups) {
                if (!uniqueKey.equals(sharedKey)) {
                    ArrayList<String> uniqueAccessions = proteinGroupCache.get(uniqueKey);
                    if (uniqueAccessions == null) {
                        ProteinMatch uniqueProteinMatch = identification.getProteinMatch(uniqueKey, false);
                        if (uniqueProteinMatch != null) {
                            uniqueAccessions = uniqueProteinMatch.getTheoreticProteinsAccessions();
                        } else {
                            uniqueAccessions = getProteins(uniqueKey);
                        }
                    }
                    if (sharedAccessions.size() >= uniqueAccessions.size()) {
                        if (sharedAccessionsAsSet == null) {
                            sharedAccessionsAsSet = new HashSet<String>(sharedAccessions);
                        }
                        if (ProteinMatch.contains(sharedAccessionsAsSet, uniqueAccessions) && !keysToDelete.contains(uniqueKey)) {
                            String subGroup = uniqueKey;
                            if (uniqueAccessions.size() > 1) {
                                String reducedGroup = processedKeys.get(uniqueKey);
                                if (reducedGroup == null) {
                                    reducedGroup = getSubgroup(identification, uniqueKey, null, processedKeys, keysToDelete, identificationParameters, identificationFeaturesGenerator);
                                    if (reducedGroup != null) {
                                        mergeProteinGroups(identification, uniqueKey, reducedGroup, keysToDelete);
                                        processedKeys.put(uniqueKey, reducedGroup);
                                        subGroup = reducedGroup;
                                    } else {
                                        processedKeys.put(uniqueKey, uniqueKey);
                                    }
                                }
                            }
                            if (!candidateUnique.contains(subGroup)) {
                                candidateUnique.add(subGroup);
                            }
                        }
                    }
                }
            }
        }

        String minimalKey = null;

        if (!candidateUnique.isEmpty()) {
            ArrayList<String> keys = new ArrayList<String>(candidateUnique.size());
            for (String accession : candidateUnique) {
                if (!keysToDelete.contains(accession)) {
                    keys.add(accession);
                }
            }

            if (!keys.isEmpty()) {
                ProteinMatch match = identification.getProteinMatch(sharedKey);
                HashMap<String, Integer> preferenceReason = new HashMap<String, Integer>();
                for (String key1 : keys) {
                    for (String accession1 : ProteinMatch.getAccessions(key1)) {
                        if (minimalKey == null) {
                            preferenceReason = new HashMap<String, Integer>();
                            boolean best = true;
                            for (String key2 : keys) {
                                if (!key1.equals(key2)) {
                                    if (!ProteinMatch.contains(key1, key2)) {
                                        if (!ProteinMatch.getCommonProteins(key1, key2).isEmpty()) {
                                            best = false;
                                        }
                                        for (String accession2 : ProteinMatch.getAccessions(key2)) {
                                            int tempPrefernce = compareMainProtein(match, accession2, match, accession1, identificationFeaturesGenerator, identificationParameters);
                                            if (tempPrefernce != 1) {
                                                best = false;
                                            } else {
                                                if (preferenceReason.containsKey(accession2)) {
                                                    tempPrefernce = Math.min(preferenceReason.get(accession2), tempPrefernce);
                                                }
                                                preferenceReason.put(accession2, tempPrefernce);
                                            }
                                        }
                                    }
                                }
                            }
                            if (best) {
                                ArrayList<String> accessions = ProteinMatch.getOtherProteins(sharedKey, key1);
                                for (String accession2 : accessions) {
                                    int tempPrefernce = compareMainProtein(match, accession2, match, accession1, identificationFeaturesGenerator, identificationParameters);
                                    if (tempPrefernce == 0) {
                                        best = false;
                                        break;
                                    } else {
                                        if (preferenceReason.containsKey(accession2)) {
                                            tempPrefernce = Math.min(preferenceReason.get(accession2), tempPrefernce);
                                        }
                                        preferenceReason.put(accession2, tempPrefernce);
                                    }
                                }
                                if (best && minimalKey == null) {
                                    minimalKey = key1;
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    if (minimalKey != null) {
                        for (String key2 : keys) {
                            if (!key2.equals(minimalKey) && !keysToDelete.contains(key2)) {
                                keysToDelete.add(key2);
                                for (int reason : preferenceReason.values()) {
                                    if (reason == 1) {
                                        enzymaticIssue++;
                                    }
                                    if (reason == 2) {
                                        evidenceIssue++;
                                    }
                                    if (reason == 3) {
                                        uncharacterizedIssue++;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        return minimalKey;
    }

    /**
     * Puts the peptide of the shared group in the unique group and adds the
     * shared group to the list of proteins to delete.
     *
     * @param identification the identification whether to get the matches
     * @param sharedGroup the key of the shared group
     * @param uniqueGroup the key of the unique group
     * @param keysToDelete list of keys to be deleted where sharedGroup will be
     * added
     *
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void mergeProteinGroups(Identification identification, String sharedGroup, String uniqueGroup, HashSet<String> keysToDelete)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch sharedMatch = identification.getProteinMatch(sharedGroup);
        ProteinMatch uniqueMatch = identification.getProteinMatch(uniqueGroup);

        for (String peptideKey : sharedMatch.getPeptideMatchesKeys()) {
            uniqueMatch.addPeptideMatchKey(peptideKey);
        }

        keysToDelete.add(sharedGroup);
        explainedGroup++;
    }

    /**
     * Retains the best scoring of intricate groups.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param metrics if provided protein metrics will be loaded while iterating
     * the groups
     * @param proteinMap the protein matches scoring map
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification feature
     * generator
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public void retainBestScoringGroups(Identification identification, Metrics metrics, ProteinMap proteinMap,
            IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator, WaitingHandler waitingHandler)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Simplifying Redundant Protein Groups. Please Wait...");

        ArrayList<String> toRemove = new ArrayList<String>();
        int maxProteinKeyLength = 0;
        ProteinInferencePreferences proteinInferencePreferences = identificationParameters.getProteinInferencePreferences();

        int max = 2 * identification.getProteinIdentification().size();
        if (proteinInferencePreferences.getSimplifyGroups() && proteinInferencePreferences.getSimplifyGroupsScore()) {
            max += identification.getProteinIdentification().size();
        }
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        if (proteinInferencePreferences.getSimplifyGroups() && proteinInferencePreferences.getSimplifyGroupsScore()) {
            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

            while (proteinMatchesIterator.hasNext()) {

                ProteinMatch proteinMatch = proteinMatchesIterator.next();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                String proteinSharedKey = proteinMatch.getKey();
                ArrayList<String> sharedAccessions = proteinMatch.getTheoreticProteinsAccessions();

                if (sharedAccessions.size() > 1) {

                    HashSet<String> sharedAccessionsAsSet = null;
                    psParameter = (PSParameter) identification.getProteinMatchParameter(proteinSharedKey, psParameter);
                    double sharedProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                    boolean better = false;

                    for (String accession : sharedAccessions) {
                        HashSet<String> otherGroups = identification.getProteinMap().get(accession);
                        for (String proteinUniqueKey : otherGroups) {
                            if (!proteinUniqueKey.equals(proteinSharedKey)) {
                                ProteinMatch uniqueProteinMatch = identification.getProteinMatch(proteinUniqueKey, false);
                                ArrayList<String> uniqueAccessions;
                                if (uniqueProteinMatch != null) {
                                    uniqueAccessions = uniqueProteinMatch.getTheoreticProteinsAccessions();
                                } else {
                                    uniqueAccessions = getProteins(proteinUniqueKey);
                                }
                                if (sharedAccessions.size() >= uniqueAccessions.size()) {
                                    if (sharedAccessionsAsSet == null) {
                                        sharedAccessionsAsSet = new HashSet<String>(sharedAccessions);
                                    }
                                    if (ProteinMatch.contains(sharedAccessionsAsSet, uniqueAccessions)) {
                                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinUniqueKey, psParameter);
                                        double uniqueProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                                        ProteinMatch proteinUnique = identification.getProteinMatch(proteinUniqueKey);
                                        ProteinMatch proteinShared = identification.getProteinMatch(proteinSharedKey);
                                        for (String sharedPeptideKey : proteinShared.getPeptideMatchesKeys()) {
                                            proteinUnique.addPeptideMatchKey(sharedPeptideKey);
                                        }
                                        identification.updateProteinMatch(proteinUnique);
                                        if (uniqueProteinProbabilityScore <= sharedProteinProbabilityScore) {
                                            better = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (better) {
                        toRemove.add(proteinSharedKey);
                    } else {
                        waitingHandler.increaseSecondaryProgressCounter();
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                for (String proteinKey : toRemove) {
                    psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                    proteinMap.removePoint(psParameter.getProteinProbabilityScore(), ProteinMatch.isDecoy(proteinKey));
                    identification.removeProteinMatch(proteinKey);
                    waitingHandler.increaseSecondaryProgressCounter();
                }
                proteinMap.cleanUp();
            }
        }

        clearCache();
        ProteinMatch.clearCache();

        int nSolved = toRemove.size();
        int nGroups = 0;
        int nLeft = 0;

        waitingHandler.setWaitingText("Inferring PI status, sorting proteins. Please Wait...");
        // As we go through all protein ids, keep the sorted list of proteins and maxima in the instance of the Metrics class to pass them to the GUI afterwards
        // proteins are sorted according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
        HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap
                = new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
        PSParameter probabilities = new PSParameter();
        double maxMW = 0;

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            ProteinMatch proteinMatch = proteinMatchesIterator.next();

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            String proteinKey = proteinMatch.getKey();

            if (!ProteinMatch.isDecoy(proteinKey)) {
                probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                double score = probabilities.getProteinProbabilityScore();
                int nPeptides = -proteinMatch.getPeptideMatchesKeys().size();
                int nSpectra = 0;

                Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

                if (currentProtein != null) {
                    double mw = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                    if (mw > maxMW) {
                        maxMW = mw;
                    }
                }

                PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, null);
                while (peptideMatchesIterator.hasNext()) {
                    PeptideMatch peptideMatch = peptideMatchesIterator.next();
                    nSpectra -= peptideMatch.getSpectrumCount();
                }
                if (!orderMap.containsKey(score)) {
                    orderMap.put(score, new HashMap<Integer, HashMap<Integer, ArrayList<String>>>());
                }

                if (!orderMap.get(score).containsKey(nPeptides)) {
                    orderMap.get(score).put(nPeptides, new HashMap<Integer, ArrayList<String>>());
                }

                if (!orderMap.get(score).get(nPeptides).containsKey(nSpectra)) {
                    orderMap.get(score).get(nPeptides).put(nSpectra, new ArrayList<String>());
                }
                orderMap.get(score).get(nPeptides).get(nSpectra).add(proteinKey);

                // save the lenght of the longest protein accession number
                if (proteinMatch.getMainMatch().length() > maxProteinKeyLength) {
                    maxProteinKeyLength = proteinMatch.getMainMatch().length();
                }
            }

            ArrayList<String> accessions = new ArrayList<String>(Arrays.asList(ProteinMatch.getAccessions(proteinKey)));
            Collections.sort(accessions);
            String mainKey = accessions.get(0);

            if (accessions.size() > 1) {
                boolean similarityFound = false;
                boolean allSimilar = false;
                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                for (String accession : accessions) {
                    if (compareMainProtein(proteinMatch, mainKey, proteinMatch, accession, identificationFeaturesGenerator, identificationParameters) > 0) {
                        mainKey = accession;
                    }
                }
                for (int i = 0; i < accessions.size() - 1; i++) {
                    for (int j = i + 1; j < accessions.size(); j++) {
                        if (getSimilarity(accessions.get(i), accessions.get(j))) {
                            similarityFound = true;
                            if (compareMainProtein(proteinMatch, mainKey, proteinMatch, accessions.get(j), identificationFeaturesGenerator, identificationParameters) > 0) {
                                mainKey = accessions.get(i);
                            }
                            break;
                        }
                    }
                    if (similarityFound) {
                        break;
                    }
                }
                if (similarityFound) {
                    allSimilar = true;
                    for (String key : accessions) {
                        if (!mainKey.equals(key)) {
                            if (!getSimilarity(mainKey, key)) {
                                allSimilar = false;
                                break;
                            }
                        }
                    }
                }
                if (!similarityFound) {
                    psParameter.setProteinInferenceClass(PSParameter.UNRELATED);
                    nGroups++;
                    nLeft++;
                    identification.updateProteinMatchParameter(proteinKey, psParameter);

                    identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null, false);
                    for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                        psParameter.setProteinInferenceClass(PSParameter.UNRELATED);
                        identification.updatePeptideMatchParameter(peptideKey, psParameter);
                    }

                } else if (!allSimilar) {
                    psParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);
                    nGroups++;
                    nSolved++;
                    identification.updateProteinMatchParameter(proteinKey, psParameter);

                    identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null, false);
                    for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                        psParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);
                        identification.updatePeptideMatchParameter(peptideKey, psParameter);
                    }

                } else {
                    psParameter.setProteinInferenceClass(PSParameter.RELATED);
                    nGroups++;
                    nSolved++;
                    identification.updateProteinMatchParameter(proteinKey, psParameter);

                    String mainMatch = proteinMatch.getMainMatch();
                    PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, null);
                    while (peptideMatchesIterator.hasNext()) {
                        PeptideMatch peptideMatch = peptideMatchesIterator.next();
                        String peptideKey = peptideMatch.getKey();
                        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                        boolean unrelated = false;
                        for (String proteinAccession : peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {
                            if (!proteinKey.contains(proteinAccession)) {
                                if (!getSimilarity(mainMatch, proteinAccession)) {
                                    unrelated = true;
                                    break;
                                }
                            }
                        }
                        if (unrelated) {
                            psParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);
                        } else {
                            psParameter.setProteinInferenceClass(PSParameter.RELATED);
                        }
                        identification.updatePeptideMatchParameter(peptideKey, psParameter);
                    }
                }
            } else {
                String mainMatch = proteinMatch.getMainMatch();
                PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, null);
                while (peptideMatchesIterator.hasNext()) {
                    PeptideMatch peptideMatch = peptideMatchesIterator.next();
                    String peptideKey = peptideMatch.getKey();
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                    boolean unrelated = false;
                    boolean otherProtein = false;
                    for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {
                        if (!proteinKey.contains(protein)) {
                            otherProtein = true;
                            if (!getSimilarity(mainMatch, protein)) {
                                unrelated = true;
                                break;
                            }
                        }
                    }
                    if (otherProtein) {
                        psParameter.setProteinInferenceClass(PSParameter.RELATED);
                    }
                    if (unrelated) {
                        psParameter.setProteinInferenceClass(PSParameter.UNRELATED);
                    }
                    identification.updatePeptideMatchParameter(peptideKey, psParameter);
                }
            }

            if (ProteinMatch.getNProteins(proteinKey) > 1) {
                if (!proteinMatch.getMainMatch().equals(mainKey)) {
                    proteinMatch.setMainMatch(mainKey);
                    identification.updateProteinMatch(proteinMatch);
                }
            }

            waitingHandler.increaseSecondaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        ArrayList<String> proteinList = new ArrayList<String>();
        ArrayList<Double> scoreList = new ArrayList<Double>(orderMap.keySet());
        Collections.sort(scoreList);
        int maxPeptides = 0;
        int maxSpectra = 0;

        for (double currentScore : scoreList) {
            ArrayList<Integer> nPeptideList = new ArrayList<Integer>(orderMap.get(currentScore).keySet());
            Collections.sort(nPeptideList);
            if (nPeptideList.get(0) < maxPeptides) {
                maxPeptides = nPeptideList.get(0);
            }
            for (int currentNPeptides : nPeptideList) {
                ArrayList<Integer> nPsmList = new ArrayList<Integer>(orderMap.get(currentScore).get(currentNPeptides).keySet());
                Collections.sort(nPsmList);
                if (nPsmList.get(0) < maxSpectra) {
                    maxSpectra = nPsmList.get(0);
                }
                for (int currentNPsms : nPsmList) {
                    ArrayList<String> tempList = orderMap.get(currentScore).get(currentNPeptides).get(currentNPsms);
                    Collections.sort(tempList);
                    proteinList.addAll(tempList);

                    waitingHandler.increaseSecondaryProgressCounter(tempList.size());
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }
        }

        if (metrics != null) {
            metrics.setProteinKeys(proteinList);
            metrics.setMaxNPeptides(-maxPeptides);
            metrics.setMaxNSpectra(-maxSpectra);
            metrics.setMaxMW(maxMW);
            metrics.setMaxProteinKeyLength(maxProteinKeyLength);
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        String report;
        if (nSolved > 0) {
            report = nSolved + " conflicts resolved. ";
        } else {
            report = "";
        }
        report += nGroups + " protein groups remaining (" + nLeft + " suspicious).";
        waitingHandler.appendReport(report, true, true);
    }

    /**
     * Parses a protein description retaining only words longer than 3
     * characters.
     *
     * @param proteinAccession the accession of the inspected protein
     * @return description words longer than 3 characters
     */
    private ArrayList<String> parseDescription(String proteinAccession) throws IOException, IllegalArgumentException, InterruptedException, ClassNotFoundException {
        String description = sequenceFactory.getHeader(proteinAccession).getSimpleProteinDescription();

        if (description == null) {
            return new ArrayList<String>();
        }

        ArrayList<String> result = new ArrayList<String>();
        for (String component : description.split(" ")) {
            if (component.length() > 3) {
                result.add(component);
            }
        }
        return result;
    }

    /**
     * Checks whether a new main protein (newAccession) of the new protein match
     * (newProteinMatch) is better than another one main protein (oldAccession)
     * of another protein match (oldProteinMatch). First checks the protein
     * evidence level (if available), if not there then checks the protein
     * description and peptide enzymaticity.
     *
     * @param oldProteinMatch the protein match of oldAccession
     * @param oldAccession the accession of the old protein
     * @param newProteinMatch the protein match of newAccession
     * @param newAccession the accession of the new protein
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param identificationParameters the identification parameters
     *
     * @return the product of the comparison: 1: better enzymaticity, 2: better
     * evidence, 3: better characterization, 0: equal or not better
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever an threading error
     * occurred
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the back-end database
     */
    private int compareMainProtein(ProteinMatch oldProteinMatch, String oldAccession, ProteinMatch newProteinMatch, String newAccession,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, IdentificationParameters identificationParameters)
            throws IOException, InterruptedException, IllegalArgumentException, ClassNotFoundException, SQLException {

        ProteinInferencePreferences proteinInferencePreferences = identificationParameters.getProteinInferencePreferences();
        if (proteinInferencePreferences.getSimplifyGroupsEnzymaticity()) {
            DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();
            if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                boolean newEnzymatic = identificationFeaturesGenerator.hasEnzymaticPeptides(newProteinMatch, newAccession);
                boolean oldEnzymatic = identificationFeaturesGenerator.hasEnzymaticPeptides(oldProteinMatch, oldAccession);
                if (newEnzymatic && !oldEnzymatic) {
                    return 1;
                } else if (!newEnzymatic && oldEnzymatic) {
                    return 0;
                }
            }
        }

        if (proteinInferencePreferences.getSimplifyGroupsEvidence()) {
            String evidenceLevelOld = sequenceFactory.getHeader(oldAccession).getProteinEvidence();
            String evidenceLevelNew = sequenceFactory.getHeader(newAccession).getProteinEvidence();

            // compare protein evidence levels
            if (evidenceLevelOld != null && evidenceLevelNew != null) {
                try {
                    Integer levelOld = new Integer(evidenceLevelOld);
                    Integer levelNew = new Integer(evidenceLevelNew);
                    if (levelNew < levelOld) {
                        return 2;
                    } else if (levelOld < levelNew) {
                        return 0;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            // only the new match has evidence information
            if (evidenceLevelOld == null && evidenceLevelNew != null) {
                return 2;
            }
            // only the old match has evidence information
            if (evidenceLevelOld != null && evidenceLevelNew == null) {
                return 0;
            }
        }

        if (proteinInferencePreferences.getSimplifyGroupsUncharacterized()) {
            // Compare descriptions for keywords of uncharacterized proteins
            String oldDescription = sequenceFactory.getHeader(oldAccession).getSimpleProteinDescription();
            String newDescription = sequenceFactory.getHeader(newAccession).getSimpleProteinDescription();

            // if the description is not set, return the accessions instead - fix for home made fasta headers
            if (oldDescription == null || oldDescription.trim().isEmpty()) {
                oldDescription = oldAccession;
            }
            if (newDescription == null || newDescription.trim().isEmpty()) {
                newDescription = newAccession;
            }

            boolean oldUncharacterized = false, newUncharacterized = false;
            String[] keyWords = {"Uncharacterized", "Putative"};
            for (String keyWord : keyWords) {
                if (newDescription.contains(keyWord)) {
                    newUncharacterized = true;
                }
                if (oldDescription.contains(keyWord)) {
                    oldUncharacterized = true;
                }
            }
            if (oldUncharacterized && !newUncharacterized) {
                return 3;
            } else if (!oldUncharacterized && newUncharacterized) {
                return 0;
            }
        }

        return 0;
    }

    /**
     * Simplistic method comparing protein similarity. Returns true if both
     * proteins come from the same gene or if the descriptions are of same
     * length and present more than half similar words.
     *
     * @param primaryProteinAccession accession number of the first protein
     * @param secondaryProteinAccession accession number of the second protein
     * @return a boolean indicating whether the proteins are similar
     */
    private boolean getSimilarity(String primaryProteinAccession, String secondaryProteinAccession) throws IOException, IllegalArgumentException, InterruptedException, ClassNotFoundException {

        String geneNamePrimaryProtein = sequenceFactory.getHeader(primaryProteinAccession).getGeneName();
        String geneNameSecondaryProtein = sequenceFactory.getHeader(secondaryProteinAccession).getGeneName();
        boolean sameGene = false;

        // compare the gene names
        if (geneNamePrimaryProtein != null && geneNameSecondaryProtein != null) {
            sameGene = geneNamePrimaryProtein.equalsIgnoreCase(geneNameSecondaryProtein);
        }

        if (sameGene) {
            return true;
        } else {

            // compare gene names, similar gene names often means related proteins, like CPNE3 and CPNE2
            if (geneNamePrimaryProtein != null && geneNameSecondaryProtein != null) {

                // one gene name is a substring of the other, for example: EEF1A1 and EEF1A1P5
                if (geneNamePrimaryProtein.contains(geneNameSecondaryProtein) || geneNameSecondaryProtein.contains(geneNamePrimaryProtein)) {
                    return true;
                }

                // equal but for the last character, for example: CPNE3 and CPNE2
                if ((geneNameSecondaryProtein.length() > 2 && geneNamePrimaryProtein.contains(geneNameSecondaryProtein.substring(0, geneNameSecondaryProtein.length() - 2)))
                        || (geneNamePrimaryProtein.length() > 2 && geneNameSecondaryProtein.contains(geneNamePrimaryProtein.substring(0, geneNamePrimaryProtein.length() - 2)))) {
                    return true;
                }

                // equal but for the two last characters, for example: CPNE11 and CPNE12
                if ((geneNameSecondaryProtein.length() > 3 && geneNamePrimaryProtein.contains(geneNameSecondaryProtein.substring(0, geneNameSecondaryProtein.length() - 3)))
                        || (geneNamePrimaryProtein.length() > 3 && geneNameSecondaryProtein.contains(geneNamePrimaryProtein.substring(0, geneNamePrimaryProtein.length() - 3)))) {
                    return true;
                }

                // @TODO: support more complex gene families?
            }

            // compare the protein descriptions, less secure than gene names
            ArrayList<String> primaryDescription = parseDescription(primaryProteinAccession);
            ArrayList<String> secondaryDescription = parseDescription(secondaryProteinAccession);

            if (primaryDescription.size() > secondaryDescription.size()) {
                int nMatch = 0;
                for (String secondaryDescription1 : secondaryDescription) {
                    if (primaryDescription.contains(secondaryDescription1)) {
                        nMatch++;
                    }
                }
                return nMatch >= secondaryDescription.size() / 2;
            } else {
                int nMatch = 0;
                for (String primaryDescription1 : primaryDescription) {
                    if (secondaryDescription.contains(primaryDescription1)) {
                        nMatch++;
                    }
                }
                return nMatch >= primaryDescription.size() / 2;
            }
        }
    }

    /**
     * Returns the proteins of a group key. Uses
     * ProteinMatch.getAccessions(groupKey) after checking if the protein group
     * is in cache. Manages cache update and size.
     *
     * @param groupKey the group key of interest
     *
     * @return the proteins of a group key
     */
    private ArrayList<String> getProteins(String groupKey) {
        ArrayList<String> result = proteinGroupCache.get(groupKey);
        if (result == null) {
            result = new ArrayList<String>(Arrays.asList(ProteinMatch.getAccessions(groupKey)));
            if (result.size() > sizeOfProteinsInCache) {
                proteinGroupCache.put(groupKey, result);
                if (proteinGroupCache.size() > cacheSize) {
                    int smallestSize = sizeOfProteinsInCache;
                    String smallestGroup = null;
                    for (String key : proteinGroupCache.keySet()) {
                        ArrayList<String> group = proteinGroupCache.get(key);
                        if (smallestGroup == null || group.size() < smallestSize) {
                            smallestGroup = key;
                            smallestSize = group.size();
                        }
                    }
                    proteinGroupCache.remove(smallestGroup);
                    sizeOfProteinsInCache = smallestSize;
                }
            }
        }
        return result;
    }

    /**
     * Clears the cache.
     */
    private void clearCache() {
        proteinGroupCache.clear();
        sizeOfProteinsInCache = 10;
    }
}
