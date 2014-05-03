package eu.broth.intellilab.util;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Bastian Roth
 * @version 02.05.2014
 */
public class Params {

	private List<BasicNameValuePair> pairs;

	public Params() {
		pairs = new LinkedList<>();
	}

	public Params(String key, String value) {
		this();
		add(key, value);
	}

	public Params add(String key, String value) {
		BasicNameValuePair pair = new BasicNameValuePair(key, value);
		pairs.add(pair);
		return this;
	}

	public String format() {
		return URLEncodedUtils.format(pairs, "utf-8");
	}
}
