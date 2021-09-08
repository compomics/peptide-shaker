package eu.isas.peptideshaker.cmd;

import com.compomics.software.cli.CommandLineUtils;
import eu.isas.peptideshaker.followup.ProgenesisExport;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc Vaudel
 */
public class FollowUpCLIInputBean {

    /**
     * The psdb file to work on.
     */
    private File psdbFile = null;
    /**
     * The zip file.
     */
    private File zipFile = null;
    /**
     * Folder where to export recalibrated files.
     */
    private File recalibrationFolder = null;
    /**
     * Parameter for the recalibation.
     */
    private int recalibrationMode = 0;
    /**
     * The folder where to export spectra.
     */
    private File spectrumExportFolder = null;
    /**
     * The spectrum export type index. See SpectrumExporter.ExportType for
     * details.
     */
    private int spectrumExportTypeIndex = 0;
    /**
     * The file where to export protein accessions.
     */
    private File accessionsExportFile = null;
    /**
     * The accessions export type index. see FastaExport.ExportType for details.
     */
    private int accessionsExportTypeIndex = 0;
    /**
     * The file where to export protein details.
     */
    private File proteinSequencesExportFile = null;
    /**
     * The protein details export type index. See FastaExport.ExportType for
     * details.
     */
    private int proteinSequencesExportTypeIndex = 0;
    /**
     * The file where to export the Progenesis file.
     */
    private File progenesisExportFile = null;
    /**
     * The type of Progenesis export.
     */
    private int progenesisExportTypeIndex = 0;
    /**
     * The file for inclusion list export.
     */
    private File inclusionFile;
    /**
     * The format of inclusion list export. Thermo by default.
     */
    private int inclusionFormat = 0;
    /**
     * List of protein inference filters for the inclusion list creation.
     */
    private ArrayList<Integer> inclusionProteinFilter;
    /**
     * List of peptide filters for the inclusion list creation.
     */
    private ArrayList<Integer> inclusionPeptideFilter;
    /**
     * RT window for the inclusion list creation. 20 by default.
     */
    private Double inclusionRtWindow = 20.0;
    /**
     * The Progenesis targeted PTMs.
     */
    private HashSet<String> progenesisTargetedPTMs = new HashSet<>();
    /**
     * The file where to export the proteoforms file.
     */
    private File proteoformsFile = null;
    /**
     * The stem to use for the path to DeepLC peptide files.
     */
    private String deepLcStem = null;
    /**
     * The file to use for the path to ms2pip peptides.
     */
    private File ms2pipFile = null;
    /**
     * The models to export ms2pip config files for.
     */
    private String[] ms2pipModels = new String[]{"CID", "HCD"};
    /**
     * The path settings.
     */
    private final PathSettingsCLIInputBean pathSettingsCLIInputBean;

