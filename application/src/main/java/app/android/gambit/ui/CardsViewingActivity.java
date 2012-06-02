package app.android.gambit.ui;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import app.android.gambit.R;
import app.android.gambit.local.Card;
import app.android.gambit.local.Deck;


public class CardsViewingActivity extends Activity
{
	private final Context activityContext = this;

	private final List<HashMap<String, Object>> cardsData;

	private static final String CARDS_DATA_BACK_SIDE_TEXT_ID = "back_side";
	private static final String CARDS_DATA_FRONT_SIDE_TEXT_ID = "front_side";
	private static final String CARDS_DATA_CURRENT_SIDE_ID = "current_side";

	private static enum CardSide {
		FRONT, BACK
	}

	private static enum CardsOrder {
		DEFAULT, STRAIGHT, SHUFFLE
	}

	private Deck deck;

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ShakeListener sensorListener;
	private boolean isLoadingInProgress;

	public CardsViewingActivity() {
		super();

		cardsData = new ArrayList<HashMap<String, Object>>();

		isLoadingInProgress = false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cards_viewing);

		initializeSensor();

		processReceivedDeck();

		loadCards(CardsOrder.DEFAULT);
	}

	private void initializeSensor() {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorListener = new ShakeListener();

		sensorListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
			@Override
			public void onShake() {
				if (!isLoadingInProgress) {
					loadCards(CardsOrder.SHUFFLE);
				}
			}
		});
	}

	private void loadCards(CardsOrder cardsOrder) {
		new LoadCardsTask(cardsOrder).execute();
	}

	private class LoadCardsTask extends AsyncTask<Void, Void, Void>
	{
		private ProgressDialogShowHelper progressDialogHelper;

		private final CardsOrder cardsOrder;

		public LoadCardsTask(CardsOrder cardsOrder) {
			this.cardsOrder = cardsOrder;
		}

		@Override
		protected void onPreExecute() {
			if (isLoadingInProgress) {
				cancel(true);
			}
			else {
				isLoadingInProgress = true;
			}

			progressDialogHelper = new ProgressDialogShowHelper();

			switch (cardsOrder) {
				case SHUFFLE:
					progressDialogHelper.show(activityContext, getString(R.string.loading_shuffling_cards));
					break;

				case STRAIGHT:
					progressDialogHelper.show(activityContext, getString(R.string.loading_resetting_cards_order));
					break;

				case DEFAULT:
					progressDialogHelper.show(activityContext, getString(R.string.loading_cards));
					break;

				default:
					break;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			switch (cardsOrder) {
				case SHUFFLE:
					deck.shuffleCards();
					break;

				case STRAIGHT:
					deck.resetCardsOrder();
					break;

				case DEFAULT:
					break;

				default:
					break;
			}

			fillCardsList(deck.getCardsList());

			return null;
		}

		private void fillCardsList(List<Card> cards) {
			cardsData.clear();

			for (Card card : cards) {
				addCardToCardsList(card);
			}
		}

		private void addCardToCardsList(Card card) {
			HashMap<String, Object> cardItem = new HashMap<String, Object>();

			cardItem.put(CARDS_DATA_FRONT_SIDE_TEXT_ID, card.getFrontSideText());
			cardItem.put(CARDS_DATA_BACK_SIDE_TEXT_ID, card.getBackSideText());
			cardItem.put(CARDS_DATA_CURRENT_SIDE_ID, CardSide.FRONT);

			cardsData.add(cardItem);
		}

		@Override
		protected void onPostExecute(Void result) {
			progressDialogHelper.hide();

			initializeCardsAdapter();

			if (cardsOrder == CardsOrder.DEFAULT) {
				restoreCurrentCardPosition();
			}

			isLoadingInProgress = false;
		}

		private void initializeCardsAdapter() {
			CardsAdapter cardsAdapter = new CardsAdapter();
			ViewPager cardsPager = (ViewPager) findViewById(R.id.cardsPager);
			cardsPager.setAdapter(cardsAdapter);
		}

		private void restoreCurrentCardPosition() {
			int currentCardPosition = deck.getCurrentCardIndex();
			setCurrentCardPosition(currentCardPosition);
		}
	}

	private class CardsAdapter extends PagerAdapter
	{
		private static final int CARD_TEXT_SIZE = 30;

		@Override
		public int getCount() {
			return cardsData.size();
		}

		@Override
		public Object instantiateItem(View container, final int position) {
			TextView cardTextView = new TextView(activityContext);

			cardTextView.setText(getCardText(position));
			cardTextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
			cardTextView.setTextSize(CARD_TEXT_SIZE);

			cardTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					TextView cardView = (TextView) view;

					invertCardSide(position);
					cardView.setText(getCardText(position));
				}
			});

			((ViewPager) container).addView(cardTextView, 0);

			return cardTextView;
		}

		private void invertCardSide(int position) {
			HashMap<String, Object> cardItem = cardsData.get(position);

			CardSide cardSide = (CardSide) cardItem.get(CARDS_DATA_CURRENT_SIDE_ID);
			CardSide invertedCardSide;

			switch (cardSide) {
				case FRONT:
					invertedCardSide = CardSide.BACK;
					break;

				case BACK:
					invertedCardSide = CardSide.FRONT;
					break;

				default:
					invertedCardSide = CardSide.FRONT;
					break;
			}

			setCardSide(invertedCardSide, position);
		}

		private void setCardSide(CardSide cardSide, int position) {
			HashMap<String, Object> cardItem = cardsData.get(position);

			cardItem.put(CARDS_DATA_CURRENT_SIDE_ID, cardSide);
		}

		private String getCardText(int position) {
			HashMap<String, Object> cardItem = cardsData.get(position);

			CardSide cardSide = (CardSide) cardItem.get(CARDS_DATA_CURRENT_SIDE_ID);

			switch (cardSide) {
				case FRONT:
					return (String) cardItem.get(CARDS_DATA_FRONT_SIDE_TEXT_ID);

				case BACK:
					return (String) cardItem.get(CARDS_DATA_BACK_SIDE_TEXT_ID);

				default:
					return new String();
			}
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView((TextView) object);

			setDefaultCardSide(position);
		}

		private void setDefaultCardSide(int position) {
			setCardSide(CardSide.FRONT, position);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == (TextView) object;
		}
	}

	private void setCurrentCardPosition(int position) {
		ViewPager cardsPager = (ViewPager) findViewById(R.id.cardsPager);

		cardsPager.setCurrentItem(position);
	}

	private void processReceivedDeck() {
		Bundle receivedData = this.getIntent().getExtras();

		if (receivedData.containsKey(IntentFactory.MESSAGE_ID)) {
			deck = receivedData.getParcelable(IntentFactory.MESSAGE_ID);
		}
		else {
			UserAlerter.alert(activityContext, getString(R.string.error_unspecified));

			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.cards_viewing_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_shuffle:
				loadCards(CardsOrder.SHUFFLE);
				return true;

			case R.id.menu_reset:
				loadCards(CardsOrder.STRAIGHT);
				return true;

			case R.id.menu_back:
				goToFirstCard();

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void goToFirstCard() {
		setCurrentCardPosition(0);
	}

	@Override
	protected void onResume() {
		super.onResume();

		sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	protected void onPause() {
		super.onPause();

		storeCurrentCardPosition();

		sensorManager.unregisterListener(sensorListener);
	}

	private void storeCurrentCardPosition() {
		new StoreCardsPositionTask().execute();
	}

	private class StoreCardsPositionTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			deck.setCurrentCardIndex(getCurrentCardPosition());

			return null;
		}

		private int getCurrentCardPosition() {
			ViewPager cardsPager = (ViewPager) findViewById(R.id.cardsPager);

			return cardsPager.getCurrentItem();
		}
	}
}