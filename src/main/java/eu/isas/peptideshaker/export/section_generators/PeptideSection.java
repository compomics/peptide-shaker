/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.export.section_generators;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures;
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

/**
 * This class outputs the peptide related export features
 *
 * @author Marc
 */
public class PeptideSection {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The features to export
     */
    private ArrayList<ExportFeature> exportFeatures;
    /**
     * The separator used to separate columns
     */
    private String separator;
    /**
     * Boolean indicating whether the line shall be indexed
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;

    /**
     * constructor
     *
     * @param exportFeatures the features to export in this section
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public PeptideSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param keys the keys of the protein matches to output
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, SearchParameters searchParameters, ArrayList<String> keys, String proteinMatchKey, int nSurroundingAA) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {
        if (header) {
            if (indexes) {
                writer.write(separator);
            }
            boolean firstColumn = true;
            for (ExportFeature exportFeature : exportFeatures) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(exportFeature.getTitle());
            }
            writer.newLine();
        }
        PSParameter psParameter = new PSParameter();
        PeptideMatch peptideMatch = null;
        String matchKey = "", parameterKey = "";
        int line = 1;
        for (String peptideKey : keys) {
            if (indexes) {
                writer.write(line + separator);
            }
            for (ExportFeature exportFeature : exportFeatures) {
                PeptideFeatures peptideFeatures = (PeptideFeatures) exportFeature;
                switch (peptideFeatures) {
                    case accessions:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        String proteins = "";
                        ArrayList<String> accessions = peptideMatch.getTheoreticPeptide().getParentProteins();
                        Collections.sort(accessions);
                        for (String accession : accessions) {
                            if (!proteins.equals("")) {
                                proteins += ", ";
                            }
                            proteins += accession;
                        }
                        writer.write(proteins + separator);
                    case confidence:
                        if (!parameterKey.equals(peptideKey)) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            parameterKey = peptideKey;
                        }
                        writer.write(psParameter.getPeptideConfidence() + separator);
                    case decoy:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        if (peptideMatch.isDecoy()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                    case hidden:
                        if (!parameterKey.equals(peptideKey)) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            parameterKey = peptideKey;
                        }
                        if (psParameter.isHidden()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                    case localization_confidence:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        HashMap<String, ArrayList<Integer>> modMap = getModMap(peptideMatch.getTheoreticPeptide());
                        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
                        Collections.sort(modifications);
                        String summary = "";
                        PSPtmScores ptmScores = new PSPtmScores();
                        for (String mod : modifications) {
                            if (!summary.equals("")) {
                                summary += ", ";
                            }
                            ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
                            summary += " (";

                            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {

                                int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();

                                if (ptmConfidence == PtmScoring.NOT_FOUND) {
                                    summary += "Not Scored"; // Well this should not happen
                                } else if (ptmConfidence == PtmScoring.RANDOM) {
                                    summary += "Random";
                                } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                                    summary += "Doubtfull";
                                } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                                    summary += "Confident";
                                } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                                    summary += "Very Confident";
                                }
                            } else {
                                summary += "Not Scored";
                            }

                            summary += ")";
                        }
                        break;
                    case pi:
                        if (!parameterKey.equals(peptideKey)) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            parameterKey = peptideKey;
                        }
                        writer.write(psParameter.getProteinInferenceClassAsString());
                    case position:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        accessions = peptideMatch.getTheoreticPeptide().getParentProteins();
                        Collections.sort(accessions);
                        Peptide peptide = peptideMatch.getTheoreticPeptide();
                        String start = "";
                        for (String proteinAccession : accessions) {
                            HashMap<Integer, String[]> surroundingAAs =
                                    sequenceFactory.getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                    nSurroundingAA);
                            ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
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
                        writer.write(start + separator);
                    case psms:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        writer.write(peptideMatch.getSpectrumCount() + separator);
                    case ptms:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        modMap = getModMap(peptideMatch.getTheoreticPeptide());
                        boolean firstLocation;
                        ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
                        Collections.sort(mods);
                        String ptms = "";
                        for (String mod : mods) {
                            if (!ptms.equals("")) {
                                firstLocation = true;
                                ptms += mod + " (";
                                for (int aa : modMap.get(mod)) {
                                    if (firstLocation) {
                                        firstLocation = false;
                                    } else {
                                        ptms += ", ";
                                    }
                                    ptms += aa;
                                }
                                ptms += ")";
                            }
                        }
                        writer.write(ptms + separator);
                    case score:
                        if (!parameterKey.equals(peptideKey)) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            parameterKey = peptideKey;
                        }
                        writer.write(psParameter.getPsmScore() + separator);
                    case sequence:
                        writer.write(Peptide.getSequence(peptideKey) + separator);
                    case starred:
                        if (!parameterKey.equals(peptideKey)) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            parameterKey = peptideKey;
                        }
                        if (psParameter.isStarred()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                    case aaBefore:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        accessions = peptideMatch.getTheoreticPeptide().getParentProteins();
                        Collections.sort(accessions);
                        peptide = peptideMatch.getTheoreticPeptide();
                        String subSequence = "";
                        for (String proteinAccession : accessions) {
                            if (!subSequence.equals("")) {
                                subSequence += ";";
                            }
                            HashMap<Integer, String[]> surroundingAAs =
                                    sequenceFactory.getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                    nSurroundingAA);
                            ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                            Collections.sort(starts);
                            boolean first = true;
                            for (int startAa : starts) {
                                if (first) {
                                    first = false;
                                } else {
                                    subSequence += "|";
                                }
                                subSequence += surroundingAAs.get(startAa)[0];
                            }
                        }
                        writer.write(subSequence + separator);
                    case aaAfter:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        accessions = peptideMatch.getTheoreticPeptide().getParentProteins();
                        Collections.sort(accessions);
                        peptide = peptideMatch.getTheoreticPeptide();
                        subSequence = "";
                        for (String proteinAccession : accessions) {
                            if (!subSequence.equals("")) {
                                subSequence += ";";
                            }
                            HashMap<Integer, String[]> surroundingAAs =
                                    sequenceFactory.getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                    nSurroundingAA);
                            ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                            Collections.sort(starts);
                            boolean first = true;
                            for (int startAa : starts) {
                                if (first) {
                                    first = false;
                                } else {
                                    subSequence += "|";
                                }
                                subSequence += surroundingAAs.get(startAa)[1];
                            }
                        }
                        writer.write(subSequence + separator);
                    case unique:
                        if (!matchKey.equals(peptideKey)) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            matchKey = peptideKey;
                        }
                        if (identificationFeaturesGenerator.isUnique(proteinMatchKey, peptideMatch.getTheoreticPeptide())) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                    case validated:
                        if (!parameterKey.equals(peptideKey)) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            parameterKey = peptideKey;
                        }
                        if (psParameter.isValidated()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                    case validated_psms:
                        writer.write(identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey) + separator);

                    default:
                        writer.write("Not implemented");
                }
            }
            writer.newLine();
            line++;
        }
    }

    /**
     * Returns a map of the modifications in a peptide. Modification name ->
     * sites
     *
     * @param peptide
     * @return the map of the modifications on a peptide sequence
     */
    private HashMap<String, ArrayList<Integer>> getModMap(Peptide peptide) {
        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();

        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }

        return modMap;
    }
}
