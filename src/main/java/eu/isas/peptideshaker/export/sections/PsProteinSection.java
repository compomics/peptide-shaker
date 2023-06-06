package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.quantification.spectrumcounting.SpectrumCountingMethod;
import com.compomics.util.experiment.units.Units;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.io.export.features.peptideshaker.PsFragmentFeature;
import com.compomics.util.io.export.features.peptideshaker.PsIdentificationAlgorithmMatchesFeature;
import com.compomics.util.io.export.features.peptideshaker.PsPeptideFeature;
import com.compomics.util.io.export.features.peptideshaker.PsProteinFeature;
import com.compomics.util.io.export.features.peptideshaker.PsPsmFeature;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import eu.isas.peptideshaker.export.ExportUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import no.uib.jsparklines.data.XYDataPoint;

/**
 * This class outputs the protein related export features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsProteinSection {

    /**
     * The protein features to export.
     */
    private final EnumSet<PsProteinFeature> proteinFeatures = EnumSet.noneOf(PsProteinFeature.class);
    /**
     * The peptide subsection if any.
     */
    private PsPeptideSection peptideSection = null;
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
     * @param exportFeatures the features to export in this section.
     * ProteinFeatures as main features. If Peptide or protein features are
     * selected, they will be added as sub-sections.
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsProteinSection(
            ArrayList<ExportFeature> exportFeatures,
            boolean indexes,
            boolean header,
            ExportWriter writer
    ) {

        ArrayList<ExportFeature> peptideFeatures = new ArrayList<>();

        for (ExportFeature exportFeature : exportFeatures) {

            if (exportFeature instanceof PsProteinFeature) {

                proteinFeatures.add((PsProteinFeature) exportFeature);

            } else if (exportFeature instanceof PsPeptideFeature
                    || exportFeature instanceof PsPsmFeature
                    || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature
                    || exportFeature instanceof PsFragmentFeature) {

                peptideFeatures.add(exportFeature);

            } else {

                throw new IllegalArgumentException(
                        "Export feature of type "
                        + exportFeature.getClass()
                        + " not recognized."
                );

            }
        }

        if (!peptideFeatures.isEmpty()) {

            peptideSection = new PsPeptideSection(
                    peptideFeatures,
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
     * @param geneMaps The gene maps.
     * @param identificationParameters The identification parameters.
     * @param keys The keys of the PSM matches to output.
     * @param nSurroundingAa The number of surrounding amino acids to export.
     * @param validatedOnly Whether only validated matches should be exported.
     * @param decoys Whether decoy matches should be exported as well.
     * @param waitingHandler The waiting handler.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing to the file
     */
    public void writeSection(
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SpectrumProvider spectrumProvider,
            GeneMaps geneMaps,
            IdentificationParameters identificationParameters,
            long[] keys,
            int nSurroundingAa,
            boolean validatedOnly,
            boolean decoys,
            WaitingHandler waitingHandler
    ) throws IOException {

        if (waitingHandler != null) {

            waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        }

        if (header) {

            writeHeader(identification.getFractions());

        }

        if (keys == null) {

            keys = identification.getProteinIdentification().stream()
                    .mapToLong(Long::longValue)
                    .toArray();

        }

        int line = 1;

        if (waitingHandler != null) {

            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.length);

        }

        for (long key : keys) {

            ProteinMatch proteinMatch = identification.getProteinMatch(key);

            if (waitingHandler != null) {

                if (waitingHandler.isRunCanceled()) {

                    return;

                }

                waitingHandler.increaseSecondaryProgressCounter();

            }

            if (decoys || !proteinMatch.isDecoy()) {

                PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                    boolean first = true;

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        first = false;

                    }

                    for (ExportFeature exportFeature : proteinFeatures) {

                        if (!first) {

                            writer.addSeparator();

                        } else {

                            first = false;

                        }

                        PsProteinFeature tempProteinFeature = (PsProteinFeature) exportFeature;

                        if (tempProteinFeature.isPerFraction()) {
                            for (int fractionsCount = 0; fractionsCount < identification.getFractions().size(); fractionsCount++) {

                                String fractionName = identification.getFractions().get(fractionsCount);

                                if (fractionsCount > 0) {
                                    writer.addSeparator();
                                }

                                writer.write(getFeature(identificationFeaturesGenerator,
                                        sequenceProvider,
                                        proteinDetailsProvider,
                                        geneMaps,
                                        identificationParameters,
                                        nSurroundingAa,
                                        key,
                                        proteinMatch,
                                        fractionName,
                                        psParameter,
                                        tempProteinFeature,
                                        waitingHandler
                                )
                                );
                            }
                        } else {

                            writer.write(getFeature(identificationFeaturesGenerator,
                                    sequenceProvider,
                                    proteinDetailsProvider,
                                    geneMaps,
                                    identificationParameters,
                                    nSurroundingAa,
                                    key,
                                    proteinMatch,
                                    psParameter,
                                    tempProteinFeature,
                                    waitingHandler
                            )
                            );

                        }

                    }

                    writer.newLine();

                    if (peptideSection != null) {

                        writer.increaseDepth();

                        if (waitingHandler != null) {

                            waitingHandler.setDisplayProgress(false);

                        }

                        peptideSection.writeSection(identification,
                                identificationFeaturesGenerator,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider,
                                identificationParameters,
                                proteinMatch.getPeptideMatchesKeys(),
                                nSurroundingAa,
                                line + ".",
                                validatedOnly,
                                decoys,
                                waitingHandler
                        );

                        if (waitingHandler != null) {

                            waitingHandler.setDisplayProgress(true);

                        }

                        writer.decreseDepth();

                    }

                    line++;

                }
            }
        }
    }

    /**
     * Returns the part of the desired section.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param geneMaps the gene maps
     * @param identificationParameters the identification parameters
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino acids, the number of surrounding amino acids to use
     * @param proteinKey the key of the protein match being written
     * @param proteinMatch the protein match, can be null if not needed
     * @param psParameter the protein match parameter containing the
     * PeptideShaker parameters, can be null if not needed
     * @param tempProteinFeatures the protein feature to write
     * @param waitingHandler the waiting handler
     *
     * @return the string to write
     */
    public static String getFeature(IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            GeneMaps geneMaps,
            IdentificationParameters identificationParameters,
            int nSurroundingAas,
            long proteinKey,
            ProteinMatch proteinMatch,
            PSParameter psParameter,
            PsProteinFeature tempProteinFeatures,
            WaitingHandler waitingHandler
    ) {

        return getFeature(identificationFeaturesGenerator,
                sequenceProvider,
                proteinDetailsProvider,
                geneMaps,
                identificationParameters,
                nSurroundingAas,
                proteinKey,
                proteinMatch,
                null,
                psParameter,
                tempProteinFeatures,
                waitingHandler);
    }

    /**
     * Returns the part of the desired section.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param geneMaps the gene maps
     * @param identificationParameters the identification parameters
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino acids, the number of surrounding amino acids to use
     * @param proteinKey the key of the protein match being written
     * @param proteinMatch the protein match, can be null if not needed
     * @param fractionName the name of the fraction
     * @param psParameter the protein match parameter containing the
     * PeptideShaker parameters, can be null if not needed
     * @param tempProteinFeatures the protein feature to write
     * @param waitingHandler the waiting handler
     *
     * @return the string to write
     */
    public static String getFeature(
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            GeneMaps geneMaps,
            IdentificationParameters identificationParameters,
            int nSurroundingAas,
            long proteinKey,
            ProteinMatch proteinMatch,
            String fractionName,
            PSParameter psParameter,
            PsProteinFeature tempProteinFeatures,
            WaitingHandler waitingHandler
    ) {

        switch (tempProteinFeatures) {

            case accession:

                return proteinMatch.getLeadingAccession();

            case protein_description:

                return proteinDetailsProvider.getSimpleDescription(proteinMatch.getLeadingAccession());

            case ensembl_gene_id:

                if (!proteinMatch.isDecoy()) {

                    String geneName = proteinDetailsProvider.getGeneName(proteinMatch.getLeadingAccession());

                    if (geneName != null) {

                        String ensemblId = geneMaps.getEnsemblId(geneName);

                        if (ensemblId != null) {

                            return ensemblId;

                        }
                    }
                }

                return "";

            case gene_name:

                if (!proteinMatch.isDecoy()) {

                    String geneName = proteinDetailsProvider.getGeneName(proteinMatch.getLeadingAccession());

                    if (geneName != null) {

                        return geneName;

                    }
                }

                return "";

            case chromosome:

                if (!proteinMatch.isDecoy()) {

                    String geneName = proteinDetailsProvider.getGeneName(proteinMatch.getLeadingAccession());

                    if (geneName != null) {

                        String chromosome = geneMaps.getChromosome(geneName);

                        if (chromosome != null) {

                            return chromosome;

                        }
                    }
                }

                return "";

            case taxonomy:

                if (!proteinMatch.isDecoy()) {

                    String taxonomy = proteinDetailsProvider.getTaxonomy(proteinMatch.getLeadingAccession());

                    if (taxonomy != null) {

                        return taxonomy;

                    }
                }

                return "";

            case organism_identifier:

                if (!proteinMatch.isDecoy()) {

                    String taxonomy = proteinDetailsProvider.getOrganismIdentifier(proteinMatch.getLeadingAccession());

                    if (taxonomy != null) {

                        return taxonomy;

                    }
                }

                return "";

            case go_accession:

                return proteinMatch.isDecoy() ? ""
                        : Arrays.stream(proteinMatch.getAccessions())
                                .map(
                                        accession -> geneMaps.getGoTermsForProtein(accession)
                                )
                                .filter(
                                        Objects::nonNull
                                )
                                .flatMap(
                                        HashSet::stream
                                )
                                .distinct()
                                .sorted()
                                .collect(
                                        Collectors.joining(",")
                                );

            case go_description:

                return proteinMatch.isDecoy() ? ""
                        : Arrays.stream(proteinMatch.getAccessions())
                                .map(
                                        accession -> geneMaps.getGoTermsForProtein(accession)
                                )
                                .filter(
                                        Objects::nonNull
                                )
                                .flatMap(
                                        HashSet::stream
                                )
                                .distinct()
                                .map(
                                        goTerm -> geneMaps.getNameForGoTerm(goTerm)
                                )
                                .filter(
                                        Objects::nonNull
                                )
                                .distinct()
                                .sorted()
                                .collect(Collectors.joining(","));

            case other_proteins:

                String mainAccession = proteinMatch.getLeadingAccession();

                return Arrays.stream(proteinMatch.getAccessions())
                        .filter(
                                accession -> !accession.equals(mainAccession)
                        )
                        .collect(
                                Collectors.joining(",")
                        );

            case protein_group:

                return Arrays.stream(proteinMatch.getAccessions())
                        .collect(
                                Collectors.joining(",")
                        );

            case descriptions:

                return Arrays.stream(proteinMatch.getAccessions())
                        .map(
                                accession -> proteinDetailsProvider.getDescription(accession)
                        )
                        .collect(
                                Collectors.joining(",")
                        );

            case confidence:

                return Double.toString(psParameter.getConfidence());

            case confident_modification_sites:

                mainAccession = proteinMatch.getLeadingAccession();
                String sequence = sequenceProvider.getSequence(mainAccession);
                ModificationMatch[] modificationMatches = proteinMatch.getVariableModifications();

                return identificationFeaturesGenerator.getModificationSites(sequence, modificationMatches, true);

            case confident_modification_sites_number:
                
                modificationMatches = proteinMatch.getVariableModifications();

                return identificationFeaturesGenerator.getModificationSitesNumber(modificationMatches, true);

            case ambiguous_modification_sites:

                mainAccession = proteinMatch.getLeadingAccession();
                sequence = sequenceProvider.getSequence(mainAccession);
                modificationMatches = proteinMatch.getVariableModifications();

                return identificationFeaturesGenerator.getModificationSites(sequence, modificationMatches, false);

            case ambiguous_modification_sites_number:
                
                modificationMatches = proteinMatch.getVariableModifications();

                return identificationFeaturesGenerator.getModificationSitesNumber(modificationMatches, false);

            case confident_phosphosites:

                mainAccession = proteinMatch.getLeadingAccession();
                sequence = sequenceProvider.getSequence(mainAccession);
                modificationMatches = proteinMatch.getVariableModifications();

                HashSet<String> modifications = ExportUtils.getPhosphorylations(identificationParameters.getSearchParameters().getModificationParameters());

                return identificationFeaturesGenerator.getModificationSites(sequence, modificationMatches, true, modifications);

            case confident_phosphosites_number:
                
                modificationMatches = proteinMatch.getVariableModifications();
                modifications = ExportUtils.getPhosphorylations(identificationParameters.getSearchParameters().getModificationParameters());

                return Integer.toString(identificationFeaturesGenerator.getModificationSitesNumber(modificationMatches, true, modifications));

            case ambiguous_phosphosites:

                mainAccession = proteinMatch.getLeadingAccession();
                sequence = sequenceProvider.getSequence(mainAccession);
                modificationMatches = proteinMatch.getVariableModifications();

                modifications = ExportUtils.getPhosphorylations(identificationParameters.getSearchParameters().getModificationParameters());

                return identificationFeaturesGenerator.getModificationSites(sequence, modificationMatches, false, modifications);

            case ambiguous_phosphosites_number:
                
                modificationMatches = proteinMatch.getVariableModifications();
                modifications = ExportUtils.getPhosphorylations(identificationParameters.getSearchParameters().getModificationParameters());

                return Integer.toString(identificationFeaturesGenerator.getModificationSitesNumber(modificationMatches, false, modifications));

            case coverage:

                double value = 100 * identificationFeaturesGenerator.getValidatedSequenceCoverage(proteinKey);
                return Double.toString(Util.roundDouble(value, 2));

            case confident_coverage:

                HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                value = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return Double.toString(Util.roundDouble(value, 2));

            case all_coverage:

                sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                Double sequenceCoverageNotValidated = 100 * sequenceCoverage.get(MatchValidationLevel.not_validated.getIndex());
                double totalCoverage = sequenceCoverageConfident + sequenceCoverageDoubtful + sequenceCoverageNotValidated;
                return Double.toString(Util.roundDouble(totalCoverage, 2));

            case possible_coverage:

                value = 100 * identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                return Double.toString(Util.roundDouble(value, 2));

            case decoy:

                return proteinMatch.isDecoy() ? "1" : "0";

            case hidden:

                return psParameter.getHidden() ? "1" : "0";

            case mw:

                return Double.toString(
                        ProteinUtils.computeMolecularWeight(
                                sequenceProvider.getSequence(
                                        proteinMatch.getLeadingAccession())));

            case peptidesPerFraction:

                return Integer.toString(psParameter.getFractionValidatedPeptides(fractionName));

            case spectraPerFraction:

                return Integer.toString(psParameter.getFractionValidatedSpectra(fractionName));

            case averagePrecursorIntensty:

                if (psParameter.getPrecursorIntensityAveragePerFraction(fractionName) != null) {
                    return Double.toString(psParameter.getPrecursorIntensityAveragePerFraction(fractionName));
                } else {
                    return "";
                }

            case fractionMinMwPeptideRange:

                if (psParameter.getFractionValidatedPeptides(fractionName) > 0) {
                    return getMinMaxMw(identificationParameters, fractionName, true);
                } else {
                    return "";
                }

            case fractionMaxMwPeptideRange:

                if (psParameter.getFractionValidatedPeptides(fractionName) > 0) {
                    return getMinMaxMw(identificationParameters, fractionName, false);
                } else {
                    return "";
                }

            case fractionMinMwSpectraRange:

                if (psParameter.getFractionValidatedSpectra(fractionName) > 0) {
                    return getMinMaxMw(identificationParameters, fractionName, true);
                } else {
                    return "";
                }

            case fractionMaxMwSpectraRange:

                if (psParameter.getFractionValidatedSpectra(fractionName) > 0) {
                    return getMinMaxMw(identificationParameters, fractionName, false);
                } else {
                    return "";
                }

            case proteinLength:

                return Double.toString(
                        sequenceProvider.getSequence(
                                proteinMatch.getLeadingAccession()).length()
                );

            case non_enzymatic:

                return Integer.toString(
                        identificationFeaturesGenerator.getNonEnzymatic(
                                proteinKey,
                                identificationParameters.getSearchParameters().getDigestionParameters()
                        ).length
                );

            case pi:

                return psParameter.getProteinInferenceClassAsString();

            case peptides:

                return Integer.toString(
                        proteinMatch.getPeptideCount());

            case psms:

                return Integer.toString(
                        identificationFeaturesGenerator.getNSpectra(proteinKey));

            case validated_peptides:

                return Integer.toString(
                        identificationFeaturesGenerator.getNValidatedPeptides(proteinKey));

            case unique_peptides:

                return Integer.toString(
                        identificationFeaturesGenerator.getNUniquePeptides(proteinKey));

            case unique_validated_peptides:

                return Integer.toString(
                        identificationFeaturesGenerator.getNUniqueValidatedPeptides(proteinKey));

            case validated_psms:

                return Integer.toString(
                        identificationFeaturesGenerator.getNValidatedSpectra(proteinKey));

            case score:

                return Double.toString(psParameter.getTransformedScore());

            case raw_score:

                return Double.toString(
                        psParameter.getScore());

            case spectrum_counting:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey)
                );

            case spectrum_counting_nsaf:

                return Double.toString(
                        identificationFeaturesGenerator.getSpectrumCounting(
                                proteinKey,
                                SpectrumCountingMethod.NSAF
                        )
                );

            case spectrum_counting_empai:

                return Double.toString(
                        identificationFeaturesGenerator.getSpectrumCounting(
                                proteinKey,
                                SpectrumCountingMethod.EMPAI
                        )
                );

            case label_free_quantification:

                return Double.toString(
                        identificationFeaturesGenerator.getSpectrumCounting(
                                proteinKey,
                                SpectrumCountingMethod.LFQ
                        )
                );

            case spectrum_counting_empai_percent:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.percent,
                                SpectrumCountingMethod.EMPAI
                        )
                );

            case spectrum_counting_nsaf_percent:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.percent,
                                SpectrumCountingMethod.NSAF
                        )
                );

            case label_free_quantification_percent:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.percent,
                                SpectrumCountingMethod.LFQ
                        )
                );

            case spectrum_counting_empai_ppm:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.ppm,
                                SpectrumCountingMethod.EMPAI
                        )
                );

            case spectrum_counting_nsaf_ppm:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.ppm,
                                SpectrumCountingMethod.NSAF
                        )
                );

            case label_free_quantification_ppm:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.ppm,
                                SpectrumCountingMethod.LFQ
                        )
                );

            case spectrum_counting_empai_fmol:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.fmol,
                                SpectrumCountingMethod.EMPAI
                        )
                );

            case spectrum_counting_nsaf_fmol:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.fmol,
                                SpectrumCountingMethod.NSAF
                        )
                );

            case label_free_quantification_fmol:

                return Double.toString(
                        identificationFeaturesGenerator.getNormalizedSpectrumCounting(
                                proteinKey,
                                Units.fmol,
                                SpectrumCountingMethod.LFQ
                        )
                );

            case starred:

                return psParameter.getStarred() ? "1" : "0";

            case validated:

                return psParameter.getMatchValidationLevel().toString();

            default:

                return "Not implemented";
        }
    }

    /**
     * Writes the header of the protein section.
     *
     * @param fractions the fraction names
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeHeader(ArrayList<String> fractions) throws IOException {

        if (indexes) {

            writer.writeHeaderText("");
            writer.addSeparator();

        }

        boolean firstColumn = true;

        for (PsProteinFeature exportFeature : proteinFeatures) {

            if (firstColumn) {

                firstColumn = false;

            } else {

                writer.addSeparator();

            }

            if (exportFeature.isPerFraction()) {

                for (int i = 0; i < fractions.size(); i++) {

                    if (i > 0) {
                        writer.addSeparator();
                    }

                    writer.writeHeaderText(exportFeature.getTitle() + " " + fractions.get(i));

                }

            } else {

                writer.writeHeaderText(exportFeature.getTitle());

            }

        }

        writer.newLine();

    }

    /**
     * Returns the minimum or maximum molecular weight for the given fraction.
     *
     * @param identificationParameters the identification parameters
     * @param fractionName the fraction name
     * @param minimum if true, the minimum molecular weight is returned, if
     * false, the maximum molecular weight is returned
     *
     * @return the minimum or maximum molecular weight for the given fraction
     */
    private static String getMinMaxMw(IdentificationParameters identificationParameters, String fractionName, boolean minimum) {

        double maxMw = Double.MIN_VALUE;
        double minMw = Double.MAX_VALUE;

        HashMap<String, XYDataPoint> expectedMolecularWeightRanges
                = identificationParameters.getFractionParameters().getFractionMolecularWeightRanges();

        if (expectedMolecularWeightRanges != null && expectedMolecularWeightRanges.get(fractionName) != null) {

            double lower = expectedMolecularWeightRanges.get(fractionName).getX();
            double upper = expectedMolecularWeightRanges.get(fractionName).getY();

            if (lower < minMw) {
                minMw = lower;
            }

            if (upper > maxMw) {
                maxMw = upper;
            }
        }

        if (minimum) {
            if (minMw != Double.MIN_VALUE) {
                return Double.toString(minMw);
            } else {
                return "";
            }
        } else {
            if (maxMw != Double.MAX_VALUE) {
                return Double.toString(maxMw);
            } else {
                return "";
            }
        }
    }
}
