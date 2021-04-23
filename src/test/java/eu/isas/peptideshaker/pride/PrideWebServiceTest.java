package eu.isas.peptideshaker.pride;

import java.text.SimpleDateFormat;
import java.util.Set;
import org.junit.Assert;
import junit.framework.TestCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetail;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetailList;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetail;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetailList;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetailList;

/**
 * Test the PRIDE web service access.
 *
 * @author Harald Barsnes
 */
public class PrideWebServiceTest extends TestCase {

    /**
     * The web service URL.
     */
    private static final String PROJECT_SERVICE_URL = "http://www.ebi.ac.uk/pride/ws/archive/"; // "http://wwwdev.ebi.ac.uk:80/pride/ws/archive/";
    /**
     * The data format.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * Test is escaped in offline mode.
     */
    private static final boolean offline = true;

    /**
     * Test the PRIDE web service access.
     *
     * @throws Exception
     */
    public void testPrideWebService() throws Exception {

        if (!offline) {

            // the test project
            String projectTestAccession = "PXD001077";
            String assayTestAccession = "37809";

            // test if the web service is online
            RestTemplate template = new RestTemplate();
            ResponseEntity<Integer> projectCountResult = template.getForEntity(PROJECT_SERVICE_URL + "project/count", Integer.class); // can also use project/count/?q=*
            Integer projectCount = projectCountResult.getBody();
            Assert.assertTrue(projectCount > 0);

            // test if the project details can be retrieved
            ResponseEntity<ProjectDetailList> projectList = template.getForEntity(PROJECT_SERVICE_URL
                    + "project/list?show=" + projectCount + "&page=1&sort=publication_date&order=desc", ProjectDetailList.class);

            for (ProjectDetail projectDetail : projectList.getBody().getList()) {
                String projectAccession = projectDetail.getAccession();
                String projectTitle = projectDetail.getTitle();
                String projectSpecies = setToString(projectDetail.getSpecies(), ", ");
                String projectTissues = setToString(projectDetail.getTissues(), ", ");
                String ptmNames = setToString(projectDetail.getPtmNames(), "; ");
                String instrumentNames = setToString(projectDetail.getInstrumentNames(), ", ");
                int numAssays = projectDetail.getNumAssays();
                String submissionType = projectDetail.getSubmissionType();
                String date = dateFormat.format(projectDetail.getPublicationDate());
            }

            // test if the assays for the PeptideShaker example dataset can be found
            String url = PROJECT_SERVICE_URL + "assay/list/project/" + projectTestAccession;
            ResponseEntity<AssayDetailList> assayDetailList = template.getForEntity(url, AssayDetailList.class);

            for (AssayDetail assayDetail : assayDetailList.getBody().getList()) {
                String assayAccession = assayDetail.getAssayAccession();
                String assayTitle = assayDetail.getTitle();
                String species = setToString(assayDetail.getSpecies(), ", ");
                String sampleDetails = setToString(assayDetail.getSampleDetails(), ", ");
                String ptmNames = setToString(assayDetail.getPtmNames(), "; ");
                String diseases = setToString(assayDetail.getDiseases(), ", ");
                int proteinCount = assayDetail.getProteinCount();
                int peptideCount = assayDetail.getPeptideCount();
                int spectrumCount = assayDetail.getTotalSpectrumCount();
            }

            // test if the files for the PeptideShaker example dataset can be found
            url = PROJECT_SERVICE_URL + "file/list/project/" + projectTestAccession;
            ResponseEntity<FileDetailList> fileDetailListResult = template.getForEntity(url, FileDetailList.class);

            for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {

                if (fileDetail.getDownloadLink() != null) {
                    String fileDownloadLink = fileDetail.getDownloadLink().toExternalForm();
                }

                String assayAccession = fileDetail.getAssayAccession();
                String fileName = fileDetail.getFileName();
                String fileType = fileDetail.getFileType().getName();
                long fileSize = fileDetail.getFileSize();
            }

            // test if the files for the PeptideShaker example dataset assay can be found
            url = PROJECT_SERVICE_URL + "file/list/assay/" + assayTestAccession;
            fileDetailListResult = template.getForEntity(url, FileDetailList.class);

            for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {

                if (fileDetail.getDownloadLink() != null) {
                    String fileDownloadLink = fileDetail.getDownloadLink().toExternalForm();
                }

                String assayAccession = fileDetail.getAssayAccession();
                String fileName = fileDetail.getFileName();
                String fileType = fileDetail.getFileType().getName();
                long fileSize = fileDetail.getFileSize();
            }
        }
    }

    /**
     * Convert a set of strings to a single string.
     *
     * @param set
     * @param the separator to use
     * @return the set as a single string
     */
    private String setToString(Set<String> set, String separator) {
        Object[] elements = set.toArray();

        String result = "";
        for (Object object : elements) {
            if (!result.isEmpty()) {
                result += separator;
            }
            result += object.toString();
        }

        return result;
    }
}
