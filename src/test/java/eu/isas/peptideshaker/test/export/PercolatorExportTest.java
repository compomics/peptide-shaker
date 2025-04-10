package eu.isas.peptideshaker.test.export;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import static eu.isas.peptideshaker.utils.PercolatorUtils.getSequenceWithModifications;
import junit.framework.TestCase;
import org.junit.Assert;

/**
 * Tests the export for Percolator.
 *
 * @author Marc Vaudel
 */
public class PercolatorExportTest extends TestCase {

    /**
     * Tests the annotation of modifications on peptides.
     */
    public void testPeptideAnnotation() {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SequenceMatchingParameters sequenceMatchingParameters = SequenceMatchingParameters.getDefaultSequenceMatching();
        
        // Variable mods and fixed at N and C termini and within the sequence

        ModificationParameters modificationParameters = new ModificationParameters();
        modificationParameters.addFixedModification(modificationFactory.getModification("Carbamidomethylation of C"));

        String sequence = "KTESTMYCPEPTIDEC";
        String expectedResult = "K[1]TESTM[35]YC[4]PEPTIDEC[4]";

        ModificationMatch[] variableModifications = new ModificationMatch[2];

        variableModifications[0] = new ModificationMatch("Acetylation of peptide N-term", 0);
        variableModifications[1] = new ModificationMatch("Oxidation of M", 6);

        Peptide peptide = new Peptide(sequence, variableModifications);

        String percolatorExport = getSequenceWithModifications(peptide, modificationParameters, null, sequenceMatchingParameters, modificationFactory);

        Assert.assertTrue(percolatorExport.equals(expectedResult));
        
        // Variable mods and fixed at N and C termini and within the sequence

        sequence = "CTESTMYCPEPTIDEK";
        expectedResult = "C[4]TESTM[35]YC[4]PEPTIDEK[1]";

        variableModifications = new ModificationMatch[2];

        variableModifications[0] = new ModificationMatch("Acetylation of K", 16);
        variableModifications[1] = new ModificationMatch("Oxidation of M", 6);

        peptide = new Peptide(sequence, variableModifications);

        percolatorExport = getSequenceWithModifications(peptide, modificationParameters, null, sequenceMatchingParameters, modificationFactory);

        Assert.assertTrue(percolatorExport.equals(expectedResult));
        
        // Variable within the sequence

        sequence = "TESTMYCPEPKTIDE";
        expectedResult = "TESTM[35]YC[4]PEPK[1]TIDE";

        variableModifications = new ModificationMatch[2];

        variableModifications[0] = new ModificationMatch("Acetylation of K", 11);
        variableModifications[1] = new ModificationMatch("Oxidation of M", 5);

        peptide = new Peptide(sequence, variableModifications);

        percolatorExport = getSequenceWithModifications(peptide, modificationParameters, null, sequenceMatchingParameters, modificationFactory);

        Assert.assertTrue(percolatorExport.equals(expectedResult));

    }
}
