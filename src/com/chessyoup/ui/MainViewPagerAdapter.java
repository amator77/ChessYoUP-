package com.chessyoup.ui;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MainViewPagerAdapter  extends FragmentStatePagerAdapter {

	private List<Fragment> fList;

    public MainViewPagerAdapter(FragmentManager fm) {
        super(fm);        
        this.fList = new ArrayList<Fragment>();
    }
    
    public void addFragment(Fragment f){
    	fList.add(f);
    }
    
    @Override
    public int getCount() {
        return this.fList.size();
    }

    @Override
    public Fragment getItem(int position) {
    	return fList.get(position);      	
    }
}
