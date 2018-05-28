package com.opswat.metadefender.core.client;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opswat.metadefender.core.client.exceptions.MetadefenderClientException;
import com.opswat.metadefender.core.client.responses.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


/**
 * Main class for the REST API client
 */
public class MetadefenderCoreClient {

	public static final String DATE_FORMAT_MILLIS_RESOLUTION = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	public static final String DATE_FORMAT_DAY_RESOLUTION = "MM/dd/yyyy";

	private HttpConnector httpConnector = new HttpConnector();

	private final String apiEndPointUrl;


	private String sessionId = null;

	// default user_agent for fileScan-s
	private String user_agent = null;


	/**
	 * Constructor for the client.
	 * If you use this constructor, you wont be logged in. (You can use several api calls without authentication)
	 * @param apiEndPointUrl Format: protocol://host:port  Example value: http://localhost:8008
	 */
	public MetadefenderCoreClient(String apiEndPointUrl) {
		this.apiEndPointUrl = apiEndPointUrl;
	}

	/**
	 * Constructs a rest client with an api key, to access protected resources.
	 *
	 * @param apiEndPointUrl Format: protocol://host:port  Example value: http://localhost:8008
	 * @param apiKey valid api key
	 */
	public MetadefenderCoreClient(String apiEndPointUrl, String apiKey) {
		this.apiEndPointUrl = apiEndPointUrl;

		this.sessionId = apiKey;
	}


	/**
	 * Constructs a rest client with an api authentication.
	 *
	 * @param apiEndPointUrl Format: protocol://host:port  Example value: http://localhost:8008
	 * @param userName username to login with
	 * @param password password to login with
	 * @throws MetadefenderClientException if the provided user/pass is not valid.
	 */
	public MetadefenderCoreClient(String apiEndPointUrl, String userName, String password) throws MetadefenderClientException {
		this.apiEndPointUrl = apiEndPointUrl;

		login(userName, password);
	}

	/**
	 * You can set your custom HttpConnector instance to use for making all the requests to the API endpoint.
	 * This can be handy if You want to handle special connection issues yourself.
	 *
	 * @param httpConnector your custom HttpConnector
	 */
	public void setHttpConnector(HttpConnector httpConnector) {
		if(httpConnector == null) {
			throw new InvalidParameterException("httpConnector cannot be null");
		}

		this.httpConnector = httpConnector;
	}

	/**
	 * For setting a default user agent string for all file scan api calls.
	 * @param user_agent custom user agent string
	 */
	public void setUserAgent(String user_agent) {
		this.user_agent = user_agent;
	}

	/**
	 * If you successfully log in with username/password you will get a session id.
	 *
	 * @return The current session id.
	 */
	public String getSessionId() {
		return this.sessionId;
	}


	/**
	 * Initiate a new session for using protected REST APIs.
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_login.html" target="_blank">REST API doc</a>
	 *
	 * @param userName username to login with
	 * @param password password to login with
	 * @throws MetadefenderClientException
	 */
	public void login(String userName, String password) throws MetadefenderClientException {

		ObjectMapper mapper = getObjectMapper();
		ObjectNode loginJson = mapper.createObjectNode();
		loginJson.put("user", userName);
		loginJson.put("password", password);

		String body = loginJson.toString();

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/login", "POST", body.getBytes());

		if(response.responseCode == 200) {
			JsonNode actualObj = getJsonFromString(response.response);
			this.sessionId = actualObj.get("session_id").asText();
		} else {
			throwRequestError(response);
		}
	}

	/**
	 * Get the current session state.
	 *
	 * @return TRUE if the current session is valid, FALSE otherwise.
	 * @throws MetadefenderClientException
	 */
	public boolean validateCurrentSession() throws MetadefenderClientException {
		if(this.sessionId == null || this.sessionId.trim().length() <= 0) {
			return false;
		}

		System.out.println("Header: " + getLoggedInHeader());
		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/version", "GET", null, getLoggedInHeader());

		return response.responseCode == 200;
	}

