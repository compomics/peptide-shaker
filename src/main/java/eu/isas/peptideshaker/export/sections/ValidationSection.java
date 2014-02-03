package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * This class outputs the validation related export features.
 *
 * @author Marc Vaudel
 */
public class ValidationSection {

    /**
     * The features to export.
     */
    private ArrayList<ExportFeature> exportFeatures;
    /**
     * The separator used to separate columns.
     */
    private String separator;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public ValidationSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param psMaps the target/decoy maps of this project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(PSMaps psMaps, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            if (indexes) {
                writer.write(separator);
            }
            writer.write("Parameter" + separator + "Value");
            writer.newLine();
        }

        int line = 1;

        for (ExportFeature exportFeature : exportFeatures) {
            ValidationFeatures validationFeatures = (ValidationFeatures) exportFeature;
            switch (validationFeatures) {
                case peptide_accuracy:
                    PeptideSpecificMap peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    ArrayList<String> peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double pmin = 0;
                        int nMax = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getnMax();
                        if (nMax != 0) {
                            pmin = 100.0 / nMax;
                        }
                        writer.write(Util.roundDouble(pmin, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_confidence:
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_fdr:
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getFdrLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_fnr:
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getFnrLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_pep:
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double result = 100 - peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case total_peptide:
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getnTPTotal();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case validated_peptide:
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (peptideKeys.size() > 1) {
                            title = peptideKey + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getN();
                        writer.write(Util.roundDouble(result, 0) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case protein_accuracy:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    ProteinMap proteinMap = psMaps.getProteinMap();
                    double pmin = 0;
                    int nMax = proteinMap.getTargetDecoyMap().getnMax();
                    if (nMax != 0) {
                        pmin = 100.0 / nMax;
                    }
                    writer.write(Util.roundDouble(pmin, 2) + " %");
                    writer.newLine();
                    line++;
                    break;
                case protein_confidence:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    proteinMap = psMaps.getProteinMap();
                    double result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Util.roundDouble(result, 2) + " %");
                    writer.newLine();
                    line++;
                    break;
                case protein_fdr:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getFdrLimit();
                    writer.write(Util.roundDouble(result, 2) + " %");
                    writer.newLine();
                    line++;
                    break;
                case protein_fnr:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getFnrLimit();
                    writer.write(Util.roundDouble(result, 2) + " %");
                    writer.newLine();
                    line++;
                    break;
                case protein_pep:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    proteinMap = psMaps.getProteinMap();
                    result = 100 - proteinMap.getTargetDecoyMap().getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Util.roundDouble(result, 2) + " %");
                    writer.newLine();
                    line++;
                    break;
                case total_protein:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getnTPTotal();
                    writer.write(Util.roundDouble(result, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case validated_protein:
                    if (indexes) {
                        writer.write(line + separator);
                    }
                    writer.write(validationFeatures.getTitle(", ") + separator);
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getN();
                    writer.write(Util.roundDouble(result, 0) + "");
                    writer.newLine();
                    line++;
                    break;
                case psm_accuracy:
                    PsmSpecificMap psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    HashMap<Integer, String> psmKeys = psmTargetDecoyMap.getKeys();
                    Set<Integer> keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        pmin = 0;
                        nMax = psmTargetDecoyMap.getTargetDecoyMap(charge).getnMax();
                        if (nMax != 0) {
                            pmin = 100.0 / nMax;
                        }
                        writer.write(Util.roundDouble(pmin, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_confidence:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    psmKeys = psmTargetDecoyMap.getKeys();
                    keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_fdr:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    psmKeys = psmTargetDecoyMap.getKeys();
                    keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge).getTargetDecoyResults().getFdrLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_fnr:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    psmKeys = psmTargetDecoyMap.getKeys();
                    keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge).getTargetDecoyResults().getFnrLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_pep:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    psmKeys = psmTargetDecoyMap.getKeys();
                    keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        result = 100 - psmTargetDecoyMap.getTargetDecoyMap(charge).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case total_psm:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    psmKeys = psmTargetDecoyMap.getKeys();
                    keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge).getTargetDecoyResults().getnTPTotal();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case validated_psm:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    psmKeys = psmTargetDecoyMap.getKeys();
                    keys = psmKeys.keySet();
                    for (int charge : keys) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        String title = "";
                        if (keys.size() > 1) {
                            title = "Charge " + psmKeys.get(charge) + " ";
                        }
                        writer.write(title + validationFeatures.getTitle(", ") + separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge).getTargetDecoyResults().getN();
                        writer.write(Util.roundDouble(result, 0) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                default:
                    writer.write("Not implemented");
            }
        }
    }
}
