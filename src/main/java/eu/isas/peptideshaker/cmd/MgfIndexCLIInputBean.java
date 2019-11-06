package eu.isas.peptideshaker.cmd;

import com.compomics.software.cli.CommandLineUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc Vaueel
 * @author Harald Barsnes
 * @author Carlos Horro
 */
public class MgfIndexCLIInputBean {

    /**
     * The spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<>();
    /**
     * The path settings.
     */
    private final PathSettingsCLIInputBean pathSettingsCLIInputBean;
    /**
     * Zipped file where to export the indexes.
     */
    private File zipExport = null;
    /**
     * The folder where the exported indexes should be output.
     */
    private File exportFolder = null;

    /**
     * Parses a MgfIndexCLI command line and stores the input in the attributes.
     *
     * @param aLine a MgfIndexCLI command line
     *
     * @throws java.io.IOException thrown whenever an IOExeption occurs
     */
    public MgfIndexCLIInputBean(CommandLine aLine) throws IOException {

        if (aLine.hasOption(MgfIndexCLIParams.MGF_FILES.id)) {
            String filesTxt = aLine.getOptionValue(MgfIndexCLIParams.MGF_FILES.id);
            spectrumFiles = getSpectrumFiles(filesTxt);
        }

        // zipped export
        if (aLine.hasOption(MgfIndexCLIParams.EXPORT_ZIP.id)) {
            zipExport = new File(aLine.getOptionValue(MgfIndexCLIParams.EXPORT_ZIP.id));
        }

        if (aLine.hasOption(MgfIndexCLIParams.EXPORT_FOLDER.id)) {
            exportFolder = new File(aLine.getOptionValue(MgfIndexCLIParams.EXPORT_FOLDER.id));
        }

        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
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
     * Returns the path settings input.
     *
     * @return the path settings input
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }

    /**
     * Returns a boolean indicating whether the export should be gzipped.
     *
     * @return a boolean indicating whether the export should be gzipped
     */
    //public boolean isGzip() {
    //    return gzip;
    //}
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
     * Returns the folder where mgf indexes will be stored.
     *
     * @return the folder where mgf indexes will be stored
     */
    public File getExportFolder() {
        return exportFolder;
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
        ArrayList<String> extentions = new ArrayList<>();
        extentions.add(".mgf");
        return CommandLineUtils.getFiles(optionInput, extentions);
    }

}
