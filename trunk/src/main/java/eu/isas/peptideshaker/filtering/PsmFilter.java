package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * PSM filter.
 *
 * @author Marc Vaudel
 */
public class PsmFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 2930349531911042645L;
    /**
     * The charges allowed.
     */
    private ArrayList<Integer> charges = null;
    /**
     * the precursor m/z.
     */
    private Double precursorMz = null;
    /**
     * The type of comparison to be used for the precursor m/z.
     */
    private ComparisonType precursorMzComparison = ComparisonType.EQUAL;
    /**
     * The precursor retention time.
     */
    private Double precursorRT = null;
    /**
     * The type of comparison to be used for the precursor retention time.
     */
    private ComparisonType precursorRTComparison = ComparisonType.EQUAL;
    /**
     * The precursor m/z error.
     */
    private Double precursorMzError = null;
    /**
     * The type of comparison to be used for the precursor m/z error.
     */
    private ComparisonType precursorMzErrorComparison = ComparisonType.EQUAL;
    /**
     * Score limit.
     */
    private Double psmScore = null;
    /**
     * The type of comparison to be used for the psm score.
     */
    private ComparisonType psmScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     */
    private Double psmConfidence = null;
    /**
     * The type of comparison to be used for the psm confidence.
     */
    private ComparisonType psmConfidenceComparison = ComparisonType.EQUAL;
    /**
     * List of spectrum files names retained.
     */
    private ArrayList<String> fileName = null;

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param charges list of allowed charges
     * @param files list of allowed files
     */
    public PsmFilter(String name, ArrayList<Integer> charges, ArrayList<String> files) {
        this.name = name;
        this.filterType = FilterType.PSM;
        this.charges = charges;
        this.fileName = files;
    }

    /**
     * Returns the allowed charges.
     *
     * @return the allowed charges
     */
    public ArrayList<Integer> getCharges() {
        return charges;
    }

    /**
     * Sets the allowed charges.
     *
     * @param charges the allowed charges
     */
    public void setCharges(ArrayList<Integer> charges) {
        this.charges = charges;
    }

    /**
     * Returns the precursor m/z.
     *
     * @return the precursor m/z
     */
    public Double getPrecursorMz() {
        return precursorMz;
    }

    /**
     * Sets the precursor m/z.
     *
     * @param precursorMz the precursor m/z
     */
    public void setPrecursorMz(Double precursorMz) {
        this.precursorMz = precursorMz;
    }

    /**
     * Returns the precursor m/z error.
     *
     * @return the precursor m/z error
     */
    public Double getPrecursorMzError() {
        return precursorMzError;
    }

    /**
     * Sets the precursor m/z error.
     *
     * @param precursorMzError the precursor m/z error
     */
    public void setPrecursorMzError(Double precursorMzError) {
        this.precursorMzError = precursorMzError;
    }

    /**
     * Returns the precursor retention time.
     *
     * @return the precursor retention time
     */
    public Double getPrecursorRT() {
        return precursorRT;
    }

    /**
     * Sets the precursor retention time.
     *
     * @param precursorRT the precursor retention time
     */
    public void setPrecursorRT(Double precursorRT) {
        this.precursorRT = precursorRT;
    }

    /**
     * Returns the comparison type used for the confidence.
     *
     * @return the comparison type used for the confidence
     */
    public ComparisonType getPsmConfidenceComparison() {
        return psmConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence.
     *
     * @param psmConfidenceComparison the comparison type used for the
     * confidence
     */
    public void setPsmConfidenceComparison(ComparisonType psmConfidenceComparison) {
        this.psmConfidenceComparison = psmConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the psm score.
     *
     * @return the comparison type used for the psm score
     */
    public ComparisonType getPsmScoreComparison() {
        return psmScoreComparison;
    }

    /**
     * Sets the comparison type used for the psm score.
     *
     * @param psmScoreComparison the comparison type used for the psm score
     */
    public void setPsmScoreComparison(ComparisonType psmScoreComparison) {
        this.psmScoreComparison = psmScoreComparison;
    }

    /**
     * Returns the threshold for the psm score.
     *
     * @return the threshold for the psm score
     */
    public Double getPsmScore() {
        return psmScore;
    }

    /**
     * Sets the threshold for the psm score.
     *
     * @param psmScore the threshold for the psm score
     */
    public void setPsmScore(Double psmScore) {
        this.psmScore = psmScore;
    }

    /**
     * Returns the threshold for the psm confidence.
     *
     * @return the threshold for the psm confidence
     */
    public Double getPsmConfidence() {
        return psmConfidence;
    }

    /**
     * Sets the threshold for the psm confidence.
     *
     * @param psmConfidence the threshold for the psm confidence
     */
    public void setPsmConfidence(Double psmConfidence) {
        this.psmConfidence = psmConfidence;
    }

    /**
     * Returns the comparison type used for the precursor m/z comparison.
     *
     * @return the comparison type used for the precursor m/z comparison
     */
    public ComparisonType getPrecursorMzComparison() {
        return precursorMzComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z comparison.
     *
     * @param precursorMzComparison the comparison type used for the precursor
     * m/z comparison
     */
    public void setPrecursorMzComparison(ComparisonType precursorMzComparison) {
        this.precursorMzComparison = precursorMzComparison;
    }

    /**
     * Returns the comparison type used for the precursor m/z error comparison.
     *
     * @return the comparison type used for the precursor m/z error comparison
     */
    public ComparisonType getPrecursorMzErrorComparison() {
        return precursorMzErrorComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z error comparison.
     *
     * @param precursorMzErrorComparison the comparison type used for the
     * precursor m/z error comparison
     */
    public void setPrecursorMzErrorComparison(ComparisonType precursorMzErrorComparison) {
        this.precursorMzErrorComparison = precursorMzErrorComparison;
    }

    /**
     * Returns the comparison type used for the precursor RT comparison.
     *
     * @return the comparison type used for the precursor RT comparison
     */
    public ComparisonType getPrecursorRTComparison() {
        return precursorRTComparison;
    }

    /**
     * Sets the comparison type used for the precursor RT comparison.
     *
     * @param precursorRTComparison the comparison type used for the precursor
     * RT comparison
     */
    public void setPrecursorRTComparison(ComparisonType precursorRTComparison) {
        this.precursorRTComparison = precursorRTComparison;
    }

    /**
     * Returns the list of spectrum files containing the desired spectra.
     *
     * @return the list of spectrum files containing the desired spectra
     */
    public ArrayList<String> getFileNames() {
        return fileName;
    }

    /**
     * Sets the list of spectrum files containing the desired spectra.
     *
     * @param filesNames the list of spectrum files containing the desired
     * spectra
     */
    public void setFileNames(ArrayList<String> filesNames) {
        this.fileName = filesNames;
    }

    /**
     * Tests whether a spectrum match is validated by this filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param identification the identification object to get the information
     * from
     * @param searchParameters the identification parameters
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public boolean isValidated(String spectrumKey, Identification identification, SearchParameters searchParameters) 
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, this, identification, searchParameters);
    }

    /**
     * Tests whether a spectrum match is validated by a given filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param psmFilter the filter
     * @param identification the identification object to get the information
     * from
     * @param searchParameters the identification parameters
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public static boolean isValidated(String spectrumKey, PsmFilter psmFilter, Identification identification, SearchParameters searchParameters) 
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (psmFilter.getExceptions().contains(spectrumKey)) {
            return false;
        }
        if (psmFilter.getManualValidation().size() > 0) {
            return psmFilter.getManualValidation().contains(spectrumKey);
        }

        PSParameter psParameter = new PSParameter();

        if (psmFilter.getPsmScore() != null
                || psmFilter.getPsmConfidence() != null) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

            if (psmFilter.getPsmScore() != null) {
                if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.AFTER) {
                    if (psParameter.getPsmScore() <= psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (psParameter.getPsmScore() >= psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (psParameter.getPsmScore() != psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPsmScore() == psmFilter.getPsmScore()) {
                        return false;
                    }
                }
            }

            if (psmFilter.getPsmConfidence() != null) {
                if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.AFTER) {
                    if (psParameter.getPsmConfidence() <= psmFilter.getPsmConfidence()) {
                        return false;
                    }
                } else if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (psParameter.getPsmConfidence() >= psmFilter.getPsmConfidence()) {
                        return false;
                    }
                } else if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (psParameter.getPsmConfidence() != psmFilter.getPsmConfidence()) {
                        return false;
                    }
                } else if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPsmConfidence() == psmFilter.getPsmConfidence()) {
                        return false;
                    }
                }
            }
        }

        if (psmFilter.getPrecursorMz() != null
                || psmFilter.getPrecursorRT() != null
                || psmFilter.getPrecursorMzError() != null) {

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

            if (psmFilter.getPrecursorMz() != null) {
                if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.AFTER) {
                    if (precursor.getMz() <= psmFilter.getPrecursorMz()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (precursor.getMz() >= psmFilter.getPrecursorMz()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (precursor.getMz() != psmFilter.getPrecursorMz()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (precursor.getMz() == psmFilter.getPrecursorMz()) {
                        return false;
                    }
                }
            }

            if (psmFilter.getPrecursorRT() != null) {
                if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.AFTER) {
                    if (precursor.getRt() <= psmFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (precursor.getRt() >= psmFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (precursor.getRt() != psmFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (precursor.getRt() == psmFilter.getPrecursorRT()) {
                        return false;
                    }
                }
            }

            if (psmFilter.getPrecursorMzError() != null) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                double error = Math.abs(spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm()));
                if (psmFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                    if (error <= psmFilter.getPrecursorMzError()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (error >= psmFilter.getPrecursorMzError()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (error != psmFilter.getPrecursorMzError()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (error == psmFilter.getPrecursorMzError()) {
                        return false;
                    }
                }
            }
        }
        if (psmFilter.getCharges() != null) {
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            int charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
            if (!psmFilter.getCharges().contains(charge)) {
                return false;
            }
        }

        return psmFilter.getFileNames().contains(Spectrum.getSpectrumFile(spectrumKey));
    }

    @Override
    public boolean isValidated(String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        return isValidated(matchKey, identification, searchParameters);
    }
}