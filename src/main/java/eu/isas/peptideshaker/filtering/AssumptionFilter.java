package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import eu.isas.peptideshaker.filtering.items.AssumptionFilterItem;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.util.ArrayList;
import java.util.Map;

/**
 * Peptide Assumption filter.
 *
 * @author Marc Vaudel
 */
public class AssumptionFilter extends MatchFilter {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = 5082744251034128558L;

    /**
     * Constructor.
     */
    public AssumptionFilter() {
        this.filterType = MatchFilter.FilterType.ASSUMPTION;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     */
    public AssumptionFilter(String name) {
        this.name = name;
        this.filterType = MatchFilter.FilterType.ASSUMPTION;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     */
    public AssumptionFilter(String name, String description) {
        this.name = name;
        this.description = description;
        this.filterType = FilterType.ASSUMPTION;
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
    public AssumptionFilter(String name, String description, String condition, String reportPassed, String reportFailed) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.reportPassed = reportPassed;
        this.reportFailed = reportFailed;
        this.filterType = MatchFilter.FilterType.ASSUMPTION;
    }

    @Override
    protected MatchFilter getNew() {
        return new AssumptionFilter();
    }

    @Override
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, long spectrumMatchKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider) {
        
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumMatchKey);
        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
        
        return isValidated(itemName, filterItemComparator, value, spectrumMatchKey, spectrumMatch.getSpectrumKey(), peptideAssumption, identification, sequenceProvider, identificationFeaturesGenerator, identificationParameters);
    
    }

    /**
     * Tests whether a match is validated by this filter.
     *
     * @param spectrumMatchKey the key of the match
     * @param spectrumKey the key of the spectrum
     * @param peptideAssumption the peptide assumption
     * @param identification the identification where to get the information
     * from
     * @param sequenceProvider the sequence provider
     * @param identificationFeaturesGenerator the identification features
     * generator providing identification features
     * @param identificationParameters the identification parameters
     *
     * @return a boolean indicating whether a match is validated by a given
     * filter
     */
    public boolean isValidated(long spectrumMatchKey, String spectrumKey, PeptideAssumption peptideAssumption, Identification identification, SequenceProvider sequenceProvider, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters) {

        if (exceptions.contains(spectrumMatchKey)) {
            
            return false;
        
        }

        if (manualValidation.contains(spectrumMatchKey)) {
            
            return true;
        
        }
        
        for (String itemName : valuesMap.keySet()) {
        
            FilterItemComparator filterItemComparator = comparatorsMap.get(itemName);
            Object value = valuesMap.get(itemName);
            
            if (!isValidated(itemName, filterItemComparator, value, spectrumMatchKey, spectrumKey, peptideAssumption, identification, sequenceProvider, identificationFeaturesGenerator, identificationParameters)) {
                
                return false;
            
            }
        }
        
        return true;
        
    }

