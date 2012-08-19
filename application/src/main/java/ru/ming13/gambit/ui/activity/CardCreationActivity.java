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

package ru.ming13.gambit.ui.activity;


import android.support.v4.app.Fragment;
import ru.ming13.gambit.local.model.Deck;
import ru.ming13.gambit.ui.fragment.CardOperationFragment;
import ru.ming13.gambit.ui.intent.IntentException;
import ru.ming13.gambit.ui.intent.IntentExtras;


public class CardCreationActivity extends FragmentWrapperActivity implements CardOperationFragment.FormCallback
{
	@Override
	protected Fragment buildFragment() {
		return CardOperationFragment.newCreationInstance(extractReceivedDeck());
	}

	private Deck extractReceivedDeck() {
		Deck deck = getIntent().getParcelableExtra(IntentExtras.DECK);

		if (deck == null) {
			throw new IntentException();
		}

		return deck;
	}

	@Override
	public <Data> void onAccept(Data data) {
		finish();
	}

	@Override
	public void onCancel() {
		finish();
	}
}
