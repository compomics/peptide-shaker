package eu.isas.peptideshaker.stirred.modules;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideVariantMatches;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.mapping.ModificationLocalizationMapper;
import com.compomics.util.experiment.identification.modification.mapping.ModificationNameMapper;
import com.compomics.util.experiment.identification.modification.scores.PhosphoRS;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import eu.isas.peptideshaker.fileimport.PsmImporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class scores the modification localization.
 *
 * @author Marc Vaudel
 */
public class StirAndExportRunnable implements Runnable {

    /**
     * The modification factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();

    private final IdfileReader idfileReader;

    private final ConcurrentLinkedQueue<SpectrumMatch> spectrumMatches;

    private final String spectrumFileName;

    private final SimpleFileWriter writer;

    private final IdentificationParameters identificationParameters;

    private final FastaMapper fastaMapper;

    private final SequenceProvider sequenceProvider;

    private final SpectrumProvider spectrumProvider;

    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

    public StirAndExportRunnable(
            ConcurrentLinkedQueue<SpectrumMatch> spectrumMatches,
            IdfileReader idfileReader,
            String spectrumFileName,
            SimpleFileWriter writer,
            IdentificationParameters identificationParameters,
            FastaMapper fastaMapper,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider
    ) {

        this.spectrumMatches = spectrumMatches;
        this.idfileReader = idfileReader;
        this.spectrumFileName = spectrumFileName;
        this.writer = writer;
        this.identificationParameters = identificationParameters;
        this.fastaMapper = fastaMapper;
        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;

    }

    @Override
    public void run() {

        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = spectrumMatches.poll()) != null) {

            processSpectrumMatch(spectrumMatch);

        }
    }

    private void processSpectrumMatch(SpectrumMatch spectrumMatch) {

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : spectrumMatch.getPeptideAssumptionsMap().entrySet()) {

            // Check that X!Tandem refinement modifications are set in the parameters, add them otherwise.
            int advocateId = entry.getKey();

            if (advocateId == Advocate.xtandem.getIndex()) {

                PsmImporter.verifyXTandemModifications(identificationParameters);

            }

            // Iterate all peptides and process them
            entry.getValue()
                    .values()
                    .stream()
                    .flatMap(
                            assumptionsAtScore -> assumptionsAtScore.stream()
                    )
                    .forEach(
                            peptideAssumption -> processPeptideAssumption(
                                    spectrumMatch,
                                    peptideAssumption
                            )
                    );
        }
    }

    private void processPeptideAssumption(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption
    ) {

        Peptide peptide = peptideAssumption.getPeptide();

        // Map peptide to protein
        proteinMapping(peptide);

        // map the algorithm specific modifications on utilities modifications
        // If there are not enough sites to put them all on the sequence, add an unknown modification
        // Note: this needs to be done for tag based assumptions as well since the protein mapping can return erroneous modifications for some pattern based modifications
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        ModificationParameters modificationParameters = searchParameters.getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        ModificationMatch[] modificationMatches = peptide.getVariableModifications();

        HashMap<Integer, ArrayList<String>> expectedNames = new HashMap<>(modificationMatches.length);
        HashMap<ModificationMatch, ArrayList<String>> modNames = new HashMap<>(modificationMatches.length);

        for (ModificationMatch modMatch : modificationMatches) {

            HashMap<Integer, HashSet<String>> tempNames = ModificationNameMapper.getPossibleModificationNames(
                    peptide,
                    modMatch,
                    idfileReader,
                    searchParameters,
                    modificationSequenceMatchingParameters,
                    sequenceProvider,
                    modificationFactory
            );

            HashSet<String> allNames = tempNames.values()
                    .stream()
                    .flatMap(
                            nameList -> nameList.stream()
                    )
                    .collect(
                            Collectors.toCollection(HashSet::new)
                    );

            modNames.put(modMatch, new ArrayList<>(allNames));

            for (int pos : tempNames.keySet()) {

                ArrayList<String> namesAtPosition = expectedNames.get(pos);

                if (namesAtPosition == null) {

                    namesAtPosition = new ArrayList<>(2);
                    expectedNames.put(pos, namesAtPosition);

                }

                for (String modName : tempNames.get(pos)) {

                    if (!namesAtPosition.contains(modName)) {

                        namesAtPosition.add(modName);

                    }
                }
            }
        }

        if (peptide.getVariableModifications().length > 0) {

            ModificationLocalizationMapper.modificationLocalization(
                    peptide,
                    expectedNames,
                    modNames,
                    identificationParameters,
                    idfileReader,
                    modificationFactory
            );

        }

        // Set peptide key
        peptide.setKey(Peptide.getKey(peptide.getSequence(), peptide.getVariableModifications()));

        // Estimate mass
        peptide.getMass(
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters
        );

        // Modification localization scores
        TreeMap<Double, HashMap<Integer, Double>> modificationLocalizationScores = scoreModificationLocalization(spectrumMatch, peptideAssumption);

    }

    /**
     * Maps the peptide sequence to the FASTA file.
     *
     * @param peptide the peptide to map
     */
    private void proteinMapping(
            Peptide peptide
    ) {

        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();

        ArrayList<PeptideProteinMapping> peptideProteinMappings = fastaMapper.getProteinMapping(peptide.getSequence(), sequenceMatchingPreferences);
        HashMap<String, HashMap<String, int[]>> sequenceIndexes = PeptideProteinMapping.getPeptideProteinIndexesMap(peptideProteinMappings);

        TreeMap<String, int[]> proteinIndexes;

        if (sequenceIndexes.size() == 1) {

            proteinIndexes = new TreeMap<>(sequenceIndexes.values().stream().findAny().get());

        } else {

            proteinIndexes = new TreeMap<>();

            for (HashMap<String, int[]> tempIndexes : sequenceIndexes.values()) {

                for (Map.Entry<String, int[]> entry : tempIndexes.entrySet()) {

                    String accession = entry.getKey();
                    int[] newIndexes = entry.getValue();
                    int[] currentIndexes = proteinIndexes.get(accession);

                    if (currentIndexes == null) {

                        proteinIndexes.put(accession, newIndexes);

                    } else {

                        int[] mergedIndexes = IntStream.concat(Arrays.stream(currentIndexes), Arrays.stream(newIndexes))
                                .distinct()
                                .sorted()
                                .toArray();
                        proteinIndexes.put(accession, mergedIndexes);

                    }
                }
            }
        }

        peptide.setProteinMapping(proteinIndexes);

        HashMap<String, HashMap<Integer, PeptideVariantMatches>> variantMatches = PeptideProteinMapping.getVariantMatches(peptideProteinMappings);
        peptide.setVariantMatches(variantMatches);

    }

