package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import eu.isas.peptideshaker.filtering.items.AssumptionFilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String spectrumKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
        return isValidated(itemName, filterItemComparator, value, spectrumKey, peptideAssumption, identification, identificationFeaturesGenerator, identificationParameters, peptideSpectrumAnnotator);
    }

    /**
     * Tests whether a match is validated by this filter.
     *
     * @param matchKey the key of the match
     * @param peptideAssumption the peptide assumption
     * @param identification the identification where to get the information
     * from
     * @param identificationFeaturesGenerator the identification features
     * generator providing identification features
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator the annotator to use to annotate spectra
     * when filtering on psm or assumptions
     *
     * @return a boolean indicating whether a match is validated by a given
     * filter
     *
     * @throws java.io.IOException exception thrown whenever an exception
     * occurred while reading or writing a file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading issue occurred while validating that the match passes the
     * filter
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserilalizing a match
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public boolean isValidated(String matchKey, PeptideAssumption peptideAssumption, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        if (exceptions.contains(matchKey)) {
            return false;
        }

        if (manualValidation.contains(matchKey)) {
            return true;
        }
        for (String itemName : valuesMap.keySet()) {
            FilterItemComparator filterItemComparator = comparatorsMap.get(itemName);
            Object value = valuesMap.get(itemName);
            if (!isValidated(itemName, filterItemComparator, value, matchKey, peptideAssumption, identification, identificationFeaturesGenerator, identificationParameters, peptideSpectrumAnnotator)) {
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
     * @param spectrumKey the key of the match of interest
     * @param peptideAssumption the assumption to validate
     * @param identification the identification objects where to get
     * identification matches from
     * @param identificationFeaturesGenerator the identification feature
     * generator where to get identification features
     * @param identificationParameters the identification parameters used
     * @param peptideSpectrumAnnotator the annotator to use to annotate spectra
     * when filtering on PSM or assumptions
     *
     * @return a boolean indicating whether the match designated by the protein
     * key validates the given item using the given comparator and value
     * threshold.
     *
     * @throws java.io.IOException exception thrown whenever an exception
     * occurred while reading or writing a file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading issue occurred while validating that the match passes the
     * filter
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserilalizing a match
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String spectrumKey, PeptideAssumption peptideAssumption, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        AssumptionFilterItem filterItem = AssumptionFilterItem.getItem(itemName);
        if (filterItem == null) {
            throw new IllegalArgumentException("Filter item " + itemName + "not recognized as spectrum assumption filter item.");
        }
        String input = value.toString();
        switch (filterItem) {
            case precrusorMz:
                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                Double mz = precursor.getMz();
                return filterItemComparator.passes(input, mz.toString());
            case precrusorRT:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                Double rt = precursor.getRt();
                return filterItemComparator.passes(input, rt.toString());
            case precrusorCharge:
                Integer charge = peptideAssumption.getIdentificationCharge().value;
                return filterItemComparator.passes(input, charge.toString());
            case precrusorMzErrorDa:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                SearchParameters searchParameters = identificationParameters.getSearchParameters();
                Double mzError = Math.abs(peptideAssumption.getDeltaMass(precursor.getMz(), false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
                return filterItemComparator.passes(input, mzError.toString());
            case precrusorMzErrorPpm:
                searchParameters = identificationParameters.getSearchParameters();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                mzError = Math.abs(peptideAssumption.getDeltaMass(precursor.getMz(), true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
                return filterItemComparator.passes(input, mzError.toString());
            case precrusorMzErrorStat:
                searchParameters = identificationParameters.getSearchParameters();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                mzError = peptideAssumption.getDeltaMass(precursor.getMz(), identificationParameters.getSearchParameters().isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                NonSymmetricalNormalDistribution precDeviationDistribution = identificationFeaturesGenerator.getMassErrorDistribution(Spectrum.getSpectrumFile(spectrumKey));
                Double p;
                if (mzError > precDeviationDistribution.getMean()) {
                    p = precDeviationDistribution.getDescendingCumulativeProbabilityAt(mzError);
                } else {
                    p = precDeviationDistribution.getCumulativeProbabilityAt(mzError);
                }
                return filterItemComparator.passes(input, p.toString());
            case sequenceCoverage:
                SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                Peptide peptide = peptideAssumption.getPeptide();
                AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
                SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                HashMap<Integer, ArrayList<IonMatch>> matches = peptideSpectrumAnnotator.getCoveredAminoAcids(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);
                double nCovered = 0;
                int nAA = peptide.getSequence().length();
                for (int i = 0; i <= nAA; i++) {
                    ArrayList<IonMatch> matchesAtAa = matches.get(i);
                    if (matchesAtAa != null && !matchesAtAa.isEmpty()) {
                        nCovered++;
                    }
                }
                Double coverage = 100.0 * nCovered / nAA;
                return filterItemComparator.passes(input, coverage.toString());
            case algorithmScore:
                Double score = peptideAssumption.getRawScore();
                if (score == null) {
                    score = peptideAssumption.getScore();
                }
                return filterItemComparator.passes(input, score.toString());
            case fileNames:
                return filterItemComparator.passes(input, Spectrum.getSpectrumFile(spectrumKey));
            case confidence:
                PSParameter psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(spectrumKey, psParameter);
                Double confidence = psParameter.getProteinConfidence();
                return filterItemComparator.passes(input, confidence.toString());
            case validationStatus:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(spectrumKey, psParameter);
                Integer validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation.toString());
            case stared:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(spectrumKey, psParameter);
                String starred;
                if (psParameter.getStarred()) {
                    starred = FilterItemComparator.trueFalse[0];
                } else {
                    starred = FilterItemComparator.trueFalse[1];
                }
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
