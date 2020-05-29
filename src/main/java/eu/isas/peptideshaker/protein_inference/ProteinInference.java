package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.google.common.collect.Sets;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * This class groups the methods for protein inference.
 *
 * @author Marc Vaudel
 */
public class ProteinInference {

    public enum GroupSimplificationOption {

        /**
         * Group supported by predicted or uncertain proteins.
         */
        lowerEvidence(0, "protein groups supported by predicted or uncertain proteins"),
        /**
         * Group supported by low confidence peptides.
         */
        lowerConfidence(0, "protein groups supported by low confidence peptides"),
        /**
         * Group supported by non-enzymatic shared peptide.
         */
        nonEnzymatic(1, "protein groups supported by non-enzymatic shared peptides"),
        /**
         * Group supported by peptide shared with protein with variant.
         */
        variant(2, "protein groups supported by peptides shared by variant only"),
        /**
         * Group explained by simpler groups.
         */
        simplerGroups(3, "protein groups explained by simpler groups");
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
    public static final String[] KEYWORDS_UNCHARACTERIZED = {"uncharacterized"};

    /**
     * Selects the leading protein of protein groups and infers PI status of
     * peptide and proteins.
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
    public void inferPiStatus(
            Identification identification,
            Metrics metrics,
            TargetDecoyMap proteinMap,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            WaitingHandler waitingHandler
    ) {

        waitingHandler.setWaitingText("Inferring PI Status and Sorting Proteins. Please Wait...");

        waitingHandler.setMaxSecondaryProgressCounter(
                identification.getProteinIdentification().size()
        );

        identification.getProteinIdentification()
                .parallelStream()
                .map(
                        key -> identification.getProteinMatch(key)
                )
                .forEach(
                        proteinMatch -> inferPiStatus(
                                proteinMatch,
                                identification,
                                metrics,
                                proteinMap,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                waitingHandler
                        )
                );

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Selects the leading protein of protein groups and infers PI status of
     * peptide and proteins.
     *
     * @param proteinMatch the protein match
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
    public void inferPiStatus(
            ProteinMatch proteinMatch,
            Identification identification,
            Metrics metrics,
            TargetDecoyMap proteinMap,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            WaitingHandler waitingHandler
    ) {

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        PSParameter proteinMatchParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
        String[] accessions = proteinMatch.getAccessions();
        String mainAccession = proteinMatch.getLeadingAccession();

        if (accessions.length > 1) {

            boolean similarityFound = false;
            boolean allSimilar = false;

            for (String accession : accessions) {

                if (compareMainProtein(proteinMatch, mainAccession, proteinMatch,
                        accession, sequenceProvider, proteinDetailsProvider,
                        identificationParameters, identification) > 0) {

                    mainAccession = accession;

                }
            }

            similarityLoop:
            for (int i = 0; i < accessions.length - 1; i++) {

                for (int j = i + 1; j < accessions.length; j++) {

                    if (getSimilarity(accessions[i], accessions[j], proteinDetailsProvider)) {

                        similarityFound = true;

                        if (compareMainProtein(proteinMatch, mainAccession, proteinMatch,
                                accessions[j], sequenceProvider, proteinDetailsProvider,
                                identificationParameters, identification) > 0) {

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

                    PeptideMatch peptideMatch = (PeptideMatch) identification.getPeptideMatch(peptideKey);
                    PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    peptideParameter.setProteinInferenceClass(PSParameter.UNRELATED);

                    identification.updateObject(peptideKey, peptideMatch);

                }

            } else if (!allSimilar) {

                proteinMatchParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);

                for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                    PeptideMatch peptideMatch = (PeptideMatch) identification.getPeptideMatch(peptideKey);
                    PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    peptideParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);
                    identification.updateObject(peptideKey, peptideMatch);

                }

            } else {

                proteinMatchParameter.setProteinInferenceClass(PSParameter.RELATED);
                HashSet<String> proteinGroupAccessions = Sets.newHashSet(proteinMatch.getAccessions());

                for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                    boolean unrelated = false;

                    for (String proteinAccession : peptideMatch.getPeptide().getProteinMapping().navigableKeySet()) {

                        if (!proteinGroupAccessions.contains(proteinAccession) 
                                && !getSimilarity(mainAccession, proteinAccession, proteinDetailsProvider)) {

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

                    identification.updateObject(peptideKey, peptideMatch);
                }
            }

        } else {

            HashSet<String> proteinGroupAccessions = Sets.newHashSet(proteinMatch.getAccessions());

            for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                boolean related = false;
                boolean unrelated = false;

                for (String proteinAccession : peptideMatch.getPeptide().getProteinMapping().navigableKeySet()) {

                    if (!proteinGroupAccessions.contains(proteinAccession)) {

                        boolean similar = getSimilarity(mainAccession, proteinAccession, proteinDetailsProvider);

                        if (similar) {
                            related = true;
                        } else {
                            unrelated = true;
                        }

                    }
                }

                PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                if (related && unrelated) {
                    peptideParameter.setProteinInferenceClass(PSParameter.RELATED_AND_UNRELATED);
                } else if (related) {
                    peptideParameter.setProteinInferenceClass(PSParameter.RELATED);
                } else if (unrelated) {
                    peptideParameter.setProteinInferenceClass(PSParameter.UNRELATED);
                }

                identification.updateObject(peptideKey, peptideMatch);
            }
        }

        if (proteinMatch.getAccessions().length > 1) {

            if (!proteinMatch.getLeadingAccession().equals(mainAccession)) {

                proteinMatch.setLeadingAccession(mainAccession);
                identification.updateObject(proteinMatch.getKey(), proteinMatch);

            }
        }

        waitingHandler.increaseSecondaryProgressCounter();
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
                .filter(
                        component -> component.length() > 3
                )
                .collect(
                        Collectors.toCollection(HashSet::new)
                );

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
    private int compareMainProtein(
            ProteinMatch oldProteinMatch,
            String oldAccession,
            ProteinMatch newProteinMatch,
            String newAccession,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters,
            Identification identification
    ) {

        DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();

        if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

            boolean newEnzymatic = Arrays.stream(newProteinMatch.getPeptideMatchesKeys())
                    .mapToObj(
                            key -> identification.getPeptideMatch(key)
                    )
                    .anyMatch(
                            peptideMatch -> PeptideUtils.isEnzymatic(
                                    peptideMatch.getPeptide(),
                                    newAccession,
                                    sequenceProvider.getSequence(newAccession),
                                    digestionPreferences.getEnzymes()
                            )
                    );

            boolean oldEnzymatic = Arrays.stream(oldProteinMatch.getPeptideMatchesKeys())
                    .mapToObj(
                            key -> identification.getPeptideMatch(key)
                    )
                    .anyMatch(
                            peptideMatch -> PeptideUtils.isEnzymatic(
                                    peptideMatch.getPeptide(),
                                    oldAccession,
                                    sequenceProvider.getSequence(oldAccession),
                                    digestionPreferences.getEnzymes()
                            )
                    );

            if (newEnzymatic && !oldEnzymatic) {

                return 1;

            } else if (!newEnzymatic && oldEnzymatic) {

                return 0;

            }
        }

        Integer evidenceLevelOld = proteinDetailsProvider.getProteinEvidence(oldAccession);
        Integer evidenceLevelNew = proteinDetailsProvider.getProteinEvidence(newAccession);

        // compare protein evidence levels
        if (evidenceLevelOld != null && evidenceLevelNew != null) {

            if (evidenceLevelNew < evidenceLevelOld) {

                return 2;

            } else if (evidenceLevelOld < evidenceLevelNew) {

                return 0;

            }
        }

        // Compare descriptions for keywords of uncharacterized proteins
        String oldDescription = proteinDetailsProvider.getSimpleDescription(oldAccession);

        if (oldDescription == null || oldDescription.trim().isEmpty()) {

            oldDescription = proteinDetailsProvider.getDescription(oldAccession);

        }

        String newDescription = proteinDetailsProvider.getSimpleDescription(newAccession);

        if (newDescription == null || newDescription.trim().isEmpty()) {

            newDescription = proteinDetailsProvider.getDescription(newAccession);

        }

        boolean oldUncharacterized = false, newUncharacterized = false;

        for (String keyWord : KEYWORDS_UNCHARACTERIZED) {

            if (newDescription != null && newDescription.contains(keyWord)) {

                newUncharacterized = true;

            }

            if (oldDescription != null && oldDescription.contains(keyWord)) {

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
     * @param proteinDetailsProvider the protein details provider
     *
     * @return a boolean indicating whether the proteins are similar
     */
    private boolean getSimilarity(
            String primaryProteinAccession,
            String secondaryProteinAccession,
            ProteinDetailsProvider proteinDetailsProvider
    ) {

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

    /**
     * Distribute the shared peptides among the protein groups.
     *
     * @param identification the identification class containing all
     * identification matches
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void distributeSharedPeptides(
            Identification identification,
            WaitingHandler waitingHandler
    ) {

        waitingHandler.setWaitingText("Inferring PI Status and Sorting Proteins. Please Wait...");

        waitingHandler.setMaxSecondaryProgressCounter(
                identification.getProteinIdentification().size()
        );

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            Peptide peptide = peptideMatch.getPeptide();

            if (peptide.getProteinMapping().size() > 1) {

                long peptideMatchKey = peptideMatch.getKey();

                for (String protein : peptide.getProteinMapping().keySet()) {

                    HashSet<Long> proteinKeys = identification.getProteinMap().get(protein);

                    if (proteinKeys != null) {

                        for (long proteinKey : proteinKeys) {

                            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

                            if (Arrays.stream(proteinMatch.getAccessions())
                                    .allMatch(
                                            accession -> peptide.getProteinMapping().containsKey(accession)
                                    )
                                    && Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                                            .allMatch(
                                                    key -> key != peptideMatchKey
                                            )) {

                                proteinMatch.addPeptideMatchKey(peptideMatchKey);
                                identification.updateObject(proteinMatch.getKey(), proteinMatch);
                            }
                        }
                    }
                }
            }

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }
}
