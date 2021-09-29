package com.stemaker.arbeitsbericht.helpers

interface ListObserver<T> {
    fun elementAdded(element: T, pos: Int)
    fun elementsAdded(elements: List<T>, startPos: Int, endPos: Int)
    fun elementRemoved(element: T, oldPos: Int)
    fun cleared()
    //fun elementChanged(element: T, pos: Int)
}