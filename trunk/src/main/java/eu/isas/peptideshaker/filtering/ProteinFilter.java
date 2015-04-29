package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.protein.Header;
import eu.isas.peptideshaker.filtering.items.ProteinFilterItem;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.RowFilter.ComparisonType;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        ProteinFilterItem filterItem = ProteinFilterItem.getItem(itemName);
        if (filterItem == null) {
            throw new IllegalArgumentException("Filter item " + itemName + "not recognized as protein filter item.");
        }
        String input = value.toString();
        switch (filterItem) {
            case proteinAccession:
                return filterItemComparator.passes(input, ProteinMatch.getAccessions(matchKey));
            case proteinDescription:
                String[] accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> descriptions = new ArrayList<String>();
                for (String accession : accessions) {
                    Header proteinHeader = SequenceFactory.getInstance().getHeader(accession);
                    descriptions.add(proteinHeader.getDescription());
                }
                return filterItemComparator.passes(input, descriptions);
            case sequence:
                accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> sequences = new ArrayList<String>();
                for (String accession : accessions) {
                    Protein protein = SequenceFactory.getInstance().getProtein(accession);
                    sequences.add(protein.getSequence());
                }
                return filterItemComparator.passes(input, sequences);
            case chromosome:
                accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> chromosomes = new ArrayList<String>();
                for (String accession : accessions) {
                    String geneName = SequenceFactory.getInstance().getHeader(accession).getGeneName();
                    String chromosomeNumber = GeneFactory.getInstance().getChromosomeForGeneName(geneName);
                    chromosomes.add(chromosomeNumber);
                }
                return filterItemComparator.passes(input, chromosomes);
            case gene:
                accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> genes = new ArrayList<String>();
                for (String accession : accessions) {
                    String geneName = SequenceFactory.getInstance().getHeader(accession).getGeneName();
                    genes.add(geneName);
                }
                return filterItemComparator.passes(input, genes);
            case GO:
                return filterItemComparator.passes(input, GOFactory.getInstance().getProteinGoDescriptions(matchKey));
            case expectedCoverage:
                Double coverage = 100 * identificationFeaturesGenerator.getObservableCoverage(matchKey);
                return filterItemComparator.passes(input, coverage.toString());
            case validatedCoverage:
                coverage = 100 * identificationFeaturesGenerator.getValidatedSequenceCoverage(matchKey);
                return filterItemComparator.passes(input, coverage.toString());
            case confidentCoverage:
                HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(matchKey);
                coverage = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return filterItemComparator.passes(input, coverage.toString());
            case spectrumCounting:
                sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(matchKey);
                coverage = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return filterItemComparator.passes(input, coverage.toString());
            case ptm:
                ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
                ArrayList<String> ptms;
                PSPtmScores psPtmScores = new PSPtmScores();
                psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
                if (psPtmScores != null) {
                    ptms = psPtmScores.getScoredPTMs();
                } else {
                    ptms = new ArrayList<String>(0);
                }
                return filterItemComparator.passes(input, ptms);
            case nPeptides:
                proteinMatch = identification.getProteinMatch(matchKey);
                Integer nPeptides = proteinMatch.getPeptideCount();
                return filterItemComparator.passes(input, nPeptides.toString());
            case nValidatedPeptides:
                nPeptides = identificationFeaturesGenerator.getNValidatedPeptides(matchKey);
                return filterItemComparator.passes(input, nPeptides.toString());
            case nConfidentPeptides:
                nPeptides = identificationFeaturesGenerator.getNConfidentPeptides(matchKey);
                return filterItemComparator.passes(input, nPeptides.toString());
            case nPSMs:
                Integer nPsms = identificationFeaturesGenerator.getNSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case nValidatedPSMs:
                nPsms = identificationFeaturesGenerator.getNValidatedSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case nConfidentPSMs:
                nPsms = identificationFeaturesGenerator.getNConfidentSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case confidence:
                PSParameter psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getProteinMatchParameter(matchKey, psParameter);
                Double confidence = psParameter.getProteinConfidence();
                return filterItemComparator.passes(input, confidence.toString());
            case proteinInference:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getProteinMatchParameter(matchKey, psParameter);
                Integer pi = psParameter.getProteinInferenceClass();
                return filterItemComparator.passes(input, pi.toString());
            case validationStatus:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getProteinMatchParameter(matchKey, psParameter);
                Integer validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation.toString());
            case stared:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getProteinMatchParameter(matchKey, psParameter);
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
        return ProteinFilterItem.values();
    }

    @Override
    public FilterItem getFilterItem(String itemName) {
        return ProteinFilterItem.getItem(itemName);
    }

    /**
     * Checks whether it is an old filter using the deprecated code below and
     * converts it to the new structure
     */
    public void backwardCompatibilityCheck() {
        if (accessionRegex != null) {
            setFilterItem(ProteinFilterItem.proteinAccession.name, FilterItemComparator.matches, accessionRegex);
            accessionRegex = null;
        }
        if (proteinCoverage != null) {
            if (proteinConfidenceComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.validatedCoverage.name, FilterItemComparator.lower, proteinCoverage);
            } else if (proteinConfidenceComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.validatedCoverage.name, FilterItemComparator.higher, proteinCoverage);
            } else if (proteinConfidenceComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.validatedCoverage.name, FilterItemComparator.equal, proteinCoverage);
            }
            accessionRegex = null;
        }
        if (spectrumCounting != null) {
            if (spectrumCountingComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.spectrumCounting.name, FilterItemComparator.lower, spectrumCounting);
            } else if (spectrumCountingComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.spectrumCounting.name, FilterItemComparator.higher, spectrumCounting);
            } else if (spectrumCountingComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.spectrumCounting.name, FilterItemComparator.equal, spectrumCounting);
            }
            spectrumCounting = null;
        }
        if (nPeptides != null) {
            if (nPeptidesComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.nPeptides.name, FilterItemComparator.lower, nPeptides);
            } else if (nPeptidesComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.nPeptides.name, FilterItemComparator.higher, nPeptides);
            } else if (nPeptidesComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.nPeptides.name, FilterItemComparator.equal, nPeptides);
            }
            nPeptides = null;
        }
        if (nValidatedPeptides != null) {
            if (nValidatedPeptidesComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.nValidatedPeptides.name, FilterItemComparator.lower, nValidatedPeptides);
            } else if (nValidatedPeptidesComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.nValidatedPeptides.name, FilterItemComparator.higher, nValidatedPeptides);
            } else if (nValidatedPeptidesComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.nValidatedPeptides.name, FilterItemComparator.equal, nValidatedPeptides);
            }
            nValidatedPeptides = null;
        }
        if (nConfidentPeptides != null) {
            if (nConfidentPeptidesComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.nConfidentPeptides.name, FilterItemComparator.lower, nConfidentPeptides);
            } else if (nConfidentPeptidesComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.nConfidentPeptides.name, FilterItemComparator.higher, nConfidentPeptides);
            } else if (nConfidentPeptidesComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.nConfidentPeptides.name, FilterItemComparator.equal, nConfidentPeptides);
            }
            nConfidentPeptides = null;
        }
        if (nSpectra != null) {
            if (nSpectraComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.nPSMs.name, FilterItemComparator.lower, nSpectra);
            } else if (nSpectraComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.nPSMs.name, FilterItemComparator.higher, nSpectra);
            } else if (nSpectraComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.nPSMs.name, FilterItemComparator.equal, nSpectra);
            }
            nSpectra = null;
        }
        if (nValidatedSpectra != null) {
            if (nValidatedSpectraComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.nValidatedPSMs.name, FilterItemComparator.lower, nValidatedSpectra);
            } else if (nValidatedSpectraComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.nValidatedPSMs.name, FilterItemComparator.higher, nValidatedSpectra);
            } else if (nValidatedSpectraComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.nValidatedPSMs.name, FilterItemComparator.equal, nValidatedSpectra);
            }
            nValidatedSpectra = null;
        }
        if (nConfidentSpectra != null) {
            if (nConfidentSpectraComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.nConfidentPSMs.name, FilterItemComparator.lower, nConfidentSpectra);
            } else if (nConfidentSpectraComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.nConfidentPSMs.name, FilterItemComparator.higher, nConfidentSpectra);
            } else if (nConfidentSpectraComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.nConfidentPSMs.name, FilterItemComparator.equal, nConfidentSpectra);
            }
            nConfidentSpectra = null;
        }
        if (proteinConfidence != null) {
            if (proteinConfidenceComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.confidence.name, FilterItemComparator.lower, proteinConfidence);
            } else if (proteinConfidenceComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.confidence.name, FilterItemComparator.higher, proteinConfidence);
            } else if (proteinConfidenceComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.confidence.name, FilterItemComparator.equal, proteinConfidence);
            }
            proteinConfidence = null;
        }
        if (pi != 5) {
            if (piComparison == ComparisonType.BEFORE) {
                setFilterItem(ProteinFilterItem.proteinInference.name, FilterItemComparator.lower, pi);
            } else if (piComparison == ComparisonType.AFTER) {
                setFilterItem(ProteinFilterItem.proteinInference.name, FilterItemComparator.higher, pi);
            } else if (piComparison == ComparisonType.EQUAL) {
                setFilterItem(ProteinFilterItem.proteinInference.name, FilterItemComparator.equal, pi);
            }
        }
    }

    /**
     * Regex in the protein accession.
     *
     * @deprecated use the filter items instead
     */
    private String accessionRegex = null;
    /**
     * Protein coverage limit.
     *
     * @deprecated use the filter items instead
     */
    private Double proteinCoverage = null;
    /**
     * The type of comparison to be used for the protein coverage.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType proteinCoverageComparison = ComparisonType.EQUAL;
    /**
     * Spectrum counting limit.
     *
     * @deprecated use the filter items instead
     */
    private Double spectrumCounting = null;
    /**
     * The type of comparison to be used for the spectrum counting.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType spectrumCountingComparison = ComparisonType.EQUAL;
    /**
     * Number of peptides limit.
     *
     * @deprecated use the filter items instead
     */
    private Integer nPeptides = null;
    /**
     * Number of validated peptides limit.
     *
     * @deprecated use the filter items instead
     */
    private Integer nValidatedPeptides = null;
    /**
     * Number of confident peptides limit.
     *
     * @deprecated use the filter items instead
     */
    private Integer nConfidentPeptides = null;
    /**
     * The type of comparison to be used for the number of peptides.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType nPeptidesComparison = ComparisonType.EQUAL;
    /**
     * The type of comparison to be used for the number of peptides.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType nValidatedPeptidesComparison = ComparisonType.EQUAL;
    /**
     * The type of comparison to be used for the number of peptides.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType nConfidentPeptidesComparison = ComparisonType.EQUAL;
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
    private Double proteinScore = null;
    /**
     * The type of comparison to be used for the protein score.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType proteinScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     *
     * @deprecated use the filter items instead
     */
    private Double proteinConfidence = null;
    /**
     * The type of comparison to be used for the protein confidence.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType proteinConfidenceComparison = ComparisonType.EQUAL;
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
}
