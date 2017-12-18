package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.PeptideVariantMatches;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

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
     * Constructor.
     *
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     */
    public PeptideMapper(IdentificationParameters identificationParameters, WaitingHandler waitingHandler) {

        this.identificationParameters = identificationParameters;
        this.waitingHandler = waitingHandler;

    }

    /**
     * Maps the peptides to the proteins in the sequence factory.
     *
     * @param spectrumMatches the spectrum matches containing the peptides to
     * map
     * @param fastaMapper the mapper to use to map peptides to proteins
     */
    public void mapPeptides(LinkedList<SpectrumMatch> spectrumMatches, FastaMapper fastaMapper) {

        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();

        spectrumMatches.parallelStream()
                .flatMap(spectrumMatch -> spectrumMatch.getAllPeptideAssumptions())
                .forEach((peptideAssumption) -> {

                    if (!waitingHandler.isRunCanceled()) {

                        Peptide peptide = peptideAssumption.getPeptide();

                        ArrayList<PeptideProteinMapping> peptideProteinMappings = fastaMapper.getProteinMapping(peptide.getSequence(), sequenceMatchingPreferences);
                        HashMap<String, HashMap<String, int[]>> sequenceIndexes = PeptideProteinMapping.getPeptideProteinIndexesMap(peptideProteinMappings);

                        if (sequenceIndexes.size() == 1) {

                            HashMap<String, int[]> proteinIndexes = sequenceIndexes.values().stream().findAny().get();
                            peptide.setProteinMapping(new TreeMap<>(proteinIndexes));

                            HashMap<String, HashMap<Integer, PeptideVariantMatches>> variantMatches = PeptideProteinMapping.getVariantMatches(peptideProteinMappings);
                            peptide.setVariantMatches(variantMatches);

                        } else if (sequenceIndexes.size() > 1) {

                            throw new UnsupportedOperationException("Ambiguous sequence " + peptide.getSequence() + " not supported at this stage of the analysis.");

                        }
                    }

                    waitingHandler.increaseSecondaryProgressCounter();
                    
                });

    }
}
