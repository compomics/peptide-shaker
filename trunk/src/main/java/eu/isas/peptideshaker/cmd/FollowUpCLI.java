/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.cmd;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.io.SerializationUtils;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.RecalibrationExporter;
import eu.isas.peptideshaker.fileimport.CpsFileImporter;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.UserPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Command line interface to run follow-up analyses on cps files
 *
 * @author Marc
 */
public class FollowUpCLI {

    /**
     * The follow up options
     */
    private FollowUpCLIInputBean followUpCLIInputBean = null;
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The GO factory.
     */
    private GOFactory goFactory = GOFactory.getInstance();
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance(30000);
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The identification features generator
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The identification filter used
     */
    private IdFilter idFilter;
    /**
     * The annotation preferences to use
     */
    private AnnotationPreferences annotationPreferences;
    /**
     * The spectrum counting preferences
     */
    private SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * The PTM scoring preferences
     */
    private PTMScoringPreferences ptmScoringPreferences;
    /**
     * The project details
     */
    private ProjectDetails projectDetails;
    /**
     * The search parameters
     */
    private SearchParameters searchParameters;
    /**
     * The processing preferences
     */
    private ProcessingPreferences processingPreferences;
    /**
     * The metrics stored during processing
     */
    private Metrics metrics;
    /**
     * The gene preferences
     */
    private GenePreferences genePreferences;
    /**
     * The user preferences.
     */
    private UserPreferences userPreferences;

    /**
     * Construct a new FollowUpCLI runnable from a FollowUpCLI Bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired output files.
     *
     * @param followUpCLIInputBean the follow-up options
     */
    public FollowUpCLI(FollowUpCLIInputBean followUpCLIInputBean) {
        this.followUpCLIInputBean = followUpCLIInputBean;
        loadEnzymes();
        loadPtms();
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     */
    public Object call() {

        waitingHandler = new WaitingHandlerCLIImpl();

        try {
            loadCpsFile();
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        // recalibrate spectra
        try {
            CliMethods.recalibrateSpectra(followUpCLIInputBean, identification, annotationPreferences, waitingHandler);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while recalibrating spectra.", true, true);
            e.printStackTrace();
        }

        try {
        closePeptideShaker();
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Imports the information needed for the follow-up processing from a cps file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws Exception 
     */
    public void loadCpsFile() throws FileNotFoundException, IOException, ClassNotFoundException, Exception {

        CpsFileImporter cpsFileImporter = new CpsFileImporter(followUpCLIInputBean.getCpsFile(), waitingHandler);

        // Get the experiment data
        MsExperiment experiment = cpsFileImporter.getExperiment();
        ArrayList<Sample> samples = cpsFileImporter.getSamples();
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("No sample found for the experiment " + experiment.getReference());
        }
        Sample sample = samples.get(0);
        if (samples.size() > 1) { // pretty unlikely to happen for now
            waitingHandler.appendReport(samples.size() + " samples found in experiment " + experiment.getReference() + ", sample " + sample.getReference() + " selected by default.", true, true);
        }
        ArrayList<Integer> replicates = cpsFileImporter.getReplicates(sample);
        if (replicates == null || replicates.isEmpty()) {
            throw new IllegalArgumentException("No replicate found for the sample " + sample.getReference() + " of experiment " + experiment.getReference());
        }
        int replicate = replicates.get(0);
        if (replicates.size() > 1) { // pretty unlikely to happen for now
            waitingHandler.appendReport(replicates.size() + " replicates found in sample " + sample.getReference() + " of experiment " + experiment.getReference() + ", replicate " + sample.getReference() + " selected by default.", true, true);
        }
        ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicate);

        // Get PeptideShaker settings
        PeptideShakerSettings experimentSettings = cpsFileImporter.getExperimentSettings();
        idFilter = experimentSettings.getIdFilter();
        annotationPreferences = experimentSettings.getAnnotationPreferences();
        spectrumCountingPreferences = experimentSettings.getSpectrumCountingPreferences();
        ptmScoringPreferences = experimentSettings.getPTMScoringPreferences();
        projectDetails = experimentSettings.getProjectDetails();
        searchParameters = experimentSettings.getSearchParameters();
        processingPreferences = experimentSettings.getProcessingPreferences();
        metrics = experimentSettings.getMetrics();
        genePreferences = experimentSettings.getGenePreferences();
        loadGeneMappings();

        // Get identification details and set up caches
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, searchParameters, idFilter, metrics, spectrumCountingPreferences);
        if (experimentSettings.getIdentificationFeaturesCache() != null) {
            identificationFeaturesGenerator.setIdentificationFeaturesCache(experimentSettings.getIdentificationFeaturesCache());
        }
        ObjectsCache objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);
        try {
            String dbFolder = new File(getJarFilePath(), PeptideShaker.SERIALIZATION_DIRECTORY).getAbsolutePath();
            identification.establishConnection(dbFolder, false, objectsCache);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occured while reading:\n" + followUpCLIInputBean.getCpsFile() + ".\n\n"
                    + "It looks like another instance of PeptideShaker is still connected to the file.\n"
                    + "Please close all instances of PeptideShaker and try again.", true, true);
            throw e;
        }

