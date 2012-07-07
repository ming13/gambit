package app.android.gambit.remote;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.text.TextUtils;
import app.android.gambit.InternetDateTime;
import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;


public class GoogleDriveHelper
{
	private static final String MIME_XLS = "application/vnd.ms-excel";
	private static final String MIME_GOOGLE_SPREADSHEET = "application/vnd.google-apps.spreadsheet";

	private static final String URL_OAUTH_TOKEN_PARAMETER = "oauth_token";

	private static final String RESPONSE_FIELD_ID = "id";
	private static final String RESPONSE_FIELD_EXPORT_LINKS = "exportLinks";
	private static final String RESPONSE_FIELD_MODIFIED_DATE = "modifiedDate";
	private static final String RESPONSE_FIELD_PERMISSION_ID = "permissionId";
	private static final String RESPONSE_FIELD_ROLE = "role";

	private static final String RESPONSE_LIST_ITEMS_PREFIX = "items";
	private static final String RESPONSE_FIELDS_DELIMITER = ",";

	private static final String PERMISSION_ROLE_OWNER = "owner";
	private static final String PERMISSION_ROLE_WRITER = "writer";

	private Drive driveService;

	private String authToken;

	public GoogleDriveHelper(String authToken, String apiKey) {
		this.authToken = authToken;

		driveService = buildDriveService(authToken, apiKey);
	}

	private Drive buildDriveService(String authToken, String apiKey) {
		HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		JsonHttpRequestInitializer requestInitializer = new DriveRequestInitializer(authToken, apiKey);

		Drive.Builder driveServiceBuilder = new Drive.Builder(httpTransport, jsonFactory, null);
		driveServiceBuilder.setJsonHttpRequestInitializer(requestInitializer);

		return driveServiceBuilder.build();
	}

	private static class DriveRequestInitializer implements JsonHttpRequestInitializer
	{
		private final String authToken;
		private final String apiKey;

		public DriveRequestInitializer(String authToken, String apiKey) {
			this.authToken = authToken;
			this.apiKey = apiKey;
		}

		@Override
		public void initialize(JsonHttpRequest jsonHttpRequest) throws IOException {
			DriveRequest driveRequest = (DriveRequest) jsonHttpRequest;

			driveRequest.setOauthToken(authToken);
			driveRequest.setKey(apiKey);
		}
	}

	public String createSpreadsheet(String spreadsheetName, byte[] xlsData) {
		File spreadsheetFile = buildSpreadsheetFile(spreadsheetName);
		AbstractInputStreamContent xlsContent = buildXlsContent(xlsData);

		return createSpreadsheet(spreadsheetFile, xlsContent);
	}

	private File buildSpreadsheetFile(String spreadsheetName) {
		File file = new File();

		file.setTitle(spreadsheetName);

		return file;
	}

	private AbstractInputStreamContent buildXlsContent(byte[] xlsData) {
		return new ByteArrayContent(MIME_XLS, xlsData);
	}

