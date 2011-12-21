package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
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
     * The enzyme used for digestion
     */
    private Enzyme enzyme;
    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The identification
     */
    private Identification identification;

    /**
     * Creates a CsvExporter object.
     *
     * @param experiment the ms experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
     * @param enzyme the enzyme used 
     */
    public CsvExporter(MsExperiment experiment, Sample sample, int replicateNumber, Enzyme enzyme) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.enzyme = enzyme;

        proteinFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_proteins.txt";
        peptideFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_peptides.txt";
        psmFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_psms.txt";
        assumptionFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_assumptions.txt";
    }

    /**
     * Exports the results to csv files.
     *
     * @param progressDialog a progress dialog, can be null
     * @param folder the folder to store the results in.
     * @return true if the export was sucessfull
     */
    public boolean exportResults(ProgressDialogX progressDialog, File folder) {

        try {
            Writer proteinWriter = new BufferedWriter(new FileWriter(new File(folder, proteinFile)));
            Writer peptideWriter = new BufferedWriter(new FileWriter(new File(folder, peptideFile)));
            Writer spectrumWriter = new BufferedWriter(new FileWriter(new File(folder, psmFile)));
            Writer assumptionWriter = new BufferedWriter(new FileWriter(new File(folder, assumptionFile)));

            String content = "Protein" + SEPARATOR + "Equivalent proteins" + SEPARATOR + "Group class" + SEPARATOR + "n peptides" + SEPARATOR + "n spectra"
                    + SEPARATOR + "n peptides validated" + SEPARATOR + "n spectra validated" + SEPARATOR + "nPossibilities" + SEPARATOR + "Protein length" + SEPARATOR + "p score"
                    + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + SEPARATOR + "Description" + "\n";
            proteinWriter.write(content);

            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM location confidence" + SEPARATOR
                    + "n Spectra" + SEPARATOR + "n Spectra Validated" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            peptideWriter.write(content);

            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM location confidence" + SEPARATOR
                    + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            spectrumWriter.write(content);

            content = "Search Engine" + SEPARATOR + "Rank" + SEPARATOR + "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR
                    + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            assumptionWriter.write(content);

            identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

            if (progressDialog != null) {
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(identification.getProteinIdentification().size()
                        + identification.getPeptideIdentification().size()
                        + 2 * identification.getSpectrumIdentification().size());
            }

            int progress = 0;
            for (String proteinKey : identification.getProteinIdentification()) {
                proteinWriter.write(getProteinLine(proteinKey));
                progress++;
                progressDialog.setValue(progress);
            }

            for (String peptideKey : identification.getPeptideIdentification()) {
                peptideWriter.write(getPeptideLine(peptideKey));
                progress++;
                progressDialog.setValue(progress);
            }

            for (String spectrumKey : identification.getSpectrumIdentification()) {
                spectrumWriter.write(getSpectrumLine(spectrumKey));
                progress++;
                progressDialog.setValue(progress);
            }

            for (String spectrumKey : identification.getSpectrumIdentification()) {
                assumptionWriter.write(getAssumptionLines(spectrumKey));
                progress++;
                progressDialog.setValue(progress);
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
        } catch (Exception e) {
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
    private String getProteinLine(String proteinKey) throws Exception {
        String line = "";
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) identification.getMatchParameter(proteinKey, probabilities);
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
        line += proteinMatch.getMainMatch() + SEPARATOR;
        for (String otherAccession : proteinMatch.getTheoreticProteinsAccessions()) {
            if (!otherAccession.equals(proteinMatch.getMainMatch())) {
                line += otherAccession + " ";
            }
        }
        line += SEPARATOR + probabilities.getGroupClass() + SEPARATOR;
        line += proteinMatch.getPeptideCount() + SEPARATOR;
        int nSpectra = 0;
        int nValidatedPeptides = 0;
        int nValidatedPsms = 0;
        PSParameter psParameter = new PSParameter();
        PeptideMatch peptideMatch;
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            peptideMatch = identification.getPeptideMatch(peptideKey);
            nSpectra += peptideMatch.getSpectrumCount();
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            if (psParameter.isValidated()) {
                nValidatedPeptides++;
                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                    psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
                    if (psParameter.isValidated()) {
                        nValidatedPsms++;
                    }
                }
            }
        }

        line += nSpectra + SEPARATOR;
        line += nValidatedPeptides + SEPARATOR + nValidatedPsms + SEPARATOR;
        try {
        line += sequenceFactory.getProtein(proteinMatch.getMainMatch()).getNPossiblePeptides(enzyme) + SEPARATOR;
        line += sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence().length() + SEPARATOR;
        } catch (Exception e) {
            line+= "protein not found " + SEPARATOR + SEPARATOR;
        }
        
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

        if (probabilities.isValidated()) {
            line += "1" + SEPARATOR;
        } else {
            line += "0" + SEPARATOR;
        }
        try {
        line += sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
        } catch (Exception e) {
            line += "Protein not found";
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
    private String getPeptideLine(String peptideKey) throws Exception {

        String line = "";
        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
        for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
            line += protein + " ";
        }

        line += SEPARATOR;
        line += peptideMatch.getTheoreticPeptide().getSequence() + SEPARATOR;

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }
        boolean first = true, first2;
        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
        Collections.sort(modifications);
        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            first2 = true;
            line += mod + "(";
            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                line += ", ";
                }
                line += aa;
            }
            line += ")";
        }
        line += SEPARATOR;
        PSPtmScores ptmScores = new PSPtmScores();
        first = true;
        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
            line += mod + " (";
            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();
                if (ptmConfidence == PtmScoring.NOT_FOUND) {
                    line += "Not Scored"; // Well this should not happen
                } else if (ptmConfidence == PtmScoring.RANDOM) {
                    line += "Random";
                } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                    line += "Doubtfull";
                } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                    line += "Confident";
                } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                    line += "Very Confident";
                }
            } else {
                line += "Not Scored";
            }
            line += ")";
        }
        line += SEPARATOR;

        line += SEPARATOR;
        line += peptideMatch.getSpectrumCount() + SEPARATOR;
        PSParameter probabilities = new PSParameter();
        int nSpectraValidated = 0;
        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
            probabilities = (PSParameter) identification.getMatchParameter(spectrumKey, probabilities);
            if (probabilities.isValidated()) {
                nSpectraValidated++;
            }
        }
        line += nSpectraValidated + SEPARATOR;
        probabilities = (PSParameter) identification.getMatchParameter(peptideKey, probabilities);

        line += probabilities.getPeptideProbabilityScore() + SEPARATOR
                + probabilities.getPeptideProbability() + SEPARATOR;

        if (peptideMatch.isDecoy()) {
            line += "1" + SEPARATOR;
        } else {
            line += "0" + SEPARATOR;
        }
        if (probabilities.isValidated()) {
            line += "1";
        } else {
            line += "0";
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
    private String getSpectrumLine(String spectrumKey) throws Exception {

        String line = "";

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);

        Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();


        for (String protein : bestAssumption.getParentProteins()) {
            line += protein + " ";
        }

        line += SEPARATOR;
        line += bestAssumption.getSequence() + SEPARATOR;

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        for (ModificationMatch modificationMatch : bestAssumption.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }
        boolean first = true, first2;
        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
        Collections.sort(modifications);
        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            first2 = true;
            line += mod + "(";
            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                line += ", ";
                }
                line += aa;
            }
            line += ")";
        }
        line += SEPARATOR;
        PSPtmScores ptmScores = new PSPtmScores();
        first = true;
        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
            line += mod + " (";
            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();
                if (ptmConfidence == PtmScoring.NOT_FOUND) {
                    line += "Not Scored"; // Well this should not happen
                } else if (ptmConfidence == PtmScoring.RANDOM) {
                    line += "Random";
                } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                    line += "Doubtfull";
                } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                    line += "Confident";
                } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                    line += "Very Confident";
                }
            } else {
                line += "Not Scored";
            }
            line += ")";
        }
        line += SEPARATOR;
        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
        Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);
        line += spectrumMatch.getBestAssumption().getIdentificationCharge().value + SEPARATOR;
        line += fileName + SEPARATOR;
        line += spectrumTitle + SEPARATOR;

        for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                line += assumption.getFile() + " ";
            }
        }

        line += SEPARATOR;
        line += spectrumMatch.getBestAssumption().getPeptide().getMass() + SEPARATOR;
        line += Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), true)) + SEPARATOR;
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
        probabilities = (PSParameter) identification.getMatchParameter(spectrumKey, probabilities);

        line += probabilities.getPsmProbabilityScore() + SEPARATOR
                + probabilities.getPsmProbability() + SEPARATOR;

        if (spectrumMatch.getBestAssumption().isDecoy()) {
            line += "1" + SEPARATOR;
        } else {
            line += "0" + SEPARATOR;
        }

        if (probabilities.isValidated()) {
            line += "1";
        } else {
            line += "0";
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
    private String getAssumptionLines(String spectrumKey) throws Exception {

        String line = "";
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        ArrayList<Integer> searchEngines = spectrumMatch.getAdvocates();
        Collections.sort(searchEngines);
        ArrayList<Double> eValues;
        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
        Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);
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
                    for (String protein : assumption.getPeptide().getParentProteins()) {
                        line += protein + " ";
                    }

                    line += SEPARATOR;
                    line += assumption.getPeptide().getSequence() + SEPARATOR;

                    for (ModificationMatch mod : assumption.getPeptide().getModificationMatches()) {
                        if (mod.isVariable()) {
                            line += mod.getTheoreticPtm() + "(" + mod.getModificationSite() + ") ";
                        }
                    }

                    line += SEPARATOR;
                    line += assumption.getIdentificationCharge().value + SEPARATOR;
                    line += spectrumTitle + SEPARATOR;
                    line += fileName + SEPARATOR;

                    line += assumption.getFile() + SEPARATOR;

                    line += spectrumMatch.getBestAssumption().getPeptide().getMass() + SEPARATOR;
                    line += Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), true)) + SEPARATOR;

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
                    probabilities = (PSParameter) assumption.getUrParam(probabilities);

                    try {
                        line += assumption.getEValue() + SEPARATOR
                                + probabilities.getSearchEngineProbability() + SEPARATOR;
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