	/**
	 * Scan is done asynchronously and each scan request is tracked by data id of which result can be retrieved by API Fetch Scan Result.
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_scan_file.html" target="_blank">REST API doc</a>
	 *
	 * @param inputStream Stream of data (file) to scan. Required
	 * @param fileScanOptions Optional file scan options. Can be NULL.
	 * @return unique data id for this file scan
	 */
	public String scanFile(InputStream inputStream, FileScanOptions fileScanOptions) throws MetadefenderClientException {
		if(inputStream == null) {
			throw new MetadefenderClientException("Stream cannot be null");
		}

		if( (fileScanOptions != null && fileScanOptions.getUserAgent() == null) && this.user_agent != null) {
			fileScanOptions.setUserAgent(this.user_agent);
		}

		if(fileScanOptions == null && this.user_agent != null) {
			fileScanOptions = new FileScanOptions().setUserAgent(this.user_agent);
		}


		Map<String, String> headers = (fileScanOptions == null) ? null : fileScanOptions.getOptions();

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/file", "POST", inputStream, headers);

		if(response.responseCode == 200) {
			JsonNode actualObj = getJsonFromString(response.response);
			return actualObj.get("data_id").asText();
		} else {
			throwRequestError(response);
			return null;
		}
	}

	/**
	 * Scan file in synchron mode.
	 * Note: this method call will block your thread until the file scan finishes.
	 *
	 * @param inputStream input stream to scan
	 * @param fileScanOptions optional file scan options
	 * @param pollingInterval polling time in millis
	 * @param timeout timeout in millis
	 * @return FileScanResult
	 * @throws MetadefenderClientException
	 */
	public FileScanResult scanFileSync(InputStream inputStream, FileScanOptions fileScanOptions, int pollingInterval, int timeout) throws MetadefenderClientException, InterruptedException, ExecutionException, TimeoutException {
		String data_id = scanFile(inputStream, fileScanOptions);

		FileScanResult fileScanResult = null;

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<FileScanResult> future = executor.submit(new FetchScanResultTask(data_id, pollingInterval));

		try {
			fileScanResult = future.get(timeout, TimeUnit.MILLISECONDS);
		} finally {
			future.cancel(true);
			executor.shutdownNow();
		}

		return fileScanResult;
	}


	/**
	 * Retrieve scan results.
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_fetch_scan_result.html" target="_blank"v>REST API doc</a>
	 *
	 * @param data_id Unique file scan id. Required.
	 * @return File scan result object
	 * @throws MetadefenderClientException
	 */
	public FileScanResult fetchScanResult(String data_id) throws MetadefenderClientException {
		if(data_id == null || data_id.trim().length()<= 0){
			throw new MetadefenderClientException("data_id is required");
		}

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/file/" + data_id, "GET");

		if(response.responseCode == 200) {
			JsonNode actualObj = getJsonFromString(response.response);
			// data_id is not found check
			if(actualObj.has(data_id)) {
				throw new MetadefenderClientException(actualObj.get(data_id).asText(), response.responseCode);
			} else {
				// success response
				return getObjectFromJson(response.response, FileScanResult.class);
			}

		} else {
			throwRequestError(response);
			return null;
		}

	}

	/**
	 * Fetch Scan Result by File Hash
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_fetch_scan_result_by_file_hash.html" target="_blank">REST API doc</a>
	 *
	 * @param hash {md5|sha1|sha256 hash}
	 * @return File scan result object
	 * @throws MetadefenderClientException
	 */
	public FileScanResult fetchScanResultByHash(String hash) throws MetadefenderClientException {
		if(hash == null || hash.trim().length() <= 0) {
			throw new MetadefenderClientException("Hash is required");
		}

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/hash/" + hash, "GET");

		if(response.responseCode == 200) {
			JsonNode actualObj = getJsonFromString(response.response);
			// data_id is not found check
			if(actualObj.has(hash)) {
				throw new MetadefenderClientException(actualObj.get(hash).asText(), response.responseCode);
			} else {
				// success response
				return getObjectFromJson(response.response, FileScanResult.class);
			}


		} else {
			throwRequestError(response);
			return null;
		}

	}

	/**
	 * You need to be logged in to access this API point.
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_getlicense.html" target="_blank">REST API doc</a>
	 *
	 * @return License object
	 * @throws MetadefenderClientException
	 */
	public License getCurrentLicenseInformation() throws MetadefenderClientException {
		checkSession();

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/admin/license", "GET", null, getLoggedInHeader());

		if(response.responseCode == 200) {
			return getObjectFromJson(response.response, License.class);
		} else {
			throwRequestError(response);
			return null;
		}

	}

	/**
	 * Fetching Engine/Database Versions
	 * The response is an array of engines with database information.
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_fetching_engine_database_versions.html" target="_blank">REST API doc</a>
	 *
	 * @return List of Engine versions
	 * @throws MetadefenderClientException
	 */
	public List<EngineVersion> getEngineVersions() throws MetadefenderClientException {
		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/stat/engines", "GET");

		if(response.responseCode == 200) {
			return getCollectionFromJson(response.response, new TypeReference<List<EngineVersion>>() {});
		} else {
			throwRequestError(response);
			return null;
		}

	}


