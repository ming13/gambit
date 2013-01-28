/*
 * Copyright 2012 Artur Dryomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ming13.gambit.local.provider;


import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import ru.ming13.gambit.local.sqlite.DbFieldNames;
import ru.ming13.gambit.local.sqlite.DbOpenHelper;
import ru.ming13.gambit.local.sqlite.DbTableNames;
import ru.ming13.gambit.local.sqlite.DbValues;


public class GambitProvider extends ContentProvider
{
	private SQLiteOpenHelper databaseHelper;

	@Override
	public boolean onCreate() {
		databaseHelper = new DbOpenHelper(getContext());

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArguments, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		switch (ProviderUris.MATCHER.match(uri)) {
			case ProviderUris.Codes.DECKS:
				queryBuilder.setTables(DbTableNames.DECKS);
				break;

			case ProviderUris.Codes.DECK:
				queryBuilder.setTables(DbTableNames.DECKS);
				selection = buildDeckSelectionClause(uri);
				break;

			case ProviderUris.Codes.CARDS:
				queryBuilder.setTables(DbTableNames.CARDS);
				selection = buildCardsSelectionClause(uri);
				break;

			case ProviderUris.Codes.CARD:
				queryBuilder.setTables(DbTableNames.CARDS);
				selection = buildCardSelectionClause(uri);
				break;

			default:
				throw new IllegalArgumentException(buildUnsupportedUriDetailMessage(uri));
		}

		Cursor decksCursor = queryBuilder.query(databaseHelper.getReadableDatabase(), projection,
			selection, selectionArguments, null, null, sortOrder);

		decksCursor.setNotificationUri(getContext().getContentResolver(), uri);

		return decksCursor;
	}

	private String buildDeckSelectionClause(Uri deckUri) {
		long deckId = ContentUris.parseId(deckUri);

		return buildSelectionClause(DbFieldNames.ID, deckId);
	}

	private String buildSelectionClause(String fieldName, long id) {
		return String.format("%s = %d", fieldName, id);
	}

	private String buildCardsSelectionClause(Uri cardsUri) {
		long deckId = ProviderUris.Content.parseDeckId(cardsUri);

		return buildSelectionClause(DbFieldNames.CARD_DECK_ID, deckId);
	}

	private String buildCardSelectionClause(Uri cardUri) {
		long cardId = ContentUris.parseId(cardUri);

		return buildSelectionClause(DbFieldNames.ID, cardId);
	}

	private String buildUnsupportedUriDetailMessage(Uri unsupportedUri) {
		return String.format("Unsupported URI: %s", unsupportedUri.toString());
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentValues) {
		switch (ProviderUris.MATCHER.match(uri)) {
			case ProviderUris.Codes.DECKS:
				return insertDeck(contentValues);

			case ProviderUris.Codes.CARDS:
				return insertCard(uri, contentValues);

			default:
				throw new IllegalArgumentException(buildUnsupportedUriDetailMessage(uri));
		}
	}

	private Uri insertDeck(ContentValues deckValues) {
		if (!areDeckValuesValidForInsertion(deckValues)) {
			throw new IllegalArgumentException("Content values are not valid.");
		}

		if (!isDeckTitleUnique(deckValues)) {
			throw new DeckExistsException();
		}

		setDeckInsertionDefaults(deckValues);

		return createDeck(deckValues);
	}

	private boolean areDeckValuesValidForInsertion(ContentValues deckValues) {
		return deckValues.containsKey(DbFieldNames.DECK_TITLE);
	}

	private boolean isDeckTitleUnique(ContentValues deckValues) {
		String deckTitle = deckValues.getAsString(DbFieldNames.DECK_TITLE);

		return queryDecksCount(deckTitle) == 0;
	}

	private long queryDecksCount(String deckTitle) {
		return DatabaseUtils.longForQuery(databaseHelper.getReadableDatabase(),
			buildDecksCountQuery(deckTitle), null);
	}

	private String buildDecksCountQuery(String deckTitle) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(String.format("select count(%s) ", DbFieldNames.ID));
		queryBuilder.append(String.format("from %s ", DbTableNames.DECKS));
		queryBuilder.append(String.format("where upper(%s) = upper(%s)", DbFieldNames.DECK_TITLE,
			DatabaseUtils.sqlEscapeString(deckTitle)));

		return queryBuilder.toString();
	}

	private void setDeckInsertionDefaults(ContentValues deckValues) {
		deckValues.put(DbFieldNames.DECK_CURRENT_CARD_INDEX, DbValues.DEFAULT_DECK_CURRENT_CARD_INDEX);
	}

	private Uri createDeck(ContentValues deckValues) {
		long deckId = databaseHelper.getWritableDatabase().insert(DbTableNames.DECKS, null, deckValues);

		Uri deckUri = ProviderUris.Content.buildDeckUri(deckId);
		getContext().getContentResolver().notifyChange(deckUri, null);

		return deckUri;
	}

	private Uri insertCard(Uri cardsUri, ContentValues cardValues) {
		if (!areCardValuesValidForInsertion(cardValues)) {
			throw new IllegalArgumentException("Content values are not valid.");
		}

		setCardInsertionDefaults(cardsUri, cardValues);

		return createCard(cardsUri, cardValues);
	}

	private boolean areCardValuesValidForInsertion(ContentValues cardValues) {
		return cardValues.containsKey(DbFieldNames.CARD_FRONT_SIDE_TEXT) && cardValues.containsKey(
			DbFieldNames.CARD_BACK_SIDE_TEXT);
	}

	private void setCardInsertionDefaults(Uri cardsUri, ContentValues cardValues) {
		long deckId = ProviderUris.Content.parseDeckId(cardsUri);
		long cardOrderIndex = calculateCardOrderIndex(deckId);

		cardValues.put(DbFieldNames.CARD_DECK_ID, deckId);
		cardValues.put(DbFieldNames.CARD_ORDER_INDEX, cardOrderIndex);
	}

	private long calculateCardOrderIndex(long deckId) {
		if (isCardOrderIndexUsed(deckId)) {
			return queryCardsCount(deckId);
		}

		return DbValues.DEFAULT_CARD_ORDER_INDEX;
	}

	private boolean isCardOrderIndexUsed(long deckId) {
		return DatabaseUtils.longForQuery(databaseHelper.getReadableDatabase(),
			buildMaximumCardsOrderIndexQuery(deckId), null) != DbValues.DEFAULT_CARD_ORDER_INDEX;
	}

	private String buildMaximumCardsOrderIndexQuery(long deckId) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(String.format("select max(%s) ", DbFieldNames.CARD_ORDER_INDEX));
		queryBuilder.append(String.format("from %s ", DbTableNames.CARDS));
		queryBuilder.append(String.format("where %s = %d", DbFieldNames.CARD_DECK_ID, deckId));

		return queryBuilder.toString();
	}

	private long queryCardsCount(long deckId) {
		return DatabaseUtils.longForQuery(databaseHelper.getReadableDatabase(),
			buildCardsCountQuery(deckId), null);
	}

	private String buildCardsCountQuery(long deckId) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(String.format("select count(%s) ", DbFieldNames.ID));
		queryBuilder.append(String.format("from %s ", DbTableNames.CARDS));
		queryBuilder.append(String.format("where %s = %d", DbFieldNames.CARD_DECK_ID, deckId));

		return queryBuilder.toString();
	}

	private Uri createCard(Uri cardsUri, ContentValues cardValues) {
		long cardId = databaseHelper.getWritableDatabase().insert(DbTableNames.CARDS, null, cardValues);

		Uri cardUri = ProviderUris.Content.buildCardUri(cardsUri, cardId);
		getContext().getContentResolver().notifyChange(cardUri, null);

		return cardUri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArguments) {
		switch (ProviderUris.MATCHER.match(uri)) {
			case ProviderUris.Codes.DECK:
				return deleteDeck(uri);

			case ProviderUris.Codes.CARD:
				return deleteCard(uri);

			default:
				throw new IllegalArgumentException(buildUnsupportedUriDetailMessage(uri));
		}
	}

	private int deleteDeck(Uri deckUri) {
		int affectedRowsCount = databaseHelper.getWritableDatabase().delete(DbTableNames.DECKS,
			buildDeckSelectionClause(deckUri), null);
		getContext().getContentResolver().notifyChange(deckUri, null);

		return affectedRowsCount;
	}

	private int deleteCard(Uri cardUri) {
		int affectedRowsCount = databaseHelper.getWritableDatabase().delete(DbTableNames.CARDS,
			buildCardSelectionClause(cardUri), null);
		getContext().getContentResolver().notifyChange(cardUri, null);

		return affectedRowsCount;
	}

	@Override
	public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArguments) {
		switch (ProviderUris.MATCHER.match(uri)) {
			case ProviderUris.Codes.DECK:
				return updateDeck(uri, contentValues);

			case ProviderUris.Codes.CARD:
				return updateCard(uri, contentValues);

			default:
				throw new IllegalArgumentException(buildUnsupportedUriDetailMessage(uri));
		}
	}

	private int updateDeck(Uri deckUri, ContentValues deckValues) {
		if (!areDeckValuesValidForUpdating(deckValues)) {
			throw new DeckExistsException();
		}

		return editDeck(deckUri, deckValues);
	}

	private boolean areDeckValuesValidForUpdating(ContentValues deckValues) {
		if (!deckValues.containsKey(DbFieldNames.DECK_TITLE)) {
			return true;
		}

		return isDeckTitleUnique(deckValues);
	}

	private int editDeck(Uri deckUri, ContentValues deckValues) {
		int affectedRowsContent = databaseHelper.getWritableDatabase().update(DbTableNames.DECKS,
			deckValues, buildDeckSelectionClause(deckUri), null);
		getContext().getContentResolver().notifyChange(deckUri, null);

		return affectedRowsContent;
	}

	private int updateCard(Uri cardUri, ContentValues cardValues) {
		int affectedRowsCount = databaseHelper.getWritableDatabase().update(DbTableNames.CARDS,
			cardValues, buildCardSelectionClause(cardUri), null);
		getContext().getContentResolver().notifyChange(cardUri, null);

		return affectedRowsCount;
	}

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		SQLiteDatabase database = databaseHelper.getWritableDatabase();

		database.beginTransaction();
		try {
			ContentProviderResult[] results = new ContentProviderResult[operations.size()];

			for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
				results[operationIndex] = operations.get(operationIndex).apply(this, results,
					operationIndex);
			}

			database.setTransactionSuccessful();
			return results;
		}
		finally {
			database.endTransaction();
		}
	}
}