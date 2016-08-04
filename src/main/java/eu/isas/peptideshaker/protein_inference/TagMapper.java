package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.TagFragmentIon;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PepnovoParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
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
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.protein_inference.PeptideMapperType;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.waiting.WaitingHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    public void mapTags(IdfileReader idfileReader, Identification identification, WaitingHandler waitingHandler, int nThreads) throws IOException,
            InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        if (nThreads == 1) {
            mapTagsSingleThread(idfileReader, identification, waitingHandler);
        } else if (identificationParameters.getSequenceMatchingPreferences().getPeptideMapperType() == PeptideMapperType.tree) {
            mapTagsThreadingPerKey(idfileReader, identification, waitingHandler, nThreads);
        } else {
            mapTagsThreadingPerMatch(idfileReader, identification, waitingHandler, nThreads);
        }
    }

    /**
     * Maps tags in the protein database.
     *
     * @param idfileReader the id file reader
     * @param identification identification object used to store the matches
     * @param waitingHandler waiting handler allowing the display of progress
     * and cancelling the process
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
    private void mapTagsSingleThread(IdfileReader idfileReader, Identification identification, WaitingHandler waitingHandler) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        HashMap<String, LinkedList<SpectrumMatch>> tagMap = idfileReader.getTagsMap();
        if (tagMap != null && !tagMap.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(tagMap.size());
            waitingHandler.appendReport("Mapping de novo tags to peptides.", true, true);
            PtmSettings modificationProfile = identificationParameters.getSearchParameters().getPtmSettings();
            TagSpectrumAnnotator spectrumAnnotator = new TagSpectrumAnnotator();
            for (String key : tagMap.keySet()) {
                TagMatcher tagMatcher = new TagMatcher(modificationProfile.getFixedModifications(), modificationProfile.getAllNotFixedModifications(), identificationParameters.getSequenceMatchingPreferences());
                Iterator<SpectrumMatch> matchIterator = tagMap.get(key).iterator();
                while (matchIterator.hasNext()) {
                    SpectrumMatch spectrumMatch = matchIterator.next();
                    mapTagsForSpectrumMatch(identification, spectrumMatch, spectrumAnnotator, tagMatcher, key, waitingHandler, !matchIterator.hasNext(), true);
                }
            }
        }
    }

    /**
     * Maps tags to the protein database proceeding per spectrum match.
     *
     * @param idfileReader an id file reader where to get spectrum matches from
     * @param identification identification object used to store the matches
     * @param waitingHandler waiting handler allowing the display of progress
     * and cancelling the process
     * @param nThreads the number of threads to use
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
    private void mapTagsThreadingPerMatch(IdfileReader idfileReader, Identification identification, WaitingHandler waitingHandler, int nThreads) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        HashMap<String, LinkedList<SpectrumMatch>> tagMap = idfileReader.getTagsMap();
        if (tagMap != null && !tagMap.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(tagMap.size());
            waitingHandler.appendReport("Mapping de novo tags to peptides.", true, true);
            PtmSettings modificationProfile = identificationParameters.getSearchParameters().getPtmSettings();
            for (String key : tagMap.keySet()) {
                TagMatcher tagMatcher = new TagMatcher(modificationProfile.getFixedModifications(), modificationProfile.getAllNotFixedModifications(), identificationParameters.getSequenceMatchingPreferences());
                tagMatcher.setSynchronizedIndexing(true);
                Iterator<SpectrumMatch> matchIterator = tagMap.get(key).iterator();
                while (matchIterator.hasNext()) {
                    SpectrumMatch spectrumMatch = matchIterator.next();
                    SpectrumMatchTagMapperRunnable tagMapperRunnable = new SpectrumMatchTagMapperRunnable(identification, spectrumMatch, tagMatcher, key, waitingHandler, !matchIterator.hasNext());
                    pool.submit(tagMapperRunnable);
                    if (waitingHandler.isRunCanceled()) {
                        pool.shutdownNow();
                        return;
                    }
                }
            }
        }
        pool.shutdown();
        if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
            waitingHandler.appendReport("Mapping tags timed out. Please contact the developers.", true, true);
        }
    }

    /**
     * Maps tags to the protein database proceeding by tag key.
     *
     * @param idfileReader an id file reader where to get spectrum matches from
     * @param identification identification object used to store the matches
     * @param waitingHandler waiting handler allowing the display of progress
     * and cancelling the process
     * @param nThreads the number of threads to use
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
    private void mapTagsThreadingPerKey(IdfileReader idfileReader, Identification identification, WaitingHandler waitingHandler, int nThreads) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        HashMap<String, LinkedList<SpectrumMatch>> tagMap = idfileReader.getTagsMap();
        if (tagMap != null && !tagMap.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(tagMap.size());
            waitingHandler.appendReport("Mapping de novo tags to peptides.", true, true);
            PtmSettings modificationProfile = identificationParameters.getSearchParameters().getPtmSettings();
            for (String key : tagMap.keySet()) {
                LinkedList<SpectrumMatch> spectrumMatches = tagMap.get(key);
                KeyTagMapperRunnable tagMapperRunnable = new KeyTagMapperRunnable(identification, spectrumMatches, modificationProfile.getFixedModifications(), modificationProfile.getAllNotFixedModifications(), identificationParameters.getSequenceMatchingPreferences(), key, waitingHandler);
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
     * @param spectrumAnnotator a spectrum annotator
     * @param tagMatcher the tag matcher to match the tags
     * @param key the key of the tag to match
     * @param waitingHandler waiting handler allowing the display of progress
     * and canceling the process
     * @param increaseProgress boolean indicating whether the progress bar of
     * the waiting handler should be increased
     * @param threadPerSpectrum boolean indicating whether only one thread is
     * used per spectrum
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
    private void mapTagsForSpectrumMatch(Identification identification, SpectrumMatch spectrumMatch, TagSpectrumAnnotator spectrumAnnotator, TagMatcher tagMatcher, String key, WaitingHandler waitingHandler, boolean increaseProgress, boolean threadPerSpectrum) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        com.compomics.util.experiment.identification.protein_inference.PeptideMapper peptideMapper = sequenceFactory.getDefaultPeptideMapper();
        int keySize = key.length();
        ArrayList<Integer> charges = new ArrayList<Integer>(1);
        charges.add(1); //@TODO: use other charges?
        String spectrumKey = spectrumMatch.getKey();
        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        HashMap<Integer, HashMap<String, ArrayList<TagAssumption>>> tagAssumptionsMap = spectrumMatch.getTagAssumptionsMap(keySize, identificationParameters.getSequenceMatchingPreferences());
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsToSave = new HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>>(1);
        HashSet<Integer> advocates = new HashSet<Integer>(tagAssumptionsMap.keySet());
        for (Integer advocateId : advocates) {
            HashMap<String, ArrayList<TagAssumption>> algorithmTags = tagAssumptionsMap.get(advocateId);
            ArrayList<TagAssumption> tagAssumptions = algorithmTags.get(key);
            if (tagAssumptions != null) {
                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateMapToSave = assumptionsToSave.get(advocateId);
                if (advocateMapToSave == null) {
                    advocateMapToSave = new HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>(2);
                    assumptionsToSave.put(advocateId, advocateMapToSave);
                }
                ArrayList<String> inspectedTags = new ArrayList<String>();
                ArrayList<String> peptidesFound = new ArrayList<String>();
                for (TagAssumption tagAssumption : tagAssumptions) {
                    String tagSequence = tagAssumption.getTag().asSequence();
                    if (!inspectedTags.contains(tagSequence)) {
                        Double score = tagAssumption.getScore();
                        ArrayList<SpectrumIdentificationAssumption> assumptionAtScoreToSave = advocateMapToSave.get(score);
                        if (assumptionAtScoreToSave == null) {
                            assumptionAtScoreToSave = new ArrayList<SpectrumIdentificationAssumption>(4);
                            advocateMapToSave.put(score, assumptionAtScoreToSave);
                        }
                        mapPtmsForTag(tagAssumption.getTag(), advocateId);
                        ArrayList<TagAssumption> extendedTagList = new ArrayList<TagAssumption>();
                        extendedTagList.add(tagAssumption);
                        SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, tagAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, spectrum, tagAssumption.getTag());
                        int nB = 0, nY = 0;
                        for (IonMatch ionMatch : annotations) {
                            Ion ion = ionMatch.ion;
                            if (ion instanceof TagFragmentIon) {
                                int ionType = ion.getSubType();
                                if (ionType == TagFragmentIon.A_ION
                                        || ionType == TagFragmentIon.B_ION
                                        || ionType == TagFragmentIon.C_ION) {
                                    nB++;
                                } else {
                                    nY++;
                                }
                            }
                        }
                        if (nB < 3) {
                            extendedTagList.addAll(tagAssumption.getPossibleTags(false, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
                        }
                        if (nY < 3) {
                            extendedTagList.addAll(tagAssumption.getPossibleTags(true, searchParameters.getMinChargeSearched().value, searchParameters.getMaxChargeSearched().value, 2));
                        }
                        if (tagAssumption.getTag().canReverse()) {
                            extendedTagList.add(tagAssumption.reverse(nY >= nB));
                        }
                        for (TagAssumption extendedAssumption : extendedTagList) {
                            // free memory if needed and possible
                            if (MemoryConsumptionStatus.memoryUsed() > 0.9) {
                                tagMatcher.clearCache();
                            }
                            assumptionAtScoreToSave.add(extendedAssumption);
                            Double refMass = spectrum.getPrecursor().getMassPlusProton(1);
                            Double fragmentIonAccuracy = searchParameters.getFragmentIonAccuracyInDaltons(refMass);
                            ArrayList<PeptideProteinMapping> proteinMapping = peptideMapper.getProteinMapping(extendedAssumption.getTag(), tagMatcher, sequenceMatchingPreferences, fragmentIonAccuracy);
                            for (Peptide peptide : PeptideProteinMapping.getPeptides(proteinMapping)) {
                                String peptideKey = peptide.getKey();
                                if (!peptidesFound.contains(peptideKey)) {
                                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, extendedAssumption.getRank(), advocateId, tagAssumption.getIdentificationCharge(), tagAssumption.getScore(), tagAssumption.getIdentificationFile());
                                    assumptionAtScoreToSave.add(peptideAssumption);
                                    peptidesFound.add(peptideKey);
                                }
                            }
                            String extendedSequence = extendedAssumption.getTag().asSequence();
                            inspectedTags.add(extendedSequence);
                        }
                    }
                }
                algorithmTags.remove(key);
                if (algorithmTags.isEmpty()) {
                    tagAssumptionsMap.remove(advocateId);
                }
            }
        }
        if (tagAssumptionsMap.isEmpty()) {
            spectrumMatch.removeAssumptions();
        }
        if (!assumptionsToSave.isEmpty()) {
            identification.addRawAssumptions(spectrumKey, assumptionsToSave, threadPerSpectrum);
        }

        if (increaseProgress) {
            tagMatcher.clearCache();
            waitingHandler.increaseSecondaryProgressCounter();
        }
        if (MemoryConsumptionStatus.memoryUsed() > 0.8 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
            ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
        }
        // free memory if needed and possible
        if (MemoryConsumptionStatus.memoryUsed() > 0.9) {
            tagMatcher.clearCache();
        }
        if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
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
     * Private runnable to map tags of all spectrum matches of a key.
     */
    private class KeyTagMapperRunnable implements Runnable {

        /**
         * The spectrum matches to process.
         */
        private final LinkedList<SpectrumMatch> spectrumMatches;
        /**
         * The tree key.
         */
        private final String key;
        /**
         * The waiting handler to display progress and cancel the process.
         */
        private final WaitingHandler waitingHandler;
        /**
         * The spectrum annotator for this thread.
         */
        private final TagSpectrumAnnotator tagSpectrumAnnotator;
        /**
         * The tag to protein matcher.
         */
        private final TagMatcher tagMatcher;
        /**
         * Identification where to store the matches.
         */
        private final Identification identification;

        /**
         * Constructor
         *
         * @param identification identification object where to store the
         * matches
         * @param spectrumMatches the spectrum matches to map
         * @param fixedModifications list of fixed modifications
         * @param variableModifications list of variable modifications
         * @param sequenceMatchingPreferences sequence matching preferences
         * @param key key of the tags to match
         * @param waitingHandler waiting handler allowing the display of
         * progress and canceling the process
         */
        public KeyTagMapperRunnable(Identification identification, LinkedList<SpectrumMatch> spectrumMatches, ArrayList<String> fixedModifications,
                ArrayList<String> variableModifications, SequenceMatchingPreferences sequenceMatchingPreferences, String key, WaitingHandler waitingHandler) {
            this.spectrumMatches = spectrumMatches;
            this.key = key;
            this.waitingHandler = waitingHandler;
            this.tagMatcher = new TagMatcher(fixedModifications, variableModifications, sequenceMatchingPreferences);
            this.tagSpectrumAnnotator = new TagSpectrumAnnotator();
            this.identification = identification;
        }

        @Override
        public void run() {

            try {
                Iterator<SpectrumMatch> matchIterator = spectrumMatches.iterator();
                while (matchIterator.hasNext()) {
                    SpectrumMatch spectrumMatch = matchIterator.next();
                    if (!waitingHandler.isRunCanceled()) {
                        mapTagsForSpectrumMatch(identification, spectrumMatch, tagSpectrumAnnotator, tagMatcher, key, waitingHandler, !matchIterator.hasNext(), false);
                    }
                }
            } catch (Exception e) {
                if (!waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                    waitingHandler.setRunCanceled();
                }
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
         * The tree key.
         */
        private final String key;

        /**
         * The waiting handler to display progress and cancel the process.
         */
        private final WaitingHandler waitingHandler;
        /**
         * The spectrum annotator for this thread.
         */
        private final TagSpectrumAnnotator tagSpectrumAnnotator;
        /**
         * boolean indicating whether the progress bar should be increased.
         */
        private final boolean increaseProgress;
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
         * @param key the key inspected
         * @param waitingHandler waiting handler allowing the display of
         * progress and cancelling the process
         * @param increaseProgress boolean indicating whether the progress bar
         * of the waiting handler should be increased
         */
        public SpectrumMatchTagMapperRunnable(Identification identification, SpectrumMatch spectrumMatch, TagMatcher tagMatcher, String key, WaitingHandler waitingHandler, boolean increaseProgress) {
            this.spectrumMatch = spectrumMatch;
            this.key = key;
            this.waitingHandler = waitingHandler;
            this.tagSpectrumAnnotator = new TagSpectrumAnnotator();
            this.increaseProgress = increaseProgress;
            this.tagMatcher = tagMatcher;
            this.identification = identification;
        }

        @Override
        public void run() {

            try {
                if (!waitingHandler.isRunCanceled()) {
                    mapTagsForSpectrumMatch(identification, spectrumMatch, tagSpectrumAnnotator, tagMatcher, key, waitingHandler, increaseProgress, true);
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
