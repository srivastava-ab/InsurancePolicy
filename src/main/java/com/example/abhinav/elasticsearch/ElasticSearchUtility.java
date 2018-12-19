package com.example.abhinav.elasticsearch;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchUtility {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchUtility.class);
	RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));

	@SuppressWarnings("deprecation")
	public void req(Map<String, Object> map) throws IOException {
		System.out.println("Root length from ES--->> " + map.size());
		IndexRequest jsonObjMap = new IndexRequest("policy", "policy",
				(String) (map.get("objectType") + "__" + map.get("objectId"))).source(map);
		if (((String) map.get("objectType")).equalsIgnoreCase("plan")) {
		}
		jsonObjMap.opType(DocWriteRequest.OpType.INDEX);
		jsonObjMap.source(map, XContentType.JSON);

		System.out.println("JsonObj--> " + jsonObjMap);

		IndexResponse indexResponse3 = client.index(jsonObjMap, RequestOptions.DEFAULT);
		System.out.println("ES response is --> " + indexResponse3);

		log.info("Index has been created successfully with ES: ");
	}
	
	public void delIndex(String id) {
		DeleteRequest delReq = new DeleteRequest("policy", "policy","plan__"+id);
		
		try {
			client.delete(delReq);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