	/**
	 * You need to be logged in to access this API point.
	 *
	 * @return ApiVersion object
	 * @throws MetadefenderClientException
	 */
	public ApiVersion getVersion() throws MetadefenderClientException {
		checkSession();

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/version", "GET", null, getLoggedInHeader());

		if(response.responseCode == 200) {
			return getObjectFromJson(response.response, ApiVersion.class);
		} else {
			throwRequestError(response);
			return null;
		}
	}

	/**
	 * Fetching Available Scan Rules
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_fetching_available_scan_workflows.html" target="_blank">REST API doc</a>
	 *
	 * @return List of Scan rules
	 * @throws MetadefenderClientException
	 */
	public List<ScanRule> getAvailableScanRules() throws MetadefenderClientException {
		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/file/rules", "GET");

		if(response.responseCode == 200) {
			return getCollectionFromJson(response.response, new TypeReference<List<ScanRule>>() {});
		} else {
			throwRequestError(response);
			return null;
		}
	}



	/**
	 * You need to be logged in to access this API point.
	 *
	 * Destroy session for not using protected REST APIs.
	 *
	 * @see <a href="http://software.opswat.com/metascan/Documentation/Metascan_v4/documentation/user_guide_metascan_developer_guide_logout.html" target="_blank">REST API doc</a>
	 * @throws MetadefenderClientException
	 */
	public void logout() throws MetadefenderClientException {
		checkSession();

		HttpConnector.HttpResponse response = httpConnector.sendRequest(this.apiEndPointUrl + "/logout", "POST", null, getLoggedInHeader());
		this.sessionId = null;

		if(response.responseCode != 200) {
			throwRequestError(response);
		}
	}


	/**
	 * Fetch scan result task
	 */
	public class FetchScanResultTask implements Callable<FileScanResult> {

		private final String data_id;
		private final int pollingInterval;


		public FetchScanResultTask(String data_id, int pollingInterval) {
			this.data_id = data_id;
			this.pollingInterval = pollingInterval;
		}

		//@Override
		public FileScanResult call() throws Exception {
			FileScanResult fileScanResult;

			do {
				fileScanResult = fetchScanResult(data_id);

				if(!fileScanResult.isScanFinished()) {
					TimeUnit.MILLISECONDS.sleep(pollingInterval);
				}
			} while (!fileScanResult.isScanFinished());

			return fileScanResult;
		}
	}



	//////// Private utils methods:


	private Map<String, String> getLoggedInHeader() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("apikey", this.sessionId);
		return headers;
	}

	/**
	 * Generic error handling for API request where response code != 200
	 * @param response actual API response
	 * @throws MetadefenderClientException
	 */
	private void throwRequestError(HttpConnector.HttpResponse response) throws MetadefenderClientException {
		JsonNode actualObj = getJsonFromString(response.response);
		String errorMessage = actualObj.get("err").asText();
		throw new MetadefenderClientException(errorMessage, response.responseCode);
	}

	/**
	 * Reusable util method for current session exist check
	 * @throws MetadefenderClientException if session is empty
	 */
	private void checkSession() throws MetadefenderClientException {
		if(this.sessionId == null || this.sessionId.trim().length() <=0) {
			throw new MetadefenderClientException("You need to be logged in to access this API point.");
		}
	}


	private ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	private JsonNode getJsonFromString(String json) throws MetadefenderClientException {
		ObjectMapper mapper = getObjectMapper();

		try {
			return mapper.readTree(json);
		} catch (IOException e) {
			throw new MetadefenderClientException("Cannot parse json: " + e.getMessage());
		}
	}

	private <T> T getObjectFromJson(String json, Class<T> clazz) throws MetadefenderClientException {
		ObjectMapper mapper = getObjectMapper();

		try {
			return mapper.readValue(json, clazz);
		} catch (IOException e) {
			throw new MetadefenderClientException("Cannot parse json: " + e.getMessage());
		}
	}

	private <T> List<T> getCollectionFromJson(String json, TypeReference<List<T>> typeReference) throws MetadefenderClientException {
		try {
			ObjectMapper mapper = getObjectMapper();
			return mapper.readValue(json, typeReference);
		} catch (IOException e) {
			throw new MetadefenderClientException("Cannot parse json: " + e.getMessage());
		}
	}


}
