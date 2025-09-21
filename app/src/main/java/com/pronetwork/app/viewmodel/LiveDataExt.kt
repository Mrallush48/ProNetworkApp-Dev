package com.pronetwork.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <X, Y> LiveData<X>.map(mapFunction: (X) -> Y): LiveData<Y> =
    MediatorLiveData<Y>().also { result ->
        result.addSource(this) { x -> result.value = mapFunction(x) }
    }

fun <X, Y> LiveData<X>.switchMap(switchMapFunction: (X) -> LiveData<Y>): LiveData<Y> =
    MediatorLiveData<Y>().also { result ->
        var currentSource: LiveData<Y>? = null
        result.addSource(this) { x ->
            val newLiveData = switchMapFunction(x)
            if (currentSource != null) result.removeSource(currentSource!!)
            currentSource = newLiveData
            result.addSource(newLiveData) { y -> result.value = y }
        }
    }