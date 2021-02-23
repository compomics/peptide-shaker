package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.peptide_shaker.PSModificationScores;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.identification.peptide_shaker.ModificationScoring;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This class outputs the peptide related export features.
 *
 * @author Marc Vaudel
 */
public class PsPeptideSection {

    /**
     * The peptide features to export.
     */
    private final EnumSet<PsPeptideFeature> peptideFeatures = EnumSet.noneOf(PsPeptideFeature.class);
    /**
     * The PSM subsection if needed.
     */
    private PsPsmSection psmSection = null;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private final boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private final boolean header;
    /**
     * The writer used to send the output to file.
     */
    private final ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsPeptideSection(
            ArrayList<ExportFeature> exportFeatures,
            boolean indexes,
            boolean header,
            ExportWriter writer
    ) {

        ArrayList<ExportFeature> psmFeatures = new ArrayList<>(0);

        for (ExportFeature exportFeature : exportFeatures) {

            if (exportFeature instanceof PsPeptideFeature) {

                peptideFeatures.add((PsPeptideFeature) exportFeature);

            } else if (exportFeature instanceof PsPsmFeature
                    || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature
                    || exportFeature instanceof PsFragmentFeature) {

                psmFeatures.add(exportFeature);

            } else {

                throw new IllegalArgumentException(
                        "Export feature of type "
                        + exportFeature.getClass()
                        + " not recognized."
                );

            }
        }

        if (!psmFeatures.isEmpty()) {

            psmSection = new PsPsmSection(
                    psmFeatures,
                    indexes,
                    header,
                    writer
            );
        }

        this.indexes = indexes;
        this.header = header;
        this.writer = writer;

    }

    /**
     * Writes the desired section.
     *
     * @param identification The identification of the project.
     * @param identificationFeaturesGenerator The identification features
     * generator of the project.
     * @param sequenceProvider The sequence provider.
     * @param proteinDetailsProvider The protein details provider.
     * @param spectrumProvider The spectrum provider.
     * @param identificationParameters The identification parameters.
     * @param keys The keys of the PSM matches to output.
     * @param linePrefix The line prefix.
     * @param nSurroundingAA The number of surrounding amino acids to export.
     * @param validatedOnly Whether only validated matches should be exported.
     * @param decoys Whether decoy matches should be exported as well.
     * @param waitingHandler The waiting handler.
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading or writing a file
     */
    public void writeSection(
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            long[] keys,
            int nSurroundingAA,
            String linePrefix,
            boolean validatedOnly,
            boolean decoys,
            WaitingHandler waitingHandler
    ) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        int lineNumber = 1;

