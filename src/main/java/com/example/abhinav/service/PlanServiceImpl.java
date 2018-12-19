package com.example.abhinav.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.abhinav.elasticsearch.ElasticSearchUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class PlanServiceImpl implements PlanService {

	public static final Logger logger = LoggerFactory.getLogger(PlanServiceImpl.class);
	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	private static JedisPool pool = null;
	Jedis jedis = null;

	public PlanServiceImpl() {
		// configure our pool connection
		pool = new JedisPool(redisHost, redisPort);
		jedis = pool.getResource();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> savePlan(String key, JsonElement root) {
		Jedis jedis = pool.getResource();
		logger.info("Inside save method :" + key);
		Set<String> members = new HashSet<>();
		if (jedis.exists(key)) {
			System.out.println("Record already present");
			return members;
		}
		jedis.sadd(key, root.toString());
		members = jedis.smembers(key);
		for (String member : members) {
			System.out.println(member);
		}
		return members;
	}

	@Override
	public boolean updatePlanById(String key, JsonElement root) {
		Jedis jedis = pool.getResource();
		logger.info("Inside update method :" + key);
		String members = root.toString();
		if (!jedis.exists(key)) {
			System.out.println("Id not found");
			return false;
		}
		deletePlan(key);
		jedis.sadd(key, members);
		System.out.println("Updated plan is::");
		System.out.println(jedis.smembers(key));
		return true;
	}

	@Override
	public Long deletePlan(String key) {
		ElasticSearchUtility esu = new ElasticSearchUtility();
		esu.delIndex(key);
		jedis.del("plan__" + key + "__");
		return jedis.del("plan__" + key);
	}

	@Override
	public String savePlanObjMap(Map<String, Object> map) {
		Map<String, String> newMap = new HashMap<String, String>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() instanceof String) {
				newMap.put(entry.getKey(), (String) entry.getValue());
			} else if (entry.getValue() instanceof String) {
				newMap.put(entry.getKey(), (String) entry.getValue());
			} else if (entry.getValue() instanceof Double) {
				newMap.put(entry.getKey(), String.valueOf(entry.getValue()));
			} else if (entry.getValue() instanceof Boolean) {
				newMap.put(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}
		String key = newMap.get("objectType") + "__" + newMap.get("objectId");
		if (jedis.exists(key)) {

		}
		String output = jedis.hmset(key, newMap);
		ElasticSearchUtility eu = new ElasticSearchUtility();
		try {
			System.out.println("Going to ES REQ");
			eu.req(map);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return newMap.get("objectType") + "__" + newMap.get("objectId");
	}

	@Override
	public void saveRelationshipMap(HashMap<String, Object> map) throws JSONException {
		for (Entry<String, Object> es : map.entrySet()) {
			if (es.getValue().toString().startsWith("[") && es.getValue().toString().endsWith("]")) {
				jedis.sadd(es.getKey(), StringUtils.substringBetween(es.getValue().toString(), "[", "]"));
				break;
			}
			jedis.sadd(es.getKey(), es.getValue().toString());
		}
	}

	@Override
	public String findPlanById(String str) {
		ArrayList<String> nodeList = new ArrayList<>();
		JSONObject rootObj = new JSONObject();
		String prettyJsonString = null;
		if (checkSet(str + "__", nodeList).size() == 1) {
			return "not found";
		}
		try {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.putAll((Map<? extends String, ? extends Object>) checkMap(str));
			JSONArray array = new JSONArray();
			JSONObject obj1 = new JSONObject();
			int nodeSize = nodeList.size() - 1;
			for (int i = 0; i <= nodeSize; i++) {
				if (i % 3 == 0 && i != nodeSize) {
					obj1.put("linkedService", checkMap(nodeList.get(i)));
				} else if (i % 3 == 1 && i != nodeSize) {
					obj1.put("planserviceCostShares", checkMap(nodeList.get(i)));
				} else if (i % 3 == 2 && i != nodeSize) {
					obj1 = putMapInObj(checkMap(nodeList.get(i)), obj1);
					array.put(obj1);
					obj1 = new JSONObject();
				} else if (i == nodeSize) {
					jsonMap.put("planCostShares", checkMap(nodeList.get(i)));
				}
			}

			jsonMap.put("linkedPlanServices", array);
			rootObj.put("test", jsonMap);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jp = new JsonParser();
			System.out.println("root is:: " + rootObj);
			JsonElement je = jp.parse(rootObj.get("test").toString());
			prettyJsonString = gson.toJson(je);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return prettyJsonString;

	}

	public Map<String, String> checkMap(String key) {
		Map<String, String> resultMap = jedis.hgetAll(key.trim());
		return resultMap;
	}

	public ArrayList<String> checkSet(String key, ArrayList<String> nodeList) {
		Set<String> tmpSet = jedis.smembers(key.trim());
		// System.out.println("Set result --> " + tmpSet.toString());
		String[] edgeList = modifyEdge(tmpSet.toString());
		for (String str : edgeList) {
			if (str.split("__").length == 3) {
				checkSet(str, nodeList);
			} else {
				nodeList.add(str);
			}
		}
		return nodeList;
	}

	public String[] modifyEdge(String str) {
		String abc[] = null;
		if (str.startsWith("[") && str.endsWith("]")) {
			str = StringUtils.substringBetween(str, "[", "]");
			abc = str.split(",");
		}
		return abc;
	}

	public JSONObject putMapInObj(Map<String, String> map, JSONObject obj) throws JSONException {

		for (Entry<String, String> entry : map.entrySet()) {
			obj.put(entry.getKey(), entry.getValue());
		}
		return obj;
	}

	public boolean updateObject(Map<String, Object> map) {
		Map<String, String> newMap = map.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
		String key = newMap.get("objectType") + "__" + newMap.get("objectId");
		System.out.println("Map:: " + newMap);
		String output = jedis.hmset(key, newMap);
		return true;

	}
}
