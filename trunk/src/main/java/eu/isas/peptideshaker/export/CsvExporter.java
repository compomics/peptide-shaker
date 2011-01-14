package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JOptionPane;

/**
 * Contains methods for exporting the search engine results to csv files.
 *
 * @author Marc Vaudel
 */
public class CsvExporter {

    /**
     * separator for csv export. Hard coded for now, could be user setting
     */
    private static final String SEPARATOR = "\t";
    /**
     * The experiment to export
     */
    private MsExperiment experiment;
    /**
     * The sample considered
     */
    private Sample sample;
    /**
     * The replicate considered
     */
    private int replicateNumber;
    /**
     * Name of the file containing the identification information at the protein level
     */
    private String proteinFile;
    /**
     * Name of the file containing the identification information at the peptide level
     */
    private String peptideFile;
    /**
     * Name of the file containing the identification information at the psm level
     */
    private String psmFile;

    /**
     * Creates a CsvExporter object.
     *
     * @param experiment the ms experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
      */
    public CsvExporter(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;

        proteinFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_proteins.txt";
        peptideFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_peptides.txt";
        psmFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_psms.txt";
    }

    /**
     * Exports the results to csv files.
     *
     * @param folder the folder to store the results in.
     */
    public void exportResults(File folder) {
        try {
            Writer proteinWriter = new BufferedWriter(new FileWriter(new File(folder, proteinFile)));
            Writer peptideWriter = new BufferedWriter(new FileWriter(new File(folder, peptideFile)));
            Writer spectrumWriter = new BufferedWriter(new FileWriter(new File(folder, psmFile)));
            String content = "Protein" + SEPARATOR + "n peptides" + SEPARATOR + "n spectra" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            proteinWriter.write(content);
            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "n Spectra" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            peptideWriter.write(content);
            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)" + SEPARATOR + "Mass Error" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value" + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            spectrumWriter.write(content);

            Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
                proteinWriter.write(getLine(proteinMatch));
            }
            for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {
                peptideWriter.write(getLine(peptideMatch));
            }
            for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                spectrumWriter.write(getLine(spectrumMatch));
            }
            proteinWriter.close();
            peptideWriter.close();
            spectrumWriter.close();
            JOptionPane.showMessageDialog(null, "Identifications were successfully exported", "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Writing of spectrum file failed.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Exports the protein match as a line of text.
     *
     * @param proteinMatch the protein match to export
     * @return the protein match as a line of text
     */
    private String getLine(ProteinMatch proteinMatch) {
        String line = "";
        line += proteinMatch.getTheoreticProtein().getAccession() + SEPARATOR;
        line += proteinMatch.getPeptideMatches().size() + SEPARATOR;
        line += proteinMatch.getSpectrumCount() + SEPARATOR;
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);
        try {
        line += probabilities.getProteinProbabilityScore() + SEPARATOR
                + probabilities.getProteinProbability() + SEPARATOR;
        } catch (Exception e) {
        line += SEPARATOR + SEPARATOR;
        }
        if (proteinMatch.isDecoy()) {
            line += "1" + SEPARATOR;
        } else {
            line += "0" + SEPARATOR;
        }
        try {
        if (probabilities.isValidated()) {
            line += "1";
        } else {
            line += "0";
        }
        } catch (Exception e) {

        }
        line += "\n";
        return line;
    }

    /**
     * Exports the peptide match as a line of text.
     *
     * @param peptideMatch the peptide match to export
     * @return the peptide match as a line of text
     */
    private String getLine(PeptideMatch peptideMatch) {
        String line = "";
        for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
            line += protein.getAccession() + " ";
        }
        line += SEPARATOR;
        line += peptideMatch.getTheoreticPeptide().getSequence() + SEPARATOR;
        ArrayList<String> varMods = new ArrayList<String>();
        for (ModificationMatch mod : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (mod.isVariable()) {
                varMods.add(mod.getTheoreticPtm().getName());
            }
        }
        Collections.sort(varMods);
        for (String varMod : varMods) {
            line += varMod + " ";
        }
        line += SEPARATOR;
        line += peptideMatch.getSpectrumMatches().size() + SEPARATOR;
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
        try {
        line += probabilities.getPeptideProbabilityScore() + SEPARATOR
                + probabilities.getPeptideProbability() + SEPARATOR;
        } catch (Exception e) {
        line += SEPARATOR + SEPARATOR;
        }
        if (peptideMatch.isDecoy()) {
            line += "1" + SEPARATOR;
        } else {
            line += "0" + SEPARATOR;
        }
        try {
        if (probabilities.isValidated()) {
            line += "1";
        } else {
            line += "0";
        }
        } catch (Exception e) {

        }
        line += "\n";
        return line;
    }

    /**
     * Exports the spectrum match as a line of text.
     *
     * @param spectrumMatch the spectrum match to export
     * @return the spectrum match as a line of text
     */
    private String getLine(SpectrumMatch spectrumMatch) {
        String line = "";
        Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();
        for (Protein protein : bestAssumption.getParentProteins()) {
            line += protein.getAccession() + " ";
        }
        line += SEPARATOR;
        line += bestAssumption.getSequence() + SEPARATOR;
        for (ModificationMatch mod : bestAssumption.getModificationMatches()) {
            if (mod.isVariable()) {
                line += mod.getTheoreticPtm().getName();
            }
        }
        line += SEPARATOR;
        line += spectrumMatch.getSpectrum().getPrecursor().getCharge() + SEPARATOR;
        line += spectrumMatch.getSpectrum().getSpectrumTitle() + SEPARATOR;
        line += spectrumMatch.getSpectrum().getFileName() + SEPARATOR;
        for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getFile() + " ";
            }
        }
        line += SEPARATOR;
        line += spectrumMatch.getBestAssumption().getDeltaMass() + SEPARATOR;
        PeptideAssumption assumption = spectrumMatch.getFirstHit(Advocate.MASCOT);
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                MascotScore score = (MascotScore) assumption.getUrParam(new MascotScore(0));
                line += score.getScore() + "";
            }
        }
        line += SEPARATOR;
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getEValue() + "";
            }
        }
        line += SEPARATOR;
        assumption = spectrumMatch.getFirstHit(Advocate.OMSSA);
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getEValue() + "";
            }
        }
        line += SEPARATOR;
        assumption = spectrumMatch.getFirstHit(Advocate.XTANDEM);
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getEValue() + "";
            }
        }
        line += SEPARATOR;
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);
        try {
        line += probabilities.getSpectrumProbabilityScore() + SEPARATOR
                + probabilities.getSpectrumProbability() + SEPARATOR;
        } catch (Exception e) {
        line += SEPARATOR + SEPARATOR;
        }
        if (spectrumMatch.getBestAssumption().isDecoy()) {
            line += "1" + SEPARATOR;
        } else {
            line += "0" + SEPARATOR;
        }
        try {
        if (probabilities.isValidated()) {
            line += "1";
        } else {
            line += "0";
        }
        } catch (Exception e) {

        }
        line += "\n";
        return line;
    }
}
