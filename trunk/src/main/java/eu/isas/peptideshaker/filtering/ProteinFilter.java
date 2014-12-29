package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import javax.swing.RowFilter.ComparisonType;
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
     * Regex in the protein accession.
     */
    private String accessionRegex = null;
    /**
     * Protein coverage limit.
     */
    private Double proteinCoverage = null;
    /**
     * The type of comparison to be used for the protein coverage.
     */
    private ComparisonType proteinCoverageComparison = ComparisonType.EQUAL;
    /**
     * Spectrum counting limit.
     */
    private Double spectrumCounting = null;
    /**
     * The type of comparison to be used for the spectrum counting.
     */
    private ComparisonType spectrumCountingComparison = ComparisonType.EQUAL;
    /**
     * Number of peptides limit.
     */
    private Integer nPeptides = null;
    /**
     * Number of validated peptides limit.
     */
    private Integer nValidatedPeptides = null;
    /**
     * Number of confident peptides limit.
     */
    private Integer nConfidentPeptides = null;
    /**
     * The type of comparison to be used for the number of peptides.
     */
    private ComparisonType nPeptidesComparison = ComparisonType.EQUAL;
    /**
     * The type of comparison to be used for the number of peptides.
     */
    private ComparisonType nValidatedPeptidesComparison = ComparisonType.EQUAL;
    /**
     * The type of comparison to be used for the number of peptides.
     */
    private ComparisonType nConfidentPeptidesComparison = ComparisonType.EQUAL;
    /**
     * Number of spectra limit.
     */
    private Integer nSpectra = null;
    /**
     * The type of comparison to be used for the number of spectra.
     */
    private ComparisonType nSpectraComparison = ComparisonType.EQUAL;
    /**
     * Number of validated spectra limit.
     */
    private Integer nValidatedSpectra = null;
    /**
     * The type of comparison to be used for the number of validated spectra.
     */
    private ComparisonType nValidatedSpectraComparison = ComparisonType.EQUAL;
    /**
     * Number of confident spectra limit.
     */
    private Integer nConfidentSpectra = null;
    /**
     * The type of comparison to be used for the number of confident spectra.
     */
    private ComparisonType nConfidentSpectraComparison = ComparisonType.EQUAL;
    /**
     * Score limit.
     */
    private Double proteinScore = null;
    /**
     * The type of comparison to be used for the protein score.
     */
    private ComparisonType proteinScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     */
    private Double proteinConfidence = null;
    /**
     * The type of comparison to be used for the protein confidence.
     */
    private ComparisonType proteinConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The current protein inference filter selection.
     */
    private int pi = 5;
    /**
     * The type of comparison to be used for the PI.
     */
    private ComparisonType piComparison = ComparisonType.EQUAL;

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
     * Returns the threshold for the number of peptides.
     *
     * @return the threshold for the number of peptides
     */
    public Integer getnPeptides() {
        return nPeptides;
    }

    /**
     * Returns the threshold for the number of peptides.
     *
     * @param nPeptides the threshold for the number of peptides
     */
    public void setnPeptides(int nPeptides) {
        this.nPeptides = nPeptides;
    }

    /**
     * Returns the threshold for the number of validated peptides.
     *
     * @return the threshold for the number of validated peptides
     */
    public Integer getnValidatedPeptides() {
        return nValidatedPeptides;
    }

    /**
     * Returns the threshold for the number of validated peptides.
     *
     * @param nValidatedPeptides the threshold for the number of validated peptides
     */
    public void setnValidatedPeptides(int nValidatedPeptides) {
        this.nValidatedPeptides = nValidatedPeptides;
    }

    /**
     * Returns the threshold for the number of confident peptides.
     *
     * @return the threshold for the number of confident peptides
     */
    public Integer getnConfidentPeptides() {
        return nConfidentPeptides;
    }

    /**
     * Returns the threshold for the number of confident peptides.
     *
     * @param nConfidentPeptides the threshold for the number of confident peptides
     */
    public void setnConfidentPeptides(int nConfidentPeptides) {
        this.nConfidentPeptides = nConfidentPeptides;
    }

    /**
     * Returns the threshold for the protein confidence.
     *
     * @return the threshold for the protein confidence
     */
    public Double getProteinConfidence() {
        return proteinConfidence;
    }

    /**
     * Sets the threshold for the protein confidence.
     *
     * @param proteinConfidence the threshold for the protein confidence
     */
    public void setProteinConfidence(Double proteinConfidence) {
        this.proteinConfidence = proteinConfidence;
    }

    /**
     * Returns the threshold for the protein coverage.
     *
     * @return the threshold for the protein coverage
     */
    public Double getProteinCoverage() {
        return proteinCoverage;
    }

    /**
     * sets the threshold for the protein coverage.
     *
     * @param proteinCoverage the threshold for the protein coverage
     */
    public void setProteinCoverage(Double proteinCoverage) {
        this.proteinCoverage = proteinCoverage;
    }

    /**
     * Returns the threshold for the number of spectra.
     *
     * @return the threshold for the number of spectra
     */
    public Integer getProteinNSpectra() {
        return nSpectra;
    }

    /**
     * Sets the threshold for the number of spectra.
     *
     * @param nSpectra the threshold for the number of spectra
     */
    public void setProteinNSpectra(Integer nSpectra) {
        this.nSpectra = nSpectra;
    }

    /**
     * Returns the threshold for the number of validated spectra.
     *
     * @return the threshold for the number of validated spectra
     */
    public Integer getProteinNValidatedSpectra() {
        return nValidatedSpectra;
    }

    /**
     * Sets the threshold for the number of validated spectra.
     *
     * @param nValidatedSpectra the threshold for the number of validated spectra
     */
    public void setProteinNValidatedSpectra(Integer nValidatedSpectra) {
        this.nValidatedSpectra = nValidatedSpectra;
    }

    /**
     * Returns the threshold for the number of confident spectra.
     *
     * @return the threshold for the number of confident spectra
     */
    public Integer getProteinNConfidentSpectra() {
        return nConfidentSpectra;
    }

    /**
     * Sets the threshold for the number of confident spectra.
     *
     * @param nConfidentSpectra the threshold for the number of confident spectra
     */
    public void setProteinNConfidentSpectra(Integer nConfidentSpectra) {
        this.nConfidentSpectra = nConfidentSpectra;
    }

    /**
     * Returns the threshold for the protein score.
     *
     * @return the threshold for the protein score
     */
    public Double getProteinScore() {
        return proteinScore;
    }

    /**
     * Sets the threshold for the protein score.
     *
     * @param proteinScore the threshold for the protein score
     */
    public void setProteinScore(Double proteinScore) {
        this.proteinScore = proteinScore;
    }

    /**
     * Returns the threshold for the spectrum counting.
     *
     * @return the threshold for the spectrum counting
     */
    public Double getSpectrumCounting() {
        return spectrumCounting;
    }

    /**
     * Sets the threshold for the spectrum counting.
     *
     * @param spectrumCounting the threshold for the spectrum counting
     */
    public void setSpectrumCounting(Double spectrumCounting) {
        this.spectrumCounting = spectrumCounting;
    }

    /**
     * Returns the comparison type used for the number of peptides.
     *
     * @return the comparison type used for the number of peptides
     */
    public ComparisonType getnPeptidesComparison() {
        return nPeptidesComparison;
    }

    /**
     * Sets the comparison type used for the number of peptides.
     *
     * @param nPeptidesComparison the comparison type used for the number of
     * peptides
     */
    public void setnPeptidesComparison(ComparisonType nPeptidesComparison) {
        this.nPeptidesComparison = nPeptidesComparison;
    }

    /**
     * Returns the comparison type used for the number of spectra.
     *
     * @return the comparison type used for the number of spectra
     */
    public ComparisonType getnSpectraComparison() {
        return nSpectraComparison;
    }

    /**
     * Sets the comparison type used for the number of spectra.
     *
     * @param nSpectraComparison the comparison type used for the number of
     * spectra
     */
    public void setnSpectraComparison(ComparisonType nSpectraComparison) {
        this.nSpectraComparison = nSpectraComparison;
    }

    /**
     * Returns the comparison type used for the number of validated spectra.
     *
     * @return the comparison type used for the number of validated spectra
     */
    public ComparisonType getnValidatedSpectraComparison() {
        return nValidatedSpectraComparison;
    }

    /**
     * Sets the comparison type used for the number of validated spectra.
     *
     * @param nValidatedSpectraComparison the comparison type used for the number of validated
     * spectra
     */
    public void setnValidatedSpectraComparison(ComparisonType nValidatedSpectraComparison) {
        this.nValidatedSpectraComparison = nValidatedSpectraComparison;
    }

    /**
     * Returns the comparison type used for the number of confident spectra.
     *
     * @return the comparison type used for the number of confident spectra
     */
    public ComparisonType getnConfidentSpectraComparison() {
        return nConfidentSpectraComparison;
    }

    /**
     * Sets the comparison type used for the number of confident spectra.
     *
     * @param nConfidentSpectraComparison the comparison type used for the number of confident
     * spectra
     */
    public void setnConfidentSpectraComparison(ComparisonType nConfidentSpectraComparison) {
        this.nConfidentSpectraComparison = nConfidentSpectraComparison;
    }

    /**
     * Returns the comparison type used for the number of validated peptides.
     *
     * @return the comparison type used for the number of validated peptides
     */
    public ComparisonType getnValidatedPeptidesComparison() {
        return nValidatedPeptidesComparison;
    }

    /**
     * Sets the comparison type used for the number of validated peptides.
     *
     * @param nValidatedPeptidesComparison the comparison type used for the number of validated
     * peptides
     */
    public void setnValidatedPeptidesComparison(ComparisonType nValidatedPeptidesComparison) {
        this.nValidatedPeptidesComparison = nValidatedPeptidesComparison;
    }

    /**
     * Returns the comparison type used for the number of confident peptides.
     *
     * @return the comparison type used for the number of confident peptides
     */
    public ComparisonType getnConfidentPeptidesComparison() {
        return nConfidentPeptidesComparison;
    }

    /**
     * Sets the comparison type used for the number of confident peptides.
     *
     * @param nConfidentPeptidesComparison the comparison type used for the number of confident
     * peptides
     */
    public void setnConfidentPeptidesComparison(ComparisonType nConfidentPeptidesComparison) {
        this.nConfidentPeptidesComparison = nConfidentPeptidesComparison;
    }

    /**
     * Returns the protein inference desired.
     *
     * @return the protein inference desired
     */
    public int getPi() {
        return pi;
    }

    /**
     * Sets the protein inference desired.
     *
     * @param pi the protein inference desired
     */
    public void setPi(int pi) {
        this.pi = pi;
    }

    /**
     * Returns the comparison type used for the confidence.
     *
     * @return the comparison type used for the confidence
     */
    public ComparisonType getProteinConfidenceComparison() {
        return proteinConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence.
     *
     * @param proteinConfidenceComparison the comparison type used for the
     * confidence
     */
    public void setProteinConfidenceComparison(ComparisonType proteinConfidenceComparison) {
        this.proteinConfidenceComparison = proteinConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the protein coverage.
     *
     * @return the comparison type used for the protein coverage
     */
    public ComparisonType getProteinCoverageComparison() {
        return proteinCoverageComparison;
    }

    /**
     * Sets the comparison type used for the protein coverage.
     *
     * @param proteinCoverageComparison the comparison type used for the protein
     * coverage
     */
    public void setProteinCoverageComparison(ComparisonType proteinCoverageComparison) {
        this.proteinCoverageComparison = proteinCoverageComparison;
    }

    /**
     * Returns the comparison type used for the protein score.
     *
     * @return the comparison type used for the protein score
     */
    public ComparisonType getProteinScoreComparison() {
        return proteinScoreComparison;
    }

    /**
     * Sets the comparison type used for the protein score.
     *
     * @param proteinScoreComparison the comparison type used for the protein
     * score
     */
    public void setProteinScoreComparison(ComparisonType proteinScoreComparison) {
        this.proteinScoreComparison = proteinScoreComparison;
    }

    /**
     * Returns the comparison type used for the spectrum counting.
     *
     * @return the comparison type used for the spectrum counting
     */
    public ComparisonType getSpectrumCountingComparison() {
        return spectrumCountingComparison;
    }

    /**
     * Sets the comparison type used for the spectrum counting.
     *
     * @param spectrumCountingComparison the comparison type used for the
     * spectrum counting
     */
    public void setSpectrumCountingComparison(ComparisonType spectrumCountingComparison) {
        this.spectrumCountingComparison = spectrumCountingComparison;
    }

    /**
     * Returns the regex contained in the accession.
     *
     * @return the regex contained in the accession
     */
    public String getIdentifierRegex() {
        return accessionRegex;
    }

    /**
     * Sets the regex contained in the accession.
     *
     * @param accessionRegex the regex contained in the accession
     */
    public void setIdentifierRegex(String accessionRegex) {
        this.accessionRegex = accessionRegex;
    }

    /**
     * Returns the comparison type to use for the PI.
     *
     * @return the comparison type to use for the PI
     */
    public ComparisonType getPiComparison() {
        return piComparison;
    }

    /**
     * Sets the comparison type to use for the PI.
     *
     * @param piComparison the comparison type to use for the PI
     */
    public void setPiComparison(ComparisonType piComparison) {
        this.piComparison = piComparison;
    }

    @Override
    public boolean isValidated(String proteinKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        return isValidated(proteinKey, this, identification, identificationFeaturesGenerator, identificationParameters);
    }

    /**
     * Tests whether a protein match is validated by a given filter.
     *
     * @param proteinMatchKey the key of the protein match
     * @param proteinFilter the filter
     * @param identification the identification where to get the information
     * from
     * @param identificationFeaturesGenerator the identification features
     * generator providing identification features
     * @param identificationParameters the identification parameters
     *
     * @return a boolean indicating whether a protein match is validated by a
     * given filter
     * 
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public static boolean isValidated(String proteinMatchKey, ProteinFilter proteinFilter, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, 
            IdentificationParameters identificationParameters) throws IOException, InterruptedException, ClassNotFoundException, SQLException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();

        if (proteinFilter.getExceptions().contains(proteinMatchKey)) {
            return false;
        }

        if (proteinFilter.getManualValidation().size() > 0) {
            return proteinFilter.getManualValidation().contains(proteinMatchKey);
        }

        if (proteinFilter.getIdentifierRegex() != null) {
            String test = "test_" + proteinMatchKey + "_test";
            if (test.split(proteinFilter.getIdentifierRegex()).length == 1) {
                boolean found = false;
                for (String accession : ProteinMatch.getAccessions(proteinMatchKey)) {
                    test = "test_" + sequenceFactory.getHeader(accession).getSimpleProteinDescription().toLowerCase() + "_test";
                    if (test.split(proteinFilter.getIdentifierRegex().toLowerCase()).length > 1) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, psParameter);

        if (proteinFilter.getPi() != 5) {
            if (proteinFilter.getPiComparison() == ComparisonType.NOT_EQUAL
                    && psParameter.getProteinInferenceClass() == proteinFilter.getPi()) {
                return false;
            } else if (proteinFilter.getPiComparison() == ComparisonType.EQUAL
                    && psParameter.getProteinInferenceClass() != proteinFilter.getPi()) {
                return false;
            }
        }

        if (proteinFilter.getProteinScore() != null) {
            if (proteinFilter.getProteinScoreComparison() == ComparisonType.AFTER) {
                if (psParameter.getProteinScore() <= proteinFilter.getProteinScore()) {
                    return false;
                }
            } else if (proteinFilter.getProteinScoreComparison() == ComparisonType.BEFORE) {
                if (psParameter.getProteinScore() >= proteinFilter.getProteinScore()) {
                    return false;
                }
            } else if (proteinFilter.getProteinScoreComparison() == ComparisonType.EQUAL) {
                if (psParameter.getProteinScore() != proteinFilter.getProteinScore()) {
                    return false;
                }
            } else if (proteinFilter.getProteinScoreComparison() == ComparisonType.NOT_EQUAL) {
                if (psParameter.getProteinScore() == proteinFilter.getProteinScore()) {
                    return false;
                }
            }
        }

        if (proteinFilter.getProteinConfidence() != null) {
            if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.AFTER) {
                if (psParameter.getProteinConfidence() <= proteinFilter.getProteinConfidence()) {
                    return false;
                }
            } else if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.BEFORE) {
                if (psParameter.getProteinConfidence() >= proteinFilter.getProteinConfidence()) {
                    return false;
                }
            } else if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.EQUAL) {
                if (psParameter.getProteinConfidence() != proteinFilter.getProteinConfidence()) {
                    return false;
                }
            } else if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.NOT_EQUAL) {
                if (psParameter.getProteinConfidence() == proteinFilter.getProteinConfidence()) {
                    return false;
                }
            }
        }

        if (proteinFilter.getnPeptides() != null
                || proteinFilter.getnValidatedPeptides()!= null
                || proteinFilter.getnConfidentPeptides()!= null
                || proteinFilter.getProteinNSpectra() != null
                || proteinFilter.getProteinNValidatedSpectra() != null
                || proteinFilter.getProteinNConfidentSpectra() != null
                || proteinFilter.getProteinCoverage() != null
                || proteinFilter.getSpectrumCounting() != null) {
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

            if (proteinFilter.getnPeptides() != null) {
                if (proteinFilter.getnPeptidesComparison() == ComparisonType.AFTER) {
                    if (proteinMatch.getPeptideMatchesKeys().size() <= proteinFilter.getnPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnPeptidesComparison() == ComparisonType.BEFORE) {
                    if (proteinMatch.getPeptideMatchesKeys().size() >= proteinFilter.getnPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnPeptidesComparison() == ComparisonType.EQUAL) {
                    if (proteinMatch.getPeptideMatchesKeys().size() != proteinFilter.getnPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnPeptidesComparison() == ComparisonType.NOT_EQUAL) {
                    if (proteinMatch.getPeptideMatchesKeys().size() == proteinFilter.getnPeptides()) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getnValidatedPeptides() != null) {
                int nValidatedPeptides = identificationFeaturesGenerator.getNValidatedPeptides(proteinMatchKey);
                if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.AFTER) {
                    if (nValidatedPeptides <= proteinFilter.getnValidatedPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.BEFORE) {
                    if (nValidatedPeptides >= proteinFilter.getnValidatedPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.EQUAL) {
                    if (nValidatedPeptides != proteinFilter.getnValidatedPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.NOT_EQUAL) {
                    if (nValidatedPeptides == proteinFilter.getnValidatedPeptides()) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getnConfidentPeptides() != null) {
                int nConfidentPeptides = identificationFeaturesGenerator.getNConfidentPeptides(proteinMatchKey);
                if (proteinFilter.getnConfidentPeptidesComparison() == ComparisonType.AFTER) {
                    if (nConfidentPeptides <= proteinFilter.getnConfidentPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.BEFORE) {
                    if (nConfidentPeptides >= proteinFilter.getnConfidentPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.EQUAL) {
                    if (nConfidentPeptides != proteinFilter.getnConfidentPeptides()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedPeptidesComparison() == ComparisonType.NOT_EQUAL) {
                    if (nConfidentPeptides == proteinFilter.getnConfidentPeptides()) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getProteinNSpectra() != null) {
                if (proteinFilter.getnSpectraComparison() == ComparisonType.AFTER) {
                    if (identificationFeaturesGenerator.getNSpectra(proteinMatchKey) <= proteinFilter.getProteinNSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnSpectraComparison() == ComparisonType.BEFORE) {
                    if (identificationFeaturesGenerator.getNSpectra(proteinMatchKey) >= proteinFilter.getProteinNSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnSpectraComparison() == ComparisonType.EQUAL) {
                    if (!identificationFeaturesGenerator.getNSpectra(proteinMatchKey).equals(proteinFilter.getProteinNSpectra())) {
                        return false;
                    }
                } else if (proteinFilter.getnSpectraComparison() == ComparisonType.NOT_EQUAL) {
                    if (!identificationFeaturesGenerator.getNSpectra(proteinMatchKey).equals(proteinFilter.getProteinNSpectra())) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getProteinNValidatedSpectra()!= null) {
                int nValidatedSpectra = identificationFeaturesGenerator.getNValidatedSpectra(proteinMatchKey);
                if (proteinFilter.getnValidatedSpectraComparison()== ComparisonType.AFTER) {
                    if (nValidatedSpectra <= proteinFilter.getProteinNValidatedSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedSpectraComparison() == ComparisonType.BEFORE) {
                    if (nValidatedSpectra >= proteinFilter.getProteinNValidatedSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedSpectraComparison() == ComparisonType.EQUAL) {
                    if (nValidatedSpectra != proteinFilter.getProteinNValidatedSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnValidatedSpectraComparison() == ComparisonType.NOT_EQUAL) {
                    if (nValidatedSpectra == proteinFilter.getProteinNValidatedSpectra()) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getProteinNConfidentSpectra()!= null) {
                int nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectra(proteinMatchKey);
                if (proteinFilter.getnConfidentSpectraComparison()== ComparisonType.AFTER) {
                    if (nConfidentSpectra <= proteinFilter.getProteinNConfidentSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnConfidentSpectraComparison() == ComparisonType.BEFORE) {
                    if (nConfidentSpectra >= proteinFilter.getProteinNConfidentSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnConfidentSpectraComparison() == ComparisonType.EQUAL) {
                    if (nConfidentSpectra != proteinFilter.getProteinNConfidentSpectra()) {
                        return false;
                    }
                } else if (proteinFilter.getnConfidentSpectraComparison() == ComparisonType.NOT_EQUAL) {
                    if (nConfidentSpectra == proteinFilter.getProteinNConfidentSpectra()) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getProteinCoverage() != null) {
                        HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinMatchKey);
                        Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                        Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                        double validatedCoverage = sequenceCoverageConfident + sequenceCoverageDoubtful;
                if (proteinFilter.getProteinCoverageComparison() == ComparisonType.AFTER) {
                    if (validatedCoverage <= proteinFilter.getProteinCoverage()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinCoverageComparison() == ComparisonType.BEFORE) {
                    if (validatedCoverage >= proteinFilter.getProteinCoverage()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinCoverageComparison() == ComparisonType.EQUAL) {
                    if (validatedCoverage != proteinFilter.getProteinCoverage()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinCoverageComparison() == ComparisonType.NOT_EQUAL) {
                    if (validatedCoverage == proteinFilter.getProteinCoverage()) {
                        return false;
                    }
                }
            }

            if (proteinFilter.getSpectrumCounting() != null) {
                double spectrumCounting = identificationFeaturesGenerator.getSpectrumCounting(proteinMatchKey);
                if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.AFTER) {
                    if (spectrumCounting <= proteinFilter.getSpectrumCounting()) {
                        return false;
                    }
                } else if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.BEFORE) {
                    if (spectrumCounting >= proteinFilter.getSpectrumCounting()) {
                        return false;
                    }
                } else if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.EQUAL) {
                    if (spectrumCounting != proteinFilter.getSpectrumCounting()) {
                        return false;
                    }
                } else if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.NOT_EQUAL) {
                    if (spectrumCounting == proteinFilter.getSpectrumCounting()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
