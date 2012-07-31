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

package app.android.gambit.ui;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import app.android.gambit.R;
import app.android.gambit.local.AlreadyExistsException;
import app.android.gambit.local.DbProvider;
import app.android.gambit.local.Deck;


public class DeckCreationActivity extends FormActivity
{
	protected String deckName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_deck_creation);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void readUserDataFromFields() {
		deckName = getTextFromEdit(R.id.edit_deck_name);
	}

	@Override
	protected String getUserDataErrorMessage() {
		return getDeckNameErrorMessage();
	}

	private String getDeckNameErrorMessage() {
		if (TextUtils.isEmpty(deckName)) {
			return getString(R.string.error_empty_deck_name);
		}

		return new String();
	}

	@Override
	protected void performSubmitAction() {
		new CreateDeckTask().execute();
	}

	private class CreateDeckTask extends AsyncTask<Void, Void, String>
	{
		private Deck deck;

		@Override
		protected String doInBackground(Void... params) {
			try {
				deck = DbProvider.getInstance().getDecks().createDeck(deckName);
			}
			catch (AlreadyExistsException e) {
				return getString(R.string.error_deck_already_exists);
			}

			return new String();
		}

		@Override
		protected void onPostExecute(String errorMessage) {
			if (TextUtils.isEmpty(errorMessage)) {
				callCardsEditing(deck);

				finish();
			}
			else {
				UserAlerter.alert(DeckCreationActivity.this, errorMessage);
			}
		}
	}

	private void callCardsEditing(Deck deck) {
		Intent callIntent = IntentFactory.createCardsEditingIntent(this, deck);
		startActivity(callIntent);
	}
}
