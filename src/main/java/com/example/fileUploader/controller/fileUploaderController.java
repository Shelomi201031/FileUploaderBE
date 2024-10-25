package com.example.fileUploader.controller;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("api/v1")
public class fileUploaderController {

    private static final Logger logger = LoggerFactory.getLogger(fileUploaderController.class);

    // In-memory map to temporarily hold file chunks based on the file name
    private Map<String, ByteArrayOutputStream> fileChunks = new HashMap<>();
    private Map<String, Integer> receivedChunkCount = new HashMap<>();

    @PostMapping("/upload")
    public ResponseEntity<String> uploadChunk(
            @RequestParam("file")MultipartFile chunk,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks){

        // Adjust chunkNumber to start from 0 (if it starts from 1)

        logger.info("Received chunk {} of {} for file: {}", chunkNumber , totalChunks, fileName);

        try{
            //Get or create ByteArrayOutputStream for the file
            ByteArrayOutputStream outputStream = fileChunks.getOrDefault(fileName,new ByteArrayOutputStream());
            outputStream.write(chunk.getBytes()); // Write the chunk Bytes

            //Store updated outputStream back in the map
            fileChunks.put(fileName,outputStream);

            // Update the number of received chunks
            receivedChunkCount.put(fileName, receivedChunkCount.getOrDefault(fileName, 0));

            if(receivedChunkCount.get(fileName) == totalChunks){
                logger.info("All chunks received for the file: {}",fileName);
                reassembleFile(fileName,outputStream);
                return ResponseEntity.ok("File successfully reassembled.");
            }

            return ResponseEntity.ok("Chunk received.");

        } catch (IOException e) {
            logger.error("Error processing chunk {} for file {}: {}", chunkNumber, fileName, e.getMessage());
            return ResponseEntity.status(500).body("Error processing chunk " + chunkNumber + " for file " + fileName);
        }
    }

    //Reassemble the file from chunks and Log the final file size
    private void reassembleFile(String fileName, ByteArrayOutputStream outputStream) {
        logger.info("Reassembing file {}", fileName);
        int totalSize = outputStream.size();
        logger.info("File {} reassembled with total size: {} bytes", fileName, totalSize);

        //After reassembly,clear data from the memory
        fileChunks.remove(fileName);
        receivedChunkCount.remove(fileName);
    }
}
