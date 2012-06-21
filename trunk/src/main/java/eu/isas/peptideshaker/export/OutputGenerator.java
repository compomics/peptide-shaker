package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.AdvocateFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JOptionPane;

/**
 * This class will generate the output as requested by the user.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class OutputGenerator {

    /**
     * The main gui.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The corresponding identification.
     */
    private Identification identification;
    /**
     * The separator (tab by default).
     */
    public static final String SEPARATOR = "\t";
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param peptideShakerGUI
     */
    public OutputGenerator(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
    }

    /**
     * Sends the desired protein output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     *
     * @param aProteinKeys The list of protein keys to output. If null, the
     * identification list will be used
     * @param aIndexes boolean indicating whether the first column shall be used
     * for line number
     * @param aOnlyValidated boolean indicating whether only validated hits
     * shall be returned
     * @param aMainAccession boolean indicating whether the accessions shall be
     * output
     * @param aOtherAccessions boolean indicating whether the the additional
     * protein accesion numbers should be included or not
     * @param aPiDetails boolean indicating whether protein inference details
     * shall be output
     * @param aDescription boolean indicating whether protein description of the
     * main match shall be output
     * @param aNPeptides boolean indicating whether the number of validated
     * peptides shall be output
     * @param aEmPAI boolean indicating whether the emPAI index shall be output
     * @param aSequenceCoverage boolean indicating whether the sequence coverage
     * shall be output
     * @param aPtmSummary boolean indicating whether a ptm summary shall be
     * output
     * @param aNSpectra boolean indicating whether the number of validated
     * spectra shall be output
     * @param aNsaf boolean indicating whether the NSAF index shall be output
     * @param aScore boolean indicating whether the protein match score shall be
     * output
     * @param aConfidence boolean indicating whether the confidence shall be
     * output
     * @param aIncludeHeader boolean indicating whether the header shall be
     * output
     * @param aOnlyStarred boolean indicating whether only starred proteins
     * shall be output
     * @param aShowStar boolean indicating wheter the starred proteins will be
     * indicated in a separate column
     * @param aIncludeHidden boolean indicating whether hidden hits shall be
     * output
     * @throws IOException exception thrown whenever an error occurred while
     * writing the results
     */
    public void getProteinsOutput(ArrayList<String> aProteinKeys, boolean aIndexes, boolean aOnlyValidated, boolean aMainAccession, boolean aOtherAccessions, boolean aPiDetails,
            boolean aDescription, boolean aNPeptides, boolean aEmPAI, boolean aSequenceCoverage, boolean aPtmSummary, boolean aNSpectra, boolean aNsaf,
            boolean aScore, boolean aConfidence, boolean aIncludeHeader, boolean aOnlyStarred, boolean aShowStar, boolean aIncludeHidden) throws IOException {
        
        // create final versions of all variables use inside the export thread
        final ArrayList<String> proteinKeys;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean mainAccession = aMainAccession;
        final boolean otherAccessions = aOtherAccessions;
        final boolean piDetails = aPiDetails;
        final boolean description = aDescription;
        final boolean nPeptides = aNPeptides;
        final boolean emPAI = aEmPAI;
        final boolean sequenceCoverage = aSequenceCoverage;
        final boolean ptmSummary = aPtmSummary;
        final boolean nSpectra = aNSpectra;
        final boolean nsaf = aNsaf;
        final boolean score = aScore;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean showStar = aShowStar;
        final boolean includeHidden = aIncludeHidden;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);
        
        if (selectedFile != null) {
            
            final String filePath = selectedFile.getPath();

            // change the peptide shaker icon to a "waiting version"
            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
            
            writer = new BufferedWriter(new FileWriter(selectedFile));
            
            if (aProteinKeys == null) {
                proteinKeys = identification.getProteinIdentification();
            } else {
                proteinKeys = aProteinKeys;
            }
            
            progressDialog = new ProgressDialogX(peptideShakerGUI, true);
            progressDialog.setTitle("Copying to File. Please Wait...");
            progressDialog.setIndeterminate(true);
            
            new Thread(new Runnable() {
                
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();
            
            new Thread("ExportThread") {
                
                @Override
                public void run() {
                    
                    try {
                        if (includeHeader) {
                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (mainAccession) {
                                writer.write("Accession" + SEPARATOR);
                            }
                            if (otherAccessions) {
                                writer.write("Other Protein(s)" + SEPARATOR);
                            }
                            if (piDetails) {
                                writer.write("Protein Inference Class" + SEPARATOR);
                            }
                            if (description) {
                                writer.write("Description" + SEPARATOR);
                            }
                            if (sequenceCoverage) {
                                writer.write("Sequence Coverage (%)" + SEPARATOR);
                                writer.write("Maximal Expected Sequence Coverage (%)" + SEPARATOR);
                            }
                            if (ptmSummary) {
                                writer.write("Confident PTM Sites" + SEPARATOR);
                                writer.write("Other PTM Sites" + SEPARATOR);
                            }
                            if (nPeptides) {
                                writer.write("#Validated Peptides" + SEPARATOR);
                            }
                            if (nSpectra) {
                                writer.write("#Validated Spectra" + SEPARATOR);
                            }
                            if (emPAI) {
                                writer.write("emPAI" + SEPARATOR);
                            }
                            if (nsaf) {
                                writer.write("NSAF" + SEPARATOR);
                            }
                            if (score) {
                                writer.write("Score" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            if (!onlyValidated) {
                                writer.write("Validated" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }
                            if (!onlyStarred && showStar) {
                                writer.write("Starred" + SEPARATOR);
                            }
                            writer.write("\n");
                        }
                        
                        PSParameter proteinPSParameter = new PSParameter();
                        int progress = 0, proteinCounter = 0;
                        
                        progressDialog.setIndeterminate(false);
                        progressDialog.setMaxProgressValue(proteinKeys.size());
                        
                        for (String proteinKey : proteinKeys) {
                            
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }
                            
                            proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, proteinPSParameter);
                            
                            if (!ProteinMatch.isDecoy(proteinKey) || !onlyValidated) {
                                if ((onlyValidated && proteinPSParameter.isValidated()) || !onlyValidated) {
                                    if ((!includeHidden && !proteinPSParameter.isHidden()) || includeHidden) {
                                        if ((onlyStarred && proteinPSParameter.isStarred()) || !onlyStarred) {
                                            if (indexes) {
                                                writer.write(++proteinCounter + SEPARATOR);
                                            }
                                            
                                            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                                            if (mainAccession) {
                                                writer.write(proteinMatch.getMainMatch() + SEPARATOR);
                                            }
                                            if (otherAccessions) {
                                                boolean first = true;
                                                for (String otherProtein : proteinMatch.getTheoreticProteinsAccessions()) {
                                                    if (!otherProtein.equals(proteinMatch.getMainMatch())) {
                                                        if (first) {
                                                            first = false;
                                                        } else {
                                                            writer.write(", ");
                                                        }
                                                        writer.write(otherProtein);
                                                    }
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (piDetails) {
                                                writer.write(proteinPSParameter.getGroupName() + SEPARATOR);
                                            }
                                            if (description) {
                                                try {
                                                    writer.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription() + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (sequenceCoverage) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey) * 100 + SEPARATOR);
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (ptmSummary) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getPrimaryPTMSummary(proteinKey) + SEPARATOR);
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSecondaryPTMSummary(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            
                                            if (nPeptides) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (nSpectra) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (emPAI) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey,
                                                            SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (nsaf) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey,
                                                            SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + SEPARATOR);
                                                } catch (Exception e) {
                                                    if (nPeptides) {
                                                        writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                    }
                                                }
                                            }
                                            
                                            if (score) {
                                                writer.write(proteinPSParameter.getProteinScore() + SEPARATOR);
                                            }
                                            if (confidence) {
                                                writer.write(proteinPSParameter.getProteinConfidence() + SEPARATOR);
                                            }
                                            if (!onlyValidated) {
                                                if (proteinPSParameter.isValidated()) {
                                                    writer.write(1 + SEPARATOR);
                                                } else {
                                                    writer.write(0 + SEPARATOR);
                                                }
                                            }
                                            if (includeHidden) {
                                                writer.write(proteinPSParameter.isHidden() + SEPARATOR);
                                            }
                                            if (!onlyStarred && showStar) {
                                                writer.write(proteinPSParameter.isStarred() + "");
                                            }
                                            writer.write("\n");
                                        }
                                        
                                        progressDialog.setValue(++progress);
                                    }
                                }
                            }
                        }
                        
                        writer.close();
                        
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        
                        if (!progressDialog.isRunCanceled()) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }                        
                    } catch (Exception e) {
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired peptide output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     * 
     * @param aPeptideKeys
     * @param aPeptidePdbArray
     * @param aIndexes
     * @param aOnlyValidated
     * @param aAccession
     * @param aProteinDescription 
     * @param aLocation
     * @param aSurroundings
     * @param aSequence
     * @param aModifications
     * @param aPtmLocations
     * @param aCharges
     * @param aNSpectra
     * @param aScore
     * @param aConfidence
     * @param aIncludeHeader
     * @param aOnlyStarred
     * @param aIncludeHidden
     * @param aUniqueOnly
     * @param aProteinKey
     * @throws IOException
     */
    public void getPeptidesOutput(ArrayList<String> aPeptideKeys,
            ArrayList<String> aPeptidePdbArray, boolean aIndexes, boolean aOnlyValidated, boolean aAccession, boolean aProteinDescription,
            boolean aLocation, boolean aSurroundings, boolean aSequence, boolean aModifications, boolean aPtmLocations, boolean aCharges,
            boolean aNSpectra, boolean aScore, boolean aConfidence, boolean aIncludeHeader, boolean aOnlyStarred,
            boolean aIncludeHidden, boolean aUniqueOnly, String aProteinKey) throws IOException {

        // create final versions of all variables use inside the export thread
        final ArrayList<String> peptideKeys;
        final ArrayList<String> peptidePdbArray = aPeptidePdbArray;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean accession = aAccession;
        final boolean proteinDescription = aProteinDescription;
        final boolean location = aLocation;
        final boolean surroundings = aSurroundings;
        final boolean sequence = aSequence;
        final boolean modifications = aModifications;
        final boolean ptmLocations = aPtmLocations;
        final boolean charges = aCharges;
        final boolean nSpectra = aNSpectra;
        final boolean score = aScore;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean includeHidden = aIncludeHidden;
        final boolean uniqueOnly = aUniqueOnly;
        final String proteinKey = aProteinKey;

        // get the file to send the output to
        File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);
        
        if (selectedFile != null) {
            
            final String filePath = selectedFile.getPath();

            // change the peptide shaker icon to a "waiting version"
            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
            
            writer = new BufferedWriter(new FileWriter(selectedFile));
            
            if (aPeptideKeys == null) {
                peptideKeys = identification.getPeptideIdentification();
            } else {
                peptideKeys = aPeptideKeys;
            }
            
            progressDialog = new ProgressDialogX(peptideShakerGUI, true);
            progressDialog.setIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");
            
            new Thread(new Runnable() {
                
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();
            
            new Thread("ExportThread") {
                
                @Override
                public void run() {
                    
                    try {
                        progressDialog.setIndeterminate(false);
                        progressDialog.setMaxProgressValue(peptideKeys.size());
                        
                        if (includeHeader) {
                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (accession) {
                                writer.write("Protein" + SEPARATOR);
                                writer.write("Other Protein(s)" + SEPARATOR);
                                writer.write("Peptide Protein(s)" + SEPARATOR);
                            }
                            if (proteinDescription) {
                                writer.write("Protein Description" + SEPARATOR);
                                writer.write("Other Protein Description(s)" + SEPARATOR);
                                writer.write("Peptide Proteins Description(s)" + SEPARATOR);
                            }
                            if (surroundings) {
                                writer.write("AA Before" + SEPARATOR);
                            }
                            if (sequence) {
                                writer.write("Sequence" + SEPARATOR);
                            }
                            if (surroundings) {
                                writer.write("AA After" + SEPARATOR);
                            }
                            if (location) {
                                writer.write("Peptide Start" + SEPARATOR);
                                writer.write("Peptide End" + SEPARATOR);
                            }
                            if (modifications) {
                                writer.write("Variable Modification" + SEPARATOR);
                            }
                            if (ptmLocations) {
                                writer.write("Location Confidence" + SEPARATOR);
                            }
                            if (charges) {
                                writer.write("Precursor Charge(s)" + SEPARATOR);
                            }
                            if (nSpectra) {
                                writer.write("#Validated Spectra" + SEPARATOR);
                            }
                            if (peptidePdbArray != null) {
                                writer.write("PDB" + SEPARATOR);
                            }
                            if (score) {
                                writer.write("Score" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            if (!onlyValidated) {
                                writer.write("Validated" + SEPARATOR);
                                writer.write("Decoy" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }
                            
                            writer.write("\n");
                        }
                        
                        PSParameter peptidePSParameter = new PSParameter();
                        PSParameter secondaryPSParameter = new PSParameter();
                        int progress = 0, peptideCounter = 0;
                        Protein currentProtein = null;
                        HashMap<Integer, String[]> surroundingAAs = null;
                        ProteinMatch proteinMatch = null;
                        
                        for (String peptideKey : peptideKeys) {
                            
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }
                            
                            boolean shared = false;
                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                            peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptidePSParameter);
                            
                            if (!peptideMatch.isDecoy() || !onlyValidated) {
                                if ((onlyValidated && peptidePSParameter.isValidated()) || !onlyValidated) {
                                    if ((!includeHidden && !peptidePSParameter.isHidden()) || includeHidden) {
                                        if ((onlyStarred && peptidePSParameter.isStarred()) || !onlyStarred) {
                                            
                                            Peptide peptide = peptideMatch.getTheoreticPeptide();
                                            
                                            if (accession || proteinDescription || surroundings || location || uniqueOnly) {
                                                if (proteinKey == null) {
                                                    ArrayList<String> possibleProteins = new ArrayList<String>();
                                                    for (String parentProtein : peptide.getParentProteins()) {
                                                        ArrayList<String> parentProteins = identification.getProteinMap().get(parentProtein);
                                                        if (parentProteins != null) {
                                                            for (String proteinKey : parentProteins) {
                                                                if (!possibleProteins.contains(proteinKey)) {
                                                                    try {
                                                                        proteinMatch = identification.getProteinMatch(proteinKey);
                                                                        if (proteinMatch.getPeptideMatches().contains(peptideKey)) {
                                                                            possibleProteins.add(proteinKey);
                                                                        }
                                                                    } catch (Exception e) {
                                                                        // protein deleted due to protein inference issue and not deleted from the map in versions earlier than 0.14.6
                                                                        System.out.println("Non-existing protein key in protein map:" + proteinKey);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    shared = possibleProteins.size() > 1;
                                                    proteinMatch = identification.getProteinMatch(possibleProteins.get(0));
                                                    currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                                                } else {
                                                    proteinMatch = identification.getProteinMatch(proteinKey);
                                                    currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                                                }
                                            }
                                            
                                            if (!shared || !uniqueOnly) {
                                                
                                                if (indexes) {
                                                    writer.write(++peptideCounter + SEPARATOR);
                                                }
                                                
                                                if (accession || proteinDescription) {
                                                    String mainMatch, secondaryProteins = "", peptideProteins = "";
                                                    String mainMatchDescription, secondaryProteinsDescriptions = "", peptideProteinDescriptions = "";
                                                    boolean first;
                                                    ArrayList<String> accessions = new ArrayList<String>();
                                                    if (!shared) {
                                                        mainMatch = proteinMatch.getMainMatch();
                                                        mainMatchDescription = sequenceFactory.getHeader(mainMatch).getDescription();
                                                        first = true;
                                                        accessions.addAll(proteinMatch.getTheoreticProteinsAccessions());
                                                        Collections.sort(accessions);
                                                        for (String key : accessions) {
                                                            if (!key.equals(mainMatch)) {
                                                                if (first) {
                                                                    first = false;
                                                                } else {
                                                                    secondaryProteins += ", ";
                                                                    secondaryProteinsDescriptions += "; ";
                                                                }
                                                                secondaryProteins += key;
                                                                secondaryProteinsDescriptions += sequenceFactory.getHeader(key).getDescription();
                                                            }
                                                        }
                                                    } else {
                                                        mainMatch = "shared peptide";
                                                        mainMatchDescription = "shared peptide";
                                                    }
                                                    first = true;
                                                    ArrayList<String> peptideAccessions = new ArrayList<String>(peptide.getParentProteins());
                                                    Collections.sort(peptideAccessions);
                                                    for (String key : peptideAccessions) {
                                                        if (!accessions.contains(key)) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                peptideProteins += ", ";
                                                                peptideProteinDescriptions += "; ";
                                                            }
                                                            peptideProteins += key;
                                                            peptideProteinDescriptions += sequenceFactory.getHeader(key).getDescription();
                                                        }
                                                    }
                                                    
                                                    if (accession) {
                                                        writer.write(mainMatch + SEPARATOR);
                                                        writer.write(secondaryProteins + SEPARATOR);
                                                        writer.write(peptideProteins + SEPARATOR);
                                                    }
                                                    if (proteinDescription) {
                                                        writer.write(mainMatchDescription + SEPARATOR);
                                                        writer.write(secondaryProteinsDescriptions + SEPARATOR);
                                                        writer.write(peptideProteinDescriptions + SEPARATOR);
                                                    }
                                                }
                                                
                                                if (location || surroundings) {
                                                    if (!shared) {
                                                        surroundingAAs = currentProtein.getSurroundingAA(peptide.getSequence(), peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides());
                                                    }
                                                }
                                                
                                                if (surroundings) {
                                                    if (!shared) {
                                                        String subSequence = "";
                                                        ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                                                        Collections.sort(starts);
                                                        boolean first = true;
                                                        for (int start : starts) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                subSequence += "|";
                                                            }
                                                            subSequence += surroundingAAs.get(start)[0];
                                                        }
                                                        writer.write(subSequence + SEPARATOR);
                                                    } else if (!accession) {
                                                        writer.write("shared peptide" + SEPARATOR);
                                                    } else {
                                                        writer.write(SEPARATOR);
                                                    }
                                                }
                                                
                                                if (sequence) {
                                                    writer.write(peptide.getSequence() + SEPARATOR);
                                                }
                                                
                                                if (surroundings) {
                                                    if (!shared) {
                                                        String subSequence = "";
                                                        ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                                                        Collections.sort(starts);
                                                        boolean first = true;
                                                        for (int start : starts) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                subSequence += "|";
                                                            }
                                                            subSequence += surroundingAAs.get(start)[1];
                                                        }
                                                        writer.write(subSequence + SEPARATOR);
                                                    } else {
                                                        writer.write(SEPARATOR);
                                                    }
                                                }
                                                
                                                if (location) {
                                                    String start = "";
                                                    String end = "";
                                                    if (!shared) {
                                                        int endAA;
                                                        String sequence = peptide.getSequence();
                                                        ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                                                        Collections.sort(starts);
                                                        boolean first = true;
                                                        for (int startAa : starts) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                start += ", ";
                                                                end += ", ";
                                                            }
                                                            start += startAa;
                                                            endAA = startAa + sequence.length();
                                                            end += endAA;
                                                        }
                                                        
                                                    } else if (!accession && !surroundings) {
                                                        start += "shared peptide";
                                                        end += "shared peptide";
                                                    }
                                                    
                                                    writer.write(start + SEPARATOR + end + SEPARATOR);
                                                }
                                                
                                                if (modifications) {
                                                    writer.write(getPeptideModificationsAsString(peptide));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (ptmLocations) {
                                                    writer.write(getPeptideModificationLocations(peptide, peptideMatch));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (charges) {
                                                    writer.write(getPeptidePrecursorChargesAsString(peptideMatch));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (nSpectra) {
                                                    int cpt = 0;
                                                    for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                                                        secondaryPSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, secondaryPSParameter);
                                                        if (secondaryPSParameter.isValidated()) {
                                                            cpt++;
                                                        }
                                                    }
                                                    writer.write(cpt + SEPARATOR);
                                                }
                                                if (peptidePdbArray != null) {
                                                    writer.write(peptidePdbArray.contains(peptideKey) + SEPARATOR);
                                                }
                                                if (score) {
                                                    writer.write(peptidePSParameter.getPeptideScore() + SEPARATOR);
                                                }
                                                if (confidence) {
                                                    writer.write(peptidePSParameter.getPeptideConfidence() + SEPARATOR);
                                                }
                                                if (!onlyValidated) {
                                                    if (peptidePSParameter.isValidated()) {
                                                        writer.write(1 + SEPARATOR);
                                                    } else {
                                                        writer.write(0 + SEPARATOR);
                                                    }
                                                    if (peptideMatch.isDecoy()) {
                                                        writer.write(1 + SEPARATOR);
                                                    } else {
                                                        writer.write(0 + SEPARATOR);
                                                    }
                                                }
                                                if (includeHidden) {
                                                    writer.write(peptidePSParameter.isHidden() + SEPARATOR);
                                                }
                                                writer.write("\n");
                                            }
                                        }
                                        progressDialog.setValue(++progress);
                                    }
                                }
                            }
                        }
                        
                        writer.close();
                        
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        
                        if (!progressDialog.isRunCanceled()) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired psm output (based on the elements needed as provided in
     * arguments) to a user chosen file.
     *
     * @param aPsmKeys
     * @param aIndexes
     * @param aOnlyValidated
     * @param aAccessions
     * @param aProteinDescription 
     * @param aSequence
     * @param aModification
     * @param aLocation
     * @param aFile
     * @param aTitle
     * @param aPrecursor
     * @param aScore
     * @param aConfidence
     * @param aIncludeHeader
     * @param aOnlyStarred
     * @param aIncludeHidden
     * @throws IOException
     */
    public void getPSMsOutput(ArrayList<String> aPsmKeys, boolean aIndexes, boolean aOnlyValidated, boolean aAccessions, boolean aProteinDescription, boolean aSequence, boolean aModification,
            boolean aLocation, boolean aFile, boolean aTitle, boolean aPrecursor, boolean aScore, boolean aConfidence, boolean aIncludeHeader,
            boolean aOnlyStarred, boolean aIncludeHidden) throws IOException {
        
        // create final versions of all variables to use inside the export thread
        final ArrayList<String> psmKeys;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean accessions = aAccessions;
        final boolean proteinDescription = aProteinDescription;
        final boolean sequence = aSequence;
        final boolean modification = aModification;
        final boolean location = aLocation;
        final boolean file = aFile;
        final boolean title = aTitle;
        final boolean precursor = aPrecursor;
        final boolean score = aScore;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean includeHidden = aIncludeHidden;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);
        
        if (selectedFile != null) {
            
            final String filePath = selectedFile.getPath();

            // change the peptide shaker icon to a "waiting version"
            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
            
            writer = new BufferedWriter(new FileWriter(selectedFile));
            
            if (aPsmKeys == null) {
                psmKeys = identification.getSpectrumIdentification();
            } else {
                psmKeys = aPsmKeys;
            }
            
            progressDialog = new ProgressDialogX(peptideShakerGUI, true);
            progressDialog.setIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");
            
            new Thread(new Runnable() {
                
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();
            
            new Thread("ExportThread") {
                
                @Override
                public void run() {
                    
                    try {
                        progressDialog.setIndeterminate(false);
                        progressDialog.setMaxProgressValue(psmKeys.size());
                        
                        if (includeHeader) {
                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (accessions) {
                                writer.write("Protein(s)" + SEPARATOR);
                            }
                            if (proteinDescription) {
                                writer.write("Protein(s) Descriptions" + SEPARATOR);
                            }
                            if (sequence) {
                                writer.write("Sequence" + SEPARATOR);
                            }
                            if (modification) {
                                writer.write("Variable Modification(s)" + SEPARATOR);
                            }
                            if (location) {
                                writer.write("Location Confidence" + SEPARATOR);
                            }
                            if (file) {
                                writer.write("Spectrum File" + SEPARATOR);
                            }
                            if (title) {
                                writer.write("Spectrum Title" + SEPARATOR);
                            }
                            if (precursor) {
                                writer.write("Precursor m/z" + SEPARATOR);
                                writer.write("Precursor Charge" + SEPARATOR);
                                writer.write("Precursor Retention Time" + SEPARATOR);
                                writer.write("Peptide Theoretical Mass" + SEPARATOR);
                                
                                if (peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()) {
                                    writer.write("Mass Error [ppm]" + SEPARATOR);
                                } else {
                                    writer.write("Mass Error [Da]" + SEPARATOR);
                                }
                            }
                            if (score) {
                                writer.write("Score" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            if (!onlyValidated) {
                                writer.write("Validated" + SEPARATOR);
                                writer.write("Decoy" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }
                            
                            writer.write("\n");
                        }
                        
                        PSParameter psParameter = new PSParameter();
                        int progress = 0, psmCounter = 0;
                        
                        for (String psmKey : psmKeys) {
                            
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }
                            
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                            psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, psParameter);
                            PeptideAssumption bestAssumption = spectrumMatch.getBestAssumption();
                            
                            if (!bestAssumption.isDecoy() || !onlyValidated) {
                                if ((onlyValidated && psParameter.isValidated()) || !onlyValidated) {
                                    if ((!includeHidden && !psParameter.isHidden()) || includeHidden) {
                                        if ((onlyStarred && psParameter.isStarred()) || !onlyStarred) {
                                            
                                            if (indexes) {
                                                writer.write(++psmCounter + SEPARATOR);
                                            }
                                            
                                            if (accessions || proteinDescription) {
                                                
                                                String proteinAccessions = "";
                                                String proteinDescriptions = "";
                                                
                                                boolean first = true;
                                                for (String protein : bestAssumption.getPeptide().getParentProteins()) {
                                                    if (first) {
                                                        first = false;
                                                    } else {
                                                        if (accessions) {
                                                            proteinAccessions += ", ";
                                                        }
                                                        if (proteinDescription) {
                                                            proteinDescriptions += "; ";
                                                        }
                                                    }
                                                    if (accessions) {
                                                        proteinAccessions += protein;
                                                    }
                                                    if (proteinDescription) {
                                                        proteinDescriptions += sequenceFactory.getHeader(protein).getDescription();
                                                    }
                                                }
                                                if (accessions) {
                                                    writer.write(proteinAccessions + SEPARATOR);
                                                }
                                                if (proteinDescription) {
                                                    writer.write(proteinDescriptions + SEPARATOR);
                                                }
                                            }
                                            if (sequence) {
                                                writer.write(bestAssumption.getPeptide().getSequence() + SEPARATOR);
                                            }
                                            if (modification) {
                                                HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
                                                for (ModificationMatch modificationMatch : bestAssumption.getPeptide().getModificationMatches()) {
                                                    if (modificationMatch.isVariable()) {
                                                        if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                                                            modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                                                        }
                                                        modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                                                    }
                                                }
                                                boolean first = true, first2;
                                                ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
                                                Collections.sort(mods);
                                                for (String mod : mods) {
                                                    if (first) {
                                                        first = false;
                                                    } else {
                                                        writer.write(", ");
                                                    }
                                                    first2 = true;
                                                    writer.write(mod + "(");
                                                    for (int aa : modMap.get(mod)) {
                                                        if (first2) {
                                                            first2 = false;
                                                        } else {
                                                            writer.write(", ");
                                                        }
                                                        writer.write(aa + "");
                                                    }
                                                    writer.write(")");
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (location) {
                                                ArrayList<String> modList = new ArrayList<String>();
                                                for (ModificationMatch modificationMatch : bestAssumption.getPeptide().getModificationMatches()) {
                                                    if (modificationMatch.isVariable()) {
                                                        if (!modList.contains(modificationMatch.getTheoreticPtm())) {
                                                            modList.add(modificationMatch.getTheoreticPtm());
                                                        }
                                                    }
                                                }
                                                Collections.sort(modList);
                                                PSPtmScores ptmScores = new PSPtmScores();
                                                boolean first = true;
                                                for (String mod : modList) {
                                                    if (first) {
                                                        first = false;
                                                    } else {
                                                        writer.write(", ");
                                                    }
                                                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                                    writer.write(mod + " (");
                                                    if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                                        int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();
                                                        if (ptmConfidence == PtmScoring.NOT_FOUND) {
                                                            writer.write("Not Scored"); // Well this should not happen
                                                        } else if (ptmConfidence == PtmScoring.RANDOM) {
                                                            writer.write("Random");
                                                        } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                                                            writer.write("Doubtfull");
                                                        } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                                                            writer.write("Confident");
                                                        } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                                                            writer.write("Very Confident");
                                                        }
                                                    } else {
                                                        writer.write("Not Scored");
                                                    }
                                                    writer.write(")");
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (file) {
                                                writer.write(Spectrum.getSpectrumFile(spectrumMatch.getKey()) + SEPARATOR);
                                            }
                                            if (title) {
                                                writer.write(Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR);
                                            }
                                            if (precursor) {
                                                Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                                                writer.write(prec.getMz() + SEPARATOR);
                                                writer.write(bestAssumption.getIdentificationCharge().value + SEPARATOR);
                                                writer.write(prec.getRt() + SEPARATOR);
                                                writer.write(bestAssumption.getPeptide().getMass() + SEPARATOR);
                                                writer.write(bestAssumption.getDeltaMass(prec.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()) + SEPARATOR);
                                            }
                                            if (score) {
                                                writer.write(psParameter.getPsmScore() + SEPARATOR);
                                            }
                                            if (confidence) {
                                                writer.write(psParameter.getPsmConfidence() + SEPARATOR);
                                            }
                                            if (!onlyValidated) {
                                                if (psParameter.isValidated()) {
                                                    writer.write(1 + SEPARATOR);
                                                } else {
                                                    writer.write(0 + SEPARATOR);
                                                }
                                                if (bestAssumption.isDecoy()) {
                                                    writer.write(1 + SEPARATOR);
                                                } else {
                                                    writer.write(0 + SEPARATOR);
                                                }
                                            }
                                            if (includeHidden) {
                                                writer.write(psParameter.isHidden() + SEPARATOR);
                                            }
                                            writer.write("\n");
                                        }
                                        
                                        progressDialog.setValue(++progress);
                                    }
                                }
                            }
                        }
                        
                        writer.close();
                        
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        
                        if (!progressDialog.isRunCanceled()) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Returns the PSM results as a tab separated text file in the Phenyx format
     * as supported by Progenesis.
     *
     * @param progressDialog the progress dialog (can be null)
     * @param psmKeys
     * @param writer the buffered writer to send the output to
     * @throws IOException
     */
    public void getPSMsProgenesisExport(ProgressDialogX progressDialog, ArrayList<String> psmKeys, BufferedWriter writer) throws IOException {
         
        if (psmKeys == null) {
            psmKeys = identification.getSpectrumIdentification();
        }
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMaxProgressValue(psmKeys.size());
        }
        
        writer.write("sequence" + SEPARATOR);
        writer.write("modif" + SEPARATOR);
        writer.write("score" + SEPARATOR);
        writer.write("main AC" + SEPARATOR);
        writer.write("description" + SEPARATOR);
        writer.write("compound" + SEPARATOR);
        writer.write("jobid" + SEPARATOR);
        writer.write("pmkey" + SEPARATOR);
        writer.write("\n");
        
        PSParameter psParameter = new PSParameter();
        int progress = 0;
        
        for (String psmKey : psmKeys) {
            try {
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, psParameter);
            PeptideAssumption bestAssumption = spectrumMatch.getBestAssumption();
            
            if (!bestAssumption.isDecoy() && psParameter.isValidated()) { // note that the validation is for the psm and not for the peptide

                for (int j = 0; j < bestAssumption.getPeptide().getParentProteins().size(); j++) {
                    
                    if (progressDialog.isRunCanceled()) {
                        break;
                    }

                    // peptide sequence
                    writer.write(bestAssumption.getPeptide().getSequence() + SEPARATOR);

                    // modifications
                    HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
                    for (ModificationMatch modificationMatch : bestAssumption.getPeptide().getModificationMatches()) {
                        
                        if (progressDialog.isRunCanceled()) {
                            break;
                        }
                        
                        if (modificationMatch.isVariable()) {
                            if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                                modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                            }
                            modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                        }
                    }
                    
                    ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
                    
                    for (int i = 0; i < bestAssumption.getPeptide().getSequence().length() + 1; i++) {
                        
                        if (progressDialog.isRunCanceled()) {
                            break;
                        }
                        
                        String allMods = "";
                        
                        for (int k = 0; k < mods.size(); k++) {
                            
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }
                            
                            String tempMod = mods.get(k);
                            
                            if (modMap.get(tempMod).contains(new Integer(i))) {
                                
                                if (allMods.length() > 0) {
                                    allMods += ", ";
                                }
                                
                                allMods += tempMod;
                            }
                        }
                        
                        writer.write(allMods + ":");
                    }
                    
                    writer.write(SEPARATOR);

                    // score
                    writer.write(psParameter.getPsmConfidence() + SEPARATOR);

                    // main AC
                    writer.write(bestAssumption.getPeptide().getParentProteins().get(j) + SEPARATOR);

                    // description
                    String description = sequenceFactory.getHeader(bestAssumption.getPeptide().getParentProteins().get(j)).getDescription();
                    writer.write(description + SEPARATOR);

                    // compound
                    writer.write(Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR);

                    // jobid
                    writer.write("N/A" + SEPARATOR);

                    // pmkey
                    writer.write("N/A" + SEPARATOR);

                    // new line
                    writer.write("\n");
                }
            }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                    writer.write("Error\n");
            }
            if (progressDialog != null) {
                progressDialog.setValue(++progress);
            }
            
            if (progressDialog.isRunCanceled()) {
                break;
            }
        }
    }

    /**
     * Sends the desired assumption output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     *
     * @param aPsmKeys
     * @param aOnlyValidated
     * @param aAccession
     * @param aProteinDescription 
     * @param aSequence
     * @param aModifications
     * @param aFile
     * @param aTitle
     * @param aPrecursor
     * @param aScores
     * @param aConfidence
     * @param aIncludeHeader
     * @throws IOException
     */
    public void getAssumptionsOutput(ArrayList<String> aPsmKeys, boolean aOnlyValidated,
            boolean aAccession, boolean aProteinDescription, boolean aSequence, boolean aModifications,
            boolean aFile, boolean aTitle, boolean aPrecursor, boolean aScores, boolean aConfidence, boolean aIncludeHeader) throws IOException {

        // create final versions of all variables use inside the export thread
        final ArrayList<String> psmKeys;
        final boolean onlyValidated = aOnlyValidated;
        final boolean accession = aAccession;
        final boolean proteinDescription = aProteinDescription;
        final boolean sequence = aSequence;
        final boolean modifications = aModifications;
        final boolean file = aFile;
        final boolean title = aTitle;
        final boolean precursor = aPrecursor;
        final boolean scores = aScores;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);
        
        if (selectedFile != null) {
            
            final String filePath = selectedFile.getPath();

            // change the peptide shaker icon to a "waiting version"
            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
            
            writer = new BufferedWriter(new FileWriter(selectedFile));
            
            if (aPsmKeys == null) {
                psmKeys = identification.getSpectrumIdentification();
            } else {
                psmKeys = aPsmKeys;
            }
            
            progressDialog = new ProgressDialogX(peptideShakerGUI, true);
            progressDialog.setIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");
            
            new Thread(new Runnable() {
                
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();
            
            new Thread("ExportThread") {
                
                @Override
                public void run() {
                    
                    try {
                        if (includeHeader) {
                            writer.write("Search Engine" + SEPARATOR);
                            writer.write("Rank" + SEPARATOR);
                            if (accession) {
                                writer.write("Protein Accession" + SEPARATOR);
                            }
                            if (proteinDescription) {
                                writer.write("Protein Description" + SEPARATOR);
                            }
                            if (sequence) {
                                writer.write("Sequence" + SEPARATOR);
                            }
                            if (modifications) {
                                writer.write("Variable Modifications" + SEPARATOR);
                            }
                            if (file) {
                                writer.write("Spectrum File" + SEPARATOR);
                            }
                            if (title) {
                                writer.write("Spectrum Title" + SEPARATOR);
                            }
                            if (precursor) {
                                writer.write("Precursor m/z" + SEPARATOR);
                                writer.write("Precursor Charge" + SEPARATOR);
                                writer.write("Precursor RT" + SEPARATOR);
                                writer.write("Peptide Theoretical Mass" + SEPARATOR);
                                
                                if (peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()) {
                                    writer.write("Mass Error [ppm]" + SEPARATOR);
                                } else {
                                    writer.write("Mass Error [Da]" + SEPARATOR);
                                }
                                
                            }
                            if (scores) {
                                writer.write("Mascot e-value" + SEPARATOR);
                                writer.write("OMSSA e-value" + SEPARATOR);
                                writer.write("X!Tandem e-value" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            writer.write("Retained as Main PSM" + SEPARATOR);
                            writer.write("Decoy" + SEPARATOR);
                            writer.write("\n");
                        }
                        
                        PSParameter psParameter = new PSParameter();
                        int rank, progress = 0;
                        
                        progressDialog.setIndeterminate(false);
                        progressDialog.setMaxProgressValue(psmKeys.size());
                        
                        for (String spectrumKey : psmKeys) {
                            
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }
                            
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                            
                            if (!onlyValidated || psParameter.isValidated()) {
                                for (int se : spectrumMatch.getAdvocates()) {
                                    ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(se).keySet());
                                    Collections.sort(eValues);
                                    rank = 1;
                                    for (double eValue : eValues) {
                                        for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                                            writer.write(AdvocateFactory.getInstance().getAdvocate(se).getName() + SEPARATOR);
                                            writer.write(rank + SEPARATOR);
                                            if (accession || proteinDescription) {
                                                
                                                String proteinAccessions = "";
                                                String proteinDescriptions = "";
                                                
                                                boolean first = true;
                                                for (String protein : peptideAssumption.getPeptide().getParentProteins()) {
                                                    if (first) {
                                                        first = false;
                                                    } else {
                                                         if (accession) {
                                                            proteinAccessions += ", ";
                                                        }
                                                        if (proteinDescription) {
                                                            proteinDescriptions += "; ";
                                                        }
                                                    }
                                                    if (accession) {
                                                        proteinAccessions += protein;
                                                    }
                                                    if (proteinDescription) {
                                                        proteinDescriptions += sequenceFactory.getHeader(protein).getDescription();
                                                    }
                                                }
                                                if (accession) {
                                                    writer.write(proteinAccessions + SEPARATOR);
                                                }
                                                if (proteinDescription) {
                                                    writer.write(proteinDescriptions + SEPARATOR);
                                                }
                                            }
                                            if (sequence) {
                                                writer.write(peptideAssumption.getPeptide().getSequence() + SEPARATOR);
                                            }
                                            if (modifications) {
                                                boolean first = true;
                                                for (ModificationMatch modificationMatch : peptideAssumption.getPeptide().getModificationMatches()) {
                                                    if (modificationMatch.isVariable()) {
                                                        if (first) {
                                                            first = false;
                                                        } else {
                                                            writer.write(", ");
                                                        }
                                                        String modName = modificationMatch.getTheoreticPtm();
                                                        writer.write(modName + "(" + modificationMatch.getModificationSite() + ")");
                                                    }
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (file) {
                                                writer.write(Spectrum.getSpectrumFile(spectrumMatch.getKey()) + SEPARATOR);
                                            }
                                            if (title) {
                                                writer.write(Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR);
                                            }
                                            if (precursor) {
                                                Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                                                writer.write(prec.getMz() + SEPARATOR);
                                                writer.write(peptideAssumption.getIdentificationCharge().value + SEPARATOR);
                                                writer.write(prec.getRt() + SEPARATOR);
                                                writer.write(peptideAssumption.getPeptide().getMass() + SEPARATOR);
                                                writer.write(Math.abs(peptideAssumption.getDeltaMass(prec.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm())) + SEPARATOR);
                                            }
                                            if (scores) {
                                                if (se == Advocate.MASCOT) {
                                                    writer.write("" + eValue);
                                                }
                                                writer.write(SEPARATOR);
                                                if (se == Advocate.OMSSA) {
                                                    writer.write("" + eValue);
                                                }
                                                writer.write(SEPARATOR);
                                                if (se == Advocate.XTANDEM) {
                                                    writer.write("" + eValue);
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (confidence) {
                                                psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                                writer.write(psParameter.getSearchEngineConfidence() + SEPARATOR);
                                            }
                                            if (peptideAssumption.getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                                                writer.write(1 + SEPARATOR);
                                            } else {
                                                writer.write(0 + SEPARATOR);
                                            }
                                            if (peptideAssumption.isDecoy()) {
                                                writer.write(1 + SEPARATOR);
                                            } else {
                                                writer.write(0 + SEPARATOR);
                                            }
                                            writer.write("\n");
                                            rank++;
                                        }
                                    }
                                }
                            }
                            
                            progressDialog.setValue(++progress);
                        }
                        
                        writer.close();
                        
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        
                        if (!progressDialog.isRunCanceled()) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Returns the possible precursor charges for a given peptide match. The
     * charges are returned in increading order with each charge only appearing
     * once.
     *
     * @param peptideMatch the peptide match
     * @return the possible precursor charges
     */
    public String getPeptidePrecursorChargesAsString(PeptideMatch peptideMatch) {
        
        String result = "";
        
        ArrayList<String> spectrumKeys = peptideMatch.getSpectrumMatches();
        ArrayList<Integer> charges = new ArrayList<Integer>(5);

        // find all unique the charges
        for (int i = 0; i < spectrumKeys.size(); i++) {
            try {
            int tempCharge = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKeys.get(i)).getBestAssumption().getIdentificationCharge().value;
            
            if (!charges.contains(tempCharge)) {
                charges.add(tempCharge);
            }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                return "Error";
            }
        }

        // sort the charges
        Collections.sort(charges);

        // add the charges to the output
        for (int i = 0; i < charges.size(); i++) {
            if (i > 0) {
                result += ", ";
            }
            
            result += charges.get(i);
        }
        
        return result;
    }

    /**
     * Returns the peptide modifications as a string.
     *
     * @param peptide the peptide
     * @return the peptide modifications as a string
     */
    public static String getPeptideModificationsAsString(Peptide peptide) {
        
        String result = "";
        
        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }
        boolean first = true, first2;
        ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
        Collections.sort(mods);
        for (String mod : mods) {
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            first2 = true;
            result += mod + "(";
            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                    result += ", ";
                }
                result += aa;
            }
            result += ")";
        }
        
        return result;
    }

    /**
     * Returns the peptide modification location confidence as a string.
     *
     * @param peptide the peptide
     * @param peptideMatch the peptide match
     * @return the peptide modification location confidence as a string.
     */
    public static String getPeptideModificationLocations(Peptide peptide, PeptideMatch peptideMatch) {
        
        String result = "";
        
        ArrayList<String> modList = new ArrayList<String>();
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modList.contains(modificationMatch.getTheoreticPtm())) {
                    modList.add(modificationMatch.getTheoreticPtm());
                }
            }
        }
        Collections.sort(modList);
        PSPtmScores ptmScores = new PSPtmScores();
        boolean first = true;
        for (String mod : modList) {
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
            result += mod + " (";
            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();
                if (ptmConfidence == PtmScoring.NOT_FOUND) {
                    result += "Not Scored"; // Well this should not happen
                } else if (ptmConfidence == PtmScoring.RANDOM) {
                    result += "Random";
                } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                    result += "Doubtfull";
                } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                    result += "Confident";
                } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                    result += "Very Confident";
                }
            } else {
                result += "Not Scored";
            }
            result += ")";
        }
        
        return result;
    }
    }
