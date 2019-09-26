package com.stemaker.arbeitsbericht

import android.util.Log

object HtmlReport {

    fun encodeReport(rep: Report, inclSignatures: Boolean = true): String {
        Log.d("Arbeitsbericht.HtmlReport.encodeReport", "Generating HTML report for ID:${rep.id}, Name: ${rep.client_name}")
        var html: String =
                "<!DOCTYPE html>" +
                "<html>" +
                "<body>" +
                "<h1>Arbeitsbericht Nr. ${rep.id}</h1>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Kunde / Projekt</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Rechnungsaddresse</th>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${rep.client_name}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">${rep.bill_address_name}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${rep.client_extra1}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">${rep.bill_address_street}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"></td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">${rep.bill_address_zip} ${rep.bill_address_city}</td>" +
                    "</tr>" +
                "</table><hr>"


        // Work times table
        html += "<h2>Arbeits-/fahrzeiten und Fahrstrecken</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Datum</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Mitarbeiter</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeitszeit</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahrzeit</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahrstrecke [km]</th>" +
                    "</tr>"
        rep.work_times.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.date}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.employee}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.duration}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.driveTime}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.distance.toString()}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Work items table
        html += "<h2>Durchgeführte Arbeiten</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeit</th>" +
                    "</tr>"
        rep.work_items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.item}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Lump sums
        html += "<h2>Pauschalen</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Pauschale</th>" +
                    "</tr>"
        rep.lump_sums.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.item}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Material table
        html += "<h2>Material</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Material</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                    "</tr>"
        rep.material.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.item}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.toString()}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"
        if(rep.photos.size != 0) {
            html += "<h2>Fotos</h2>"
            rep.photos.forEach {
                Log.d("Arbeitsbericht.html", it.file)
                html += "<img src=\"file://${it.file}\">"
                html += "${it.description}"
            }
            html += "<hr>"
        }
        if(inclSignatures) {
            // Employee signature
            html += "<h2>Unterschrift Auftragnehmer</h2>"
            if (rep.employee_signature == "") {
                html += "Keine Unterschrift vorhanden"
            } else {
                html += "<div class=\"svg\">"
                html += rep.employee_signature
                html += "</div>"
            }
            // Client signature
            html += "<h2>Unterschrift Auftraggeber</h2>"
            if (rep.client_signature == "") {
                html += "Keine Unterschrift vorhanden"
            } else {
                html += "<div class=\"svg\">"
                html += rep.client_signature
                html += "</div>"
            }
        }
        html += "</body></html>"
        return html
    }
}