    /**
     * Construct a FollowUpCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     */
    public FollowUpCLIInputBean(CommandLine aLine) {

        if (aLine.hasOption(FollowUpCLIParams.PSDB_FILE.id)) {
            String file = aLine.getOptionValue(FollowUpCLIParams.PSDB_FILE.id);
            if (file.toLowerCase().endsWith(".psdb")) {
                psdbFile = new File(file);
            } else if (file.toLowerCase().endsWith(".zip")) {
                zipFile = new File(file);
            } else {
                    throw new IllegalArgumentException("Unknown file format \'" + file + "\' for PeptideShaker project input.");
            }
        }
        if (aLine.hasOption(FollowUpCLIParams.RECALIBRATION_FOLDER.id)) {
            recalibrationFolder = new File(aLine.getOptionValue(FollowUpCLIParams.RECALIBRATION_FOLDER.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.RECALIBRATION_MODE.id)) {
            recalibrationMode = Integer.valueOf(aLine.getOptionValue(FollowUpCLIParams.RECALIBRATION_MODE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.SPECTRUM_FOLDER.id)) {
            spectrumExportFolder = new File(aLine.getOptionValue(FollowUpCLIParams.SPECTRUM_FOLDER.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PSM_TYPE.id)) {
            spectrumExportTypeIndex = Integer.valueOf(aLine.getOptionValue(FollowUpCLIParams.PSM_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.ACCESSIONS_FILE.id)) {
            accessionsExportFile = new File(aLine.getOptionValue(FollowUpCLIParams.ACCESSIONS_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.ACCESSIONS_TYPE.id)) {
            accessionsExportTypeIndex = Integer.valueOf(aLine.getOptionValue(FollowUpCLIParams.ACCESSIONS_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.SEQUENCES_FILE.id)) {
            proteinSequencesExportFile = new File(aLine.getOptionValue(FollowUpCLIParams.SEQUENCES_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.SEQUENCES_TYPE.id)) {
            proteinSequencesExportTypeIndex = Integer.valueOf(aLine.getOptionValue(FollowUpCLIParams.SEQUENCES_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PROGENESIS_FILE.id)) {
            progenesisExportFile = new File(aLine.getOptionValue(FollowUpCLIParams.PROGENESIS_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PROGENESIS_TYPE.id)) {
            progenesisExportTypeIndex = Integer.valueOf(aLine.getOptionValue(FollowUpCLIParams.PROGENESIS_TYPE.id));
            if (progenesisExportTypeIndex == ProgenesisExport.ExportType.confident_ptms.index) {
                if (aLine.hasOption(FollowUpCLIParams.PROGENESIS_TARGETED_PTMS.id)) {
                    progenesisTargetedPTMs = getModificationNames(aLine.getOptionValue(FollowUpCLIParams.PROGENESIS_TARGETED_PTMS.id));
                }
            }
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_FILE.id)) {
            inclusionFile = new File(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_FORMAT.id)) {
            inclusionFormat = Integer.valueOf(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_FORMAT.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_PROTEIN_FILTERS.id)) {
            inclusionProteinFilter = CommandLineUtils.getIntegerListFromString(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_PROTEIN_FILTERS.id), ",");
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_PEPTIDE_FILTERS.id)) {
            inclusionPeptideFilter = CommandLineUtils.getIntegerListFromString(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_PEPTIDE_FILTERS.id), ",");
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_RT_WINDOW.id)) {
            inclusionRtWindow = Double.valueOf(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_RT_WINDOW.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PROTEOFORMS_FILE.id)) {
            proteoformsFile = new File(aLine.getOptionValue(FollowUpCLIParams.PROTEOFORMS_FILE.id));
        }
        
        if (aLine.hasOption(FollowUpCLIParams.DEEPLC_FILE.id)) {
            
            deepLcStem = aLine.getOptionValue(FollowUpCLIParams.DEEPLC_FILE.id);
            
        }
        
        if (aLine.hasOption(FollowUpCLIParams.MS2PIP_FILE.id)) {
            
            String path = aLine.getOptionValue(FollowUpCLIParams.MS2PIP_FILE.id);
            
            if (!path.endsWith(".gz")) {
                
                path = path + ".gz";
                
            }
            
            ms2pipFile = new File(path);
            
        }
        
        if (aLine.hasOption(FollowUpCLIParams.MS2PIP_MODELS.id)) {
            
            ms2pipModels = aLine.getOptionValue(FollowUpCLIParams.MS2PIP_FILE.id).split(",");
            
        }
        
        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
        
    }

    /**
     * Returns the list of modifications comprised in a command line input.
     *
     * @param commandLineInput the command line input
     *
     * @return the given list of modifications
     */
    private HashSet<String> getModificationNames(String commandLineInput) {
        
        return Arrays.stream(commandLineInput.split(","))
                .map(name -> name.trim())
                .collect(Collectors.toCollection(HashSet::new));
        
    }

    /**
     * The psdb file selected by the user. Null if not set.
     *
     * @return psdb file selected by the user
     */
    public File getPsdbFile() {
        return psdbFile;
    }

    /**
     * The zip file selected by the user. Null if not set.
     *
     * @return zip file selected by the user
     */
    public File getZipFile() {
        return zipFile;
    }

    /**
     * The folder where to export recalibrated spectra. Null if not set.
     *
     * @return the folder where to export recalibrated spectra
     */
    public File getRecalibrationFolder() {
        return recalibrationFolder;
    }

    /**
     * The recalibration mode. 0 by default. See the FollowUpCLIParams for
     * detailed description.
     *
     * @return the recalibration mode
     */
    public int getRecalibrationMode() {
        return recalibrationMode;
    }

    /**
     * Returns the folder where to export the spectrum files. Null if not set.
     *
     * @return the folder where to export the spectrum files
     */
    public File getSpectrumExportFolder() {
        return spectrumExportFolder;
    }

    /**
     * Returns the type of export needed for the spectra. 0 by default. See the
     * FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the spectra
     */
    public int getSpectrumExportTypeIndex() {
        return spectrumExportTypeIndex;
    }

    /**
     * Returns the file where to export the accessions. Null if not set.
     *
     * @return the file where to export the accessions
     */
    public File getAccessionsExportFile() {
        return accessionsExportFile;
    }

    /**
     * Returns the type of export needed for the accessions. 0 by default. See
     * the FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the accession
     */
    public int getAccessionsExportTypeIndex() {
        return accessionsExportTypeIndex;
    }

    /**
     * Returns the file where to export the protein details. Null if not set.
     *
     * @return the file where to export the protein details
     */
    public File getProteinSequencesExportFile() {
        return proteinSequencesExportFile;
    }

    /**
     * Returns the type of export needed for the protein details. 0 by default.
     * See the FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the protein details
     */
    public int getProteinSequencesExportTypeIndex() {
        return proteinSequencesExportTypeIndex;
    }

    /**
     * Returns the Progenesis file. Null if not set.
     *
     * @return the Progenesis file
     */
    public File getProgenesisExportFile() {
        return progenesisExportFile;
    }

    /**
     * Returns the proteoforms file. Null if not set.
     *
     * @return the proteoforms file
     */
    public File getProteoformsFile() {
        return proteoformsFile;
    }

    /**
     * Returns the stem to use for the path to DeepLC files.
     * 
     * @return The stem to use for the path to DeepLC files.
     */
    public String getDeepLcStem() {
        return deepLcStem;
    }

    /**
     * Returns the file where to write the peptides for ms2pip.
     * 
     * @return The file where to write the peptides for ms2pip.
     */
    public File getMs2pipFile() {
        return ms2pipFile;
    }

    /**
     * Returns the models for which to write an ms2pip config file.
     * 
     * @return The models for which to write an ms2pip config file.
     */
    public String[] getMs2pipModels() {
        return ms2pipModels;
    }

    /**
     * Returns the type of export needed for the Progenesis export. 0 by
     * default. See the FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the Progenesis export
     */
    public int getProgenesisExportTypeIndex() {
        return progenesisExportTypeIndex;
    }

    /**
     * Returns the list of PTMs targeted for the Progenesis PTM export. An empty
     * list if none selected.
     *
     * @return the list of PTMs targeted for the Progenesis PTM export
     */
    public HashSet<String> getProgenesisTargetedPTMs() {
        return progenesisTargetedPTMs;
    }
    
    /**
     * Returns the file for the inclusion list generation. null if not set.
     *
     * @return the file for the inclusion list generation. null if not set
     */
    public File getInclusionFile() {
        return inclusionFile;
    }

    /**
     * Returns the format for inclusion list generation.
     *
     * @return the format for inclusion list generation.
     */
    public int getInclusionFormat() {
        return inclusionFormat;
    }

    /**
     * Returns the protein inference filters to use for inclusion list
     * generation.
     *
     * @return the protein inference filters to use for inclusion list
     * generation
     */
    public ArrayList<Integer> getInclusionProteinFilter() {
        if (inclusionProteinFilter == null) {
            inclusionProteinFilter = new ArrayList<>();
            inclusionProteinFilter.add(3);
        }
        return inclusionProteinFilter;
    }

    /**
     * Returns the peptide filters to use for inclusion list generation.
     *
     * @return the peptide filters to use for inclusion list generation
     */
    public ArrayList<Integer> getInclusionPeptideFilter() {
        if (inclusionPeptideFilter == null) {
            inclusionPeptideFilter = new ArrayList<>();
            inclusionPeptideFilter.add(0);
            inclusionPeptideFilter.add(1);
            inclusionPeptideFilter.add(2);
        }
        return inclusionPeptideFilter;
    }

    /**
     * Returns the retention time window to use for inclusion list generation.
     *
     * @return the retention time window to use for inclusion list generation
     */
    public Double getInclusionRtWindow() {
        return inclusionRtWindow;
    }

    /**
     * Indicates whether follow-up tasks are required.
     *
     * @return indicates whether follow-up tasks are required
     */
    public boolean followUpNeeded() {
        return recalibrationNeeded()
                || spectrumExportNeeded()
                || accessionExportNeeded()
                || proteinSequencesExportNeeded()
                || progenesisExportNeeded()
                || inclusionListNeeded()
                || proteoformsNeeded()
                || deepLcExportNeeded()
                || ms2pipExportNeeded();
    }

    /**
     * Indicates whether a recalibration is needed.
     *
     * @return whether a recalibration is needed
     */
    public boolean recalibrationNeeded() {
        return recalibrationFolder != null;
    }

    /**
     * Indicates whether a spectrum export is needed.
     *
     * @return whether a spectrum export is needed
     */
    public boolean spectrumExportNeeded() {
        return spectrumExportFolder != null;
    }

    /**
     * Indicates whether an accession export is needed.
     *
     * @return whether an accession export is needed
     */
    public boolean accessionExportNeeded() {
        return accessionsExportFile != null;
    }

    /**
     * Indicates whether protein details (in FASTA format) export is needed.
     *
     * @return whether protein details (in FASTA format) export is needed
     */
    public boolean proteinSequencesExportNeeded() {
        return proteinSequencesExportFile != null;
    }

    /**
     * Indicates whether a Progenesis export is needed.
     *
     * @return whether a Progenesis export is needed
     */
    public boolean progenesisExportNeeded() {
        return progenesisExportFile != null;
    }

    /**
     * Indicates whether an inclusion list generation is needed.
     *
     * @return whether an inclusion list generation is needed
     */
    public boolean inclusionListNeeded() {
        return inclusionFile != null;
    }

    /**
     * Indicates whether a list of proteoforms is needed.
     *
     * @return whether a list of proteoforms is needed
     */
    public boolean proteoformsNeeded() {
        return proteoformsFile != null;
    }

    /**
     * Indicates whether DeepLC export is needed.
     *
     * @return whether DeepLC export is needed
     */
    public boolean deepLcExportNeeded() {
        return deepLcStem != null;
    }

    /**
     * Indicates whether DeepLC export is needed.
     *
     * @return whether DeepLC export is needed
     */
    public boolean ms2pipExportNeeded() {
        return ms2pipFile != null;
    }

    /**
     * Returns the path settings provided by the user.
     *
     * @return the path settings provided by the user
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }
}
