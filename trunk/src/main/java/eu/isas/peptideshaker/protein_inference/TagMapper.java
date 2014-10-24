package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.TagFragmentIon;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.NeutralLossesMap;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PepnovoParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.experiment.identification.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.tags.Tag;
import com.compomics.util.experiment.identification.tags.TagComponent;
import com.compomics.util.experiment.identification.tags.tagcomponents.MassGap;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
     * The protein tree to use for the mapping.
     */
    private ProteinTree proteinTree;
    /**
     * The search parameters to use.
     */
    private SearchParameters searchParameters;
    /**
     * The sequence matching preferences.
     */
    private SequenceMatchingPreferences sequenceMatchingPreferences;
    /**
     * The annotation preferences to use.
     */
    private AnnotationPreferences annotationPreferences;
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
     * @param proteinTree the protein tree to use for the mapping
     * @param searchParameters the search parameters
     * @param sequenceMatchingPreferences the sequence matching parameters
     * @param annotationPreferences the annotation parameters
     * @param exceptionHandler an exception handler
     */
    public TagMapper(ProteinTree proteinTree, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences, 
            AnnotationPreferences annotationPreferences, ExceptionHandler exceptionHandler) {
        this.proteinTree = proteinTree;
        this.searchParameters = searchParameters;
        this.annotationPreferences = annotationPreferences;
        this.sequenceMatchingPreferences = sequenceMatchingPreferences;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Maps the tags found in an identification files to the ProteinTree of this
     * mapper.
     *
     * @param idfileReader the identification file to map
     * @param waitingHandler a waiting handler used to display progress and
     * cancel the process
     * @param nThreads the number of threads to use
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws MzMLUnmarshallerException
     */
    public void mapTags(IdfileReader idfileReader, WaitingHandler waitingHandler, int nThreads) throws IOException, 
            InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        if (nThreads == 1) {
        mapTagsSingleThread(idfileReader, waitingHandler);
        } else {
            mapTagsMultipleThreads(idfileReader, waitingHandler, nThreads);
        }
    }

    /**
     * Maps tags in the protein database.
     *
     * @param idfileReader the id file reader
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws MzMLUnmarshallerException
     */
    private void mapTagsSingleThread(IdfileReader idfileReader, WaitingHandler waitingHandler) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        HashMap<String, LinkedList<SpectrumMatch>> tagMap = idfileReader.getTagsMap();
        if (tagMap != null && !tagMap.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(tagMap.size());
            waitingHandler.appendReport("Mapping tags to peptides.", true, true);
            for (String key : tagMap.keySet()) {
                Iterator<SpectrumMatch> matchIterator = tagMap.get(key).iterator();
                while (matchIterator.hasNext()) {
                    SpectrumMatch spectrumMatch = matchIterator.next();
                    mapTagsForSpectrumMatch(spectrumMatch, key, waitingHandler, !matchIterator.hasNext());
                }
            }
        }
    }

    /**
     * Maps tags in the protein database.
     *
     * @param idfileReader the id file reader
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws MzMLUnmarshallerException
     */
    private void mapTagsMultipleThreads(IdfileReader idfileReader, WaitingHandler waitingHandler, int nThreads) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        HashMap<String, LinkedList<SpectrumMatch>> tagMap = idfileReader.getTagsMap();
        if (tagMap != null && !tagMap.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(tagMap.size());
            waitingHandler.appendReport("Mapping tags to peptides.", true, true);
            for (String key : tagMap.keySet()) {
                Iterator<SpectrumMatch> matchIterator = tagMap.get(key).iterator();
                while (matchIterator.hasNext()) {
                    SpectrumMatch spectrumMatch = matchIterator.next();
                    TagMapperRunnable tagMapperRunnable = new TagMapperRunnable(spectrumMatch, key, waitingHandler, !matchIterator.hasNext());
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
     * Maps tags in the protein database.
     *
     * @param idfileReader the id file reader
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws MzMLUnmarshallerException
     */
    private void mapTagsForSpectrumMatch(SpectrumMatch spectrumMatch, String key, WaitingHandler waitingHandler, boolean increaseProgress) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {

        TagSpectrumAnnotator spectrumAnnotator = new TagSpectrumAnnotator();
        int keySize = key.length();
        ArrayList<Integer> charges = new ArrayList<Integer>(1);
        charges.add(1); //@TODO: use other charges?
        String spectrumKey = spectrumMatch.getKey();
        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
        HashMap<Integer, HashMap<String, ArrayList<TagAssumption>>> tagAssumptionsMap = spectrumMatch.getTagAssumptionsMap(keySize, sequenceMatchingPreferences);
        for (int advocateId : tagAssumptionsMap.keySet()) {
            HashMap<String, ArrayList<TagAssumption>> algorithmTags = tagAssumptionsMap.get(advocateId);
            ArrayList<TagAssumption> tagAssumptions = algorithmTags.get(key);
            if (tagAssumptions != null) {
                ArrayList<String> inspectedTags = new ArrayList<String>();
                ArrayList<String> peptidesFound = new ArrayList<String>();
                for (TagAssumption tagAssumption : tagAssumptions) {
                    String tagSequence = tagAssumption.getTag().asSequence();
                    if (!inspectedTags.contains(tagSequence)) {
                        mapPtmsForTag(tagAssumption.getTag(), advocateId);
                        ArrayList<TagAssumption> extendedTagList = new ArrayList<TagAssumption>();
                        extendedTagList.add(tagAssumption);
                        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                new NeutralLossesMap(),
                                charges,
                                tagAssumption.getIdentificationCharge().value,
                                spectrum, tagAssumption.getTag(),
                                0,
                                annotationPreferences.getFragmentIonAccuracy(),
                                false, annotationPreferences.isHighResolutionAnnotation());
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
                            HashMap<Peptide, HashMap<String, ArrayList<Integer>>> proteinMapping = proteinTree.getProteinMapping(extendedAssumption.getTag(), sequenceMatchingPreferences, searchParameters.getFragmentIonAccuracy(), searchParameters.getModificationProfile().getFixedModifications(), searchParameters.getModificationProfile().getVariableModifications(), false);
                            for (Peptide peptide : proteinMapping.keySet()) {
                                String peptideKey = peptide.getKey();
                                if (!peptidesFound.contains(peptideKey)) {
                                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, extendedAssumption.getRank(), advocateId, tagAssumption.getIdentificationCharge(), tagAssumption.getScore(), tagAssumption.getIdentificationFile());
                                    peptideAssumption.addUrParam(tagAssumption);
                                    spectrumMatch.addHit(advocateId, peptideAssumption, true);
                                    peptidesFound.add(peptideKey);
                                }
                            }
                            String extendedSequence = extendedAssumption.getTag().asSequence();
                            inspectedTags.add(extendedSequence);
                        }
                    }
                }
            }
        }
        if (increaseProgress) {
            waitingHandler.increaseSecondaryProgressCounter();
        }
        // free memory if needed bzw possible
        if (MemoryConsumptionStatus.memoryUsed() > 0.8 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
            ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
        }
        if (MemoryConsumptionStatus.memoryUsed() > 0.9 && sequenceFactory.getNodesInCache() > 0) {
            sequenceFactory.reduceNodeCacheSize(0.5);
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
     * @throws IOException
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void mapPtmsForTag(Tag tag, int advocateId) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        // add the fixed PTMs
        ptmFactory.checkFixedModifications(searchParameters.getModificationProfile(), tag, sequenceMatchingPreferences);

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
                                String utilitiesPtmName = searchParameters.getModificationProfile().getVariableModifications().get(directagIndex);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("DirecTag PTM " + directagIndex + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
                                PTM ptm = ptmFactory.getPTM(utilitiesPtmName);
                                ArrayList<AminoAcid> aaAtTarget = ptm.getPattern().getAminoAcidsAtTarget();
                                if (aaAtTarget.size() > 1) {
                                    throw new IllegalArgumentException("More than one amino acid can be targeted by the modification " + ptm + ", tag duplication required.");
                                }
                                int aaIndex = aa - 1;
                                aminoAcidPattern.setTargeted(aaIndex, aaAtTarget);
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
                            } else if (advocateId == Advocate.direcTag.getIndex()) {
                                Integer directagIndex = new Integer(modificationMatch.getTheoreticPtm());
                                String utilitiesPtmName = searchParameters.getModificationProfile().getVariableModifications().get(directagIndex);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("DirecTag PTM " + directagIndex + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
                                PTM ptm = ptmFactory.getPTM(utilitiesPtmName);
                                ArrayList<AminoAcid> aaAtTarget = ptm.getPattern().getAminoAcidsAtTarget();
                                if (aaAtTarget.size() > 1) {
                                    throw new IllegalArgumentException("More than one amino acid can be targeted by the modification " + ptm + ", tag duplication required.");
                                }
                                int aaIndex = aa - 1;
                                aminoAcidSequence.setAaAtIndex(aaIndex, aaAtTarget.get(0).singleLetterCode.charAt(0));
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
    private class TagMapperRunnable implements Runnable {

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
         * boolean indicating whether the progress bar should be increased.
         */
        private final boolean increaseProgress;

        /**
         * Constructor.
         *
         * @param spectrumMatch the spectrum match to process
         * @param key the key of tags to map
         * @param waitingHandler waiting handler to display progress and cancel
         * the process
         */
        public TagMapperRunnable(SpectrumMatch spectrumMatch, String key, WaitingHandler waitingHandler, boolean increaseProgress) {
            this.spectrumMatch = spectrumMatch;
            this.key = key;
            this.waitingHandler = waitingHandler;
            this.increaseProgress = increaseProgress;
        }

        @Override
        public void run() {

            try {
                if (!waitingHandler.isRunCanceled()) {
                    mapTagsForSpectrumMatch(spectrumMatch, key, waitingHandler, increaseProgress);
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
