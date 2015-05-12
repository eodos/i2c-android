package net.eodos.controlderiego;

import android.app.ActionBar;
import android.os.Bundle;

import android.app.Fragment;
import android.app.Activity;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Obtenemos una referencia a la actionbar
        ActionBar abar = getActionBar();

        //Establecemos el modo de navegación por pestañas
        if (abar != null) {
            abar.setNavigationMode(
                    ActionBar.NAVIGATION_MODE_TABS);
        }

        //Ocultamos si queremos el título de la actividad
        //abar.setDisplayShowTitleEnabled(false);

        //Creamos las pestañas
        ActionBar.Tab tab1 =
                abar.newTab().setText(getString(R.string.tab_1));

        ActionBar.Tab tab2 =
                abar.newTab().setText(getString(R.string.tab_2));

        //Creamos los fragments de cada pestaña
        Fragment tab1frag = new MainVariablesFragment();
        Fragment tab2frag = new AdvancedVariablesFragment();

        //Asociamos los listener a las pestañas
        tab1.setTabListener(new TabListener(tab1frag));
        tab2.setTabListener(new TabListener(tab2frag));

        //Añadimos las pestañas a la action bar
        abar.addTab(tab1);
        abar.addTab(tab2);
    }
}