        // Load fasta file
        File providedFastaLocation = experimentSettings.getSearchParameters().getFastaFile();
        String fileName = providedFastaLocation.getName();
        File projectFolder = followUpCLIInputBean.getCpsFile().getParentFile();
        File dataFolder = new File(projectFolder, "data");
        if (providedFastaLocation.exists()) {
            sequenceFactory.loadFastaFile(providedFastaLocation);
        } else if (new File(projectFolder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(projectFolder, fileName));
            experimentSettings.getSearchParameters().setFastaFile(new File(projectFolder, fileName));
        } else if (new File(dataFolder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(dataFolder, fileName));
            experimentSettings.getSearchParameters().setFastaFile(new File(dataFolder, fileName));
        } else {
            throw new IllegalArgumentException("Fasta file " + providedFastaLocation.getAbsolutePath() + " not found, please locate the fasta file using the GUI.");
        }

        // Load spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {
            File providedSpectrumLocation = projectDetails.getSpectrumFile(spectrumFileName);
            // try to locate the spectrum file
            if (providedSpectrumLocation == null || !providedSpectrumLocation.exists()) {
                File fileInProjectFolder = new File(projectFolder, spectrumFileName);
                File fileInDataFolder = new File(dataFolder, spectrumFileName);
                if (fileInProjectFolder.exists()) {
                    projectDetails.addSpectrumFile(fileInProjectFolder);
                } else if (fileInDataFolder.exists()) {
                    projectDetails.addSpectrumFile(fileInDataFolder);
                } else {
                    throw new IllegalArgumentException("Spectrum file " + providedSpectrumLocation.getAbsolutePath() + " not found, please locate the spectrum file using the GUI.");
                }
            }
            File mgfFile = projectDetails.getSpectrumFile(fileName);
            spectrumFactory.addSpectra(mgfFile, waitingHandler);
        }

        // Looks like everything is correctly loaded
        waitingHandler.appendReport(followUpCLIInputBean.getCpsFile().getName() + " loaded successfully.", true, true);
    }

    /**
     * Close the PeptideShaker instance by clearing up factories and cache.
     *
     * @throws IOException
     * @throws SQLException
     */
    public void closePeptideShaker() throws IOException, SQLException {

        SpectrumFactory.getInstance().closeFiles();
        SequenceFactory.getInstance().closeFile();
        GOFactory.getInstance().closeFiles();
        identification.close();

        File matchFolder = new File(getJarFilePath(), PeptideShaker.SERIALIZATION_DIRECTORY);
        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                Util.deleteDir(currentFile);
            }
        }
    }
    
    
    /**
     * PeptideShaker CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker follow-up command line takes a cps file and generates various types of output files." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://peptide-shaker.googlecode.com and http://code.google.com/p/peptide-shaker/wiki/PeptideShakerCLI." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/peptide-shaker." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator");
    }

    /**
     * Verifies the command line start parameters.
     *
     * @return true if the startup was valid
     */
    private static boolean isValidStartup(CommandLine aLine) throws IOException {

        if (aLine.getOptions().length == 0) {
            return false;
        }
        

        if (!aLine.hasOption(FollowUpCLIParams.CPS_FILE.id) || ((String) aLine.getOptionValue(FollowUpCLIParams.CPS_FILE.id)).equals("")) {
            System.out.println("\n" + FollowUpCLIParams.CPS_FILE.description + " not specified.\n");
            return false;
        } else {
            String fileTxt = aLine.getOptionValue(FollowUpCLIParams.CPS_FILE.id);
            File testFile = new File(fileTxt.trim());
            if (!testFile.exists()) {
                System.out.println("\n" + FollowUpCLIParams.CPS_FILE.description + " \'" + testFile.getAbsolutePath()+ "\' not found.\n");
                return false;
            }
        }
        
        return true;
    }

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            File lEnzymeFile = new File(getJarFilePath() + File.separator + PeptideShaker.ENZYME_FILE);
            enzymeFactory.importEnzymes(lEnzymeFile);
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
    }

    /**
     * Loads the modifications from the modification file.
     */
    public void loadPtms() {

        // reset ptm factory
        ptmFactory = PTMFactory.getInstance();

        try {
            ptmFactory.importModifications(new File(getJarFilePath(), PeptideShaker.MODIFICATIONS_FILE), false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ptmFactory.importModifications(new File(getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Imports the gene mapping.
     */
    private void loadGeneMappings() {

        // @TODO: move to GenePreferences?

        try {
            genePreferences.createDefaultGeneMappingFiles(
                    new File(getJarFilePath(), "resources/conf/gene_ontology/ensembl_versions"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/go_domains"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/species"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/hsapiens_gene_ensembl_go_mappings"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/hsapiens_gene_ensembl_gene_mappings"));
            genePreferences.loadSpeciesAndGoDomains();
        } catch (IOException e) {
            waitingHandler.appendReport("An error occurred while attempting to create the gene preferences.", true, true);
            e.printStackTrace();
        }

        if (genePreferences.getCurrentSpecies() != null && genePreferences.getSpeciesMap() != null && new File(genePreferences.getGeneMappingFolder(),
                genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GENE_MAPPING_FILE_SUFFIX).exists()) {
            try {
                geneFactory.initialize(new File(genePreferences.getGeneMappingFolder(),
                        genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GENE_MAPPING_FILE_SUFFIX), null);
            } catch (Exception e) {
                waitingHandler.appendReport("Unable to load the gene mapping file.", true, true);
                e.printStackTrace();
            }
        }

        if (genePreferences.getCurrentSpecies() != null && genePreferences.getSpeciesMap() != null && new File(genePreferences.getGeneMappingFolder(),
                genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GO_MAPPING_FILE_SUFFIX).exists()) {
            try {
                goFactory.initialize(new File(genePreferences.getGeneMappingFolder(),
                        genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GO_MAPPING_FILE_SUFFIX), null);
            } catch (Exception e) {
                waitingHandler.appendReport("Unable to load the gene ontology mapping file.", true, true);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("PeptideShakerCLI.class").getPath(), "PeptideShaker");
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            Options lOptions = new Options();
            PeptideShakerCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print("\n==============================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker follow-up - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("==============================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(PeptideShakerCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                FollowUpCLIInputBean lCLIBean = new FollowUpCLIInputBean(line);
                FollowUpCLI lPeptideShakerCLI = new FollowUpCLI(lCLIBean);
                lPeptideShakerCLI.call();
            }
        } catch (OutOfMemoryError e) {
            System.out.println("<CompomicsError> PeptideShaker used up all the memory and had to be stopped. See the PeptideShaker log for details. </CompomicsError>");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.print("<CompomicsError> PeptideShaker processing failed. See the PeptideShaker log for details. </CompomicsError>");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "FollowUpCLI{"
                + ", waitingHandler=" + waitingHandler
                + ", cliInputBean=" + followUpCLIInputBean
                + ", ptmFactory=" + ptmFactory
                + ", enzymeFactory=" + enzymeFactory
                + '}';
    }

    /**
     * Loads the user preferences.
     */
    public void loadUserPreferences() {

        // @TODO: this method should be merged with the similar method in the PeptideShakerGUI class!!

        try {
            File file = new File(PeptideShaker.USER_PREFERENCES_FILE);
            if (!file.exists()) {
                userPreferences = new UserPreferences();
                saveUserPreferences();
            } else {
                userPreferences = (UserPreferences) SerializationUtils.readObject(file);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while loading " + PeptideShaker.USER_PREFERENCES_FILE + " (see below). User preferences set back to default.");
            e.printStackTrace();
            userPreferences = new UserPreferences();
        }
    }

    /**
     * Saves the user preferences.
     */
    public void saveUserPreferences() {

        // @TODO: this method should be merged with the similar method in the PeptideShakerGUI class!!

        try {
            File file = new File(PeptideShaker.USER_PREFERENCES_FILE);
            boolean parentExists = true;

            if (!file.getParentFile().exists()) {
                parentExists = file.getParentFile().mkdir();
            }

            if (parentExists) {
                SerializationUtils.writeObject(userPreferences, file);
            } else {
                System.out.println("Parent folder does not exist: \'" + file.getParentFile() + "\'. User preferences not saved.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
