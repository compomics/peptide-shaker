package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.ProteinInferenceParameters;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import com.google.common.collect.Sets;
import static eu.isas.peptideshaker.protein_inference.ProteinInference.KEYWORDS_UNCHARACTERIZED;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * This class handles the simplification of redundant groups based on the
 * protein inference settings.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class GroupSimplification {

    /**
     * Number of groups indexed by option.
     */
    private final int[] nDeleted = new int[ProteinInference.GroupSimplificationOption.values().length];

    /**
     * Remove groups that can be explained by a simpler group.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param identificationParameters the identification parameters
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void removeRedundantGroups(
            Identification identification,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            WaitingHandler waitingHandler
    ) {

        int max = identification.getProteinIdentification().size();

        if (waitingHandler != null) {

            waitingHandler.setWaitingText("Symplifying Protein Groups. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(max);

        }

        HashSet<Long> proteinGroupKeys = identification.getProteinIdentification();
        HashSet<Long> toDelete = new HashSet<>();
        HashMap<Long, long[]> processedKeys = new HashMap<>();
        SimpleSemaphore mutex = new SimpleSemaphore(1);

        proteinGroupKeys.stream()
                .filter(
                        key -> !processedKeys.containsKey(key)
                )
                .map(
                        key -> identification.getProteinMatch(key)
                )
                .filter(
                        proteinMatch -> proteinMatch.getNProteins() > 1
                )
                .forEach(
                        proteinSharedGroup -> removeRedundantGroups(
                                identification,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                proteinSharedGroup,
                                processedKeys,
                                toDelete,
                                mutex,
                                waitingHandler
                        )
                );

        int totalSimplified = Arrays.stream(nDeleted).sum();

        if (totalSimplified > 0) {

            if (waitingHandler != null) {

                waitingHandler.appendReport(toDelete.size() + " unlikely protein mappings found:", true, true);

                String padding = "    ";

                for (int i = 0; i < ProteinInference.GroupSimplificationOption.values().length; i++) {

                    int iSimplified = nDeleted[i];

                    if (iSimplified > 0) {

                        ProteinInference.GroupSimplificationOption option = ProteinInference.GroupSimplificationOption.values()[i];

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
     * Removes the redundant groups for the given protein shared group.
     *
     * @param identification The identification object containing the
     * identification matches.
     * @param identificationParameters The identification parameters.
     * @param sequenceProvider The protein sequence provider.
     * @param proteinDetailsProvider The protein details provider.
     * @param proteinSharedGroup The protein shared group investigated.
     * @param processedKeys Map of the already processed keys.
     * @param toDelete Set of the keys to delete.
     * @param mutex Mutex to synchronize the different threads.
     * @param waitingHandler Waiting handler to show progress and interrupt
     * processes.
     */
    public void removeRedundantGroups(
            Identification identification,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            ProteinMatch proteinSharedGroup,
            HashMap<Long, long[]> processedKeys,
            HashSet<Long> toDelete,
            SimpleSemaphore mutex,
            WaitingHandler waitingHandler
    ) {

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        long sharedKey = proteinSharedGroup.getKey();

        mutex.acquire();

        if (!processedKeys.containsKey(sharedKey)) {

            ProteinMatch[] reducedGroups = getSubgroup(
                    identification,
                    proteinSharedGroup,
                    processedKeys,
                    toDelete,
                    sequenceProvider,
                    proteinDetailsProvider,
                    identificationParameters
            );

            if (reducedGroups != null) {

                long[] reducedGroupKeys = new long[reducedGroups.length];
                processedKeys.put(sharedKey, reducedGroupKeys);

                for (int i = 0; i < reducedGroups.length; i++) {

                    ProteinMatch reducedGroup = reducedGroups[i];
                    long reducedGroupKey = reducedGroup.getKey();
                    reducedGroupKeys[i] = reducedGroupKey;

                }

                toDelete.add(sharedKey);

            } else {

                long[] reducedGroupKeys = new long[1];
                reducedGroupKeys[0] = sharedKey;
                processedKeys.put(sharedKey, reducedGroupKeys);

            }
        }

        mutex.release();

        waitingHandler.increaseSecondaryProgressCounter();
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
    private ProteinMatch[] getSubgroup(
            Identification identification,
            ProteinMatch sharedProteinMatch,
            HashMap<Long, long[]> processedKeys,
            HashSet<Long> toDelete,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters
    ) {

        long sharedKey = sharedProteinMatch.getKey();

        String[] accessions = sharedProteinMatch.getAccessions();
        HashSet<String> accessionsAsSet = Sets.newHashSet(accessions);

        HashMap<Long, ProteinMatch> subgroups = new HashMap<>(0);

        for (String accession : accessions) {

            HashSet<Long> otherGroups = identification.getProteinMap().get(accession);

            for (long uniqueKey : otherGroups) {

                if (uniqueKey != sharedKey && !toDelete.contains(uniqueKey) && !subgroups.containsKey(uniqueKey)) {

                    ProteinMatch simplerProteinMatch = identification.getProteinMatch(uniqueKey);

                    String[] uniqueAccessions = simplerProteinMatch.getAccessions();

                    if (accessions.length > uniqueAccessions.length) {

                        if (Arrays.stream(uniqueAccessions)
                                .allMatch(uniqueAccession -> accessionsAsSet.contains(uniqueAccession))) {

                            if (uniqueAccessions.length > 1) {

                                long[] reducedGroupKeys = processedKeys.get(uniqueKey);

                                if (reducedGroupKeys == null) {

                                    ProteinMatch[] reducedGroups = getSubgroup(identification,
                                            simplerProteinMatch,
                                            processedKeys,
                                            toDelete,
                                            sequenceProvider,
                                            proteinDetailsProvider,
                                            identificationParameters
                                    );

                                    if (reducedGroups != null) {

                                        reducedGroupKeys = new long[reducedGroups.length];
                                        processedKeys.put(uniqueKey, reducedGroupKeys);

                                        for (int i = 0; i < reducedGroups.length; i++) {

                                            ProteinMatch reducedGroup = reducedGroups[i];
                                            long reducedGroupKey = reducedGroup.getKey();
                                            reducedGroupKeys[i] = reducedGroupKey;

                                            subgroups.put(reducedGroupKey, reducedGroup);

                                        }

                                        toDelete.add(sharedKey);

                                    } else {

                                        reducedGroupKeys = new long[1];
                                        reducedGroupKeys[0] = uniqueKey;
                                        processedKeys.put(uniqueKey, reducedGroupKeys);
                                        subgroups.put(uniqueKey, simplerProteinMatch);

                                    }

                                } else {

                                    Arrays.stream(reducedGroupKeys)
                                            .forEach(reducedGroupKey -> subgroups.put(reducedGroupKey, identification.getProteinMatch(reducedGroupKey))
                                            );

                                }
                            } else {

                                subgroups.put(uniqueKey, simplerProteinMatch);

                            }
                        }
                    }
                }
            }
        }

        if (subgroups.isEmpty()) {

            return null;

        }

        // Gather the proteins explained or not by the subgroups
        HashSet<String> coveredAccessionsSet = subgroups.values().stream()
                .flatMap(proteinMatch -> Arrays.stream(proteinMatch.getAccessions()))
                .collect(Collectors.toCollection(HashSet::new));
        String[] uniqueAccessions = Arrays.stream(accessions)
                .filter(accession -> !coveredAccessionsSet.contains(accession))
                .toArray(String[]::new);
        String[] coveredAccessions = coveredAccessionsSet.toArray(new String[coveredAccessionsSet.size()]);

        // Check whether simpler groups can explain the protein group
        int bestSimplification = ProteinInference.GroupSimplificationOption.values().length - 1;

        for (String uniqueAccession : uniqueAccessions) {

            ProteinInference.GroupSimplificationOption groupSimplificationOption = getSimplificationOption(
                    sharedProteinMatch,
                    uniqueAccession,
                    coveredAccessions,
                    sequenceProvider,
                    proteinDetailsProvider,
                    identificationParameters,
                    identification
            );

            if (groupSimplificationOption == null) {

                return null;

            }

            if (groupSimplificationOption.index < bestSimplification) {

                bestSimplification = groupSimplificationOption.index;

            }
        }

        nDeleted[bestSimplification]++;
        toDelete.add(sharedKey);

        return subgroups.values().toArray(
                new ProteinMatch[subgroups.size()]
        );
    }

    /**
     * Returns whether the protein match with unique shared protein accessions
     * can be simplified.
     *
     * @param sharedMatch the protein match
     * @param accession the unique shared protein accession
     * @param coveredAccessions the accessions covered by the subgroups
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param identificationParameters the identification parameters
     * @param identification the identification
     *
     * @return the protein match simplification option that can be used
     */
    private ProteinInference.GroupSimplificationOption getSimplificationOption(
            ProteinMatch sharedMatch,
            String accession,
            String[] coveredAccessions,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters,
            Identification identification
    ) {

        ProteinInferenceParameters proteinInferenceParameters = identificationParameters.getProteinInferenceParameters();

        // See wheter the unique accession is from a lower confidence protein
        if (proteinInferenceParameters.getSimplifyGroupsEvidence()) {

            if (isLowerEvidence(accession, coveredAccessions, proteinDetailsProvider)) {

                return ProteinInference.GroupSimplificationOption.lowerEvidence;

            }
        }

        // See whether unique accession is due to absent peptides
        if (proteinInferenceParameters.getSimplifyGroupsVariants()) {

            double threshold = proteinInferenceParameters.getConfidenceThreshold();

            if (Arrays.stream(sharedMatch.getPeptideMatchesKeys())
                    .mapToDouble(key -> ((PSParameter) identification.getPeptideMatch(key).getUrParam(PSParameter.dummy)).getConfidence())
                    .allMatch(confidence -> confidence <= threshold)) {

                return ProteinInference.GroupSimplificationOption.lowerConfidence;

            }
        }

        // See whether the peptides in the group are non-enzymatic
        if (proteinInferenceParameters.getSimplifyGroupsEnzymaticity()) {

            DigestionParameters digestionParameters = identificationParameters.getSearchParameters().getDigestionParameters();

            if (digestionParameters.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme
                    && Arrays.stream(sharedMatch.getPeptideMatchesKeys())
                            .mapToObj(
                                    key -> identification.getPeptideMatch(key)
                            )
                            .allMatch(
                                    peptideMatch -> !PeptideUtils.isEnzymatic(
                                            peptideMatch.getPeptide(),
                                            sequenceProvider, digestionParameters.getEnzymes()
                                    )
                            )) {

                return ProteinInference.GroupSimplificationOption.nonEnzymatic;

            }
        }

        // See whether unique accession is due to variant mapping
        if (proteinInferenceParameters.getSimplifyGroupsVariants()) {

            if (identificationParameters.getPeptideVariantsParameters().getnVariants() > 0
                    && Arrays.stream(sharedMatch.getPeptideMatchesKeys())
                            .mapToObj(key -> identification.getPeptideMatch(key))
                            .allMatch(peptideMatch -> PeptideUtils.isVariant(peptideMatch.getPeptide(), accession))) {

                return ProteinInference.GroupSimplificationOption.variant;

            }
        }

        return null;

    }

    /**
     * Returns a boolean indicating whether the protein is considered as
     * uncharacterized compared to the others.
     *
     * @param accession the accession of the protein
     * @param otherAccessions the accessions of the other protein
     * @param proteinDetailsProvider the protein details provider
     *
     * @return a boolean indicating whether the protein is considered as
     * uncharacterized
     */
    public static boolean isLowerEvidence(
            String accession,
            String[] otherAccessions,
            ProteinDetailsProvider proteinDetailsProvider
    ) {

        // Get the protein evidence according to UnitProt
        Integer proteinEvidence = proteinDetailsProvider.getProteinEvidence(accession);

        if (proteinEvidence != null) {

            // Return false if the evidence for this protein is transcript or protein
            if (proteinEvidence <= 2) {

                return false;

            }

            // See the evidence level of the other proteins
            Integer bestEvidenceOthers = null;

            for (String otherAccession : otherAccessions) {

                Integer evidence = proteinDetailsProvider.getProteinEvidence(otherAccession);

                if (evidence != null) {

                    if (bestEvidenceOthers == null || evidence < bestEvidenceOthers) {

                        bestEvidenceOthers = evidence;

                    }
                }
            }

            // If evidence is available, return true if the evidence is worse according to uniprot
            if (bestEvidenceOthers != null) {

                return proteinEvidence > bestEvidenceOthers;

            }
        }

        // Evidence level not available, see whether the protein description contain key words
        String description = proteinDetailsProvider.getSimpleDescription(accession);

        if (description == null) {

            description = proteinDetailsProvider.getDescription(accession);

        }

        final String descriptionFinal = description.toLowerCase();

        boolean proteinUncharacterized = Arrays.stream(KEYWORDS_UNCHARACTERIZED)
                .anyMatch(keyWord -> descriptionFinal.equals(keyWord));

        boolean otherUncharacterized = true;

        for (String otherAccession : otherAccessions) {

            description = proteinDetailsProvider.getSimpleDescription(otherAccession);

            if (description == null) {

                description = proteinDetailsProvider.getDescription(otherAccession);

            }

            final String otherDescriptionFinal = description.toLowerCase();

            boolean tempUncharacterized = Arrays.stream(KEYWORDS_UNCHARACTERIZED)
                    .anyMatch(keyWord -> otherDescriptionFinal.equals(keyWord));

            if (!tempUncharacterized) {

                otherUncharacterized = false;
                break;

            }
        }

        return !otherUncharacterized && proteinUncharacterized;

    }
}
