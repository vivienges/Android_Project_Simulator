package com.example.android_project_simulator

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.multidex.MultiDex
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    lateinit var adapter: ArrayAdapter<String>
    private var db = FirebaseFirestore.getInstance()
    private var idList = mutableListOf<String>()

    // Set up multidex for this activity
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        idList = mutableListOf()
        val listView = findViewById<ListView>(R.id.listView)

        adapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            idList
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, BikeSimulator::class.java)
            val itemText = listView.getItemAtPosition(position).toString()
            intent.putExtra(EXTRA_BIKE_ID, itemText.replace("[^0-9]".toRegex(), ""))
            startActivity(intent)
        }

        db.collection("bikes")
            .addSnapshotListener{ snapshots, e ->
                if (e == null && snapshots != null )
                    //TODO: Add error handling
                    for (documentChange in snapshots.documentChanges){
                        when (documentChange.type) {
                            DocumentChange.Type.ADDED ->
                                idList.add("Bike " + documentChange.document.id)
                            DocumentChange.Type.REMOVED ->
                                idList.remove("Bike " + documentChange.document.id)
                            else -> {}
                        }
                    }
                    adapter.notifyDataSetChanged()
            }
    }

    companion object {
        const val EXTRA_BIKE_ID = "BIKE_ID"
    }
}
