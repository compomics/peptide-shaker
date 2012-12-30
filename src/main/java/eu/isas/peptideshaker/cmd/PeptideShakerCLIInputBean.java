package eu.isas.peptideshaker.cmd;

import com.compomics.software.CommandLineUtils;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.SearchParameters;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Kenny Helsens
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
    private ArrayList<File> spectrumFiles = null;
    /**
     * The identification files.
     */
    private ArrayList<File> idFiles = null;
    /**
     * PeptideShaker output file.
     */
    private File output = null;
    /**
     * csv output directory.
     */
    private File csvDirectory = null;
    /**
     * PeptideShaker pride output file.
     */
    private File prideFile = null;
    /**
     * PSM FDR used for validation.
     */
    private double psmFDR = 1.0;
    /**
     * PSM FLR used for modification localization.
     */
    private double psmFLR = 1.0;
    /**
     * Peptide FDR used for validation.
     */
    private double peptideFDR = 1.0;
    /**
     * Protein FDR used for validation.
     */
    private double proteinFDR = 1.0;
    /**
     * The identification parameters used for the search.
     */
    private SearchParameters identificationParameters = null;
    /**
     * boolean indicating whether a waiting dialog shall be used
     */
    private boolean gui = false;
    /**
     * boolean indicating whether the results shall be displayed in the GUI
     * after processing
     */
    private boolean displayResults = false;

    /**
     * Construct a PeptideShakerCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public PeptideShakerCLIInputBean(CommandLine aLine) throws FileNotFoundException, IOException, ClassNotFoundException {

        iExperimentID = aLine.getOptionValue(PeptideShakerCLIParams.EXPERIMENT.id);
        iSampleID = aLine.getOptionValue(PeptideShakerCLIParams.SAMPLE.id);

        if (aLine.hasOption(PeptideShakerCLIParams.REPLICATE.id)) {
            replicate = new Integer(aLine.getOptionValue(PeptideShakerCLIParams.REPLICATE.id));
        }

        String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
        spectrumFiles = getSpectrumFiles(filesTxt);

        filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
        idFiles = getIdentificationFiles(filesTxt);

        output = new File(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id));

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id)) {
            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id).trim();
            File testFile = new File(filesTxt);
            if (testFile.exists()) {
                csvDirectory = testFile;

            } else {
                throw new FileNotFoundException(filesTxt + " not found.");
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id)) {
            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id);
            File testFile = new File(filesTxt);
            prideFile = testFile;
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FDR.id)) {
            psmFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PSM_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FLR.id)) {
            psmFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PSM_FLR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDE_FDR.id)) {
            peptideFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDE_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FDR.id)) {
            proteinFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.SEARCH_PARAMETERS.id)) {

            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SEARCH_PARAMETERS.id);
            File testFile = new File(filesTxt);
            if (testFile.exists()) {
                identificationParameters = SearchParameters.getIdentificationParameters(testFile);
            } else {
                throw new FileNotFoundException(filesTxt + " not found.");
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.GUI.id)) {
            String guiOption = aLine.getOptionValue(PeptideShakerCLIParams.GUI.id);
            if (guiOption.trim().equals("1")) {
                gui = true;
            }
        }
        if (aLine.hasOption(PeptideShakerCLIParams.DISPLAY_RESULTS.id)) {
            String guiOption = aLine.getOptionValue(PeptideShakerCLIParams.DISPLAY_RESULTS.id);
            if (guiOption.trim().equals("1")) {
                displayResults = true;
            }
        }

    }

    /**
     * Empty constructor for API usage via other tools.
     */
    public PeptideShakerCLIInputBean() {
    }

    /**
     * Returns the directory for csv output. Null if not set.
     *
     * @return the directory for csv output
     */
    public File getCsvDirectory() {
        return csvDirectory;
    }

    /**
     * Sets the directory for csv output.
     *
     * @param csvDirectory the directory for csv output
     */
    public void setCsvDirectory(File csvDirectory) {
        this.csvDirectory = csvDirectory;
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
     * Returns the PSM FDR in percent.
     *
     * @return the PSM FDR
     */
    public double getPsmFDR() {
        return psmFDR;
    }

    /**
     * Sets the PSM FDR in percent.
     *
     * @param psmFDR the PSM FDR
     */
    public void setPsmFDR(double psmFDR) {
        this.psmFDR = psmFDR;
    }

    /**
     * Sets the PSM FLR in percent.
     *
     * @return the PSM FLR
     */
    public double getiPsmFLR() {
        return psmFLR;
    }

    /**
     * Sets the PSM FLR in percent.
     *
     * @param psmFLR the PSM FLR
     */
    public void setPsmFLR(double psmFLR) {
        this.psmFLR = psmFLR;
    }

    /**
     * Returns the peptide FDR in percent.
     *
     * @return the peptide FDR
     */
    public double getPeptideFDR() {
        return peptideFDR;
    }

    /**
     * Sets the peptide FDR in percent.
     *
     * @param peptideFDR the peptide FDR
     */
    public void setPeptideFDR(double peptideFDR) {
        this.peptideFDR = peptideFDR;
    }

    /**
     * Returns the protein FDR in percent.
     *
     * @return the protein FDR
     */
    public double getProteinFDR() {
        return proteinFDR;
    }

    /**
     * Sets the protein FDR in percent.
     *
     * @param proteinFDR the protein FDR
     */
    public void setProteinFDR(double proteinFDR) {
        this.proteinFDR = proteinFDR;
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
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public SearchParameters getIdentificationParameters() {
        return identificationParameters;
    }

    /**
     * Sets the identification parameters.
     *
     * @param identificationParameters the identification parameters
     */
    public void setIdentificationParameters(SearchParameters identificationParameters) {
        this.identificationParameters = identificationParameters;
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
        return CommandLineUtils.getFiles(optionInput, extentions);
    }

    /**
     * Indicates whether a gui shall be used to display the progress
     *
     * @return a boolean indicating whether a gui shall be used to display the
     * progress
     */
    public boolean isGUI() {
        return gui;
    }

    /**
     * Indicates whether the results shall be displayed in the gui after
     * processing
     *
     * @return a boolean indicating whether the results shall be displayed in
     * the gui after processing
     */
    public boolean displayResults() {
        return displayResults;
    }
}
