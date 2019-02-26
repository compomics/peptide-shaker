package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Export for PathwayMatcher.
 *
 * @author Marc Vaudel
 */
public class ProteoformExport {

    /**
     * Writes an export with all the possible proteoforms of the validated
     * proteins.
     *
     * @param destinationFile the destination file
     * @param identification the identification
     * @param waitingHandler the waiting handler
     */
    public static void writeProteoforms(
            File destinationFile,
            Identification identification,
            WaitingHandler waitingHandler
    ) {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        if (waitingHandler != null) {

            waitingHandler.setWaitingText("Exporting Proteoforms. Please Wait...");
            // reset the progress bar
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());

        }

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        ProteinMatch proteinMatch;

        try (SimpleFileWriter simpleFileWriter = new SimpleFileWriter(destinationFile, false)) {

            while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                if (!proteinMatch.isDecoy()) {

                    for (String accession : proteinMatch.getAccessions()) {

                        boolean unmodified = false;
                        TreeSet<String> modifications = new TreeSet<>();

                        for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                            Peptide peptide = peptideMatch.getPeptide();

                            ModificationMatch[] modificationMatches = peptide.getVariableModifications();

                            if (modificationMatches.length > 0) {

                                int[] peptideStarts = peptide.getProteinMapping().get(accession);

                                for (int peptideStart : peptideStarts) {

                                    TreeMap<Integer, String> modMap = new TreeMap<>();

                                    for (ModificationMatch modificationMatch : modificationMatches) {
                                        String modName = modificationMatch.getModification();

                                        Modification modification = modificationFactory.getModification(modName);

                                        // try to map to PSI-MOD
                                        String modAccession = modificationFactory.getPsiModAccession(modName);

                                        // remove everything but the accession number, i.e. "MOD:00040" ends up as "00040"
                                        if (modAccession != null) {
                                            modAccession = modAccession.substring(modAccession.indexOf(':') + 1);
                                        }

                                        // not mapping to PSI-MOD, use the Unimod accession number instead
                                        if (modAccession == null) {
                                            modAccession = modification.getCvTerm().getAccession();
                                        }

                                        // not mapping to PSI-MOD or Unimod, use the modification name...
                                        if (modAccession == null) {
                                            modAccession = modName;
                                        }

                                        int site = modificationMatch.getSite();
                                        if (site == 0) {
                                            site = 1;
                                        } else if (site == peptide.getSequence().length() + 1) {
                                            site = peptide.getSequence().length();
                                        }
                                        int siteOnProtein = peptideStart + site;

                                        modMap.put(siteOnProtein, modAccession);

                                    }

                                    String proteoformMods = modMap.entrySet()
                                            .stream()
                                            .map(entry -> String.join(
                                            ":",
                                            entry.getValue(),
                                            Integer.toString(entry.getKey())))
                                            .collect(Collectors.joining(","));
                                    modifications.add(proteoformMods);

                                }

                            } else {

                                unmodified = true;

                            }
                        }

                        if (unmodified) {

                            simpleFileWriter.writeLine(accession + ";");

                        }

                        for (String modification : modifications) {

                            simpleFileWriter.writeLine(String.join(";", accession, modification));

                        }
                    }
                }

                if (waitingHandler != null) {

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }
            }
        }
    }
}
