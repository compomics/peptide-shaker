package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.ProteinMap;
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
     * Reduce artifact groups which can be explained by a simpler group.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param shotgunProtocol the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     */
    public void removeRedundantGroups(Identification identification, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {

        ArrayList<String> toRemove = new ArrayList<String>();
        int max = identification.getProteinIdentification().size();

        identification.loadProteinMatches(waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Symplifying Protein Groups. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(max);
        }

        HashSet<String> toDelete = new HashSet<String>();
        HashMap<String, String> processedKeys = new HashMap<String, String>();

        for (String proteinSharedKey : identification.getProteinIdentification()) {
            if (ProteinMatch.getNProteins(proteinSharedKey) > 1) {
                if (!processedKeys.containsKey(proteinSharedKey)) {
                    String uniqueKey = getSubgroup(identification, proteinSharedKey, processedKeys, toDelete, shotgunProtocol, identificationParameters);
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
                waitingHandler.setMaxSecondaryProgressCounter(toRemove.size());
            }

            for (String proteinKey : toRemove) { // @TODO: nothing is ever added to this map..?
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
     *
     * @return the best smaller group, null if none found.
     *
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private String getSubgroup(Identification identification, String sharedKey, HashMap<String, String> processedKeys,
            HashSet<String> keysToDelete, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        String[] sharedAccessions = ProteinMatch.getAccessions(sharedKey);
        ArrayList<String> candidateUnique = new ArrayList<String>();

        for (String accession : sharedAccessions) {
            for (String uniqueGroupCandidate : identification.getProteinMap().get(accession)) {
                if (ProteinMatch.contains(sharedKey, uniqueGroupCandidate) && !keysToDelete.contains(uniqueGroupCandidate)) {
                    String subGroup = uniqueGroupCandidate;
                    if (ProteinMatch.getNProteins(uniqueGroupCandidate) > 1) {
                        String reducedGroup = processedKeys.get(uniqueGroupCandidate);
                        if (reducedGroup == null) {
                            reducedGroup = getSubgroup(identification, uniqueGroupCandidate, processedKeys, keysToDelete, shotgunProtocol, identificationParameters);
                            if (reducedGroup != null) {
                                mergeProteinGroups(identification, uniqueGroupCandidate, reducedGroup, keysToDelete);
                                processedKeys.put(uniqueGroupCandidate, reducedGroup);
                                subGroup = reducedGroup;
                            } else {
                                processedKeys.put(uniqueGroupCandidate, uniqueGroupCandidate);
                            }
                        }
                    }
                    if (!candidateUnique.contains(subGroup)) {
                        candidateUnique.add(subGroup);
                    }
                }
            }
        }

        ArrayList<String> keys = new ArrayList<String>();
        for (String accession : candidateUnique) {
            if (!keysToDelete.contains(accession)) {
                keys.add(accession);
            }
        }

        String minimalKey = null;
        if (keys.size() > 1) {
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
                                        int tempPrefernce = compareMainProtein(match, accession2, match, accession1, shotgunProtocol, identificationParameters);
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
                                int tempPrefernce = compareMainProtein(match, accession2, match, accession1, shotgunProtocol, identificationParameters);
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

        if (!keysToDelete.contains(sharedGroup)) {
            keysToDelete.add(sharedGroup);
            explainedGroup++;
        }
    }

    /**
     * Retains the best scoring of intricate groups.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param metrics if provided protein metrics will be loaded while iterating
     * the groups
     * @param proteinMap the protein matches scoring map
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     */
    public void retainBestScoringGroups(Identification identification, Metrics metrics, ProteinMap proteinMap,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Simplifying Redundant Protein Groups. Please Wait...");

        ArrayList<String> toRemove = new ArrayList<String>();
        int maxProteinKeyLength = 0;

        int max = 3 * identification.getProteinIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            ProteinMatch proteinMatch = proteinMatchesIterator.next();

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            String proteinSharedKey = proteinMatch.getKey();

            if (ProteinMatch.getNProteins(proteinSharedKey) > 1) {

                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinSharedKey, psParameter);
                double sharedProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                boolean better = false;

                for (String accession : ProteinMatch.getAccessions(proteinSharedKey)) {
                    for (String proteinUniqueKey : identification.getProteinMap().get(accession)) {
                        if (ProteinMatch.contains(proteinSharedKey, proteinUniqueKey)) {
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

        for (String proteinKey : toRemove) {
            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
            proteinMap.removePoint(psParameter.getProteinProbabilityScore(), ProteinMatch.isDecoy(proteinKey));
            identification.removeProteinMatch(proteinKey);
            waitingHandler.increaseSecondaryProgressCounter();
        }

        int nSolved = toRemove.size();
        int nGroups = 0;
        int nLeft = 0;

        // As we go through all protein ids, keep the sorted list of proteins and maxima in the instance of the Metrics class to pass them to the GUI afterwards
        // proteins are sorted according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
        HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap
                = new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
        PSParameter probabilities = new PSParameter();
        double maxMW = 0;

        proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

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

                identification.loadPeptideMatches(proteinMatch.getPeptideMatchesKeys(), null);
                for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
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
                    if (compareMainProtein(proteinMatch, mainKey, proteinMatch, accession, shotgunProtocol, identificationParameters) > 0) {
                        mainKey = accession;
                    }
                }
                for (int i = 0; i < accessions.size() - 1; i++) {
                    for (int j = i + 1; j < accessions.size(); j++) {
                        if (getSimilarity(accessions.get(i), accessions.get(j))) {
                            similarityFound = true;
                            if (compareMainProtein(proteinMatch, mainKey, proteinMatch, accessions.get(j), shotgunProtocol, identificationParameters) > 0) {
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

                    identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null);
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

                    identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null);
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
                    identification.loadPeptideMatches(proteinMatch.getPeptideMatchesKeys(), null);
                    identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null);
                    for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
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
                identification.loadPeptideMatches(proteinMatch.getPeptideMatchesKeys(), null);
                identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null);

                for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
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
        waitingHandler.appendReport(nSolved + " conflicts resolved. " + nGroups + " protein groups remaining (" + nLeft + " suspicious).", true, true);
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
     *
     * @return the product of the comparison: 1: better enzymaticity, 2: better
     * evidence, 3: better characterization, 0: equal or not better
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws IllegalArgumentException
     */
    private int compareMainProtein(ProteinMatch oldProteinMatch, String oldAccession, ProteinMatch newProteinMatch, String newAccession,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws IOException, InterruptedException, IllegalArgumentException, ClassNotFoundException {

        Enzyme enzyme = shotgunProtocol.getEnzyme();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        if (enzyme != null && !enzyme.isSemiSpecific()) { // null enzymes should not occur, but could happen with old search param files

            // @TODO: could semi-specific, top-down, whole protein, and non enzyme be handled better??
            boolean newEnzymatic = newProteinMatch.hasEnzymaticPeptide(newAccession, enzyme, sequenceMatchingPreferences);
            boolean oldEnzymatic = oldProteinMatch.hasEnzymaticPeptide(oldAccession, enzyme, sequenceMatchingPreferences);
            if (newEnzymatic && !oldEnzymatic) {
                return 1;
            } else if (!newEnzymatic && oldEnzymatic) {
                return 0;
            }
        }

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

        // protein evidence level missing, compare descriptions instead
        String oldDescription = sequenceFactory.getHeader(oldAccession).getSimpleProteinDescription();
        String newDescription = sequenceFactory.getHeader(newAccession).getSimpleProteinDescription();

        // if the description are not set, return the accessions instead - fix for home made fasta headers
        if (oldDescription == null || oldDescription.trim().isEmpty()) {
            oldDescription = oldAccession;
        }
        if (newDescription == null || newDescription.trim().isEmpty()) {
            newDescription = newAccession;
        }

        boolean oldUncharacterized = false, newUncharacterized = false;
        String[] keyWords = {"Uncharacterized", "putative"};
        for (String keyWord : keyWords) {
            if (newDescription.toLowerCase().contains(keyWord)) {
                newUncharacterized = true;
            }
            if (oldDescription.toLowerCase().contains(keyWord)) {
                oldUncharacterized = true;
            }
        }
        if (oldUncharacterized && !newUncharacterized) {
            return 3;
        } else if (!oldUncharacterized && newUncharacterized) {
            return 0;
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
}
