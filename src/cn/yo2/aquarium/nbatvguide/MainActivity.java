package cn.yo2.aquarium.nbatvguide;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MainActivity extends ListActivity {
	private static final String NBA_CCTV5_GUIDE_URL = "http://space.tv.cctv.com/page/PAGE1256625547472273";

	private static final String CLASSTAG = MainActivity.class.getSimpleName();

	private static final int PROGRESS_DIALOG = 1;
	private static final int ALERT_DB_DIALOG = 2;
	private static final int ALERT_IO_DIALOG = 3;
	private static final int ALERT_ABOUT_DIALOG = 4;

	private static final int WHAT_SUCCESS = 0;
	private static final int WHAT_FAIL_DB = 1;
	private static final int WHAT_FAIL_IO = 2;

	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST + 1;

	private static final String DATE_FORMAT = "yyyyMMdd";

	private final java.text.DateFormat mDateFormat = new SimpleDateFormat(
			DATE_FORMAT);

	private String[] mDaysOfWeek;
	private Calendar mCalendar = Calendar.getInstance();

	private Cursor mCursor;
	private DatabaseHelper mDatabaseHelper;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			dismissDialog(PROGRESS_DIALOG);
			switch (msg.what) {
			case WHAT_SUCCESS:
				populateList();
				break;
			case WHAT_FAIL_DB:
				showDialog(ALERT_DB_DIALOG);
				break;
			case WHAT_FAIL_IO:
				showDialog(ALERT_IO_DIALOG);
				break;
			default:
				break;
			}
		}

	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.setTitle(R.string.loading_title);
			progressDialog.setMessage(getString(R.string.loading_msg));
			return progressDialog;
		case ALERT_DB_DIALOG:
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setTitle(R.string.database_error_title);
			builder1.setMessage(R.string.database_error_msg);
			builder1.setCancelable(false);
			builder1.setPositiveButton(R.string.button_yes,
					new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
			return builder1.create();
		case ALERT_IO_DIALOG:
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle(R.string.network_error_title);
			builder2.setMessage(R.string.network_error_msg);
			builder2.setCancelable(false);
			builder2.setPositiveButton(R.string.button_retry,
					new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							updateDatabase();
						}
					});
			builder2.setNegativeButton(R.string.button_cancel,
					new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
			return builder2.create();
		case ALERT_ABOUT_DIALOG:
			AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
			LayoutInflater inflater = LayoutInflater.from(this);
			View layout = inflater.inflate(R.layout.about,
					(ViewGroup) findViewById(R.id.root_about));
			builder3.setView(layout);
			builder3.setTitle(R.string.menu_about);
			builder3.setNegativeButton(R.string.button_ok,
					new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
			return builder3.create();
		default:
			break;
		}
		return super.onCreateDialog(id);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDaysOfWeek = getResources().getStringArray(R.array.days_of_week);

		mDatabaseHelper = new DatabaseHelper(this);
		mDatabaseHelper.open();

		if (isItemsExpired())
			updateDatabase();
		else {
			populateList();
		}
	}

	/**
	 * 
	 */
	private void populateList() {
		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}
		mCursor = mDatabaseHelper.queryItemsBetweenDate(getMondayOfThisWeek(),
				getSundayOfThisWeek());
		// ListAdapter adapter = new RowAdapter(this, R.layout.list_item,
		// mCursor,
		// new String[] { DatabaseHelper.C_DATE, DatabaseHelper.C_TIME,
		// DatabaseHelper.C_WEEK, DatabaseHelper.C_VS },
		// new int[] { R.id.tv_date, R.id.tv_time, R.id.tv_week,
		// R.id.tv_vs });
		ListAdapter adapter = new RowAdapter(this, mCursor);
		setListAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}
		mDatabaseHelper.close();
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private String getMondayOfThisWeek() {
		Calendar c = Calendar.getInstance();

		int dayofweek = c.get(Calendar.DAY_OF_WEEK) - 1;
		if (dayofweek == 0)
			dayofweek = 7;
		c.add(Calendar.DATE, -dayofweek + 1);
		return mDateFormat.format(c.getTime());
	}

	/**
	 * Get the date of sunday in this week
	 * 
	 * @return the Date
	 */
	private String getSundayOfThisWeek() {
		Calendar c = Calendar.getInstance();
		int dayofweek = c.get(Calendar.DAY_OF_WEEK) - 1;
		if (dayofweek == 0)
			dayofweek = 7;
		c.add(Calendar.DATE, -dayofweek + 7);
		return mDateFormat.format(c.getTime());
	}

	private boolean isItemsExpired() {
		boolean result = true;
		String start = DateFormat
				.format("yyyyMMdd", System.currentTimeMillis()).toString();
		Cursor cursor = mDatabaseHelper.queryItemsAfterDate(start);
		if (cursor != null) {
			if (cursor.moveToFirst())
				result = false;
			cursor.close();
		}
		return result;
	}

	private void updateDatabase() {
		showDialog(PROGRESS_DIALOG);
		new Thread() {

			@Override
			public void run() {
				try {
					long count = mDatabaseHelper.deleteItemsAll();
					Log.d(CLASSTAG, "Delete " + count + " items.");
					downloadItems();
					mHandler.sendEmptyMessage(WHAT_SUCCESS);
				} catch (DatabaseError e) {
					mHandler.sendEmptyMessage(WHAT_FAIL_DB);
				} catch (IOException e) {
					mHandler.sendEmptyMessage(WHAT_FAIL_IO);
				}
			}

		}.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, MENU_REFRESH, Menu.NONE, R.string.menu_refresh)
				.setIcon(R.drawable.ic_menu_refresh);
		menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about)
				.setIcon(android.R.drawable.ic_menu_info_details);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			updateDatabase();
			return true;
		case MENU_ABOUT:
			showDialog(ALERT_ABOUT_DIALOG);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void downloadItems() throws DatabaseError, IOException {
		int timeoutConnection = 3000;
		int timeoutSocket = 1000 * 300;

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
		HttpConnectionParams.setSoTimeout(params, timeoutSocket);

		DefaultHttpClient httpClient = new DefaultHttpClient(params);

		HttpGet get = new HttpGet(NBA_CCTV5_GUIDE_URL);

		ResponseHandler<String> handler = new BasicResponseHandler();

		String body;
		try {
			body = httpClient.execute(get, handler);
			parse(body);
		} catch (ClientProtocolException e) {
			get.abort();
			throw e;
		} catch (IOException e) {
			get.abort();
			throw e;
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	private void parse(String body) throws DatabaseError {

		String year = DateFormat.format("yyyy", System.currentTimeMillis())
				.toString();

		int tableStart = body.indexOf("<table");
		int tableEnd = body.indexOf("</table>", tableStart);

		String tableString = body.substring(tableStart, tableEnd);

		Pattern pattern = Pattern
				.compile("<tr[\\s\\w=#]+><td\\s+align=center\\s+>(\\d+)</td><td\\s+align=center\\s+>(\\d+)[^<\\d](\\d+)[^<\\d]+</td><td\\s+align=center\\s+>([^<]+)</td><td\\s+align=center\\s+>(\\d+):(\\d+)</td></tr>");

		Matcher matcher = pattern.matcher(tableString);

		Log.d(CLASSTAG, "number\t\tdate\t\tvs\t\ttime");
		while (matcher.find()) {

			String number = matcher.group(1);
			String date = year + formatTwoDigit(matcher.group(2))
					+ formatTwoDigit(matcher.group(3));
			String vs = matcher.group(4);
			String time = formatTwoDigit(matcher.group(5))
					+ formatTwoDigit(matcher.group(6));
			String week = getWeekDay(date);

			Log.d(CLASSTAG, String.format("%s\t\t%s\t\t%s\t\t%s\t\t%s\n",
					number, date, vs, week, time));

			if (-1 == mDatabaseHelper.createItem(number, date, time, week, vs)) {
				String error = "Error when store item in database -- "
						+ "{number = " + number + ", date = " + date
						+ ", time = " + time + ", week = " + week + ", vs = "
						+ vs + "}";
				Log.e(CLASSTAG, error);
				throw new DatabaseError(error);
			}
		}
	}

	private String formatTwoDigit(String number) {
		if (number.length() == 2)
			return number;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2 - number.length(); i++) {
			sb.append("0");
		}
		sb.append(number);
		return sb.toString();
	}

	private String getWeekDay(String dateString) {
		try {
			Date date = mDateFormat.parse(dateString);
			mCalendar.setTime(date);
			return mDaysOfWeek[mCalendar.get(Calendar.DAY_OF_WEEK) - 1];
		} catch (ParseException e) {
			Log.e(CLASSTAG, "Error when parse date string.");
		}
		return mDaysOfWeek[0];
	}

	class DatabaseError extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6218783160908732027L;

		public DatabaseError() {
			super();
			// TODO Auto-generated constructor stub
		}

		public DatabaseError(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
			// TODO Auto-generated constructor stub
		}

		public DatabaseError(String detailMessage) {
			super(detailMessage);
			// TODO Auto-generated constructor stub
		}

		public DatabaseError(Throwable throwable) {
			super(throwable);
			// TODO Auto-generated constructor stub
		}
	}

	class RowAdapter extends CursorAdapter {

		private final LayoutInflater mInflater;

		public RowAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
			mInflater = LayoutInflater.from(context);
		}

		public RowAdapter(Context context, Cursor c) {
			super(context, c);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			Log.d(CLASSTAG, "In bindView -- Cursor pos: "
					+ cursor.getPosition());
			
			ViewWraper wraper = (ViewWraper)view.getTag();

			StringBuilder date = new StringBuilder(cursor.getString(cursor
					.getColumnIndex(DatabaseHelper.C_DATE)));
			StringBuilder time = new StringBuilder(cursor.getString(cursor
					.getColumnIndex(DatabaseHelper.C_TIME)));
			String week = cursor.getString(cursor
					.getColumnIndex(DatabaseHelper.C_WEEK));
			String vs = cursor.getString(cursor
					.getColumnIndex(DatabaseHelper.C_VS));

			if (DateFormat.format("yyyyMMdd", System.currentTimeMillis())
					.toString().equals(date.toString())) {
				// Log.d(CLASSTAG, "Date -- " + date + "Time -- " + time +
				// "Week -- " + week);
				wraper.getIcon().setImageResource(R.drawable.nba);
			} else {
				wraper.getIcon().setImageResource(R.drawable.blank);
			}

			wraper.getDate().setText(date.insert(4, '-').insert(7, '-'));
			wraper.getTime().setText(time.insert(time.length() - 2, ':'));
			wraper.getWeek().setText(week);
			wraper.getVs().setText(vs);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			Log
					.d(CLASSTAG, "In newView -- Cursor pos: "
							+ cursor.getPosition());
			View view = mInflater.inflate(R.layout.list_item, parent, false);
			ViewWraper wraper = new ViewWraper(view);
			view.setTag(wraper);
			return view;

		}
		
		class ViewWraper {
			View view;
			ImageView ivIcon;
			TextView tvDate;
			TextView tvTime;
			TextView tvWeek;
			TextView tvVs;
			
			public ViewWraper(View view) {
				this.view = view;
			}
			
			ImageView getIcon() {
				if (ivIcon == null) {
					ivIcon = (ImageView)view.findViewById(R.id.iv_icon);
				}
				return ivIcon;
			}
			
			TextView getDate() {
				if (tvDate == null) {
					tvDate = (TextView) view.findViewById(R.id.tv_date);
				}
				return tvDate;
			}
			
			TextView getTime() {
				if (tvTime == null) {
					tvTime = (TextView) view.findViewById(R.id.tv_time); 
				}
				return tvTime;
			}
			
			TextView getWeek() {
				if (tvWeek == null) {
					tvWeek = (TextView) view.findViewById(R.id.tv_week);
				}
				return tvWeek;
			}
			
			TextView getVs() {
				if (tvVs == null) {
					tvVs = (TextView) view.findViewById(R.id.tv_vs);
				}
				return tvVs;
			}
		}
	}
}