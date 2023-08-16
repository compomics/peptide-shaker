package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.pride.CvTerm;

/**
 * Utils for the export and import of ms2pip results.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class DeepLcUtils {

    /**
     * Gets the peptide data to provide to ms2pip.
     *
     * @param peptideAssumption The peptide assumption.
     * @param retentionTime The retention time when the peptide was measured.
     * @param modificationParameters The modification parameters of the search.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param modificationFactory The factory containing the modification
     * details
     *
     * @return The peptide data as string.
     */
    public static String getPeptideData(
            PeptideAssumption peptideAssumption,
            double retentionTime,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory
    ) {

        return String.join(",",
                peptideAssumption.getPeptide().getSequence(),
                getModifications(peptideAssumption.getPeptide(), modificationParameters, sequenceProvider, sequenceMatchingParameters, modificationFactory),
                Double.toString(retentionTime)
        );
    }

    /**
     * Returns the modifications of the peptides encoded as required by DeepLc.
     *
     * @param peptide The peptide.
     * @param modificationParameters The modification parameters of the search.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param modificationFactory The factory containing the modification
     * details
     *
     * @return the modifications of the peptides encoded as required by DeepLc.
     */
    public static String getModifications(
            Peptide peptide,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory
    ) {

        String peptideSequence = peptide.getSequence();

        // Fixed modifications
        String[] fixedModifications = peptide.getFixedModifications(modificationParameters, sequenceProvider, sequenceMatchingParameters);

        StringBuilder modificationSites = new StringBuilder();

        for (int i = 0; i < fixedModifications.length; i++) {

            if (fixedModifications[i] != null) {

                int site = i < peptideSequence.length() + 1 ? i : -1;

                String modName = fixedModifications[i];
                Modification modification = modificationFactory.getModification(modName);
                CvTerm cvTerm = modification.getUnimodCvTerm();

                if (cvTerm == null) {

                    throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

                }

                String unimodName = cvTerm.getName();

                if (modificationSites.length() > 0) {

                    modificationSites.append('|');

                }

                modificationSites.append(site)
                        .append('|')
                        .append(unimodName);

            }
        }

        // Variable modifications
        String[] variableModifications = peptide.getIndexedVariableModifications();

        for (int i = 0; i < variableModifications.length; i++) {

            if (variableModifications[i] != null) {

                int site = i < peptideSequence.length() + 1 ? i : -1;

                String modName = variableModifications[i];
                Modification modification = modificationFactory.getModification(modName);

                CvTerm cvTerm = modification.getUnimodCvTerm();

                if (cvTerm == null) {

                    throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

                }

                String unimodName = cvTerm.getName();

                if (modificationSites.length() > 0) {

                    modificationSites.append('|');

                }

                modificationSites.append(site)
                        .append('|')
                        .append(unimodName);

            }
        }

        return modificationSites.toString();

    }

    /**
     * Returns a unique key corresponding to the given peptide.
     *
     * @param peptideData The peptide data as string.
     *
     * @return The unique key corresponding to the peptide data.
     */
    public static long getPeptideKey(
            String peptideData
    ) {

        return ExperimentObject.asLong(peptideData);

    }

}
