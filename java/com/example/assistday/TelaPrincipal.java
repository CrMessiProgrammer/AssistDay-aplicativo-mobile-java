package com.example.assistday;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.AlarmClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class TelaPrincipal extends AppCompatActivity {

    private TextView textTitle;
    private TextView stateAlarm;
    private boolean isOn = false;
    private ImageButton bt_alarm;
    private ImageButton bt_help;
    private ImageButton bt_accessibility;
    private ImageButton bt_information;
    private ImageButton bt_bluetooth;
    private static final int SOLICITACAO_CONTATOS = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_LOCATION = 2;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<String> deviceList;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String usuarioID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_principal);

        findViewById(R.id.ButtonCam).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String appPackageName = "com.yoosee";

                PackageManager packageManager = getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(appPackageName);

                if (intent != null) {
                    startActivity(intent);
                } else {
                    exibirConfirmacao();
                }
            }
        });

        findViewById(R.id.ButtonContato).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(TelaPrincipal.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    TelaContatos();
                } else {
                    ActivityCompat.requestPermissions(TelaPrincipal.this, new String[]{Manifest.permission.READ_CONTACTS}, SOLICITACAO_CONTATOS);
                }
            }
        });

        findViewById(R.id.ButtonPerfil).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TelaPrincipal.this, TelaPerfil.class);
                startActivity(intent);
            }
        });

        bt_alarm = findViewById(R.id.ButtonAlarm);
        stateAlarm = findViewById(R.id.StateAlarm);

        bt_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String appPackageName = "project.bluetoothterminal";

                PackageManager packageManager = getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(appPackageName);

                if (intent != null) {
                    startActivity(intent);
                } else {
                    Uri uri = Uri.parse("market://details?id=" + appPackageName);
                    Intent intent1 = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent1);
                }
            }
        });

        findViewById(R.id.ButtonEmergency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("tel:" + 192);
                Intent intent = new Intent(Intent.ACTION_DIAL, uri);
                startActivity(intent);
            }
        });
        IniciarComponentes();

        bt_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exibirAjuda();
            }
        });

        bt_accessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exibirAccessibility();
            }
        });

        bt_information.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exibirConfirmacao();
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);

        bt_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetoothPermissions();
            }
        });
    }

    public void onImageButtonClick(View view) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não é compatível com este dispositivo", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                showPairedDevices();
            }
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_LOCATION);
            } else {
                onImageButtonClick(null);
            }
        } else {
            onImageButtonClick(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onImageButtonClick(null);
            } else {
                Toast.makeText(this, "A permissão de localização é necessária para Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceList.clear();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            deviceList.add("Nenhum dispositivo pareado encontrado");
        }

        ListView listView = new ListView(this);
        listView.setAdapter(deviceListAdapter);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Dispositivos Pareados");
        builder.setMessage("O sistema de segurança é nomeado por 'HC-05', caso o mesmo não apareça na lista abaixo, faça a conectividade em 'Parear Agora'.");
        builder.setView(listView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(TelaPrincipal.this, "Clicou em OK", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Parear Agora", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }
        });
        builder.show();
    }

    public void exibirAjuda() {
        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);
        msgBox.setTitle("Precisando de ajuda?");
        msgBox.setIcon(android.R.drawable.ic_menu_help);
        msgBox.setMessage("Para problemas ou dúvidas, contate abaixo via WhatsApp nossa equipe do Suporte Técnico.");
        msgBox.setPositiveButton("Voltar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(TelaPrincipal.this, "Clicou em VOLTAR", Toast.LENGTH_SHORT).show();
            }
        });
        msgBox.setNegativeButton("Fale conosco", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String numSupport = "5511970530153";
                Uri uri = Uri.parse("smsto:" + numSupport);
                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            }
        });
        msgBox.show();
    }

    public void exibirAccessibility() {
        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);
        msgBox.setTitle("Modo de Acessibilidade...");
        msgBox.setIcon(android.R.drawable.ic_dialog_alert);
        msgBox.setMessage("Caro cliente, essa função reúne recursos de adaptação para pessoas com deficiência ou limitação visual.");
        msgBox.setPositiveButton("Entendi", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(TelaPrincipal.this, "Clicou em Entendi", Toast.LENGTH_SHORT).show();
            }
        });
        msgBox.setNegativeButton("Ver Configurações", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
        msgBox.show();
    }
    public void exibirConfirmacao() {
        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);
        msgBox.setTitle("Informações adicionais...");
        msgBox.setIcon(android.R.drawable.ic_dialog_info);
        msgBox.setMessage("Recomendamos a pré-instalação do aplicativo Yoosee. Junto de nosso app, ele trará um melhor acesso e suporte às câmeras de segurança!");
        msgBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(TelaPrincipal.this, "Clicou em OK", Toast.LENGTH_SHORT).show();
            }
        });
        msgBox.setNegativeButton("Baixar agora", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri uri = Uri.parse("market://details?id=" + "com.yoosee");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        msgBox.show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        usuarioID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference documentReference = db.collection("Usuarios").document(usuarioID);
        documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                if (documentSnapshot != null) {
                    textTitle.setText(documentSnapshot.getString("nome"));
                }
            }
        });
    }

    private void TelaContatos() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivity(intent);
    }

    private void IniciarComponentes(){
        textTitle = findViewById(R.id.TextTitle);
        bt_help = findViewById(R.id.bt_help);
        bt_accessibility = findViewById(R.id.bt_accessibility);
        bt_information = findViewById(R.id.bt_information);
        bt_bluetooth = findViewById(R.id.bt_bluetooth);
    }
}