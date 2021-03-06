package com.ucm.tfg.tracktrainme.Fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ucm.tfg.tracktrainme.DataBase.ActivityDataTransfer;
import com.ucm.tfg.tracktrainme.DataBase.DatabaseAdapter;
import com.ucm.tfg.tracktrainme.MainActivity;
import com.ucm.tfg.tracktrainme.R;
import com.ucm.tfg.tracktrainme.Services.BluetoothLeService;
import com.ucm.tfg.tracktrainme.Services.RecogidaDeDatosService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import joinery.DataFrame;

public class ReconocerActividadFragment extends Fragment {

    private BluetoothLeService mServiceBluetooth;
    private HashMap<Integer, HashMap<String, Object>> actividadesSistema;
    private static final String KEY_NOMBRE_ACTIVIDAD = "text";
    private static final String KEY_IMAGEN = "imagen";

    private Dialog consejo;
    private boolean consejoAceptado;

    private Intent intent;

    private Handler modificadorActividad = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int idActividad = (Integer)msg.obj;

            if(idActividad != 0){
                nombreActividad.setText((String)((actividadesSistema.get(idActividad)).get(KEY_NOMBRE_ACTIVIDAD)));
                iconoActividad.setImageBitmap((Bitmap)((actividadesSistema.get(idActividad)).get(KEY_IMAGEN)));
            }
            else if(idActividad == 0){
                iconoActividad.setImageBitmap(BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyFiles/Imagenes/" + "ico_act_0.png"));
                nombreActividad.setText("No tengo muy claro lo que estás haciendo.");
            }

        }
    };

    //GESTIÓN DE DATAFRAMES//////////////////////////////////////////////////////////////////////////////////////////////////
    private DataFrame featuresSegmentado1;
    private DataFrame featuresSegmentado2;

    // {"Andar", "Barrer", "De pie", "Subir escaleras", "Bajar escaleras"};

    private int reconocedorEncendido = 0;


    //GESTIÓN ELEMENTOS GRÁFICOS
    private Button button;
    private TextView nombreActividad;
    private ImageView iconoActividad;

    //BROADCAST RECEIVER
    private BroadcastReceiver receiver;

    public ReconocerActividadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        this.actividadesSistema = new HashMap<Integer, HashMap<String, Object>>();
        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                int estado = intent.getIntExtra("estado", 0);
                Message msg = new Message();
                msg.obj = estado;
                modificadorActividad.sendMessage(msg);
            }
        };


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_reconocer_actividad, container, false);

        nombreActividad = (TextView) view.findViewById(R.id.nombre_actividad);
        iconoActividad = (ImageView) view.findViewById(R.id.icono_actividad);
        iconoActividad.setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ico_pausa));

        button = (Button) view.findViewById(R.id.boton_reconocer);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(reconocedorEncendido == 0) {
                    configurarConsejoDialog();

                    reconocedorEncendido = 1;
                    nombreActividad.setText("Comenzando a reconocer...");
                    button.setText("Parar el reconocimiento");

                    DatabaseAdapter db = new DatabaseAdapter(getContext());
                    ActivityDataTransfer activityDataTransfer = null;
                    db.open();
                    List<ActivityDataTransfer> listaAct = db.listarActividadesSistema();
                    db.close();

                    for(final ActivityDataTransfer act: listaAct){
                        Log.d("ACTIVIDADES", String.valueOf((int)act.getId()) + " " + act.getName() + " " + act.getUrlImage());
                        HashMap<String, Object> aux = new HashMap<String, Object>();
                        aux.put(KEY_NOMBRE_ACTIVIDAD, act.getName());
                        aux.put(KEY_IMAGEN, BitmapFactory.decodeFile(act.getUrlImage()));

                        actividadesSistema.put((int)act.getId(), aux);

                    }

                    if(mBound){
                        mService.mensaje_encenderSensorCC2650();
                    }

                    intent = new Intent(getContext(), RecogidaDeDatosService.class);
                    intent.putExtra("modo", ((MainActivity)getActivity()).getModo());
                    getActivity().startService(intent);
                }
                else if(reconocedorEncendido == 1){
                    getActivity().stopService(intent);
                    nombreActividad.setText("Para volver a reconocer pulse el botón de abajo.");
                    button.setText("Comenzar a reconocer");
                    reconocedorEncendido = 0;
                    iconoActividad.setImageBitmap(BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyFiles/Imagenes/" + "ico_pausa.png"));
                    actividadesSistema.clear();
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver((receiver),
                new IntentFilter("estado_actualizado")
        );


        if(((MainActivity)getActivity()).getModo().equals("PULSERA")) {
            if (!mBound) {
                Intent intent = new Intent(getContext(), BluetoothLeService.class);
                getActivity().bindService(intent, mConnetion, Context.BIND_AUTO_CREATE);
            }
        }
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
        super.onStop();
    }

    private BluetoothLeService mService;
    private boolean mBound = false;
    private ServiceConnection mConnetion = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            mService = binder.getService();
            Log.d("BIND", "mBound(true)");
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("BIND", "mBound(false)");
            mBound = false;
        }
    };

    public void desconexionService(){
        if(mBound){
            getActivity().unbindService(mConnetion);
            mBound = false;
        }
    }

    private Handler modificadorFoto = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int re = (Integer)msg.obj;

            ImageView imagen = (ImageView) consejo.findViewById(R.id.imageView_colocacion);

            if(re == 0){
                imagen.setBackgroundResource(R.drawable.colocacion);
            }
            else if(re == 1){
                imagen.setBackgroundResource(R.drawable.colocacionmal);
            }

        }
    };

    private void configurarConsejoDialog(){
        consejoAceptado = false;
        this.consejo = new Dialog(getActivity());
        consejo.setContentView(R.layout.colocacion_dispositivo);

        Button ok_boton = (Button) consejo.findViewById(R.id.button_recordatorio);

        class MiThread extends Thread {

            @Override
            public void run() {
                while(true){
                    Message msg = new Message();
                    msg.obj = 0;
                    modificadorFoto.sendMessage(msg);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    msg = new Message();
                    msg.obj = 1;
                    modificadorFoto.sendMessage(msg);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void parar(){
                this.interrupt();
            }
        }

        final MiThread fotos_thread = new MiThread();

        ok_boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consejo.dismiss();
                fotos_thread.parar();
                consejoAceptado = true;
            }
        });

        consejo.setCanceledOnTouchOutside(false);

        consejo.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    fotos_thread.parar();
                    consejo.dismiss();
                }
                return true;
            }
        });

        consejo.show();
        fotos_thread.start();
    }

    @Override
    public void onDestroy(){
        if(reconocedorEncendido == 1) {
            getActivity().stopService(intent);
        }

        super.onDestroy();

    }

}
