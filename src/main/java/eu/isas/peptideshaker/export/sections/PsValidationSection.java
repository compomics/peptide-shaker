package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsValidationFeature;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.maps.PsmSpecificMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class outputs the validation related export features.
 *
 * @author Marc Vaudel
 */
public class PsValidationSection {

    /**
     * The features to export.
     */
    private ArrayList<PsValidationFeature> validationFeatures;
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
    private ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsValidationSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        validationFeatures = new ArrayList<PsValidationFeature>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsValidationFeature) {
                validationFeatures.add((PsValidationFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as validation feature.");
            }
        }
        Collections.sort(validationFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param psMaps the target/decoy maps of this project
     * @param identificationParameters the identification parameters
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(PSMaps psMaps, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            if (indexes) {
                writer.writeHeaderText("");
                writer.addSeparator();
            }
            writer.writeHeaderText("Class");
            writer.addSeparator();
            writer.writeHeaderText("Parameter");
            writer.addSeparator();
            writer.writeHeaderText("Value");
            writer.newLine();
        }

        int line = 1;

        for (PsValidationFeature validationFeature : validationFeatures) {
            switch (validationFeature) {
                case peptide_accuracy:
                    PtmSettings ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    PeptideSpecificMap peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    ArrayList<String> peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        double pmin = 0;
                        int nMax = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getnMax();
                        if (nMax != 0) {
                            pmin = 100.0 / nMax;
                        }
                        writer.write(Util.roundDouble(pmin, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_confidence:
                    ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_fdr:
                    ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getFdrLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_fnr:
                    ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getFnrLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case peptide_pep:
                    ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        double result = 100 - peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case total_peptide:
                    ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle());
                        writer.addSeparator();
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getnTPTotal();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case validated_peptide:
                    ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
                    peptideTargetDecoyMap = psMaps.getPeptideSpecificMap();
                    peptideKeys = peptideTargetDecoyMap.getKeys();
                    for (String peptideKey : peptideKeys) {
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("Peptides");
                        if (peptideKeys.size() > 1) {
                            String title = PeptideSpecificMap.getKeyName(ptmSettings, peptideKey);
                            writer.write(" (" + title + ")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle());
                        writer.addSeparator();
                        double result = peptideTargetDecoyMap.getTargetDecoyMap(peptideKey).getTargetDecoyResults().getN();
                        writer.write(Util.roundDouble(result, 0) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case protein_accuracy:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();
                    ProteinMap proteinMap = psMaps.getProteinMap();
                    double pmin = 0;
                    int nMax = proteinMap.getTargetDecoyMap().getnMax();
                    if (nMax != 0) {
                        pmin = 100.0 / nMax;
                    }
                    writer.write(Util.roundDouble(pmin, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case protein_confidence:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();
                    proteinMap = psMaps.getProteinMap();
                    double result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Util.roundDouble(result, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case protein_fdr:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getFdrLimit();
                    writer.write(Util.roundDouble(result, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case protein_fnr:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getFnrLimit();
                    writer.write(Util.roundDouble(result, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case protein_pep:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();
                    proteinMap = psMaps.getProteinMap();
                    result = 100 - proteinMap.getTargetDecoyMap().getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Util.roundDouble(result, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case total_protein:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getnTPTotal();
                    writer.write(Util.roundDouble(result, 2) + "");
                    writer.newLine();
                    line++;
                    break;
                case validated_protein:
                    if (indexes) {
                        writer.write(line + "");
                        writer.addSeparator();
                    }
                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();
                    proteinMap = psMaps.getProteinMap();
                    result = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getN();
                    writer.write(Util.roundDouble(result, 0) + "");
                    writer.newLine();
                    line++;
                    break;
                case psm_accuracy:
                    PsmSpecificMap psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    ArrayList<Integer> possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    HashSet<Integer> foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle() + " [%]");
                                writer.addSeparator();
                                pmin = 0;
                                nMax = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getnMax();
                                if (nMax != 0) {
                                    pmin = 100.0 / nMax;
                                }
                                writer.write(Util.roundDouble(pmin, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    HashMap<Integer, ArrayList<Integer>> groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    ArrayList<Integer> representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        pmin = 0;
                        nMax = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getnMax();
                        if (nMax != 0) {
                            pmin = 100.0 / nMax;
                        }
                        writer.write(Util.roundDouble(pmin, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_confidence:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle() + " [%]");
                                writer.addSeparator();
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getConfidenceLimit();
                                writer.write(Util.roundDouble(result, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_fdr:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle() + " [%]");
                                writer.addSeparator();
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getFdrLimit();
                                writer.write(Util.roundDouble(result, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getFdrLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_fnr:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle() + " [%]");
                                writer.addSeparator();
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getFnrLimit();
                                writer.write(Util.roundDouble(result, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getFnrLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case psm_pep:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle() + " [%]");
                                writer.addSeparator();
                                result = 100 - psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getConfidenceLimit();
                                writer.write(Util.roundDouble(result, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle() + " [%]");
                        writer.addSeparator();
                        result = 100 - psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getConfidenceLimit();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case total_psm:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle());
                                writer.addSeparator();
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getnTPTotal();
                                writer.write(Util.roundDouble(result, 2) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle());
                        writer.addSeparator();
                        result = psmTargetDecoyMap.getTargetDecoyMap(charge, null).getTargetDecoyResults().getnTPTotal();
                        writer.write(Util.roundDouble(result, 2) + "");
                        writer.newLine();
                        line++;
                    }
                    break;
                case validated_psm:
                    psmTargetDecoyMap = psMaps.getPsmSpecificMap();
                    possibleCharges = psmTargetDecoyMap.getPossibleCharges();
                    Collections.sort(possibleCharges);
                    foundCharges = new HashSet<Integer>(possibleCharges.size());
                    for (int charge : possibleCharges) {
                        ArrayList<String> files = psmTargetDecoyMap.getFilesAtCharge(charge);
                        Collections.sort(files);
                        for (String file : files) {
                            if (!psmTargetDecoyMap.isFileGrouped(charge, file)) {
                                foundCharges.add(charge);
                                if (indexes) {
                                    writer.write(line + "");
                                    writer.addSeparator();
                                }
                                writer.write("PSMs");
                                if (files.size() > 1 || possibleCharges.size() > 1) {
                                    writer.write(" (");
                                    if (possibleCharges.size() > 1) {
                                        writer.write("Charge " + charge);
                                        if (files.size() > 1) {
                                            writer.write(" of file ");
                                        }
                                    }
                                    if (files.size() > 1) {
                                        writer.write(file);
                                    }
                                    writer.write(")");
                                }
                                writer.addSeparator();
                                writer.write(validationFeature.getTitle());
                                writer.addSeparator();
                                result = psmTargetDecoyMap.getTargetDecoyMap(charge, file).getTargetDecoyResults().getN();
                                writer.write(Util.roundDouble(result, 0) + "");
                                writer.newLine();
                                line++;
                            }
                        }
                    }
                    groupedCharges = psmTargetDecoyMap.getChargeGroupingMap();
                    representativeCharges = new ArrayList<Integer>(groupedCharges.keySet());
                    Collections.sort(representativeCharges);
                    for (int charge : representativeCharges) {
                        ArrayList<Integer> secondaryCharges = groupedCharges.get(charge);
                        if (indexes) {
                            writer.write(line + "");
                            writer.addSeparator();
                        }
                        writer.write("PSMs");
                        if (groupedCharges.size() > 1 || !foundCharges.isEmpty()) {
                            writer.write(" (");
                            if (foundCharges.contains(charge)) {
                                writer.write("Other ");
                            }
                            writer.write("Charge " + charge);
                            if (!secondaryCharges.isEmpty()) {
                                writer.write(" and Charge ");
                                StringBuilder secondaryChargesTxt = new StringBuilder();
                                Collections.sort(secondaryCharges);
                                for (Integer secondaryCharge : secondaryCharges) {
                                    if (secondaryChargesTxt.length() > 0) {
                                        secondaryChargesTxt.append(", ");
                                    }
                                    secondaryChargesTxt.append(secondaryCharge);
                                }
                                writer.write(secondaryChargesTxt.toString());
                            }
                            writer.write(")");
                        }
                        writer.addSeparator();
                        writer.write(validationFeature.getTitle());
                        writer.addSeparator();
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
