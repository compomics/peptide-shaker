package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class can be used to export spectra.
 *
 * @author Marc Vaudel
 */
public class SpectrumExporter {

    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    /**
     * Constructor.
     *
     * @param identification The identification of this project
     */
    public SpectrumExporter(Identification identification) {
        this.identification = identification;
    }

    /**
     * Exports the spectra from different categories of PSMs according to the
     * export type number: 1 - non-validated PSMs 2 - PSMs of non-validated
     * peptides 3 - PSMs of non-validated proteins 4 - validated PSMs 5 -
     * validated PSMs of validated peptides 6 - validated PSMs of validated
     * peptides of validated proteins. The export format is mgf.
     *
     * @param destinationFolder the folder where to write the spectra
     * @param waitingHandler waiting handler used to display progress and cancel
     * the process. Can be null.
     * @param exportType the type of PSM to export as detailed above.
     *
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void exportSpectra(File destinationFolder, WaitingHandler waitingHandler, int exportType) 
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        PSParameter psParameter = new PSParameter();
        if (exportType == 2 || exportType == 5 || exportType == 6) {
            identification.loadPeptideMatchParameters(psParameter, waitingHandler);
        }
        if (exportType == 3 || exportType == 6) {
            identification.loadProteinMatchParameters(psParameter, waitingHandler);
        }
        for (String mgfFile : spectrumFactory.getMgfFileNames()) {
            if (exportType == 2 || exportType == 3 || exportType == 5 || exportType == 6) {
                identification.loadSpectrumMatches(mgfFile, waitingHandler);
            }
            if (exportType == 1 || exportType == 4 || exportType == 5 || exportType == 6) {
                identification.loadSpectrumMatchParameters(mgfFile, psParameter, waitingHandler);
            }

            FileWriter f = new FileWriter(new File(destinationFolder, getFileName(mgfFile, exportType)));
            try {
                BufferedWriter b = new BufferedWriter(f);
                try {
                    for (String spectrumTitle : spectrumFactory.getSpectrumTitles(mgfFile)) {
                        String spectrumKey = Spectrum.getSpectrumKey(mgfFile, spectrumTitle);
                        if (shallExport(spectrumKey, exportType)) {
                            b.write(((MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey)).asMgf());
                        }
                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                            waitingHandler.increaseSecondaryProgressValue();
                        }
                    }
                } finally {
                    b.close();
                }
            } finally {
                f.close();
            }
        }
    }

    /**
     * Returns the suffix for a spectrum file name.
     *
     * @param exportType the type of PSM to be exported
     * @return the suffix for a spectrum file name
     */
    public static String getSuffix(int exportType) {
        switch (exportType) {
            case 1:
                return "_non_validated_PSMs";
            case 2:
                return "_non_validated_peptides";
            case 3:
                return "_non_validated_proteins";
            case 4:
                return "_validated_PSMs";
            case 5:
                return "_validated_PSMs-peptides";
            case 6:
                return "_validated_PSMs-peptides-proteins";
            default:
                throw new IllegalArgumentException("Export type " + exportType + " not supported.");
        }
    }

    /**
     * Returns the name of the file to write.
     *
     * @param fileName the original file name
     * @param exportType the export type
     * @return the name of the file
     */
    public static String getFileName(String fileName, int exportType) {
        String tempName = fileName.substring(0, fileName.lastIndexOf("."));
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return tempName + getSuffix(exportType) + extension;
    }

    /**
     * Indicates whether a spectrum shall be exported according to the export
     * type number.
     *
     * @param spectrumKey the key of the spectrum
     * @param exportType the export type number
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private boolean shallExport(String spectrumKey, int exportType) throws SQLException, IOException, ClassNotFoundException {
        PSParameter psParameter = new PSParameter();
        switch (exportType) {
            case 1:
            case 4:
                if (identification.matchExists(spectrumKey)) {
                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                    if (psParameter.isValidated()) {
                        return exportType == 4;
                    }
                }
                return exportType == 1;
            case 2:
            case 5:
                if (identification.matchExists(spectrumKey)) {
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    Peptide peptide = spectrumMatch.getBestAssumption().getPeptide();
                    String peptideKey = peptide.getKey();
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                    if (exportType == 2 || ((PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter)).isValidated()) {
                        if (psParameter.isValidated()) {
                            return exportType == 5;
                        }
                    }
                }
                return exportType == 2;
            case 3:
            case 6:
                if (identification.matchExists(spectrumKey)) {
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    Peptide peptide = spectrumMatch.getBestAssumption().getPeptide();
                    String peptideKey = peptide.getKey();
                    if (exportType == 3
                            || ((PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter)).isValidated()
                            && ((PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter)).isValidated()) {
                        ArrayList<String> proteins = peptide.getParentProteins();
                        for (String accession : proteins) {
                            for (String proteinKey : identification.getProteinMap().get(accession)) {
                                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                                if (psParameter.isValidated()) {
                                    return exportType == 6;
                                }
                            }
                        }
                    }
                    return exportType == 3;

                }
                return true;
            default:
                throw new IllegalArgumentException("Export type " + exportType + " not supported.");
        }
    }
}
