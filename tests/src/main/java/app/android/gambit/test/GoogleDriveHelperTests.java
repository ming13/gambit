package app.android.gambit.test;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import android.accounts.Account;
import android.app.Activity;
import android.test.InstrumentationTestCase;
import app.android.gambit.InternetDateTime;
import app.android.gambit.remote.GoogleDriveHelper;
import app.android.gambit.remote.RemoteCard;
import app.android.gambit.remote.RemoteDeck;
import app.android.gambit.remote.RemoteDecksConverter;
import app.android.gambit.ui.AccountSelector;
import app.android.gambit.ui.Authorizer;
import app.android.gambit.ui.DeckCreationActivity;
import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.model.File;


public class GoogleDriveHelperTests extends InstrumentationTestCase
{
	private static String token;
	private static Activity hostActivity;
	public static final String MIME_GOOGLE_SPREADSHEET = "application/vnd.google-apps.spreadsheet";

	private GoogleDriveHelper driveHelper;
	private Drive driveService; // Needed to prepare some testing data

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ensureAuthorized();

		driveHelper = new GoogleDriveHelper(token, getApiKey());
		driveService = buildDriveService();
	}

	private void ensureAuthorized() {
		if (hostActivity == null) {
			hostActivity = launchActivity("app.android.gambit", DeckCreationActivity.class, null);
		}

		if (token == null) {
			Account account = AccountSelector.select(hostActivity);
			Authorizer authorizer = new Authorizer(hostActivity);
			token = authorizer.getToken(Authorizer.ServiceType.DRIVE, account);
			authorizer.invalidateToken(token);
			token = authorizer.getToken(Authorizer.ServiceType.DRIVE, account);
		}
	}

	private String getApiKey() {
		return hostActivity.getString(app.android.gambit.R.string.google_api_key);
	}

	private Drive buildDriveService() {
		HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = new JacksonFactory();

		Drive.Builder driveServiceBuilder = new Drive.Builder(httpTransport, jsonFactory, null);
		driveServiceBuilder.setJsonHttpRequestInitializer(new JsonHttpRequestInitializer()
		{
			@Override
			public void initialize(JsonHttpRequest jsonHttpRequest) throws IOException {
				DriveRequest driveRequest = (DriveRequest) jsonHttpRequest;

				driveRequest.setOauthToken(token);
				driveRequest.setKey(getApiKey());
			}
		});

		return driveServiceBuilder.build();
	}

	@Override
	protected void tearDown() throws Exception {
		hostActivity.finish();
		super.tearDown();
	}

	public void testUploadXlsData() throws IOException {
		byte[] xlsData = generateXlsData();
		String key = createNewSpreadsheet();

		// No exceptions is test pass criteria
		driveHelper.uploadXlsData(key, xlsData);
	}

	private byte[] generateXlsData() {
		RemoteDecksConverter converter = new RemoteDecksConverter();
		return converter.toXlsData(generateRemoteDecks());
	}

	public List<RemoteDeck> generateRemoteDecks() {
		final int DECKS_COUNT = 1;
		final int CARDS_COUNT = 1;

		List<RemoteDeck> decks = new ArrayList<RemoteDeck>();

		for (int deckIndex = 0; deckIndex < DECKS_COUNT; deckIndex++) {

			List<RemoteCard> cards = new ArrayList<RemoteCard>();
			for (int cardIndex = 0; cardIndex < CARDS_COUNT; cardIndex++) {
				cards.add(new RemoteCard(String.format("Front %s", cardIndex + 1),
					String.format("Back %s", cardIndex + 1)));
			}

			decks.add(new RemoteDeck(String.format("Deck %s", deckIndex + 1), cards));
		}

		return decks;
	}

	public void testCreateSpreadsheetFromXlsData() throws IOException {
		byte[] xlsData = generateXlsData();

		// No exceptions is test pass criteria
		driveHelper.createSpreadsheetFromXlsData("New spreadsheet", xlsData);
	}

	public void testDownloadXlsData() throws IOException {
		String key = createNewSpreadsheet();

		InputStream xlsDataInputStream = driveHelper.downloadXlsData(key);

		ensureXlsDataCorrect(xlsDataInputStream);
	}

	private String createNewSpreadsheet() throws IOException {
		return createNewSpreadsheet("Test file");
	}

	private String createNewSpreadsheet(String spreadsheetName) throws IOException {
		File file = new File();
		file.setTitle(spreadsheetName);
		file.setMimeType(MIME_GOOGLE_SPREADSHEET);

		return driveService.files().insert(file).execute().getId();
	}

	private void ensureXlsDataCorrect(InputStream xlsData) {
		RemoteDecksConverter converter = new RemoteDecksConverter();

		// This will throw if xls data is invalid
		@SuppressWarnings("unused")
		List<RemoteDeck> decks = converter.fromXlsData(xlsData);
	}

	public void testGetNewestSpreadsheetKeyByName() throws IOException {
		final String SPREADSHEET_NAME = "Test spreadsheet";

		String realKey = createNewSpreadsheet(SPREADSHEET_NAME);
		String obtainedKey = driveHelper.getNewestSpreadsheetKeyByName(SPREADSHEET_NAME);

		assertEquals(realKey, obtainedKey);
	}

	public void testGetSpreadsheetUpdateTime() throws IOException, InterruptedException {
		// We need time delta because we're going to compare local date with
		// date on Google servers. There obviously might be some difference.
		final int TIME_DELTA_IN_SECONDS = 60;

		InternetDateTime beforeCreation = addSecondsToDateTime(new InternetDateTime(),
			-TIME_DELTA_IN_SECONDS);

		String key = createNewSpreadsheet();
		InternetDateTime dateTime = driveHelper.getSpreadsheetUpdateTime(key);

		InternetDateTime afterCreation = addSecondsToDateTime(new InternetDateTime(),
			TIME_DELTA_IN_SECONDS);

		// This is not a strict check, but it will at least make sure driveHelper
		// doesn't return nonsense
		assertTrue(beforeCreation.isBefore(dateTime));
		assertTrue(afterCreation.isAfter(dateTime));
	}

	private InternetDateTime addSecondsToDateTime(InternetDateTime dateTime, int seconds) {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setTime(dateTime.toDate());

		calendar.add(Calendar.SECOND, seconds);

		return new InternetDateTime(calendar.getTime());
	}
}
