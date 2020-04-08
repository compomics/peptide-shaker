package eu.isas.peptideshaker.cmd;

import com.compomics.cli.identification_parameters.IdentificationParametersCLIParams;
import static com.compomics.software.cli.CommandLineUtils.formatter;
import org.apache.commons.cli.Options;

/**
 * This class provides the parameters which can be used for the identification
 * parameters cli in PeptideShaker.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerIdentificationParametersCLIParams {

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {
        for (IdentificationParametersCLIParams identificationParametersCLIParams : IdentificationParametersCLIParams.values()) {
            aOptions.addOption(identificationParametersCLIParams.id, identificationParametersCLIParams.hasArgument, identificationParametersCLIParams.description);
        }

        // Path setup
        PathSettingsCLIParams.createOptionsCLI(aOptions);

        //@TODO: Add QC filters?
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";

        output += "Parameters Files:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OUT.id) + " " + IdentificationParametersCLIParams.OUT.description + ". (Mandatory)\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.id) + " " + IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.description + " (Optional)\n";
        output += getParametersOptionsAsString();
        return output;
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getParametersOptionsAsString() {

        String output = "";
        String formatter = "%-35s";

        output += "\n\nSpectrum Matching:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PREC_TOL.id) + " " + IdentificationParametersCLIParams.PREC_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PREC_PPM.id) + " " + IdentificationParametersCLIParams.PREC_PPM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FRAG_TOL.id) + " " + IdentificationParametersCLIParams.FRAG_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FRAG_PPM.id) + " " + IdentificationParametersCLIParams.FRAG_PPM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIGESTION.id) + " " + IdentificationParametersCLIParams.DIGESTION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ENZYME.id) + " " + IdentificationParametersCLIParams.ENZYME.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SPECIFICITY.id) + " " + IdentificationParametersCLIParams.SPECIFICITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FIXED_MODS.id) + " " + IdentificationParametersCLIParams.FIXED_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.VARIABLE_MODS.id) + " " + IdentificationParametersCLIParams.VARIABLE_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MIN_CHARGE.id) + " " + IdentificationParametersCLIParams.MIN_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MAX_CHARGE.id) + " " + IdentificationParametersCLIParams.MAX_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MC.id) + " " + IdentificationParametersCLIParams.MC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FI.id) + " " + IdentificationParametersCLIParams.FI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.RI.id) + " " + IdentificationParametersCLIParams.RI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MIN_ISOTOPE.id) + " " + IdentificationParametersCLIParams.MIN_ISOTOPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MAX_ISOTOPE.id) + " " + IdentificationParametersCLIParams.MAX_ISOTOPE.description + "\n";
  
        output += "\n\nX!Tandem advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_DYNAMIC_RANGE.id) + " " + IdentificationParametersCLIParams.XTANDEM_DYNAMIC_RANGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_NPEAKS.id) + " " + IdentificationParametersCLIParams.XTANDEM_NPEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_MIN_FRAG_MZ.id) + " " + IdentificationParametersCLIParams.XTANDEM_MIN_FRAG_MZ.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_MIN_PEAKS.id) + " " + IdentificationParametersCLIParams.XTANDEM_MIN_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_NOISE_SUPPRESSION.id) + " " + IdentificationParametersCLIParams.XTANDEM_NOISE_SUPPRESSION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_MIN_PREC_MASS.id) + " " + IdentificationParametersCLIParams.XTANDEM_MIN_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_PARENT_MONOISOTOPIC_MASS_ISOTOPE_ERROR.id) + " " + IdentificationParametersCLIParams.XTANDEM_PARENT_MONOISOTOPIC_MASS_ISOTOPE_ERROR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_QUICK_ACETYL.id) + " " + IdentificationParametersCLIParams.XTANDEM_QUICK_ACETYL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_QUICK_PYRO.id) + " " + IdentificationParametersCLIParams.XTANDEM_QUICK_PYRO.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_STP_BIAS.id) + " " + IdentificationParametersCLIParams.XTANDEM_STP_BIAS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_PTM_COMPLEXITY.id) + " " + IdentificationParametersCLIParams.XTANDEM_PTM_COMPLEXITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_EVALUE.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_EVALUE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_UNANTICIPATED_CLEAVAGE.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_UNANTICIPATED_CLEAVAGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_SEMI.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_SEMI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_POTENTIAL_MOD_FULL_REFINEMENT.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_POTENTIAL_MOD_FULL_REFINEMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_POINT_MUTATIONS.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_POINT_MUTATIONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_SNAPS.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_SNAPS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_REFINE_SPECTRUM_SYNTHESIS.id) + " " + IdentificationParametersCLIParams.XTANDEM_REFINE_SPECTRUM_SYNTHESIS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_EVALUE.id) + " " + IdentificationParametersCLIParams.XTANDEM_EVALUE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_OUTPUT_RESULTS.id) + " " + IdentificationParametersCLIParams.XTANDEM_OUTPUT_RESULTS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_OUTPUT_PROTEINS.id) + " " + IdentificationParametersCLIParams.XTANDEM_OUTPUT_PROTEINS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_OUTPUT_SEQUENCES.id) + " " + IdentificationParametersCLIParams.XTANDEM_OUTPUT_SEQUENCES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_OUTPUT_SPECTRA.id) + " " + IdentificationParametersCLIParams.XTANDEM_OUTPUT_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_OUTPUT_HISTOGRAMS.id) + " " + IdentificationParametersCLIParams.XTANDEM_OUTPUT_HISTOGRAMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.XTANDEM_SKYLINE.id) + " " + IdentificationParametersCLIParams.XTANDEM_SKYLINE.description + "\n";

        output += "\n\nMyriMatch advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_MIN_PREC_MASS.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_MIN_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_MAX_PREC_MASS.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_MAX_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_NUM_MATCHES.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_NUM_MATCHES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_PTMS.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_PTMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_FRAGMENTATION.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_FRAGMENTATION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_TERMINI.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_TERMINI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_SMART_PLUS_THREE.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_SMART_PLUS_THREE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_XCORR.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_XCORR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_TIC_CUTOFF.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_TIC_CUTOFF.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_INTENSTITY_CLASSES.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_INTENSTITY_CLASSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_CLASS_MULTIPLIER.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_CLASS_MULTIPLIER.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_NUM_BATCHES.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_NUM_BATCHES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_MAX_PEAK_COUNT.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_MAX_PEAK_COUNT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MYRIMATCH_OUTPUT_FORMAT.id) + " " + IdentificationParametersCLIParams.MYRIMATCH_OUTPUT_FORMAT.description + "\n";

        output += "\n\nMS Amanda advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_DECOY.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_DECOY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_DECOY_RANKING.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_DECOY_RANKING.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_INSTRUMENT.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_INSTRUMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_RANK.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_RANK.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MONOISOTOPIC.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MONOISOTOPIC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_PERFORM_DEISOTOPING.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_PERFORM_DEISOTOPING.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_MOD.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_MOD.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_VAR_MOD.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_VAR_MOD.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_MOD_SITES.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_MOD_SITES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_NEUTRAL_LOSSES.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_NEUTRAL_LOSSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_NEUTRAL_LOSSES_MODIFICATIONS.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_NEUTRAL_LOSSES_MODIFICATIONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MIN_PEPTIDE_LENGTH.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MIN_PEPTIDE_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_MAX_PEPTIDE_LENGTH.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_MAX_PEPTIDE_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_LOADED_PROTEINS.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_LOADED_PROTEINS   .description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_LOADED_SPECTRA.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_LOADED_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MS_AMANDA_OUTPUT_FORMAT.id) + " " + IdentificationParametersCLIParams.MS_AMANDA_OUTPUT_FORMAT.description + "\n";

        output += "\n\nMS-GF+ advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_DECOY.id) + " " + IdentificationParametersCLIParams.MSGF_DECOY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_INSTRUMENT.id) + " " + IdentificationParametersCLIParams.MSGF_INSTRUMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_FRAGMENTATION.id) + " " + IdentificationParametersCLIParams.MSGF_FRAGMENTATION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_PROTOCOL.id) + " " + IdentificationParametersCLIParams.MSGF_PROTOCOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_TERMINI.id) + " " + IdentificationParametersCLIParams.MSGF_TERMINI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.MSGF_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.MSGF_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_PTMS.id) + " " + IdentificationParametersCLIParams.MSGF_PTMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_NUM_MATCHES.id) + " " + IdentificationParametersCLIParams.MSGF_NUM_MATCHES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_ADDITIONAL.id) + " " + IdentificationParametersCLIParams.MSGF_ADDITIONAL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MSGF_TASKS.id) + " " + IdentificationParametersCLIParams.MSGF_TASKS.description + "\n";

        output += "\n\nOMSSA advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_LOW_INTENSITY.id) + " " + IdentificationParametersCLIParams.OMSSA_LOW_INTENSITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_HIGH_INTENSITY.id) + " " + IdentificationParametersCLIParams.OMSSA_HIGH_INTENSITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_INTENSITY_INCREMENT.id) + " " + IdentificationParametersCLIParams.OMSSA_INTENSITY_INCREMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MIN_PEAKS.id) + " " + IdentificationParametersCLIParams.OMSSA_MIN_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_REMOVE_PREC.id) + " " + IdentificationParametersCLIParams.OMSSA_REMOVE_PREC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_ESTIMATE_CHARGE.id) + " " + IdentificationParametersCLIParams.OMSSA_ESTIMATE_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_PLUS_ONE.id) + " " + IdentificationParametersCLIParams.OMSSA_PLUS_ONE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MAX_FRACTION.id) + " " + IdentificationParametersCLIParams.OMSSA_MAX_FRACTION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_PREC_PER_SPECTRUM.id) + " " + IdentificationParametersCLIParams.OMSSA_PREC_PER_SPECTRUM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_SCALE_PREC.id) + " " + IdentificationParametersCLIParams.OMSSA_SCALE_PREC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_SEQUENCES_IN_MEMORY.id) + " " + IdentificationParametersCLIParams.OMSSA_SEQUENCES_IN_MEMORY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_METHIONINE.id) + " " + IdentificationParametersCLIParams.OMSSA_METHIONINE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_NEUTRON.id) + " " + IdentificationParametersCLIParams.OMSSA_NEUTRON.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_SINGLE_WINDOW_WIDTH.id) + " " + IdentificationParametersCLIParams.OMSSA_SINGLE_WINDOW_WIDTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_DOUBLE_WINDOW_WIDTH.id) + " " + IdentificationParametersCLIParams.OMSSA_DOUBLE_WINDOW_WIDTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_SINGLE_WINDOW_PEAKS.id) + " " + IdentificationParametersCLIParams.OMSSA_SINGLE_WINDOW_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_DOUBLE_WINDOW_PEAKS.id) + " " + IdentificationParametersCLIParams.OMSSA_DOUBLE_WINDOW_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MIN_ANNOTATED_INTENSE_PEAKS.id) + " " + IdentificationParametersCLIParams.OMSSA_MIN_ANNOTATED_INTENSE_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MIN_ANNOTATED_PEAKS.id) + " " + IdentificationParametersCLIParams.OMSSA_MIN_ANNOTATED_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MAX_LADDERS.id) + " " + IdentificationParametersCLIParams.OMSSA_MAX_LADDERS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MAX_FRAG_CHARGE.id) + " " + IdentificationParametersCLIParams.OMSSA_MAX_FRAG_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_POSITIVE_IONS.id) + " " + IdentificationParametersCLIParams.OMSSA_POSITIVE_IONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_FORWARD_IONS.id) + " " + IdentificationParametersCLIParams.OMSSA_FORWARD_IONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_REWIND_IONS.id) + " " + IdentificationParametersCLIParams.OMSSA_REWIND_IONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MAX_FRAG_SERIES.id) + " " + IdentificationParametersCLIParams.OMSSA_MAX_FRAG_SERIES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_CORRELATION_CORRECTION.id) + " " + IdentificationParametersCLIParams.OMSSA_CORRELATION_CORRECTION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_CONSECUTIVE_ION_PROBABILITY.id) + " " + IdentificationParametersCLIParams.OMSSA_CONSECUTIVE_ION_PROBABILITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_HITLIST_LENGTH_CHARGE.id) + " " + IdentificationParametersCLIParams.OMSSA_HITLIST_LENGTH_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_ITERATIVE_SEQUENCE_EVALUE.id) + " " + IdentificationParametersCLIParams.OMSSA_ITERATIVE_SEQUENCE_EVALUE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_ITERATIVE_SPECTRUM_EVALUE.id) + " " + IdentificationParametersCLIParams.OMSSA_ITERATIVE_SPECTRUM_EVALUE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_ITERATIVE_REPLACE_EVALUE.id) + " " + IdentificationParametersCLIParams.OMSSA_ITERATIVE_REPLACE_EVALUE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.OMSSA_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.OMSSA_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_MAX_EVALUE.id) + " " + IdentificationParametersCLIParams.OMSSA_MAX_EVALUE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_HITLIST_LENGTH.id) + " " + IdentificationParametersCLIParams.OMSSA_HITLIST_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OMSSA_FORMAT.id) + " " + IdentificationParametersCLIParams.OMSSA_FORMAT.description + "\n";

        output += "\n\nComet advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MIN_PEAKS.id) + " " + IdentificationParametersCLIParams.COMET_MIN_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MIN_PEAK_INTENSITY.id) + " " + IdentificationParametersCLIParams.COMET_MIN_PEAK_INTENSITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_REMOVE_PRECURSOR.id) + " " + IdentificationParametersCLIParams.COMET_REMOVE_PRECURSOR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_REMOVE_PRECURSOR_TOLERANCE.id) + " " + IdentificationParametersCLIParams.COMET_REMOVE_PRECURSOR_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_CLEAR_MZ_RANGE_LOWER.id) + " " + IdentificationParametersCLIParams.COMET_CLEAR_MZ_RANGE_LOWER.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_CLEAR_MZ_RANGE_UPPER.id) + " " + IdentificationParametersCLIParams.COMET_CLEAR_MZ_RANGE_UPPER.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_ENZYME_TYPE.id) + " " + IdentificationParametersCLIParams.COMET_ENZYME_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_ISOTOPE_CORRECTION.id) + " " + IdentificationParametersCLIParams.COMET_ISOTOPE_CORRECTION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MIN_PREC_MASS.id) + " " + IdentificationParametersCLIParams.COMET_MIN_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MAX_PREC_MASS.id) + " " + IdentificationParametersCLIParams.COMET_MAX_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.COMET_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.COMET_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_MAX_FRAGMENT_CHARGE.id) + " " + IdentificationParametersCLIParams.COMET_MAX_FRAGMENT_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_REMOVE_METH.id) + " " + IdentificationParametersCLIParams.COMET_REMOVE_METH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_BATCH_SIZE.id) + " " + IdentificationParametersCLIParams.COMET_BATCH_SIZE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_PTMS.id) + " " + IdentificationParametersCLIParams.COMET_PTMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_REQ_PTMS.id) + " " + IdentificationParametersCLIParams.COMET_REQ_PTMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_THEORETICAL_FRAGMENT_IONS.id) + " " + IdentificationParametersCLIParams.COMET_THEORETICAL_FRAGMENT_IONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_FRAGMENT_BIN_OFFSET.id) + " " + IdentificationParametersCLIParams.COMET_FRAGMENT_BIN_OFFSET.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_NUM_MATCHES.id) + " " + IdentificationParametersCLIParams.COMET_NUM_MATCHES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.COMET_OUTPUT.id) + " " + IdentificationParametersCLIParams.COMET_OUTPUT.description + "\n";

        output += "\n\nTide advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.TIDE_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.TIDE_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MIN_PREC_MASS.id) + " " + IdentificationParametersCLIParams.TIDE_MIN_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MAX_PREC_MASS.id) + " " + IdentificationParametersCLIParams.TIDE_MAX_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MONOISOTOPIC.id) + " " + IdentificationParametersCLIParams.TIDE_MONOISOTOPIC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_CLIP_N_TERM.id) + " " + IdentificationParametersCLIParams.TIDE_CLIP_N_TERM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_PTMS.id) + " " + IdentificationParametersCLIParams.TIDE_PTMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_PTMS_PER_TYPE.id) + " " + IdentificationParametersCLIParams.TIDE_PTMS_PER_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_DIGESTION_TYPE.id) + " " + IdentificationParametersCLIParams.TIDE_DIGESTION_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_PRINT_PEPTIDES.id) + " " + IdentificationParametersCLIParams.TIDE_PRINT_PEPTIDES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_DECOY_FORMAT.id) + " " + IdentificationParametersCLIParams.TIDE_DECOY_FORMAT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_KEEP_TERM_AA.id) + " " + IdentificationParametersCLIParams.TIDE_KEEP_TERM_AA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_DECOY_SEED.id) + " " + IdentificationParametersCLIParams.TIDE_DECOY_SEED.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_REMOVE_TEMP.id) + " " + IdentificationParametersCLIParams.TIDE_REMOVE_TEMP.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_COMPUTE_P.id) + " " + IdentificationParametersCLIParams.TIDE_COMPUTE_P.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_COMPUTE_SP.id) + " " + IdentificationParametersCLIParams.TIDE_COMPUTE_SP.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MIN_SPECTRUM_MZ.id) + " " + IdentificationParametersCLIParams.TIDE_MIN_SPECTRUM_MZ.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MAX_SPECTRUM_MZ.id) + " " + IdentificationParametersCLIParams.TIDE_MAX_SPECTRUM_MZ.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MIN_SPECTRUM_PEAKS.id) + " " + IdentificationParametersCLIParams.TIDE_MIN_SPECTRUM_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_SPECTRUM_CHARGES.id) + " " + IdentificationParametersCLIParams.TIDE_SPECTRUM_CHARGES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_REMOVE_PREC.id) + " " + IdentificationParametersCLIParams.TIDE_REMOVE_PREC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_REMOVE_PREC_TOL.id) + " " + IdentificationParametersCLIParams.TIDE_REMOVE_PREC_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_USE_FLANKING.id) + " " + IdentificationParametersCLIParams.TIDE_USE_FLANKING.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_USE_NEUTRAL_LOSSES.id) + " " + IdentificationParametersCLIParams.TIDE_USE_NEUTRAL_LOSSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MZ_BIN_WIDTH.id) + " " + IdentificationParametersCLIParams.TIDE_MZ_BIN_WIDTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MZ_BIN_OFFSET.id) + " " + IdentificationParametersCLIParams.TIDE_MZ_BIN_OFFSET.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_MAX_PSMS.id) + " " + IdentificationParametersCLIParams.TIDE_MAX_PSMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_EXPORT_TEXT.id) + " " + IdentificationParametersCLIParams.TIDE_EXPORT_TEXT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_EXPORT_SQT.id) + " " + IdentificationParametersCLIParams.TIDE_EXPORT_SQT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_EXPORT_PEPXML.id) + " " + IdentificationParametersCLIParams.TIDE_EXPORT_PEPXML.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_EXPORT_MZID.id) + " " + IdentificationParametersCLIParams.TIDE_EXPORT_MZID.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_EXPORT_PIN.id) + " " + IdentificationParametersCLIParams.TIDE_EXPORT_PIN.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_OUTPUT_FOLDER.id) + " " + IdentificationParametersCLIParams.TIDE_OUTPUT_FOLDER.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_VERBOSITY.id) + " " + IdentificationParametersCLIParams.TIDE_VERBOSITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_PROGRESS_INDICATOR.id) + " " + IdentificationParametersCLIParams.TIDE_PROGRESS_INDICATOR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_CONCAT.id) + " " + IdentificationParametersCLIParams.TIDE_CONCAT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.TIDE_STORE_SPECTRA.id) + " " + IdentificationParametersCLIParams.TIDE_STORE_SPECTRA.description + "\n";

        output += "\n\nAndromeda advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_MAX_PEPTIDE_MASS.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_MAX_PEPTIDE_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_MAX_COMBINATIONS.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_MAX_COMBINATIONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_TOP_PEAKS.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_TOP_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_TOP_PEAKS_WINDOW.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_TOP_PEAKS_WINDOW.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_INCL_WATER.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_INCL_WATER.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_INCL_AMMONIA.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_INCL_AMMONIA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_NEUTRAL_LOSSES.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_NEUTRAL_LOSSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_FRAGMENT_ALL.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_FRAGMENT_ALL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_EMP_CORRECTION.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_EMP_CORRECTION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_HIGHER_CHARGE.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_HIGHER_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_FRAG_METHOD.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_FRAG_METHOD.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_MAX_MODS.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_MAX_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_EQUAL_IL.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_EQUAL_IL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_MAX_PSMS.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_MAX_PSMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANDROMEDA_DECOY_MODE.id) + " " + IdentificationParametersCLIParams.ANDROMEDA_DECOY_MODE.description + "\n";
        
        output += "\n\nMetaMorpheus advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MIN_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MIN_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MAX_PEP_LENGTH.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MAX_PEP_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_SEARCH_TYPE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_SEARCH_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_NUM_PARTITIONS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_NUM_PARTITIONS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_DISSOCIATION_TYPE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_DISSOCIATION_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MAX_MODS_FOR_PEPTIDE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MAX_MODS_FOR_PEPTIDE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_INITIATOR_METHIONINE_BEHAVIOR.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_INITIATOR_METHIONINE_BEHAVIOR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_SCORE_CUTOFF.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_SCORE_CUTOFF.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_USE_DELTA_SCORE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_USE_DELTA_SCORE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_FRAGMENTATION_TERMINUS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_FRAGMENTATION_TERMINUS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MAX_FRAGMENTATION_SIZE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MAX_FRAGMENTATION_SIZE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MASS_DIFF_ACCEPTOR_TYPE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MASS_DIFF_ACCEPTOR_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_WRITE_MZID.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_WRITE_MZID.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_WRITE_PEPXML.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_WRITE_PEPXML.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_USE_PROVIDED_PRECURSOR.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_USE_PROVIDED_PRECURSOR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_DO_PREC_DECONVOLUTION.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_DO_PREC_DECONVOLUTION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_DECONVOLUTION_INT_RATIO.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_DECONVOLUTION_INT_RATIO.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_DECONVOLUTION_MASS_TOL.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_DECONVOLUTION_MASS_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_DECONVOLUTION_MASS_TOL_TYPE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_DECONVOLUTION_MASS_TOL_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_TRIM_MS1_PEAKS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_TRIM_MS1_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_TRIM_MSMS_PEAKS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_TRIM_MSMS_PEAKS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_NUM_PEAKS_PER_WINDOWS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_NUM_PEAKS_PER_WINDOWS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MIN_ALLOWED_INT_RATIO_TO_BASE_PEAK.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MIN_ALLOWED_INT_RATIO_TO_BASE_PEAK.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_WINDOW_WITH_THOMPSON.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_WINDOW_WITH_THOMPSON.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_NUM_WINDOWS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_NUM_WINDOWS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_NORMALIZE_PEAKS_ACROSS_ALL_WINDOWS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_NORMALIZE_PEAKS_ACROSS_ALL_WINDOWS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MOD_PEPTIDES_ARE_DIFFERENT.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MOD_PEPTIDES_ARE_DIFFERENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_NO_ONE_HIT_WONDERS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_NO_ONE_HIT_WONDERS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_SEARCH_TARGET.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_SEARCH_TARGET.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_DECOY_TYPE.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_DECOY_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MAX_MOD_ISOFORMS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MAX_MOD_ISOFORMS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MIN_VARIANT_DEPTH.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MIN_VARIANT_DEPTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.META_MORPHEUS_MAX_HETROZYGOUS_VARIANTS.id) + " " + IdentificationParametersCLIParams.META_MORPHEUS_MAX_HETROZYGOUS_VARIANTS.description + "\n";
        
        output += "\n\nNovor:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.NOVOR_FRAGMENTATION.id) + " " + IdentificationParametersCLIParams.NOVOR_FRAGMENTATION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.NOVOR_MASS_ANALYZER.id) + " " + IdentificationParametersCLIParams.NOVOR_MASS_ANALYZER.description + "\n";

        output += "\n\nPNovo+:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_ACTIVATION_TYPE.id) + " " + IdentificationParametersCLIParams.PNOVO_ACTIVATION_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_LOWER_PRECURSOR_MASS.id) + " " + IdentificationParametersCLIParams.PNOVO_LOWER_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_UPPER_PRECURSOR_MASS.id) + " " + IdentificationParametersCLIParams.PNOVO_UPPER_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_NUMBER_OF_PEPTIDES.id) + " " + IdentificationParametersCLIParams.PNOVO_NUMBER_OF_PEPTIDES.description + "\n";

        output += "\n\nPepNovo+:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.id) + " " + IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_ESTIMATE_CHARGE.id) + " " + IdentificationParametersCLIParams.PEPNOVO_ESTIMATE_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.id) + " " + IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.id) + " " + IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.id) + " " + IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.description + "\n";               
              
        output += "\n\nDirectTag advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_TAG_LENGTH.id) + " " + IdentificationParametersCLIParams.DIRECTAG_TAG_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_DYNAMIC_MODS.id) + " " + IdentificationParametersCLIParams.DIRECTAG_MAX_DYNAMIC_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_NUM_CHARGE_STATES.id) + " " + IdentificationParametersCLIParams.DIRECTAG_NUM_CHARGE_STATES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_DUPLICATE_SPECTRA.id) + " " + IdentificationParametersCLIParams.DIRECTAG_DUPLICATE_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_ISOTOPE_MZ_TOLERANCE.id) + " " + IdentificationParametersCLIParams.DIRECTAG_ISOTOPE_MZ_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_DEISOTOPING_MODE.id) + " " + IdentificationParametersCLIParams.DIRECTAG_DEISOTOPING_MODE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_NUM_INTENSITY_CLASSES.id) + " " + IdentificationParametersCLIParams.DIRECTAG_NUM_INTENSITY_CLASSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_OUTPUT_SUFFIX.id) + " " + IdentificationParametersCLIParams.DIRECTAG_OUTPUT_SUFFIX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_PEAK_COUNT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_MAX_PEAK_COUNT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_TAG_COUNT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_MAX_TAG_COUNT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_TIC_CUTOFF_PERCENTAGE.id) + " " + IdentificationParametersCLIParams.DIRECTAG_TIC_CUTOFF_PERCENTAGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_MZ_TOLERANCE.id) + " " + IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_MZ_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_PRECURSOR_ADJUSTMENT_STEP.id) + " " + IdentificationParametersCLIParams.DIRECTAG_PRECURSOR_ADJUSTMENT_STEP.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MIN_PRECURSOR_ADJUSTMENT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_MIN_PRECURSOR_ADJUSTMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_PRECURSOR_ADJUSTMENT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_MAX_PRECURSOR_ADJUSTMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_INTENSITY_SCORE_WEIGHT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_INTENSITY_SCORE_WEIGHT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MZ_FIDELITY_SCORE_WEIGHT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_MZ_FIDELITY_SCORE_WEIGHT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_SCORE_WEIGHT.id) + " " + IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_SCORE_WEIGHT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.id) + " " + IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_USE_CHARGE_STATE_FROM_MS.id) + " " + IdentificationParametersCLIParams.DIRECTAG_USE_CHARGE_STATE_FROM_MS.description + "\n";        
        
        output += "\n\nSpectrum Annotation:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_LEVEL.id) + " " + IdentificationParametersCLIParams.ANNOTATION_LEVEL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_LEVEL_TYPE.id) + " " + IdentificationParametersCLIParams.ANNOTATION_LEVEL_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_MZ_TOLERANCE.id) + " " + IdentificationParametersCLIParams.ANNOTATION_MZ_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_HIGH_RESOLUTION.id) + " " + IdentificationParametersCLIParams.ANNOTATION_HIGH_RESOLUTION.description + "\n";

        output += "\n\nSequence Matching:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SEQUENCE_MATCHING_TYPE.id) + " " + IdentificationParametersCLIParams.SEQUENCE_MATCHING_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SEQUENCE_MATCHING_X.id) + " " + IdentificationParametersCLIParams.SEQUENCE_MATCHING_X.description + "\n";

        output += "\n\nImport Filters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MIN.id) + " " + IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MIN.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MAX.id) + " " + IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MAX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_MC_MIN.id) + " " + IdentificationParametersCLIParams.IMPORT_MC_MIN.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_MC_MAX.id) + " " + IdentificationParametersCLIParams.IMPORT_MC_MAX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ.id) + " " + IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ_PPM.id) + " " + IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ_PPM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.EXCLUDE_UNKNOWN_PTMs.id) + " " + IdentificationParametersCLIParams.EXCLUDE_UNKNOWN_PTMs.description + "\n";

        output += "\n\nPTM Localization:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_SCORE.id) + " " + IdentificationParametersCLIParams.PTM_SCORE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_THRESHOLD.id) + " " + IdentificationParametersCLIParams.PTM_THRESHOLD.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SCORE_NEUTRAL_LOSSES.id) + " " + IdentificationParametersCLIParams.SCORE_NEUTRAL_LOSSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_SEQUENCE_MATCHING_TYPE.id) + " " + IdentificationParametersCLIParams.PTM_SEQUENCE_MATCHING_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_ALIGNMENT.id) + " " + IdentificationParametersCLIParams.PTM_ALIGNMENT.description + "\n";

        output += "\n\nGene Annotation:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.USE_GENE_MAPPING.id) + " " + IdentificationParametersCLIParams.USE_GENE_MAPPING.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.UPDATE_GENE_MAPPING.id) + " " + IdentificationParametersCLIParams.UPDATE_GENE_MAPPING.description + "\n";

        output += "\n\nProtein Inference:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SIMPLIFY_GOUPS.id) + " " + IdentificationParametersCLIParams.SIMPLIFY_GOUPS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SIMPLIFY_GOUPS_ENZYMATICITY.id) + " " + IdentificationParametersCLIParams.SIMPLIFY_GOUPS_ENZYMATICITY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SIMPLIFY_GOUPS_EVIDENCE.id) + " " + IdentificationParametersCLIParams.SIMPLIFY_GOUPS_EVIDENCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SIMPLIFY_GOUPS_CONFIDENCE.id) + " " + IdentificationParametersCLIParams.SIMPLIFY_GOUPS_CONFIDENCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SIMPLIFY_GOUPS_CONFIDENCE_THRESHOLD.id) + " " + IdentificationParametersCLIParams.SIMPLIFY_GOUPS_CONFIDENCE_THRESHOLD.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SIMPLIFY_GOUPS_VARIANT.id) + " " + IdentificationParametersCLIParams.SIMPLIFY_GOUPS_VARIANT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PROTEIN_INFERENCE_MODIFICATIONS.id) + " " + IdentificationParametersCLIParams.PROTEIN_INFERENCE_MODIFICATIONS.description + "\n";

        output += "\n\nValidation Levels:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PSM_FDR.id) + " " + IdentificationParametersCLIParams.PSM_FDR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPTIDE_FDR.id) + " " + IdentificationParametersCLIParams.PEPTIDE_FDR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PROTEIN_FDR.id) + " " + IdentificationParametersCLIParams.PROTEIN_FDR.description + "\n";

        output += "\n\nFraction Analysis:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PROTEIN_FRACTION_MW_CONFIDENCE.id) + " " + IdentificationParametersCLIParams.PROTEIN_FRACTION_MW_CONFIDENCE.description + "\n";

        output += "\n\nDatabase Processing:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FASTA_TARGET_DECOY.id) + " " + IdentificationParametersCLIParams.FASTA_TARGET_DECOY.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FASTA_DECOY_TAG.id) + " " + IdentificationParametersCLIParams.FASTA_DECOY_TAG.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FASTA_DECOY_SUFFIX.id) + " " + IdentificationParametersCLIParams.FASTA_DECOY_SUFFIX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FASTA_DECOY_FILE_TAG.id) + " " + IdentificationParametersCLIParams.FASTA_DECOY_FILE_TAG.description + "\n";
        
//        output += "\n\nQuality Control:\n\n";
//        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_LEVEL.id) + IdentificationParametersCLIParams.ANNOTATION_LEVEL.description + "\n";
        output += "\n\nHelp:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MODS.id) + IdentificationParametersCLIParams.MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.USAGE.id) + IdentificationParametersCLIParams.USAGE.description + "\n";

        return output;
    }
}
