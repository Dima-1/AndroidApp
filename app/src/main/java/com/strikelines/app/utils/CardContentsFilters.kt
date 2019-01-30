package com.strikelines.app.utils

import com.strikelines.app.domain.models.Chart

fun descriptionFilter(chart: Chart):String {

    var description = chart.description
            .replace("<[^>]*>".toRegex(), "")
            .replace("\r","")
            .replace("\n","")
            .replace("\t","")

    if (chart.name.contains("3D "))
        if(chart.description.contains("Description")) {
            description = description.substring(chart.description.indexOf("Description"))
        }

    return description
}

fun clearTitleForWrecks(title:String):String = title.replace("&#8211;", "-").replace("&#8217;","\'")

fun String.clearGarbadge():String = this.replace("\\**.*?\\*".toRegex(), "")

