# AndroidArbeitsbericht
**Simple work report Android app**

Diese App befindet sich im Alpha Zustand und unterstützt zur Zeit nur Tablets.

Einfache App mit der man folgendes erfassen kann:
 - Kunde / Projekt
 - Rechnungsadresse
 - Arbeits- und Fahrtzeiten
 - Durchgeführte Arbeiten
 - Verwendete Materialien

Nach Abschluss kann der Bericht als pdf Datei per e-mail verschickt werden.

Diese App ist ein Hobbyprojekt, das ich kostenlos zur Verfügung stelle. Ich übernehme keinerlei Verantwortung für Schäden, die durch die Installation oder Nutzung dieser App entstehen könnten.

Die App ist unter der Apache 2.0 Lizenz (http://www.apache.org/licenses/LICENSE-2.0.html) freigegeben, der source code kann bei github heruntergeladen werden.

**Verwendung der Daten**
Die App speichert die eingegebenen Berichtsdaten im json Format und die erstellten Berichte als pdf auf dem Gerät. Die Berichtsdaten liegen im App-eigenen Ordner, die pdf Berichte im allgemeinen Documents Ordner (für andere apps einfach zugreifbar). Ausserdem werden "Wörterbücher" der bisher verwendeten Eingaben erstellt, um Eingabevorschläge beim Erstellen der Berichte zu machen. Auch diese Wörterbücher werden im App-eigenen Ordner auf dem Gerät gespeichert. Das Versenden der Berichte per mail setzt eine e-mail App voraus, die Zugriff auf ein e-mail Konto hat. Die App selber kommuniziert nicht direkt per e-mail, sondern reicht den Bericht nur an eine e-mail-App weiter. Falls eine Verschlüsselung der Daten für den Versand notwendig ist, muss diese durch die e-mail-App durchgeführt werden - die Arbeitsbericht App bietet keine derartige Möglichkeit. Die vorgenommenen Einstellungen im Einstellungsdialog werden wiederum auf dem Gerät abgelegt.
