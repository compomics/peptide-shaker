package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidPattern;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidSequence;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
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
import com.compomics.util.experiment.massspectrometry.spectra.MSnSpectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class can be used to map tags to proteins.
 *
 * @author Marc Vaudel
 */
public class TagMapper {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The PTM factory.
     */
    private final ModificationFactory ptmFactory = ModificationFactory.getInstance();
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * Exception handler.
     */
    private final ExceptionHandler exceptionHandler;
    /**
     * The sequence factory.
     */
    private final SequenceFactory sequenceFactory = SequenceFactory.getInstance();

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
     * @param spectrumMatches the spectrum matches containing the peptides to
     * map
     * @param waitingHandler a waiting handler
     */
    public void mapTags(LinkedList<SpectrumMatch> spectrumMatches, WaitingHandler waitingHandler) {

        spectrumMatches.parallelStream().forEach((spectrumMatch) -> {
            try {
                if (!waitingHandler.isRunCanceled()) {
                    mapTagsForSpectrumMatch(spectrumMatch);
                    waitingHandler.increaseSecondaryProgressCounter();
                }
            } catch (Exception e) {
                if (!waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                    waitingHandler.setRunCanceled();
                }
            }
        });
    }

    /**
     * Maps tags to the protein database.
     *
     * @param spectrumMatch the spectrum match containing the tags to map
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading error
     * occurred while mapping the tags.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing a file.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with a database.
     * @throws MzMLUnmarshallerException exception thrown whenever an error
     * occurred while accessing an mzML file.
     */
    private void mapTagsForSpectrumMatch(SpectrumMatch spectrumMatch) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        com.compomics.util.experiment.identification.protein_inference.PeptideMapper peptideMapper = sequenceFactory.getDefaultPeptideMapper();
        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey());
        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = spectrumMatch.getAssumptionsMap();
        for (int advocateId : assumptionsMap.keySet()) {
            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmAssumptions = assumptionsMap.get(advocateId);
            ArrayList<Double> scores = new ArrayList<>(algorithmAssumptions.keySet());
            HashSet<String> inspectedTags = new HashSet<>(algorithmAssumptions.size());
            HashSet<String> peptidesFound = new HashSet<>(algorithmAssumptions.size());
            Collections.sort(scores);
            for (double score : scores) {
                ArrayList<SpectrumIdentificationAssumption> assumptionsAtScore = algorithmAssumptions.get(score);
                // @TODO: allow the user to extend the tags?
//                        extendedTagList.addAll(tagAssumption.getPossibleTags(false, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
//                        extendedTagList.addAll(tagAssumption.getPossibleTags(true, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
//                        if (tagAssumption.getTag().canReverse()) {
//                            extendedTagList.add(tagAssumption.reverse(true));
//                            extendedTagList.add(tagAssumption.reverse(false));
//                        }
                ArrayList<SpectrumIdentificationAssumption> newAssumptions = new ArrayList<>(assumptionsAtScore);
                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : assumptionsAtScore) {
                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        String tagSequence = tagAssumption.getTag().asSequence();
                        if (!inspectedTags.contains(tagSequence)) {
                            Tag tag = tagAssumption.getTag();
                            mapPtmsForTag(tag, advocateId);
                            Double refMass = spectrum.getPrecursor().getMassPlusProton(1);
                            Double fragmentIonAccuracy = searchParameters.getFragmentIonAccuracyInDaltons(refMass);
                            ArrayList<PeptideProteinMapping> proteinMapping = peptideMapper.getProteinMapping(tag, sequenceMatchingPreferences, fragmentIonAccuracy);
                            for (Peptide peptide : PeptideProteinMapping.getPeptides(proteinMapping, sequenceMatchingPreferences)) {
                                String peptideKey = peptide.getKey();
                                if (!peptidesFound.contains(peptideKey)) {
                                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, tagAssumption.getRank(), advocateId, tagAssumption.getIdentificationCharge(), tagAssumption.getScore(), tagAssumption.getIdentificationFile());
                                    newAssumptions.add(peptideAssumption);
                                    peptidesFound.add(peptideKey);
                                }
                            }
                            String sequence = tag.asSequence();
                            inspectedTags.add(sequence);
                        }
                    } else {
                        newAssumptions.add(spectrumIdentificationAssumption);
                    }
                }
                algorithmAssumptions.put(score, newAssumptions);
            }
        }

    }

    /**
     * Remaps the PTMs for a given tag based on the search parameters.
     *
     * @param tag the tag with original algorithm PTMs
     * @param searchParameters the parameters used for the identification
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param advocateId the ID of the advocate
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading error
     * occurred while mapping the tags.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing a file.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with a database.
     * @throws MzMLUnmarshallerException exception thrown whenever an error
     * occurred while accessing an mzML file.
     */
    private void mapPtmsForTag(Tag tag, int advocateId) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        ModificationParameters modificationProfile = searchParameters.getModificationParameters();
        // add the fixed PTMs
        ptmFactory.checkFixedModifications(modificationProfile, tag, identificationParameters.getSequenceMatchingParameters());

        // rename the variable modifications
        for (TagComponent tagComponent : tag.getContent()) {
            
            if (tagComponent instanceof AminoAcidSequence) {

                AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;

                for (int aa : aminoAcidSequence.getModificationIndexes()) {
                    for (ModificationMatch modificationMatch : aminoAcidSequence.getModificationsAt(aa)) {
                        if (modificationMatch.getVariable()) {
                            if (advocateId == Advocate.pepnovo.getIndex()) {
                                String pepnovoPtmName = modificationMatch.getModification();
                                PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocateId);
                                String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("PepNovo+ PTM " + pepnovoPtmName + " not recognized.");
                                }
                                modificationMatch.setModification(utilitiesPtmName);
                            } else if (advocateId == Advocate.direcTag.getIndex()
                                    || advocateId == Advocate.pNovo.getIndex()
                                    || advocateId == Advocate.novor.getIndex()) {
                                // already mapped
                            } else {
                                Advocate notImplemented = Advocate.getAdvocate(advocateId);
                                if (notImplemented == null) {
                                    throw new IllegalArgumentException("Advocate of id " + advocateId + " not recognized.");
                                }
                                throw new IllegalArgumentException("PTM mapping not implemented for " + Advocate.getAdvocate(advocateId).getName() + ".");
                            }
                        }
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
