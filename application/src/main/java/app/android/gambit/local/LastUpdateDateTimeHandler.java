package app.android.gambit.local;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import app.android.gambit.remote.InternetDateTime;


class LastUpdateDateTimeHandler
{
	private final SQLiteDatabase database;

	public LastUpdateDateTimeHandler() {
		this.database = DbProvider.getInstance().getDatabase();
	}

	public void setCurrentDateTimeAsLastUpdated() {
		database.beginTransaction();
		try {
			trySetCurrentDateTimeAsLastUpdated();
			database.setTransactionSuccessful();
		}
		finally {
			database.endTransaction();
		}
	}

	private void trySetCurrentDateTimeAsLastUpdated() {
		if (!recordExists()) {
			insertEmptyRecord();
		}

		updateRecord(new InternetDateTime());
	}

	private boolean recordExists() {
		Cursor databaseCursor = database.rawQuery(buildRecordsCountSelectingQuery(), null);
		databaseCursor.moveToFirst();

		final int RECORDS_COUNT_COLUMN_INDEX = 0;
		int recordsCount = databaseCursor.getInt(RECORDS_COUNT_COLUMN_INDEX);

		return recordsCount > 0;
	}

	private String buildRecordsCountSelectingQuery() {
		return String.format("select count(*) from %s", DbTableNames.DB_LAST_UPDATE_TIME);
	}

	private void insertEmptyRecord() {
		database.execSQL(buildEmptyRecordInsertionQuery());
	}

	private String buildEmptyRecordInsertionQuery() {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(String.format("insert into %s ", DbTableNames.DB_LAST_UPDATE_TIME));
		queryBuilder.append(String.format("(%s) ", DbFieldNames.DB_LAST_UPDATE_TIME));
		queryBuilder.append(String.format("values (%s)", "''"));

		return queryBuilder.toString();
	}

	private void updateRecord(InternetDateTime dateTime) {
		database.execSQL(buildRecordUpdatingQuery(dateTime));
	}

	private String buildRecordUpdatingQuery(InternetDateTime dateTime) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(String.format("update %s ", DbTableNames.DB_LAST_UPDATE_TIME));
		queryBuilder.append(
			String.format("set %s='%s' ", DbFieldNames.DB_LAST_UPDATE_TIME, dateTime.toString()));

		return queryBuilder.toString();
	}

	public InternetDateTime getLastUpdatedDateTime() {
		database.beginTransaction();
		try {
			InternetDateTime lastUpdatedDateTime = tryGetLastUpdatedDateTime();
			database.setTransactionSuccessful();
			return lastUpdatedDateTime;
		}
		finally {
			database.endTransaction();
		}
	}

	private InternetDateTime tryGetLastUpdatedDateTime() {
		ensureRecordExists();

		Cursor databaseCursor = database.rawQuery(buildRecordSelectingQuery(), null);
		databaseCursor.moveToFirst();

		String dateTimeAsString = databaseCursor.getString(0);

		return new InternetDateTime(dateTimeAsString);
	}

	private void ensureRecordExists() {
		if (!recordExists()) {
			setCurrentDateTimeAsLastUpdated();
		}
	}

	private String buildRecordSelectingQuery() {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(String.format("select %s ", DbFieldNames.DB_LAST_UPDATE_TIME));
		queryBuilder.append(String.format("from %s", DbTableNames.DB_LAST_UPDATE_TIME));

		return queryBuilder.toString();
	}
}
