package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.protein.Header;
import eu.isas.peptideshaker.filtering.items.PeptideFilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.swing.RowFilter.ComparisonType;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Peptide filter.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 959658989341486818L;

    /**
     * Constructor.
     */
    public PeptideFilter() {
        this.filterType = FilterType.PEPTIDE;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     */
    public PeptideFilter(String name) {
        this.name = name;
        this.filterType = FilterType.PEPTIDE;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     */
    public PeptideFilter(String name, String description) {
        this.name = name;
        this.description = description;
        this.filterType = FilterType.PEPTIDE;
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
    public PeptideFilter(String name, String description, String condition, String reportPassed, String reportFailed) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.reportPassed = reportPassed;
        this.reportFailed = reportFailed;
        this.filterType = FilterType.PEPTIDE;
    }

    @Override
    protected MatchFilter getNew() {
        return new PeptideFilter();
    }

    @Override
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        PeptideFilterItem filterItem = PeptideFilterItem.getItem(itemName);
        if (filterItem == null) {
            throw new IllegalArgumentException("Filter item " + itemName + "not recognized as peptide filter item.");
        }
        String input = value.toString();
        switch (filterItem) {
            case proteinAccession:
                PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
                return filterItemComparator.passes(input, peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences()));
            case proteinDescription:
                peptideMatch = identification.getPeptideMatch(matchKey);
                ArrayList<String> accessions = peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                ArrayList<String> descriptions = new ArrayList<String>();
                for (String accession : accessions) {
                    Header proteinHeader = SequenceFactory.getInstance().getHeader(accession);
                    descriptions.add(proteinHeader.getDescription());
                }
                return filterItemComparator.passes(input, descriptions);
            case sequence:
                return filterItemComparator.passes(input, Peptide.getSequence(matchKey));
            case ptm:
                peptideMatch = identification.getPeptideMatch(matchKey);
                ArrayList<String> ptms;
                PSPtmScores psPtmScores = new PSPtmScores();
                psPtmScores = (PSPtmScores) peptideMatch.getUrParam(psPtmScores);
                if (psPtmScores != null) {
                    ptms = psPtmScores.getScoredPTMs();
                } else {
                    ptms = new ArrayList<String>(0);
                }
                return filterItemComparator.passes(input, ptms);
            case nPSMs:
                peptideMatch = identification.getPeptideMatch(matchKey);
                Integer nPsms = peptideMatch.getSpectrumCount();
                return filterItemComparator.passes(input, nPsms.toString());
            case nValidatedPSMs:
                nPsms = identificationFeaturesGenerator.getNValidatedSpectraForPeptide(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case nConfidentPSMs:
                nPsms = identificationFeaturesGenerator.getNConfidentSpectraForPeptide(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case confidence:
                PSParameter psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                Double confidence = psParameter.getProteinConfidence();
                return filterItemComparator.passes(input, confidence.toString());
            case proteinInference:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                Integer pi = psParameter.getProteinInferenceClass();
                return filterItemComparator.passes(input, pi.toString());
            case validationStatus:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                Integer validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation.toString());
            case stared:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                String starred;
                if (psParameter.isStarred()) {
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
        return PeptideFilterItem.values();
    }

    @Override
    public FilterItem getFilterItem(String itemName) {
        return PeptideFilterItem.getItem(itemName);
    }
}
