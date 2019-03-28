package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.ProteinInferenceParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.google.common.collect.Sets;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This class groups the methods for protein inference.
 *
 * @author Marc Vaudel
 */
public class ProteinInference {

    public enum GroupSimplificationOption {

        /**
         * Group supported by peptide shared with low evidence protein.
         */
        lowEvidence(0, "protein groups supported by proteins of low evidence"),
        /**
         * Group supported by peptide shared with uncharacterized protein.
         */
        uncharacterized(1, "protein groups supported by uncharacterized proteins"),
        /**
         * Group supported by non-enzymatic shared peptide.
         */
        nonEnzymatic(2, "protein groups supported by non-enzymatic shared peptides"),
        /**
         * Group supported by peptide shared with protein with variant.
         */
        variant(3, "protein groups supported by peptides shared by variant only");
        /**
         * The index of the option.
         */
        public final int index;
        /**
         * The description to write in the report.
         */
        public final String description;

        /**
         * Constructor.
         *
         * @param index the index
         */
        private GroupSimplificationOption(int index, String description) {

            this.index = index;
            this.description = description;

        }

    }
    /**
     * Key words used to flag uncharacterized proteins.
     */
    public static final String[] unchatacterizedKeyWords = {"uncharacterized", "putative"};
    /**
     * Number of groups indexed by option.
     */
    private int[] nDeleted = new int[GroupSimplificationOption.values().length];

