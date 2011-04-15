package co.uk.gauntface.android.sssview;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.MeasureSpec;
import android.widget.Adapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class SideSwipeSnapView extends HorizontalScrollView {
	
	private static final String TAG = "gauntface";
	
	// Layout containing child views to be swiped
	private LinearLayout mContainerLinearLayout;
	// The currently displayed child relative to the adapter count
	private int mSelectedPosition;
	// During animation we reference the new child to be displayed
	private int mNewSelectedPosition;
	// This is the child count from the adapter (Set up on notifyDataSetChanged())
	private int mItemCount;
	// This is a reference to the scaled touch slop to determine a quick swipe
	private int mScaledTouchSlop;
	// Keep track of the previous x coordinate touch event
	private float mPrevX;
	// Keep track of when the scroll is reacting to a touch event scroll
	private boolean mIsTouchEventScroll;
	// Reference to an OnDisplayChangeListener
	private OnDisplayChangeListener mDisplayChangeListener;
	// Reference to the associated adapter for the view
	private SideSwipeSnapAdapter mAdapter;
	// The DataSetObserver set up between the view and an adapter
	private SideSwipeSnapDataSetObserver mDataSetObserver;
	
	public SideSwipeSnapView(Context context) {
		super(context);
		constructor();
	}
	
	public SideSwipeSnapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		constructor();
	}
	
	public SideSwipeSnapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		constructor();
	}
    
	/**
	 * This method simply sets up the view for use
	 */
	private void constructor() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.root_swipesnapview, this, true);
		
		mContainerLinearLayout = (LinearLayout) findViewById(R.id.root_swipesnapview_container_linearlayout);
		mContainerLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		
		/**if(mContainerLinearLayout == null) {
			mContainerLinearLayout = new LinearLayout(getContext());
			mContainerLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			

			addView(mContainerLinearLayout);
		}**/
		
		setHorizontalFadingEdgeEnabled(false);

		mSelectedPosition = 0;
		mNewSelectedPosition = 0;

		mItemCount = 0;

		mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_MOVE:
			int deltaX = (int) (mPrevX - event.getX());
			scrollBy(deltaX, 0);
			break;
		case MotionEvent.ACTION_UP:
			animateToPosition();
		}

		mPrevX = event.getX();

		return true;
	}
	
	private void animateToPosition() {
		double scrollX = getScrollX();
		int screenWidth = getWidth();

		double ratio = scrollX / (double) screenWidth;
		int whichScreen = (int) Math.round(ratio);

		if(whichScreen >= 0 && whichScreen < mItemCount) {
			int newX = whichScreen * screenWidth;
			int screenDiff = whichScreen - mSelectedPosition;
			switch(screenDiff) {
			case -1:
				mNewSelectedPosition = mSelectedPosition - 1;
				break;
			case 0:
				mNewSelectedPosition = mSelectedPosition;
				break;
			case 1:
				mNewSelectedPosition = mSelectedPosition + 1;
				break;
			}

			if(mNewSelectedPosition < 0) {
				mNewSelectedPosition = 0;
			} else if(mNewSelectedPosition >= mItemCount) {
				mNewSelectedPosition = mItemCount - 1;
			}

			mSelectedPosition = mNewSelectedPosition;

			mIsTouchEventScroll = true;
			smoothScrollTo(newX, 0);
		}
	}
	
	/**
	 * This method will set a display change listener in case the user wishes to
	 * perform any actions when the view changes.
	 * 
	 * A simple example is changing an action bar depending on the screen
	 * content.
	 * 
	 * @param changeListener
	 */
	public void setOnDisplayChangeListener(OnDisplayChangeListener changeListener) {
		mDisplayChangeListener = changeListener;
	}
	
	/**
	 * The adapter is used similar to a ListView and BaseAdapter.
	 * 
	 * This allows the user to abstract the logic away from the View
	 * 
	 * @param adapter
	 */
	public void setAdapter(SideSwipeSnapAdapter adapter) {
		if(mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}

		mAdapter = adapter;

		if(mAdapter != null) {
			mDataSetObserver = new SideSwipeSnapDataSetObserver();
			mAdapter.registerDataSetObserver(mDataSetObserver);

			mItemCount = mAdapter.getCount();
			mSelectedPosition = mAdapter.getSelectedPosition();
		} else {
			mItemCount = 0;
			mSelectedPosition = 0;

			return;
		}

		populateLayout();
	}
	
	// l is horizontal scroll origin, t is vertical
	protected void onScrollChanged (int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if(mIsTouchEventScroll) {
			if(l % getWidth() == 0) {
				mIsTouchEventScroll = false;
				mSelectedPosition = mNewSelectedPosition;
				if(mDisplayChangeListener != null) {
					mDisplayChangeListener.onDisplayChange(l/getWidth());
				}
				if(mAdapter != null) {
					mAdapter.setSelectedPosition(l / getWidth());
				}
			}
		}
	}
	
	private void populateLayout() {
		Log.v(TAG, "SideSwipeSnapView: MeasuredWidget = "+getMeasuredWidth() + " MeasuredHeight = "+getMeasuredHeight());
		
		if(mContainerLinearLayout != null) {
			mContainerLinearLayout.measure(MeasureSpec.makeMeasureSpec(mItemCount * getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
		}
		
		boolean viewInLayout;
		for(int i = 0; i < mItemCount; i++) {
			// First retrieve any possible view in the layout
			View v = mContainerLinearLayout.getChildAt(i);
			
			// See if the view needs adding to the container layout or if it already exists
			viewInLayout = true;
			if(v == null) {
				viewInLayout = false;
			}
			
			// Pass the view to the adapter to populate it
			v = mAdapter.getView(i, v, mContainerLinearLayout);
			
			// Assign a minimum width
			v.setMinimumWidth(getMeasuredWidth());
			// Force the view to be the width of the SideSwipeSnapView
			v.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
			Log.v("mubaloo", "SideSWipeSnapView: getMeasuredWidth() "+getMeasuredWidth());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getMeasuredWidth(), getMeasuredHeight());
			v.setLayoutParams(params);
			// Ensure the view draws itself
			//v.postInvalidate();
			
			// If the view isn't already in the layout then add it to eh container layout
			if(viewInLayout) {
				mContainerLinearLayout.removeViewAt(i);
			}
			mContainerLinearLayout.addView(v, i);
			//mContainerLinearLayout.addView(v);
			
			/**if(viewInLayout == false) {
				
			} else {
				
			}**/
		}
		
		// When the view is next free scroll to the currently displayed view
		post(new Runnable() {

			public void run() {
				scrollTo(mSelectedPosition * getMeasuredWidth(), 0);
				invalidate();
			}

		});
	}
	
	/**
	 * Handle when the views size changes
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if(w > 0 && h > 0) {
			if(mAdapter != null) {
				mItemCount = mAdapter.getCount();
				mSelectedPosition = mAdapter.getSelectedPosition();
			}
			populateLayout();
		}

	}
	
	/**
	 * Interface for a display change listener
	 * @author matt
	 *
	 */
	public interface OnDisplayChangeListener {
		abstract void onDisplayChange(int position);
	}
	
	/**
	 * The data set observer is used when the adapters notifyDataSetChanged() or
	 * notifyInvalidated() methods are called.
	 * 
	 * @author matt
	 *
	 */
	public class SideSwipeSnapDataSetObserver extends DataSetObserver {
		public SideSwipeSnapDataSetObserver() {
			super();
		}

		@Override
		public void onChanged() {
			mItemCount = mAdapter.getCount();
			mSelectedPosition = mAdapter.getSelectedPosition();
			populateLayout();
		}

		@Override
		public void onInvalidated() {
			// Data is invalid so we should reset our state
			mItemCount = mAdapter.getCount();
			mSelectedPosition = mAdapter.getSelectedPosition();
			populateLayout();
		}
	}
}