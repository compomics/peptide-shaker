package eu.isas.peptideshaker.cmd;

/**
 * Enum class specifying the Command Line Parameters for PeptideShaker.
 *
 * @author Kenny Helsens
 */
public enum PeptideShakerCLIParams {

    PEPTIDESHAKER_INPUT("search_gui_results", "SearchGUI result folder"),
    PEPTIDESHAKER_OUTPUT("out", "PeptideShaker output folder"),
    FDR_LEVEL_PSM("psm", "FDR at PSM level (default 1% FDR: <1>)"),
    FDR_LEVEL_PEPTIDE("pep", "FDR at PEPTIDE level (default 1% FDR: <1>)"),
    FDR_LEVEL_PROTEIN("prot", "FDR at PROTEIN level (default 1% FDR: <1>)"),
    ASCORE("ascore", "Include ascore to estimate the probability of phospho sites"),
    EXPERIMENT("experiment", "Specifies the experiment name"),
    SAMPLE("sample", "Specifies the sample name within an experiment");
    /**
     * Short Id for the CLI parameter.
     */
    public String id;
    /**
     * Explanation for the CLI parameter.
     */
    public String description;

    /**
     * Private constructor managing the various variables for the enum
     * instances.
     * 
     * @param id the id
     * @param description the description
     */
    private PeptideShakerCLIParams(String id, String description) {
        this.id = id;
        this.description = description;
    }
}
