package com.stemaker.arbeitsbericht.helpers

import android.util.Log
import com.stemaker.arbeitsbericht.data.ReportData

object HtmlReport {

    fun encodeReport(rep: ReportData, inclSignatures: Boolean = true): String {
        Log.d("Arbeitsbericht.HtmlReport.encodeReport", "Generating HTML report for ID:${rep.id}, Name: ${rep.project.name.value}")
        var html: String =
            "<!DOCTYPE html>" +
                    "<html>" +
                    "<body>" +
                    "<h1>Arbeitsbericht Nr. ${rep.id.value}</h1>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Kunde / Projekt</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Rechnungsaddresse</th>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${rep.project.name.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">${rep.bill.name.value}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${rep.project.extra1.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">${rep.bill.street.value}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"></td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">${rep.bill.zip.value} ${rep.bill.city.value}</td>" +
                    "</tr>" +
                "</table><hr>"


        // Work times table
        html += "<h2>Arbeits-/fahrzeiten und Fahrstrecken</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Datum</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Mitarbeiter</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeitsanfang</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeitsend</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahrzeit</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahrstrecke [km]</th>" +
                    "</tr>"
            rep.workTimeContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.date.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\">"
                for(empl in it.employees)
                    html += "${empl}<br>"
                html += "</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.startTime.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.endTime.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.driveTime.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.distance.value}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Work items table
        html += "<h2>Durchgeführte Arbeiten</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeit</th>" +
                    "</tr>"
        rep.workItemContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Lump sums
        html += "<h2>Pauschalen</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Pauschale</th>" +
                        "<th style=\"padding: 15px;font-size:24px;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                    "</tr>"
        rep.lumpSumContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
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
        rep.materialContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "<td style=\"padding: 10px;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"
        if(rep.photoContainer.items.size != 0) {
            html += "<h2>Fotos</h2>"
            rep.photoContainer.items.forEach {
                html += "<img src=\"file://${it.file.value}\">"
                html += "${it.description.value}"
            }
            html += "<hr>"
        }
        if(inclSignatures) {
            // Employee signature
            html += "<h2>Unterschrift Auftragnehmer</h2>"
            if (rep.signatureData.employeeSignatureSvg.value == "") {
                html += "Keine Unterschrift vorhanden"
            } else {
                html += "<div class=\"svg\">"
                html += rep.signatureData.employeeSignatureSvg.value
                html += "</div>"
            }
            // Client signature
            html += "<h2>Unterschrift Auftraggeber</h2>"
            if (rep.signatureData.clientSignatureSvg.value == "") {
                html += "Keine Unterschrift vorhanden"
            } else {
                html += "<div class=\"svg\">"
                html += rep.signatureData.clientSignatureSvg.value
                html += "</div>"
            }
        }
        html += "</body></html>"

        return html
    }
}

