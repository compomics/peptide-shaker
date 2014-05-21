package eu.isas.peptideshaker.preferences;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import com.compomics.util.pride.prideobjects.*;
import java.util.HashMap;

/**
 * This class contains the details about a project.
 *
 * @author Marc Vaudel
 */
public class ProjectDetails implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -2635206350852992221L;
    /**
     * List of the identification files loaded.
     */
    private ArrayList<File> identificationFiles = new ArrayList<File>();
    /**
     * Map of the search engine versions used to generate the identification
     * files. Key: identification file name, element: the search engine name and
     * version.
     *
     * @deprecated use identificationAlgorithmVersion instead
     */
    private HashMap<String, String> identificationFileSearchEngineVersion = new HashMap<String, String>();
    /**
     * Map of the identification algorithms versions used to generate the
     * identification files. Key: identification file name, element: the
     * identification algorithm version.
     */
    private HashMap<String, String> identificationAlgorithmsVersion = new HashMap<String, String>();
    /**
     * Map of the identification algorithms used to generate the identification
     * files. Key: identification file name, element: the identification
     * algorithm version index.
     */
    private HashMap<String, Integer> identificationAdvocate = new HashMap<String, Integer>();
    /**
     * List of the spectrum files.
     */
    private HashMap<String, File> spectrumFiles = new HashMap<String, File>();
    /**
     * When the project was created.
     */
    private Date creationDate;
    /**
     * The report created during the loading of the tool
     */
    private String report;
    /**
     * The PRIDE experiment title.
     */
    private String prideExperimentTitle;
    /**
     * The PRIDE experiment label.
     */
    private String prideExperimentLabel;
    /**
     * The PRIDE experiment project title.
     */
    private String prideExperimentProjectTitle;
    /**
     * The PRIDE experiment description.
     */
    private String prideExperimentDescription;
    /**
     * The PRIDE reference group.
     */
    private ReferenceGroup prideReferenceGroup;
    /**
     * The PRIDE contact group.
     */
    private ContactGroup prideContactGroup;
    /**
     * The PRIDE sample details.
     */
    private Sample prideSample;
    /**
     * The PRIDE protocol details.
     */
    private Protocol prideProtocol;
    /**
     * The PRIDE instrument details.
     */
    private Instrument prideInstrument;
    /**
     * The PRIDE output folder.
     */
    private String prideOutputFolder;
    /**
     * The PeptideShaker version used to create the project.
     */
    private String peptideShakerVersion;
    /**
     * The first name of the contact for the mzIdentML dataset.
     */
    private String contactFirstName;
    /**
     * The last name of the contact for the mzIdentML dataset.
     */
    private String contactLastName;
    /**
     * The e-mail of the contact for the mzIdentML dataset.
     */
    private String contactEmail;
    /**
     * The URL of the contact for the mzIdentML dataset.
     */
    private String contactUrl;
    /**
     * The address of the contact for the mzIdentML dataset.
     */
    private String contactAddress;
    /**
     * The name of the organization for the mzIdentML dataset.
     */
    private String organizationName;
    /**
     * The e-mail of the organization for the mzIdentML dataset.
     */
    private String organizationEmail;
    /**
     * The URL of the organization for the mzIdentML dataset.
     */
    private String organizationUrl;
    /**
     * The address of the organization for the mzIdentML dataset.
     */
    private String organizationAddress;
    /**
     * The user advocates mapping of this project.
     */
    private HashMap<Integer, Advocate> userAdvocateMapping;

    /**
     * Constructor.
     */
    public ProjectDetails() {
    }

    /**
     * Getter for all identification files loaded.
     *
     * @return all identification files loaded
     */
    public ArrayList<File> getIdentificationFiles() {
        return identificationFiles;
    }

    /**
     * Adds an identification file to the list of loaded identification files
     *
     * @param identificationFile the identification file loaded
     */
    public void addIdentificationFiles(File identificationFile) {
        identificationFiles.add(identificationFile);
    }

    /**
     * Attaches a spectrum file to the project. Warning: any previous file with
     * the same name will be silently ignored.
     *
     * @param spectrumFile the spectrum file to add
     */
    public void addSpectrumFile(File spectrumFile) {
        String fileName = Util.getFileName(spectrumFile.getAbsolutePath());
        spectrumFiles.put(fileName, spectrumFile);
    }

    /**
     * Returns the file corresponding to the given name.
     *
     * @param fileName the name of the desired file
     * @return the corresponding file, null if not found.
     */
    public File getSpectrumFile(String fileName) {
        // Compatibility check
        if (spectrumFiles == null) {
            spectrumFiles = new HashMap<String, File>();
        }
        return spectrumFiles.get(fileName);
    }

    /**
     * Getter for the creation date of the project.
     *
     * @return the creation date of the project
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Setter the creation date of the project.
     *
     * @param creationDate the creation date of the project
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns the PeptideShaker version used to create the project.
     *
     * @return the PeptideShaker version used to create the project
     */
    public String getPeptideShakerVersion() {
        if (peptideShakerVersion == null) {
            peptideShakerVersion = "unknown";
        }
        return peptideShakerVersion;
    }

    /**
     * Sets the PeptideShaker version used to create the project.
     *
     * @param peptideShakerVersion the PeptideShaker version used to create the
     * project
     */
    public void setPeptideShakerVersion(String peptideShakerVersion) {
        this.peptideShakerVersion = peptideShakerVersion;
    }

    /**
     * Returns the report created during the loading of the project.
     *
     * @return the report created during the loading of the project
     */
    public String getReport() {

        if (report == null) {
            return "(report not saved)";
        }

        return report;
    }

    /**
     * Set the report created during the loading of the project.
     *
     * @param report the report to set
     */
    public void setReport(String report) {
        this.report = report;
    }

    /**
     * Returns the PRIDE experiment title.
     *
     * @return the prideExperimenttitle
     */
    public String getPrideExperimentTitle() {
        return prideExperimentTitle;
    }

    /**
     * Sets the PRIDE experiment title.
     *
     * @param prideExperimentTitle the prideExperimentTitle to set
     */
    public void setPrideExperimentTitle(String prideExperimentTitle) {
        this.prideExperimentTitle = prideExperimentTitle;
    }

    /**
     * Returns the PRIDE experiment label.
     *
     * @return the prideExperimentLabel
     */
    public String getPrideExperimentLabel() {
        return prideExperimentLabel;
    }

    /**
     * Sets the PRIDE experiment label.
     *
     * @param prideExperimentLabel the prideExperimentLabel to set
     */
    public void setPrideExperimentLabel(String prideExperimentLabel) {
        this.prideExperimentLabel = prideExperimentLabel;
    }

    /**
     * Returns the PRIDE experiment project title.
     *
     * @return the prideExperimentProjectTitle
     */
    public String getPrideExperimentProjectTitle() {
        return prideExperimentProjectTitle;
    }

    /**
     * Set the PRIDE experiment project title.
     *
     * @param prideExperimentProjectTitle the prideExperimentProjectTitle to set
     */
    public void setPrideExperimentProjectTitle(String prideExperimentProjectTitle) {
        this.prideExperimentProjectTitle = prideExperimentProjectTitle;
    }

    /**
     * Returns the PRIDE experiment project description.
     *
     * @return the prideExperimentDescription
     */
    public String getPrideExperimentDescription() {
        return prideExperimentDescription;
    }

    /**
     * Set the PRIDE experiment project description.
     *
     * @param prideExperimentDescription the prideExperimentDescription to set
     */
    public void setPrideExperimentDescription(String prideExperimentDescription) {
        this.prideExperimentDescription = prideExperimentDescription;
    }

    /**
     * Returns the PRIDE reference group.
     *
     * @return the prideReferenceGroup
     */
    public ReferenceGroup getPrideReferenceGroup() {
        return prideReferenceGroup;
    }

    /**
     * Set the PRIDE reference group.
     *
     * @param prideReferenceGroup the prideReferenceGroup to set
     */
    public void setPrideReferenceGroup(ReferenceGroup prideReferenceGroup) {
        this.prideReferenceGroup = prideReferenceGroup;
    }

    /**
     * Returns the PRIDE contact group.
     *
     * @return the prideContactGroup
     */
    public ContactGroup getPrideContactGroup() {
        return prideContactGroup;
    }

    /**
     * Set the PRIDE contact group.
     *
     * @param prideContactGroup the prideContactGroup to set
     */
    public void setPrideContactGroup(ContactGroup prideContactGroup) {
        this.prideContactGroup = prideContactGroup;
    }

    /**
     * Returns the PRIDE sample.
     *
     * @return the prideSample
     */
    public Sample getPrideSample() {
        return prideSample;
    }

    /**
     * Set the PRIDE sample.
     *
     * @param prideSample the prideSample to set
     */
    public void setPrideSample(Sample prideSample) {
        this.prideSample = prideSample;
    }

    /**
     * Returns the PRIDE protocol.
     *
     * @return the prideProtocol
     */
    public Protocol getPrideProtocol() {
        return prideProtocol;
    }

    /**
     * Set the PRIDE protocol.
     *
     * @param prideProtocol the prideProtocol to set
     */
    public void setPrideProtocol(Protocol prideProtocol) {
        this.prideProtocol = prideProtocol;
    }

    /**
     * Returns the PRIDE instrument.
     *
     * @return the prideInstrument
     */
    public Instrument getPrideInstrument() {
        return prideInstrument;
    }

    /**
     * Set the the PRIDE instrument.
     *
     * @param prideInstrument the prideInstrument to set
     */
    public void setPrideInstrument(Instrument prideInstrument) {
        this.prideInstrument = prideInstrument;
    }

    /**
     * Returns the PRIDE output folder.
     *
     * @return the prideOutputFolder
     */
    public String getPrideOutputFolder() {
        return prideOutputFolder;
    }

    /**
     * Set the PRIDE output folder.
     *
     * @param prideOutputFolder the prideOutputFolder to set
     */
    public void setPrideOutputFolder(String prideOutputFolder) {
        this.prideOutputFolder = prideOutputFolder;
    }

    /**
     * Returns a list of identification algorithms used based on the
     * identification files of the project.
     *
     * @return a list of identification algorithms indexed by the static field
     * of the Advocate class
     */
    public ArrayList<Integer> getIdentificationAlgorithms() {
        if (identificationAdvocate == null) {
            backwardCompatibilityFix();
        }
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (Integer advocate : identificationAdvocate.values()) {
            if (!result.contains(advocate)) {
                result.add(advocate);
            }
        }
        return result;
    }

    /**
     * Loads the identification files advocate and version from the id files.
     */
    public void backwardCompatibilityFix() {
        identificationAdvocate = new HashMap<String, Integer>();
        identificationAlgorithmsVersion = new HashMap<String, String>();
        IdfileReaderFactory idFileReaderFactory = IdfileReaderFactory.getInstance();
        ArrayList<File> idFiles = identificationFiles;
        for (File idFile : idFiles) {
            String idFileName = Util.getFileName(idFile);
            Advocate advocate = null;
            try {
                IdfileReader idFileReader = idFileReaderFactory.getFileReader(idFile);
                advocate = Advocate.getAdvocate(idFileReader.getSoftware());
                identificationAlgorithmsVersion.put(idFileName, idFileReader.getSoftwareVersion());
            } catch (Exception e) {
                // File was moved, use the extension to map it
                if (idFileName.toLowerCase().endsWith("dat")) {
                    advocate = Advocate.mascot;
                } else if (idFileName.toLowerCase().endsWith("omx")) {
                    advocate = Advocate.omssa;
                } else if (idFileName.toLowerCase().endsWith("xml")) {
                    advocate = Advocate.xtandem;
                } else if (idFileName.toLowerCase().endsWith("mzid")) {
                    advocate = Advocate.msgf;
                } else if (idFileName.toLowerCase().endsWith("csv")) {
                    advocate = Advocate.msAmanda;
                }
            }
            if (advocate == null) {
                throw new IllegalArgumentException("The algorithm used to generate " + idFileName + " could not be recognized.");
            }
            identificationAdvocate.put(idFileName, advocate.getIndex());
        }
    }

    /**
     * Sets the identification algorithm version for a given identification
     * file.
     *
     * @param identificationFileName the name of the identification file
     * @param version the version of the algorithm used
     */
    public void setIdentificationAlgorithmVersion(String identificationFileName, String version) {
        if (identificationAlgorithmsVersion == null) {
            identificationAlgorithmsVersion = new HashMap<String, String>();
        }
        identificationAlgorithmsVersion.put(identificationFileName, version);
    }

    /**
     * Returns the version of the identification algorithm used for the
     * identification of the given file. Null if not found.
     *
     * @param identificationFileName the identification file name
     *
     * @return the version of the algorithm used
     */
    public String getIdentificationAlgorithmVersion(String identificationFileName) {
        if (identificationAdvocate == null) {
            backwardCompatibilityFix();
        }
        return identificationAlgorithmsVersion.get(identificationFileName);
    }

    /**
     * Sets the identification algorithm for a given identification file.
     *
     * @param identificationFileName the name of the identification file
     * @param advocateId the index of the advocate used for identification
     */
    public void setIdentificationAlgorithm(String identificationFileName, Integer advocateId) {
        if (identificationAdvocate == null) {
            identificationAdvocate = new HashMap<String, Integer>();
        }
        identificationAdvocate.put(identificationFileName, advocateId);
    }

    /**
     * Returns the identification algorithm used for the identification of the
     * given file. Null if not found.
     *
     * @param identificationFileName the identification file name
     *
     * @return the index of the algorithm used
     */
    public Integer getIdentificationAlgorithm(String identificationFileName) {
        if (identificationAdvocate == null) {
            backwardCompatibilityFix();
        }
        return identificationAdvocate.get(identificationFileName);
    }

    /**
     * Returns the different identification algorithm versions used in a map:
     * algorithm name -> versions.
     *
     * @return the different identification algorithm versions used
     */
    public HashMap<String, ArrayList<String>> getAlgorithmNameToVersionsMap() {
        HashMap<String, ArrayList<String>> algorithmNameToVersionMap = new HashMap<String, ArrayList<String>>();
        for (File idFile : identificationFiles) {
            String idFileName = Util.getFileName(idFile);
            Integer advocateId = getIdentificationAlgorithm(idFileName);
            Advocate advocate = Advocate.getAdvocate(advocateId);
            String name = advocate.getName();
            String version = getIdentificationAlgorithmVersion(name);
            ArrayList<String> algorithmVersions = algorithmNameToVersionMap.get(name);
            if (algorithmVersions == null) {
                algorithmVersions = new ArrayList<String>();
                algorithmVersions.add(version);
                algorithmNameToVersionMap.put(name, algorithmVersions);
            } else if (!algorithmVersions.contains(version)) {
                algorithmVersions.add(version);
            }
        }
        return algorithmNameToVersionMap;
    }

    /**
     * Returns the first name of the contact for the mzIdentML dataset.
     *
     * @return the contactFirstName
     */
    public String getContactFirstName() {
        return contactFirstName;
    }

    /**
     * Set the first name of the contact for the mzIdentML dataset.
     *
     * @param contactFirstName the contactFirstName to set
     */
    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    /**
     * Returns the last name of the contact for the mzIdentML dataset.
     *
     * @return the contactLastName
     */
    public String getContactLastName() {
        return contactLastName;
    }

    /**
     * Set the last name of the contact for the mzIdentML dataset.
     *
     * @param contactLastName the contactLastName to set
     */
    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    /**
     * Returns the e-mail of the contact for the mzIdentML dataset.
     *
     * @return the contactEmailName
     */
    public String getContactEmail() {
        return contactEmail;
    }

    /**
     * Set the e-mail of the contact for the mzIdentML dataset.
     *
     * @param contactEmail the contactEmailName to set
     */
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    /**
     * Returns the URL of the contact for the mzIdentML dataset.
     *
     * @return the contactUrl
     */
    public String getContactUrl() {
        return contactUrl;
    }

    /**
     * Set the first URL of the contact for the mzIdentML dataset.
     *
     * @param contactUrl the contactUrl to set
     */
    public void setContactUrl(String contactUrl) {
        this.contactUrl = contactUrl;
    }

    /**
     * Returns the address of the contact for the mzIdentML dataset.
     *
     * @return the contactAddress
     */
    public String getContactAddress() {
        return contactAddress;
    }

    /**
     * SEt the address of the contact for the mzIdentML dataset.
     *
     * @param contactAddress the contactAddress to set
     */
    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    /**
     * Returns the name of the organization for the mzIdentML dataset.
     *
     * @return the organizationName
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Set the name of the organization for the mzIdentML dataset.
     *
     * @param organizationName the organizationName to set
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    /**
     * Returns the e-mail of the organization for the mzIdentML dataset.
     *
     * @return the organizationEmail
     */
    public String getOrganizationEmail() {
        return organizationEmail;
    }

    /**
     * Set the name of the organization for the mzIdentML dataset.
     *
     * @param organizationEmail the organizationEmail to set
     */
    public void setOrganizationEmail(String organizationEmail) {
        this.organizationEmail = organizationEmail;
    }

    /**
     * Returns the URL of the organization for the mzIdentML dataset.
     *
     * @return the organizationUrl
     */
    public String getOrganizationUrl() {
        return organizationUrl;
    }

    /**
     * Set the URL of the organization for the mzIdentML dataset.
     *
     * @param organizationUrl the organizationUrl to set
     */
    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
    }

    /**
     * Returns the address of the organization for the mzIdentML dataset.
     *
     * @return the organizationAddress
     */
    public String getOrganizationAddress() {
        return organizationAddress;
    }

    /**
     * Set the address of the organization for the mzIdentML dataset.
     *
     * @param organizationAddress the organizationAddress to set
     */
    public void setOrganizationAddress(String organizationAddress) {
        this.organizationAddress = organizationAddress;
    }

    /**
     * Returns the user advocates used in this project.
     * 
     * @return the user advocates used in this project
     */
    public HashMap<Integer, Advocate> getUserAdvocateMapping() {
        return userAdvocateMapping;
    }

    /**
     * Sets the user advocates used in this project.
     * 
     * @param userAdvocateMapping  the user advocates used in this project
     */
    public void setUserAdvocateMapping(HashMap<Integer, Advocate> userAdvocateMapping) {
        this.userAdvocateMapping = userAdvocateMapping;
    }
}
