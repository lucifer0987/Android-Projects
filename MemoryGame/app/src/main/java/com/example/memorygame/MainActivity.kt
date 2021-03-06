package com.example.memorygame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.utils.DEFAULT_ICONS

class MainActivity : AppCompatActivity() {

    private lateinit var rvBoard:RecyclerView
    private lateinit var tvNumMoves:TextView
    private lateinit var tvNumPairs:TextView
    private var boardSize:BoardSize = BoardSize.MEDIUM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Init();
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages+chosenImages).shuffled()
        rvBoard.adapter = MemoryBoardAdapter(this, boardSize, randomizedImages)

    }

    private fun Init() {
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumPairs)
        tvNumPairs = findViewById(R.id.tvNumMoves)
    }
}