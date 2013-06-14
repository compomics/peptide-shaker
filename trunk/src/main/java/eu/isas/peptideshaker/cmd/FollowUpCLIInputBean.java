package eu.isas.peptideshaker.cmd;

import java.io.File;
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
     * The file where to export the progenesis file.
     */
    private File progenesisExportFile = null;
    /**
     * the type of progenesis export
     */
    private int progenesisExportTypeIndex = 0;

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
     * Returns the type of export needed for the progenesis export. 0 by default.
     * See the FollowUpCLIParams for detailed description.
     *
     * @return the type of export needed for the progenesis export
     */
    public int getProgenesisExportTypeIndex() {
        return progenesisExportTypeIndex;
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
                || progenesisExportNeeded();
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
     * Indicates whether a progenesis export is needed.
     * 
     * @return whether a progenesis export is needed
     */
    public boolean progenesisExportNeeded() {
        return progenesisExportFile != null;
    }
}
