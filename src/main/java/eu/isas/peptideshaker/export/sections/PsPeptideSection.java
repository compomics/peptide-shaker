package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.Header;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class outputs the peptide related export features.
 *
 * @author Marc Vaudel
 */
public class PsPeptideSection {

    /**
     * The peptide features to export.
     */
    private final ArrayList<PsPeptideFeature> peptideFeatures = new ArrayList<>();
    /**
     * The PSM subsection if needed.
     */
    private PsPsmSection psmSection = null;
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
    public PsPeptideSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> psmFeatures = new ArrayList<>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPeptideFeature) {
                peptideFeatures.add((PsPeptideFeature) exportFeature);
            } else if (exportFeature instanceof PsPsmFeature || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature || exportFeature instanceof PsFragmentFeature) {
                psmFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(peptideFeatures);
        if (!psmFeatures.isEmpty()) {
            psmSection = new PsPsmSection(psmFeatures, indexes, header, writer);
        }
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
     * @param sequenceProvider a provider for the protein sequences
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading or writing a file
     * @throws java.lang.InterruptedException exception thrown if a thread got
     * interrupted
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, IdentificationParameters identificationParameters, ArrayList<Long> keys, int nSurroundingAA,
            String linePrefix, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException, InterruptedException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        if (keys == null) {
            keys = new ArrayList<>(identification.getPeptideIdentification());
        }

        int lineNumber = 1;

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        for (long key : keys) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            PeptideMatch peptideMatch = identification.getPeptideMatch(key);
            
            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                if (decoys || !PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider)) {

                    boolean first = true;

                    if (indexes) {

                        if (linePrefix != null) {

                            writer.write(linePrefix);

                        }

                        writer.write(Integer.toString(lineNumber));
                        first = false;

                    }

                    for (ExportFeature exportFeature : peptideFeatures) {

                        if (!first) {

                            writer.addSeparator();

                        } else {

                            first = false;

                        }

                        PsPeptideFeature peptideFeature = (PsPeptideFeature) exportFeature;
                        writer.write(getfeature(identification, identificationFeaturesGenerator, sequenceProvider, identificationParameters, keys, nSurroundingAA, linePrefix, peptideMatch, psParameter, peptideFeature, validatedOnly, decoys, waitingHandler));

                    }

                    writer.newLine();

                    if (psmSection != null) {

                        String psmSectionPrefix = "";

                        if (linePrefix != null) {

                            psmSectionPrefix += linePrefix;

                        }

                        psmSectionPrefix += lineNumber + ".";
                        writer.increaseDepth();

                        if (waitingHandler != null) {

                            waitingHandler.setDisplayProgress(false);

                        }

                        psmSection.writeSection(identification, identificationFeaturesGenerator, sequenceProvider, identificationParameters, peptideMatch.getSpectrumMatchesKeys(), psmSectionPrefix, nSurroundingAA, validatedOnly, decoys, waitingHandler);

                        if (waitingHandler != null) {

                            waitingHandler.setDisplayProgress(true);

                        }

                        writer.decreseDepth();

                    }

                    lineNumber++;

                }
            }
        }
    }

    /**
     * Returns the component of the section corresponding to the given feature.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param sequenceProvider a provider for the protein sequences
     * @param proteinDetailsProvider a provider for protein details
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param peptideMatch the peptide match
     * @param psParameter the PeptideShaker parameters of the match
     * @param peptideFeature the peptide feature to export
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @return the component of the section corresponding to the given feature
     */
    public static String getfeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, IdentificationParameters identificationParameters, ArrayList<String> keys, int nSurroundingAA, String linePrefix,
            PeptideMatch peptideMatch, PSParameter psParameter, PsPeptideFeature peptideFeature,
            boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) {

        switch (peptideFeature) {

            case accessions:

                TreeMap<String, int[]> proteinMapping = peptideMatch.getPeptide().getProteinMapping();

                return proteinMapping.navigableKeySet().stream()
                        .collect(Collectors.joining(","));

            case protein_description:

                proteinMapping = peptideMatch.getPeptide().getProteinMapping();

                return proteinMapping.navigableKeySet().stream()
                        .map(accession -> proteinDetailsProvider.getDescription(accession))
                        .collect(Collectors.joining(","));

            case protein_groups:

                HashSet<Long> proteinGroups = identification.getProteinMatches(peptideMatch.getPeptide());
                
                return proteinGroups.stream()
                        .sorted()
                        .map(proteinGroupKey -> getProteinGroupText(proteinGroupKey, identification))
                        .collect(Collectors.joining(";"));

            case best_protein_group_validation:
                
                MatchValidationLevel bestProteinValidationLevel = MatchValidationLevel.none;
                proteinGroups = identification.getProteinMatches(peptideMatch.getPeptide());
                
                for (long proteinGroup : proteinGroups) {
                    
                    if (identification.getProteinIdentification().contains(proteinGroup)) {
                    
                        psParameter = (PSParameter) (identification.getProteinMatch(proteinGroup)).getUrParam(psParameter);
                        
                        if (psParameter.getMatchValidationLevel().getIndex() > bestProteinValidationLevel.getIndex()) {
                        
                            bestProteinValidationLevel = psParameter.getMatchValidationLevel();
                        
                        }
                    }
                }
                
                return bestProteinValidationLevel.getName();
                
            case confidence:
                
                return Double.toString(psParameter.getPeptideConfidence());
                
            case decoy:
                
                return PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider) ? "1" : "0";
                
            case hidden:
                
                return psParameter.getHidden() ? "1" : "0";
                
            case localization_confidence:
                
                return getPeptideModificationLocationConfidence(peptideMatch, identificationParameters.getSearchParameters().getModificationParameters());
                
            case pi:
                
                return psParameter.getProteinInferenceClassAsString();
            
            case position:
                
                proteinMapping = peptideMatch.getPeptide().getProteinMapping();
                
                return proteinMapping.entrySet().stream()
                        .map(entry -> getPeptideLocalizationText(entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(";"));
                
            case psms:
                
                return Integer.toString(peptideMatch.getSpectrumCount());
                
            case variable_ptms:
                
                return PeptideUtils.getPeptideModificationsAsString(peptideMatch.getPeptide(), true);
                
            case fixed_ptms:
                
                return PeptideUtils.getPeptideModificationsAsString(peptideMatch.getPeptide(), false);
                
            case score:
                
                return Double.toString(psParameter.getPeptideScore());
                
            case raw_score:
                
                return Double.toString(psParameter.getPeptideProbabilityScore());
                
            case sequence:
                
                return peptideMatch.getPeptide().getSequence();
                
            case missed_cleavages:
                
                int nMissedCleavages = peptideMatch.getPeptide()
                        .getNMissedCleavages(identificationParameters.getSearchParameters().getDigestionParameters());
                
                return Integer.toString(nMissedCleavages);
            
            case modified_sequence:
                
                return peptideMatch.getPeptide()
                        .getTaggedModifiedSequence(identificationParameters.getSearchParameters().getModificationParameters(), false, false, true);
            
            case starred:
                
                return psParameter.getStarred() ? "1" : "0";

            case aaBefore:

                TreeMap<String, String[]> aaMap = PeptideUtils.getAaBefore(peptideMatch.getPeptide(), nSurroundingAA, sequenceProvider);

                return aaMap.values().stream()
                        .map(aas -> (Arrays.stream(aas))
                        .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";"));

            case aaAfter:

                aaMap = PeptideUtils.getAaAfter(peptideMatch.getPeptide(), nSurroundingAA, sequenceProvider);

                return aaMap.values().stream()
                        .map(aas -> (Arrays.stream(aas))
                        .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";"));
                
            case nValidatedProteinGroups:
                
                return identificationFeaturesGenerator.getNValidatedProteinGroups(peptideMatch.getPeptide(), waitingHandler) + "";
                
            case unique_group:
                
                return identification.getProteinMatches(peptideMatch.getPeptide()).size() == 1 ? "1" : "0";
                
            case validated:
                
                return psParameter.getMatchValidationLevel().toString();
            
            case validated_psms:
            
                return Integer.toString(identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideMatch.getKey()));
            
            case probabilistic_score:
                PSPtmScores ptmScores = new PSPtmScores();
                ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
                if (ptmScores != null) {
                    StringBuilder result = new StringBuilder();
                    ArrayList<String> modList = new ArrayList<>(ptmScores.getScoredPTMs());
                    Collections.sort(modList);
                    for (String mod : modList) {
                        PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                        ArrayList<Integer> sites = new ArrayList<>(ptmScoring.getProbabilisticSites());
                        if (!sites.isEmpty()) {
                            Collections.sort(sites);
                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(mod).append(" (");
                            boolean firstSite = true;
                            for (int site : sites) {
                                if (firstSite) {
                                    firstSite = false;
                                } else {
                                    result.append(", ");
                                }
                                result.append(site).append(": ").append(ptmScoring.getProbabilisticScore(site));
                            }
                            result.append(")");
                        }
                    }
                    return result.toString();
                }
                return "";
            case d_score:
                StringBuilder result = new StringBuilder();
                ptmScores = new PSPtmScores();
                ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
                if (ptmScores != null) {
                    ArrayList<String> modList = new ArrayList<>(ptmScores.getScoredPTMs());
                    Collections.sort(modList);
                    for (String mod : modList) {
                        PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                        ArrayList<Integer> sites = new ArrayList<>(ptmScoring.getDSites());
                        if (!sites.isEmpty()) {
                            Collections.sort(sites);
                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(mod).append(" (");
                            boolean firstSite = true;
                            for (int site : sites) {
                                if (firstSite) {
                                    firstSite = false;
                                } else {
                                    result.append(", ");
                                }
                                result.append(site).append(": ").append(ptmScoring.getDeltaScore(site));
                            }
                            result.append(")");
                        }
                    }
                    return result.toString();
                }
                return "";
            case confident_modification_sites:
                String sequence = peptideMatch.getPeptide().getSequence();
                return identificationFeaturesGenerator.getConfidentPtmSites(peptideMatch, sequence);
            case confident_modification_sites_number:
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(peptideMatch);
            case ambiguous_modification_sites:
                sequence = peptideMatch.getPeptide().getSequence();
                return identificationFeaturesGenerator.getAmbiguousPtmSites(peptideMatch, sequence);
            case ambiguous_modification_sites_number:
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(peptideMatch);
            case confident_phosphosites:
                ArrayList<String> modifications = new ArrayList<>();
                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getConfidentPtmSites(peptideMatch, peptideMatch.getPeptide().getSequence(), modifications);
            case confident_phosphosites_number:
                modifications = new ArrayList<>();
                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(peptideMatch, modifications);
            case ambiguous_phosphosites:
                modifications = new ArrayList<>();
                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSites(peptideMatch, peptideMatch.getPeptide().getSequence(), modifications);
            case ambiguous_phosphosites_number:
                modifications = new ArrayList<>();
                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(peptideMatch, modifications);
            default:
                return "Not implemented";
        }
    }

    /**
     * Returns a description of the given protein group in the form proteinA,proteinB(confidence).
     * 
     * @param proteinGroupKey the key of the protein group
     * @param identification the identification object
     * 
     * @return a description of the given protein group
     */
    public static String getProteinGroupText(long proteinGroupKey, Identification identification) {
        
        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinGroupKey);
        String[] accessions = proteinMatch.getAccessions();
        String accessionsString = Arrays.stream(accessions)
                .collect(Collectors.joining(","));

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) (proteinMatch).getUrParam(psParameter);
        String validationString = psParameter.getMatchValidationLevel().getName();

        StringBuilder sb = new StringBuilder(accessionsString.length() + validationString.length() + 2);
        sb.append(accessionsString).append("(").append(validationString).append(")");
        
        return sb.toString();
    }
    
    /**
     * Returns the peptide localization on the given protein as text in the form accession(site1,site2).
     * 
     * @param proteinAccession the protein accession
     * @param sites the position of the peptide on the protein
     * 
     * @return the peptide localization on the given protein as text
     */
    public static String getPeptideLocalizationText(String proteinAccession, int[] sites) {
        
        String sitesString = Arrays.stream(sites)
                .mapToObj(site -> Integer.toString(site))
                .collect(Collectors.joining(","));
        
        StringBuilder sb = new StringBuilder(proteinAccession.length() + sitesString.length() + 2);
        sb.append(proteinAccession).append("(").append(sitesString).append(")");
        
        return sb.toString();
    }

    /**
     * Returns the peptide modification location confidence as a string.
     *
     * @param peptideMatch the peptide match
     * @param modificationParameters the modification parameters
     *
     * @return the peptide modification location confidence as a string
     */
    public static String getPeptideModificationLocationConfidence(PeptideMatch peptideMatch, ModificationParameters modificationParameters) {

        PSPtmScores psPtmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());

        if (psPtmScores != null) {
            
            ArrayList<String> modList = psPtmScores.getScoredPTMs();

            StringBuilder result = new StringBuilder();
            Collections.sort(modList);

            for (String mod : modList) {
            
                if (result.length() > 0) {
                
                    result.append(", ");
                
                }
                
                result.append(mod).append(" (");
                PtmScoring ptmScoring = psPtmScores.getPtmScoring(mod);
                boolean firstSite = true;
                
                for (int site : ptmScoring.getOrderedPtmLocations()) {
                
                    if (firstSite) {
                    
                        firstSite = false;
    
                    } else {
                    
                        result.append(", ");
                    
                    }
                    
                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                    
                    if (ptmConfidence == PtmScoring.NOT_FOUND) {
                    
                        result.append(site).append(": Not Scored");
                  
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
                
                result.append(")");
            
            }

            return result.toString();
            
        }
        
        return "";
    }

    /**
     * Writes the title of the section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : peptideFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        writer.newLine();
    }
}
