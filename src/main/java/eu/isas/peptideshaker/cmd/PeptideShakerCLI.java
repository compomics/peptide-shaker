package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.io.identifications.IdentificationParametersReader;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.fileimport.FileImporter;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.gui.NewDialog;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SearchParameters;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * A Command line interface to run PeptideShaker on a SearchGUI output folder.
 *
 * @author Kenny Helsens
 */
public class PeptideShakerCLI implements Callable {

    /**
     * The xml file containing the enzymes.
     */
    private static final String ENZYME_FILE = "/resources/conf/peptideshaker_enzymes.xml";
    /**
     * Modification file.
     */
    private static final String MODIFICATIONS_FILE = "/resources/conf/peptideshaker_mods.xml";
    /**
     * User modification file.
     */
    private static final String USER_MODIFICATIONS_FILE = "/resources/conf/peptideshaker_usermods.xml";
    /**
     * User preferences file.
     */
    private static final String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences.cpf";
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler iWaitingHandler = new WaitingHandlerCLIImpl();
    /**
     * The CLI input parameters to start PeptideShaker from command line.
     */
    private PeptideShakerCLIInputBean iCLIInputBean = null;
    /**
     * The experiment conducted.
     */
    private MsExperiment experiment = null;
    /**
     * The sample analyzed.
     */
    private Sample sample;
    /**
     * The list of identification files.
     */
    private ArrayList<File> idFiles = new ArrayList<File>();
    /**
     * The parameters files found.
     */
    private ArrayList<File> searchParametersFiles = new ArrayList<File>();
    /**
     * The list of spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
    /**
     * The xml modification files found.
     */
    private ArrayList<File> modificationFiles = new ArrayList<File>();
    /**
     * The fasta file.
     */
    private File fastaFile = null;
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The parameters of the search.
     */
    private SearchParameters searchParameters = new SearchParameters();
    /**
     * The annotation preferences.
     */
    private AnnotationPreferences annotationPreferences = new AnnotationPreferences();
    /**
     * The identification filter used for this project.
     */
    private IdFilter idFilter = new IdFilter();
    /**
     * The project details.
     */
    private ProjectDetails projectDetails = null;
    protected int iReplicaNumber = 1;

    /**
     * Construct a new PeptideShakerCLI runnable from a PeptideShakerCLI Bean.
     * When initialization is successful, calling "run" will start PeptideShaker
     * and write the output files when finished.
     * 
     * @param aCLIInputBean the PeptideShakerCLIInputBean
     */
    public PeptideShakerCLI(PeptideShakerCLIInputBean aCLIInputBean) {
        iCLIInputBean = aCLIInputBean;

        loadEnzymes();

        importSearchGUIFiles();

        File lInputFolder = iCLIInputBean.getInput();
        importSearchParameters(searchParametersFiles.get(0));

        File lSearchGUIMGFFiles = new File(lInputFolder, NewDialog.SEARCHGUI_INPUT);
        importMgfFiles(lSearchGUIMGFFiles);

        // Define new sample and experiment if not existing so far.
        experiment = new MsExperiment(aCLIInputBean.getExperimentID());
        sample = new Sample(aCLIInputBean.getSampleID());

        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(iReplicaNumber));
        experiment.addAnalysisSet(sample, analysisSet);

        // Set the project details
        projectDetails = new ProjectDetails();
        
        try {
            projectDetails.setModificationFile(getModificationFile());
            projectDetails.setUserModificationFile(getUserModificationFile());
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     */
    public Object call() {

        // Define new sample and experiment
        // Create new PeptideShaker instance from this experiment and sample
        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, iReplicaNumber);

        // Import the current files/settings
        ProcessingPreferences processingPreferences = new ProcessingPreferences();
        processingPreferences.setPsmFDR(iCLIInputBean.getPSMFDR());
        processingPreferences.setPeptideFDR(iCLIInputBean.getPeptideFDR());
        processingPreferences.setProteinFDR(iCLIInputBean.getProteinFDR());
        processingPreferences.estimateAScore(iCLIInputBean.estimateAScore());
        peptideShaker.importFiles(iWaitingHandler, idFilter, idFiles, spectrumFiles, fastaFile, searchParameters, annotationPreferences, projectDetails, processingPreferences);

        // Creates a dummy IdentificationFeaturesGenerator instnace.
        IdentificationFeaturesGenerator lIdentificationFeaturesGenerator = new IdentificationFeaturesGenerator(null);

        // Export the PeptideShaker project into a CSV file
        CsvExporter exporter = new CsvExporter(experiment, sample, 1, searchParameters.getEnzyme(), lIdentificationFeaturesGenerator);
        exporter.exportResults(null, iCLIInputBean.getOutput()); //@TODO you might want to use other kind of output


        // Finished!
        System.out.println("finished PeptideShaker-CLI");

        return null;
    }

