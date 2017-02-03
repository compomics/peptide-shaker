package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PepnovoParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.amino_acid_tags.TagComponent;
import com.compomics.util.experiment.biology.MassGap;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.experiment.identification.amino_acid_tags.matchers.TagMatcher;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.protein_inference.PeptideMapperType;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import org.xmlpull.v1.XmlPullParserException;
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
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * Exception handler.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

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
     * Maps the tags found in an identification files to the ProteinTree of this
     * mapper.
     *
     * @param idfileReader the identification file to map
     * @param identification identification object used to store the matches
     * @param waitingHandler a waiting handler used to display progress and
     * cancel the process
     * @param nThreads the number of threads to use
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever a threading error
     * occurred while mapping the tags
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing a file
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with a database
     * @throws MzMLUnmarshallerException exception thrown whenever an error
     * occurred while accessing an mzML file
     * @throws javax.xml.bind.JAXBException thrown whenever an error occurred
     * while accessing an mzML file
     * @throws org.xmlpull.v1.XmlPullParserException thrown whenever an error
     * occurred while accessing an mzML file
     */
    public void mapTags(IdfileReader idfileReader, Identification identification, WaitingHandler waitingHandler, int nThreads) throws IOException,
            InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, JAXBException, XmlPullParserException {

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        LinkedList<SpectrumMatch> spectrumMatches = idfileReader.getAllSpectrumMatches(waitingHandler, identificationParameters.getSearchParameters());
        if (spectrumMatches != null && !spectrumMatches.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(spectrumMatches.size());
            waitingHandler.appendReport("Mapping de novo tags to peptides.", true, true);
            PtmSettings modificationProfile = identificationParameters.getSearchParameters().getPtmSettings();
            TagMatcher tagMatcher = new TagMatcher(modificationProfile.getFixedModifications(), modificationProfile.getAllNotFixedModifications(), identificationParameters.getSequenceMatchingPreferences());
            tagMatcher.setSynchronizedIndexing(true);
            for (SpectrumMatch spectrumMatch : spectrumMatches) {
                SpectrumMatchTagMapperRunnable tagMapperRunnable = new SpectrumMatchTagMapperRunnable(identification, spectrumMatch, tagMatcher, waitingHandler);
                pool.submit(tagMapperRunnable);
                if (waitingHandler.isRunCanceled()) {
                    pool.shutdownNow();
                    return;
                }
            }
        }
        pool.shutdown();
        if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
            waitingHandler.appendReport("Mapping tags timed out. Please contact the developers.", true, true);
        }
    }

    /**
     * Maps tags to the protein database.
     *
     * @param identification identification object used to store the matches
     * @param spectrumMatch the spectrum match containing the tags to map
     * @param tagMatcher the tag matcher to match the tags
     * @param key the key of the tag to match
     * @param waitingHandler waiting handler allowing the display of progress
     * and canceling the process
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
    private void mapTagsForSpectrumMatch(Identification identification, SpectrumMatch spectrumMatch, TagMatcher tagMatcher, WaitingHandler waitingHandler) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        com.compomics.util.experiment.identification.protein_inference.PeptideMapper peptideMapper = sequenceFactory.getDefaultPeptideMapper();
        String spectrumKey = spectrumMatch.getKey();
        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = spectrumMatch.getAssumptionsMap();
        for (Integer advocateId : assumptionsMap.keySet()) {
            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmTags = assumptionsMap.get(advocateId);
            HashSet<Double> scores = new HashSet<Double>(algorithmTags.keySet());
            HashSet<String> inspectedTags = new HashSet<String>(algorithmTags.size());
            HashSet<String> peptidesFound = new HashSet<String>(algorithmTags.size());
            for (Double score : scores) {
                ArrayList<SpectrumIdentificationAssumption> tagAssumptions = algorithmTags.get(score);
                // @TODO: allow the user to extend the tags
//                        extendedTagList.addAll(tagAssumption.getPossibleTags(false, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
//                        extendedTagList.addAll(tagAssumption.getPossibleTags(true, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
//                        if (tagAssumption.getTag().canReverse()) {
//                            extendedTagList.add(tagAssumption.reverse(true));
//                            extendedTagList.add(tagAssumption.reverse(false));
//                        }
                ArrayList<SpectrumIdentificationAssumption> newAssumptions = new ArrayList<SpectrumIdentificationAssumption>(tagAssumptions);
                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : tagAssumptions) {
                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        String tagSequence = tagAssumption.getTag().asSequence();
                        if (!inspectedTags.contains(tagSequence)) {
                            Tag tag = tagAssumption.getTag();
                            mapPtmsForTag(tag, advocateId);
                            Double refMass = spectrum.getPrecursor().getMassPlusProton(1);
                            Double fragmentIonAccuracy = searchParameters.getFragmentIonAccuracyInDaltons(refMass);
                            ArrayList<PeptideProteinMapping> proteinMapping = peptideMapper.getProteinMapping(tag, tagMatcher, sequenceMatchingPreferences, fragmentIonAccuracy);
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
                algorithmTags.put(score, newAssumptions);
            }
        }

        identification.addRawAssumptions(spectrumKey, assumptionsMap);

        tagMatcher.clearCache();
        waitingHandler.increaseSecondaryProgressCounter();

        // free memory if needed and possible
        if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
            if (MemoryConsumptionStatus.memoryUsed() > 0.8 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
                ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
            }
            ProteinTree proteinTree = (ProteinTree) sequenceFactory.getDefaultPeptideMapper();
            if (MemoryConsumptionStatus.memoryUsed() > 0.9 && proteinTree.getNodesInCache() > 0) {
                proteinTree.reduceNodeCacheSize(0.5);
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
        PtmSettings modificationProfile = searchParameters.getPtmSettings();
        // add the fixed PTMs
        ptmFactory.checkFixedModifications(modificationProfile, tag, identificationParameters.getSequenceMatchingPreferences());

        // rename the variable modifications
        for (TagComponent tagComponent : tag.getContent()) {
            if (tagComponent instanceof AminoAcidPattern) {

                AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;

                for (int aa : aminoAcidPattern.getModificationIndexes()) {
                    for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(aa)) {
                        if (modificationMatch.isVariable()) {
                            if (advocateId == Advocate.pepnovo.getIndex()) {
                                String pepnovoPtmName = modificationMatch.getTheoreticPtm();
                                PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocateId);
                                String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("PepNovo+ PTM " + pepnovoPtmName + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
                            } else if (advocateId == Advocate.direcTag.getIndex()) {
                                Integer directagIndex = new Integer(modificationMatch.getTheoreticPtm());
                                String utilitiesPtmName = modificationProfile.getVariableModifications().get(directagIndex);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("DirecTag PTM " + directagIndex + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
                                PTM ptm = ptmFactory.getPTM(utilitiesPtmName);
                                if (ptm.getPattern() != null) {
                                    ArrayList<Character> aaAtTarget = ptm.getPattern().getAminoAcidsAtTarget();
                                    if (aaAtTarget.size() > 1) {
                                        throw new IllegalArgumentException("More than one amino acid can be targeted by the modification " + ptm + ", tag duplication required.");
                                    }
                                    int aaIndex = aa - 1;
                                    aminoAcidPattern.setTargeted(aaIndex, aaAtTarget);
                                }
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
            } else if (tagComponent instanceof AminoAcidSequence) {

                AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;

                for (int aa : aminoAcidSequence.getModificationIndexes()) {
                    for (ModificationMatch modificationMatch : aminoAcidSequence.getModificationsAt(aa)) {
                        if (modificationMatch.isVariable()) {
                            if (advocateId == Advocate.pepnovo.getIndex()) {
                                String pepnovoPtmName = modificationMatch.getTheoreticPtm();
                                PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocateId);
                                String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("PepNovo+ PTM " + pepnovoPtmName + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
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

    /**
     * Private runnable to map tags of a spectrum match.
     */
    private class SpectrumMatchTagMapperRunnable implements Runnable {

        /**
         * The spectrum match to process.
         */
        private final SpectrumMatch spectrumMatch;

        /**
         * The waiting handler to display progress and cancel the process.
         */
        private final WaitingHandler waitingHandler;
        /**
         * The tag to protein matcher.
         */
        private final TagMatcher tagMatcher;
        /**
         * Identification where to store the matches
         */
        private final Identification identification;

        /**
         * Constructor
         *
         * @param identification the identification object where to store the
         * matches
         * @param spectrumMatch the spectrum match to map
         * @param tagMatcher the tag matcher
         * @param waitingHandler waiting handler allowing the display of
         * progress and cancelling the process
         */
        public SpectrumMatchTagMapperRunnable(Identification identification, SpectrumMatch spectrumMatch, TagMatcher tagMatcher, WaitingHandler waitingHandler) {
            this.spectrumMatch = spectrumMatch;
            this.waitingHandler = waitingHandler;
            this.tagMatcher = tagMatcher;
            this.identification = identification;
        }

        @Override
        public void run() {

            try {
                if (!waitingHandler.isRunCanceled()) {
                    mapTagsForSpectrumMatch(identification, spectrumMatch, tagMatcher, waitingHandler);
                }
            } catch (Exception e) {
                if (!waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                    waitingHandler.setRunCanceled();
                }
            }
        }
    }
}
