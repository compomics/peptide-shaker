package eu.isas.peptideshaker.export;

import com.compomics.util.parameters.identification.search.ModificationParameters;
import java.util.HashSet;

/**
 * Utils for exports.
 *
 * @author Marc Vaudel
 */
public class ExportUtils {

    /**
     * Placeholder for the names of phosphorylations.
     */
    private static HashSet<String> phosphorylations = null;

    /**
     * Returns the names of the variable modifications containing "phospho", not
     * case sensitive.
     *
     * @param modificationParameters The modification parameters to use to get
     * the variable modifications.
     *
     * @return The names of the variable modifications containing "phospho" as a
     * set.
     */
    public static HashSet<String> getPhosphorylations(
            ModificationParameters modificationParameters
    ) {

        if (phosphorylations == null) {

            HashSet<String> modifications = new HashSet<>(3);

            for (String ptm : modificationParameters.getAllNotFixedModifications()) {

                if (ptm.toLowerCase().contains("phospho")) {

                    modifications.add(ptm);

                }
            }

            phosphorylations = modifications;

        }

        return phosphorylations;

    }

}
