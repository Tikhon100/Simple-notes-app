package com.example.lab2

class NativeLib {
    companion object {
        init {
            System.loadLibrary("invertnumber")
        }
    }

    external fun invertNumber(number: Int): Int
}