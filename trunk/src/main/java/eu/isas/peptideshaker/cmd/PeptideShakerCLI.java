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
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SearchParameters;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * A Command line interface to run PeptideShaker on a SearchGUI output folder.
 *
 * @author Kenny Helsens
 */
public class PeptideShakerCLI implements Runnable {

     /**
     * The xml file containing the enzymes.
     */
    private static final String ENZYME_FILE = "resources/conf/peptideshaker_enzymes.xml";
    /**
     * Modification file.
     */
    private final String MODIFICATIONS_FILE = "resources/conf/peptideshaker_mods.xml";
    /**
     * User modification file.
     */
    private final String USER_MODIFICATIONS_FILE = "resources/conf/peptideshaker_usermods.xml";
    /**
     * User preferences file.
     */
    private final String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences.cpf";

    /**
     * The Progress messaging handler reports the status throughout all PeptideShaker processes
     */
    private WaitingHandlerCLIImpl iWaitingHandler = new WaitingHandlerCLIImpl();

    /**
     * The list of spectrum files
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();

    /**
     * The identification filter used for this project.
     */
    private IdFilter idFilter = new IdFilter();


    /**
     * The fasta file.
     */
    private File fastaFile = null;

    /**
     * The CLI input parameters to start PeptideShaker from command line
     */
    private final PeptideShakerCLIInputBean iPeptideShakerCLIInputBean;

    /**
     * The list of identification files
     */
    private ArrayList<File> idFiles = new ArrayList<File>();
    /**
     * The parameters files found
     */
    private ArrayList<File> searchParametersFiles = new ArrayList<File>();

    /**
     * The xml modification files found
     */
    private ArrayList<File> modificationFiles = new ArrayList<File>();


    private SearchParameters searchParameters = new SearchParameters();

    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The experiment conducted.
     */
    private MsExperiment experiment = null;
    /**
     * The sample analyzed.
     */
    private Sample sample;
    /**
     * The replicate number.
     */
    private int replicateNumber;

    /**
     * The annotation preferences.
     */
    private AnnotationPreferences annotationPreferences = new AnnotationPreferences();

    /**
     * The project details.
     */
    private ProjectDetails projectDetails = null;

    /**
     * Construct a new PeptideShakerCLI runnable from a PeptideShakerCLI Bean.
     * When initialization is successful, calling "run" will start PeptideShaker and write the output files when finished.
     */
    public PeptideShakerCLI(PeptideShakerCLIInputBean aPeptideShakerCLIInputBean) {
        iPeptideShakerCLIInputBean = aPeptideShakerCLIInputBean;

        loadEnzymes();

        importSearchGUIFiles();

        File lInputFolder = iPeptideShakerCLIInputBean.getInput();
        importSearchParameters(searchParametersFiles.get(0));

        File lSearchGUIMGFFiles = new File(lInputFolder, NewDialog.SEARCHGUI_INPUT);
        importMgfFiles(lSearchGUIMGFFiles);


    }

