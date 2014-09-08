package eu.isas.peptideshaker.test;

import eu.isas.peptideshaker.pride.PrideWebServiceTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This class represents the full suite of test for the PeptideShaker project.
 *
 * @author Harald Barsnes
 */
public class FullSuite extends TestCase {

    public FullSuite() {
        super("Full test suite for the PeptideShaker project.");
    }

    public static Test suite() {
        TestSuite ts = new TestSuite("Test suite for the PeptideShaker project.");
        ts.addTest(new TestSuite(PrideWebServiceTest.class));
        return ts;
    }
}
