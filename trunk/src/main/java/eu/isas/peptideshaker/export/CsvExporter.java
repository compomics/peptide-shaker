package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
     * Name of the file containing the identification information at the peptide assumption level
     */
    private String assumptionFile;
    /**
     * The sequence database
     */
    private SequenceDataBase db;
    /**
     * The enzyme used for digestion
     */
    private Enzyme enzyme;

    /**
     * Creates a CsvExporter object.
     *
     * @param experiment the ms experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
     */
    public CsvExporter(MsExperiment experiment, Sample sample, int replicateNumber, Enzyme enzyme) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.enzyme = enzyme;
        db = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSequenceDataBase();

        proteinFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_proteins.txt";
        peptideFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_peptides.txt";
        psmFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_psms.txt";
        assumptionFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_assumptions.txt";
    }

    /**
     * Exports the results to csv files.
     *
     * @param folder the folder to store the results in.
     * @return true if the export was sucessfull
     */
    public boolean exportResults(File folder) {

        try {
            Writer proteinWriter = new BufferedWriter(new FileWriter(new File(folder, proteinFile)));
            Writer peptideWriter = new BufferedWriter(new FileWriter(new File(folder, peptideFile)));
            Writer spectrumWriter = new BufferedWriter(new FileWriter(new File(folder, psmFile)));
            Writer assumptionWriter = new BufferedWriter(new FileWriter(new File(folder, assumptionFile)));

            String content = "Protein" + SEPARATOR + "Other proteins" + SEPARATOR + "Group class" + SEPARATOR + "n peptides" + SEPARATOR + "n spectra" + SEPARATOR + "n peptides validated" + SEPARATOR + "n spectra validated" + SEPARATOR + "nPossibilities" + SEPARATOR + "Protein length" + SEPARATOR + "p score"
                    + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + SEPARATOR + "Description" + "\n";
            proteinWriter.write(content);

            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR
                    + "n Spectra" + SEPARATOR + "n Spectra Validated" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            peptideWriter.write(content);

            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR
                    + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            spectrumWriter.write(content);

            content = "Search Engine" + SEPARATOR + "Rank" + SEPARATOR + "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR
                    + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            assumptionWriter.write(content);

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

            for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                assumptionWriter.write(getLines(spectrumMatch));
            }

            proteinWriter.close();
            peptideWriter.close();
            spectrumWriter.close();
            assumptionWriter.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (MzMLUnmarshallerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exports the protein match as a line of text.
     *
     * @param proteinMatch the protein match to export
     * @return the protein match as a line of text
     */
    private String getLine(ProteinMatch proteinMatch) {
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);
        String line = "";
        line += proteinMatch.getMainMatch().getAccession() + SEPARATOR;
        for (String otherAccession : proteinMatch.getTheoreticProteinsAccessions()) {
            if (!otherAccession.equals(proteinMatch.getMainMatch().getAccession())) {
                line += otherAccession + " ";
            }
        }
        line += SEPARATOR + probabilities.getGroupClass() + SEPARATOR;
        line += proteinMatch.getPeptideCount() + SEPARATOR;
        line += proteinMatch.getSpectrumCount() + SEPARATOR;
        int nValidatedPeptides = 0;
        int nValidatedPsms = 0;
        PSParameter psParameter = new PSParameter();
        for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
            psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);
            if (psParameter.isValidated() && peptideMatch.getSpectrumCount() > 0) {
                nValidatedPeptides++;
                for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
                    psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);
                    if (psParameter.isValidated() && spectrumMatch.getBestAssumption().getPeptide().isSameAs(peptideMatch.getTheoreticPeptide())) {
                        nValidatedPsms++;
                    }
                }
            }
        }
        line += nValidatedPeptides + SEPARATOR + nValidatedPsms + SEPARATOR;
            line += db.getProtein(proteinMatch.getMainMatch().getAccession()).getNPossiblePeptides(enzyme) + SEPARATOR;
            line += db.getProtein(proteinMatch.getMainMatch().getAccession()).getSequence().length() + SEPARATOR;

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
                line += "1" + SEPARATOR;
            } else {
                line += "0" + SEPARATOR;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            line += db.getProteinHeader(proteinMatch.getMainMatch().getAccession()).getDescription();
        
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
        line += peptideMatch.getSpectrumCount() + SEPARATOR;
        PSParameter probabilities = new PSParameter();
        int nSpectraValidated = 0;
        for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
            probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);
            if (probabilities.isValidated() && spectrumMatch.getBestAssumption().getPeptide().isSameAs(peptideMatch.getTheoreticPeptide())) {
                nSpectraValidated++;
            }
        }
        line += nSpectraValidated + SEPARATOR;
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
            e.printStackTrace();
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
    private String getLine(SpectrumMatch spectrumMatch) throws MzMLUnmarshallerException {
        MSnSpectrum spectrum = (MSnSpectrum) experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSpectrumCollection().getSpectrum(spectrumMatch.getKey());

        String line = "";

        Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();

        for (Protein protein : bestAssumption.getParentProteins()) {
            line += protein.getAccession() + " ";
        }

        line += SEPARATOR;
        line += bestAssumption.getSequence() + SEPARATOR;

        for (ModificationMatch mod : bestAssumption.getModificationMatches()) {
            if (mod.isVariable()) {
                line += mod.getTheoreticPtm().getName() + "(" + mod.getModificationSite() + ") ";
            }
        }

        line += SEPARATOR;
        line += spectrum.getPrecursor().getCharge() + SEPARATOR;
        line += spectrum.getSpectrumTitle() + SEPARATOR;
        line += spectrum.getFileName() + SEPARATOR;

        for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getFile() + " ";
            }
        }

        line += SEPARATOR;
        line += spectrumMatch.getBestAssumption().getPeptide().getMass() + SEPARATOR;
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
            line += probabilities.getPsmProbabilityScore() + SEPARATOR
                    + probabilities.getPsmProbability() + SEPARATOR;
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
            e.printStackTrace();
        }

        line += "\n";

        return line;
    }

    /**
     * Exports the peptide assumptions from a peptide spectrum match as lines of text.
     *
     * @param spectrumMatch the spectrum match to export
     * @return the peptide assumptions from a peptide spectrum match as lines of text
     */
    private String getLines(SpectrumMatch spectrumMatch) throws MzMLUnmarshallerException {
        MSnSpectrum spectrum = (MSnSpectrum) experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSpectrumCollection().getSpectrum(spectrumMatch.getKey());

        String line = "";
        ArrayList<Integer> searchEngines = spectrumMatch.getAdvocates();
        Collections.sort(searchEngines);
        ArrayList<Double> eValues;
        int rank;
        for (int se : searchEngines) {
            eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(se).keySet());
            Collections.sort(eValues);
            rank = 1;
            for (double eValue : eValues) {
                for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                    if (se == Advocate.MASCOT) {
                        line += "M" + SEPARATOR;
                    } else if (se == Advocate.OMSSA) {
                        line += "O" + SEPARATOR;
                    } else if (se == Advocate.XTANDEM) {
                        line += "X" + SEPARATOR;
                    }
                    line += rank + SEPARATOR;
                    for (Protein protein : assumption.getPeptide().getParentProteins()) {
                        line += protein.getAccession() + " ";
                    }

                    line += SEPARATOR;
                    line += assumption.getPeptide().getSequence() + SEPARATOR;

                    for (ModificationMatch mod : assumption.getPeptide().getModificationMatches()) {
                        if (mod.isVariable()) {
                            line += mod.getTheoreticPtm().getName() + "(" + mod.getModificationSite() + ") ";
                        }
                    }

                    line += SEPARATOR;
                    line += spectrum.getPrecursor().getCharge() + SEPARATOR;
                    line += spectrum.getSpectrumTitle() + SEPARATOR;
                    line += spectrum.getFileName() + SEPARATOR;

                    line += assumption.getFile() + SEPARATOR;

                    line += spectrumMatch.getBestAssumption().getPeptide().getMass() + SEPARATOR;
                    line += spectrumMatch.getBestAssumption().getDeltaMass() + SEPARATOR;

                    if (se == Advocate.MASCOT) {
                        MascotScore score = (MascotScore) assumption.getUrParam(new MascotScore(0));
                        line += score.getScore() + SEPARATOR;
                        line += assumption.getEValue() + SEPARATOR;
                    } else {
                        line += SEPARATOR + SEPARATOR;
                    }

                    if (se == Advocate.OMSSA) {
                        line += assumption.getEValue() + "";
                    }
                    line += SEPARATOR;

                    if (se == Advocate.XTANDEM) {
                        line += assumption.getEValue() + "";
                    }
                    line += SEPARATOR;

                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);

                    try {
                        line += probabilities.getPsmProbabilityScore() + SEPARATOR
                                + probabilities.getPsmProbability() + SEPARATOR;
                    } catch (Exception e) {
                        line += SEPARATOR + SEPARATOR;
                    }

                    if (assumption.isDecoy()) {
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
                        e.printStackTrace();
                    }

                    line += "\n";
                    rank++;
                }
            }
        }
        return line;
    }
}