        if (waitingHandler != null) {

            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys == null ? identification.getPeptideIdentification().size() : keys.length);

        }

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(keys, waitingHandler);

        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                if (decoys || !PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider)) {

                    boolean first = true;

                    if (indexes) {

                        if (linePrefix != null) {

                            writer.write(linePrefix);

                        }

                        writer.write(Integer.toString(lineNumber));
                        first = false;

                    }

                    for (ExportFeature exportFeature : peptideFeatures) {

                        if (!first) {

                            writer.addSeparator();

                        } else {

                            first = false;

                        }

                        PsPeptideFeature peptideFeature = (PsPeptideFeature) exportFeature;
                        writer.write(
                                getfeature(
                                        identification,
                                        identificationFeaturesGenerator,
                                        sequenceProvider,
                                        proteinDetailsProvider,
                                        identificationParameters,
                                        nSurroundingAA,
                                        linePrefix,
                                        peptideMatch,
                                        peptideFeature,
                                        validatedOnly,
                                        decoys,
                                        waitingHandler
                                )
                        );

                    }

                    writer.newLine();

                    if (psmSection != null) {

                        String psmSectionPrefix = "";

                        if (linePrefix != null) {

                            psmSectionPrefix += linePrefix;

                        }

                        psmSectionPrefix += lineNumber + ".";
                        writer.increaseDepth();

                        if (waitingHandler != null) {

                            waitingHandler.setDisplayProgress(false);

                        }

                        psmSection.writeSection(
                                identification,
                                identificationFeaturesGenerator,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider,
                                identificationParameters,
                                peptideMatch.getSpectrumMatchesKeys(),
                                psmSectionPrefix,
                                nSurroundingAA,
                                validatedOnly,
                                decoys,
                                waitingHandler
                        );

                        if (waitingHandler != null) {

                            waitingHandler.setDisplayProgress(true);

                        }

                        writer.decreseDepth();

                    }

                    lineNumber++;

                }
            }
        }
    }

    /**
     * Returns the component of the section corresponding to the given feature.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param sequenceProvider a provider for the protein sequences
     * @param proteinDetailsProvider a provider for protein details
     * @param identificationParameters the identification parameters
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param peptideMatch the peptide match
     * @param peptideFeature the peptide feature to export
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @return the component of the section corresponding to the given feature
     */
    public static String getfeature(
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters,
            int nSurroundingAA,
            String linePrefix,
            PeptideMatch peptideMatch,
            PsPeptideFeature peptideFeature,
            boolean validatedOnly,
            boolean decoys,
            WaitingHandler waitingHandler
    ) {

        switch (peptideFeature) {

            case accessions:

                TreeMap<String, int[]> proteinMapping = peptideMatch.getPeptide().getProteinMapping();

                return proteinMapping.navigableKeySet().stream()
                        .collect(Collectors.joining(","));

            case protein_description:

                proteinMapping = peptideMatch.getPeptide().getProteinMapping();

                return proteinMapping.navigableKeySet().stream()
                        .map(accession -> proteinDetailsProvider.getDescription(accession))
                        .collect(Collectors.joining(","));

            case protein_groups:

                TreeSet<Long> proteinGroups = identification.getProteinMatches(peptideMatch.getKey());

                return proteinGroups.stream()
                        .map(proteinGroupKey -> getProteinGroupText(proteinGroupKey, identification))
                        .collect(Collectors.joining(";"));

            case best_protein_group_validation:

                MatchValidationLevel bestProteinValidationLevel = MatchValidationLevel.none;
                proteinGroups = identification.getProteinMatches(peptideMatch.getKey());

                for (long proteinGroup : proteinGroups) {

                    if (identification.getProteinIdentification().contains(proteinGroup)) {

                        PSParameter psParameter = (PSParameter) (identification.getProteinMatch(proteinGroup)).getUrParam(PSParameter.dummy);

                        if (psParameter.getMatchValidationLevel().getIndex() > bestProteinValidationLevel.getIndex()) {

                            bestProteinValidationLevel = psParameter.getMatchValidationLevel();

                        }
                    }
                }

                return bestProteinValidationLevel.getName();

            case confidence:

                PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return Double.toString(psParameter.getConfidence());

            case decoy:

                return PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider) ? "1" : "0";

            case hidden:

                psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return psParameter.getHidden() ? "1" : "0";

            case localization_confidence:

                return getPeptideModificationLocationConfidence(
                        peptideMatch,
                        identificationParameters.getSearchParameters().getModificationParameters()
                );

            case pi:

                psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return psParameter.getProteinInferenceClassAsString();

            case position:

                proteinMapping = peptideMatch.getPeptide().getProteinMapping();

                return proteinMapping.entrySet().stream()
                        .map(entry -> getPeptideLocalizationText(entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(";"));

            case psms:

                return Integer.toString(peptideMatch.getSpectrumCount());

            case variable_ptms:

                return PeptideUtils.getVariableModificationsAsString(peptideMatch.getPeptide());

            case fixed_ptms:

                ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                return PeptideUtils.getFixedModificationsAsString(
                        peptideMatch.getPeptide(),
                        modificationParameters,
                        sequenceProvider,
                        modificationSequenceMatchingParameters
                );

            case score:

                psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return Double.toString(psParameter.getTransformedScore());

            case raw_score:

                psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return Double.toString(psParameter.getScore());

            case sequence:

                return peptideMatch.getPeptide().getSequence();

            case missed_cleavages:

                int nMissedCleavages = peptideMatch.getPeptide()
                        .getNMissedCleavages(identificationParameters.getSearchParameters().getDigestionParameters());

                return Integer.toString(nMissedCleavages);

            case modified_sequence:

                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                return peptideMatch.getPeptide()
                        .getTaggedModifiedSequence(
                                modificationParameters,
                                sequenceProvider,
                                modificationSequenceMatchingParameters,
                                false,
                                false,
                                true,
                                null
                        );

            case starred:

                psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return psParameter.getStarred() ? "1" : "0";

            case aaBefore:

                TreeMap<String, String[]> aaMap = PeptideUtils.getAaBefore(peptideMatch.getPeptide(), nSurroundingAA, sequenceProvider);

                return aaMap.values().stream()
                        .map(
                                aas -> Arrays.stream(aas)
                                        .collect(
                                                Collectors.joining(",")
                                        )
                        )
                        .collect(
                                Collectors.joining(";")
                        );

            case aaAfter:

                aaMap = PeptideUtils.getAaAfter(
                        peptideMatch.getPeptide(),
                        nSurroundingAA,
                        sequenceProvider
                );

                return aaMap.values().stream()
                        .map(
                                aas -> Arrays.stream(aas)
                                        .collect(
                                                Collectors.joining(",")
                                        )
                        )
                        .collect(
                                Collectors.joining(";")
                        );

            case nValidatedProteinGroups:

                return Integer.toString(
                        identificationFeaturesGenerator.getNValidatedProteinGroups(
                                peptideMatch.getKey(),
                                waitingHandler
                        )
                );

            case unique_group:

                return identification.getProteinMatches(peptideMatch.getKey()).size() == 1 ? "1" : "0";

            case validated:

                psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                return psParameter.getMatchValidationLevel().toString();

            case validated_psms:

                return Integer.toString(identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideMatch.getKey()));

            case probabilistic_score:

                PSModificationScores ptmScores = (PSModificationScores) peptideMatch.getUrParam(PSModificationScores.dummy);

                if (ptmScores != null) {

                    StringBuilder result = new StringBuilder();
                    TreeSet<String> modList = new TreeSet<>(ptmScores.getScoredModifications());

                    for (String mod : modList) {

                        ModificationScoring ptmScoring = ptmScores.getModificationScoring(mod);
                        TreeSet<Integer> sites = new TreeSet<>(ptmScoring.getProbabilisticSites());

                        if (!sites.isEmpty()) {

                            if (result.length() > 0) {

                                result.append(", ");

                            }
                            result.append(mod).append(" (");

                            boolean firstSite = true;

                            for (int site : sites) {

                                if (firstSite) {

                                    firstSite = false;

                                } else {

                                    result.append(", ");

                                }

                                result.append(site).append(": ").append(ptmScoring.getProbabilisticScore(site));

                            }

                            result.append(")");

                        }
                    }

                    return result.toString();

                }

                return "";

            case d_score:
                StringBuilder result = new StringBuilder();
                ptmScores = (PSModificationScores) peptideMatch.getUrParam(PSModificationScores.dummy);

                if (ptmScores != null) {

                    TreeSet<String> modList = new TreeSet<>(ptmScores.getScoredModifications());

                    for (String mod : modList) {

                        ModificationScoring ptmScoring = ptmScores.getModificationScoring(mod);
                        TreeSet<Integer> sites = new TreeSet<>(ptmScoring.getDSites());

                        if (!sites.isEmpty()) {

                            if (result.length() > 0) {

                                result.append(", ");

                            }

                            result.append(mod).append(" (");
                            boolean firstSite = true;

                            for (int site : sites) {

                                if (firstSite) {

                                    firstSite = false;

                                } else {

                                    result.append(", ");

                                }

                                result.append(site).append(": ").append(ptmScoring.getDeltaScore(site));

                            }

                            result.append(")");

                        }
                    }

                    return result.toString();

                }

                return "";

            case confident_modification_sites:

                String sequence = peptideMatch.getPeptide().getSequence();
                return identificationFeaturesGenerator.getConfidentModificationSites(peptideMatch, sequence);

            case confident_modification_sites_number:

                return identificationFeaturesGenerator.getConfidentModificationSitesNumber(peptideMatch);

            case ambiguous_modification_sites:

                sequence = peptideMatch.getPeptide().getSequence();
                return identificationFeaturesGenerator.getAmbiguousModificationSites(peptideMatch, sequence);

            case ambiguous_modification_sites_number:

                return identificationFeaturesGenerator.getAmbiguousModificationSiteNumber(peptideMatch);

            case confident_phosphosites:

                ArrayList<String> modifications = new ArrayList<>(3);

                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {

                    if (ptm.contains("Phospho")) {

                        modifications.add(ptm);

                    }
                }

                return identificationFeaturesGenerator.getConfidentModificationSites(peptideMatch, peptideMatch.getPeptide().getSequence(), modifications);

            case confident_phosphosites_number:

                modifications = new ArrayList<>(3);

                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {

                    if (ptm.contains("Phospho")) {

                        modifications.add(ptm);

                    }
                }

                return identificationFeaturesGenerator.getConfidentModificationSitesNumber(peptideMatch, modifications);

            case ambiguous_phosphosites:

                modifications = new ArrayList<>(3);

                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {

                    if (ptm.contains("Phospho")) {

                        modifications.add(ptm);

                    }
                }

                return identificationFeaturesGenerator.getAmbiguousModificationSites(peptideMatch, peptideMatch.getPeptide().getSequence(), modifications);

            case ambiguous_phosphosites_number:

                modifications = new ArrayList<>(3);

                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {

                    if (ptm.contains("Phospho")) {

                        modifications.add(ptm);

                    }
                }

                return identificationFeaturesGenerator.getAmbiguousModificationSiteNumber(peptideMatch, modifications);

            default:

                return "Not implemented";
        }
    }

    /**
     * Returns a description of the given protein group in the form
     * proteinA,proteinB(confidence).
     *
     * @param proteinGroupKey the key of the protein group
     * @param identification the identification object
     *
     * @return a description of the given protein group
     */
    public static String getProteinGroupText(
            long proteinGroupKey,
            Identification identification
    ) {

        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinGroupKey);
        String[] accessions = proteinMatch.getAccessions();
        String accessionsString = Arrays.stream(accessions)
                .collect(Collectors.joining(","));

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) (proteinMatch).getUrParam(psParameter);
        String validationString = psParameter.getMatchValidationLevel().getName();

        StringBuilder sb = new StringBuilder(accessionsString.length() + validationString.length() + 3);
        sb.append(accessionsString).append(" (").append(validationString).append(")");

        return sb.toString();
    }

    /**
     * Returns the peptide localization on the given protein as text in the form
     * accession (site1,site2).
     *
     * @param proteinAccession the protein accession
     * @param sites the position of the peptide on the protein
     *
     * @return the peptide localization on the given protein as text
     */
    public static String getPeptideLocalizationText(
            String proteinAccession,
            int[] sites
    ) {

        String sitesString = Arrays.stream(sites)
                .mapToObj(site -> Integer.toString(site + 1))
                .collect(Collectors.joining(","));

        StringBuilder sb = new StringBuilder(proteinAccession.length() + sitesString.length() + 3);
        sb.append(proteinAccession).append(" (").append(sitesString).append(")");

        return sb.toString();
    }

    /**
     * Returns the peptide modification location confidence as a string.
     *
     * @param peptideMatch the peptide match
     * @param modificationParameters the modification parameters
     *
     * @return the peptide modification location confidence as a string
     */
    public static String getPeptideModificationLocationConfidence(
            PeptideMatch peptideMatch,
            ModificationParameters modificationParameters
    ) {

        PSModificationScores psPtmScores = (PSModificationScores) peptideMatch.getUrParam(PSModificationScores.dummy);

        if (psPtmScores != null) {

            TreeSet<String> modList = new TreeSet(psPtmScores.getScoredModifications());

            StringBuilder result = new StringBuilder();

            for (String mod : modList) {

                if (result.length() > 0) {

                    result.append(", ");

                }

                result.append(mod).append(" (");
                ModificationScoring ptmScoring = psPtmScores.getModificationScoring(mod);
                boolean firstSite = true;

                for (int site : ptmScoring.getOrderedPtmLocations()) {

                    if (firstSite) {

                        firstSite = false;

                    } else {

                        result.append(", ");

                    }

                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);

                    switch (ptmConfidence) {
                        case ModificationScoring.NOT_FOUND:
                            result.append(site).append(": Not Scored");
                            break;
                        case ModificationScoring.RANDOM:
                            result.append(site).append(": Random");
                            break;
                        case ModificationScoring.DOUBTFUL:
                            result.append(site).append(": Doubtfull");
                            break;
                        case ModificationScoring.CONFIDENT:
                            result.append(site).append(": Confident");
                            break;
                        case ModificationScoring.VERY_CONFIDENT:
                            result.append(site).append(": Very Confident");
                            break;
                        default:
                            break;
                    }
                }

                result.append(")");

            }

            return result.toString();

        }

        return "";
    }

    /**
     * Writes the title of the section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : peptideFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        writer.newLine();
    }
}