    /**
     * Reduce artifact groups which can be explained by a simpler group.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param identificationParameters the identification parameters
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void removeRedundantGroups(Identification identification, IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, WaitingHandler waitingHandler) {

        int max = identification.getProteinIdentification().size();

        if (waitingHandler != null) {

            waitingHandler.setWaitingText("Symplifying Protein Groups. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(max);

        }

        HashSet<Long> proteinGroupKeys = identification.getProteinIdentification();
        HashSet<Long> toDelete = new HashSet<>();
        HashMap<Long, Long> processedKeys = new HashMap<>();

        proteinGroupKeys.parallelStream()
                .map(key -> identification.getProteinMatch(key))
                .filter(proteinMatch -> proteinMatch.getNProteins() > 1)
                .forEach(proteinSharedGroup -> {

                    long sharedKey = proteinSharedGroup.getKey();

                    if (!processedKeys.containsKey(sharedKey)) {

                        ProteinMatch uniqueGroup = getSubgroup(identification, proteinSharedGroup, processedKeys, toDelete, sequenceProvider, proteinDetailsProvider, identificationParameters);

                        if (uniqueGroup != null) {

                            mergeProteinGroups(proteinSharedGroup, uniqueGroup);

                            toDelete.add(sharedKey);
                            processedKeys.put(sharedKey, uniqueGroup.getKey());

                        } else {

                            processedKeys.put(sharedKey, sharedKey);

                        }
                    }

                    if (waitingHandler != null) {

                        if (waitingHandler.isRunCanceled()) {

                            return;

                        }

                        waitingHandler.increaseSecondaryProgressCounter();

                    }

                });

        int totalSimplified = Arrays.stream(nDeleted).sum();

        if (totalSimplified > 0) {

            if (waitingHandler != null) {

                waitingHandler.appendReport(toDelete.size() + " unlikely protein mappings found:", true, true);

                String padding = "    ";

                for (int i = 0; i < GroupSimplificationOption.values().length; i++) {

                    int iSimplified = nDeleted[i];

                    if (iSimplified > 0) {

                        GroupSimplificationOption option = GroupSimplificationOption.values()[i];

                        waitingHandler.appendReport(padding + "- " + iSimplified + " " + option.description + ".", true, true);

                    }
                }

                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxSecondaryProgressCounter(toDelete.size());

            }

            toDelete.stream()
                    .forEach(key -> {

                        identification.removeObject(key);

                        if (waitingHandler != null) {

                            if (waitingHandler.isRunCanceled()) {

                                return;

                            }

                            waitingHandler.increaseSecondaryProgressCounter();

                        }
                    });
        }
    }

    /**
     * Returns the best subgroup of a protein key, null if none found. If
     * intermediate groups are found they will be processed. Processed keys are
     * stored in processedKeys. Keys to delete are stored in keysToDelete.
     * Returns null if no simpler group is found.
     *
     * @param identification the identification where to get the matches from.
     * @param sharedProteinMatch the shared protein match
     * @param processedKeys map of already processed keys and their best smaller
     * key
     * @param toDelete list of keys to delete
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param identificationParameters the identification parameters
     *
     * @return the best smaller group, null if none found.
     */
    private ProteinMatch getSubgroup(Identification identification, ProteinMatch sharedProteinMatch, HashMap<Long, Long> processedKeys,
            HashSet<Long> toDelete, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters) {

        long sharedKey = sharedProteinMatch.getKey();

        String[] sharedAccessions = sharedProteinMatch.getAccessions();
        HashSet<String> sharedAccessionsAsSet = Sets.newHashSet(sharedAccessions);

        HashMap<Long, ProteinMatch> candidateUnique = new HashMap<>(0);

        for (String accession : sharedAccessions) {

            HashSet<Long> otherGroups = identification.getProteinMap().get(accession);

            for (long uniqueKey : otherGroups) {

                if (uniqueKey != sharedKey && !toDelete.contains(uniqueKey) && !candidateUnique.containsKey(uniqueKey)) {

                    ProteinMatch uniqueProteinMatch = identification.getProteinMatch(uniqueKey);

                    String[] uniqueAccessions = uniqueProteinMatch.getAccessions();

                    if (sharedAccessions.length > uniqueAccessions.length) {

                        if (Arrays.stream(uniqueAccessions)
                                .allMatch(uniqueAccession -> sharedAccessionsAsSet.contains(uniqueAccession))) {

                            if (uniqueAccessions.length > 1) {

                                Long reducedGroupKey = processedKeys.get(uniqueKey);

                                if (reducedGroupKey == null) {

                                    ProteinMatch reducedGroup = getSubgroup(identification, uniqueProteinMatch, processedKeys, toDelete, sequenceProvider, proteinDetailsProvider, identificationParameters);

                                    if (reducedGroup != null) {

                                        reducedGroupKey = reducedGroup.getKey();
                                        mergeProteinGroups(sharedProteinMatch, uniqueProteinMatch);
                                        processedKeys.put(uniqueKey, reducedGroupKey);
                                        toDelete.add(sharedKey);

                                        candidateUnique.put(reducedGroupKey, reducedGroup);

                                    } else {

                                        processedKeys.put(uniqueKey, uniqueKey);
                                        candidateUnique.put(uniqueKey, uniqueProteinMatch);

                                    }

                                } else {

                                    candidateUnique.put(reducedGroupKey, identification.getProteinMatch(reducedGroupKey));

                                }

                            } else {

                                candidateUnique.put(uniqueKey, uniqueProteinMatch);

                            }
                        }
                    }
                }
            }
        }

        // Look for the smallest group that can be used
        ProteinMatch minimalGroup = null;
        int nUnique = 0, bestSimplification = GroupSimplificationOption.values().length;

        for (Entry<Long, ProteinMatch> entry : candidateUnique.entrySet()) {

            long key = entry.getKey();
            ProteinMatch uniqueMatch = entry.getValue();
            HashSet<String> uniqueAccessions = getUniqueAccessions(sharedProteinMatch, uniqueMatch);

            GroupSimplificationOption groupSimplificationOption = getSimplificationOption(sharedProteinMatch, uniqueAccessions, sequenceProvider, proteinDetailsProvider, identificationParameters, identification);

            if (groupSimplificationOption != null) {

                if (uniqueAccessions.size() >= nUnique) {

                    if (uniqueAccessions.size() > nUnique) {

                        nUnique = uniqueAccessions.size();
                        bestSimplification = GroupSimplificationOption.values().length;

                    }

                    if (groupSimplificationOption.index < bestSimplification) {

                        bestSimplification = groupSimplificationOption.index;

                    }

                    if (minimalGroup == null || minimalGroup.getKey() > key) {

                        minimalGroup = uniqueMatch;

                    }
                }
            }
        }

        if (minimalGroup == null) {

            return sharedProteinMatch;

        }

        nDeleted[bestSimplification]++;

        return minimalGroup;

    }

