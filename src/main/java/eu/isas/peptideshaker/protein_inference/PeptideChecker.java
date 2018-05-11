package eu.isas.peptideshaker.protein_inference;

import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidPattern;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class checks that the peptide to protein mapping is compatible with the
 * modification status of the peptide.
 *
 * @author Marc Vaudel
 */
public class PeptideChecker {

    
    /**
     * Corrects the protein mapping based on the confident or inferred variable modifications when located at the protein termini or targetting amino acid patterns.
     * 
     * @param peptide the peptide to check
     * @param sequenceProvider a protein sequence provider 
     * @param modificationMatchingParameters the modification sequence matching parameters
     */
    public static void checkPeptide(Peptide peptide, SequenceProvider sequenceProvider, SequenceMatchingParameters modificationMatchingParameters) {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        ModificationMatch[] variableModifications = peptide.getVariableModifications();

        TreeMap<String, int[]> proteinMapping = peptide.getProteinMapping();

        for (ModificationMatch modificationMatch : variableModifications) {

            if (modificationMatch.getConfident() || modificationMatch.getInferred()) {

                Modification modification = modificationFactory.getModification(modificationMatch.getModification());
                ModificationType modificationType = modification.getModificationType();

                if (modificationType == ModificationType.modn_protein || modificationType == ModificationType.modnaa_protein) {

                    proteinMapping = getNTermMapping(proteinMapping);

                } else if (modificationType == ModificationType.modc_protein || modificationType == ModificationType.modcaa_protein) {

                    proteinMapping = getCTermMapping(proteinMapping, peptide.getSequence().length(), sequenceProvider);

                }

                if (modification.getPattern().length() > 0) {

                    proteinMapping = getPatternMapping(proteinMapping, modificationMatch, modification, peptide.getSequence().length(), sequenceProvider, modificationMatchingParameters);

                }
            }
        }

        peptide.setProteinMapping(proteinMapping);

    }

    /**
     * Returns the modification mapping for a peptide carrying a protein n-term modification.
     * 
     * @param proteinMapping the protein mapping to check
     * 
     * @return the modification mapping for a peptide carrying a protein c-term modification
     */
    private static TreeMap<String, int[]> getNTermMapping(TreeMap<String, int[]> proteinMapping) {

        TreeMap<String, int[]> cleanedProteinMapping = new TreeMap<>();

        for (Entry<String, int[]> entry : proteinMapping.entrySet()) {

            if (Arrays.stream(entry.getValue()).anyMatch(site -> site == 0)) {

                cleanedProteinMapping.put(entry.getKey(), new int[]{0});

            }
        }

        return cleanedProteinMapping;

    }

    /**
     * Returns the modification mapping for a peptide carrying a protein c-term modification.
     * 
     * @param proteinMapping the protein mapping to check
     * @param peptideLength the peptide length
     * @param sequenceProvider a protein sequence provider 
     * 
     * @return the modification mapping for a peptide carrying a protein c-term modification
     */
    private static TreeMap<String, int[]> getCTermMapping(TreeMap<String, int[]> proteinMapping, int peptideLength, SequenceProvider sequenceProvider) {

        TreeMap<String, int[]> cleanedProteinMapping = new TreeMap<>();

        for (Entry<String, int[]> entry : proteinMapping.entrySet()) {

            String proteinAccession = entry.getKey();

            int peptideStart = sequenceProvider.getSequence(proteinAccession).length() - peptideLength;

            if (Arrays.stream(entry.getValue()).anyMatch(site -> site == peptideStart)) {

                cleanedProteinMapping.put(proteinAccession, new int[]{peptideStart});

            }
        }

        return cleanedProteinMapping;

    }

    /**
     * Returns the modification mapping corrected for modification amino acid pattern.
     * 
     * @param proteinMapping the protein mapping to check
     * @param modificationMatch the modification match
     * @param modification the modification
     * @param peptideLength the peptide length
     * @param sequenceProvider a protein sequence provider 
     * @param modificationMatchingParameters the modification sequence matching parameters
     * 
     * @return the modification mapping corrected for modification amino acid pattern
     */
    private static TreeMap<String, int[]> getPatternMapping(TreeMap<String, int[]> proteinMapping, ModificationMatch modificationMatch, Modification modification, int peptideLength, SequenceProvider sequenceProvider, SequenceMatchingParameters modificationMatchingParameters) {

        TreeMap<String, int[]> cleanedProteinMapping = new TreeMap<>();

        for (Entry<String, int[]> entry : proteinMapping.entrySet()) {

            String proteinAccession = entry.getKey();
            String proteinSequence = sequenceProvider.getSequence(proteinAccession);

            int[] sequenceStart = entry.getValue();

            int[] peptideStart = Arrays.stream(sequenceStart)
                    .filter(aa -> validSite(proteinSequence, aa, modificationMatch.getSite(), peptideLength, modification.getPattern(), modificationMatchingParameters))
                    .toArray();

            if (peptideStart.length > 0) {

                cleanedProteinMapping.put(proteinAccession, peptideStart);

            }

        }

        return cleanedProteinMapping;

    }

    /**
     * Indicates whether the peptide start allows the modification of given pattern to be on the given site of the given protein.
     * 
     * @param proteinSequence the protein sequence
     * @param peptideStart the peptide start
     * @param modificationSite the modification site as stated in the modification match
     * @param peptideLength the peptide length
     * @param aminoAcidPattern the amino acid pattern of the modification
     * @param modificationMatchingParameters the modification sequence matching parameters
     * 
     * @return a boolean indicating whether the peptide start allows the modification of given pattern to be on the given site of the given protein
     */
    private static boolean validSite(String proteinSequence, int peptideStart, int modificationSite, int peptideLength, AminoAcidPattern aminoAcidPattern, SequenceMatchingParameters modificationMatchingParameters) {

        if (modificationSite == 0) {

            modificationSite = 1;

        } else if (modificationSite == peptideLength + 1) {

            modificationSite = peptideLength;

        }

        int siteOnProtein = peptideStart + modificationSite - 1;

        return aminoAcidPattern.matchesAt(proteinSequence, modificationMatchingParameters, siteOnProtein);

    }
}
