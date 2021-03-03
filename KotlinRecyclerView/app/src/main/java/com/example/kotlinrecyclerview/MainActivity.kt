package com.example.kotlinrecyclerview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    lateinit var rv:RecyclerView
    val contacts = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Init();
        createContacts();

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ContactAdapter(this, contacts)

    }

    private fun createContacts(){
        for(i in 1..150){
            contacts.add(Contact("Person $i", i))
        }
    }

    private fun Init() {
        rv = findViewById(R.id.rvContacts);
    }
}