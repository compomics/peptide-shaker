package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.util.LinkedList;

/**
 * This class can be used to map peptides to proteins.
 *
 * @author Marc Vaudel
 */
public class PeptideMapper {

    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * A waiting handler.
     */
    private final WaitingHandler waitingHandler;
    /**
     * Boolean indicating whether the mapping was canceled for memory issues.
     */
    private boolean canceled = false;
    /**
     * The sequence factory.
     */
    private final SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * Exception handler used to catch exceptions.
     */
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructor.
     *
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     * @param exceptionHandler an exception handler
     */
    public PeptideMapper(IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
        this.identificationParameters = identificationParameters;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Maps the peptides to the proteins in the sequence factory.
     *
     * @param spectrumMatches the spectrum matches containing the peptides to
     * map
     * @param waitingHandler a waiting handler
     */
    public void mapPeptides(LinkedList<SpectrumMatch> spectrumMatches, WaitingHandler waitingHandler) {

        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();

        spectrumMatches.parallelStream()
                .flatMap(spectrumMatch -> spectrumMatch.getAllAssumptions().stream())
                .filter(spectrumIdentificationAssumption -> spectrumIdentificationAssumption instanceof PeptideAssumption)
                .map(spectrumIdentificationAssumption -> ((PeptideAssumption) spectrumIdentificationAssumption).getPeptide())
                .forEach((peptide) -> {
                    try {
                        if (!canceled && !waitingHandler.isRunCanceled()) {
                            peptide.getParentProteins(sequenceMatchingPreferences);
                        }
                        waitingHandler.increaseSecondaryProgressCounter();
                    } catch (Exception e) {
                        if (!canceled && !waitingHandler.isRunCanceled()) {
                            exceptionHandler.catchException(e);
                            canceled = true;
                        }
                    }
                });

    }

    /**
     * Sets whether the mapping is canceled.
     *
     * @param canceled a boolean indicating whether the mapping should be
     * canceled.
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Indicates whether the mapping was canceled.
     *
     * @return boolean indicating whether the mapping was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }
    
}
