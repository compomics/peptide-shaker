package eu.isas.peptideshaker.cmd;

import com.compomics.software.ToolFactory;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.fileimport.IdFilter;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.export.CpsExporter;
import eu.isas.peptideshaker.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import java.awt.Toolkit;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import javax.swing.JFrame;

/**
 * A Command line interface to run PeptideShaker on a SearchGUI output folder.
 *
 * @author Kenny Helsens
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerCLI implements Callable {

    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The CLI input parameters to start PeptideShaker from command line.
     */
    private PeptideShakerCLIInputBean cliInputBean = null;
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();

    /**
     * Construct a new PeptideShakerCLI runnable from a PeptideShakerCLI Bean.
     * When initialization is successful, calling "run" will start PeptideShaker
     * and write the output files when finished.
     *
     * @param cliInputBean the PeptideShakerCLIInputBean
     */
    public PeptideShakerCLI(PeptideShakerCLIInputBean cliInputBean) {
        this.cliInputBean = cliInputBean;
        loadEnzymes();
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     */
    public Object call() {

        // Set up the waiting handler
        if (cliInputBean.isGUI()) {

            // set the look and feel
            try {
                UtilitiesGUIDefaults.setLookAndFeel();
            } catch (Exception e) {
                // ignore, use default look and feel
            }

            waitingHandler = new WaitingDialog(new JFrame(),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true, null, "Importing Data", "PeptideShaker", null, true); // @TODO: add tips and version number
            ((WaitingDialog) waitingHandler).setLocationRelativeTo(null);

            new Thread(new Runnable() {

                public void run() {
                    try {
                        ((WaitingDialog) waitingHandler).setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();
        } else {
            waitingHandler = new WaitingHandlerCLIImpl();
        }

        // Define new project references.
        MsExperiment experiment = new MsExperiment(cliInputBean.getiExperimentID());
        Sample sample = new Sample(cliInputBean.getiSampleID());
        int replicateNumber = cliInputBean.getReplicate();

        // Create the analysis set of this PeptideShaker process
        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber));
        experiment.addAnalysisSet(sample, analysisSet);

        // Set the project details
        ProjectDetails projectDetails = new ProjectDetails();
        projectDetails.setCreationDate(new Date());

        // Get the search parameters
        SearchParameters searchParameters = cliInputBean.getIdentificationParameters();
        String error = PeptideShaker.loadModifications(searchParameters);
        if (error != null) {
            System.out.println(error);
        }

        // Get the input files
        ArrayList<File> spectrumFiles = cliInputBean.getSpectrumFiles();
        ArrayList<File> identificationFiles = cliInputBean.getIdFiles();

        // Set default filtering import settings
        IdFilter idFilter = new IdFilter();

        // set the processing settings
        ProcessingPreferences processingPreferences = new ProcessingPreferences();
        processingPreferences.setPsmFDR(cliInputBean.getPsmFDR());
        processingPreferences.setPeptideFDR(cliInputBean.getPeptideFDR());
        processingPreferences.setProteinFDR(cliInputBean.getProteinFDR());

        // set the PTM scoring preferences
        PTMScoringPreferences ptmScoringPreferences = new PTMScoringPreferences();
        ptmScoringPreferences.setFlrThreshold(cliInputBean.getiPsmFLR());

        // set the spectrum counting prefrences
        SpectrumCountingPreferences spectrumCountingPreferences = new SpectrumCountingPreferences();

        // Set the annotation preferences
        AnnotationPreferences annotationPreferences = new AnnotationPreferences();
        annotationPreferences.setPreferencesFromSearchParamaers(searchParameters);

        // Create a shaker which will perform the analysis
        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

        // Import the files
        peptideShaker.importFiles(waitingHandler, idFilter, identificationFiles, spectrumFiles, searchParameters,
                annotationPreferences, projectDetails, processingPreferences, ptmScoringPreferences,
                spectrumCountingPreferences, false);

        // Identification as created by PeptideShaker
        ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        // Metrics saved while processing the data
        Metrics metrics = peptideShaker.getMetrics();

        // The cache used for identification
        ObjectsCache objectsCache = peptideShaker.getCache();

        // The identification feature generator
        IdentificationFeaturesGenerator identificationFeaturesGenerator =
                new IdentificationFeaturesGenerator(identification, searchParameters, idFilter, metrics, spectrumCountingPreferences);

        // Save results
        File ouptutFile = cliInputBean.getOutput();
        try {
            waitingHandler.appendReport("Saving results, please wait...", true, true);
            CpsExporter.saveAs(ouptutFile, waitingHandler, experiment, identification, searchParameters,
                    annotationPreferences, spectrumCountingPreferences, projectDetails, metrics,
                    processingPreferences, identificationFeaturesGenerator.getIdentificationFeaturesCache(),
                    ptmScoringPreferences, objectsCache, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // CSV output required?
        if (cliInputBean.getCsvDirectory() != null) {
            waitingHandler.appendReport("Exporting results in csv file, please wait...", true, true);
            CsvExporter exporter = new CsvExporter(experiment, sample, replicateNumber, identificationFeaturesGenerator);
            exporter.exportResults(waitingHandler, cliInputBean.getCsvDirectory());
        }

        // PRIDE output required?
        //@TODO!

        //Export entire project?
        //@TODO!

        // Finished
        System.out.println(System.getProperty("line.separator") + "End of PeptideShaker command line execution" + System.getProperty("line.separator"));

        if (cliInputBean.displayResults()) {
            try {
                ToolFactory.startPeptideShaker(null, ouptutFile);
            } catch (Exception e) {
                waitingHandler.appendReport("An exception occurred while opening the cps file: " + e.getLocalizedMessage(), true, true);
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * PeptideShaker CLI header message when printing the usage.
     */
    private static String getHeader() {
        return "\n"
                + "----------------------"
                + System.getProperty("line.separator")
                + "INFO"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "The PeptideShaker command line tool takes identification files from Mascot, OMSSA and X!Tandem and generates various types of output files." + System.getProperty("line.separator")
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
                + "\n";
    }

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            File lEnzymeFile = new File(PeptideShaker.ENZYME_FILE);
            enzymeFactory.importEnzymes(lEnzymeFile);
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
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

        if (!aLine.hasOption(PeptideShakerCLIParams.EXPERIMENT.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.EXPERIMENT.id)).equals("")) {
            System.out.println("\nExperiment name not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.SAMPLE.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.SAMPLE.id)).equals("")) {
            System.out.println("\nSample name not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.REPLICATE.id) || aLine.getOptionValue(PeptideShakerCLIParams.REPLICATE.id) == null) {
            System.out.println("\nReplicate number not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id)).equals("")) {
            System.out.println("\nSpectrum files not specified.\n");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            ArrayList<File> idFiles = PeptideShakerCLIInputBean.getSpectrumFiles(filesTxt);
            if (idFiles.isEmpty()) {
                System.out.println("\nNo spectrum file found.\n");
                return false;
            }
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.IDENTIFICATION_FILES.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id)).equals("")) {
            System.out.println("\nIdentification files not specified.\n");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
            ArrayList<File> idFiles = PeptideShakerCLIInputBean.getIdentificationFiles(filesTxt);
            if (idFiles.isEmpty()) {
                System.out.println("\nNo identification file found.\n");
                return false;
            }
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)).equals("")) {
            System.out.println("\nOutput file not specified.\n");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id);
            File testFile = new File(filesTxt.trim());
            File parentFolder = testFile.getParentFile();
            if (!parentFolder.exists()) {
                System.out.println("\nDestination folder " + parentFolder.getPath() + " not found.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id).trim();
            File testFile = new File(filesTxt);
            if (!testFile.exists()) {
                System.out.println("\nDestination folder for CSV " + filesTxt + " not found.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id);
            File testFile = new File(filesTxt.trim());
            File parentFolder = testFile.getParentFile();
            if (!parentFolder.exists()) {
                System.out.println("\nDestination folder for Pride file " + parentFolder.getPath() + " not found.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PSM_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse " + input + " as PSM FDR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FLR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PSM_FLR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse " + input + " as PSM FLR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDE_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDE_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse " + input + " as peptide FDR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse " + input + " as protein FDR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.SEARCH_PARAMETERS.id)) {

            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SEARCH_PARAMETERS.id).trim();
            File testFile = new File(filesTxt);
            if (testFile.exists()) {
                try {
                    SearchParameters identificationParameters = SearchParameters.getIdentificationParameters(testFile);
                } catch (Exception e) {
                    System.out.println("\nAn error occurred while parsing " + filesTxt + ".\n");
                    e.printStackTrace();
                }
            } else {
                System.out.println("\nSearch parameters file " + filesTxt + " not found.\n");
                return false;
            }
        }

        return true;
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
                HelpFormatter formatter = new HelpFormatter();

                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print("\nPeptideShaker - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                formatter.printOptions(lPrintWriter, 200, lOptions, 0, 0);
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                PeptideShakerCLIInputBean lCLIBean = new PeptideShakerCLIInputBean(line);
                PeptideShakerCLI lPeptideShakerCLI = new PeptideShakerCLI(lCLIBean);
                lPeptideShakerCLI.call();
            }
        } catch (ParseException e) {
            System.err.println("\n" + e.getMessage() + "\n");
        } catch (IOException e) {
            System.err.println("\n" + e.getMessage() + "\n");
        } catch (ClassNotFoundException e) {
            System.err.println("\n" + e.getMessage() + "\n");
        }
    }

    @Override
    public String toString() {
        return "PeptideShakerCLI{"
                + ", waitingHandler=" + waitingHandler
                + ", cliInputBean=" + cliInputBean
                + ", ptmFactory=" + ptmFactory
                + ", enzymeFactory=" + enzymeFactory
                + '}';
    }
}
