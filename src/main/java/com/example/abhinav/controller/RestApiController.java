package com.example.abhinav.controller;

import java.io.File;
import java.util.Set;

import javax.validation.Valid;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.ValidationUtils;
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

	@Autowired
	PlanServiceImpl planServiceImpl;

	@GetMapping
	@RequestMapping(value = "/InsurancePlan/getPlan/{id}")
	public ResponseEntity<?> getPlan(@PathVariable("id") String _id) {
		logger.info("Fetching Plan with id {}", _id);
		// Get the plan by its key
		Set<String> set = planServiceImpl.findPlanById(_id);
		if (set.size()<1) {
			logger.error("Plan with id {} not found.", _id);
			return new ResponseEntity("User with id " + _id + " not found", HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<String>(set.toString(), HttpStatus.OK);
	}

	@PostMapping
	@RequestMapping(value = "/InsurancePlan/createPlan")
	public ResponseEntity<?> createPlan(@Valid @RequestBody String jsonString) {
		logger.info("Creating plan....");
		validateJson(jsonString);
		JsonElement root = new JsonParser().parse(jsonString);
		Set<String> set = planServiceImpl.savePlan(root.getAsJsonObject().get("objectId").getAsString(), root);
		if(set.isEmpty()) {
			return new ResponseEntity<String>("Plan already present",HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<String>(set.toString(),HttpStatus.CREATED);

	}

	@PutMapping
	@RequestMapping(value = "/InsurancePlan/updatePlan/{id}")
	public void updatePlan() {

	}

	@DeleteMapping
	@RequestMapping(value = "/InsurancePlan/deletePlan/{id}")
	public ResponseEntity<?> deletePlan(@PathVariable("id") String _id) {
		logger.info("Fetching & Deleting Plan with id {}", _id);
		if(planServiceImpl.deletePlan(_id)>0) {
			logger.info("Plan with id {} deleted", _id);
			return new ResponseEntity("Plan with id:"+ _id +" deleted successfully", HttpStatus.OK);
		}
		return new ResponseEntity<>("Plan with id:"+ _id +" couldn't be deleted successfully",HttpStatus.NOT_FOUND);
	}
	
	public void validateJson(String jsonString) {
		logger.info("Inside Validate JSON method");
		System.out.println(jsonString);
		System.out.println();
	}
}