    private void importSearchGUIFiles() {

        FileImporter.setCLIMode(true);

        File lInputFolder = iPeptideShakerCLIInputBean.getInput();

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
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     */
    public static void main(String[] args) {
        try {
            Options lOptions = new Options();

            createOptions(lOptions);

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
                lPeptideShakerCLI.run();


            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     */
    public void run() {


        experiment = new MsExperiment("peptideshaker_cli");
        sample = new Sample("peptideshaker_cli_sample");
        int lReplicaNumber = 1;

        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(lReplicaNumber));
        experiment.addAnalysisSet(sample, analysisSet);

        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, lReplicaNumber);

        peptideShaker.importFiles(iWaitingHandler, idFilter, idFiles, spectrumFiles, fastaFile, searchParameters, annotationPreferences, projectDetails);
//        peptideShaker.processIdentifications();
//        peptideShaker.validateIdentifications();

        peptideShaker.fdrValidation(iWaitingHandler);
        Metrics lMetrics = peptideShaker.getMetrics();

        IdentificationFeaturesGenerator lIdentificationFeaturesGenerator = new IdentificationFeaturesGenerator(null);
        CsvExporter exporter = new CsvExporter(experiment, sample, 1, searchParameters.getEnzyme(), lIdentificationFeaturesGenerator);

        exporter.exportResults(null, false, iPeptideShakerCLIInputBean.getOutput());

        System.out.println("finished PeptideShaker-CLI");
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
     * @param aOptions Apache Commons CLI Options instance to set the possible parameters that can be passed to PeptideShakerCLI.
     */
    private static void createOptions(Options aOptions) {
        aOptions.addOption(PeptideShakerCLIParams.FDR.id, true, PeptideShakerCLIParams.FDR.description);
        aOptions.addOption(PeptideShakerCLIParams.FDR_LEVEL.id, true, PeptideShakerCLIParams.FDR_LEVEL.description);
        aOptions.addOption(PeptideShakerCLIParams.SEARCH_GUI_RES.id, true, PeptideShakerCLIParams.SEARCH_GUI_RES.description);
        aOptions.addOption(PeptideShakerCLIParams.OUTPUT.id, true, PeptideShakerCLIParams.OUTPUT.description);
    }


    /**
     * Verifies the command line start parameters.
     *
     * @return
     */
    public static boolean isValidStartup(CommandLine aLine) throws IOException {
        // No params.
        if (aLine.getOptions().length == 0) {
            return false;
        }

        // Required params.
        if (aLine.getOptionValue(PeptideShakerCLIParams.SEARCH_GUI_RES.id) == null || aLine.getOptionValue(PeptideShakerCLIParams.OUTPUT.id) == null) {
            System.out.println("input/output not specified!!");
            return false;
        }

        // SearchGUI input folder exists?
        String lFile = aLine.getOptionValue(PeptideShakerCLIParams.SEARCH_GUI_RES.id);
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
        String lOutputFileName = aLine.getOptionValue(PeptideShakerCLIParams.OUTPUT.id);
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

        // Required params for FDR calculation.
        if (aLine.getOptionValue(PeptideShakerCLIParams.FDR.id) == null || aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL.id) == null) {
            System.out.println("FDR and FDR level not specified appropriately!!");
            return false;
        }


        // FDR is a number?
        String lFDR = aLine.getOptionValue(PeptideShakerCLIParams.FDR.id);
        Double lFDRNumber;
        try {
            lFDRNumber = Double.parseDouble(lFDR);
        } catch (NumberFormatException e) {
            System.out.println(String.format("FDR '%s' not a number!!", lFDR));
            return false;
        }

        // FDR is larger then 0?
        if (lFDRNumber <= 0) {
            System.out.println(String.format("FDR '%f' is not a valid FDR!!", lFDRNumber));
            return false;
        }

        // FDR level is known?
        String lFDRLevel = aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL.id);
        if (!(
                lFDRLevel.equals(PeptideShakerCLIParams.FDR_LEVEL_PSM.id) ||
                        lFDRLevel.equals(PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.id) ||
                        lFDRLevel.equals(PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.id))
                ) {

            System.out.println("FDR level not specified appropriately!!");
            return false;
        }


        // All is fine!
        return true;
    }


    /**
     * Loads the modifications from the modification file.
     */
    private void resetPtmFactory() {

        // reset ptm factory
        ptmFactory.reloadFactory();
        ptmFactory = PTMFactory.getInstance();
        try {
            ptmFactory.importModifications(new File(MODIFICATIONS_FILE), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            ptmFactory.importModifications(new File(USER_MODIFICATIONS_FILE), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            enzymeFactory.importEnzymes(new File(ENZYME_FILE));
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
    }
    /**
     * Imports the search parameters from a searchGUI file.
     *
     * @param searchGUIFile the selected searchGUI file
     */
    public void importSearchParameters(File searchGUIFile) {

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
    private boolean importMgfFiles(File searchGUIFile) {

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


        return success;
    }

    @Override
    public String toString() {
        return "PeptideShakerCLI{" +
                "idFiles=" + idFiles +
                ", iWaitingHandler=" + iWaitingHandler +
                ", spectrumFiles=" + spectrumFiles +
                ", iPeptideShakerCLIInputBean=" + iPeptideShakerCLIInputBean +
                ", searchParametersFiles=" + searchParametersFiles +
                ", modificationFiles=" + modificationFiles +
                '}';
    }
}

