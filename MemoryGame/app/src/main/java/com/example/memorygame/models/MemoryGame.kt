package com.example.memorygame.models

import com.example.memorygame.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize) {

    val cards: List<MemoryCard>
    var numPairsFound = 0
    private var indexOfSingleSelectedCard: Int? = null
    private var numCardFlips = 0

    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card = cards[position]
        var foundMatch = false
        //Three cases:
        // 0 cards flipped => flip ove the selected
        // 1 cards flipped => flip over the selected + match could be found
        // 2 cards flipped => restore the cards and face down + flip over the selected card
        if (indexOfSingleSelectedCard == null) {
            // 0 or 2 cards flipped
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            // 1 cards flipped over
            foundMatch = checkForMatch(position, indexOfSingleSelectedCard!!)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(pos1: Int, pos2: Int): Boolean {
        if (cards[pos1].identifier != cards[pos2].identifier) {
            return false
        } else {
            cards[pos1].isMatched = true
            cards[pos2].isMatched = true
            numPairsFound++
            return true
        }
    }


    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }

    init {
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()
        cards = randomizedImages.map { MemoryCard(it, false, false) }
    }
}