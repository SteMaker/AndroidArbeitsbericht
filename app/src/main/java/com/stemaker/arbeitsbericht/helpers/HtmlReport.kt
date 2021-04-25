package com.stemaker.arbeitsbericht.helpers

import android.util.Base64
import android.util.Log
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.data.ReportData
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val TAG = "HtmlReport"

object HtmlReport {

    fun readFileToBytes(f: File): ByteArray {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        val fis= FileInputStream(f)
        try {
            var read = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (e: IOException){
            throw e
        } finally {
            fis.close()
        }
        return bytes
    }

    fun encodeReport(rep: ReportData, dir: File, inclSignatures: Boolean = true): String {
        val fs = "font-size:${configuration().fontSize}px"
        var html: String =
            "<!DOCTYPE html>" +
                    "<html lang=\"de\">" +
                    "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head>" +
                    "<body>"
        if(configuration().logoFile != "" && configuration().pdfUseLogo) {
            val logoFileContent = readFileToBytes(File(dir, configuration().logoFile))
            val logo = Base64.encodeToString(logoFileContent, Base64.DEFAULT)
            html += "<img src=\"data:image/jpg;base64,${logo}\" style=\"height: 100%; width: 100%; object-fit: contain\"/>"
        }
        html +=     "<h1>Arbeitsbericht Nr. ${rep.id}</h1>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Kunde / Projekt</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Rechnungsaddresse</th>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${rep.project.name.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">${rep.bill.name.value}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${rep.project.extra1.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">${rep.bill.street.value}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"></td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">${rep.bill.zip.value} ${rep.bill.city.value}</td>" +
                    "</tr>" +
                "</table><hr>"


        // Work times table
        html += "<h2>Arbeits-/fahrzeiten und Fahrstrecken</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Datum</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Mitarbeiter</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeits-anfang</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeits-ende</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahrzeit<br>[h:m]</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahr-strecke [km]</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Pause<br>[h:m]</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeits-zeit [h:m]</th>" +
                "</tr>"
            rep.workTimeContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.date.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">"
                for(empl in it.employees)
                    html += "${empl.value!!}<br>"
                html += "</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.startTime.value} Uhr</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.endTime.value} Uhr</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.driveTime.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.distance.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.pauseDuration.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.workDuration.value}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Work items table
        html += "<h2>Durchgeführte Arbeiten</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeit</th>" +
                    "</tr>"
        rep.workItemContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Lump sums
        html += "<h2>Pauschalen</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Pauschale</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Bemerkung</th>" +
                "</tr>"
        rep.lumpSumContainer.items.forEach {
            html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.comment.value}</td>" +
                    "</tr>"
        }
        html += "</table><hr>"

        // Material table
        html += "<h2>Material</h2>" +
                "<table style=\"border: 2px solid black;border-collapse: collapse;\">"
        if(rep.materialContainer.isAnyMaterialUnitSet()) {
            html += "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Material</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Einheit</th>" +
                    "</tr>"
            rep.materialContainer.items.forEach {
                html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.unit.value}</td>" +
                        "</tr>"
            }
        } else {
            html += "<tr>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Material</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                    "</tr>"
            rep.materialContainer.items.forEach {
                html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                        "</tr>"
            }
        }
        html += "</table><hr>"
        if(rep.photoContainer.items.size != 0) {
            html += "<h2>Fotos</h2>"
            rep.photoContainer.items.forEach {
                val fileName = File(it.file.value).name // old version stored full path
                html += "<img src=\"content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtPhotos/$fileName\" style=\"max-width:100%\">"
                html += "${it.description.value}"
            }
            html += "<hr>"
        }
        if(inclSignatures) {
            // Employee signature
            html += "<h2>Unterschriften</h2>"
            if (rep.signatureData.clientSignatureSvg.value == "" && rep.signatureData.employeeSignatureSvg.value == "") {
                html += "Keine Unterschriften vorhanden<hr>"
            } else {
                html += "<table>" +
                        "<tr>" +
                        "<th>Auftragnehmer</th>" +
                        "<th>Auftraggeber</th>" +
                        "</tr>" +
                        "<tr>"
                if(rep.signatureData.employeeSignatureSvg.value != "" && rep.signatureData.employeeSignaturePngFile != null)
                    html += "<th><img src=\"content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtSignatures/${rep.signatureData.employeeSignaturePngFile!!.name}\" style=\"max-width:50%\"></th>"
                else
                    html += "<th>Keine Unterschrift</th>"
                if(rep.signatureData.clientSignatureSvg.value != "" && rep.signatureData.clientSignaturePngFile != null)
                    html += "<th><img src=\"content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtSignatures/${rep.signatureData.clientSignaturePngFile!!.name}\" style=\"max-width:50%\"></th>"
                else
                    html += "<th>Keine Unterschrift</th>"
                html += "</tr>" +
                        "</table><hr>"
            }
        }
        if(configuration().footerFile != "" && configuration().pdfUseFooter) {
            val footerFileContent = readFileToBytes(File(dir, configuration().footerFile))
            val footer = Base64.encodeToString(footerFileContent, Base64.DEFAULT)
            html += "<img src=\"data:image/jpg;base64,${footer}\" style=\"height: 100%; width: 100%; object-fit: contain\"/>"
        }

        html += "</body></html>"

        return html
    }
}

