package com.strikelines.app.domain

interface RepoCallback{
    //callback for loading indicator (goes to Main Activity)
    fun isResourcesLoading(status:Boolean)
    fun onLoadingComplete(status:String)

}