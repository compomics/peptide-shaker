package eu.isas.peptideshaker.cmd;

import com.compomics.software.cli.CommandLineUtils;
import com.compomics.cli.identification_parameters.IdentificationParametersInputBean;
import com.compomics.util.parameters.peptide_shaker.ProjectType;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerCLIInputBean {

    /**
     * The project reference.
     */
    private String reference = null;
    /**
     * The project type.
     */
    private ProjectType projectType = ProjectType.protein;
    /**
     * The spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<>();
    /**
     * The FASTA file.
     */
    private File fastaFile = null;
    /**
     * The identification files.
     */
    private ArrayList<File> idFiles = null;
    /**
     * PeptideShaker output file.
     */
    private File output = null;
    /**
     * PeptideShaker pride output file.
     */
    private File prideFile = null;
    /**
     * Boolean indicating whether a waiting dialog shall be used.
     */
    private boolean gui = false;
    /**
     * The identification parameters options.
     */
    private IdentificationParametersInputBean identificationParametersInputBean;
    /**
     * The identification parameters file.
     */
    private File identificationParametersFile;
    /**
     * The follow up options .
     */
    private FollowUpCLIInputBean followUpCLIInputBean;
    /**
     * The report export options.
     */
    private ReportCLIInputBean reportCLIInputBean;
    /**
     * The mzid export options.
     */
    private MzidCLIInputBean mzidCLIInputBean;
    /**
     * The path settings.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;
    /**
     * File where to export the zipped folder.
     */
    private File zipExport = null;
    /**
     * Boolean that indicates whether mgf file/s should be written apart from
     * the zip file
     */
    private boolean mgfExternalExportWhenZip = false;
    /**
     * The number of threads to use.
     */
    private Integer nThreads = null;

    /**
     * Construct a PeptideShakerCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     *
     * @throws IOException thrown of IOException occurs
     * @throws ClassNotFoundException thrown of ClassNotFoundException occurs
     */
    public PeptideShakerCLIInputBean(CommandLine aLine) throws IOException, ClassNotFoundException {

        // reference
        reference = aLine.getOptionValue(PeptideShakerCLIParams.REFERENCE.id);

        // project type
        if (aLine.hasOption(PeptideShakerCLIParams.PROJECT_TYPE.id)) {

            String optionValue = aLine.getOptionValue(PeptideShakerCLIParams.PROJECT_TYPE.id);

            int index;

            try {

                index = Integer.parseInt(optionValue);

            } catch (Exception e) {

                throw new IllegalArgumentException("Input for " + PeptideShakerCLIParams.PROJECT_TYPE.id + " (" + optionValue + ") could not be parsed as an integer.");

            }

            projectType = ProjectType.getProjectType(index);

        }

        if (aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            spectrumFiles = getSpectrumFiles(filesTxt);
        }

        if (aLine.hasOption(PeptideShakerCLIParams.FASTA_FILE.id)) {
            fastaFile = new File(aLine.getOptionValue(PeptideShakerCLIParams.FASTA_FILE.id));
        }

        String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
        idFiles = getIdentificationFiles(filesTxt);

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)) {
            output = new File(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.GUI.id)) {
            String guiOption = aLine.getOptionValue(PeptideShakerCLIParams.GUI.id);
            if (guiOption.trim().equals("1")) {
                gui = true;
            }
        }

        // zipped export
        if (aLine.hasOption(PeptideShakerCLIParams.ZIP.id)) {
            zipExport = new File(aLine.getOptionValue(PeptideShakerCLIParams.ZIP.id));
        }

        // mgf exported out of the zip file
        if (aLine.hasOption(PeptideShakerCLIParams.OUTPUT_MGF.id)) {
            String mgfOption = aLine.getOptionValue(PeptideShakerCLIParams.OUTPUT_MGF.id);
            if (mgfOption.trim().equals("1")) {
                mgfExternalExportWhenZip = true;
            }
        }

        // n threads
        if (aLine.hasOption(PeptideShakerCLIParams.THREADS.id)) {
            nThreads = Integer.valueOf(aLine.getOptionValue(PeptideShakerCLIParams.THREADS.id));
        }

        followUpCLIInputBean = new FollowUpCLIInputBean(aLine);
        reportCLIInputBean = new ReportCLIInputBean(aLine);
        mzidCLIInputBean = new MzidCLIInputBean(aLine);
        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
        identificationParametersInputBean = new IdentificationParametersInputBean(aLine);
    }

    /**
     * Empty constructor for API usage via other tools.
     */
    public PeptideShakerCLIInputBean() {
    }

    /**
     * Returns the project type.
     * 
     * @return the project type
     */
    public ProjectType getProjectType() {
        return projectType;
    }

    /**
     * Returns the file where to export the project as zip file. Null if not
     * set.
     *
     * @return the file where to export the project as zip file
     */
    public File getZipExport() {
        return zipExport;
    }

    /**
     * When exporting the project to a zip file, returns whether mgf file/s
     * should be written apart from it, into the same folder.
     *
     * @return whether mgf file/s should be written apart from the zip file
     */
    public boolean getMgfExport() {
        return mgfExternalExportWhenZip;
    }

    /**
     * Returns the experiment name.
     *
     * @return the experiment name
     */
    public String getExperimentID() {
        return reference;
    }

    /**
     * Sets the experiment name.
     *
     * @param experimentID the experiment name
     */
    public void setExperimentID(String experimentID) {
        this.reference = experimentID;
    }

    /**
     * Returns the psdb output file. Null if not set.
     *
     * @return the psdb output file
     */
    public File getOutput() {
        return output;
    }

    /**
     * Sets the psdb output file.
     *
     * @param output the psdb output file
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * Returns the identification files.
     *
     * @return the identification files
     */
    public ArrayList<File> getIdFiles() {
        return idFiles;
    }

    /**
     * Sets the identification files.
     *
     * @param idFiles the identification files
     */
    public void setIdFiles(ArrayList<File> idFiles) {
        this.idFiles = idFiles;
    }

    /**
     * Returns the pride file.
     *
     * @return the pride file
     */
    public File getPrideFile() {
        return prideFile;
    }

    /**
     * Sets the pride file.
     *
     * @param prideFile the pride file
     */
    public void setPrideFile(File prideFile) {
        this.prideFile = prideFile;
    }

    /**
     * Returns the spectrum files.
     *
     * @return the spectrum files
     */
    public ArrayList<File> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Sets the spectrum files.
     *
     * @param spectrumFiles the spectrum files
     */
    public void setSpectrumFiles(ArrayList<File> spectrumFiles) {
        this.spectrumFiles = spectrumFiles;
    }

    /**
     * Returns the FASTA file.
     *
     * @return the FASTA file
     */
    public File getFastaFile() {
        return fastaFile;
    }

    /**
     * Sets the FASTA file.
     *
     * @param fastaFile the FASTA file
     */
    public void setFastaFile(File fastaFile) {
        this.fastaFile = fastaFile;
    }

    /**
     * Returns a list of spectrum files as imported from the command line
     * option.
     *
     * @param optionInput the command line option
     * @return a list of file candidates
     * @throws FileNotFoundException exception thrown whenever a file is not
     * found
     */
    public static ArrayList<File> getSpectrumFiles(String optionInput) throws FileNotFoundException {

        ArrayList<String> extensions = new ArrayList<>();

        extensions.add(".mgf");
        extensions.add(".mzML");
        extensions.add(".cms");

        return CommandLineUtils.getFiles(optionInput, extensions);
    }

    /**
     * Returns a list of identification files as imported from the command line
     * option.
     *
     * @param optionInput the command line option
     * @return a list of file candidates
     * @throws FileNotFoundException exception thrown whenever a file is not
     * found
     */
    public static ArrayList<File> getIdentificationFiles(String optionInput) throws FileNotFoundException {

        ArrayList<String> extensions = new ArrayList<>();

        extensions.add(".dat");
        extensions.add(".omx");
        extensions.add(".t.xml");
        extensions.add(".pep.xml");
        extensions.add(".mzid");
        extensions.add(".csv");
        extensions.add(".res");
        extensions.add(".txt");
        extensions.add(".tags");
        extensions.add(".psm");
        extensions.add(".gz");
        extensions.add(".zip");

        return CommandLineUtils.getFiles(optionInput, extensions);
    }

    /**
     * Indicates whether a GUI shall be used to display the progress.
     *
     * @return a boolean indicating whether a GUI shall be used to display the
     * progress
     */
    public boolean isGUI() {
        return gui;
    }

    /**
     * Returns the follow-up options required.
     *
     * @return the follow-up options required
     */
    public FollowUpCLIInputBean getFollowUpCLIInputBean() {
        return followUpCLIInputBean;
    }

    /**
     * Returns the report export options required.
     *
     * @return the report export options required
     */
    public ReportCLIInputBean getReportCLIInputBean() {
        return reportCLIInputBean;
    }

    /**
     * Returns the mzid export options required.
     *
     * @return the mzid export options required
     */
    public MzidCLIInputBean getMzidCLIInputBean() {
        return mzidCLIInputBean;
    }

    /**
     * Returns the path settings provided by the user.
     *
     * @return the path settings provided by the user
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }

    /**
     * Returns the identification parameters provided by the user.
     *
     * @return the identification parameters provided by the user
     */
    public IdentificationParametersInputBean getIdentificationParametersInputBean() {
        return identificationParametersInputBean;
    }

    /**
     * Returns the identification parameters file.
     *
     * @return the identification parameters file
     */
    public File getIdentificationParametersFile() {
        return identificationParametersFile;
    }

    /**
     * Returns the number of threads to use.
     *
     * @return the number of threads to use
     */
    public Integer getnThreads() {
        return nThreads;
    }

    /**
     * Verifies the command line start parameters.
     *
     * @param aLine the command line to validate
     *
     * @return true if the startup was valid
     *
     * @throws IOException if the spectrum file(s) are not found
     */
    public static boolean isValidStartup(CommandLine aLine) throws IOException {

        if (aLine.getOptions().length == 0) {
            System.out.println("\nMandatory parameters not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.REFERENCE.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.REFERENCE.id)).equals("")) {
            System.out.println("\nProject reference not specified.\n");
            return false;
        }

        if (aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            ArrayList<File> idFiles = PeptideShakerCLIInputBean.getSpectrumFiles(filesTxt);
            if (idFiles.isEmpty()) {
                System.out.println("\nNo spectrum file found for command line input " + filesTxt + ".\n");
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

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)) {
            if (((String) aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)).equals("")) {
                System.out.println("\nOutput file cannot be empty.\n");
                return false;
            } else {
                String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id);
                File testFile = new File(filesTxt.trim());
                File parentFolder = testFile.getParentFile();
                if (parentFolder == null) {
                    System.out.println("\nDestination folder not found. Please provide the complete path to the PeptideShaker output file.\n");
                    return false;
                } else if (!parentFolder.exists() && !parentFolder.mkdirs()) {
                    System.out.println("\nDestination folder \'" + parentFolder.getPath() + "\' not found and cannot be created. Make sure that PeptideShaker has the right to write in the destination folder.\n");
                    return false;
                }
            }
        }

        // if the zip option is used, the out option is mandatory
        if (aLine.hasOption(PeptideShakerCLIParams.ZIP.id)
                && !aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)) {
            System.out.println("\nThe out option is mandatory when using the zip option.\n");
            return false;
        }

//        // Check the identification parameters
//        if (!IdentificationParametersInputBean.isValidStartup(aLine, false)) { // @TODO: ok to add?
//            return false;
//        }
        return true;
    }
}
