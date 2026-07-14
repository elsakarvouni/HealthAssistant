# MedMinder 💊🏥

## Project Overview
MedMinder is a standalone digital health assistant application for Android mobile devices. It is carefully designed to facilitate and support patients who follow a medication regimen, ensuring compliance with their treatments and timely attendance at their scheduled medical appointments.

## 👥 Target Users
This application is specifically tailored for:
* **Users on long-term medication:** Individuals following complex daily regimens involving multiple medications who need a stable daily schedule.
* **Elderly people:** Users who require a simple, clean interface without unnecessary information, featuring large font options for optimal readability.
* **Individuals with busy lifestyles:** Professionals or students who might forget their medication times or scheduled appointments due to heavy workloads.

## ✨ Key Features
* **Dynamic Dashboard:** The main screen provides immediate information by dynamically displaying the Next Medication (Time and Name) and the Next Doctor's Appointment.
* **Medication Management & Inventory:** Users can add medications, specifying if they are pills or syrup. The app requires total stock and dose per intake, automatically subtracting from the inventory when the user confirms they have taken the medication. A low-stock warning is issued when fewer than 2 doses remain.
* **Smart Alarms & Emergency SMS:** When a medication is due, an alarm triggers with two options: "TAKE IT" (updates stock and stops the alarm) or "SNOOZE". If the user snoozes the notification two or more times, the app automatically sends an SMS to a predefined emergency contact.
* **Appointment Tracking:** Users can add doctor appointments (Name, Specialty, Date, Time, Phone). The app sends a pop-up notification 2 hours prior and exactly at the time of the appointment.
* **Daily Schedule:** A dedicated screen that filters and chronologically displays all the medications to be consumed and appointments to be attended for the current day.
* **Customizable Settings:** Users can personalize their experience by toggling Dark/Light mode, adjusting font sizes (Small/Medium/Large), changing notification types (sound/popup), and setting the snooze duration (5, 10, 15, or 30 minutes).
* **Mandatory Emergency Contact:** Upon the first launch, the app requires the user to input an emergency contact number before granting access to its features.

## 🛠️ Architecture & Technologies
The application follows the classic Android SDK development pattern, strictly separating the visual interface (XML Layouts) from the business logic (Java Classes).

### Code Structure
* **Central Management & Navigation:** Handled by `MainActivity.java` (Dashboard) and `CalendarActivity.java` (Daily Schedule via a custom ListView).
* **Data Entry & Editing:** Managed by distinct activities for adding and editing medications (`AddMedicineActivity.java`, `EditMedicineActivity.java`) and appointments (`AddAppointmentActivity.java`, `EditAppointmentActivity.java`). Date and time selections are handled securely via `DatePickerDialog` and `TimePickerDialog`.
* **Background Tasks:** Utilizes `AlarmReceiver.java` (a `BroadcastReceiver`) to listen for system alarms in the background and construct Push Notifications. `AlarmActivity.java` manages the interface when an alarm rings.
* **Storage:** Data is stored locally using an SQLite database managed by `DatabaseHelper.java`. Global preferences are handled by `SettingsActivity.java` using `SharedPreferences`.
* **UI & Values:** XML layouts utilize structures like `ConstraintLayout` and `ScrollView` for responsiveness. Texts and colors are modularized in `strings.xml` and `colors.xml` to support future localizations and maintain design consistency (e.g., Deep Purple #673AB7 and Material Green #4CAF50).

## 👨‍💻 Authors & Contributors
* **Elsa Karvouni**
* **Foteini Taramopoulou**
* **Konstantinos Pilateris** 

*Note: The team collaborated closely to perfect all application functions and produce the project documentation and demo video.*
