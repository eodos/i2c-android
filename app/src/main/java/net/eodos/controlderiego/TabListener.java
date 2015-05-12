package net.eodos.controlderiego;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.util.Log;

public class TabListener implements ActionBar.TabListener {

    private Fragment fragment;

    public TabListener(Fragment fg) {
        this.fragment = fg;
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {}

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        ft.replace(R.id.contenedor, fragment);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        ft.remove(fragment);
    }
}
