package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.RowFilter.ComparisonType;

/**
 * Peptide Filter.
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
     * A protein regex.
     */
    private String protein = null;
    /**
     * Sequence regex.
     */
    private String sequence = null;
    /**
     * The compiled protein pattern.
     */
    private Pattern proteinPattern = null;
    /**
     * The compiled peptide sequence pattern.
     */
    private Pattern sequencePattern = null;
    /**
     * Number of spectra limit.
     */
    private Integer nSpectra = null;
    /**
     * The type of comparison to be used for the number of spectra.
     */
    private ComparisonType nSpectraComparison = ComparisonType.EQUAL;
    /**
     * Score limit.
     */
    private Double peptideScore = null;
    /**
     * The type of comparison to be used for the peptide score.
     */
    private ComparisonType peptideScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     */
    private Double peptideConfidence = null;
    /**
     * The type of comparison to be used for the peptide confidence.
     */
    private ComparisonType peptideConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The current protein inference filter selection.
     */
    private int pi = 5;
    /**
     * The type of comparison to be used for the PI.
     */
    private ComparisonType piComparison = ComparisonType.EQUAL;
    /**
     * The list of modifications allowed for the peptide.
     */
    private ArrayList<String> modificationStatus;

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param allModifications list of all modifications found in peptides
     */
    public PeptideFilter(String name, ArrayList<String> allModifications) {
        this.name = name;
        this.modificationStatus = allModifications;
        this.filterType = FilterType.PEPTIDE;
    }

    /**
     * Returns the threshold for the peptide confidence.
     *
     * @return the threshold for the peptide confidence
     */
    public Double getPeptideConfidence() {
        return peptideConfidence;
    }

    /**
     * Sets the threshold for the peptide confidence.
     *
     * @param peptideConfidence the threshold for the peptide confidence
     */
    public void setPeptideConfidence(Double peptideConfidence) {
        this.peptideConfidence = peptideConfidence;
    }

    /**
     * Returns the threshold for the number of spectra.
     *
     * @return the threshold for the number of spectra
     */
    public Integer getNSpectra() {
        return nSpectra;
    }

    /**
     * Sets the threshold for the number of spectra.
     *
     * @param nSpectra the threshold for the number of spectra
     */
    public void setNSpectra(Integer nSpectra) {
        this.nSpectra = nSpectra;
    }

    /**
     * Returns the threshold for the peptide score
     *
     * @return the threshold for the peptide score
     */
    public Double getPeptideScore() {
        return peptideScore;
    }

    /**
     * Sets the threshold for the peptide score.
     *
     * @param peptideScore the threshold for the peptide score
     */
    public void setPeptideScore(Double peptideScore) {
        this.peptideScore = peptideScore;
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
    public ComparisonType getPeptideConfidenceComparison() {
        return peptideConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence.
     *
     * @param peptideConfidenceComparison the comparison type used for the
     * confidence
     */
    public void setPeptideConfidenceComparison(ComparisonType peptideConfidenceComparison) {
        this.peptideConfidenceComparison = peptideConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the peptide score.
     *
     * @return the comparison type used for the peptide score
     */
    public ComparisonType getPeptideScoreComparison() {
        return peptideScoreComparison;
    }

    /**
     * Sets the comparison type used for the peptide score.
     *
     * @param peptideScoreComparison the comparison type used for the peptide
     * score
     */
    public void setPeptideScoreComparison(ComparisonType peptideScoreComparison) {
        this.peptideScoreComparison = peptideScoreComparison;
    }

    /**
     * Returns the modifications to retain.
     *
     * @return the modifications to retain
     */
    public ArrayList<String> getModificationStatus() {
        return modificationStatus;
    }

    /**
     * Sets the modifications to retain.
     *
     * @param modificationStatus the modifications to retain
     */
    public void setModificationStatus(ArrayList<String> modificationStatus) {
        this.modificationStatus = modificationStatus;
    }

    /**
     * Returns a regular exception to be searched in protein which contain the
     * peptide sequence.
     *
     * @return a regular exception to be searched in protein which contain the
     * peptide sequence
     */
    public String getProtein() {
        return protein;
    }

    /**
     * Sets a regular exception to be searched in protein which contain the
     * peptide sequence.
     *
     * @param protein a regular exception to be searched in protein which
     * contain the peptide sequence
     */
    public void setProtein(String protein) {
        this.protein = protein;
        this.proteinPattern = Pattern.compile("(.*?)" + protein + "(.*?)");
    }

    /**
     * Returns a regex to be found in the sequence.
     *
     * @return a regex to be found in the sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * Sets a regex to be found in the sequence.
     *
     * @param sequence a regex to be found in the sequence
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
        this.sequencePattern = Pattern.compile("(.*?)" + sequence + "(.*?)");
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

    /**
     * Returns the compiled protein pattern. Null if no pattern is set.
     * 
     * @return the compiled protein pattern
     */
    public Pattern getProteinPattern() {
        if (protein != null) {
            if (proteinPattern != null) {
                return proteinPattern;
            }
        }
        return null;
    }

    /**
     * Returns the compiled peptide sequence pattern. Null if no pattern is set.
     * 
     * @return the compiled peptide sequence pattern
     */
    public Pattern getSequencePattern() {
        if (sequence != null) {
            if (sequencePattern != null) {
                return sequencePattern;
            }
        }
        return null;
    }

    /**
     * Tests whether a peptide match is validated by this filter.
     *
     * @param peptideKey the key of the peptide match
     * @param identification the identification where to get the information from
     * 
     * @return a boolean indicating whether a peptide match is validated by a
     * given filter
     * 
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public boolean isValidated(String peptideKey, Identification identification) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        return isValidated(peptideKey, this, identification);
    }

    /**
     * Tests whether a peptide match is validated by a given filter.
     *
     * @param peptideKey the key of the peptide match
     * @param peptideFilter the filter
     * @param identification the identification where to get the information from
     * 
     * @return a boolean indicating whether a peptide match is validated by a
     * given filter
     * 
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public static boolean isValidated(String peptideKey, PeptideFilter peptideFilter, Identification identification) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        
            if (peptideFilter.getExceptions().contains(peptideKey)) {
                return false;
            }

            if (peptideFilter.getManualValidation().size() > 0) {
                if (peptideFilter.getManualValidation().contains(peptideKey)) {
                    return true;
                } else {
                    return false;
                }
            }

            PSParameter psParameter = new PSParameter();
            boolean found = false;

            for (String ptm : peptideFilter.getModificationStatus()) {
                if (ptm.equals(PtmPanel.NO_MODIFICATION)) {
                    if (!Peptide.isModified(peptideKey)) {
                        found = true;
                        break;
                    }
                } else {
                    if (Peptide.isModified(peptideKey, ptm)) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                return false;
            }

            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (peptideFilter.getPi() != 5) {
                if (peptideFilter.getPiComparison() == ComparisonType.NOT_EQUAL
                        && psParameter.getProteinInferenceClass() == peptideFilter.getPi()) {
                    return false;
                } else if (peptideFilter.getPiComparison() == ComparisonType.EQUAL
                        && psParameter.getProteinInferenceClass() != peptideFilter.getPi()) {
                    return false;
                }
            }

            if (peptideFilter.getPeptideScore() != null) {
                if (peptideFilter.getPeptideScoreComparison() == ComparisonType.AFTER) {
                    if (psParameter.getPeptideScore() <= peptideFilter.getPeptideScore()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideScoreComparison() == ComparisonType.BEFORE) {
                    if (psParameter.getPeptideScore() >= peptideFilter.getPeptideScore()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideScoreComparison() == ComparisonType.EQUAL) {
                    if (psParameter.getPeptideScore() != peptideFilter.getPeptideScore()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideScoreComparison() == ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPeptideScore() == peptideFilter.getPeptideScore()) {
                        return false;
                    }
                }
            }

            if (peptideFilter.getPeptideConfidence() != null) {
                if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.AFTER) {
                    if (psParameter.getPeptideConfidence() <= peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.BEFORE) {
                    if (psParameter.getPeptideConfidence() >= peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.EQUAL) {
                    if (psParameter.getPeptideConfidence() != peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPeptideConfidence() == peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                }
            }

            if (peptideFilter.getNSpectra() != null
                    || peptideFilter.getProtein() != null) {
                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                if (peptideFilter.getNSpectra() != null) {
                    if (peptideFilter.getnSpectraComparison() == ComparisonType.AFTER) {
                        if (peptideMatch.getSpectrumCount() <= peptideFilter.getNSpectra()) {
                            return false;
                        }
                    } else if (peptideFilter.getnSpectraComparison() == ComparisonType.BEFORE) {
                        if (peptideMatch.getSpectrumCount() >= peptideFilter.getNSpectra()) {
                            return false;
                        }
                    } else if (peptideFilter.getnSpectraComparison() == ComparisonType.EQUAL) {
                        if (peptideMatch.getSpectrumCount() != peptideFilter.getNSpectra()) {
                            return false;
                        }
                    } else if (peptideFilter.getnSpectraComparison() == ComparisonType.NOT_EQUAL) {
                        if (peptideMatch.getSpectrumCount() != peptideFilter.getNSpectra()) {
                            return false;
                        }
                    }
                }

                if (peptideFilter.getProtein() != null) {
                    found = false;
                    for (String accession : peptideMatch.getTheoreticPeptide().getParentProteinsNoRemapping()) {
                        if (accession.split(peptideFilter.getProtein()).length > 1) {
                            found = true;
                            break;
                        }
                        if (sequenceFactory.getHeader(accession).getSimpleProteinDescription() != null
                                && sequenceFactory.getHeader(accession).getSimpleProteinDescription().split(peptideFilter.getProtein()).length > 1) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }

            // sequence pattern
            if (peptideFilter.getSequence() != null && peptideFilter.getSequence().trim().length() > 0) {
                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                String peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();
                Matcher m;
                if (peptideFilter.getSequencePattern() != null) {
                    m = peptideFilter.getSequencePattern().matcher(peptideSequence);
                } else {
                    Pattern p = Pattern.compile("(.*?)" + peptideFilter.getSequence() + "(.*?)");
                    m = p.matcher(peptideSequence);
                }
                if (!m.matches()) {
                    return false;
                }
            }

            // protein pattern
            if (peptideFilter.getProtein() != null && peptideFilter.getProtein().trim().length() > 0) {
                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                String accessions = "";
                for (String accession : peptideMatch.getTheoreticPeptide().getParentProteinsNoRemapping()) {
                    accessions += accession + " ";
                }
                Matcher m;
                if (peptideFilter.getProteinPattern() != null) {
                    m = peptideFilter.getProteinPattern().matcher(accessions);
                } else {
                    Pattern p = Pattern.compile("(.*?)" + peptideFilter.getProtein() + "(.*?)");
                    m = p.matcher(accessions);
                }
                if (!m.matches()) {
                    return false;
                }
            }

            return true;
    }
}
