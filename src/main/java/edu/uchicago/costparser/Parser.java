package edu.uchicago.costparser;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.event.JSONEvent;
import org.apache.flume.source.http.HTTPBadRequestException;
import org.apache.flume.source.http.HTTPSourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class Parser implements HTTPSourceHandler {
	private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

	private final Type listType = new TypeToken<List<JSONEvent>>() {
	}.getType();
	private final Gson gson;

	// private means that only Builder can build me.
	public Parser() {
		gson = new GsonBuilder().disableHtmlEscaping().create();
	}

	// public void initialize() {}
	//
	// public Event intercept(Event event) {
	// JsonParser parser = new JsonParser();
	// Gson gson = new GsonBuilder().create();
	//
	// LOG.debug("message intercepted...");
	//
	// // Map<String, String> headers = event.getHeaders();
	// String body = new String(event.getBody());
	//
	// JsonElement obj;
	// try {
	// obj = parser.parse(body).getAsJsonObject();
	// } catch (JsonSyntaxException e) {
	// LOG.error("problem in parsing msg body");
	// return null;
	// }
	//
	// for(Map.Entry<String,JsonElement> entry :
	// obj.getAsJsonObject().entrySet() ){
	// JsonObject payload = entry.getValue().getAsJsonObject();
	// // headers.put("target_filename", entry.getKey());
	// //
	// // if(payload.has("timestamp")) {
	// // Long timestamp = payload.get("timestamp").getAsLong();
	// // headers.put("timestamp", timestamp.toString());
	// // } else if(payload.has("time")) {
	// // Long timestamp = payload.get("time").getAsLong();
	// // headers.put("timestamp", timestamp.toString());
	// // }
	// //
	// try {
	// event.setBody(gson.toJson(payload).getBytes("UTF-8"));
	// } catch (java.io.UnsupportedEncodingException e) {
	// LOG.error("problem in setting msg body");
	// }
	// }
	//
	// return event;
	// }

	public void configure(Context context) {
	}

	// public List<Event> getEvents(HttpServletRequest request) throws
	// HTTPBadRequestException, Exception {
	// // TODO Auto-generated method stub
	// return null;
	// }

	public List<Event> getEvents(HttpServletRequest request) throws Exception {
		LOG.warn("message received...");
		BufferedReader reader = request.getReader();
		String charset = request.getCharacterEncoding();
		// UTF-8 is default for JSON. If no charset is specified, UTF-8 is to
		// be assumed.
		if (charset == null) {
			LOG.debug("Charset is null, default charset of UTF-8 will be used.");
			charset = "UTF-8";
		} else if (!(charset.equalsIgnoreCase("utf-8") || charset.equalsIgnoreCase("utf-16") || charset.equalsIgnoreCase("utf-32"))) {
			LOG.error("Unsupported character set in request {}. " + "JSON handler supports UTF-8, " + "UTF-16 and UTF-32 only.", charset);
			throw new UnsupportedCharsetException("JSON handler supports UTF-8, " + "UTF-16 and UTF-32 only.");
		}

		/*
		 * Gson throws Exception if the data is not parseable to JSON. Need not
		 * catch it since the source will catch it and return error.
		 */
		List<Event> eventList = new ArrayList<Event>(0);
		try {
			eventList = gson.fromJson(reader, listType);
		} catch (JsonSyntaxException ex) {
			throw new HTTPBadRequestException("Request has invalid JSON Syntax.", ex);
		}

		for (Event e : eventList) {
			((JSONEvent) e).setCharset(charset);
		}
		return getSimpleEvents(eventList);
	}

	private List<Event> getSimpleEvents(List<Event> events) {
		List<Event> newEvents = new ArrayList<Event>(events.size());
		for (Event e : events) {
			newEvents.add(EventBuilder.withBody(e.getBody(), e.getHeaders()));
		}
		return newEvents;
	}

}