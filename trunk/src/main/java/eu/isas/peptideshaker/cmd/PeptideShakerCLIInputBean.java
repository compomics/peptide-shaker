package eu.isas.peptideshaker.cmd;


import org.apache.commons.cli.CommandLine;

import java.io.File;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an Options instance.
 */
public class PeptideShakerCLIInputBean {

    // Accepted PSM FDR
    private double iPSMFDR = 1.0;

    // Accepted Peptide sequence FDR
    private double iPeptideFDR = 1.0;

    // Accepted Protein FDR
    private double iProteinFDR = 1.0;


    // SearchGUI input folder
    private File iInput = null;

    // PeptideShaker output folder
    private File iOutput = null;


    /**
     * Construct a PeptideShakerCLIInputBean from a Apache CLI instance.
     */
    public PeptideShakerCLIInputBean(CommandLine aLine) {

        iInput = new File(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_INPUT.id));
        iOutput = new File(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id));

        if (aLine.hasOption(PeptideShakerCLIParams.FDR_LEVEL_PSM.id)) {
            iPSMFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL_PSM.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.id)) {
            iPeptideFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL_PEPTIDE.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.id)) {
            iProteinFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.FDR_LEVEL_PROTEIN.id));
        }

    }

    /**
     * Empty constructor for API usage via other tools.
     * Use the SETTERS!
     */
    public PeptideShakerCLIInputBean() {
    }

    /**
     * Returns the accepted identification PSM FDR
     */
    public double getPSMFDR() {
        return iPSMFDR;
    }

    /**
     * Returns the accepted identification Peptide FDR
     */
    public double getPeptideFDR() {
        return iPeptideFDR;
    }

    /**
     * Returns the accepted identification Protein FDR
     */
    public double getProteinFDR() {
        return iProteinFDR;
    }

    /**
     * Returns the SearchGUI result folder as the input folder for PeptideShaker
     *
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
     * Set the accepted identification PSM FDR
     * @param aPSMFDR Accepted FDR at Peptide-Spectrum-Match level (e.g. '1.0' for 1% FDR)
     */
    public void setPSMFDR(double aPSMFDR) {
        iPSMFDR = aPSMFDR;
    }

    /**
     * Set the accepted identification Peptide FDR
     * @param aPeptideFDR Accepted FDR at Peptide Sequence level (e.g. '1.0' for 1% FDR)
     */
    public void setPeptideFDR(double aPeptideFDR) {
        iPeptideFDR = aPeptideFDR;
    }

    /**
     * Set the accepted identification Protein FDR
     * @param aProteinFDR Accepted FDR at Protein level (e.g. '1.0' for 1% FDR)
     */
    public void setProteinFDR(double aProteinFDR) {
        iProteinFDR = aProteinFDR;
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
