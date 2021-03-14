package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.io.export.features.peptideshaker.PsValidationFeature;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the validation related export features.
 *
 * @author Marc Vaudel
 */
public class PsValidationSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsValidationFeature> validationFeatures;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private final boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private final boolean header;
    /**
     * The writer used to send the output to file.
     */
    private final ExportWriter writer;

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

        validationFeatures = new ArrayList<>(exportFeatures.size());

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

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    TargetDecoyMap targetDecoyMap = psMaps.getPeptideMap();
                    int nMax = targetDecoyMap.getnMax();

                    double pmin = nMax > 0 ? 0.0 : 100.0 / nMax;

                    writer.write(Double.toString(Util.roundDouble(pmin, 2)));
                    writer.newLine();
                    line++;

                    break;

                case peptide_confidence:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPeptideMap();
                    double result = targetDecoyMap.getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;

                    break;

                case peptide_fdr:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPeptideMap();
                    result = targetDecoyMap.getTargetDecoyResults().getFdrLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;

                    break;

                case peptide_fnr:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPeptideMap();
                    result = targetDecoyMap.getTargetDecoyResults().getFnrLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;

                    break;

                case peptide_pep:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPeptideMap();
                    result = 100.0 - targetDecoyMap.getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;

                    break;

                case total_peptide:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPeptideMap();
                    result = targetDecoyMap.getTargetDecoyResults().getnTPTotal();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;

                    break;

                case validated_peptide:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Peptides");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPeptideMap();
                    result = targetDecoyMap.getTargetDecoyResults().getN();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;

                    break;

                case protein_accuracy:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    nMax = targetDecoyMap.getnMax();
                    pmin = nMax > 0 ? 0.0 : 100.0 / nMax;
                    writer.write(Double.toString(Util.roundDouble(pmin, 2)));
                    writer.newLine();
                    line++;
                    break;

                case protein_confidence:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    result = targetDecoyMap.getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case protein_fdr:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    result = targetDecoyMap.getTargetDecoyResults().getFdrLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case protein_fnr:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    result = targetDecoyMap.getTargetDecoyResults().getFnrLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case protein_pep:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    result = targetDecoyMap.getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case total_protein:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    result = targetDecoyMap.getTargetDecoyResults().getnTPTotal();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case validated_protein:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("Proteins");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getProteinMap();
                    result = targetDecoyMap.getTargetDecoyResults().getN();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case psm_accuracy:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("PSMs");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    nMax = targetDecoyMap.getnMax();
                    pmin = nMax > 0 ? 0.0 : 100.0 / nMax;
                    writer.write(Double.toString(Util.roundDouble(pmin, 2)));
                    writer.newLine();
                    line++;
                    break;

                case psm_confidence:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("PSMs");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    result = targetDecoyMap.getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case psm_fdr:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("PSMs");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    result = targetDecoyMap.getTargetDecoyResults().getFdrLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case psm_fnr:

                    if (indexes) {
                        writer.write(Integer.toString(line));
                        writer.addSeparator();
                    }
                    writer.write("PSMs");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    result = targetDecoyMap.getTargetDecoyResults().getFnrLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case psm_pep:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("PSMs");
                    writer.addSeparator();

                    writer.write(validationFeature.getTitle() + " [%]");
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    result = 100.0 - targetDecoyMap.getTargetDecoyResults().getConfidenceLimit();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case total_psm:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("PSMs");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    result = targetDecoyMap.getTargetDecoyResults().getnTPTotal();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                case validated_psm:

                    if (indexes) {

                        writer.write(Integer.toString(line));
                        writer.addSeparator();

                    }

                    writer.write("PSMs");
                    writer.addSeparator();
                    writer.write(validationFeature.getTitle());
                    writer.addSeparator();

                    targetDecoyMap = psMaps.getPsmMap();
                    result = targetDecoyMap.getTargetDecoyResults().getN();
                    writer.write(Double.toString(Util.roundDouble(result, 2)));
                    writer.newLine();
                    line++;
                    break;

                default:
                    writer.write("Not implemented");
                    
            }
        }
    }
}
