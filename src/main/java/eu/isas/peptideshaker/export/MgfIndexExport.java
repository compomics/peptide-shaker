package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * This class takes care of exporting mgf indexes.
 *
 * @author Marc Vaueel
 * @author Harald Barsnes
 * @author Carlos horro
 * 
 */
public class MgfIndexExport {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    
    /**
     * Exports mgf file indexes as zip file or into a folder adding them to the first
     * one or just copying them to the second one.
     *
     * @param destinationZipFile the destination compressed file
     * @param destinationFolder the destination folder
     * @param spectrumFiles the spectrum files
     * @param cpsFile the psdb file
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing the file
     */
    public MgfIndexExport(File destinationZipFile, File destinationFolder, ArrayList<File> spectrumFiles, File cpsFile, WaitingHandler waitingHandler) throws IOException {
        if (spectrumFiles == null)
            spectrumFiles = new ArrayList<File>();
        
        if (spectrumFiles.size() == 0){
            ArrayList<String> mgfFileNames = spectrumFactory.getMgfFileNames();
            
            for(String mgfFileName: mgfFileNames){
                spectrumFiles.add(spectrumFactory.getMgfFileFromName(mgfFileName));
            }
        }
        
        ArrayList<String> indexFiles = new ArrayList<String>();
        
        int indexesPositions = indexFiles.size();
        
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting Spectrum Index Files. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setSecondaryProgressCounter(0);
            waitingHandler.setMaxSecondaryProgressCounter(spectrumFiles.size());
        }

        for (File spectrumFile : spectrumFiles) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            if (spectrumFile.exists()) {
                File indexFile = new File(spectrumFile.getParentFile(), SpectrumFactory.getIndexName(spectrumFile.getName()));
                if (indexFile.exists()) {
                    indexFiles.add(indexesPositions++, indexFile.getAbsolutePath());
                }

            }
        }

        if (waitingHandler != null) {
            if (destinationZipFile != null)
                waitingHandler.setWaitingText("Zipping index files. Please Wait...");
            else
                waitingHandler.setWaitingText("Copying index files. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        // zip index files
        if (destinationZipFile != null){
            FileOutputStream fos = new FileOutputStream(destinationZipFile);
            try {
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                try {
                    ZipOutputStream out = new ZipOutputStream(bos);
                    try {
                        // get the total uncompressed size
                        long totalUncompressedSize = 0;

                        for (String dataFilePath : indexFiles) {
                            totalUncompressedSize += new File(dataFilePath).length();
                        }

                        // add the files to the zip
                        if (waitingHandler != null) {
                            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                            waitingHandler.setSecondaryProgressCounter(0);
                            waitingHandler.setMaxSecondaryProgressCounter(100);
                        }

                        for (String indexFilePath : indexFiles) {

                            if (waitingHandler != null) {
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                                waitingHandler.increaseSecondaryProgressCounter();
                            }

                            File indexFile = new File(indexFilePath);
                            ZipUtils.addFileToZip(indexFile, out, waitingHandler, totalUncompressedSize);
                        }

                        if (waitingHandler != null) {
                            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                        }

                    } finally {
                        out.close();
                    }
                } finally {
                    bos.close();
                }
            } finally {
                fos.close();
            }
        // copy the index files to the destination folder
        }else if (destinationFolder != null){
            
            if (waitingHandler != null) {
                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                waitingHandler.setSecondaryProgressCounter(0);
                waitingHandler.setMaxSecondaryProgressCounter(100);
            }

            for (String indexFilePath : indexFiles) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }
                File indexFile = new File(indexFilePath);
                FileUtils.copyFileToDirectory(indexFile, destinationFolder);
            }

            if (waitingHandler != null) {
                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            }
                    
        }
            
    }
}
