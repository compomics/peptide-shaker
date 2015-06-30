package eu.isas.peptideshaker.cmd;

import com.compomics.software.CommandLineUtils;
import com.compomics.util.experiment.identification.SearchParameters;
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
     * PSM FDR used for validation.
     */
    private double psmFDR = 1.0;
    /**
     * Peptide FDR used for validation.
     */
    private double peptideFDR = 1.0;
    /**
     * Protein FDR used for validation.
     */
    private double proteinFDR = 1.0;
    /**
     * The minimum confidence required for a protein to be included in the
     * average molecular weight analysis in the Fractions tab.
     */
    private Double proteinConfidenceMwPlots = 95.0;
    /**
     * The PTM localization score to use.
     */
    private PtmScore ptmScore = null;
    /**
     * The PTM score threshold.
     */
    private Double ptmScoreThreshold = null;
    /**
     * Boolean indicating whether neutral losses shall be accounted in the
     * calculation of the A-score.
     */
    private boolean ptmScoreNeutralLosses = false;
    /**
     * The species to use for the gene mappings.
     */
    private String species = null;
    /**
     * The species type to use for the gene mappings.
     */
    private String speciesType = null;
    /**
     * If true the tool will always check the Ensembl version for the selected
     * species and try to update if the version is outdated.
     */
    private boolean autoUpdateSpecies = false;
    /**
     * The minimal peptide length allowed.
     */
    private int minPepLength = 4;
    /**
     * The maximal peptide length allowed.
     */
    private int maxPepLength = 30;
    /**
     * The maximal m/z deviation allowed.
     */
    private Double maxMassDeviation = null;
    /**
     * Boolean indicating the unit of the allowed m/z deviation (true: ppm,
     * false: Da).
     */
    private boolean maxMassDeviationIsPpm = true;
    /**
     * Boolean indicating whether peptides presenting unknown PTMs should be
     * ignored.
     */
    private boolean excludeUnknownPtm = true;
    /**
     * The identification parameters used for the search.
     */
    private SearchParameters searchParameters = null;
    /**
     * Boolean indicating whether a waiting dialog shall be used.
     */
    private boolean gui = false;
    /**
     * The follow up options chosen.
     */
    private FollowUpCLIInputBean followUpCLIInputBean;
    /**
     * The report export options chosen.
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

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FDR.id)) {
            psmFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PSM_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDE_FDR.id)) {
            peptideFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDE_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FDR.id)) {
            proteinFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PTM_SCORE.id)) {
            int scoreId = new Integer(aLine.getOptionValue(PeptideShakerCLIParams.PTM_SCORE.id));
            ptmScore = PtmScore.getScore(scoreId);
            if (aLine.hasOption(PeptideShakerCLIParams.PTM_THRESHOLD.id)) {
                ptmScoreThreshold = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PTM_THRESHOLD.id));
            }
            if (aLine.hasOption(PeptideShakerCLIParams.SCORE_NEUTRAL_LOSSES.id)) {
                String aScoreNeutralLossesOption = aLine.getOptionValue(PeptideShakerCLIParams.SCORE_NEUTRAL_LOSSES.id);
                if (aScoreNeutralLossesOption.trim().equals("0")) {
                    ptmScoreNeutralLosses = false;
                } else if (aScoreNeutralLossesOption.trim().equals("1")) {
                    ptmScoreNeutralLosses = true;
                } else {
                    throw new IllegalArgumentException("Unknown value \'" + ptmScoreNeutralLosses + "\' for " + PeptideShakerCLIParams.SCORE_NEUTRAL_LOSSES.id + ".");
                }
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FRACTION_MW_CONFIDENCE.id)) {
            proteinConfidenceMwPlots = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FRACTION_MW_CONFIDENCE.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.MIN_PEPTIDE_LENGTH.id)) {
            minPepLength = Integer.parseInt(aLine.getOptionValue(PeptideShakerCLIParams.MIN_PEPTIDE_LENGTH.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.MAX_PEPTIDE_LENGTH.id)) {
            maxPepLength = Integer.parseInt(aLine.getOptionValue(PeptideShakerCLIParams.MAX_PEPTIDE_LENGTH.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.MAX_PRECURSOR_ERROR.id)) {
            maxMassDeviation = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.MAX_PRECURSOR_ERROR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.MAX_PRECURSOR_ERROR_TYPE.id)) {
            String tempMaxPrecursorErrorType = aLine.getOptionValue(PeptideShakerCLIParams.MAX_PRECURSOR_ERROR_TYPE.id);
            if (tempMaxPrecursorErrorType.trim().equals("0")) {
                maxMassDeviationIsPpm = true;
            } else if (tempMaxPrecursorErrorType.trim().equals("1")) {
                maxMassDeviationIsPpm = false;
            } else {
                throw new IllegalArgumentException("Unknown value \'" + maxMassDeviationIsPpm + "\' for " + PeptideShakerCLIParams.MAX_PRECURSOR_ERROR_TYPE.id + ".");
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.EXCLUDE_UNKNOWN_PTMS.id)) {
            String tempExcludeUnknownPtms = aLine.getOptionValue(PeptideShakerCLIParams.EXCLUDE_UNKNOWN_PTMS.id);
            if (tempExcludeUnknownPtms.trim().equals("0")) {
                excludeUnknownPtm = true;
            } else if (tempExcludeUnknownPtms.trim().equals("1")) {
                excludeUnknownPtm = false;
            } else {
                throw new IllegalArgumentException("Unknown value \'" + ptmScoreNeutralLosses + "\' for " + PeptideShakerCLIParams.EXCLUDE_UNKNOWN_PTMS.id + ".");
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.IDENTIFICATION_PARAMETERS.id)) {
            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_PARAMETERS.id);
            File tempSettingsFile = new File(filesTxt);
            if (tempSettingsFile.exists()) {
                searchParameters = SearchParameters.getIdentificationParameters(tempSettingsFile);
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

        // species and species type
        if (aLine.hasOption(PeptideShakerCLIParams.SPECIES.id)) {
            species = aLine.getOptionValue(PeptideShakerCLIParams.SPECIES.id); // @TODO: check that it's a valid species??
        }
        if (aLine.hasOption(PeptideShakerCLIParams.SPECIES_TYPE.id)) {
            speciesType = aLine.getOptionValue(PeptideShakerCLIParams.SPECIES_TYPE.id); // @TODO: check that it's a valid species type??
        }
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
     * Returns the minimum confidence required for a protein to be included in
     * the average molecular weight analysis in the Fractions tab.
     *
     * @return the minimum confidence
     */
    public Double getProteinConfidenceMwPlots() {
        if (proteinConfidenceMwPlots == null) {
            return 95.0;
        }
        return proteinConfidenceMwPlots;
    }

    /**
     * Sets the minimum confidence required for a protein to be included in the
     * average molecular weight analysis in the Fractions tab..
     *
     * @param proteinConfidenceMwPlots minimum confidence
     */
    public void setProteinConfidenceMwPlots(Double proteinConfidenceMwPlots) {
        this.proteinConfidenceMwPlots = proteinConfidenceMwPlots;
    }

    /**
     * Indicates whether the A-score calculation should take neutral losses into
     * account.
     *
     * @return a boolean indicating whether the A-score calculation should take
     * neutral losses into account
     */
    public boolean isaScoreNeutralLosses() {
        return ptmScoreNeutralLosses;
    }

    /**
     * Sets whether the A-score calculation should take neutral losses into
     * account.
     *
     * @param aScoreNeutralLosses a boolean indicating whether the A-score
     * calculation should take neutral losses into account
     */
    public void setaScoreNeutralLosses(boolean aScoreNeutralLosses) {
        this.ptmScoreNeutralLosses = aScoreNeutralLosses;
    }

    /**
     * Returns a boolean indicating whether unknown PTMs shall be excluded.
     *
     * @return a boolean indicating whether unknown PTMs shall be excluded
     */
    public boolean excludeUnknownPTMs() {
        return excludeUnknownPtm;
    }

    /**
     * Set whether unknown PTMs shall be excluded.
     *
     * @param unknownPtm whether unknown PTMs shall be excluded
     */
    public void setExcludeUnknownPTMs(boolean unknownPtm) {
        this.excludeUnknownPtm = unknownPtm;
    }

    /**
     * Indicates whether the mass tolerance is in ppm (true) or Dalton (false).
     *
     * @return a boolean indicating whether the mass tolerance is in ppm (true)
     * or Dalton (false)
     */
    public boolean isMaxMassDeviationPpm() {
        return maxMassDeviationIsPpm;
    }

    /**
     * Sets whether the mass tolerance is in ppm (true) or Dalton (false).
     *
     * @param isPpm a boolean indicating whether the mass tolerance is in ppm
     * (true) or Dalton (false)
     */
    public void setMaxMassDeviationIsPpm(boolean isPpm) {
        this.maxMassDeviationIsPpm = isPpm;
    }

    /**
     * Returns the maximal m/z deviation allowed.
     *
     * @return the maximal mass deviation allowed
     */
    public Double getMaxMzDeviation() {
        return maxMassDeviation;
    }

    /**
     * Sets the maximal m/z deviation allowed.
     *
     * @param maxMzDeviation the maximal mass deviation allowed
     */
    public void setMaxMzDeviation(Double maxMzDeviation) {
        this.maxMassDeviation = maxMzDeviation;
    }

    /**
     * Returns the maximal peptide length allowed.
     *
     * @return the maximal peptide length allowed
     */
    public int getMaxPepLength() {
        return maxPepLength;
    }

    /**
     * Sets the maximal peptide length allowed.
     *
     * @param maxPepLength the maximal peptide length allowed
     */
    public void setMaxPepLength(int maxPepLength) {
        this.maxPepLength = maxPepLength;
    }

    /**
     * Returns the maximal peptide length allowed.
     *
     * @return the maximal peptide length allowed
     */
    public int getMinPepLength() {
        return minPepLength;
    }

    /**
     * Sets the maximal peptide length allowed.
     *
     * @param minPepLength the maximal peptide length allowed
     */
    public void setMinPepLength(int minPepLength) {
        this.minPepLength = minPepLength;
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
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    /**
     * Sets the identification parameters.
     *
     * @param searchParameters the identification parameters
     */
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
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
     * Returns the species.
     *
     * @return the species
     */
    public String getSpecies() {
        return species;
    }

    /**
     * Sets the species.
     *
     * @param species the species to set
     */
    public void setSpecies(String species) {
        this.species = species;
    }

    /**
     * Returns the species type.
     *
     * @return the species type
     */
    public String getSpeciesType() {
        return speciesType;
    }

    /**
     * Sets the species type.
     *
     * @param speciesType the species to set
     */
    public void setSpeciesType(String speciesType) {
        this.speciesType = speciesType;
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
     * Returns the PTM score to use.
     *
     * @return the PTM score to use
     */
    public PtmScore getPtmScore() {
        return ptmScore;
    }

    /**
     * Returns the PTM score threshold.
     *
     * @return the PTM score threshold
     */
    public Double getPtmScoreThreshold() {
        return ptmScoreThreshold;
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
     * Returns the number of threads to use.
     *
     * @return the number of threads to use
     */
    public Integer getnThreads() {
        return nThreads;
    }
}
