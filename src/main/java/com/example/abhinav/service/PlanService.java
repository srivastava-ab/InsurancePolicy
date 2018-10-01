package com.example.abhinav.service;

import java.util.Set;

import com.google.gson.JsonElement;

public interface PlanService {
	
	Set<String> findPlanById(String key);
	Set<String> savePlan(String Key, JsonElement Value);
	void updatePlanById();
	Long deletePlan(String key);

}
