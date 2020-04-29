package eu.isas.peptideshaker.utils;

import java.io.InputStream;

/**
 * This class provides the PeptideShaker version number.
 *
 * @author Harald Barsnes
 */
public class Properties {

    /**
     * Creates a new empty Properties object.
     */
    public Properties() {
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of the PeptideShaker
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("peptide-shaker.properties");
            p.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return p.getProperty("peptide-shaker.version");
    }
}
