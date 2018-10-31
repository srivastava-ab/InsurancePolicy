package com.example.abhinav.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;

public interface PlanService {
	
	Set<String> findPlanById(String key);
	Set<String> savePlan(String Key, JsonElement Value);
	Long deletePlan(String key);
	boolean updatePlanById(String Key, JsonElement Value);
	String savePlanInMap(Map<String, Object> map);
}
