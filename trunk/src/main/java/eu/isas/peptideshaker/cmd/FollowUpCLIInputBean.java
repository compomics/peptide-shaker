/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.cmd;

import java.io.File;
import org.apache.commons.cli.CommandLine;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc
 */
public class FollowUpCLIInputBean {

    /**
     * The cps file to work on
     */
    private File cpsFile = null;
    /**
     * folder where to export recalibrated files
     */
    private File recalibrationFolder = null;
    /**
     * parameter for the recalibation
     */
    private int recalibrationMode = 0;

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
    }

    /**
     * The cps file selected by the user. Null if not set.
     * @return 
     */
    public File getCpsFile() {
        return cpsFile;
    }

    /**
     * The folder where to export recalibrated spectra. Null if not set.
     * @return 
     */
    public File getRecalibrationFolder() {
        return recalibrationFolder;
    }

    /**
     * The recalibration mode. 0 by default. See the FollowUpCLIParams for detailed description.
     * @return 
     */
    public int getRecalibrationMode() {
        return recalibrationMode;
    }
    
    /**
     * Indicates whether follow-up tasks are required
     * @return 
     */
    public boolean followUpNeeded() {
        return recalibrationFolder != null;
    }
    
}
