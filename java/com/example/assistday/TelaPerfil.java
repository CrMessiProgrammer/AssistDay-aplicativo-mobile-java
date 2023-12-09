package com.example.assistday;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.assistday.model.UserModel;
import com.example.assistday.utils.AndroidUtil;
import com.example.assistday.utils.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class TelaPerfil extends AppCompatActivity {
    ShapeableImageView imgUser;
    FloatingActionButton ButtonUser;
    ProgressBar progressBar;
    Button bt_salvar;
    private TextView nomeUsuario,emailUsuario;
    private Button bt_voltar;
    private Button bt_sair;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String usuarioID;

    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_perfil);

        imgUser = findViewById(R.id.imgUser);
        ButtonUser = findViewById(R.id.ButtonUser);
        bt_salvar = findViewById(R.id.bt_save);
        progressBar = findViewById(R.id.progressbar);

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data != null && data.getData() != null){
                            selectedImageUri = data.getData();
                            AndroidUtil.setProfilePic(getApplicationContext(),selectedImageUri,imgUser);
                        }
                    }
                });

        getUserData();

        ButtonUser.setOnClickListener((v)->{
            ImagePicker.with(this)
                    .cropSquare()	    			//Crop image(Optional), Check Customization for more option
                    .compress(512)			//Final image size will be less than 1 MB(Optional)
                    .maxResultSize(512, 512)	//Final image resolution will be less than 1080 x 1080(Optional)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickLauncher.launch(intent);
                            return null;
                        }
            });
        });
        IniciarComponentes();

        bt_voltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TelaPerfil.this,TelaPrincipal.class);
                startActivity(intent);
            }
        });

        bt_salvar.setOnClickListener((v -> {
            updateBtnClick();
        }));

        bt_sair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(TelaPerfil.this,FormLogin.class);
                startActivity(intent);
                finish();
            }
        });
    }

    void updateBtnClick(){

        setInProgress(true);

        if(selectedImageUri != null){
            FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            updateToFirestore();
                        }
                    });
        }else{
            updateToFirestore();
        }
    }

    void updateToFirestore(){
        FirebaseUtil.currentUserDetails().set(currentUserModel).addOnCompleteListener(task -> {
            setInProgress(false);
            if(task.isSuccessful()){
                AndroidUtil.showToast(getApplicationContext(),"Salvo com sucesso");
            }else{
                AndroidUtil.showToast(getApplicationContext(),"Erro ao salvar");
            }
        });
    }

    void getUserData(){
        setInProgress(true);

        FirebaseUtil.getCurrentProfilePicStorageRef().getDownloadUrl()
                        .addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if(task.isSuccessful()){
                                    Uri uri = task.getResult();
                                    AndroidUtil.setProfilePic(getApplicationContext(),uri,imgUser);
                                }
                            }
                        });

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                setInProgress(false);
                currentUserModel = task.getResult().toObject(UserModel.class);
            }
        });
    }

    void setInProgress(boolean inProgress){
        if(inProgress){
            progressBar.setVisibility(View.VISIBLE);
            bt_salvar.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            bt_salvar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = data.getData();
        imgUser.setImageURI(uri);
    }

    @Override
    protected void onStart() {
        super.onStart();

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        usuarioID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference documentReference = db.collection("Usuarios").document(usuarioID);
        documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                if (documentSnapshot != null) {
                    nomeUsuario.setText(documentSnapshot.getString("nome"));
                    emailUsuario.setText(email);
                }
            }
        });
    }

    private void IniciarComponentes(){
        nomeUsuario = findViewById(R.id.textUser);
        emailUsuario = findViewById(R.id.textEmail);
        bt_voltar = findViewById(R.id.bt_voltar);
        bt_sair = findViewById(R.id.bt_sair);
    }
}