package eu.isas.peptideshaker.cmd;

import com.compomics.software.CommandLineUtils;
import java.io.File;
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc Vaudel
 */
public class FollowUpCLIInputBean {

    /**
     * The cps file to work on.
     */
    private File cpsFile = null;
    /**
     * folder where to export recalibrated files.
     */
    private File recalibrationFolder = null;
    /**
     * parameter for the recalibation.
     */
    private int recalibrationMode = 0;
    /**
     * The folder where to export spectra.
     */
    private File spectrumExportFolder = null;
    /**
     * the spectrum export type index. see SpectrumExporter.ExportType for
     * details.
     */
    private int spectrumExportTypeIndex = 0;
    /**
     * The file where to export protein accessions.
     */
    private File accessionsExportFile = null;
    /**
     * the accessions export type index. see FastaExport.ExportType for details.
     */
    private int accessionsExportTypeIndex = 0;
    /**
     * The file where to export protein details.
     */
    private File fastaExportFile = null;
    /**
     * the protein details export type index. see FastaExport.ExportType for
     * details.
     */
    private int fastaExportTypeIndex = 0;
    /**
     * The file where to export the Progenesis file.
     */
    private File progenesisExportFile = null;
    /**
     * the type of Progenesis export.
     */
    private int progenesisExportTypeIndex = 0;
    /**
     * The folder where to output the PepNovo training files.
     */
    private File pepnovoTrainingFolder = null;
    /**
     * Boolean indicating whether the mgf files exported by the PepNovo training
     * method shall be recalibrated.
     */
    private boolean pepnovoTrainingRecalibrate = true;
    /**
     * The FDR for the "good spectra" training set.
     */
    private Double pepnovoTrainingFDR = null;
    /**
     * The FNR for the "bad spectra" training set.
     */
    private Double pepnovoTrainingFNR = null;
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
     * Construct a FollowUpCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     */
    public FollowUpCLIInputBean(CommandLine aLine) {

        if (aLine.hasOption(FollowUpCLIParams.CPS_FILE.id)) {
            cpsFile = new File(aLine.getOptionValue(FollowUpCLIParams.CPS_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.RECALIBRATION_FOLDER.id)) {
            recalibrationFolder = new File(aLine.getOptionValue(FollowUpCLIParams.RECALIBRATION_FOLDER.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.RECALIBRATION_MODE.id)) {
            recalibrationMode = new Integer(aLine.getOptionValue(FollowUpCLIParams.RECALIBRATION_MODE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.SPECTRUM_FOLDER.id)) {
            spectrumExportFolder = new File(aLine.getOptionValue(FollowUpCLIParams.SPECTRUM_FOLDER.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PSM_TYPE.id)) {
            spectrumExportTypeIndex = new Integer(aLine.getOptionValue(FollowUpCLIParams.PSM_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.ACCESSIONS_FILE.id)) {
            accessionsExportFile = new File(aLine.getOptionValue(FollowUpCLIParams.ACCESSIONS_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.ACCESSIONS_TYPE.id)) {
            accessionsExportTypeIndex = new Integer(aLine.getOptionValue(FollowUpCLIParams.ACCESSIONS_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.FASTA_FILE.id)) {
            fastaExportFile = new File(aLine.getOptionValue(FollowUpCLIParams.FASTA_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.FASTA_TYPE.id)) {
            fastaExportTypeIndex = new Integer(aLine.getOptionValue(FollowUpCLIParams.FASTA_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PROGENESIS_FILE.id)) {
            progenesisExportFile = new File(aLine.getOptionValue(FollowUpCLIParams.PROGENESIS_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PROGENESIS_TYPE.id)) {
            progenesisExportTypeIndex = new Integer(aLine.getOptionValue(FollowUpCLIParams.PROGENESIS_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PEPNOVO_TRAINING_FOLDER.id)) {
            pepnovoTrainingFolder = new File(aLine.getOptionValue(FollowUpCLIParams.PEPNOVO_TRAINING_FOLDER.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PEPNOVO_TRAINING_RECALIBRATION.id)) {
            int value = new Integer(aLine.getOptionValue(FollowUpCLIParams.PEPNOVO_TRAINING_RECALIBRATION.id));
            pepnovoTrainingRecalibrate = value == 1;
        }
        if (aLine.hasOption(FollowUpCLIParams.PEPNOVO_TRAINING_FDR.id)) {
            pepnovoTrainingFDR = new Double(aLine.getOptionValue(FollowUpCLIParams.PEPNOVO_TRAINING_FDR.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PEPNOVO_TRAINING_FNR.id)) {
            pepnovoTrainingFNR = new Double(aLine.getOptionValue(FollowUpCLIParams.PEPNOVO_TRAINING_FNR.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_FILE.id)) {
            inclusionFile = new File(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_FORMAT.id)) {
            inclusionFormat = new Integer(aLine.getOptionValue(FollowUpCLIParams.INCLUSION_LIST_FORMAT.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_PROTEIN_FILTERS.id)) {
            inclusionProteinFilter = CommandLineUtils.getIntegerListFromString(FollowUpCLIParams.INCLUSION_LIST_PROTEIN_FILTERS.id, ",");
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_PEPTIDE_FILTERS.id)) {
            inclusionPeptideFilter = CommandLineUtils.getIntegerListFromString(FollowUpCLIParams.INCLUSION_LIST_PEPTIDE_FILTERS.id, ",");
        }
        if (aLine.hasOption(FollowUpCLIParams.INCLUSION_LIST_RT_WINDOW.id)) {
            inclusionRtWindow = new Double(FollowUpCLIParams.INCLUSION_LIST_RT_WINDOW.id);
        }

    }

    /**
     * The cps file selected by the user. Null if not set.
     *
     * @return cps file selected by the user
     */
    public File getCpsFile() {
        return cpsFile;
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
    public File getFastaExportFile() {
        return fastaExportFile;
    }

    /**
     * Returns the type of export needed for the protein details. 0 by default.
     * See the FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the protein details
     */
    public int getFastaExportTypeIndex() {
        return fastaExportTypeIndex;
    }

    /**
     * Returns the progenesis file. Null if not set.
     *
     * @return the progenesis file
     */
    public File getProgenesisExportFile() {
        return progenesisExportFile;
    }

    /**
     * Returns the type of export needed for the progenesis export. 0 by
     * default. See the FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the progenesis export
     */
    public int getProgenesisExportTypeIndex() {
        return progenesisExportTypeIndex;
    }

    /**
     * Returns the folder set for the export of Pepnovo training files.
     *
     * @return the folder set for the export of Pepnovo training files
     */
    public File getPepnovoTrainingFolder() {
        return pepnovoTrainingFolder;
    }

    /**
     * Indicates whether the mgf files exported by the pepnovo export shall be
     * recalibrated.
     *
     * @return a boolean indicating whether the mgf files exported by the
     * pepnovo export shall be recalibrated
     */
    public boolean isPepnovoTrainingRecalibrate() {
        return pepnovoTrainingRecalibrate;
    }

    /**
     * Indicates the FDR set for the "good spectra" set of the pepnovo training
     * files. Null if not set.
     *
     * @return the FDR set for the "good spectra" set of the pepnovo training
     * files
     */
    public Double getPepnovoTrainingFDR() {
        return pepnovoTrainingFDR;
    }

    /**
     * Indicates the FNR set for the "good spectra" set of the pepnovo training
     * files. Null if not set.
     *
     * @return the FNR set for the "good spectra" set of the pepnovo training
     * files
     */
    public Double getPepnovoTrainingFNR() {
        return pepnovoTrainingFNR;
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
            inclusionProteinFilter = new ArrayList<Integer>();
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
        if (inclusionProteinFilter == null) {
            inclusionProteinFilter = new ArrayList<Integer>();
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
                || fastaExportNeeded()
                || progenesisExportNeeded()
                || inclusionListNeeded();
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
     * Indicates whether a FASTA export is needed.
     *
     * @return whether a FASTA export is needed
     */
    public boolean fastaExportNeeded() {
        return fastaExportFile != null;
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
     * Indicates whether a PepNovo training export is needed.
     *
     * @return whether a PepNovo training export export is needed
     */
    public boolean pepnovoTrainingExportNeeded() {
        return pepnovoTrainingFolder != null;
    }

    /**
     * Indicates whether an inclusion list generation is needed.
     *
     * @return whether an inclusion list generation is needed
     */
    public boolean inclusionListNeeded() {
        return inclusionFile != null;
    }
}
