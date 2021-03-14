package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.ions.Ion;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.io.export.features.peptideshaker.PsFragmentFeature;
import static com.compomics.util.io.export.features.peptideshaker.PsFragmentFeature.fragment_number;
import static com.compomics.util.io.export.features.peptideshaker.PsFragmentFeature.fragment_type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class outputs the PSM related export features.
 *
 * @author Marc Vaudel
 */
public class PsFragmentSection {

    /**
     * The features to export.
     */
    private final EnumSet<PsFragmentFeature> fragmentFeatures;
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
     * @param exportFeatures The features to export in this section.
     * @param indexes A boolean indicating whether the line index should be
     * written.
     * @param header A boolean indicates whether the table header should be
     * written.
     * @param writer The writer which will write to the file.
     */
    public PsFragmentSection(
            ArrayList<ExportFeature> exportFeatures,
            boolean indexes,
            boolean header,
            ExportWriter writer
    ) {

        this.indexes = indexes;
        this.header = header;
        this.writer = writer;

        fragmentFeatures = EnumSet.noneOf(PsFragmentFeature.class);

        for (ExportFeature exportFeature : exportFeatures) {

            if (exportFeature instanceof PsFragmentFeature) {

                PsFragmentFeature fragmentFeature = (PsFragmentFeature) exportFeature;
                fragmentFeatures.add(fragmentFeature);

            } else {

                throw new IllegalArgumentException(
                        "Impossible to export "
                        + exportFeature.getClass().getName()
                        + " as fragment feature."
                );

            }
        }
    }

    /**
     * Writes the desired section.
     *
     * @param spectrumFile The file name of the spectrum.
     * @param spectrumTitle The title of the spectrum.
     * @param spectrumIdentificationAssumption The spectrum identification of
     * interest.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param identificationParameters The identification parameters.
     * @param linePrefix The line prefix.
     * @param waitingHandler The waiting handler.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(
            String spectrumFile,
            String spectrumTitle,
            SpectrumIdentificationAssumption spectrumIdentificationAssumption,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            String linePrefix,
            WaitingHandler waitingHandler
    ) throws IOException {

        if (waitingHandler != null) {

            waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        }

        if (header) {

            writeHeader();

        }

        IonMatch[] annotations;
        Spectrum spectrum = spectrumProvider.getSpectrum(
                spectrumFile,
                spectrumTitle
        );
        AnnotationParameters annotationParameters = identificationParameters.getAnnotationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        if (spectrumIdentificationAssumption instanceof PeptideAssumption) {

            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
            PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
            SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                    spectrumFile,
                    spectrumTitle,
                    spectrumIdentificationAssumption,
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters,
                    spectrumAnnotator
            );
            annotations = spectrumAnnotator.getSpectrumAnnotation(annotationParameters,
                    specificAnnotationParameters,
                    spectrumFile,
                    spectrumTitle,
                    spectrum,
                    peptideAssumption.getPeptide(),
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters
            );

        } else if (spectrumIdentificationAssumption instanceof TagAssumption) {

            TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
            TagSpectrumAnnotator spectrumAnnotator = new TagSpectrumAnnotator();
            SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                    spectrumFile,
                    spectrumTitle,
                    spectrumIdentificationAssumption,
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters,
                    spectrumAnnotator
            );
            annotations = spectrumAnnotator.getSpectrumAnnotation(
                    annotationParameters,
                    modificationParameters,
                    modificationSequenceMatchingParameters,
                    specificAnnotationParameters,
                    spectrumFile,
                    spectrumTitle,
                    spectrum,
                    tagAssumption.getTag()
            );

        } else {

            throw new UnsupportedOperationException(
                    "Export not implemented for spectrum identification of type "
                    + spectrumIdentificationAssumption.getClass()
                    + "."
            );

        }

        TreeMap<Double, ArrayList<IonMatch>> sortedAnnotation = new TreeMap<>();

        for (IonMatch ionMatch : annotations) {

            double mz = ionMatch.peakMz;

            if (!sortedAnnotation.containsKey(mz)) {

                sortedAnnotation.put(mz, new ArrayList<>(1));

            }

            sortedAnnotation.get(mz).add(ionMatch);

        }

        int line = 1;

        if (waitingHandler != null) {

            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(annotations.length);

        }

        for (Entry<Double, ArrayList<IonMatch>> entry : sortedAnnotation.entrySet()) {

            for (IonMatch ionMatch : entry.getValue()) {

                if (waitingHandler != null) {

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }

                if (indexes) {

                    if (linePrefix != null) {

                        writer.write(linePrefix);

                    }

                    writer.write(Integer.toString(line));
                    writer.addSeparator();

                }

                for (PsFragmentFeature fragmentFeature : fragmentFeatures) {

                    switch (fragmentFeature) {
                        case annotation:
                            writer.write(ionMatch.getPeakAnnotation());
                            break;
                        case fragment_type:
                            writer.write(Ion.getTypeAsString(ionMatch.ion.getType()));
                            break;
                        case fragment_subType:
                            writer.write(ionMatch.ion.getSubTypeAsString());
                            break;
                        case fragment_number:
                            Ion ion = ionMatch.ion;
                            if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                                writer.write(Integer.toString(((PeptideFragmentIon) ion).getNumber()));
                            }
                            break;
                        case fragment_losses:
                            writer.write(ionMatch.ion.getNeutralLossesAsString());
                            break;
                        case fragment_name:
                            ion = ionMatch.ion;
                            String name = ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION
                                    ? ((PeptideFragmentIon) ion).getNameWithNumber()
                                    : ion.getName();
                            writer.write(name);
                            break;
                        case fragment_charge:
                            writer.write(Integer.toString(ionMatch.charge));
                            break;
                        case theoretic_mz:
                            writer.write(Double.toString(ionMatch.ion.getTheoreticMz(ionMatch.charge)));
                            break;
                        case mz:
                            writer.write(Double.toString(ionMatch.peakMz));
                            break;
                        case intensity:
                            writer.write(Double.toString(ionMatch.peakIntensity));
                            break;
                        case error_Da:
                            writer.write(Double.toString(ionMatch.getAbsoluteError()));
                            break;
                        case error_ppm:
                            writer.write(Double.toString(ionMatch.getRelativeError()));
                            break;
                        default:
                            writer.write("Not implemented");
                    }

                    writer.addSeparator();

                }

                writer.newLine();
                line++;

            }
        }
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
        for (PsFragmentFeature fragmentFeature : fragmentFeatures) {

            if (firstColumn) {

                firstColumn = false;

            } else {

                writer.addSeparator();

            }

            writer.writeHeaderText(fragmentFeature.getTitle());

        }

        writer.newLine();

    }
}
