package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.identification.FastaHeaderParser;
import com.compomics.util.experiment.identification.SequenceDataBase;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.SwingWorker;

/**
 * This class will import sequences from a FASTA file
 *
 * @author Marc
 */
public class FastaImporter {

    /**
     * the parent PeptideShaker object
     */
    private PeptideShaker parent;
    /**
     * Boolean indicating whether the run is finished
     */
    private boolean runFinished = false;

    /**
     * Constructor for a a FASTA importer
     *
     * @param parent    the parent PeptideShaker object
     */
    public FastaImporter(PeptideShaker parent) {
        this.parent = parent;
    }

    /**
     * Imports proteins from a FASTA file
     *
     * @param waitingDialog     Dialog displaying feedback to the user
     * @param proteomicAnalysis The proteomic analysis to attach the database to
     * @param fastaFile         The FASTA file
     * @param databaseName      Database name
     * @param databaseVersion   Database version
     * @param stringBefore      String before the protein accession
     * @param stringAfter       String after the protein accession
     */
    public void importSpectra(WaitingDialog waitingDialog, ProteomicAnalysis proteomicAnalysis, File fastaFile, String databaseName, String databaseVersion, String stringBefore, String stringAfter) {
        FastaProcessor fastaProcessor = new FastaProcessor(waitingDialog, proteomicAnalysis, fastaFile, databaseName, databaseVersion, stringBefore, stringAfter);
        fastaProcessor.execute();
    }

    /**
     * Returns a boolean indicating whether the run is finished
     * @return a boolean indicating whether the run is finished
     */
    public boolean isRunFinished() {
        return runFinished;
    }

    /**
     * Worker which processes a FASTA file and gives feedback to the user.
     */
    private class FastaProcessor extends SwingWorker {

        /**
         * dialog to display feedback to the user
         */
        private WaitingDialog waitingDialog;
        /**
         * the proteomicAnalysis to attach the database to
         */
        private ProteomicAnalysis proteomicAnalysis;
        /**
         * file to process
         */
        private File fastaFile;
        /**
         * Name of the database
         */
        private String databaseName;
        /**
         * Database version
         */
        private String databaseVersion;
        /**
         * String before the accession in the FASTA header
         */
        private String stringBefore;
        /**
         * String after the accession in the FASTA header
         */
        private String stringAfter;

        /**
         * Constructor for the worker
         *
         * @param waitingDialog     Dialog displaying feedback to the user
         * @param proteomicAnalysis The proteomic analysis to attach the database to
         * @param fastaFile         FASTA file to process
         * @param databaseName      Database name
         * @param databaseVersion   Database version
         * @param stringBefore      String before the protein accession
         * @param stringAfter       String after the protein accession
         */
        public FastaProcessor(WaitingDialog waitingDialog, ProteomicAnalysis proteomicAnalysis, File fastaFile, String databaseName, String databaseVersion, String stringBefore, String stringAfter) {
            this.waitingDialog = waitingDialog;
            this.proteomicAnalysis = proteomicAnalysis;
            this.fastaFile = fastaFile;
            this.databaseName = databaseName;
            this.databaseVersion = databaseVersion;
            this.stringBefore = stringBefore;
            this.stringAfter = stringAfter;
        }

        @Override
        protected Object doInBackground() throws Exception {
            try {
                waitingDialog.appendReport("Importing sequences from " + fastaFile.getName() + ".");
                SequenceDataBase db = new SequenceDataBase(databaseName, databaseVersion);
                FastaHeaderParser fastaHeaderParser = new FastaHeaderParser(stringBefore, stringAfter);
                db.importDataBase(fastaHeaderParser, fastaFile);
                proteomicAnalysis.setSequenceDataBase(db);
                waitingDialog.appendReport("FASTA file import completed.");
            } catch (FileNotFoundException e) {
                waitingDialog.appendReport("File " + fastaFile + " was not found. Please open another FASTA file.");
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while loading " + fastaFile + ". Please open another FASTA file.");
            }
            runFinished = true;
            parent.setRunFinished(waitingDialog);
            return 0;
        }
    }
}