	private String createSpreadsheet(File spreadsheetFile, AbstractInputStreamContent xlsContent) {
		try {
			Drive.Files.Insert insertRequest = driveService.files().insert(spreadsheetFile, xlsContent);

			insertRequest.setConvert(Boolean.TRUE);
			insertRequest.setFields(buildResponseFields(RESPONSE_FIELD_ID));

			return insertRequest.execute().getId();
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	private String buildResponseFields(String... responseFields) {
		return TextUtils.join(RESPONSE_FIELDS_DELIMITER, responseFields);
	}

	public void updateSpreadsheet(String spreadsheetKey, byte[] xlsData) {
		try {
			AbstractInputStreamContent xlsContent = buildXlsContent(xlsData);

			Drive.Files.Update updateRequest = driveService.files().update(spreadsheetKey, null,
				xlsContent);

			updateRequest.setConvert(Boolean.TRUE);
			updateRequest.setFields(buildResponseFields(RESPONSE_FIELD_ID));

			updateRequest.execute();
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	public InputStream downloadXlsData(String spreadsheetKey) {
		File spreadsheetFile = getFile(spreadsheetKey,
			buildResponseFields(RESPONSE_FIELD_EXPORT_LINKS));
		GenericUrl xlsExportUrl = getXlsExportUrl(spreadsheetFile);

		return downloadFileContent(xlsExportUrl);
	}

	private File getFile(String spreadsheetKey, String responseFields) {
		try {
			Drive.Files.Get getRequest = driveService.files().get(spreadsheetKey);

			getRequest.setFields(responseFields);

			return getRequest.execute();
		}
		catch (HttpResponseException e) {
			throw buildExceptionFromHttpStatusCode(e.getStatusCode());
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	private RuntimeException buildExceptionFromHttpStatusCode(int httpStatusCode) {
		switch (httpStatusCode) {
			case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
				return new FileNotExistsException();

			default:
				return new SyncException();
		}
	}

	private GenericUrl getXlsExportUrl(File spreadsheetFile) {
		if (!spreadsheetFile.getExportLinks().keySet().contains(MIME_XLS)) {
			throw new SyncException("No XLS export for file provided");
		}

		return new GenericUrl(spreadsheetFile.getExportLinks().get(MIME_XLS));
	}

	private InputStream downloadFileContent(GenericUrl fileUrl) {
		try {
			fileUrl.set(URL_OAUTH_TOKEN_PARAMETER, authToken);

			HttpRequest getRequest = driveService.getRequestFactory().buildGetRequest(fileUrl);

			return getRequest.execute().getContent();
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	public String getNewestSpreadsheetKey(String spreadsheetName) {
		List<File> spreadsheetsWithName = getSpreadsheetsWithName(spreadsheetName);

		if (spreadsheetsWithName.isEmpty()) {
			throw new FileNotExistsException();
		}

		Collections.sort(spreadsheetsWithName,
			Collections.reverseOrder(new FileByModifiedDateComparator()));

		for (File spreadsheetFile : spreadsheetsWithName) {
			if (isFileWritable(spreadsheetFile)) {
				return spreadsheetFile.getId();
			}
		}

		throw new FileNotExistsException();
	}

	private List<File> getSpreadsheetsWithName(String spreadsheetName) {
		try {
			Drive.Files.List listRequest = driveService.files().list();

			listRequest.setQ(buildFileSelectionQuery(spreadsheetName));
			listRequest.setFields(buildResponseFieldsList(
				buildResponseFields(RESPONSE_FIELD_ID, RESPONSE_FIELD_MODIFIED_DATE)));

			return listRequest.execute().getItems();
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	private String buildFileSelectionQuery(String spreadsheetName) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append("trashed = false");
		queryBuilder.append(" and ");
		queryBuilder.append(String.format("mimeType = '%s'", MIME_GOOGLE_SPREADSHEET));
		queryBuilder.append(" and ");
		queryBuilder.append(String.format("title = '%s'", escapeSingleQuote(spreadsheetName)));

		return queryBuilder.toString();
	}

	private String escapeSingleQuote(String string) {
		return string.replace("'", "\\'");
	}

	private String buildResponseFieldsList(String responseFields) {
		return String.format("%s(%s)", RESPONSE_LIST_ITEMS_PREFIX, responseFields);
	}

	private static class FileByModifiedDateComparator implements Comparator<File>
	{
		@Override
		public int compare(File firstSpreadsheetFile, File secondSpreadsheetFile) {
			InternetDateTime firstFileModifiedTime = new InternetDateTime(
				firstSpreadsheetFile.getModifiedDate().toStringRfc3339());
			InternetDateTime secondFileModifiedTime = new InternetDateTime(
				secondSpreadsheetFile.getModifiedDate().toStringRfc3339());

			if (firstFileModifiedTime.isBefore(secondFileModifiedTime)) {
				return -1;
			}
			else if (firstFileModifiedTime.isAfter(secondFileModifiedTime)) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	private boolean isFileWritable(File file) {
		String userPermissionId = getUserPermissionId();

		for (Permission permission : getFilePermissions(file)) {
			if (isUserHaveWritablePermission(userPermissionId, permission)) {
				return true;
			}
		}

		return false;
	}

	private String getUserPermissionId() {
		try {
			Drive.About.Get getRequest = driveService.about().get();

			getRequest.setFields(buildResponseFields(RESPONSE_FIELD_PERMISSION_ID));

			return getRequest.execute().getPermissionId();
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	private List<Permission> getFilePermissions(File file) {
		try {
			Drive.Permissions.List getRequest = driveService.permissions().list(file.getId());

			getRequest.setFields(
				buildResponseFieldsList(buildResponseFields(RESPONSE_FIELD_ID, RESPONSE_FIELD_ROLE)));

			return getRequest.execute().getItems();
		}
		catch (IOException e) {
			throw new SyncException();
		}
	}

	private boolean isUserHaveWritablePermission(String userPermissionId, Permission permission) {
		if (!permission.getId().equals(userPermissionId)) {
			return false;
		}

		if (permission.getRole().equals(PERMISSION_ROLE_OWNER)) {
			return true;
		}

		return permission.getRole().equals(PERMISSION_ROLE_WRITER);
	}

	public InternetDateTime getSpreadsheetUpdateTime(String spreadsheetKey) {
		File spreadsheetFile = getFile(spreadsheetKey,
			buildResponseFields(RESPONSE_FIELD_MODIFIED_DATE));

		return new InternetDateTime(spreadsheetFile.getModifiedDate().toStringRfc3339());
	}
}
