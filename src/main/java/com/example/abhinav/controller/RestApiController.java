package com.example.abhinav.controller;

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

import javax.servlet.http.HttpServletRequest;
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

import com.example.abhinav.elasticsearch.ElasticSearchUtility;
import com.example.abhinav.service.PlanService;
import com.example.abhinav.service.PlanServiceImpl;
import com.example.abhinav.util.EncryptionUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

@Controller
public class RestApiController {

	public static final Logger logger = LoggerFactory.getLogger(RestApiController.class);
	String _VALIDATION_FAILED = "validation failed";
	EncryptionUtil encUtil = new EncryptionUtil();

	@Autowired
	PlanService planService;

	@GetMapping
	@RequestMapping(value = "/InsurancePlan/plan/{id}")
	public ResponseEntity<?> getPlan(HttpServletRequest request, @PathVariable("id") String _id) {
		logger.info("Fetching Plan with id {}", _id);
		String bearerToken = request.getHeader("Authorization");
//		if (!encUtil.decrypt(bearerToken)) {
//			return new ResponseEntity<String>("Invalid bearer token", HttpStatus.UNAUTHORIZED);
//		}
		String set = planService.findPlanById("plan__" + _id);
		if (set.equalsIgnoreCase("not found")) {
			return new ResponseEntity<String>("Id not found", HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<String>(set.toString(), HttpStatus.OK);
	}

	@PostMapping
	@RequestMapping(value = "/InsurancePlan/plan")
	public ResponseEntity<?> createPlan(HttpServletRequest request, @Valid @RequestBody String jsonString) {
		logger.info("Creating plan testing....");
		String bearerToken = request.getHeader("Authorization");
		String newRootKey = null;
		
		ElasticSearchUtility eu = new ElasticSearchUtility();
		
		
		
		try {
			if (!encUtil.decrypt(bearerToken)) {
				return new ResponseEntity<String>("Invalid bearer token", HttpStatus.UNAUTHORIZED);
			}
			JsonElement rootValidation = new JsonParser().parse(jsonString);
			if (validateJson(rootValidation.toString()).equalsIgnoreCase(_VALIDATION_FAILED)) {
				return validationMessage();
			}
			Type mapType = new TypeToken<Map<String, Object>>() {
			}.getType();
			Map<String, Object> root = new Gson().fromJson(jsonString, mapType);
			newRootKey = parseJson(root).split("__")[1];
			eu.req(root);
			System.out.println("Root length --->> "+root.size());
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>("Bad processing", HttpStatus.PROCESSING);
		}
		return new ResponseEntity<String>("RootKey:: " + newRootKey, HttpStatus.CREATED);
	}

	@PutMapping
	@RequestMapping(value = "/InsurancePlan/update/plan")
	public ResponseEntity<?> updatePlan(HttpServletRequest request, @RequestBody String jsonString) throws Exception {
		logger.info("Inside update method plan");
		String bearerToken = request.getHeader("Authorization");
		if (!encUtil.decrypt(bearerToken)) {
			return new ResponseEntity<String>("Invalid bearer token", HttpStatus.UNAUTHORIZED);
		}
		JSONObject root = new JSONObject(jsonString);
		Type mapType = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, Object> rootMap = new Gson().fromJson(jsonString, mapType);
		if (root.get("objectType").toString().equalsIgnoreCase("plan")) {
			if (validateJson(root.toString()).equalsIgnoreCase(_VALIDATION_FAILED)) {
				return validationMessage();
			}
			
			parseJson(rootMap);
			return new ResponseEntity<String>("Plan updated", HttpStatus.OK);
		}else {
			PlanServiceImpl psImpl = new PlanServiceImpl();
			psImpl.updateObject(rootMap);
			return new ResponseEntity<String>("Plan updated", HttpStatus.OK);
		}
	}

	@DeleteMapping
	@RequestMapping(value = "/InsurancePlan/deletePlan/{id}")
	public ResponseEntity<?> deletePlan(HttpServletRequest request, @PathVariable("id") String _id) {
		logger.info("Fetching & Deleting Plan with id {}", _id);
		String bearerToken = request.getHeader("Authorization");
		if (!encUtil.decrypt(bearerToken)) {
			return new ResponseEntity<String>("Invalid bearer token", HttpStatus.UNAUTHORIZED);
		}
		if (planService.deletePlan(_id) > 0) {
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
			JSONObject jsonSchema = new JSONObject(IOUtils.toString(inputStream));
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

	@SuppressWarnings("unchecked")
	public String parseJson(Map<String, Object> root) throws JSONException {
		logger.info("Parsing payload");
		HashMap<String, Object> childObjectMap = new HashMap<>();
		//	HashMap<String, ArrayList<String>> childObjectList = new HashMap<>();
		HashMap<String, Object> rootMapAttributes = new HashMap<>();
		JSONObject rootObj = new JSONObject((Map) root);
		String rootKey1 = null;
		String rootKey2 = null;
		for (Entry<String, Object> rootMap : root.entrySet()) {

			if (rootMap.getValue() instanceof Map) {
				checkMap((Map<String, Object>) rootMap.getValue());
				JSONObject obj = new JSONObject((Map) rootMap.getValue());
				rootKey2 = rootObj.get("objectType") + "__" + rootObj.get("objectId") + "__" + rootMap.getKey();
				childObjectMap.put(rootKey2, obj.get("objectType") + "__" + obj.get("objectId"));
				System.out.println("Set::::1 " + childObjectMap);
				saveRelationshipMap(childObjectMap);
				childObjectMap.clear();
			} else if (rootMap.getValue() instanceof List) {
				String key = rootObj.get("objectType") + "__" + rootObj.get("objectId") + "__" + rootMap.getKey();
				rootKey1 = key;
				checkList((List<Object>) rootMap.getValue(), key);
			} else if (!(rootMap.getValue() instanceof List) && !(rootMap.getValue() instanceof Map)) {
				rootMapAttributes.put(rootMap.getKey(), rootMap.getValue());
			}
		}
		ArrayList<String> rootKeyList = new ArrayList<>();
		rootKeyList.add(rootKey1);
		rootKeyList.add(rootKey2);
		childObjectMap.put(rootObj.get("objectType") + "__" + rootObj.get("objectId") + "__", rootKeyList);
		System.out.println("Set::::4 " + childObjectMap);
		saveRelationshipMap(childObjectMap);
		String rootKey = saveJsonObject(rootMapAttributes);
		return rootKey;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void checkMap(Map<String, Object> map) throws JSONException {
		HashMap<String, Object> saveMap = new HashMap<>();
		HashMap<String, Object> childObjectMap = new HashMap<>();
		JSONObject rootObj = new JSONObject((Map) map);
		for (Entry<String, Object> tmpMap : map.entrySet()) {
			if (tmpMap.getValue() instanceof Map) {
				checkMap((Map<String, Object>) tmpMap.getValue());
				JSONObject obj = new JSONObject((Map) tmpMap.getValue());
				childObjectMap.put(rootObj.get("objectType") + "__" + rootObj.get("objectId") + "__" + tmpMap.getKey(),
						obj.get("objectType") + "__" + obj.get("objectId"));
			} else if (tmpMap.getValue() instanceof List) {
				checkList((List<Object>) tmpMap.getValue(), null);
			} else if (!(tmpMap.getValue() instanceof List) && !(tmpMap.getValue() instanceof Map)) {
				saveMap.put(tmpMap.getKey(), tmpMap.getValue());
			}
		}
		if (!childObjectMap.isEmpty()) {
			saveRelationshipMap(childObjectMap);
			System.out.println("Set::::2 " + childObjectMap);
		}
		saveJsonObject(saveMap);

	}

	@SuppressWarnings("unchecked")
	public void checkList(List<Object> listMap, String keyName) throws JSONException {
		HashMap<String, Object> childObjectMap = new HashMap<>();
		ArrayList<String> tmpList = new ArrayList<>();
		for (Object tmpMap : listMap) {
			if (tmpMap instanceof Map) {
				JSONObject rootObj = new JSONObject((Map) tmpMap);
				Set<String> key = new Gson().fromJson(rootObj.toString(), HashMap.class).keySet();
				Object[] keyList = key.toArray();
				tmpList.add(rootObj.get("objectType") + "__" + rootObj.get("objectId") + "__" + keyList[0]);
				tmpList.add(rootObj.get("objectType") + "__" + rootObj.get("objectId") + "__" + keyList[1]);
				tmpList.add(rootObj.get("objectType") + "__" + rootObj.get("objectId"));
				childObjectMap.put(keyName, tmpList);

				checkMap((Map<String, Object>) tmpMap);
			}
		}
		saveRelationshipMap(childObjectMap);
		System.out.println("Set::::3 " + childObjectMap.toString());
	}

	public String saveJsonObject(HashMap<String, Object> parsedMap) {
		return planService.savePlanObjMap(parsedMap);
	}

	public void saveRelationshipMap(HashMap<String, Object> relationshipMap) {
		try {
			planService.saveRelationshipMap(relationshipMap);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
