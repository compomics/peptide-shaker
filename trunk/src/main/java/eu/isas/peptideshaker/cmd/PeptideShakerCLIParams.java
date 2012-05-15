package eu.isas.peptideshaker.cmd;

/**
 * Enum class specifying the Command Line Parameters for PeptideShaker
 *
 * @author Kenny Helsens
 */
public enum PeptideShakerCLIParams {

    FDR("fdr", "False Discovery Rate (e.g. <0.01>)"),
    SEARCH_GUI_RES("search_gui_results", "SearchGUI result folder"),
    OUTPUT("out", "PeptideShaker output folder"),
    FDR_LEVEL("fdr_level", String.format("Calculate FDR at specific ID level (<%s>, <%s> or <%s>)", "psm", "pep", "prot")),
    FDR_LEVEL_PSM("psm", "Calculate FDR at PSM level"),
    FDR_LEVEL_PEPTIDE("pep", "Calculate FDR at PEPTIDE level"),
    FDR_LEVEL_PROTEIN("prot", "Calculate FDR at PROTEIN level");

    /**
     * Short Id for the CLI parameter
     */
    public String id;
    /**
     * Explanation for the CLI parameter
     */
    public String description;

    /**
     * Private constructor managing the various variables for the enum instances.
     */
    private PeptideShakerCLIParams(String id, String description) {
        this.id = id;
        this.description = description;
    }
}

