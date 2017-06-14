package com.ucm.tfg.tracktrainme.Historial;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ucm.tfg.tracktrainme.DataBase.DatabaseAdapter;
import com.ucm.tfg.tracktrainme.DataBase.HistoryDataTransfer;
import com.ucm.tfg.tracktrainme.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistorialDiaConcreto extends Activity {

    private ListView listView;
    //private ArrayAdapter<String> adapter;
    private HistorialListAdapter adapter;
    //private ArrayList<String> list;

    protected void onCreate(Bundle savedInstanceState){
        List<HistoryDataTransfer> list;
        List<String> nombreActividad = new ArrayList<>();
        List<Date> horaInicio = new ArrayList<>();
        List<Date> horaFin = new ArrayList<>();
        Intent i;
        String dateToShow;
        DatabaseAdapter db;



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_dia_concreto);
        this.listView = (ListView)findViewById(R.id.list);

        //INICIALIZAMOS MODELO LISTA//////////////////////////////////////////////////////////////////////
        //this.list = new ArrayList<String>();

        //INICIALIZAMOS ADAPTADOR/////////////////////////////////////////////////////////////////////////


        i = getIntent();
        dateToShow = i.getStringExtra("dayToShow");
        db = new DatabaseAdapter(this);

        db.open();
        list = db.dameActividadesFecha(dateToShow);
        db.close();

        for(HistoryDataTransfer aux: list){
            nombreActividad.add(aux.getNombreActividad());
            horaInicio.add(aux.getfIni());
            horaFin.add(aux.getfFin());
        }

        /*this.adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, list);*/


        this.adapter = new HistorialListAdapter(this, nombreActividad, horaInicio, horaFin);

        listView.setAdapter(adapter);
        //this.textoPruebas.setText("Aquí aparecerá una lista con las actividades que has realizado en el día " + Integer.toString(i.getIntExtra("dayOfMonth", 1)) + " del " + Integer.toString(i.getIntExtra("month", 1) + 1) + " del " + Integer.toString(i.getIntExtra("year", 1970)) + " ;)");
    }

    protected void onResume() {

        super.onResume();
    }
}