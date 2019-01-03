package eu.isas.peptideshaker.preferences;

import com.compomics.util.Util;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.identification.Advocate;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import com.compomics.util.pride.prideobjects.*;
import java.util.HashMap;
import java.util.Set;

/**
 * This class contains the details about a project.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProjectDetails extends DbObject {

    /**
     * List of the identification files loaded.
     */
    private final ArrayList<File> identificationFiles = new ArrayList<>();
    /**
     * Map of the identification algorithms names and versions used to generate
     * the identification files. identification file name &gt; Advocate Ids &gt;
     * identification algorithm versions used.
     */
    private HashMap<String, HashMap<String, ArrayList<String>>> identificationAlgorithms = new HashMap<>();
    /**
     * Map of the spectrum files paths indexed by name.
     */
    private HashMap<String, String> spectrumFiles = new HashMap<>();
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
     * The mzIdentML output file.
     */
    private String mzIdentMLOutputFile;
    /**
     * If true, the protein sequences are included in the mzid file.
     */
    private boolean includeProteinSequences;
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
        readDBMode();
        return identificationFiles;
        
    }

    /**
     * Adds an identification file to the list of loaded identification files
     *
     * @param identificationFile the identification file loaded
     */
    public void addIdentificationFiles(File identificationFile) {
        writeDBMode();
        identificationFiles.add(identificationFile);
        
    }

    /**
     * Attaches a spectrum file to the project.
     *
     * @param spectrumFile the spectrum file to add
     */
    public void addSpectrumFile(File spectrumFile) {
        
        String path = spectrumFile.getAbsolutePath();
        String fileName = Util.getFileName(path);
        spectrumFiles.put(fileName, path);
    }
    
    /**
     * Returns the name of the input spectrum files as a set.
     * 
     * @return the spectrum file names
     */
    public Set<String> getSpectrumFileNames() {
        
        readDBMode();
        return spectrumFiles.keySet();
        
    }

    /**
     * Returns the file corresponding to the given name.
     *
     * @param fileName the name of the desired file
     * 
     * @return the corresponding file, null if not found.
     */
    public File getSpectrumFile(String fileName) {
        
        readDBMode();
        return new File(spectrumFiles.get(fileName));
        
    }

    /**
     * Getter for the creation date of the project.
     *
     * @return the creation date of the project
     */
    public Date getCreationDate() {
        
        readDBMode();
        return creationDate;
        
    }

    /**
     * Setter the creation date of the project.
     *
     * @param creationDate the creation date of the project
     */
    public void setCreationDate(Date creationDate) {
        writeDBMode();
        this.creationDate = creationDate;
        
    }

    /**
     * Returns the PeptideShaker version used to create the project.
     *
     * @return the PeptideShaker version used to create the project
     */
    public String getPeptideShakerVersion() {
        
        readDBMode();
        return peptideShakerVersion;
        
    }

    /**
     * Sets the PeptideShaker version used to create the project.
     *
     * @param peptideShakerVersion the PeptideShaker version used to create the
     * project
     */
    public void setPeptideShakerVersion(String peptideShakerVersion) {
        writeDBMode();
        this.peptideShakerVersion = peptideShakerVersion;
        
    }

    /**
     * Returns the report created during the loading of the project.
     *
     * @return the report created during the loading of the project
     */
    public String getReport() {

        readDBMode();
        return report == null ? "(report not saved)" : report;
        
    }

    /**
     * Set the report created during the loading of the project.
     *
     * @param report the report to set
     */
    public void setReport(String report) {
        writeDBMode();
        this.report = report;
        
    }

    /**
     * Returns the PRIDE experiment title.
     *
     * @return the prideExperimenttitle
     */
    public String getPrideExperimentTitle() {
        
        readDBMode();
        return prideExperimentTitle;
        
    }

    /**
     * Sets the PRIDE experiment title.
     *
     * @param prideExperimentTitle the prideExperimentTitle to set
     */
    public void setPrideExperimentTitle(String prideExperimentTitle) {
        writeDBMode();
        this.prideExperimentTitle = prideExperimentTitle;
        
    }

    /**
     * Returns the PRIDE experiment label.
     *
     * @return the prideExperimentLabel
     */
    public String getPrideExperimentLabel() {
        
        readDBMode();
        return prideExperimentLabel;
        
    }

    /**
     * Sets the PRIDE experiment label.
     *
     * @param prideExperimentLabel the prideExperimentLabel to set
     */
    public void setPrideExperimentLabel(String prideExperimentLabel) {
        writeDBMode();
        this.prideExperimentLabel = prideExperimentLabel;
        
    }

    /**
     * Returns the PRIDE experiment project title.
     *
     * @return the prideExperimentProjectTitle
     */
    public String getPrideExperimentProjectTitle() {
        
        readDBMode();
        return prideExperimentProjectTitle;
        
    }

    /**
     * Set the PRIDE experiment project title.
     *
     * @param prideExperimentProjectTitle the prideExperimentProjectTitle to set
     */
    public void setPrideExperimentProjectTitle(String prideExperimentProjectTitle) {
        writeDBMode();
        this.prideExperimentProjectTitle = prideExperimentProjectTitle;
        
    }

    /**
     * Returns the PRIDE experiment project description.
     *
     * @return the prideExperimentDescription
     */
    public String getPrideExperimentDescription() {
        
        readDBMode();
        return prideExperimentDescription;
        
    }

    /**
     * Set the PRIDE experiment project description.
     *
     * @param prideExperimentDescription the prideExperimentDescription to set
     */
    public void setPrideExperimentDescription(String prideExperimentDescription) {
        writeDBMode();
        this.prideExperimentDescription = prideExperimentDescription;
        
    }

    /**
     * Returns the PRIDE reference group.
     *
     * @return the prideReferenceGroup
     */
    public ReferenceGroup getPrideReferenceGroup() {
        
        readDBMode();
        return prideReferenceGroup;
        
    }

    /**
     * Set the PRIDE reference group.
     *
     * @param prideReferenceGroup the prideReferenceGroup to set
     */
    public void setPrideReferenceGroup(ReferenceGroup prideReferenceGroup) {
        writeDBMode();
        this.prideReferenceGroup = prideReferenceGroup;
        
    }

    /**
     * Returns the PRIDE contact group.
     *
     * @return the prideContactGroup
     */
    public ContactGroup getPrideContactGroup() {
        
        readDBMode();
        return prideContactGroup;
        
    }

    /**
     * Set the PRIDE contact group.
     *
     * @param prideContactGroup the prideContactGroup to set
     */
    public void setPrideContactGroup(ContactGroup prideContactGroup) {
        writeDBMode();
        this.prideContactGroup = prideContactGroup;
        
    }

    /**
     * Returns the PRIDE sample.
     *
     * @return the prideSample
     */
    public Sample getPrideSample() {
        
        readDBMode();
        return prideSample;
        
    }

    /**
     * Set the PRIDE sample.
     *
     * @param prideSample the prideSample to set
     */
    public void setPrideSample(Sample prideSample) {
        writeDBMode();
        this.prideSample = prideSample;
        
    }

    /**
     * Returns the PRIDE protocol.
     *
     * @return the prideProtocol
     */
    public Protocol getPrideProtocol() {
        
        readDBMode();
        return prideProtocol;
        
    }

    /**
     * Set the PRIDE protocol.
     *
     * @param prideProtocol the prideProtocol to set
     */
    public void setPrideProtocol(Protocol prideProtocol) {
        writeDBMode();
        this.prideProtocol = prideProtocol;
        
    }

    /**
     * Returns the PRIDE instrument.
     *
     * @return the prideInstrument
     */
    public Instrument getPrideInstrument() {
        
        readDBMode();
        return prideInstrument;
        
    }

    /**
     * Set the the PRIDE instrument.
     *
     * @param prideInstrument the prideInstrument to set
     */
    public void setPrideInstrument(Instrument prideInstrument) {
        writeDBMode();
        this.prideInstrument = prideInstrument;
        
    }

    /**
     * Returns the PRIDE output folder.
     *
     * @return the prideOutputFolder
     */
    public String getPrideOutputFolder() {
        
        readDBMode();
        return prideOutputFolder;
        
    }

    /**
     * Set the PRIDE output folder.
     *
     * @param prideOutputFolder the prideOutputFolder to set
     */
    public void setPrideOutputFolder(String prideOutputFolder) {
        writeDBMode();
        this.prideOutputFolder = prideOutputFolder;
        
    }

    /**
     * Returns the mzIdentML output file.
     *
     * @return the mzIdentML output file
     */
    public String getMzIdentMLOutputFile() {
        
        readDBMode();
        return mzIdentMLOutputFile;
        
    }

    /**
     * Set the mzIdentML output file.
     *
     * @param mzIdentMLOutputFile the mzIdentMLOutputFile to set
     */
    public void setMzIdentOutputFile(String mzIdentMLOutputFile) {
        writeDBMode();
        this.mzIdentMLOutputFile = mzIdentMLOutputFile;
        
    }

    /**
     * Returns true if the identification algorithms are stored.
     *
     * @return true if the identification algorithms are stored
     */
    public boolean hasIdentificationAlgorithms() {
        
        readDBMode();
        return identificationAlgorithms != null;
        
    }

    /**
     * Returns a list of identification algorithms used based on the
     * identification files of the project.
     *
     * @return a list of identification algorithms indexed by the static field
     * of the Advocate class
     */
    public ArrayList<Integer> getIdentificationAlgorithms() {
        
        readDBMode();
        ArrayList<Integer> result = new ArrayList<>();
        
        for (HashMap<String, ArrayList<String>> advocateVersions : identificationAlgorithms.values()) {
            
            for (String advocateName : advocateVersions.keySet()) {
                
                Advocate advocate = Advocate.getAdvocate(advocateName);
                
                if (advocate == null) {
                    
                    throw new IllegalArgumentException("Identification algorithm " + advocateName + " not recognized.");
                    
                }
                
                int advocateId = advocate.getIndex();
                
                if (!result.contains(advocateId)) {
                    
                    result.add(advocateId);
                    
                }
            }
        }
        
        return result;
        
    }

    /**
     * Returns the different identification algorithm versions used in a map:
     * algorithm name &gt; versions.
     *
     * @return the different identification algorithm versions used
     */
    public HashMap<String, ArrayList<String>> getAlgorithmNameToVersionsMap() {
        
        readDBMode();
        HashMap<String, ArrayList<String>> algorithmNameToVersionMap = new HashMap<>();
        
        for (HashMap<String, ArrayList<String>> fileMapping : identificationAlgorithms.values()) {
            
            for (String softwareName : fileMapping.keySet()) {
                
                ArrayList<String> newVersions = fileMapping.get(softwareName);
                
                if (newVersions != null && !newVersions.isEmpty()) {
                    
                    ArrayList<String> currentVersions = algorithmNameToVersionMap.get(softwareName);
                    
                    if (currentVersions == null) {
                        
                        currentVersions = new ArrayList<>(newVersions);
                        algorithmNameToVersionMap.put(softwareName, currentVersions);
                        
                    } else {
                        
                        for (String version : newVersions) {
                            
                            if (!currentVersions.contains(version)) {
                                
                                currentVersions.add(version);
                                
                            }
                        }
                    }
                }
            }
        }
        
        return algorithmNameToVersionMap;
        
    }

    /**
     * Returns the identification algorithms used to create the id file in map:
     * algorithm name &gt; algorithm version.
     *
     * @param idFileName the identification file name
     *
     * @return the identification algorithms used
     */
    public HashMap<String, ArrayList<String>> getIdentificationAlgorithmsForFile(String idFileName) {
        
        readDBMode();
        return identificationAlgorithms == null ? null : identificationAlgorithms.get(idFileName);
        
    }

    /**
     * Sets the identification algorithms used to create an identification file.
     *
     * @param idFileName the name of the identification file
     * @param fileIdentificationAlgorithms the identification algorithms used to
     * create this file in a map: algorithm name &gt; versions
     */
    public void setIdentificationAlgorithmsForFile(String idFileName, HashMap<String, ArrayList<String>> fileIdentificationAlgorithms) {
        writeDBMode();
        identificationAlgorithms.put(idFileName, fileIdentificationAlgorithms);
        
    }

    /**
     * Returns the first name of the contact for the mzIdentML dataset.
     *
     * @return the contactFirstName
     */
    public String getContactFirstName() {
        
        readDBMode();
        return contactFirstName;
        
    }

    /**
     * Set the first name of the contact for the mzIdentML dataset.
     *
     * @param contactFirstName the contactFirstName to set
     */
    public void setContactFirstName(String contactFirstName) {
        writeDBMode();
        this.contactFirstName = contactFirstName;
        
    }

    /**
     * Returns the last name of the contact for the mzIdentML dataset.
     *
     * @return the contactLastName
     */
    public String getContactLastName() {
        
        readDBMode();
        return contactLastName;
        
    }

    /**
     * Set the last name of the contact for the mzIdentML dataset.
     *
     * @param contactLastName the contactLastName to set
     */
    public void setContactLastName(String contactLastName) {
        writeDBMode();
        this.contactLastName = contactLastName;
        
    }

    /**
     * Returns the e-mail of the contact for the mzIdentML dataset.
     *
     * @return the contactEmailName
     */
    public String getContactEmail() {
        
        readDBMode();
        return contactEmail;
        
    }

    /**
     * Set the e-mail of the contact for the mzIdentML dataset.
     *
     * @param contactEmail the contactEmailName to set
     */
    public void setContactEmail(String contactEmail) {
        writeDBMode();
        this.contactEmail = contactEmail;
        
    }

    /**
     * Returns the URL of the contact for the mzIdentML dataset.
     *
     * @return the contactUrl
     */
    public String getContactUrl() {
        
        readDBMode();
        return contactUrl;
        
    }

    /**
     * Set the first URL of the contact for the mzIdentML dataset.
     *
     * @param contactUrl the contactUrl to set
     */
    public void setContactUrl(String contactUrl) {
        writeDBMode();
        this.contactUrl = contactUrl;
        
    }

    /**
     * Returns the address of the contact for the mzIdentML dataset.
     *
     * @return the contactAddress
     */
    public String getContactAddress() {
        
        readDBMode();
        return contactAddress;
        
    }

    /**
     * SEt the address of the contact for the mzIdentML dataset.
     *
     * @param contactAddress the contactAddress to set
     */
    public void setContactAddress(String contactAddress) {
        writeDBMode();
        this.contactAddress = contactAddress;
        
    }

    /**
     * Returns the name of the organization for the mzIdentML dataset.
     *
     * @return the organizationName
     */
    public String getOrganizationName() {
        
        readDBMode();
        return organizationName;
        
    }

    /**
     * Set the name of the organization for the mzIdentML dataset.
     *
     * @param organizationName the organizationName to set
     */
    public void setOrganizationName(String organizationName) {
        writeDBMode();
        this.organizationName = organizationName;
        
    }

    /**
     * Returns the e-mail of the organization for the mzIdentML dataset.
     *
     * @return the organizationEmail
     */
    public String getOrganizationEmail() {
        
        readDBMode();
        return organizationEmail;
        
    }

    /**
     * Set the name of the organization for the mzIdentML dataset.
     *
     * @param organizationEmail the organizationEmail to set
     */
    public void setOrganizationEmail(String organizationEmail) {
        writeDBMode();
        this.organizationEmail = organizationEmail;
        
    }

    /**
     * Returns the URL of the organization for the mzIdentML dataset.
     *
     * @return the organizationUrl
     */
    public String getOrganizationUrl() {
        
        readDBMode();
        return organizationUrl;
        
    }

    /**
     * Set the URL of the organization for the mzIdentML dataset.
     *
     * @param organizationUrl the organizationUrl to set
     */
    public void setOrganizationUrl(String organizationUrl) {
        writeDBMode();
        this.organizationUrl = organizationUrl;
        
    }

    /**
     * Returns the address of the organization for the mzIdentML dataset.
     *
     * @return the organizationAddress
     */
    public String getOrganizationAddress() {
        
        readDBMode();
        return organizationAddress;
        
    }

    /**
     * Set the address of the organization for the mzIdentML dataset.
     *
     * @param organizationAddress the organizationAddress to set
     */
    public void setOrganizationAddress(String organizationAddress) {
        writeDBMode();
        this.organizationAddress = organizationAddress;
        
    }

    /**
     * Returns the user advocates used in this project.
     *
     * @return the user advocates used in this project
     */
    public HashMap<Integer, Advocate> getUserAdvocateMapping() {
        
        readDBMode();
        return userAdvocateMapping;
        
    }

    /**
     * Sets the user advocates used in this project.
     *
     * @param userAdvocateMapping the user advocates used in this project
     */
    public void setUserAdvocateMapping(HashMap<Integer, Advocate> userAdvocateMapping) {
        writeDBMode();
        this.userAdvocateMapping = userAdvocateMapping;
        
    }

    /**
     * Returns true if the protein sequences are to be included in the mzid
     * export.
     *
     * @return true if the protein sequences are to be included in the mzid
     * export
     */
    public Boolean getIncludeProteinSequences() {
        
        readDBMode();
        
        if (includeProteinSequences == null) {
            includeProteinSequences = false;
        }
        
        return includeProteinSequences;
        
    }

    /**
     * Set if the protein sequences are to be included in the mzid export.
     *
     * @param includeProteinSequences the includeProteinSequences to set
     */
    public void setIncludeProteinSequences(Boolean includeProteinSequences) {
        writeDBMode();
        this.includeProteinSequences = includeProteinSequences;
        
    }
}
