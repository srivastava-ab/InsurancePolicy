package com.example.abhinav.service;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class PlanServiceImpl implements PlanService {

	public static final Logger logger = LoggerFactory.getLogger(PlanServiceImpl.class);
	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	private static JedisPool pool = null;

	public PlanServiceImpl() {
		// configure our pool connection
		pool = new JedisPool(redisHost, redisPort);
	}

	@Override
	public Set<String> findPlanById(String key) {
		logger.info("inside find method, key is " + key);
		Jedis jedis = pool.getResource();
		// after saving the data, lets retrieve them to be sure that it has really added in redis
		Set<String> members = jedis.smembers(key);
		for (String member : members) {
			System.out.println(member);
		}
		return members;
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
		Jedis jedis = pool.getResource();
		return jedis.del(key);
	}

}
