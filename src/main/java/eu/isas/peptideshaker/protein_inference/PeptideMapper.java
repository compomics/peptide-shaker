package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.PeptideMapperType;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.fileimport.PsmImporter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * Exception handler used to catch exceptions.
     */
    private ExceptionHandler exceptionHandler;

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
     * Maps the peptides found to the proteins.
     *
     * @param peptideMap a map of the peptides to map: start of the sequence
     * &gt; list of peptides
     * @param nThreads the number of threads to use
     * @param waitingHandler a waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database
     */
    public void mapPeptides(HashMap<String, LinkedList<Peptide>> peptideMap, int nThreads, WaitingHandler waitingHandler) throws IOException, InterruptedException, SQLException,
            ClassNotFoundException {
        if (nThreads == 1) {
            mapPeptidesSingleThreaded(peptideMap, waitingHandler);
        } else {
            mapPeptidesThreadingPerKey(peptideMap, nThreads, waitingHandler);
        }
    }

    /**
     * Maps the peptides found to the proteins.
     *
     * @param peptideMap a map of the peptides to map: start of the sequence
     * &gt; list of peptides
     * @param sequenceMatchingPreferences The sequence matching preferences
     * @param idFilter The import filter
     * @param waitingHandler A waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database
     */
    private void mapPeptidesSingleThreaded(HashMap<String, LinkedList<Peptide>> peptideMap, WaitingHandler waitingHandler)
            throws IOException, InterruptedException, SQLException, ClassNotFoundException {

        if (peptideMap != null && !peptideMap.isEmpty()) {
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(peptideMap.size());
            waitingHandler.appendReport("Mapping peptides to proteins.", true, true);
            HashSet<String> keys = new HashSet<String>(peptideMap.keySet());
            for (String key : keys) {
                LinkedList<Peptide> peptides = peptideMap.get(key);
                Iterator<Peptide> peptideIterator = peptides.iterator();
                while (peptideIterator.hasNext()) {
                    Peptide peptide = peptideIterator.next();
                    mapPeptide(peptide, !peptideIterator.hasNext());
                }
                peptideMap.remove(key);
            }
        }
    }

    /**
     * Maps the peptides found to the proteins.
     *
     * @param peptideMap a map of the peptides to map: start of the sequence
     * &gt; list of peptides
     * @param sequenceMatchingPreferences The sequence matching preferences
     * @param idFilter the import filter
     * @param nThreads the number of threads to use
     * @param waitingHandler a waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database
     */
    private void mapPeptidesThreadingPerKey(HashMap<String, LinkedList<Peptide>> peptideMap, int nThreads,
            WaitingHandler waitingHandler) throws IOException, InterruptedException, SQLException, ClassNotFoundException {

        if (peptideMap != null && !peptideMap.isEmpty()) {
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(peptideMap.size());
            waitingHandler.appendReport("Mapping peptides to proteins.", true, true);
            HashSet<String> keys = new HashSet<String>(peptideMap.keySet());
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            for (String key : keys) {
                LinkedList<Peptide> peptides = peptideMap.get(key);
                PeptideListMapperRunnable peptideMapperRunnable = new PeptideListMapperRunnable(peptides);
                pool.submit(peptideMapperRunnable);
                if (canceled || waitingHandler.isRunCanceled()) {
                    pool.shutdownNow();
                    return;
                }
                peptideMap.remove(key);
                if (canceled || waitingHandler.isRunCanceled()) {
                    pool.shutdownNow();
                    return;
                }
            }
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
                waitingHandler.appendReport("Mapping peptides timed out. Please contact the developers.", true, true);
            }
        }
    }

    /**
     * Maps the peptides found to the proteins.
     *
     * @param peptideMap a map of the peptides to map: start of the sequence
     * &gt; list of peptides
     * @param sequenceMatchingPreferences The sequence matching preferences
     * @param idFilter the import filter
     * @param nThreads the number of threads to use
     * @param waitingHandler a waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database
     */
    private void mapPeptidesThreadingPerPeptide(HashMap<String, LinkedList<Peptide>> peptideMap, int nThreads,
            WaitingHandler waitingHandler) throws IOException, InterruptedException, SQLException, ClassNotFoundException, ExecutionException {

        if (peptideMap != null && !peptideMap.isEmpty()) {
            waitingHandler.setMaxSecondaryProgressCounter(peptideMap.size());
            waitingHandler.appendReport("Mapping peptides to proteins.", true, true);
            HashSet<String> keys = new HashSet<String>(peptideMap.keySet());
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            for (String key : keys) {
                LinkedList<Peptide> peptides = peptideMap.get(key);
                Iterator<Peptide> peptideIterator = peptides.iterator();
                while (peptideIterator.hasNext()) {
                    Peptide peptide = peptideIterator.next();
                    PeptideMapperRunnable peptideMapperRunnable = new PeptideMapperRunnable(peptide, !peptideIterator.hasNext());
                    pool.submit(peptideMapperRunnable);
                    if (canceled || waitingHandler.isRunCanceled()) {
                        pool.shutdownNow();
                        return;
                    }
                }
                peptideMap.remove(key);
                if (canceled || waitingHandler.isRunCanceled()) {
                    pool.shutdownNow();
                    return;
                }
            }
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
                waitingHandler.appendReport("Mapping peptides timed out. Please contact the developers.", true, true);
            }
        }
    }

    /**
     * Sets whether the mapping should be canceled.
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

    /**
     * Maps a list of peptides.
     *
     * @param peptide the peptide to map
     * @param increaseProgressBar boolean indicating whether the progress bar
     * should be increased after mapping the peptide
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database
     */
    private void mapPeptide(Peptide peptide, boolean increaseProgressBar) throws IOException, InterruptedException, SQLException, ClassNotFoundException {
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        if (identificationParameters.getPeptideAssumptionFilter().validatePeptide(peptide, sequenceMatchingPreferences, identificationParameters.getSearchParameters().getDigestionPreferences())) {
            try {
                peptide.getParentProteins(sequenceMatchingPreferences);
            } catch (java.sql.SQLNonTransientConnectionException derbyException) {
                derbyException.printStackTrace();
                throw new IllegalArgumentException("PeptideShaker could not access the FASTA index database. "
                        + "Please make sure that no other instance of PeptideShaker is running. "
                        + "If the problem persists, restart your computer." 
                        + System.getProperty("line.separator"));
            }
        }
        if (increaseProgressBar) {
            waitingHandler.increaseSecondaryProgressCounter();
        }
        // free memory if needed
        if (MemoryConsumptionStatus.memoryUsed() > 0.8 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
            ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
        }
        if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
            ProteinTree proteinTree = (ProteinTree) sequenceFactory.getDefaultPeptideMapper();
            if (MemoryConsumptionStatus.memoryUsed() > 0.9 && proteinTree.getNodesInCache() > 0) {
                proteinTree.reduceNodeCacheSize(0.5);
            }
        }
        if (MemoryConsumptionStatus.memoryUsed() > 0.8) {
            Runtime.getRuntime().gc();
            if (MemoryConsumptionStatus.memoryUsed() > 0.8) {
                // all peptides/protein mappings cannot be kept in memory at the same time, abort
                canceled = true;
            }
        }
    }

    /**
     * Creates a map of peptides which are likely to require protein mapping
     * from a given identification file. These are best scoring peptides and
     * peptides likely to carry protein terminal modifications. Peptides are
     * sorted in a map indexed by their leading first amino acids. The number of
     * amino acids depends on the key size of the protein tree.
     *
     * @param fileReader the file reader used to parse the identification file
     * @param idFileSpectrumMatches the list of spectrum matches from this file
     * @param identification the identification used to store matches
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allowing
     * canceling the process
     *
     * @return a map of peptides which are likely to require protein mapping
     *
     * @throws SQLException exception thrown if an error occurred while
     * accessing the protein tree
     * @throws IOException exception thrown if an error occurred while accessing
     * the protein tree
     * @throws ClassNotFoundException exception thrown if an error occurred
     * while accessing the protein tree
     * @throws InterruptedException exception thrown if an error occurred while
     * accessing the protein tree
     */
    public static HashMap<String, LinkedList<Peptide>> getPeptideMap(IdfileReader fileReader, LinkedList<SpectrumMatch> idFileSpectrumMatches, Identification identification, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        PeptideAssumptionFilter peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        PtmSettings ptmSettings = searchParameters.getPtmSettings();
        LinkedList<Double> terminalModificationMasses = new LinkedList<Double>();
        PTMFactory ptmFactory = PTMFactory.getInstance();

        for (String ptmName : ptmSettings.getAllModifications()) {
            PTM ptm = ptmFactory.getPTM(ptmName);
            if (ptm.getType() == PTM.MODC
                    || ptm.getType() == PTM.MODCAA
                    || ptm.getType() == PTM.MODN
                    || ptm.getType() == PTM.MODNAA) {
                terminalModificationMasses.add(ptm.getMass());
            }
        }

        int peptideMapKeyLength = 2;
        if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
            ProteinTree proteinTree = (ProteinTree) SequenceFactory.getInstance().getDefaultPeptideMapper();
            peptideMapKeyLength = proteinTree.getInitialTagSize();
        }
        int rankMax = 3;
        HashMap<String, LinkedList<Peptide>> peptideMap = new HashMap<String, LinkedList<Peptide>>(8000);

        for (SpectrumMatch spectrumMatch : idFileSpectrumMatches) {

            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> matchAssumptions = spectrumMatch.getAssumptionsMap();
            String spectrumKey = spectrumMatch.getKey();
            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> rawDbAssumptions = null;

            if (fileReader.hasDeNovoTags()) { // for now only de novo results are stored in the database at this point
                rawDbAssumptions = identification.getRawAssumptions(spectrumKey);
            }
            HashSet<Integer> algorithms = new HashSet<Integer>();
            if (matchAssumptions != null) {
                algorithms.addAll(matchAssumptions.keySet());
            }
            if (rawDbAssumptions != null) {
                algorithms.addAll(rawDbAssumptions.keySet());
            }

            for (Integer algorithm : algorithms) {

                HashSet<Double> scores = new HashSet<Double>();
                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> scoreMap1 = null;
                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> scoreMap2 = null;

                if (matchAssumptions != null) {
                    scoreMap1 = matchAssumptions.get(algorithm);
                    if (scoreMap1 != null) {
                        scores.addAll(scoreMap1.keySet());
                    }
                }

                if (rawDbAssumptions != null) {
                    scoreMap2 = rawDbAssumptions.get(algorithm);
                    if (scoreMap2 != null) {
                        scores.addAll(scoreMap2.keySet());
                    }
                }

                LinkedList<Double> scoresList = new LinkedList<Double>(scores);
                Collections.sort(scoresList);
                ArrayList<Peptide> bestScoringPeptides = new ArrayList<Peptide>(2);
                int rank = 1;
                ArrayList<Peptide> terminalModificationPeptides = new ArrayList<Peptide>(2);

                for (Double score : scoresList) {

                    if (scoreMap1 != null) {

                        ArrayList<SpectrumIdentificationAssumption> assumptions = scoreMap1.get(score);
                        if (assumptions != null) {

                            for (SpectrumIdentificationAssumption assumption : assumptions) {

                                if (assumption instanceof PeptideAssumption) {

                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    Peptide peptide = peptideAssumption.getPeptide();
                                    boolean potentialTerminalModification = false;

                                    if (rank > rankMax) {
                                        potentialTerminalModification = PsmImporter.hasPotentialTerminalModification(fileReader, searchParameters, peptide, terminalModificationMasses);
                                    }

                                    if ((rank <= rankMax || potentialTerminalModification)
                                            && peptideAssumptionFilter.validatePeptide(peptide, sequenceMatchingPreferences, searchParameters.getDigestionPreferences())) {
                                        if (rank < rankMax) {
                                            bestScoringPeptides.add(peptide);
                                        } else if (potentialTerminalModification) {
                                            terminalModificationPeptides.add(peptide);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (scoreMap2 != null) {

                        ArrayList<SpectrumIdentificationAssumption> assumptions = scoreMap2.get(score);

                        if (assumptions != null) {

                            for (SpectrumIdentificationAssumption assumption : assumptions) {

                                if (assumption instanceof PeptideAssumption) {

                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    Peptide peptide = peptideAssumption.getPeptide();
                                    boolean potentialTerminalModification = false;

                                    if (rank > rankMax) {
                                        potentialTerminalModification = PsmImporter.hasPotentialTerminalModification(fileReader, searchParameters, peptide, terminalModificationMasses);
                                    }

                                    if ((rank <= rankMax || potentialTerminalModification)
                                            && peptideAssumptionFilter.validatePeptide(peptide, sequenceMatchingPreferences, searchParameters.getDigestionPreferences())) {
                                        if (rank <= rankMax) {
                                            bestScoringPeptides.add(peptide);
                                        } else if (potentialTerminalModification) {
                                            terminalModificationPeptides.add(peptide);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (rank <= rankMax && !bestScoringPeptides.isEmpty()) {

                        for (Peptide peptide : bestScoringPeptides) {

                            String sequence = peptide.getSequence();
                            String subSequence = sequence.substring(0, peptideMapKeyLength);
                            subSequence = AminoAcid.getMatchingSequence(subSequence, sequenceMatchingPreferences);
                            LinkedList<Peptide> peptidesForTag = peptideMap.get(subSequence);

                            if (peptidesForTag == null) {
                                peptidesForTag = new LinkedList<Peptide>();
                                peptideMap.put(subSequence, peptidesForTag);
                            }

                            peptidesForTag.add(peptide);
                        }

                        rank++;
                    }

                    if (!terminalModificationPeptides.isEmpty()) {

                        for (Peptide peptide : terminalModificationPeptides) {

                            String sequence = peptide.getSequence();
                            String subSequence = sequence.substring(0, peptideMapKeyLength);
                            subSequence = AminoAcid.getMatchingSequence(subSequence, sequenceMatchingPreferences);
                            LinkedList<Peptide> peptidesForTag = peptideMap.get(subSequence);

                            if (peptidesForTag == null) {
                                peptidesForTag = new LinkedList<Peptide>();
                                peptideMap.put(subSequence, peptidesForTag);
                            }

                            peptidesForTag.add(peptide);
                        }

                        terminalModificationPeptides.clear();
                    }
                }
            }

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return new HashMap<String, LinkedList<Peptide>>(0);
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }
        }

        return peptideMap;
    }

    /**
     * Private runnable to map peptides from a list.
     */
    private class PeptideListMapperRunnable implements Runnable {

        /**
         * The peptides to map.
         */
        private LinkedList<Peptide> peptideList;

        /**
         * Constructor.
         *
         * @param peptideList the peptides to map
         */
        public PeptideListMapperRunnable(LinkedList<Peptide> peptideList) {
            this.peptideList = peptideList;
        }

        @Override
        public void run() {

            try {
                Iterator<Peptide> peptideIterator = peptideList.iterator();
                while (peptideIterator.hasNext()) {
                    Peptide peptide = peptideIterator.next();
                    if (!canceled && !waitingHandler.isRunCanceled()) {
                        mapPeptide(peptide, !peptideIterator.hasNext());
                    }
                }
            } catch (Exception e) {
                if (!canceled && !waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                    canceled = true;
                }
            }
        }
    }

    /**
     * Private runnable to map a peptide.
     */
    private class PeptideMapperRunnable implements Runnable {

        /**
         * The peptide to map.
         */
        private Peptide peptide;

        /**
         * Boolean indicating whether the progress bar should be increased.
         */
        private boolean increaseProgressBar;

        /**
         * Constructor.
         */
        public PeptideMapperRunnable() {

        }

        /**
         * Constructor.
         *
         * @param peptide the peptide to map
         * @param increaseProgressBar boolean indicating whether the progress
         * bar should be increased after mapping the peptide
         */
        public PeptideMapperRunnable(Peptide peptide, boolean increaseProgressBar) {
            this.peptide = peptide;
            this.increaseProgressBar = increaseProgressBar;
        }

        @Override
        public void run() {

            try {
                if (!canceled && !waitingHandler.isRunCanceled()) {
                    mapPeptide(peptide, increaseProgressBar);
                }
            } catch (Exception e) {
                if (!canceled && !waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                    canceled = true;
                }
            }
        }
    }
}