    /**
     * PeptideShaker CLI header message when printing the usage.
     */
    private static String getHeader() {
        return ""
                + "----------------------\n"
                + "\n"
                + "INFO"
                + "\n"
                + "----------------------\n"
                + "\n"
                + "The PeptideShaker command line tool takes a SearchGUI result folder and performs the X!Tandem/OMSSA integration including user specified FDR calculations generates PSM, peptide and protein an output files.\n"
                + "\n"
                + "----------------------\n"
                + "OPTIONS\n"
                + "\n"
                + "----------------------\n"
                + "";
    }

    /**
     * CreateOptionsCLI. 
     * 
     * @param aOptions Apache Commons CLI Options instance to set the possible
     * parameters that can be passed to PeptideShakerCLI.
     */
    private static void createOptionsCLI(Options aOptions) {
        aOptions.addOption(PeptideShakerCLIParams.FDR_LEVEL_PSM.id, true, PeptideShakerCLIParams.FDR_LEVEL_PSM.description);
        aOptions.addOption(PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.id, true, PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.description);
        aOptions.addOption(PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.id, true, PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.description);
        aOptions.addOption(PeptideShakerCLIParams.PEPTIDESHAKER_INPUT.id, true, PeptideShakerCLIParams.PEPTIDESHAKER_INPUT.description);
        aOptions.addOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id, true, PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.description);
        aOptions.addOption(PeptideShakerCLIParams.ASCORE.id, false, PeptideShakerCLIParams.ASCORE.description);
    }

    /**
     * Loads the modifications from the modification file.
     */
    private void resetPtmFactory() {

        // reset ptm factory
        ptmFactory.reloadFactory();
        ptmFactory = PTMFactory.getInstance();
        try {
            ptmFactory.importModifications(getModificationFile(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            ptmFactory.importModifications(getUserModificationFile(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a File handle from to the mods.xml file in the classpath.
     * 
     * @return a File handle from to the mods.xml file in the classpath
     */
    private File getModificationFile() throws URISyntaxException {
        return new File(this.getClass().getResource(MODIFICATIONS_FILE).toURI());
    }

    /**
     * Returns a File handle from to the mods.xml file in the classpath.
     * 
     * @return a File handle from to the mods.xml file in the classpath
     */
    private File getUserModificationFile() throws URISyntaxException {
        return new File(this.getClass().getResource(USER_MODIFICATIONS_FILE).toURI());
    }

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            File lEnzymeFile = new File(this.getClass().getResource(ENZYME_FILE).toURI());
            enzymeFactory.importEnzymes(lEnzymeFile);
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
    }

    /**
     * Initialize the SearchGUI result folder. Loads the identification files,
     * the mgf files and other parameter files located in that folder.
     */
    private void importSearchGUIFiles() {

        FileImporter.setCLIMode(true);
        FileImporter.setReducedMemory(false);

        File lInputFolder = iCLIInputBean.getInput();
        File[] lInputList = lInputFolder.listFiles();
        
        for (File lInputFile : lInputList) {
            if (lInputFile.getName().toLowerCase().endsWith("dat")
                    || lInputFile.getName().toLowerCase().endsWith("omx")
                    || lInputFile.getName().toLowerCase().endsWith("xml")) {
                if (!lInputFile.getName().equals("mods.xml")
                        && !lInputFile.getName().equals("usermods.xml")) {
                    idFiles.add(lInputFile);
                } else if (lInputFile.getName().endsWith("usermods.xml")) {
                    modificationFiles.add(lInputFile);
                }
            } else if (lInputFile.getName().toLowerCase().endsWith(".properties")) {
                boolean found = false;
                for (File tempFile : searchParametersFiles) {
                    if (tempFile.getName().equals(lInputFile.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    searchParametersFiles.add(lInputFile);
                }
            }
        }
    }

    /**
     * Imports the search parameters from a searchGUI file.
     *
     * @param searchGUIFile the selected searchGUI file
     */
    private void importSearchParameters(File searchGUIFile) {

        this.resetPtmFactory(); // reload the ptms

        try {
            Properties props = IdentificationParametersReader.loadProperties(searchGUIFile);
            ArrayList<String> searchedMods = new ArrayList<String>();
            String temp = props.getProperty(IdentificationParametersReader.VARIABLE_MODIFICATIONS);

            if (temp != null && !temp.trim().equals("")) {
                searchedMods = IdentificationParametersReader.parseModificationLine(temp);
            }

            temp = props.getProperty(IdentificationParametersReader.FIXED_MODIFICATIONS);

            if (temp != null && !temp.trim().equals("")) {
                searchedMods.addAll(IdentificationParametersReader.parseModificationLine(temp));
            }

            ArrayList<String> missing = new ArrayList<String>();

            for (String name : searchedMods) {
                if (!ptmFactory.containsPTM(name)) {
                    missing.add(name);
                } else {
                    if (!searchParameters.getModificationProfile().getUtilitiesNames().contains(name)) {
                        searchParameters.getModificationProfile().setPeptideShakerName(name, name);
                        if (!searchParameters.getModificationProfile().getPeptideShakerNames().contains(name)) {
                            int index = name.length() - 1;
                            if (name.lastIndexOf(" ") > 0) {
                                index = name.indexOf(" ");
                            }
                            if (name.lastIndexOf("-") > 0) {
                                index = Math.min(index, name.indexOf("-"));
                            }
                            searchParameters.getModificationProfile().setShortName(name, name.substring(0, index));
                        }

                        ArrayList<String> conflicts = new ArrayList<String>();

                        for (String oldModification : searchParameters.getModificationProfile().getUtilitiesNames()) {
                            PTM oldPTM = ptmFactory.getPTM(oldModification);
                            if (Math.abs(oldPTM.getMass() - ptmFactory.getPTM(name).getMass()) < 0.01) {
                                if (!searchedMods.contains(oldModification)) {
                                    conflicts.add(oldModification);
                                }
                            }
                        }
                        for (String conflict : conflicts) {
                            searchParameters.getModificationProfile().remove(conflict);
                        }
                    }
                }
            }
            if (!missing.isEmpty()) {
                for (File modFile : modificationFiles) {
                    try {
                        ptmFactory.importModifications(modFile, true);
                    } catch (Exception e) {
                        // ignore error
                    }
                }
                ArrayList<String> missing2 = new ArrayList<String>();
                for (String ptmName : missing) {
                    if (!ptmFactory.containsPTM(ptmName)) {
                        missing2.add(ptmName);
                    }
                }
                if (!missing2.isEmpty()) {
                    if (missing2.size() == 1) {
                        System.err.print("The following modification is currently not recognized by PeptideShaker: "
                                + missing2.get(0) + ".\nPlease import it by editing the search parameters.");
                    } else {
                        String output = "The following modifications are currently not recognized by PeptideShaker:\n";
                        boolean first = true;
                        for (String ptm : missing2) {
                            if (first) {
                                first = false;
                            } else {
                                output += ", ";
                            }
                            output += ptm;
                        }
                        output += ".\nPlease import it by editing the search parameters.";
                        System.err.print(output);
                    }
                }
            }

            temp = props.getProperty(IdentificationParametersReader.ENZYME);

            if (temp != null && !temp.equals("")) {
                searchParameters.setEnzyme(enzymeFactory.getEnzyme(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_ION_MASS_ACCURACY);

            if (temp != null) {
                searchParameters.setFragmentIonAccuracy(new Double(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.PRECURSOR_MASS_TOLERANCE);

            if (temp != null) {
                try {
                    searchParameters.setPrecursorAccuracy(new Double(temp.trim()));
                    idFilter.setMaxMzDeviation(new Double(temp.trim()));
                } catch (Exception e) {
                }
            }

            temp = props.getProperty(IdentificationParametersReader.PRECURSOR_MASS_ACCURACY_UNIT);

            if (temp != null) {
                if (temp.equalsIgnoreCase("ppm")) {
                    searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.PPM);
                    idFilter.setIsPpm(true);
                } else {
                    searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.DA);
                    idFilter.setIsPpm(false);
                }
            }

            temp = props.getProperty(IdentificationParametersReader.MISSED_CLEAVAGES);

            if (temp != null) {
                searchParameters.setnMissedCleavages(new Integer(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.MIN_PEPTIDE_SIZE);

            if (temp != null && temp.length() > 0) {
                try {
                    searchParameters.setPrecursorAccuracy(new Double(temp.trim()));
                    idFilter.setMinPepLength(new Integer(temp.trim()));
                } catch (Exception e) {
                }
            }

            temp = props.getProperty(IdentificationParametersReader.MAX_PEPTIDE_SIZE);

            if (temp != null && temp.length() > 0) {
                try {
                    searchParameters.setPrecursorAccuracy(new Double(temp.trim()));
                    idFilter.setMaxPepLength(new Integer(temp.trim()));
                } catch (Exception e) {
                }
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_ION_TYPE_1);

            if (temp != null && temp.length() > 0) {
                searchParameters.setIonSearched1(temp);
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_ION_TYPE_2);

            if (temp != null && temp.length() > 0) {
                searchParameters.setIonSearched2(temp);
            }


            searchParameters.setParametersFile(searchGUIFile);
            temp = props.getProperty(IdentificationParametersReader.DATABASE_FILE);

            try {
                File file = new File(temp);
                if (file.exists()) {
                    searchParameters.setFastaFile(file);
                    fastaFile = file;
                } else {

                    // try to find it in the same folder as the SearchGUI.properties file
                    if (new File(searchGUIFile.getParentFile(), file.getName()).exists()) {
                        searchParameters.setFastaFile(new File(searchGUIFile.getParentFile(), file.getName()));
                        fastaFile = new File(searchGUIFile.getParentFile(), file.getName());
                    } else {
                        System.err.println("FASTA file \'" + temp + "\' not found.\nPlease locate it manually.");
                    }
                }
            } catch (Exception e) {
                // file not found: use manual input
                e.printStackTrace();
                System.err.println("FASTA file \'" + temp + "\' not found.\nPlease locate it manually.");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println(searchGUIFile.getName() + " not found.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("An error occured while reading " + searchGUIFile.getName() + ".\n"
                    + "Please verify the version compatibility.");
        }
    }

    /**
     * Imports the mgf files from a searchGUI file.
     *
     * @param searchGUIFile a searchGUI file @returns true of the mgf files were
     * imported successfully
     */
    private void importMgfFiles(File searchGUIFile) {

        boolean success = true;

        try {
            BufferedReader br = new BufferedReader(new FileReader(searchGUIFile));
            String line;
            ArrayList<String> names = new ArrayList<String>();
            String missing = "";
            for (File file : spectrumFiles) {
                names.add(file.getName());
            }
            while ((line = br.readLine()) != null) {
                // Skip empty lines.
                line = line.trim();
                if (line.equals("")) {
                } else {
                    try {
                        File newFile = new File(line);
                        if (!names.contains(newFile.getName())) {
                            if (newFile.exists()) {
                                names.add(newFile.getName());
                                spectrumFiles.add(newFile);
                            } else {
                                // try to find it in the same folder as the SearchGUI.properties file
                                if (new File(searchGUIFile.getParentFile(), newFile.getName()).exists()) {
                                    names.add(new File(searchGUIFile.getParentFile(), newFile.getName()).getName());
                                    spectrumFiles.add(new File(searchGUIFile.getParentFile(), newFile.getName()));
                                } else {
                                    missing += newFile.getName() + "\n";
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!missing.equals("")) {
                System.err.println("Input file(s) not found:\n" + missing);
                success = false;
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies the command line start parameters.
     *
     * @return true if the startup was valid
     */
    private static boolean isValidStartup(CommandLine aLine) throws IOException {
        // No params.
        if (aLine.getOptions().length == 0) {
            return false;
        }

        // Required params.
        if (aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_INPUT.id) == null || aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id) == null) {
            System.out.println("input/output not specified!!");
            return false;
        }

        // SearchGUI input folder exists?
        String lFile = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_INPUT.id);
        File lInputFile = new File(lFile);
        if (!lInputFile.exists()) {
            String lMessage = String.format("SearchGUI results folder '%s' does not exist!!", lFile);
            System.out.println(lMessage);
            return false;
        }

        // Folder contains "SearchGUI.properties" file?
        boolean hasSearchGUIProperties = lInputFile.listFiles(new FileFilter() {

            @Override
            public boolean accept(File aFile) {
                return aFile.getName().equals("SearchGUI.properties");
            }
        }).length > 0;

        if (!hasSearchGUIProperties) {
            String lMessage = String.format("SearchGUI results folder '%s' does not contain SearchGUI.properties!!", lFile);
            System.out.println(lMessage);
            return false;
        }


        // if output given, does it exist? if not, make it!
        String lOutputFileName = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id);
        if (lOutputFileName != null) {
            File lOutputFile = new File(lOutputFileName);
            if (!lOutputFile.exists()) {
                boolean lNewFile = lOutputFile.createNewFile();
                if (!lNewFile) {
                    String lMessage = String.format("Failed to create output folder '%s'!!", lOutputFile);
                    throw new IOException(lMessage);
                }

            }
        }


        // FDR is a number?
        ArrayList<String> aFDRs = new ArrayList<String>();
        if (aLine.hasOption(PeptideShakerCLIParams.FDR_LEVEL_PSM.id)) {
            aFDRs.add(aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL_PSM.id));
        }
        if (aLine.hasOption(PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.id)) {
            aFDRs.add(aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.id));
        }
        if (aLine.hasOption(PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.id)) {
            aFDRs.add(aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.id));
        }
        for (String lFDR : aFDRs) {
            double lFDRNumber = 0;
            try {
                // FDR is a number?
                lFDRNumber = Double.parseDouble(lFDR);
            } catch (NumberFormatException e) {
                System.out.println(String.format("FDR '%s' not a number!!", lFDR));
            }

            if (lFDRNumber <= 0) {
                // FDR is larger then 0?
                System.out.println(String.format("FDR '%f' is not a valid FDR!!", lFDRNumber));
                return false;
            }
        }


        // All is fine!
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

            createOptionsCLI(lOptions);

            BasicParser parser = new BasicParser();

            CommandLine line = null;
            line = parser.parse(lOptions, args);

            if (!isValidStartup(line)) {
                HelpFormatter formatter = new HelpFormatter();

                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print("PeptideShaker-CLI\n");

                lPrintWriter.print(getHeader());

                lPrintWriter.print("\nOptions:\n");
                formatter.printOptions(lPrintWriter, 200, lOptions, 0, 0);

                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                System.out.println("PeptideShaker-CLI startup parameters ok!");

                PeptideShakerCLIInputBean lCLIBean = new PeptideShakerCLIInputBean(line);
                PeptideShakerCLI lPeptideShakerCLI = new PeptideShakerCLI(lCLIBean);
                lPeptideShakerCLI.call();
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "PeptideShakerCLI{"
                + "annotationPreferences=" + annotationPreferences
                + ", iWaitingHandler=" + iWaitingHandler
                + ", fastaFile=" + fastaFile
                + ", iPeptideShakerCLIInputBean=" + iCLIInputBean
                + ", experiment=" + experiment
                + ", sample=" + sample
                + ", idFiles=" + idFiles
                + ", searchParametersFiles=" + searchParametersFiles
                + ", spectrumFiles=" + spectrumFiles
                + ", modificationFiles=" + modificationFiles
                + ", ptmFactory=" + ptmFactory
                + ", enzymeFactory=" + enzymeFactory
                + ", searchParameters=" + searchParameters
                + ", idFilter=" + idFilter
                + ", projectDetails=" + projectDetails
                + '}';
    }
}
