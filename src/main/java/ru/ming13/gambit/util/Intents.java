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

package ru.ming13.gambit.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import ru.ming13.gambit.R;
import ru.ming13.gambit.activity.CardCreationActivity;
import ru.ming13.gambit.activity.CardEditingActivity;
import ru.ming13.gambit.activity.CardsListActivity;
import ru.ming13.gambit.activity.CardsPagerActivity;
import ru.ming13.gambit.activity.DeckCreationActivity;
import ru.ming13.gambit.activity.DeckEditingActivity;
import ru.ming13.gambit.activity.LicensesActivity;
import ru.ming13.gambit.model.Card;
import ru.ming13.gambit.model.Deck;

public final class Intents
{
	private Intents() {
	}

	public static final class Extras
	{
		private Extras() {
		}

		public static final String DECK = "deck";
		public static final String CARD = "card";
	}

	private static final class UriMasks
	{
		private UriMasks() {
		}

		public static final String EMAIL = "mailto:%s?subject=%s";

		public static final String GOOGLE_PLAY_APP = "market://details?id=%s";
		public static final String GOOGLE_PLAY_WEB = "https://play.google.com/store/apps/details?id=%s";
	}

	public static final class Builder
	{
		private final Context context;

		public static Builder with(Context context) {
			return new Builder(context);
		}

		private Builder(Context context) {
			this.context = context;
		}

		public Intent buildDeckCreationIntent() {
			return new Intent(context, DeckCreationActivity.class);
		}

		public Intent buildDeckEditingIntent(Deck deck) {
			Intent intent = new Intent(context, DeckEditingActivity.class);
			intent.putExtra(Extras.DECK, deck);

			return intent;
		}

		public Intent buildCardsListIntent(Deck deck) {
			Intent intent = new Intent(context, CardsListActivity.class);
			intent.putExtra(Extras.DECK, deck);

			return intent;
		}

		public Intent buildCardsPagerIntent(Deck deck) {
			Intent intent = new Intent(context, CardsPagerActivity.class);
			intent.putExtra(Extras.DECK, deck);

			return intent;
		}

		public Intent buildCardCreationIntent(Deck deck) {
			Intent intent = new Intent(context, CardCreationActivity.class);
			intent.putExtra(Extras.DECK, deck);

			return intent;
		}

		public Intent buildCardEditingIntent(Deck deck, Card card) {
			Intent intent = new Intent(context, CardEditingActivity.class);
			intent.putExtra(Extras.DECK, deck);
			intent.putExtra(Extras.CARD, card);

			return intent;
		}

		public Intent buildLicensesIntent() {
			return new Intent(context, LicensesActivity.class);
		}

		public Intent buildFeedbackIntent() {
			String feedbackAddress = context.getString(R.string.email_feedback_address);
			String feedbackSubject = context.getString(R.string.email_feedback_subject);

			String feedbackUri = String.format(UriMasks.EMAIL, feedbackAddress, feedbackSubject);

			return new Intent(Intent.ACTION_SENDTO, Uri.parse(feedbackUri));
		}

		public Intent buildGooglePlayAppIntent() {
			String packageName = context.getPackageName();

			String googlePlayUri = String.format(UriMasks.GOOGLE_PLAY_APP, packageName);

			return new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayUri));
		}

		public Intent buildGooglePlayWebIntent() {
			String packageName = context.getPackageName();

			String googlePlayUri = String.format(UriMasks.GOOGLE_PLAY_WEB, packageName);

			return new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayUri));
		}
	}
}