    /**
     * Indicates whether the match designated by the match key validates the
     * given item using the given comparator and value threshold.
     *
     * @param itemName the name of the item to filter on
     * @param filterItemComparator the comparator to use
     * @param value the value to use as a threshold
     * @param spectrumMatchKey the key of the match of interest
     * @param spectrumKey the key of the spectrum
     * @param peptideAssumption the assumption to validate
     * @param identification the identification objects where to get
     * identification matches from
     * @param sequenceProvider the sequence provider
     * @param identificationFeaturesGenerator the identification feature
     * generator where to get identification features
     * @param identificationParameters the identification parameters used
     *
     * @return a boolean indicating whether the match designated by the protein
     * key validates the given item using the given comparator and value
     * threshold.
     */
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, long spectrumMatchKey, String spectrumKey, PeptideAssumption peptideAssumption, 
            Identification identification, SequenceProvider sequenceProvider, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters) {

        AssumptionFilterItem filterItem = AssumptionFilterItem.getItem(itemName);
        
        if (filterItem == null) {
        
            throw new IllegalArgumentException("Filter item " + itemName + " not recognized as spectrum assumption filter item.");
        
        }
        
        String input = value.toString();
        switch (filterItem) {
            case precrusorMz:
                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                double mz = precursor.getMz();
                return filterItemComparator.passes(input, mz);
                
            case precrusorRT:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                double rt = precursor.getRt();
                return filterItemComparator.passes(input, rt);
                
            case precrusorCharge:
                int charge = peptideAssumption.getIdentificationCharge();
                return filterItemComparator.passes(input, charge);
                
            case precrusorMzErrorDa:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                SearchParameters searchParameters = identificationParameters.getSearchParameters();
                double mzError = Math.abs(peptideAssumption.getDeltaMass(precursor.getMz(), false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
                return filterItemComparator.passes(input, mzError);
                
            case precrusorMzErrorPpm:
                searchParameters = identificationParameters.getSearchParameters();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                mzError = Math.abs(peptideAssumption.getDeltaMass(precursor.getMz(), true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
                return filterItemComparator.passes(input, mzError);
                
            case precrusorMzErrorStat:
                searchParameters = identificationParameters.getSearchParameters();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                mzError = peptideAssumption.getDeltaMass(precursor.getMz(), identificationParameters.getSearchParameters().isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                NonSymmetricalNormalDistribution precDeviationDistribution = identificationFeaturesGenerator.getMassErrorDistribution(Spectrum.getSpectrumFile(spectrumKey));
                double p = mzError > precDeviationDistribution.getMean() ?
                    precDeviationDistribution.getDescendingCumulativeProbabilityAt(mzError)
                            :  precDeviationDistribution.getCumulativeProbabilityAt(mzError);
                return filterItemComparator.passes(input, p);
                
            case sequenceCoverage:
                SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
                Spectrum spectrum = spectrumFactory.getSpectrum(spectrumKey);
                Peptide peptide = peptideAssumption.getPeptide();
                AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();
                ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrum.getSpectrumKey(), peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
                Map<Integer, ArrayList<IonMatch>> matches = peptideSpectrumAnnotator.getCoveredAminoAcids(annotationPreferences, specificAnnotationPreferences, spectrum, peptide, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters, true);
                double nCovered = 0;
                int nAA = peptide.getSequence().length();
                
                for (int i = 0; i <= nAA; i++) {
                
                    ArrayList<IonMatch> matchesAtAa = matches.get(i);
                    
                    if (matchesAtAa != null && !matchesAtAa.isEmpty()) {
                    
                        nCovered++;
                    
                    }
                }
                
                double coverage = 100.0 * nCovered / nAA;
                
                return filterItemComparator.passes(input, coverage);
            
            case algorithmScore:
                double score = peptideAssumption.getRawScore();
                return filterItemComparator.passes(input, score);
                
            case fileNames:
                return filterItemComparator.passes(input, Spectrum.getSpectrumFile(spectrumKey));
            
            case confidence:
                PSParameter psParameter = (PSParameter)(identification.getSpectrumMatch(spectrumMatchKey)).getUrParam(PSParameter.dummy);
                double confidence = psParameter.getConfidence();
                return filterItemComparator.passes(input, confidence);
           
            case validationStatus:
                psParameter = (PSParameter)(identification.getSpectrumMatch(spectrumMatchKey)).getUrParam(PSParameter.dummy);
                Integer validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation.toString());
                
            case stared:
                psParameter = (PSParameter)(identification.getSpectrumMatch(spectrumMatchKey)).getUrParam(PSParameter.dummy);
                String starred = psParameter.getStarred() ? FilterItemComparator.trueFalse[0] : FilterItemComparator.trueFalse[1];
                return filterItemComparator.passes(input, starred);
                
            default:
                throw new IllegalArgumentException("Protein filter not implemented for item " + filterItem.name + ".");
        }
    }

    @Override
    public FilterItem[] getPossibleFilterItems() {
        return AssumptionFilterItem.values();
    }

    @Override
    public FilterItem getFilterItem(String itemName) {
        return AssumptionFilterItem.getItem(itemName);
    }
}
