package eu.isas.peptideshaker.cmd;


import org.apache.commons.cli.CommandLine;

import java.io.File;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an Options instance.
 */
public class PeptideShakerCLIInputBean {

    // Accepted identification FDR
    private double iFDR;

    // SearchGUI input folder
    private File iInput;

    // PeptideShaker output folder
    private File iOutput;

    // FDR level (psm, peptide, protein)
    private String iFDR_Level;

    /**
     * Construct a PeptideShakerCLIInputBean from a Apache CLI instance.
     */
    public PeptideShakerCLIInputBean(CommandLine aLine) {
        iFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.FDR.id));
        iInput = new File(aLine.getOptionValue(PeptideShakerCLIParams.SEARCH_GUI_RES.id));
        iOutput = new File(aLine.getOptionValue(PeptideShakerCLIParams.OUTPUT.id));
        iFDR_Level = aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL.id);
    }

    /**
     * Empty constructor for API usage via other tools.
     * Use the SETTERS!
     */
    public PeptideShakerCLIInputBean() {
    }

    /**
     * Returns the accepted identification FDR
     */
    public double getFDR() {
        return iFDR;
    }

    /**
     * Returns the FDR level (psm, peptide, protein)
     * @return
     */
    public String getFDR_Level() {
        return iFDR_Level;
    }

    /**
     * Returns the SearchGUI result folder as the input folder for PeptideShaker
     * @return
     */
    public File getInput() {
        return iInput;
    }

    /**
     * Returns the PeptideShaker output folder
     */
    public File getOutput() {
        return iOutput;
    }

    /**
     * Set the accepted identification FDR
     */
    public void setFDR(double aFDR) {
        iFDR = aFDR;
    }


    /**
     * Returns the FDR level (psm, peptide, protein)
     */
    public void setFDR_Level(String aFDR_Level) {
        iFDR_Level = aFDR_Level;
    }

    /**
     * Set the SearchGUI result folder as the input folder for PeptideShaker
     */
    public void setInput(File aInput) {
        iInput = aInput;
    }


    /**
     * Set the SearchGUI result folder as the input folder for PeptideShaker
     */
    public void setOutput(File aOutput) {
        iOutput = aOutput;
    }
}
