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
import eu.isas.peptideshaker.myparameters.SVParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import javax.swing.JOptionPane;

/**
 * Contains methods for exporting the search engine results to csv files.
 *
 * @author Marc Vaudel
 */
public class CsvExporter {

    private static final String separator = "\t";
    private MsExperiment experiment;
    private Sample sample;
    private int replicateNumber;
    private String proteinFile;
    private String peptideFile;
    private String spectrumFile;

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
        spectrumFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_psms.txt";
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
            Writer spectrumWriter = new BufferedWriter(new FileWriter(new File(folder, spectrumFile)));
            String content = "Protein" + separator + "n peptides" + separator + "n spectra" + separator + "p score" + separator + "p" + separator + "Decoy\n";
            proteinWriter.write(content);
            content = "Protein(s)" + separator + "Sequence" + separator + "Variable Modification(s)" + separator + "n Spectra" + separator + "p score" + separator + "p" + separator + "Decoy\n";
            peptideWriter.write(content);
            content = "Protein(s)" + separator + "Sequence" + separator + "Variable Modification(s)" + separator + "Charge" + separator + "Spectrum" + separator + "Spectrum File" 
                    + separator + "Identification File(s)" + separator + "Mass Error" + separator + "Mascot Score" + separator + "Mascot E-Value" + separator + "OMSSA E-Value"
                    + separator + "X!Tandem E-Value" + separator + "p score" + separator + "p" + separator + "Decoy\n";
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
        }
    }

    /**
     * Returns the protein match as a line of text.
     *
     * @param proteinMatch the protein match to export
     * @return the protein match as a line of text
     */
    private String getLine(ProteinMatch proteinMatch) {
        String line = "";
        line += proteinMatch.getTheoreticProtein().getAccession() + separator;
        line += proteinMatch.getPeptideMatches().size() + separator;
        line += proteinMatch.getSpectrumCount() + separator;
        SVParameter probabilities = new SVParameter();
        probabilities = (SVParameter) proteinMatch.getUrParam(probabilities);
        line += probabilities.getProteinProbabilityScore() + separator
                + probabilities.getProteinProbability() + separator;
        if (proteinMatch.isDecoy()) {
            line += "1\n";
        } else {
            line += "0\n";
        }
        return line;
    }

    /**
     * Returns the peptide match as a line of text.
     *
     * @param peptideMatch the peptide match to export
     * @return the peptide match as a line of text
     */
    private String getLine(PeptideMatch peptideMatch) {
        String line = "";
        for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
            line += protein.getAccession() + " ";
        }
        line += separator;
        line += peptideMatch.getTheoreticPeptide().getSequence() + separator;
        for (ModificationMatch mod : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (mod.isVariable()) {
                line += mod.getTheoreticPtm().getName();
            }
        }
        line += separator;
        line += peptideMatch.getSpectrumMatches().size() + separator;
        SVParameter probabilities = new SVParameter();
        probabilities = (SVParameter) peptideMatch.getUrParam(probabilities);
        line += probabilities.getPeptideProbabilityScore() + separator
                + probabilities.getPeptideProbability() + separator;
        if (peptideMatch.isDecoy()) {
            line += "1\n";
        } else {
            line += "0\n";
        }
        return line;
    }

    /**
     * Returns the spectrum match as a line of text.
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
        line += separator;
        line += bestAssumption.getSequence() + separator;
        for (ModificationMatch mod : bestAssumption.getModificationMatches()) {
            if (mod.isVariable()) {
                line += mod.getTheoreticPtm().getName();
            }
        }
        line += separator;
        line += spectrumMatch.getSpectrum().getPrecursor().getCharge() + separator;
        line += spectrumMatch.getSpectrum().getSpectrumTitle() + separator;
        line += spectrumMatch.getSpectrum().getFileName() + separator;
        for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getFile() + " ";
            }
        }
        line += separator;
        line += spectrumMatch.getBestAssumption().getDeltaMass() + separator;
        PeptideAssumption assumption = spectrumMatch.getFirstHit(Advocate.MASCOT);
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                MascotScore score = (MascotScore) assumption.getUrParam(new MascotScore(0));
                line += score.getScore() + "";
            }
        }
        line += separator;
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getEValue() + "";
            }
        }
        line += separator;
        assumption = spectrumMatch.getFirstHit(Advocate.OMSSA);
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getEValue() + "";
            }
        }
        line += separator;
        assumption = spectrumMatch.getFirstHit(Advocate.XTANDEM);
        if (assumption != null) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getEValue() + "";
            }
        }
        line += separator;
        SVParameter probabilities = new SVParameter();
        probabilities = (SVParameter) spectrumMatch.getUrParam(probabilities);
        line += probabilities.getSpectrumProbabilityScore() + separator
                + probabilities.getSpectrumProbability() + separator;
        if (spectrumMatch.getBestAssumption().isDecoy()) {
            line += "1\n";
        } else {
            line += "0\n";
        }
        return line;
    }
}
