package eu.isas.peptideshaker.gui.pride;

import java.util.HashMap;
import java.util.Iterator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetail;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetailList;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetail;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetailList;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetailList;

/**
 * Test class for the PRIDE web service.
 *
 * @author Harald Barsnes
 */
public class PrideWsTest {

    /**
     * The web service URL.
     */
    private static final String projectServiceURL = "http://wwwdev.ebi.ac.uk/pride/ws/archive/";

    /**
     * Main class. For testing only.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // @TODO: get rid of the "Class path contains multiple SLF4J bindings" warning
        RestTemplate template = new RestTemplate();

        System.out.println("\n");

        // get the project count
        ResponseEntity<Integer> projectCountResult = template.getForEntity(projectServiceURL + "project/count", Integer.class);
        Integer projectCount = projectCountResult.getBody();
        System.out.println("Project count: " + projectCount + "\n");

        // get the list of projects
        ResponseEntity<ProjectDetailList> projectList = template.getForEntity(projectServiceURL + "project/list?show=" + projectCount + "&page=1&sort=publication_date&order=desc", ProjectDetailList.class);
        HashMap<String, Integer> typeCounter = new HashMap<String, Integer>();

        // iterate the project and print some details
        System.out.println("All projects:");
        int counter = 0;
        for (ProjectDetail projectDetail : projectList.getBody().getList()) {

            String submissionType = projectDetail.getSubmissionType();
            if (!typeCounter.containsKey(submissionType)) {
                typeCounter.put(submissionType, 0);
            }
            typeCounter.put(submissionType, typeCounter.get(submissionType) + 1);

            System.out.println(++counter + " " + projectDetail.getAccession() + ": " + submissionType + " date: " + projectDetail.getPublicationDate());
        }

        System.out.println("\nSubmission types:");

        // print the number per submission type
        Iterator<String> iterator = typeCounter.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            System.out.println(key + ": " + typeCounter.get(key));
        }

        System.out.println("\nProject details:");

        // get the details for a specific project
        String projectAccession = "PXD000375";
        ResponseEntity<ProjectDetail> projectDetails = template.getForEntity(projectServiceURL + "project/" + projectAccession, ProjectDetail.class);
        ProjectDetail projectDetail = projectDetails.getBody();

        System.out.println("Project: " + projectDetail.getAccession());
        System.out.println("Title: " + projectDetail.getTitle());
        System.out.println("Type: " + projectDetail.getSubmissionType());

        // get all assays for a given project
        ResponseEntity<AssayDetailList> assayDetailList = template.getForEntity(projectServiceURL + "assay/list/project/" + projectAccession, AssayDetailList.class);

        System.out.println("\nProject assays:");
        counter = 0;
        for (AssayDetail assayDetail : assayDetailList.getBody().getList()) {
            System.out.println(++counter + " " + assayDetail.getAssayAccession() + ": " + assayDetail.getTotalSpectrumCount());
        }
        System.out.println("\nAssay details:");

        // get the details for a given assay
        String assayAccession = "30323";
        ResponseEntity<AssayDetail> assayDetailsResult = template.getForEntity(projectServiceURL + "assay/" + assayAccession, AssayDetail.class);
        AssayDetail assayDetail = assayDetailsResult.getBody();
        System.out.println(assayDetail.getAssayAccession() + ": " + assayDetail.getExperimentalFactor());

        // get all the files for a given project
        ResponseEntity<FileDetailList> fileDetailListResult = template.getForEntity(projectServiceURL + "file/list/project/" + projectAccession, FileDetailList.class);

        System.out.println("\nProject file details:");
        counter = 0;
        for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {
            System.out.println(++counter + " " + fileDetail.getFileName() + ": " + fileDetail.getFileType() + " " + fileDetail.getDownloadLink());
        }
        System.out.println("\nAssay file details:");

        // get all the files for a given assay
        ResponseEntity<FileDetailList> fileDetailListResult2 = template.getForEntity(projectServiceURL + "file/list/assay/" + assayAccession, FileDetailList.class);

        counter = 0;
        for (FileDetail fileDetail : fileDetailListResult2.getBody().getList()) {
            System.out.println(++counter + " " + fileDetail.getFileName() + ": " + fileDetail.getFileType() + " " + fileDetail.getDownloadLink());
        }
        System.out.println("\n");
        // web service links:
        //
        // to get the number of projects
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/project/count
        //
        // to get the list of projects
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/project/list?show=1000&page=1&sort=publication_date&order=desc
        //
        // to get the details for a given project
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/project/PXD000375
        //
        // to get all assays for a given project
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/assay/list/project/PXD000375
        //
        // to get the details of a given assay
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/assay/30323
        //
        // to get all the files for a given project
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/file/list/project/PXD000375
        //
        // to get all the files for a given assay
        // http://wwwdev.ebi.ac.uk:80/pride/ws/archive/file/list/assay/30323
        //
        // to access private data:
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
//        String userName = "username"; // has to be an e-mail address, for reviewers add "@ebi.ac.uk"
//        String passWord = "password";
//        String authString = userName + ":" + passWord;
//        byte[] encodedAuthorisation = Base64.encodeBase64(authString.getBytes());
//        headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
//        HttpEntity<String> requestEntity = new HttpEntity<String>(headers);
//
//        String projectAccession = "PXD000651";
//        RestTemplate template = new RestTemplate();
//        ResponseEntity<ProjectDetail> entity;
//        entity = template.exchange("http://wwwdev.ebi.ac.uk/pride/ws/archive/project/" + projectAccession, HttpMethod.GET, requestEntity, ProjectDetail.class);
//
//        if (entity == null) {
//            System.out.println("ERROR: null return");
//            return;
//        }
//
//        if (entity.getHeaders() != null && entity.getHeaders().getLocation() != null) {
//            String path = entity.getHeaders().getLocation().getPath();
//            System.out.println("Path: " + path);
//        }
//
//        if (entity.getStatusCode() != null) {
//            System.out.println("Equals? " + HttpStatus.OK + " = " + entity.getStatusCode());
//        }
//        ProjectDetail project = entity.getBody();
//
//        System.out.println("The Project acc is: " + project.getAccession());
//        System.out.println("Project desc: " + project.getProjectDescription());
    }
}