    private TreeMap<Double, HashMap<Integer, Double>> scoreModificationLocalization(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption
    ) {

        AnnotationParameters annotationParameters = identificationParameters.getAnnotationParameters();
        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        ModificationLocalizationParameters modificationLocalizationParameters = identificationParameters.getModificationLocalizationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = modificationLocalizationParameters.getSequenceMatchingParameters();

        Peptide peptide = peptideAssumption.getPeptide();
        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();

        Spectrum spectrum = spectrumProvider.getSpectrum(spectrumFile, spectrumTitle);

        HashMap<Double, ArrayList<Modification>> modificationsMap = new HashMap<>(1);

        for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

            Modification refMod = modificationFactory.getModification(modificationMatch.getModification());
            double modMass = refMod.getMass();

            if (!modificationsMap.containsKey(modMass)) {

                ArrayList<Modification> modifications = modificationParameters.getSameMassNotFixedModifications(modMass)
                        .stream()
                        .map(
                                modification -> modificationFactory.getModification(modification)
                        )
                        .collect(
                                Collectors.toCollection(ArrayList::new)
                        );

                modificationsMap.put(modMass, modifications);

            }
        }
        SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                spectrumFileName,
                spectrumTitle,
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                peptideSpectrumAnnotator
        );

        return modificationsMap.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                entry -> entry.getKey(),
                                entry -> PhosphoRS.getSequenceProbabilities(
                                        peptide,
                                        entry.getValue(),
                                        modificationParameters,
                                        spectrum,
                                        sequenceProvider,
                                        annotationParameters,
                                        specificAnnotationParameters,
                                        modificationLocalizationParameters.isProbabilisticScoreNeutralLosses(),
                                        sequenceMatchingParameters,
                                        modificationSequenceMatchingParameters,
                                        peptideSpectrumAnnotator
                                ),
                                (a, b) -> a,
                                TreeMap::new
                        )
                );
    }

}
