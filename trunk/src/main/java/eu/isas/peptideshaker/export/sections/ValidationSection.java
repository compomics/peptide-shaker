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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                        boolean firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
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
                    boolean firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
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
                    firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
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
                    firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
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
                    firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
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
                    firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
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
                    firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
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
                    firstTitle = true;
                    for (String subTitle : exportFeature.getTitles()) {
                        if (firstTitle) {
                            firstTitle = false;
                        } else {
                            writer.write(", ");
                        }
                        writer.write(subTitle);
                    }
                    writer.write(separator);
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getN();
                    writer.write(Util.roundDouble(result, 0) + "");
                    writer.newLine();
                    line++;
                    break;
                case psm_accuracy:
                    PsmSpecificMap psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                pmin = 0;
                                nMax = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getnMax();
                                if (nMax != 0) {
                                    pmin = 100.0 / nMax;
                                }
                                writer.write(Util.roundDouble(pmin, 2) + " %");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        pmin = 0;
                        nMax = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getnMax();
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
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getConfidenceLimit();
                                writer.write(Util.roundDouble(result, 2) + " %");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_fdr:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getFdrLimit();
                                writer.write(Util.roundDouble(result, 2) + " %");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getFdrLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_fnr:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getFnrLimit();
                                writer.write(Util.roundDouble(result, 2) + " %");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getFnrLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_pep:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                result = 100 - psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getConfidenceLimit();
                                writer.write(Util.roundDouble(result, 2) + " %");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        result = 100 - psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + " %");
                        writer.newLine();
                        line++;
                    }
                    break;
                case total_psm:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getnTPTotal();
                                writer.write(Util.roundDouble(result, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getnTPTotal();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case validated_psm:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    for (int charge : psmTargetDecoyMap.getPossibleCharges()) {
                        for (String file : psmTargetDecoyMap.getFilesAtCharge(charge)) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                if (indexes) {
                                    writer.write(line + separator);
                                }
                                firstTitle = true;
                                for (String subTitle : exportFeature.getTitles()) {
                                    if (firstTitle) {
                                        firstTitle = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(subTitle);
                                }
                                writer.write(separator);
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getN();
                                writer.write(Util.roundDouble(result, 0) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    for (int charge : psmTargetDecoyMap.getGroupedCharges()) {
                        if (indexes) {
                            writer.write(line + separator);
                        }
                        firstTitle = true;
                        for (String subTitle : exportFeature.getTitles()) {
                            if (firstTitle) {
                                firstTitle = false;
                            } else {
                                writer.write(", ");
                            }
                            writer.write(subTitle);
                        }
                        writer.write(separator);
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getN();
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
