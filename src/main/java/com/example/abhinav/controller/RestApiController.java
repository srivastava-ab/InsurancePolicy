package com.example.abhinav.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import javax.validation.Valid;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.abhinav.service.PlanServiceImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@Controller
public class RestApiController {

	public static final Logger logger = LoggerFactory.getLogger(RestApiController.class);
	String VALIDATION_FAILED = "validation failed";
	@Autowired
	PlanServiceImpl planServiceImpl;
	

	@GetMapping
	@RequestMapping(value = "/InsurancePlan/getPlan/{id}")
	public ResponseEntity<?> getPlan(@PathVariable("id") String _id) {
		logger.info("Fetching Plan with id {}", _id);
		// Get the plan by its key
		Set<String> set = planServiceImpl.findPlanById(_id);
		if (set.size() < 1) {
			logger.error("Plan with id {} not found.", _id);
			return new ResponseEntity<String>("Plan with id " + _id + " not found", HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<String>(set.toString(), HttpStatus.OK);
	}

	@PostMapping
	@RequestMapping(value = "/InsurancePlan/createPlan")
	public ResponseEntity<?> createPlan(@Valid @RequestBody String jsonString) throws Exception {
		logger.info("Creating plan....");

		JsonElement root = new JsonParser().parse(jsonString);
		if(validateJson(root.toString()).equalsIgnoreCase(VALIDATION_FAILED)) {
			return validationMessage();
		}
		Set<String> set = planServiceImpl.savePlan(root.getAsJsonObject().get("objectId").getAsString(), root);
		if (set.isEmpty()) {
			return new ResponseEntity<String>("Plan already present", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<String>(set.toString(), HttpStatus.CREATED);
	}

	@PutMapping
	@RequestMapping(value = "/InsurancePlan/updatePlan")
	public ResponseEntity<?> updatePlan(@RequestBody String jsonString) throws Exception {
		logger.info("Inside update method plan");
		JsonElement root = new JsonParser().parse(jsonString);
		if(validateJson(root.toString()).equalsIgnoreCase(VALIDATION_FAILED)) {
			return validationMessage();
		}
		boolean flag = planServiceImpl.updatePlanById(root.getAsJsonObject().get("objectId").getAsString(), root);
		if (flag) {
			return new ResponseEntity<String>("Plan updated", HttpStatus.OK);
		}
		return new ResponseEntity<String>("Bad request", HttpStatus.BAD_REQUEST);
	}

	@DeleteMapping
	@RequestMapping(value = "/InsurancePlan/deletePlan/{id}")
	public ResponseEntity<?> deletePlan(@PathVariable("id") String _id) {
		logger.info("Fetching & Deleting Plan with id {}", _id);
		if (planServiceImpl.deletePlan(_id) > 0) {
			logger.info("Plan with id {} deleted", _id);
			return new ResponseEntity<String>("Plan with id:" + _id + " deleted successfully", HttpStatus.OK);
		}
		return new ResponseEntity<String>("Plan with id:" + _id + " not found!!!", HttpStatus.NOT_FOUND);
	}

	@SuppressWarnings("deprecation")
	public String validateJson(String jsonString) throws Exception {
		logger.info("Inside Validate JSON method");
		try {
			String filePath = ".\\src\\main\\resources\\schema.json";
			File file = new File(filePath);
			InputStream inputStream = new FileInputStream(file);
			String jsonTxt = IOUtils.toString(inputStream);
			JSONObject jsonSchema = new JSONObject(jsonTxt);
			//JSONObject rawSchema = jsonSchema;
			Schema schema = SchemaLoader.load(jsonSchema);
			schema.validate(new JSONObject(jsonString.toString())); // throws a ValidationException if this object is invalid
		} catch (ValidationException ve) {
			System.out.println(ve.getMessage());
			return VALIDATION_FAILED;
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "success";
	}
	
	public ResponseEntity<?> validationMessage() {
		return new ResponseEntity<String>("Validation failed for input payload", HttpStatus.NOT_ACCEPTABLE);
	}
}
