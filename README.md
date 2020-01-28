# AndroidArbeitsbericht
**Simple work report Android app**

Diese App unterstützt zur Zeit nur Tablets.

Mit der App kann ein Handwerker folgende Daten erfassen und als Bericht per E-Mail verschicken:
 - Kunde / Projekt
 - Rechnungsadresse
 - Arbeits- und Fahrtzeiten
 - Durchgeführte Arbeiten
 - Verwendete Materialien
 - Pauschalen (Pauschalen werden vordefiniert und können dann im Bericht nur noch ausgewählt werden)
 - Fotos
 - Unterschrift des Auftraggebers und Auftragnehmers

Nach Abschluss kann der Bericht als Anhang per E-Mail verschickt werden. Der Bericht wird standardmäßig als ODF Dokument generiert, das später mit verschiedenen Textverarbeitungsprogrammen (u.a. LibreOffice, MS Word) editiert werden kann. Eine ODF Vorlage kann dazu verwendet werden, um Firmenlogos, feste Textteile, eine Kopf- oder Fußzeile, usw. einzubinden. Details zur Vorlage weiter unten.

Alternativ kann der Bericht auch als PDF generiert und als E-Mail-Anhang verschickt werden. In diesem Fall wird keine Vorlage unterstützt.

Als E-Mail client eignen sich nicht alle E-Mail Apps, da nicht alle von ihnen Anhänge von einer anderen App akzeptieren. GMail als Beispiel funktioniert.

Eine Liste der Pauschalen und die ODF Vorlage können zur Zeit nur von einem SFTP Server heruntergeladen werden.

Diese App ist ein Hobbyprojekt, das ich kostenlos zur Verfügung stelle. Ich übernehme keinerlei Verantwortung für Schäden, die durch die Installation oder Nutzung dieser App entstehen könnten.

**Lizenz**
Die App ist unter der Apache 2.0 Lizenz (http://www.apache.org/licenses/LICENSE-2.0.html) freigegeben, der source code kann bei github unter https://github.com/SteMaker/AndroidArbeitsbericht heruntergeladen werden. Pull requests are welcome.

**Pauschalen**
Pauschalen können auf dem Gerät direkt erstellt und gelöscht werden. Es besteht aber auch die Möglichkeit eine Textdatei per SFTP zu laden, in der die Pauschalen Zeilenweise aufgelistet sind. Jede Zeile wird als einzelne Pauschale interpretiert.

Vorsicht: Wenn eine Pauschale gelöscht wird, die in einem Bericht verwendet wird, dann wird in dem Bericht dieser Eintrag automatisch auf die erste definierte Pauschale zurückgesetzt - es gibt keine Warnung!

**ODF Vorlage**
Eine vorgefertigte Vorlage liegt im github repository (https://github.com/SteMaker/AndroidArbeitsbericht/tree/master/app/src/main/assets) mit dem Namen output_template.odt. Die Vorlage kann z.B. mit LibreOffice editiert werden (MS Word sollte theoretisch auch gehen, habe ich aber nicht probiert). An den Stellen, an denen die Berichtselemente einzufügen sind müssen Feldbefehle eingebunden sein. Es werden zwei Arten von Feldbefehlen benutzt. Einer für einzelne Elemente, wie z.B. der Rechnungsname, und einer
für komplexere Teile, wie Tabellen. Einen Feldbefehl für komplexere Elemente kann man mit LibreOffice wie folgt anlegen:
  - Den cursor an die entsprechende Stelle setzen
  - Sicherstellen, dass ein neuer Absatz angefangen wurde (am Ende der vorherige Zeile return drücken)
  - Einfügen -> Feldbefehl -> Weitere Feldbefehle -> Tab Variablen auswählen -> Typ: Benutzerfeld auswählen. Das entsprechende Feld (siehe Liste unten) unter Auswählen markieren und Einfügen clicken.
  - Mit Return einen neuen Absatz beginnen. Das Feld muss also seinen eigenen Absatz besitzen.

Folgende Daten verwenden Feldbefehle für komplexere Daten und dürfen nur einmal im template verwendet werden:
  - worktimetag -> Arbeits-/ und Fahrzeiten 
  - workitemtag -> Durchgeführte Arbeiten
  - lumpsumtag -> Pauschalen
  - materialtag -> Verwendete Materialien
  - phototag -> Fotos
  - signatureemployeetag -> Unterschrift des Mitarbeiters
  - signatureclienttag -> Unterschrift des Auftraggebers

Einfache Elemente können wie folgt eingebunden werden (sie können beliebig oft eingebunden werden, ein extra Absatz ist nicht nötig):
  - Den cursor an die entsprechende Stelle setzen
  - Einfügen -> Feldbefehl -> Weitere Feldbefehle -> Tab Dokumentinfo auswählen -> Typ: Benutzerfeld aufklappen. Das entsprechende Feld (siehe Liste unten) unter Auswählen markieren und Einfügen clicken.

Folgende Daten verwenden Feldbefehle für einfache Daten und können beliebig oft in der Vorlage verwendet werden:
  - report_id -> ID des Berichts (Berichtsnummer)
  - create_date -> Erstellungsdatum
  - bill_name -> Rechnungsname
  - bill_street -> Rechnungsstrasse
  - bill_zip -> Rechnungspostleitzahl
  - bill_city -> Rechnungsort
  - project_name -> Projektname
  - project_extra1 -> Projektzusatz

**Verwendung der Daten**
Die App speichert die eingegebenen Berichtsdaten im json Format und die erstellten Berichte als pdf oder odf auf dem Gerät. Die Berichtsdaten liegen im App-eigenen Ordner, die PDF bzw. ODF Berichte in einem Ordner Arbeitsbericht parallel zu den Standard Dokumenten Ordnern, wie Camera, Documents usw. (für andere Apps potentiell zugreifbar). Ausserdem werden "Wörterbücher" der bisher verwendeten Eingaben erstellt, um Eingabevorschläge beim Erstellen der Berichte zu machen. Auch diese Wörterbücher werden im App-eigenen Ordner auf dem Gerät gespeichert. Das Versenden der Berichte per mail setzt eine E-Mail App voraus, die Zugriff auf ein E-Mail Konto hat. Die App selber kommuniziert nicht direkt per E-Mail, sondern reicht den Bericht nur an eine E-Mail-App weiter. Falls eine Verschlüsselung der Daten für den Versand notwendig ist, muss diese durch die E-Mail-App durchgeführt werden - die Arbeitsbericht App bietet keine derartige Möglichkeit. Die vorgenommenen Einstellungen im Einstellungsdialog werden wiederum auf dem Gerät abgelegt. Zum Herunterladen von Pauschalenlisten und einer ODF-Vorlage wird ein SFTP Zugang verwendet.

Es findet neben der Kommunikation mit dem vom Benutzer definierten SFTP Server keine weitere Kommunikation mit irgendwelchen anderen Servern statt.
