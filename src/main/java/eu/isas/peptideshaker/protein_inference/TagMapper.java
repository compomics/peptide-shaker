package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidSequence;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.parameters.identification.tool_specific.PepnovoParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.amino_acid_tags.TagComponent;
import com.compomics.util.experiment.identification.amino_acid_tags.MassGap;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class can be used to map tags to proteins.
 *
 * @author Marc Vaudel
 */
public class TagMapper {

    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * Exception handler.
     */
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructor.
     *
     * @param identificationParameters the identification parameters
     * @param exceptionHandler an exception handler
     */
    public TagMapper(IdentificationParameters identificationParameters, ExceptionHandler exceptionHandler) {

        this.identificationParameters = identificationParameters;
        this.exceptionHandler = exceptionHandler;

    }

    /**
     * Maps the tags to the proteins in the sequence factory.
     *
     * @param spectrumMatches the spectrum matches containing the tags to map
     * @param fastaMapper the FASTA mapper to use
     * @param waitingHandler a waiting handler
     */
    public void mapTags(
            SpectrumMatch[] spectrumMatches, 
            FastaMapper fastaMapper, 
            WaitingHandler waitingHandler
    ) {

        for (SpectrumMatch spectrumMatch : spectrumMatches) {

            if (!waitingHandler.isRunCanceled()) {

                mapTagsForSpectrumMatch(spectrumMatch, fastaMapper);
                waitingHandler.increaseSecondaryProgressCounter();

            }
        }
    }

    /**
     * Maps tags to the protein database.
     *
     * @param fastaMapper the fasta mapper to use
     * @param spectrumMatch the spectrum match containing the tags to map
     */
    private void mapTagsForSpectrumMatch(
            SpectrumMatch spectrumMatch, 
            FastaMapper fastaMapper
    ) {

        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();
        HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> assumptionsMap = spectrumMatch.getTagAssumptionsMap();

        for (Entry<Integer, TreeMap<Double, ArrayList<TagAssumption>>> entry : assumptionsMap.entrySet()) {

            int advocateId = entry.getKey();
            TreeMap<Double, ArrayList<TagAssumption>> algorithmAssumptions = entry.getValue();

            HashSet<String> inspectedTags = new HashSet<>(algorithmAssumptions.size());
            HashSet<Long> peptidesFound = new HashSet<>(algorithmAssumptions.size());

            for (ArrayList<TagAssumption> assumptionsAtScore : algorithmAssumptions.values()) {

                // @TODO: allow the user to extend the tags?
//                        extendedTagList.addAll(tagAssumption.getPossibleTags(false, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
//                        extendedTagList.addAll(tagAssumption.getPossibleTags(true, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
//                        if (tagAssumption.getTag().canReverse()) {
//                            extendedTagList.add(tagAssumption.reverse(true));
//                            extendedTagList.add(tagAssumption.reverse(false));
//                        }
                for (TagAssumption tagAssumption : assumptionsAtScore) {

                    String tagSequence = tagAssumption.getTag().asSequence();

                    if (!inspectedTags.contains(tagSequence)) {

                        Tag tag = tagAssumption.getTag();
                        mapModificationsForTag(tag, advocateId);
                        ArrayList<PeptideProteinMapping> proteinMapping = fastaMapper.getProteinMapping(tag, sequenceMatchingPreferences);

                        for (Peptide peptide : PeptideProteinMapping.getPeptides(proteinMapping, sequenceMatchingPreferences)) {

                            long peptideKey = peptide.getKey();

                            if (!peptidesFound.contains(peptideKey)) {

                                PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, tagAssumption.getRank(), advocateId, tagAssumption.getIdentificationCharge(), tagAssumption.getScore(), tagAssumption.getIdentificationFile());
                                spectrumMatch.addPeptideAssumption(advocateId, peptideAssumption);
                                peptidesFound.add(peptideKey);

                            }
                        }

                        inspectedTags.add(tagSequence);

                    }
                }
            }
        }
    }

    /**
     * Remaps the modifications for a given tag based on the search parameters.
     *
     * @param tag the tag with original algorithm PTMs
     * @param searchParameters the parameters used for the identification
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param advocateId the ID of the advocate
     */
    private void mapModificationsForTag(Tag tag, int advocateId) {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        // rename the variable modifications
        for (TagComponent tagComponent : tag.getContent()) {

            if (tagComponent instanceof AminoAcidSequence) {

                AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;

                for (ModificationMatch modificationMatch : aminoAcidSequence.getVariableModifications()) {

                    if (advocateId == Advocate.direcTag.getIndex()
                            || advocateId == Advocate.pNovo.getIndex()
                            || advocateId == Advocate.novor.getIndex()) {
                        // already mapped

                    } else if (advocateId == Advocate.pepnovo.getIndex()) {

                        String pepnovoPtmName = modificationMatch.getModification();
                        PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocateId);
                        String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);

                        if (utilitiesPtmName == null) {

                            throw new IllegalArgumentException("PepNovo+ PTM " + pepnovoPtmName + " not recognized.");

                        }

                        modificationMatch.setModification(utilitiesPtmName);

                    } else {

                        Advocate notImplemented = Advocate.getAdvocate(advocateId);

                        if (notImplemented == null) {

                            throw new IllegalArgumentException("Advocate of id " + advocateId + " not recognized.");

                        }

                        throw new IllegalArgumentException("PTM mapping not implemented for " + Advocate.getAdvocate(advocateId).getName() + ".");

                    }
                }

            } else if (tagComponent instanceof MassGap) {
                // no PTM there

            } else {

                throw new UnsupportedOperationException("PTM mapping not implemeted for tag component " + tagComponent.getClass() + ".");

            }
        }
    }
}
