package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.ions.Ion;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import static eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature.fragment_number;
import static eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature.fragment_type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class outputs the PSM related export features.
 *
 * @author Marc Vaudel
 */
public class PsFragmentSection {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The features to export.
     */
    private final ArrayList<PsFragmentFeature> fragmentFeatures;
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
    public PsFragmentSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {

        this.indexes = indexes;
        this.header = header;
        this.writer = writer;

        fragmentFeatures = new ArrayList<>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsFragmentFeature) {
                PsFragmentFeature fragmentFeature = (PsFragmentFeature) exportFeature;
                fragmentFeatures.add(fragmentFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as fragment feature.");
            }
        }
        Collections.sort(fragmentFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param spectrumKey the key of the spectrum
     * @param spectrumIdentificationAssumption the spectrum identification of
     * interest
     * @param sequenceProvider the sequence provider
     * @param identificationParameters the identification parameters
     * @param linePrefix the line prefix
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(String spectrumKey, SpectrumIdentificationAssumption spectrumIdentificationAssumption, 
            SequenceProvider sequenceProvider, IdentificationParameters identificationParameters,
            String linePrefix, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            
        }

        if (header) {
            
            writeHeader();
            
        }

        IonMatch[] annotations;
        Spectrum spectrum = spectrumFactory.getSpectrum(spectrumKey);
        AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        SpecificAnnotationParameters specificAnnotationParameters = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, spectrumIdentificationAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
        
        if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
            
            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
            PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
            annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationParameters,
                    spectrum, peptideAssumption.getPeptide(), modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
            
        } else if (spectrumIdentificationAssumption instanceof TagAssumption) {
            
            TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
            TagSpectrumAnnotator spectrumAnnotator = new TagSpectrumAnnotator();
            annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, modificationParameters, modificationSequenceMatchingParameters, 
                    specificAnnotationParameters, spectrum, tagAssumption.getTag());
            
        } else {
            
            throw new UnsupportedOperationException("Export not implemented for spectrum identification of type " + spectrumIdentificationAssumption.getClass() + ".");
        
        }

        HashMap<Double, ArrayList<IonMatch>> sortedAnnotation = new HashMap<>(annotations.length);
        
        for (IonMatch ionMatch : annotations) {
            
            double mz = ionMatch.peak.mz;
            
            if (!sortedAnnotation.containsKey(mz)) {
                
                sortedAnnotation.put(mz, new ArrayList<>(1));
                
            }
            
            sortedAnnotation.get(mz).add(ionMatch);
        }
        ArrayList<Double> mzs = new ArrayList<>(sortedAnnotation.keySet());
        Collections.sort(mzs);

        int line = 1;

        if (waitingHandler != null) {
            
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(annotations.length);
            
        }

        for (double mz : mzs) {
            
            for (IonMatch ionMatch : sortedAnnotation.get(mz)) {

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
                                writer.write(((PeptideFragmentIon) ion).getNumber() + "");
                            }
                            break;
                        case fragment_losses:
                            writer.write(ionMatch.ion.getNeutralLossesAsString());
                            break;
                        case fragment_name:
                            ion = ionMatch.ion;
                            String name;
                            if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                                name = ((PeptideFragmentIon) ion).getNameWithNumber();
                            } else {
                                name = ion.getName();
                            }
                            writer.write(name);
                            break;
                        case fragment_charge:
                            writer.write(ionMatch.charge + "");
                            break;
                        case theoretic_mz:
                            writer.write(ionMatch.ion.getTheoreticMz(ionMatch.charge) + "");
                            break;
                        case mz:
                            writer.write(ionMatch.peak.mz + "");
                            break;
                        case intensity:
                            writer.write(ionMatch.peak.intensity + "");
                            break;
                        case error_Da:
                            writer.write(ionMatch.getAbsoluteError() + "");
                            break;
                        case error_ppm:
                            writer.write(ionMatch.getRelativeError() + "");
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
