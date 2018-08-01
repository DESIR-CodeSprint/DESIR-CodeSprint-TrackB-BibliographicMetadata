package eu.dariah.desir.trackb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import eu.dariah.desir.trackb.service.MetadataExtractor;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.Enumeration;

/**
 * Created by hube on 8/1/2018.
 *
 * This controller manages the extraction of bibliographic data from json and pdf files
 */

@Controller
public class ExtractionController {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractionController.class);

    /**
     * @param file
     * @param text
     * @return
     */
    @PostMapping(value="/extract", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public @ResponseBody String handleFileUpload(
            HttpServletRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "text", required = false) String text) {

        Enumeration<String> enumeration = request.getParameterNames();
        LOG.info("Params:");
        while(enumeration.hasMoreElements()) {
            LOG.info(enumeration.nextElement());
        }
        LOG.info(request.getMethod());
        try {
            if (file == null && text == null) {

                throw new InvalidParameterException("The request does not contain a file nor a text string. We need one or " +
                        "the other.");
            } else if (file != null && text != null) {
                throw new InvalidParameterException("The request does contain both a file and a text string. We only need one or the other.");
            }
            if (file != null) {
                //handle file upload
                String file_name = file.getName();
                try {
                    File json_file = new File(System.getProperty("java.io.tmpdir") + "/" + file.getName());
                    file.transferTo(json_file);
                    LOG.info("Successfully uploaded " + file_name + " into " + json_file.getName());
                } catch (Exception e) {
                    LOG.error("Failed to upload " + file_name + " => " + e.getMessage());
                }
            } else {
                    //handle string "text"
            }
        } catch (InvalidParameterException ipe) {
            LOG.error("Error with parameters: ", ipe);
        }
        return "{}";
    }
}

