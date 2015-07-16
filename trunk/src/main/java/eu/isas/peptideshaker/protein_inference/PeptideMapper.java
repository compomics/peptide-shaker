package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import java.io.IOException;
import java.sql.SQLException;
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
            if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
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
            if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
                waitingHandler.appendReport("Mapping peptides timed out. Please contact the developers.", true, true);
            }
        }
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
        if (identificationParameters.getIdFilter().validatePeptide(peptide, sequenceMatchingPreferences)) {
            try {
                if (peptide.getParentProteins(sequenceMatchingPreferences).isEmpty()) {
                    throw new IllegalArgumentException("No protein was found for peptide of sequence " + peptide.getSequence() + ". Please verify the following:" + System.getProperty("line.separator")
                            + "- The protein sequence database must be the same or contain the database used for the search." + System.getProperty("line.separator")
                            + "- When using the 'REVERSED' tag, decoy sequences must be reversed versions of the target sequences, use the 'DECOY' tag otherwise." + System.getProperty("line.separator")
                            + "- When using in house databases make sure that the format is recognized by search engines and PeptideShaker (more details at https://code.google.com/p/searchgui/wiki/DatabaseHelp)." + System.getProperty("line.separator"));
                }
            } catch (java.sql.SQLNonTransientConnectionException derbyException) {
                throw new IllegalArgumentException("PeptideShaker could not access the FASTA index databse. Please make sure that no other instance of PeptideShaker is running. If the problem persists, restart your computer." + System.getProperty("line.separator"));
            }
        }
        if (increaseProgressBar) {
            waitingHandler.increaseSecondaryProgressCounter();
        }
        // free memory if needed
        if (MemoryConsumptionStatus.memoryUsed() > 0.8 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
            ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
        }
        if (!MemoryConsumptionStatus.halfGbFree() && sequenceFactory.getNodesInCache() > 0) {
            sequenceFactory.reduceNodeCacheSize(0.5);
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
