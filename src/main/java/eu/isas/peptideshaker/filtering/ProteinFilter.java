package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import eu.isas.peptideshaker.filtering.items.ProteinFilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSModificationScores;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Protein filter.
 *
 * @author Marc Vaudel
 */
public class ProteinFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 5753850468907866679L;

    /**
     * Constructor.
     */
    public ProteinFilter() {

    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     */
    public ProteinFilter(String name) {
        this.name = name;
        this.filterType = FilterType.PROTEIN;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     */
    public ProteinFilter(String name, String description) {
        this.name = name;
        this.description = description;
        this.filterType = FilterType.PROTEIN;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     * @param condition a description of the condition to be met to pass the
     * filter
     * @param reportPassed a report for when the filter is passed
     * @param reportFailed a report for when the filter is not passed
     */
    public ProteinFilter(String name, String description, String condition, String reportPassed, String reportFailed) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.reportPassed = reportPassed;
        this.reportFailed = reportFailed;
        this.filterType = FilterType.PROTEIN;
    }

    @Override
    protected MatchFilter getNew() {
        return new ProteinFilter();
    }

    @Override
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, long matchKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider) {

        ProteinFilterItem filterItem = ProteinFilterItem.getItem(itemName);
        if (filterItem == null) {
            throw new IllegalArgumentException("Filter item " + itemName + "not recognized as protein filter item.");
        }
        String input = value.toString();
        switch (filterItem) {
            case proteinAccession:
                ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
                return filterItemComparator.passes(input, proteinMatch.getAccessions());

            case proteinDescription:
                proteinMatch = identification.getProteinMatch(matchKey);
                return filterItemComparator.passes(input, Arrays.stream(proteinMatch.getAccessions())
                        .map(accession -> proteinDetailsProvider.getDescription(accession))
                        .toArray(String[]::new));

            case sequence:
                proteinMatch = identification.getProteinMatch(matchKey);
                return filterItemComparator.passes(input, Arrays.stream(proteinMatch.getAccessions())
                        .map(accession -> sequenceProvider.getSequence(accession))
                        .toArray(String[]::new));

            case chromosome:
                proteinMatch = identification.getProteinMatch(matchKey);
                return filterItemComparator.passes(input, Arrays.stream(proteinMatch.getAccessions())
                        .map(accession -> geneMaps.getChromosome(
                        proteinDetailsProvider.getGeneName(accession)))
                        .toArray(String[]::new));

            case gene:
                proteinMatch = identification.getProteinMatch(matchKey);
                return filterItemComparator.passes(input, Arrays.stream(proteinMatch.getAccessions())
                        .map(accession -> proteinDetailsProvider.getGeneName(accession))
                        .toArray(String[]::new));

            case GO:
                proteinMatch = identification.getProteinMatch(matchKey);
                return filterItemComparator.passes(input, Arrays.stream(proteinMatch.getAccessions())
                        .flatMap(accession -> geneMaps.getGoNamesForProtein(accession).stream())
                        .toArray(String[]::new));

            case expectedCoverage:
                double coverage = 100 * identificationFeaturesGenerator.getObservableCoverage(matchKey);
                return filterItemComparator.passes(input, coverage);

            case validatedCoverage:
                coverage = 100 * identificationFeaturesGenerator.getValidatedSequenceCoverage(matchKey);
                return filterItemComparator.passes(input, coverage);

            case confidentCoverage:
                HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(matchKey);
                coverage = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return filterItemComparator.passes(input, coverage);

            case spectrumCounting:
                double spectrumCounting = identificationFeaturesGenerator.getSpectrumCounting(matchKey);
                return filterItemComparator.passes(input, spectrumCounting);

            case modification:
                proteinMatch = identification.getProteinMatch(matchKey);
                PSModificationScores modificationScores = new PSModificationScores();
                modificationScores = (PSModificationScores) proteinMatch.getUrParam(modificationScores);
                Set<String> modifications = modificationScores == null ? new HashSet<>(0)
                        : modificationScores.getScoredModifications();
                return filterItemComparator.passes(input, modifications);

            case nPeptides:
                proteinMatch = identification.getProteinMatch(matchKey);
                int nPeptides = proteinMatch.getPeptideCount();
                return filterItemComparator.passes(input, nPeptides);

            case nValidatedPeptides:
                nPeptides = identificationFeaturesGenerator.getNValidatedPeptides(matchKey);
                return filterItemComparator.passes(input, nPeptides);

            case nConfidentPeptides:
                nPeptides = identificationFeaturesGenerator.getNConfidentPeptides(matchKey);
                return filterItemComparator.passes(input, nPeptides);

            case nPSMs:
                int nPsms = identificationFeaturesGenerator.getNSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms);

            case nValidatedPSMs:
                nPsms = identificationFeaturesGenerator.getNValidatedSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms);

            case nConfidentPSMs:
                nPsms = identificationFeaturesGenerator.getNConfidentSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms);

            case confidence:
                proteinMatch = identification.getProteinMatch(matchKey);
                PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                double confidence = psParameter.getConfidence();
                return filterItemComparator.passes(input, confidence);

            case proteinInference:
                proteinMatch = identification.getProteinMatch(matchKey);
                psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                int pi = psParameter.getProteinInferenceGroupClass();
                return filterItemComparator.passes(input, pi);

            case validationStatus:
                proteinMatch = identification.getProteinMatch(matchKey);
                psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                int validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation);

            case stared:
                proteinMatch = identification.getProteinMatch(matchKey);
                psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                String starred = psParameter.getStarred() ? FilterItemComparator.trueFalse[0] : FilterItemComparator.trueFalse[1];
                return filterItemComparator.passes(input, starred);

            default:
                throw new IllegalArgumentException("Protein filter not implemented for item " + filterItem.name + ".");
        }
    }

    @Override
    public FilterItem[] getPossibleFilterItems() {
        return ProteinFilterItem.values();
    }

    @Override
    public FilterItem getFilterItem(String itemName) {
        return ProteinFilterItem.getItem(itemName);
    }
}
