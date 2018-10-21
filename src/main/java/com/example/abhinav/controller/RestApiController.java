package com.example.abhinav.controller;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.validation.Valid;

import org.apache.commons.collections.iterators.EntrySetMapIterator;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

@Controller
public class RestApiController {

	public static final Logger logger = LoggerFactory.getLogger(RestApiController.class);
	String _VALIDATION_FAILED = "validation failed";
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
		// parseJson(jsonString);
		JsonElement root = new JsonParser().parse(jsonString);
		if (validateJson(root.toString()).equalsIgnoreCase(_VALIDATION_FAILED)) {
			return validationMessage();
		}
		Set<String> set = planServiceImpl.savePlan(root.getAsJsonObject().get("objectId").getAsString(), root);
		if (set.isEmpty()) {
			return new ResponseEntity<String>("Plan already present", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<String>(set.toString(), HttpStatus.CREATED);
	}

	@PostMapping
	@RequestMapping(value = "/InsurancePlan/testPlan")
	public ResponseEntity<?> testPlan(@Valid @RequestBody String jsonString) throws Exception {
		logger.info("Creating plan testing....");

		Type mapType = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, Object> root = new Gson().fromJson(jsonString, mapType);
		parseJson(jsonString, root);

		return new ResponseEntity<String>("ABC", HttpStatus.CREATED);
	}

	@PutMapping
	@RequestMapping(value = "/InsurancePlan/updatePlan")
	public ResponseEntity<?> updatePlan(@RequestBody String jsonString) throws Exception {
		logger.info("Inside update method plan");
		JsonElement root = new JsonParser().parse(jsonString);
		if (validateJson(root.toString()).equalsIgnoreCase(_VALIDATION_FAILED)) {
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
			// JSONObject rawSchema = jsonSchema;
			Schema schema = SchemaLoader.load(jsonSchema);
			schema.validate(new JSONObject(jsonString.toString())); // throws a ValidationException if this object is
																	// invalid
		} catch (ValidationException ve) {
			System.out.println(ve.getMessage());
			return _VALIDATION_FAILED;
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

	public void parseJson(String jsonString, Map<String, Object> root) throws JSONException {

		System.out.println(root);
		HashMap<String, Object> saveMap = new HashMap<>();
		for (Entry<String, Object> rootMap : root.entrySet()) {
			if (rootMap.getValue() instanceof Map) {
				checkMap((Map<String, Object>) rootMap.getValue());
			} else if (rootMap.getValue() instanceof List) {
				checkList((List<Object>) rootMap.getValue());
			}
			else if(!(rootMap.getValue() instanceof List) && !(rootMap.getValue() instanceof Map)) {
				saveMap.put(rootMap.getKey(), rootMap.getValue());
			}
		}
		save(saveMap);
		saveMap.clear();
	}

	public void checkMap(Map<String, Object> map) {
		HashMap<String, Object> saveMap = new HashMap<>();
		for (Entry<String, Object> tmpMap : map.entrySet()) {
			if (tmpMap.getValue() instanceof Map) {
				checkMap((Map<String, Object>) tmpMap.getValue());
			} else if (tmpMap.getValue() instanceof List) {
				checkList((List<Object>) tmpMap.getValue());
			}
			
			else if(!(tmpMap.getValue() instanceof List) && !(tmpMap.getValue() instanceof Map)) {
				saveMap.put(tmpMap.getKey(), tmpMap.getValue());
			}
		}
		save(saveMap);
		saveMap.clear();

	}

	public void checkList(List<Object> listMap) {
		for (Object tmpMap : listMap) {
			if (tmpMap instanceof Map) {
				System.out.println("List has a map, redirecting to checkMap()");
				checkMap((Map<String, Object>) tmpMap);
				//listMap.remove(tmpMap);
			}

			// save(listMap);

			// save the remaining item in listMap
		}

	}

	public void save(Map<String, Object> parsedMap) {
		System.out.println("Map:: " + parsedMap);
	}

	// JsonArray ele = root.getagetAsJsonArray("linkedPlanServices");
	// System.out.println(ele);
	// System.out.println(root.getAsJsonPrimitive());
	/*
	 * JSONObject obj = new JSONObject(jsonString); System.out.println(obj);
	 * //String pageName = obj.getJSONObject("linkedService").toString();
	 * //System.out.println(pageName); JSONArray arr =
	 * obj.getJSONArray("linkedPlanServices");
	 * System.out.println("array length:: "+arr.length()); for (int i = 0; i <
	 * arr.length(); i++) { String post_id =
	 * arr.getJSONObject(i).getString("objectType"); System.out.println(post_id); }
	 */
}
