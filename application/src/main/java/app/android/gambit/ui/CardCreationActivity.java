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


import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import app.android.gambit.R;
import app.android.gambit.local.Deck;


public class CardCreationActivity extends FormActivity
{
	private Deck deck;

	protected String frontSideText;
	protected String backSideText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_card_creation);
		super.onCreate(savedInstanceState);

		processReceivedData();
	}

	@Override
	protected void readUserDataFromFields() {
		frontSideText = getTextFromEdit(R.id.edit_front_side_text);
		backSideText = getTextFromEdit(R.id.edit_back_side_text);
	}

	@Override
	protected String getUserDataErrorMessage() {
		String errorMessage = getFrontSideTextErrorMessage();
		if (!TextUtils.isEmpty(errorMessage)) {
			return errorMessage;
		}

		return getBackSideTextErrorMessage();
	}

	private String getFrontSideTextErrorMessage() {
		if (TextUtils.isEmpty(frontSideText)) {
			return getString(R.string.error_empty_card_front_text);
		}

		return new String();
	}

	private String getBackSideTextErrorMessage() {
		if (TextUtils.isEmpty(backSideText)) {
			return getString(R.string.error_empty_card_back_text);
		}

		return new String();
	}

	@Override
	protected void performSubmitAction() {
		new CreateCardTask().execute();
	}

	private class CreateCardTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			deck.createCard(frontSideText, backSideText);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			finish();
		}
	}

	protected void processReceivedData() {
		try {
			deck = (Deck) IntentProcessor.getMessage(this);
		}
		catch (IntentCorruptedException e) {
			UserAlerter.alert(this, R.string.error_unspecified);

			finish();
		}
	}
}
