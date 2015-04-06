package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.protein.Header;
import eu.isas.peptideshaker.filtering.items.PeptideFilterItem;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
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
        return new ProteinFilter();
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

    /**
     * Checks whether it is an old filter using the deprecated code below and
     * converts it to the new structure
     */
    public void backwardCompatibilityCheck() {
        if (protein != null) {
            setFilterItem(PeptideFilterItem.proteinAccession.name, FilterItemComparator.matches, protein);
            protein = null;
        }
        if (sequence != null) {
            setFilterItem(PeptideFilterItem.proteinAccession.name, FilterItemComparator.matches, sequence);
            sequence = null;
        }
        if (proteinPattern != null) {
            setFilterItem(PeptideFilterItem.sequence.name, FilterItemComparator.matches, proteinPattern);
            proteinPattern = null;
        }
        if (sequencePattern != null) {
            setFilterItem(PeptideFilterItem.sequence.name, FilterItemComparator.matches, sequencePattern);
            sequencePattern = null;
        }
        if (nSpectra != null) {
            if (nSpectraComparison == ComparisonType.BEFORE) {
                setFilterItem(PeptideFilterItem.nPSMs.name, FilterItemComparator.lower, nSpectra);
            } else if (nSpectraComparison == ComparisonType.AFTER) {
                setFilterItem(PeptideFilterItem.nPSMs.name, FilterItemComparator.higher, nSpectra);
            } else if (nSpectraComparison == ComparisonType.EQUAL) {
                setFilterItem(PeptideFilterItem.nPSMs.name, FilterItemComparator.equal, nSpectra);
            }
            nSpectra = null;
        }
        if (nValidatedSpectra != null) {
            if (nValidatedSpectraComparison == ComparisonType.BEFORE) {
                setFilterItem(PeptideFilterItem.nValidatedPSMs.name, FilterItemComparator.lower, nValidatedSpectra);
            } else if (nValidatedSpectraComparison == ComparisonType.AFTER) {
                setFilterItem(PeptideFilterItem.nValidatedPSMs.name, FilterItemComparator.higher, nValidatedSpectra);
            } else if (nValidatedSpectraComparison == ComparisonType.EQUAL) {
                setFilterItem(PeptideFilterItem.nValidatedPSMs.name, FilterItemComparator.equal, nValidatedSpectra);
            }
            nValidatedSpectra = null;
        }
        if (nConfidentSpectra != null) {
            if (nConfidentSpectraComparison == ComparisonType.BEFORE) {
                setFilterItem(PeptideFilterItem.nConfidentPSMs.name, FilterItemComparator.lower, nConfidentSpectra);
            } else if (nConfidentSpectraComparison == ComparisonType.AFTER) {
                setFilterItem(PeptideFilterItem.nConfidentPSMs.name, FilterItemComparator.higher, nConfidentSpectra);
            } else if (nConfidentSpectraComparison == ComparisonType.EQUAL) {
                setFilterItem(PeptideFilterItem.nConfidentPSMs.name, FilterItemComparator.equal, nConfidentSpectra);
            }
            nConfidentSpectra = null;
        }
        if (peptideConfidence != null) {
            if (peptideConfidenceComparison == ComparisonType.BEFORE) {
                setFilterItem(PeptideFilterItem.confidence.name, FilterItemComparator.lower, peptideConfidence);
            } else if (peptideConfidenceComparison == ComparisonType.AFTER) {
                setFilterItem(PeptideFilterItem.confidence.name, FilterItemComparator.higher, peptideConfidence);
            } else if (peptideConfidenceComparison == ComparisonType.EQUAL) {
                setFilterItem(PeptideFilterItem.confidence.name, FilterItemComparator.equal, peptideConfidence);
            }
            peptideConfidence = null;
        }
        if (pi != 5) {
            if (piComparison == ComparisonType.BEFORE) {
                setFilterItem(PeptideFilterItem.proteinInference.name, FilterItemComparator.lower, pi);
            } else if (piComparison == ComparisonType.AFTER) {
                setFilterItem(PeptideFilterItem.proteinInference.name, FilterItemComparator.higher, pi);
            } else if (piComparison == ComparisonType.EQUAL) {
                setFilterItem(PeptideFilterItem.proteinInference.name, FilterItemComparator.equal, pi);
            }
        }
    }

    /**
     * A protein regex.
     *
     * @deprecated use the filter items instead
     */
    private String protein = null;
    /**
     * Sequence regex.
     *
     * @deprecated use the filter items instead
     */
    private String sequence = null;
    /**
     * The compiled protein pattern.
     *
     * @deprecated use the filter items instead
     */
    private Pattern proteinPattern = null;
    /**
     * The compiled peptide sequence pattern.
     *
     * @deprecated use the filter items instead
     */
    private Pattern sequencePattern = null;
    /**
     * Number of spectra limit.
     *
     * @deprecated use the filter items instead
     */
    private Integer nSpectra = null;
    /**
     * The type of comparison to be used for the number of spectra.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType nSpectraComparison = ComparisonType.EQUAL;
    /**
     * Number of validated spectra limit.
     *
     * @deprecated use the filter items instead
     */
    private Integer nValidatedSpectra = null;
    /**
     * The type of comparison to be used for the number of validated spectra.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType nValidatedSpectraComparison = ComparisonType.EQUAL;
    /**
     * Number of confident spectra limit.
     *
     * @deprecated use the filter items instead
     */
    private Integer nConfidentSpectra = null;
    /**
     * The type of comparison to be used for the number of confident spectra.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType nConfidentSpectraComparison = ComparisonType.EQUAL;
    /**
     * Score limit.
     *
     * @deprecated use the filter items instead
     */
    private Double peptideScore = null;
    /**
     * The type of comparison to be used for the peptide score.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType peptideScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     *
     * @deprecated use the filter items instead
     */
    private Double peptideConfidence = null;
    /**
     * The type of comparison to be used for the peptide confidence.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType peptideConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The current protein inference filter selection.
     *
     * @deprecated use the filter items instead
     */
    private int pi = 5;
    /**
     * The type of comparison to be used for the PI.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType piComparison = ComparisonType.EQUAL;
    /**
     * The list of modifications allowed for the peptide.
     *
     * @deprecated use the filter items instead
     */
    private ArrayList<String> modificationStatus = null;
}
