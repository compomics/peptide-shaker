package eu.isas.peptideshaker.cmd;

import com.compomics.software.CommandLineUtils;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.parameters_cli.IdentificationParametersInputBean;
import com.compomics.util.experiment.identification.ptm.PtmScore;
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
 */
public class PeptideShakerCLIInputBean {

    /**
     * The experiment name.
     */
    private String iExperimentID = null;
    /**
     * The sample name.
     */
    private String iSampleID = null;
    /**
     * The replicate number.
     */
    private int replicate = 0;
    /**
     * The spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
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
     * If true the tool will always check the Ensembl version for the selected
     * species and try to update if the version is outdated.
     */
    private boolean autoUpdateSpecies = false;
    /**
     * Boolean indicating whether a waiting dialog shall be used.
     */
    private boolean gui = false;
    /**
     * The identification parameters options.
     */
    private IdentificationParametersInputBean identificationParametersInputBean;
    /**
     * The follow up options .
     */
    private FollowUpCLIInputBean followUpCLIInputBean;
    /**
     * The report export options.
     */
    private ReportCLIInputBean reportCLIInputBean;
    /**
     * The path settings.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;
    /**
     * File where to export the zipped folder.
     */
    private File zipExport = null;
    /**
     * The number of threads to use.
     */
    private Integer nThreads = null;

    /**
     * Construct a PeptideShakerCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     * @throws ClassNotFoundException thrown of ClassNotFoundException occurs
     */
    public PeptideShakerCLIInputBean(CommandLine aLine) throws FileNotFoundException, IOException, ClassNotFoundException {
        
        iExperimentID = aLine.getOptionValue(PeptideShakerCLIParams.EXPERIMENT.id);
        iSampleID = aLine.getOptionValue(PeptideShakerCLIParams.SAMPLE.id);

        if (aLine.hasOption(PeptideShakerCLIParams.REPLICATE.id)) {
            replicate = new Integer(aLine.getOptionValue(PeptideShakerCLIParams.REPLICATE.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            spectrumFiles = getSpectrumFiles(filesTxt);
        }

        String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
        idFiles = getIdentificationFiles(filesTxt);

        output = new File(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id));

        if (aLine.hasOption(PeptideShakerCLIParams.GUI.id)) {
            String guiOption = aLine.getOptionValue(PeptideShakerCLIParams.GUI.id);
            if (guiOption.trim().equals("1")) {
                gui = true;
            }
        }

        // species auto update
        if (aLine.hasOption(PeptideShakerCLIParams.SPECIES_UPDATE.id)) {
            String guiOption = aLine.getOptionValue(PeptideShakerCLIParams.SPECIES_UPDATE.id);
            if (guiOption.trim().equals("1")) {
                autoUpdateSpecies = true;
            }
        }

        // zipped export
        if (aLine.hasOption(PeptideShakerCLIParams.ZIP.id)) {
            zipExport = new File(aLine.getOptionValue(PeptideShakerCLIParams.ZIP.id));
        }

        // n threads
        if (aLine.hasOption(PeptideShakerCLIParams.THREADS.id)) {
            nThreads = new Integer(aLine.getOptionValue(PeptideShakerCLIParams.THREADS.id));
        }

        followUpCLIInputBean = new FollowUpCLIInputBean(aLine);
        reportCLIInputBean = new ReportCLIInputBean(aLine);
        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
        identificationParametersInputBean = new IdentificationParametersInputBean(aLine);
    }

    /**
     * Empty constructor for API usage via other tools.
     */
    public PeptideShakerCLIInputBean() {
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
     * Returns the experiment name.
     *
     * @return the experiment name
     */
    public String getiExperimentID() {
        return iExperimentID;
    }

    /**
     * Sets the experiment name.
     *
     * @param iExperimentID the experiment name
     */
    public void setiExperimentID(String iExperimentID) {
        this.iExperimentID = iExperimentID;
    }

    /**
     * Returns the cps output file.
     *
     * @return the cps output file
     */
    public File getOutput() {
        return output;
    }

    /**
     * Sets the cps output file.
     *
     * @param output the cps output file
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * Returns the name of the sample.
     *
     * @return the name of the sample
     */
    public String getiSampleID() {
        return iSampleID;
    }

    /**
     * Sets the name of the sample.
     *
     * @param iSampleID the name of the sample
     */
    public void setiSampleID(String iSampleID) {
        this.iSampleID = iSampleID;
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
     * Returns the replicate number.
     *
     * @return the replicate number
     */
    public int getReplicate() {
        return replicate;
    }

    /**
     * Sets the replicate number.
     *
     * @param replicate the replicate number
     */
    public void setReplicate(int replicate) {
        this.replicate = replicate;
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
     * Returns a list of spectrum files as imported from the command line
     * option.
     *
     * @param optionInput the command line option
     * @return a list of file candidates
     * @throws FileNotFoundException exception thrown whenever a file is not
     * found
     */
    public static ArrayList<File> getSpectrumFiles(String optionInput) throws FileNotFoundException {
        ArrayList<String> extentions = new ArrayList<String>();
        extentions.add(".mgf");
        return CommandLineUtils.getFiles(optionInput, extentions);
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
        ArrayList<String> extentions = new ArrayList<String>();
        extentions.add(".dat");
        extentions.add(".omx");
        extentions.add(".t.xml");
        extentions.add(".mzid");
        extentions.add(".csv");
        extentions.add(".txt");
        extentions.add(".pep.xml");
        extentions.add(".zip");
        return CommandLineUtils.getFiles(optionInput, extentions);
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
     * Returns true of the species is to be auto updated to the latest version.
     *
     * @return rue of the species is to be auto updated to the latest version
     */
    public boolean updateSpecies() {
        return autoUpdateSpecies;
    }

    /**
     * Set if the species is to be auto updated to the latest version.
     *
     * @param autoUpdateSpecies auto update species_
     */
    public void setUpdateSpecies(boolean autoUpdateSpecies) {
        this.autoUpdateSpecies = autoUpdateSpecies;
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
     * Returns the number of threads to use.
     *
     * @return the number of threads to use
     */
    public Integer getnThreads() {
        return nThreads;
    }
}
