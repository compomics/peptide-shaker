package eu.isas.peptideshaker.cmd;

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
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.fileimport.IdFilter;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.export.CpsExporter;
import eu.isas.peptideshaker.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * A Command line interface to run PeptideShaker on a SearchGUI output folder.
 *
 * @author Kenny Helsens
 * @author Marc Vaudel
 */
public class PeptideShakerCLI implements Callable {

    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler = new WaitingHandlerCLIImpl();
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

        // Define new project references.
        MsExperiment experiment = new MsExperiment(cliInputBean.getiExperimentID());
        Sample sample = new Sample(cliInputBean.getiSampleID());
        int replicateNumber = cliInputBean.getReplicate();

        // Create the analysis set of this PeptideShaker process
        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber));
        experiment.addAnalysisSet(sample, analysisSet);

        // Set the project details
        ProjectDetails projectDetails = new ProjectDetails();
        try {
            projectDetails.setModificationFile(getModificationFile());
            projectDetails.setUserModificationFile(getUserModificationFile());
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
        }

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

        // Create a shaker which will perform the analysis
        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

        // Import the files
        peptideShaker.importFiles(waitingHandler, idFilter, identificationFiles, spectrumFiles, searchParameters, annotationPreferences, projectDetails, processingPreferences, ptmScoringPreferences, spectrumCountingPreferences);

        // Identification as created by PeptideShaker
        ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        // Metrics saved while processing the data
        Metrics metrics = peptideShaker.getMetrics();

        // The cache used for identification
        ObjectsCache objectsCache = peptideShaker.getCache();

        // The identification feature generator
        IdentificationFeaturesGenerator identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, searchParameters, idFilter, metrics, spectrumCountingPreferences);

        // Save results
        try {
            File ouptutFile = cliInputBean.getOutput();
            CpsExporter.saveAs(ouptutFile, waitingHandler, experiment, identification, searchParameters, annotationPreferences, spectrumCountingPreferences, projectDetails, metrics, processingPreferences, identificationFeaturesGenerator.getIdentificationFeaturesCache(), ptmScoringPreferences, objectsCache, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // CSV output required?
        if (cliInputBean.getCsvDirectory() != null) {
            CsvExporter exporter = new CsvExporter(experiment, sample, replicateNumber, searchParameters.getEnzyme(), identificationFeaturesGenerator);
            exporter.exportResults(null, cliInputBean.getCsvDirectory());
        }

        // Pride output required?
        //@TODO!

        //Export entire project?
        //@TODO!

        // Finished
        System.out.println("finished PeptideShaker-CLI");

        return null;
    }

    /**
     * PeptideShaker CLI header message when printing the usage.
     */
    private static String getHeader() {
        return ""
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "INFO"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "The PeptideShaker command line tool takes identification files from Mascot, OMSSA and X!Tandem and generates various types of output files.\n"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + "OPTIONS" + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + "";
    }

    /**
     * Returns a File handle from to the mods.xml file in the classpath.
     *
     * @return a File handle from to the mods.xml file in the classpath
     */
    private File getModificationFile() throws URISyntaxException {
        return new File(PeptideShaker.MODIFICATIONS_FILE);
    }

    /**
     * Returns a File handle from to the mods.xml file in the classpath.
     *
     * @return a File handle from to the mods.xml file in the classpath
     */
    private File getUserModificationFile() throws URISyntaxException {
        return new File(PeptideShaker.USER_MODIFICATIONS_FILE);
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
            System.out.println("Experiment name not specified.");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.SAMPLE.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.SAMPLE.id)).equals("")) {
            System.out.println("Sample name not specified.");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.REPLICATE.id) || aLine.getOptionValue(PeptideShakerCLIParams.REPLICATE.id) == null) {
            System.out.println("Replicate number not specified.");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id)).equals("")) {
            System.out.println("Spectrum files not specified.");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            for (String file : PeptideShakerCLIInputBean.splitInput(filesTxt)) {
                File testFile = new File(file);
                if (!testFile.exists()) {
                    System.out.println("Spectrum file " + file + " not found.");
                    return false;
                }
            }
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.IDENTIFICATION_FILES.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id)).equals("")) {
            System.out.println("Identification files not specified.");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
            for (String file : PeptideShakerCLIInputBean.splitInput(filesTxt)) {
                File testFile = new File(file);
                if (!testFile.exists()) {
                    System.out.println("Identification file " + file + " not found.");
                    return false;
                }
            }
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)).equals("")) {
            System.out.println("Output file not specified.");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
            File testFile = new File(filesTxt.trim());
            File parentFolder = testFile.getParentFile();
            if (!parentFolder.exists()) {
                System.out.println("Destination folder " + parentFolder.getPath() + " not found.");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id).trim();
            File testFile = new File(filesTxt);
            if (!testFile.exists()) {
                System.out.println("Destination folder for CSV " + filesTxt + " not found.");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id);
            File testFile = new File(filesTxt.trim());
            File parentFolder = testFile.getParentFile();
            if (!parentFolder.exists()) {
                System.out.println("Destination folder for Pride file " + parentFolder.getPath() + " not found.");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PSM_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("Could not parse " + input + " as PSM FDR threshold.");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FLR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PSM_FLR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("Could not parse " + input + " as PSM FLR threshold.");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDE_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDE_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("Could not parse " + input + " as peptide FDR threshold.");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("Could not parse " + input + " as protein FDR threshold.");
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
                    System.out.println("An error occurred while parsing " + filesTxt + ".");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Search parameters file " + filesTxt + " not found.");
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

            CommandLine line = null;
            line = parser.parse(lOptions, args);

            if (!isValidStartup(line)) {
                HelpFormatter formatter = new HelpFormatter();

                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print("PeptideShaker-CLI" + System.getProperty("line.separator"));

                lPrintWriter.print(getHeader());

                lPrintWriter.print(System.getProperty("line.separator") + "Options:" + System.getProperty("line.separator"));
                formatter.printOptions(lPrintWriter, 200, lOptions, 0, 0);

                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                System.out.println("PeptideShaker-CLI startup parameters ok.");

                PeptideShakerCLIInputBean lCLIBean = new PeptideShakerCLIInputBean(line);
                PeptideShakerCLI lPeptideShakerCLI = new PeptideShakerCLI(lCLIBean);
                lPeptideShakerCLI.call();
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
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
