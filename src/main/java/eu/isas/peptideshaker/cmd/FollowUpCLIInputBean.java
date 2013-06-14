package eu.isas.peptideshaker.cmd;

import eu.isas.peptideshaker.export.SpectrumExporter;
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
     * The folder where to export spectra
     */
    private File spectrumExportFolder = null;
    /**
     * the spectrum export type index. see SpectrumExporter.ExportType for details.
     */
    private int spectrumExportTypeIndex = 0;
    /**
     * The file where to export protein accessions
     */
    private File accessionsExportFile = null;
    /**
     * the accessions export type index. see FastaExport.ExportType for details.
     */
    private int accessionsExportTypeIndex = 0;
    /**
     * The file where to export protein details
     */
    private File fastaExportFile = null;
    /**
     * the protein details export type index. see FastaExport.ExportType for details.
     */
    private int fastaExportTypeIndex = 0;

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
            recalibrationFolder = new File(aLine.getOptionValue(FollowUpCLIParams.SPECTRUM_FOLDER.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.PSM_TYPE.id)) {
            recalibrationMode = new Integer(aLine.getOptionValue(FollowUpCLIParams.PSM_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.ACCESSIONS_FILE.id)) {
            recalibrationFolder = new File(aLine.getOptionValue(FollowUpCLIParams.ACCESSIONS_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.ACCESSIONS_TYPE.id)) {
            recalibrationMode = new Integer(aLine.getOptionValue(FollowUpCLIParams.ACCESSIONS_TYPE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.FASTA_FILE.id)) {
            recalibrationFolder = new File(aLine.getOptionValue(FollowUpCLIParams.FASTA_FILE.id));
        }
        if (aLine.hasOption(FollowUpCLIParams.FASTA_TYPE.id)) {
            recalibrationMode = new Integer(aLine.getOptionValue(FollowUpCLIParams.FASTA_TYPE.id));
        }
    }

    /**
     * The cps file selected by the user. Null if not set.
     *
     * @return
     */
    public File getCpsFile() {
        return cpsFile;
    }

    /**
     * The folder where to export recalibrated spectra. Null if not set.
     *
     * @return
     */
    public File getRecalibrationFolder() {
        return recalibrationFolder;
    }

    /**
     * The recalibration mode. 0 by default. See the FollowUpCLIParams for
     * detailed description.
     *
     * @return
     */
    public int getRecalibrationMode() {
        return recalibrationMode;
    }

    /**
     * Returns the folder where to export the spectrum files. Null if not set.
     * 
     * @return 
     */
    public File getSpectrumExportFolder() {
        return spectrumExportFolder;
    }

    /**
     * Returns the type of export needed for the spectra. 0 by default. See the FollowUpCLIParams for
     * detailed description.
     * 
     * @return 
     */
    public int getSpectrumExportTypeIndex() {
        return spectrumExportTypeIndex;
    }

    /**
     * Returns the file where to export the accessions. Null if not set.
     * 
     * @return 
     */
    public File getAccessionsExportFile() {
        return accessionsExportFile;
    }

    /**
     * Returns the type of export needed for the accessions. 0 by default. See the FollowUpCLIParams for
     * detailed description.
     * 
     * @return 
     */
    public int getAccessionsExportTypeIndex() {
        return accessionsExportTypeIndex;
    }

    /**
     * returns the file where to export the protein details. Null if not set.
     * 
     * @return 
     */
    public File getFastaExportFile() {
        return fastaExportFile;
    }

    /**
     * Returns the type of export needed for the protein details. 0 by default. See the FollowUpCLIParams for
     * detailed description.
     * 
     * @return 
     */
    public int getFastaExportTypeIndex() {
        return fastaExportTypeIndex;
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
                || fastaExportNeeded();
    }
    
    /**
     * Indicates whether a recalibration is needed.
     * 
     * @return 
     */
    public boolean recalibrationNeeded() {
        return recalibrationFolder != null ;
    }
    
    /**
     * Indicates whether a spectrum export is needed.
     * 
     * @return 
     */
    public boolean spectrumExportNeeded() {
        return spectrumExportFolder != null;
    }
    
    /**
     * Indicates whether an accession export is needed.
     * 
     * @return 
     */
    public boolean accessionExportNeeded() {
        return accessionsExportFile != null;
    }
    
    /**
     * Indicates whether a fasta export is needed.
     * 
     * @return 
     */
    public boolean fastaExportNeeded() {
        return fastaExportFile != null;
    }
    
}
