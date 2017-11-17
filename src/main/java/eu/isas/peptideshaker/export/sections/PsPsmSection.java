package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSModificationScores;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.ModificationScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM level export features.
 *
 * @author Marc Vaudel
 */
public class PsPsmSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsPsmFeature> psmFeatures = new ArrayList<>();
    /**
     * The features to export.
     */
    private final ArrayList<PsIdentificationAlgorithmMatchesFeature> identificationAlgorithmMatchesFeatures = new ArrayList<>();
    /**
     * The fragment subsection if needed.
     */
    private PsFragmentSection fragmentSection = null;
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
    public PsPsmSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPsmFeature) {
                psmFeatures.add((PsPsmFeature) exportFeature);
            } else if (exportFeature instanceof PsIdentificationAlgorithmMatchesFeature) {
                identificationAlgorithmMatchesFeatures.add((PsIdentificationAlgorithmMatchesFeature) exportFeature);
            } else if (exportFeature instanceof PsFragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(psmFeatures);
        Collections.sort(identificationAlgorithmMatchesFeatures);
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new PsFragmentSection(fragmentFeatures, indexes, header, writer);
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
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * writing to the file.
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider,
            IdentificationParameters identificationParameters, long[] keys,
            String linePrefix, int nSurroundingAA, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        int line = 1;
        int totalSize = identification.getNumber(SpectrumMatch.class);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(totalSize);
        }

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(keys, waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            String spectrumMatchKey = spectrumMatch.getSpectrumKey();
            PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                if (decoys || peptideAssumption == null || !PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider)) {

                    boolean first = true;

                    if (indexes) {
                        if (linePrefix != null) {
                            writer.write(linePrefix);
                        }
                        writer.write(line + "");
                        first = false;
                    }
                    for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : identificationAlgorithmMatchesFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        String feature;
                        if (peptideAssumption != null) {
                            peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                            feature = PsIdentificationAlgorithmMatchesSection.getPeptideAssumptionFeature(identification, identificationFeaturesGenerator,
                                    sequenceProvider, proteinDetailsProvider,
                                    identificationParameters, linePrefix, nSurroundingAA, peptideAssumption, spectrumMatch.getSpectrumKey(),
                                    psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                            feature = PsIdentificationAlgorithmMatchesSection.getTagAssumptionFeature(identification, identificationFeaturesGenerator,
                                    identificationParameters, linePrefix, tagAssumption, spectrumMatch.getSpectrumKey(), psParameter,
                                    identificationAlgorithmMatchesFeature, waitingHandler);
                        } else {
                            throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                        }
                        writer.write(feature);
                    }
                    for (PsPsmFeature psmFeature : psmFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        writer.write(getFeature(identification, identificationFeaturesGenerator, identificationParameters,
                                linePrefix, spectrumMatch, psParameter, psmFeature, validatedOnly, decoys, waitingHandler));
                    }
                    writer.newLine();
                    if (fragmentSection != null) {
                        String fractionPrefix = "";
                        if (linePrefix != null) {
                            fractionPrefix += linePrefix;
                        }
                        fractionPrefix += line + ".";
                        writer.increaseDepth();
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            fragmentSection.writeSection(spectrumMatchKey, spectrumMatch.getBestPeptideAssumption(), identificationParameters, sequenceProvider, fractionPrefix, null);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            fragmentSection.writeSection(spectrumMatchKey, spectrumMatch.getBestTagAssumption(), identificationParameters, sequenceProvider, fractionPrefix, null);
                        }
                        writer.decreseDepth();
                    }
                    line++;
                }
            }
        }
    }

    /**
     * Writes the given feature of the current section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param identificationParameters the identification parameters
     * @param linePrefix the line prefix
     * @param spectrumMatch the spectrum match inspected
     * @param psParameter the PeptideShaker parameter of the match
     * @param psmFeature the feature to export
     * @param validatedOnly indicates whether only validated hits should be
     * exported
     * @param decoys indicates whether decoys should be included in the export
     * @param waitingHandler the waiting handler
     *
     * @return the content corresponding to the given feature of the current
     * section
     */
    public static String getFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, String linePrefix,
            SpectrumMatch spectrumMatch, PSParameter psParameter, PsPsmFeature psmFeature, boolean validatedOnly, boolean decoys,
            WaitingHandler waitingHandler) {

        switch (psmFeature) {

            case protein_groups:

                if (spectrumMatch.getBestPeptideAssumption() != null) {

                    HashSet<Long> proteinGroups = identification.getProteinMatches(spectrumMatch.getBestPeptideAssumption().getPeptide());

                    return proteinGroups.stream()
                            .map(key -> getProteinGroupText(key, identification))
                            .collect(Collectors.joining(";"));

                }

                return "";

            case best_protein_group_validation:

                if (spectrumMatch.getBestPeptideAssumption() != null) {

                    HashSet<Long> proteinGroups = identification.getProteinMatches(spectrumMatch.getBestPeptideAssumption().getPeptide());

                    int bestIndex = proteinGroups.stream()
                            .map(key -> ((PSParameter) identification.getProteinMatch(key).getUrParam(PSParameter.dummy)).getMatchValidationLevel())
                            .mapToInt(MatchValidationLevel::getIndex)
                            .max()
                            .orElse(MatchValidationLevel.none.getIndex());

                    return MatchValidationLevel.getMatchValidationLevel(bestIndex).getName();

                }

                return "";

            case probabilistic_score:

                if (spectrumMatch.getBestPeptideAssumption() != null) {

                    PSModificationScores ptmScores = new PSModificationScores();
                    ptmScores = (PSModificationScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        StringBuilder result = new StringBuilder();
                        ArrayList<String> modList = new ArrayList<>(ptmScores.getScoredPTMs());
                        Collections.sort(modList);
                        for (String mod : modList) {
                            ModificationScoring ptmScoring = ptmScores.getModificationScoring(mod);
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
                }
                
                return "";
                
            case d_score:
                
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    
                    StringBuilder result = new StringBuilder();
                    PSModificationScores ptmScores = new PSModificationScores();
                    ptmScores = (PSModificationScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        ArrayList<String> modList = new ArrayList<>(ptmScores.getScoredPTMs());
                        Collections.sort(modList);
                        for (String mod : modList) {
                            ModificationScoring ptmScoring = ptmScores.getModificationScoring(mod);
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
                    }
                    return result.toString();
                }
                
                return "";
                
            case localization_confidence:
                
                return getPeptideModificationLocationConfidence(spectrumMatch, identificationParameters.getSearchParameters().getModificationParameters());
                
            case algorithm_score:
                
                PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();
                TagAssumption bestTagAssumption = spectrumMatch.getBestTagAssumption();
                
                if (bestPeptideAssumption != null) {
                    
                   return spectrumMatch.getAllPeptideAssumptions()
                            .filter(peptideAssumption -> peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(
                                            bestPeptideAssumption.getPeptide(), identificationParameters.getSequenceMatchingParameters()))
                            .collect(Collectors.toMap(PeptideAssumption::getAdvocate, 
                                    Function.identity(), 
                                    (a, b) -> b.getScore() < a.getScore() ? b : a, 
                                    TreeMap::new))
                            .entrySet().stream()
                            .map(entry -> Util.toString(Advocate.getAdvocate(entry.getKey()).getName(), 
                                    Double.toString(entry.getValue().getRawScore())))
                            .collect(Collectors.joining(","));
                   
                }
                
                return "";
                   
            case confidence:
            
                return Double.toString(psParameter.getPsmConfidence());
                
            case score:
                
                return Double.toString(psParameter.getPsmScore());
                
            case raw_score:
                
                return Double.toString(psParameter.getPsmProbabilityScore());
                
            case validated:
                
                return psParameter.getMatchValidationLevel().toString();
                
            case starred:
                
               return psParameter.getStarred() ? "1" : "0";
                
            case hidden:
                
               return psParameter.getHidden() ? "1" : "0";
               
            case confident_modification_sites:
                
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    return identificationFeaturesGenerator.getConfidentPtmSites(spectrumMatch, sequence);
                    
                }
                
                return "";
                
            case confident_modification_sites_number:
                
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(spectrumMatch);
                
            case ambiguous_modification_sites:
                
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    return identificationFeaturesGenerator.getAmbiguousPtmSites(spectrumMatch, sequence);
                    
                }
                
                return "";
                
            case ambiguous_modification_sites_number:
                
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(spectrumMatch);
                
            case confident_phosphosites:
                
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    ArrayList<String> modifications = new ArrayList<>(3);
                    
                    for (String modName : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                        
                        if (modName.toLowerCase().contains("phospho")) {
                            
                            modifications.add(modName);
                            
                        }
                    }
                    
                    return identificationFeaturesGenerator.getConfidentPtmSites(spectrumMatch, sequence, modifications);
                    
                }
                
                return "";
                
            case confident_phosphosites_number:
                
                ArrayList<String> modifications = new ArrayList<>(3);
                
                for (String modName : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    
                    if (modName.toLowerCase().contains("phospho")) {
                        
                        modifications.add(modName);
                        
                    }
                }
                
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(spectrumMatch, modifications);
                
            case ambiguous_phosphosites:
                
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    modifications = new ArrayList<>(3);
                    
                    for (String modName : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                        
                        if (modName.toLowerCase().contains("phospho")) {
                            
                            modifications.add(modName);
                            
                        }
                    }
                    
                    return identificationFeaturesGenerator.getAmbiguousPtmSites(spectrumMatch, sequence, modifications);
                    
                }
                
                return "";
                
            case ambiguous_phosphosites_number:
                
                modifications = new ArrayList<>(3);
                
                for (String modName : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    
                    if (modName.toLowerCase().contains("phospho")) {
                        
                        modifications.add(modName);
                        
                    }
                }
                
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(spectrumMatch, modifications);
                
            default:
                return "Not implemented";
                
        }
    }

    /**
     * Returns a description of the given protein group in the form
     * proteinA,proteinB(confidence).
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
     * Returns the peptide modification location confidence as a string.
     *
     * @param spectrumMatch the spectrum match
     * @param modificationParameters the PTM profile
     *
     * @return the peptide modification location confidence as a string
     */
    public static String getPeptideModificationLocationConfidence(SpectrumMatch spectrumMatch, ModificationParameters modificationParameters) {

        PSModificationScores psPtmScores = (PSModificationScores) spectrumMatch.getUrParam(new PSModificationScores());

        if (psPtmScores != null) {
            
            ArrayList<String> modList = psPtmScores.getScoredPTMs();

            StringBuilder result = new StringBuilder();
            Collections.sort(modList);

            for (String mod : modList) {
            
                if (result.length() > 0) {
                
                    result.append(", ");
                
                }
                
                result.append(mod).append(" (");
                ModificationScoring ptmScoring = psPtmScores.getModificationScoring(mod);
                boolean firstSite = true;
                
                for (int site : ptmScoring.getOrderedPtmLocations()) {
                
                    if (firstSite) {
                    
                        firstSite = false;
    
                    } else {
                    
                        result.append(", ");
                    
                    }
                    
                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                    
                    if (ptmConfidence == ModificationScoring.NOT_FOUND) {
                    
                        result.append(site).append(": Not Scored");
                  
                    } else if (ptmConfidence == ModificationScoring.RANDOM) {
                    
                        result.append(site).append(": Random");
                    
                    } else if (ptmConfidence == ModificationScoring.DOUBTFUL) {
                    
                        result.append(site).append(": Doubtfull");
                    
                    } else if (ptmConfidence == ModificationScoring.CONFIDENT) {
                    
                        result.append(site).append(": Confident");
                    
                    } else if (ptmConfidence == ModificationScoring.VERY_CONFIDENT) {
                    
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
     * Writes the header of this section.
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
        for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        for (ExportFeature exportFeature : psmFeatures) {
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