    /**
     * Returns the accessions unique to a protein group.
     *
     * @param sharedMatch the shared protein group
     * @param uniqueMatch the unique protein group
     *
     * @return a n array of accessions unique to a match
     */
    private HashSet<String> getUniqueAccessions(ProteinMatch sharedMatch, ProteinMatch uniqueMatch) {

        HashSet<String> uniqueAsSet = Sets.newHashSet(uniqueMatch.getAccessions());

        return Arrays.stream(sharedMatch.getAccessions())
                .filter(accession -> !uniqueAsSet.contains(accession))
                .collect(Collectors.toCollection(HashSet::new));

    }

    /**
     * Returns whether the protein match with unique shared protein accessions
     * can be simplified.
     *
     * @param sharedMatch the protein match
     * @param uniqueAccessions the unique shared protein accessions
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param identificationParameters the identification parameters
     * @param identification the identification
     *
     * @return the protein match simplification option that can be used
     */
    private GroupSimplificationOption getSimplificationOption(ProteinMatch sharedMatch, HashSet<String> uniqueAccessions, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, IdentificationParameters identificationParameters, Identification identification) {

        ProteinInferenceParameters proteinInferenceParameters = identificationParameters.getProteinInferenceParameters();

        // See whether the unique accession have lower level of evidence
        if (proteinInferenceParameters.getSimplifyGroupsEvidence()) {

            Integer evidenceShared = null, evidenceUnique = null;

            for (String accession : sharedMatch.getAccessions()) {

                Integer evidenceLevel = proteinDetailsProvider.getProteinEvidence(accession);

                if (evidenceLevel != null) {

                    if (uniqueAccessions.contains(accession)) {

                        evidenceUnique = evidenceLevel;

                    } else {

                        evidenceShared = evidenceLevel;

                    }
                }
            }

            if (evidenceShared != null && evidenceUnique != null && evidenceUnique < evidenceShared) {

                return GroupSimplificationOption.lowEvidence;

            }
        }

        // See wheter the unique accessions are from uncharacterized proteins
        if (proteinInferenceParameters.getSimplifyGroupsUncharacterized()) {

            boolean uncharacterizedUnique = uniqueAccessions.stream()
                    .allMatch(accession -> isUncharacterized(accession, proteinDetailsProvider));

            if (uncharacterizedUnique) {

                boolean uncharactrizedShared = Arrays.stream(sharedMatch.getAccessions())
                        .filter(accession -> !uniqueAccessions.contains(accession))
                        .allMatch(accession -> isUncharacterized(accession, proteinDetailsProvider));

                if (uncharacterizedUnique && !uncharactrizedShared) {

                    return GroupSimplificationOption.uncharacterized;

                }
            }
        }

        // See whether unique accessions are due to miscleaved peptides
        if (proteinInferenceParameters.getSimplifyGroupsEnzymaticity()) {

            DigestionParameters digestionParameters = identificationParameters.getSearchParameters().getDigestionParameters();

            if (digestionParameters.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme
                    && Arrays.stream(sharedMatch.getPeptideMatchesKeys())
                            .mapToObj(key -> identification.getPeptideMatch(key))
                            .allMatch(peptideMatch -> !PeptideUtils.isEnzymatic(peptideMatch.getPeptide(), sequenceProvider, digestionParameters.getEnzymes()))) {

                return GroupSimplificationOption.nonEnzymatic;

            }
        }

        // See whether unique accessions are due to variant mapping
        if (proteinInferenceParameters.getSimplifyGroupsVariants()) {

            if (identificationParameters.getPeptideVariantsParameters().getnVariants() > 0
                    && Arrays.stream(sharedMatch.getPeptideMatchesKeys())
                            .mapToObj(key -> identification.getPeptideMatch(key))
                            .allMatch(peptideMatch -> uniqueAccessions.stream()
                            .allMatch(accession -> PeptideUtils.isVariant(peptideMatch.getPeptide(), accession)))) {

                return GroupSimplificationOption.variant;

            }
        }

        return null;

    }

    /**
     * Returns a boolean indicating whether the protein is considered as
     * uncharacterized.
     *
     * @param accession the accession of the protein
     * @param proteinDetailsProvider the protein details provider
     *
     * @return a boolean indicating whether the protein is considered as
     * uncharacterized
     */
    public static boolean isUncharacterized(String accession, ProteinDetailsProvider proteinDetailsProvider) {

        String description = proteinDetailsProvider.getSimpleDescription(accession);

        if (description == null) {

            description = proteinDetailsProvider.getDescription(accession);

        }

        final String descriptionFinal = description.toLowerCase();

        return Arrays.stream(unchatacterizedKeyWords)
                .anyMatch(keyWord -> descriptionFinal.contains(keyWord));

    }

