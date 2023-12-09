package com.example.assistday.model;

import com.google.firebase.auth.FirebaseAuth;

public class UserModel {
    private String nome = FirebaseAuth.getInstance().getCurrentUser().getUid();

    public UserModel() {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

}
