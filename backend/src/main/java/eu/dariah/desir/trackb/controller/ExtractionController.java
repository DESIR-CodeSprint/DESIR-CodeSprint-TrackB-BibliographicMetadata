package eu.dariah.desir.trackb.controller;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.dariah.desir.trackb.json.JsonViews;
import eu.dariah.desir.trackb.model.YetAnotherBibliographicItem;
import eu.dariah.desir.trackb.service.BibSonomyAdaptor;
import eu.dariah.desir.trackb.service.MetadataExtractor;

/**
 * Created by hube on 8/1/2018.
 *
 * This controller manages the extraction of bibliographic data from json and pdf files and the storing in BibSonomy.
 */

@Controller
public class ExtractionController {

	private static final Logger LOG = LoggerFactory.getLogger(ExtractionController.class);
	private static final String ERROR_JSON = "{\"error\": true}";

	private final MetadataExtractor me;
	private final BibSonomyAdaptor adaptor;

	@Autowired
	public ExtractionController(MetadataExtractor me, BibSonomyAdaptor adaptor) {
		this.me = me;
		this.adaptor = adaptor;
	}

	/**
	 * @param file
	 * @return
	 */
	@PostMapping(value="/store")
	public @ResponseBody String storeInBibSonomy(@RequestParam(value = "file", required = false) MultipartFile file) {

		try {
			if (file != null) {
				final String fileName = file.getName();
				final File jsonFile;
				try {
					jsonFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getName());
					file.transferTo(jsonFile);
					LOG.info("Successfully uploaded " + fileName + " into " + jsonFile.getName());
				} catch (Exception e) {
					LOG.error("Failed to upload " + fileName, e);
					return ERROR_JSON;
				}
				try {
					adaptor.storeItems(me.extractItems(jsonFile));
				} catch(Exception e) {
					LOG.error("Failed to extract items", e);
					return ERROR_JSON;
				}
			} else {
				LOG.error("Received empty file");
				return ERROR_JSON;
			}
		} catch (InvalidParameterException ipe) {
			LOG.error("Error with parameters: ", ipe);
			return ERROR_JSON;
		}

		return null;
	}


	/**
	 * @param file
	 * @param text
	 * @return
	 */
	/**
	 * @param file
	 * @param text
	 * @return
	 */
	@PostMapping(value="/extract", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
	public @ResponseBody String handleFileUpload(
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "text", required = false) String text) {

        LOG.debug(file==null?"File is null":"File is not null");
        LOG.debug(text==null?"Text is null":"Text is not null");
        
		final List<YetAnotherBibliographicItem> items;

		try {
			// sanity checks for parameters
			if (file == null && text == null) {
				throw new InvalidParameterException("The request does neither contain a file nor a text string. We need one or the other.");
			} else if (file != null && text != null) {
				throw new InvalidParameterException("The request does contain both a file and a text string. We only need one of them.");
			}
			
			// handle file or text
			if (file != null) {
				final String fileName = file.getName();
				final File jsonFile;
				try {
					jsonFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getName());
					file.transferTo(jsonFile);
					LOG.info("Successfully uploaded " + fileName + " into " + jsonFile.getName());
				} catch (Exception e) {
					LOG.error("Failed to upload " + fileName + " => " + e.getMessage());
					return ERROR_JSON;
				}
				try {
					items = me.extractItems(jsonFile);
				} catch(Exception e) {
					return ERROR_JSON;
				}
			} else {
				try {
					items = me.extractItems(text);
				} catch(Exception e) {
					return ERROR_JSON;
				}
			}

			if (items == null) {
				LOG.error("Could not extract any items");
				return ERROR_JSON;
			}
			return toJson(items);

		} catch (InvalidParameterException ipe) {
			LOG.error("Error with parameters: ", ipe);
			return ERROR_JSON;
		}
	}

	/**
	 * Convert bibliographic items to JSON
	 * 
	 * @param items
	 * @return
	 */
	private static String toJson(List<YetAnotherBibliographicItem> items) {
		final StringBuilder sb = new StringBuilder();

		final ObjectMapper mapper = new ObjectMapper();
		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		try {
			sb.append(mapper.writerWithView(JsonViews.Public.class).writeValueAsString(items));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			LOG.error("TODO", e);
		}

		return sb.toString();
	}
}
