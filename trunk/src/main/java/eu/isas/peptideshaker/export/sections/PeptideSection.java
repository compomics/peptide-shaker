package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.ModificationProfile;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeatures;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the peptide related export features.
 *
 * @author Marc Vaudel
 */
public class PeptideSection {

    /**
     * The peptide features to export.
     */
    private ArrayList<ExportFeature> peptideFeatures = new ArrayList<ExportFeature>();
    /**
     * The psm subsection if needed.
     */
    private PsmSection psmSection = null;
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
    public PeptideSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> psmFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PeptideFeatures) {
                peptideFeatures.add(exportFeature);
            } else if (exportFeature instanceof PsmFeatures || exportFeature instanceof FragmentFeatures) {
                psmFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!psmFeatures.isEmpty()) {
            psmSection = new PsmSection(psmFeatures, separator, indexes, header, writer);
        }
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, int nSurroundingAA, String linePrefix, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        if (keys == null) {
            keys = identification.getPeptideIdentification();
        }

        PSParameter psParameter = new PSParameter();
        PeptideMatch peptideMatch;
        int line = 1;

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Peptides. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadPeptideMatches(keys, waitingHandler);
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Peptide Details. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadPeptideMatchParameters(keys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        for (String peptideKey : keys) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            peptideMatch = identification.getPeptideMatch(peptideKey);
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (indexes) {
                if (linePrefix != null) {
                    writer.write(linePrefix);
                }
                writer.write(line + separator);
            }

            for (ExportFeature exportFeature : peptideFeatures) {
                PeptideFeatures peptideFeature = (PeptideFeatures) exportFeature;
                writer.write(getfeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, nSurroundingAA, linePrefix, separator, peptideMatch, psParameter, peptideFeature, waitingHandler) + separator);
            }
            writer.newLine();
            if (psmSection != null) {
                String psmSectionPrefix = "";
                if (linePrefix != null) {
                    psmSectionPrefix += linePrefix;
                }
                psmSectionPrefix += line + ".";
                psmSection.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, peptideMatch.getSpectrumMatches(), psmSectionPrefix, null);
            }
            line++;
        }
    }

    /**
     * Returns the component of the section corresponding to the given feature.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param separator the column separator
     * @param peptideMatch the peptide match
     * @param psParameter the PeptideShaker parameters of the match
     * @param peptideFeature the peptide feature to export
     * @param waitingHandler the waiting handler
     *
     * @return the component of the section corresponding to the given feature
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static String getfeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, int nSurroundingAA, String linePrefix, String separator, PeptideMatch peptideMatch, PSParameter psParameter, PeptideFeatures peptideFeature, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (peptideFeature) {
            case accessions:
                StringBuilder proteins = new StringBuilder();
                ArrayList<String> accessions = peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                Collections.sort(accessions);
                for (String accession : accessions) {
                    if (proteins.length() > 0) {
                        proteins.append("; ");
                    }
                    proteins.append(accession);
                }
                return proteins.toString();
            case protein_description:
                SequenceFactory sequenceFactory = SequenceFactory.getInstance();
                StringBuilder descriptions = new StringBuilder();
                accessions = peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                Collections.sort(accessions);
                for (String accession : accessions) {
                    if (descriptions.length() > 0) {
                        descriptions.append("; ");
                    }
                    descriptions.append(sequenceFactory.getHeader(accession).getDescription());
                }
                return descriptions.toString();
            case confidence:
                return psParameter.getPeptideConfidence() + "";
            case decoy:
                if (peptideMatch.getTheoreticPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                    return "1";
                } else {
                    return "0";
                }
            case hidden:
                if (psParameter.isHidden()) {
                    return "1";
                } else {
                    return "0";
                }
            case localization_confidence:
                return getPeptideModificationLocations(peptideMatch.getTheoreticPeptide(), peptideMatch, searchParameters.getModificationProfile()) + "";
            case pi:
                return psParameter.getProteinInferenceClassAsString();
            case position:
                accessions = peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                Collections.sort(accessions);
                Peptide peptide = peptideMatch.getTheoreticPeptide();
                String start = "";
                for (String proteinAccession : accessions) {
                    Protein protein = SequenceFactory.getInstance().getProtein(proteinAccession);
                    ArrayList<Integer> starts = protein.getPeptideStart(peptide.getSequence(),
                            PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            start += ", ";
                        }
                        start += startAa;
                    }

                    start += "; ";
                }
                return start;
            case psms:
                return peptideMatch.getSpectrumCount() + "";
            case variable_ptms:
                return getPeptideModificationsAsString(peptideMatch.getTheoreticPeptide(), true);
            case fixed_ptms:
                return getPeptideModificationsAsString(peptideMatch.getTheoreticPeptide(), false);
            case score:
                return psParameter.getPeptideScore() + "";
            case sequence:
                return peptideMatch.getTheoreticPeptide().getSequence();
            case missed_cleavages:
                String sequence = peptideMatch.getTheoreticPeptide().getSequence();
                return Peptide.getNMissedCleavages(sequence, searchParameters.getEnzyme()) + "";
            case modified_sequence:
                return peptideMatch.getTheoreticPeptide().getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true);
            case starred:
                if (psParameter.isStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case aaBefore:
                accessions = peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                Collections.sort(accessions);
                peptide = peptideMatch.getTheoreticPeptide();
                String subSequence = "";
                for (String proteinAccession : accessions) {
                    if (!subSequence.equals("")) {
                        subSequence += "; ";
                    }
                    HashMap<Integer, String[]> surroundingAAs = SequenceFactory.getInstance().getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                            nSurroundingAA, PeptideShaker.MATCHING_TYPE,
                            searchParameters.getFragmentIonAccuracy());
                    ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            subSequence += ", ";
                        }
                        subSequence += surroundingAAs.get(startAa)[0];
                    }
                }
                return subSequence;
            case aaAfter:
                accessions = peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                Collections.sort(accessions);
                peptide = peptideMatch.getTheoreticPeptide();
                subSequence = "";
                for (String proteinAccession : accessions) {
                    if (!subSequence.equals("")) {
                        subSequence += "; ";
                    }
                    HashMap<Integer, String[]> surroundingAAs
                            = SequenceFactory.getInstance().getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                    nSurroundingAA, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                    ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            subSequence += ", ";
                        }
                        subSequence += surroundingAAs.get(startAa)[1];
                    }
                }
                return subSequence;
            case unique:
                peptide = peptideMatch.getTheoreticPeptide();
                if (identification.isUnique(peptide)) {
                    return "1";
                } else {
                    return "0";
                }
            case validated:
                return psParameter.getMatchValidationLevel().toString();
            case validated_psms:
                return identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideMatch.getKey()) + "";
            default:
                return "Not implemented";
        }

    }

    /**
     * Returns the peptide modifications as a string.
     *
     * @param peptide the peptide
     * @param variablePtms if true, only variable PTMs are shown, false return
     * only the fixed PTMs
     * @return the peptide modifications as a string
     */
    public static String getPeptideModificationsAsString(Peptide peptide, boolean variablePtms) {

        StringBuilder result = new StringBuilder();

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if ((variablePtms && modificationMatch.isVariable()) || (!variablePtms && !modificationMatch.isVariable())) {
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
                result.append(", ");
            }
            first2 = true;
            result.append(mod);
            result.append(" (");
            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                    result.append(", ");
                }
                result.append(aa);
            }
            result.append(")");
        }

        return result.toString();
    }

    /**
     * Returns the peptide modification location confidence as a string.
     *
     * @param peptide the peptide
     * @param peptideMatch the peptide match
     * @param ptmProfile the PTM profile
     * @return the peptide modification location confidence as a string.
     */
    public static String getPeptideModificationLocations(Peptide peptide, PeptideMatch peptideMatch, ModificationProfile ptmProfile) {

        PTMFactory ptmFactory = PTMFactory.getInstance();

        ArrayList<String> modList = new ArrayList<String>();

        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                PTM refPtm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                for (String equivalentPtm : ptmProfile.getSimilarNotFixedModifications(refPtm.getMass())) {
                    if (!modList.contains(equivalentPtm)) {
                        modList.add(equivalentPtm);
                    }
                }
            }
        }

        StringBuilder result = new StringBuilder();
        Collections.sort(modList);

        for (String mod : modList) {
            if (result.length() > 0) {
                result.append(", ");
            }
            PSPtmScores ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
            result.append(mod).append(" (");
            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                boolean firstSite = true;
                for (int site : ptmScoring.getOrderedPtmLocations()) {
                    if (firstSite) {
                        firstSite = false;
                    } else {
                        result.append(", ");
                    }
                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                    if (ptmConfidence == PtmScoring.NOT_FOUND) {
                        result.append(site).append(": Not Scored"); // Well this should not happen
                    } else if (ptmConfidence == PtmScoring.RANDOM) {
                        result.append(site).append(": Random");
                    } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                        result.append(site).append(": Doubtfull");
                    } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                        result.append(site).append(": Confident");
                    } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                        result.append(site).append(": Very Confident");
                    }
                }
            } else {
                result.append("Not Scored");
            }
            result.append(")");
        }

        return result.toString();
    }

    /**
     * Writes the title of the section.
     *
     * @throws IOException
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.write(separator);
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : peptideFeatures) {
            for (String title : exportFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title);
            }
        }
        writer.newLine();
    }
}
