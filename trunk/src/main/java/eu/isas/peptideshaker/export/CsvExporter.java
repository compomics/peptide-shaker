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
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
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
     * Separator for csv export. Hard coded for now, could be user setting.
     */
    private static final String SEPARATOR = "\t";
    /**
     * The experiment to export.
     */
    private MsExperiment experiment;
    /**
     * The sample considered.
     */
    private Sample sample;
    /**
     * The replicate considered.
     */
    private int replicateNumber;
    /**
     * Name of the file containing the identification information at the protein
     * level.
     */
    private String proteinFile;
    /**
     * Name of the file containing the identification information at the peptide
     * level.
     */
    private String peptideFile;
    /**
     * Name of the file containing the identification information at the psm
     * level.
     */
    private String psmFile;
    /**
     * Name of the file containing the identification information at the peptide
     * assumption level.
     */
    private String assumptionFile;
    /**
     * The enzyme used for digestion.
     */
    private Enzyme enzyme;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The identification features generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;

    /**
     * Creates a CsvExporter object.
     *
     * @param experiment the ms experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
     * @param enzyme the enzyme used
     * @param identificationFeaturesGenerator
     */
    public CsvExporter(MsExperiment experiment, Sample sample, int replicateNumber, Enzyme enzyme, IdentificationFeaturesGenerator identificationFeaturesGenerator) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.enzyme = enzyme;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;

        proteinFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_proteins.txt";
        peptideFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_peptides.txt";
        psmFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_psms.txt";
        //assumptionFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_assumptions.txt";
    }

    /**
     * Exports the results to csv files.
     *
     * @param progressDialog a progress dialog, can be null
     * @param cancelProgress
     * @param folder the folder to store the results in.
     * @return true if the export was sucessfull
     */
    public boolean exportResults(ProgressDialogX progressDialog, boolean cancelProgress, File folder) {

        try {
            // write the proteins
            String lMessage = "Exporting Proteins. Please Wait...";
            if (progressDialog != null) {
                progressDialog.setTitle(lMessage);
            }else{
                System.out.println(lMessage);
            }

            Writer proteinWriter = new BufferedWriter(new FileWriter(new File(folder, proteinFile)));
            String content = "Protein" + SEPARATOR + "Equivalent proteins" + SEPARATOR + "Group class" + SEPARATOR + "n peptides" + SEPARATOR + "n spectra"
                    + SEPARATOR + "n peptides validated" + SEPARATOR + "n spectra validated" + SEPARATOR + "MW" + SEPARATOR + "NSAF" + SEPARATOR + "p score"
                    + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + SEPARATOR + "Description" + "\n";
            proteinWriter.write(content);

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

                if (progressDialog != null) {
                    progressDialog.setValue(++progress);
                }

                if (cancelProgress) {
                    break;
                }
            }

            proteinWriter.close();


            // write the peptides
            lMessage = "Exporting Peptides. Please Wait...";
            if (progressDialog != null) {
                progressDialog.setTitle(lMessage);
            }else{
                System.out.println(lMessage);
            }

            Writer peptideWriter = new BufferedWriter(new FileWriter(new File(folder, peptideFile)));
            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM location confidence" + SEPARATOR
                    + "n Spectra" + SEPARATOR + "n Spectra Validated" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            peptideWriter.write(content);

            for (String peptideKey : identification.getPeptideIdentification()) {

                peptideWriter.write(getPeptideLine(peptideKey));

                if (progressDialog != null) {
                    progressDialog.setValue(++progress);
                }

                if (cancelProgress) {
                    break;
                }
            }

            peptideWriter.close();


            // write the spectra
            lMessage = "Exporting Spectra. Please Wait...";
            if (progressDialog != null) {
                progressDialog.setTitle(lMessage);
            }else{
                System.out.println(lMessage);
            }

            Writer spectrumWriter = new BufferedWriter(new FileWriter(new File(folder, psmFile)));
            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM location confidence" + SEPARATOR
                    + "Spectrum Charge" + SEPARATOR + "Identification Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
                    + SEPARATOR + "Precursor RT" + SEPARATOR + "Precursor mz" + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR + 
                    "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
            spectrumWriter.write(content);

            for (String spectrumKey : identification.getSpectrumIdentification()) {

                spectrumWriter.write(getSpectrumLine(spectrumKey));

                if (progressDialog != null) {
                    progressDialog.setValue(++progress);
                }

                if (cancelProgress) {
                    break;
                }
            }

            spectrumWriter.close();


            // write the assumptions
//            if (progressDialog != null) {
//                progressDialog.setTitle("Exporting Assumptions. Please Wait...");
//            }
//            
//            Writer assumptionWriter = new BufferedWriter(new FileWriter(new File(folder, assumptionFile)));
//            content = "Search Engine" + SEPARATOR + "Rank" + SEPARATOR + "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR
//                    + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
//                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
//                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + "\n";
//            assumptionWriter.write(content);

//            for (String spectrumKey : identification.getSpectrumIdentification()) {
//                assumptionWriter.write(getAssumptionLines(spectrumKey));
//                progressDialog.setValue(++progress);
//            }
//            
//            assumptionWriter.close();

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
        
        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??

        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) identification.getMatchParameter(proteinKey, probabilities);
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
        String line = proteinMatch.getMainMatch() + SEPARATOR;

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

        for (String peptideKey : proteinMatch.getPeptideMatches()) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
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
            line += sequenceFactory.getProtein(proteinMatch.getMainMatch()).computeMolecularWeight() + SEPARATOR;
            line += identificationFeaturesGenerator.getSpectrumCounting(proteinKey) + SEPARATOR;
        } catch (Exception e) {
            line += "protein not found " + SEPARATOR + SEPARATOR;
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
        
        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??

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
        
        boolean first = true;
        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
        Collections.sort(modifications);
        
        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            
            boolean first2 = true;
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
        first = true;
        
        for (String mod : modifications) {
            
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            
            PSPtmScores ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
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
        
        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();

        String line = "";

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
        
        boolean first = true;
        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
        Collections.sort(modifications);
        
        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line += ", ";
            }
            
            boolean first2 = true;
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
            
            if (spectrumMatch.getUrParam(ptmScores) != null) {
                
                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
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
        }
        
        line += SEPARATOR;
        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
        Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);
        line += precursor.getPossibleChargesAsString() + SEPARATOR;
        line += spectrumMatch.getBestAssumption().getIdentificationCharge().value + SEPARATOR;
        line += fileName + SEPARATOR;
        line += spectrumTitle + SEPARATOR;

        ArrayList<String> fileNames = new ArrayList<String>();
        
        for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption.getPeptide().isSameAs(bestAssumption)) {
                if (!fileNames.contains(assumption.getFile())) {
                    fileNames.add(assumption.getFile());
                }
            }
        }
        
        Collections.sort(fileNames);
        
        for (String name : fileNames) {
            line += name + " ";
        }

        line += SEPARATOR;
        line += precursor.getRt() + SEPARATOR;
        line += precursor.getMz() + SEPARATOR;
        line += spectrumMatch.getBestAssumption().getPeptide().getMass() + SEPARATOR;
        line += Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), true)) + SEPARATOR;
        Double mascotEValue = null;
        Double omssaEValue = null;
        Double xtandemEValue = null;
        double mascotScore = 0;
        
        for (int se : spectrumMatch.getAdvocates()) {
            for (double eValue : spectrumMatch.getAllAssumptions(se).keySet()) {
                for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                    if (assumption.getPeptide().isSameAs(bestAssumption)) {
                        if (se == Advocate.MASCOT) {
                            if (mascotEValue == null || mascotEValue > eValue) {
                                mascotEValue = eValue;
                                mascotScore = ((MascotScore) assumption.getUrParam(new MascotScore(0))).getScore();
                            }
                        } else if (se == Advocate.OMSSA) {
                            if (omssaEValue == null || omssaEValue > eValue) {
                                omssaEValue = eValue;
                            }
                        } else if (se == Advocate.XTANDEM) {
                            if (xtandemEValue == null || xtandemEValue > eValue) {
                                xtandemEValue = eValue;
                            }
                        }
                    }
                }
            }
        }

        if (mascotEValue != null) {
            line += mascotScore;
        }
        
        line += SEPARATOR;
        
        if (mascotEValue != null) {
            line += mascotEValue;
        }

        line += SEPARATOR;
        
        if (omssaEValue != null) {
            line += omssaEValue;
        }

        line += SEPARATOR;
        
        if (xtandemEValue != null) {
            line += xtandemEValue;
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
     * Exports the peptide assumptions from a peptide spectrum match as lines of
     * text.
     *
     * @param spectrumMatch the spectrum match to export
     * @return the peptide assumptions from a peptide spectrum match as lines of
     * text
     */
    private String getAssumptionLines(String spectrumKey) throws Exception {

        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??
        
        String line = "";
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        ArrayList<Integer> searchEngines = spectrumMatch.getAdvocates();
        Collections.sort(searchEngines);
        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
        Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);
        
        for (int se : searchEngines) {
            
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(se).keySet());
            Collections.sort(eValues);
            int rank = 1;
            
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
