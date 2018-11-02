package com.example.abhinav.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import com.google.gson.JsonElement;

public interface PlanService {
	
	String findPlanById(String key);
	Set<String> savePlan(String Key, JsonElement Value);
	Long deletePlan(String key);
	boolean updatePlanById(String Key, JsonElement Value);
	String savePlanObjMap(Map<String, Object> map);
	void saveRelationshipMap(HashMap<String, Object> relationshipMap) throws JSONException;
}