    /**
     * Puts the peptide of the shared group in the unique group.
     *
     * @param sharedGroup the shared group
     * @param uniqueGroup the unique group
     */
    private void mergeProteinGroups(ProteinMatch sharedGroup, ProteinMatch uniqueGroup) {

        uniqueGroup.addPeptideMatchKeys(sharedGroup.getPeptideMatchesKeys());

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
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void inferPiStatus(Identification identification, Metrics metrics, TargetDecoyMap proteinMap,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, WaitingHandler waitingHandler) {

        waitingHandler.setWaitingText("Inferring PI status, sorting proteins. Please Wait...");

        // As we go through all protein ids, keep the sorted list of proteins and maxima in the instance of the Metrics class to pass them to the GUI afterwards
        // proteins are sorted according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
        TreeMap<Double, TreeMap<Integer, TreeMap<Integer, TreeSet<Long>>>> orderMap
                = new TreeMap<>();
        double maxMW = 0;
        int maxProteinKeyLength = 6;
        int maxPeptides = 0;
        int maxSpectra = 0;
        int nProteinsSorted = 0;
        
        int max = 2 * identification.getProteinIdentification().size();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler); // @TODO: progress goes waaaay beyond 100%...
        ProteinMatch proteinMatch;

        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            PSParameter proteinMatchParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

            if (!proteinMatch.isDecoy()) {

                double score = proteinMatchParameter.getScore();

                TreeMap<Integer, TreeMap<Integer, TreeSet<Long>>> scoreMap = orderMap.get(score);

                if (scoreMap == null) {

                    scoreMap = new TreeMap<>();
                    orderMap.put(score, scoreMap);

                }

                int nPeptides = proteinMatch.getPeptideMatchesKeys().length;

                TreeMap<Integer, TreeSet<Long>> nPeptidesMap = scoreMap.get(nPeptides);

                if (nPeptidesMap == null) {

                    nPeptidesMap = new TreeMap<>();
                    scoreMap.put(nPeptides, nPeptidesMap);
                    
                    if (nPeptides > maxPeptides) {
                        
                        maxPeptides = nPeptides;
                        
                    }
                }

                int nSpectra = Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                        .mapToObj(peptideKey -> identification.getPeptideMatch(peptideKey))
                        .mapToInt(peptideMatch -> peptideMatch.getSpectrumCount())
                        .sum();

                TreeSet<Long> nSpectraList = nPeptidesMap.get(nSpectra);

                if (nSpectraList == null) {

                    nSpectraList = new TreeSet<>();
                    nPeptidesMap.put(nSpectra, nSpectraList);
                    
                    if (nSpectra > maxSpectra) {
                        
                        maxSpectra = nSpectra;
                        
                    }
                }

                nSpectraList.add(proteinMatch.getKey());
                nProteinsSorted++;

                // Get leading protein accession
                String mainAccession = proteinMatch.getLeadingAccession();

                // Save maximal mw
                String proteinSequence = sequenceProvider.getSequence(mainAccession);
                double mw = ProteinUtils.computeMolecularWeight(proteinSequence);

                if (mw > maxMW) {

                    maxMW = mw;

                }

                // save the length of the longest protein accession number
                if (mainAccession.length() > maxProteinKeyLength) {

                    maxProteinKeyLength = proteinMatch.getLeadingAccession().length();

                }
            }

            String[] accessions = proteinMatch.getAccessions();
            String mainAccession = proteinMatch.getLeadingAccession();

            if (accessions.length > 1) {

                boolean similarityFound = false;
                boolean allSimilar = false;

                for (String accession : accessions) {

                    if (compareMainProtein(proteinMatch, mainAccession, proteinMatch, accession, sequenceProvider, proteinDetailsProvider, identificationParameters, identification) > 0) {

                        mainAccession = accession;

                    }
                }

                similarityLoop:
                for (int i = 0; i < accessions.length - 1; i++) {

                    for (int j = i + 1; j < accessions.length; j++) {

                        if (getSimilarity(accessions[i], accessions[j], proteinDetailsProvider)) {

                            similarityFound = true;

                            if (compareMainProtein(proteinMatch, mainAccession, proteinMatch, accessions[j], sequenceProvider, proteinDetailsProvider, identificationParameters, identification) > 0) {

                                mainAccession = accessions[i];

                            }

                            break similarityLoop;

                        }
                    }
                }

                if (similarityFound) {

                    allSimilar = true;

                    for (String accession : accessions) {

                        if (!mainAccession.equals(accession)) {

                            if (!getSimilarity(mainAccession, accession, proteinDetailsProvider)) {

                                allSimilar = false;
                                break;

                            }
                        }
                    }
                }

                if (!similarityFound) {

                    proteinMatchParameter.setProteinInferenceClass(PSParameter.UNRELATED);

                    for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        PSParameter peptideParameter = (PSParameter) (identification.getPeptideMatch(peptideKey)).getUrParam(PSParameter.dummy);
                        peptideParameter.setProteinInferenceClass(PSParameter.UNRELATED);

                    }

                } else if (!allSimilar) {

                    proteinMatchParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);

                    for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        PSParameter peptideParameter = (PSParameter) (identification.getPeptideMatch(peptideKey)).getUrParam(PSParameter.dummy);
                        peptideParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);

                    }

                } else {

                    proteinMatchParameter.setProteinInferenceClass(PSParameter.RELATED);
                    HashSet<String> proteinGroupAccessions = Sets.newHashSet(proteinMatch.getAccessions());

                    for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                        boolean unrelated = false;

                        for (String proteinAccession : peptideMatch.getPeptide().getProteinMapping().navigableKeySet()) {

                            if (!proteinGroupAccessions.contains(proteinAccession) && !getSimilarity(mainAccession, proteinAccession, proteinDetailsProvider)) {

                                unrelated = true;
                                break;

                            }
                        }

                        PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                        if (unrelated) {

                            peptideParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);

                        } else {

                            peptideParameter.setProteinInferenceClass(PSParameter.RELATED);

                        }
                    }
                }

            } else {

                HashSet<String> proteinGroupAccessions = Sets.newHashSet(proteinMatch.getAccessions());

                for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    boolean unrelated = false;
                    boolean otherProtein = false;

                    for (String proteinAccession : peptideMatch.getPeptide().getProteinMapping().navigableKeySet()) {

                        if (!proteinGroupAccessions.contains(proteinAccession)) {

                            otherProtein = true;

                            if (!getSimilarity(mainAccession, proteinAccession, proteinDetailsProvider)) {

                                unrelated = true;
                                break;

                            }
                        }
                    }

                    PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                    if (otherProtein) {

                        peptideParameter.setProteinInferenceClass(PSParameter.RELATED);

                    }

                    if (unrelated) {

                        peptideParameter.setProteinInferenceClass(PSParameter.UNRELATED);

                    }
                }
            }

            if (proteinMatch.getAccessions().length > 1) {
                
                if (!proteinMatch.getLeadingAccession().equals(mainAccession)) {
                
                    proteinMatch.setLeadingAccession(mainAccession);
                
                }
            }

            waitingHandler.increaseSecondaryProgressCounter();
            
            if (waitingHandler.isRunCanceled()) {
            
                return;
            
            }
        }

        ArrayList<Long> proteinKeys = new ArrayList<>(nProteinsSorted);

        for (TreeMap<Integer, TreeMap<Integer, TreeSet<Long>>> scoreValues : orderMap.values()) {
            
            for (TreeMap<Integer, TreeSet<Long>> nPeptideValues : scoreValues.descendingMap().values()) {
                
                for (TreeSet<Long> nSpectraKeys : nPeptideValues.descendingMap().values()) {
                    
                    proteinKeys.addAll(nSpectraKeys);

                    waitingHandler.increaseSecondaryProgressCounter(proteinKeys.size());
                    
                    if (waitingHandler.isRunCanceled()) {
                        
                        return;
                    
                    }
                }
            }
        }

        if (metrics != null) {
            
            metrics.setProteinKeys(proteinKeys.stream().mapToLong(a -> a).toArray());
            metrics.setMaxNPeptides(maxPeptides);
            metrics.setMaxNSpectra(maxSpectra);
            metrics.setMaxMW(maxMW);
            metrics.setMaxProteinKeyLength(maxProteinKeyLength);
            
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        
    }

    /**
     * Parses a protein description retaining only words longer than 3
     * characters.
     *
     * @param proteinDescription the description of the protein
     * 
     * @return description words longer than 3 characters
     */
    private HashSet<String> parseDescription(String proteinDescription) {
        
        if (proteinDescription == null) {
            
            return new HashSet<>(0);
        
        }
        
        return Arrays.stream(proteinDescription.split(" "))
                .filter(component -> component.length() > 3)
                .collect(Collectors.toCollection(HashSet::new));
        
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
     * @param sequenceProvider the sequence provider
     * @param identificationParameters the identification parameters
     *
     * @return the product of the comparison: 1: better enzymaticity, 2: better
     * evidence, 3: better characterization, 0: equal or not better
     */
    private int compareMainProtein(ProteinMatch oldProteinMatch, String oldAccession, ProteinMatch newProteinMatch, String newAccession, 
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters, Identification identification) {

        ProteinInferenceParameters proteinInferencePreferences = identificationParameters.getProteinInferenceParameters();
        
        if (proteinInferencePreferences.getSimplifyGroupsEnzymaticity()) {
        
            DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();
            
            if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {
            
                boolean newEnzymatic = Arrays.stream(newProteinMatch.getPeptideMatchesKeys())
                        .mapToObj(key -> identification.getPeptideMatch(key))
                        .anyMatch(peptideMatch -> PeptideUtils.isEnzymatic(peptideMatch.getPeptide(), newAccession, sequenceProvider.getSequence(newAccession), digestionPreferences.getEnzymes()));
                boolean oldEnzymatic = Arrays.stream(oldProteinMatch.getPeptideMatchesKeys())
                        .mapToObj(key -> identification.getPeptideMatch(key))
                        .anyMatch(peptideMatch -> PeptideUtils.isEnzymatic(peptideMatch.getPeptide(), oldAccession, sequenceProvider.getSequence(oldAccession), digestionPreferences.getEnzymes()));
                
                if (newEnzymatic && !oldEnzymatic) {
                
                    return 1;
                
                } else if (!newEnzymatic && oldEnzymatic) {
                    
                    return 0;
                
                }
            }
        }

        if (proteinInferencePreferences.getSimplifyGroupsEvidence()) {
            
            Integer evidenceLevelOld = proteinDetailsProvider.getProteinEvidence(oldAccession);
            Integer evidenceLevelNew = proteinDetailsProvider.getProteinEvidence(newAccession);

            // compare protein evidence levels
            if (evidenceLevelOld != null && evidenceLevelNew != null) {
                
                try {
                
                    if (evidenceLevelNew < evidenceLevelOld) {

                        return 2;

                    } else if (evidenceLevelOld < evidenceLevelNew) {

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
            String oldDescription = proteinDetailsProvider.getSimpleDescription(oldAccession);
            String newDescription = proteinDetailsProvider.getSimpleDescription(newAccession);

            // if the description is not set, return the accessions instead - fix for home made fasta headers
            if (oldDescription == null || oldDescription.trim().isEmpty()) {

                oldDescription = oldAccession;

            }

            if (newDescription == null || newDescription.trim().isEmpty()) {

                newDescription = newAccession;

            }

            boolean oldUncharacterized = false, newUncharacterized = false;

            for (String keyWord : unchatacterizedKeyWords) {

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
     * @param proteinDetailsProvider the protein details provider
     *
     * @return a boolean indicating whether the proteins are similar
     */
    private boolean getSimilarity(String primaryProteinAccession, String secondaryProteinAccession, ProteinDetailsProvider proteinDetailsProvider) {

        String geneNamePrimaryProtein = proteinDetailsProvider.getGeneName(primaryProteinAccession);
        String geneNameSecondaryProtein = proteinDetailsProvider.getGeneName(secondaryProteinAccession);
        
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
            HashSet<String> primaryDescription = parseDescription(proteinDetailsProvider.getSimpleDescription(primaryProteinAccession));
            HashSet<String> secondaryDescription = parseDescription(proteinDetailsProvider.getSimpleDescription(secondaryProteinAccession));

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
