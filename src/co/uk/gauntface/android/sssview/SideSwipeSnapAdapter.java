package co.uk.gauntface.android.sssview;

import android.widget.BaseAdapter;

public abstract class SideSwipeSnapAdapter extends BaseAdapter {
	
	/**
	 * Keep track of the selected index
	 */
	private int mSelectedIndex;
	
	public SideSwipeSnapAdapter() {
		mSelectedIndex = 0;
	}
	
	public int getSelectedPosition() {
		return mSelectedIndex;
	}
	
	public void setSelectedPosition(int selectedPosition) {
		mSelectedIndex = selectedPosition;
	}
}
