package eu.isas.peptideshaker.cmd.stirred.application;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.scores.PhosphoRS;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Runnable for the modification score application.
 *
 * @author Marc Vaudel
 */
public class ModificationScoreRunnable implements Runnable {

    /**
     * The modification factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();

    private final SimpleFileReader reader;

    private final String spectrumFileName;

    private final SimpleFileWriter writer;

    private final ModificationParameters modificationParameters;

    private final AnnotationParameters annotationParameters;

    private final SequenceMatchingParameters sequenceMatchingParameters;

    private final SequenceMatchingParameters modificationSequenceMatchingParameters;

    private final boolean accountNeutralLosses;

    private final SequenceProvider sequenceProvider;

    private final SpectrumProvider spectrumProvider;

    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

    public ModificationScoreRunnable(
            SimpleFileReader reader,
            String spectrumFileName,
            SimpleFileWriter writer,
            ModificationParameters modificationParameters,
            AnnotationParameters annotationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceMatchingParameters modificationSequenceMatchingParameters,
            boolean accountNeutralLosses,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider
    ) {

        this.reader = reader;
        this.spectrumFileName = spectrumFileName;
        this.writer = writer;
        this.modificationParameters = modificationParameters;
        this.annotationParameters = annotationParameters;
        this.sequenceMatchingParameters = sequenceMatchingParameters;
        this.modificationSequenceMatchingParameters = modificationSequenceMatchingParameters;
        this.accountNeutralLosses = accountNeutralLosses;
        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;

    }

    @Override
    public void run() {

        String line;

        while ((line = reader.readLine()) != null) {

            SpectrumMatch spectrumMatch = null;
            
            PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
            Peptide peptide = peptideAssumption.getPeptide();
            String spectrumTitle = spectrumMatch.getSpectrumTitle();

            Spectrum spectrum = spectrumProvider.getSpectrum(line, spectrumTitle);

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

            TreeMap<Double, HashMap<Integer, Double>> scores = modificationsMap.entrySet()
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
                                            accountNeutralLosses,
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

}
