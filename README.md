# 🏥 Remote Healthcare Management System (RHMS)

![RHMS Diagram](https://github.com/user-attachments/assets/fb411d4d-2d78-44ec-91ae-d7916baef692)

## 📋 Overview

The Remote Healthcare Management System (RHMS) is a Java-based console application designed to facilitate remote healthcare interactions among patients, doctors, and administrators. RHMS offers appointment scheduling, health monitoring, emergency alerts, and secure communication, backed by a MySQL database and JDBC. The project uses Maven for dependency management and Javax APIs for messaging and video services.

## ✨ Features

### 👤 User Management

* Role-based logins for Patients, Doctors, and Administrators
* Secure registration with validation
* Profile updates

### 📅 Appointment Management

* Patients can book appointments; doctors can approve or cancel
* Automatic reminders via email/SMS
* Appointment status tracking

### 📊 Health Data Management

* Patients upload vital signs (heart rate, blood pressure, etc.)
* Doctors view and annotate medical records
* Automatic alerts for out-of-range vitals

### 🚨 Emergency Services

* One-tap panic button for emergencies
* Automatic alerts to on-call doctors
* Real-time chat initiation during critical events

### 💬 Communication Tools

* Secure text chat between users
* Video consultations via Javax API
* Message read receipts and archiving

### 📲 Notification System

* Email notifications using Javax Mail
* SMS reminders for appointments and medication

## 🛠️ Technology Stack

* **Language:** Java (JDK 11+)
* **Build Tool:** Maven
* **Database:** MySQL with JDBC
* **Communication APIs:** Javax Mail, Javax WebSocket
* **Interface:** Console-based (`Scanner`)
* **Data Structures:** ArrayList, HashMap
* **Design Pattern:** Modular OOP packages

## 📦 Package Structure

```
com.rhms
├── appointmentScheduling
│   ├ Appointment.java
│   └ AppointmentManager.java
├── Database
│   ├ DatabaseConnection.java
│   ├ AppointmentDatabaseHandler.java
│   ├ ChatMessageDatabaseHandler.java
│   ├ DoctorPatientAssignmentHandler.java
│   ├ FeedbackDatabaseHandler.java
│   ├ MedicalHistoryDatabaseHandler.java
│   ├ MedicalRecordDatabaseHandler.java
│   ├ PrescriptionDatabaseHandler.java
│   ├ UserDatabaseHandler.java
│   ├ VitalSignDatabaseHandler.java
│   └ VitalsDatabase.java
├── doctorPatientInteraction
│   ├ ChatClient.java
│   ├ ChatServer.java
│   ├ Feedback.java
│   ├ MedicalHistory.java
│   ├ Prescription.java
│   └ VideoCall.java
├── emergencyAlert
│   ├ EmergencyAlert.java
│   └ PanicButton.java
├── healthDataHandling
│   ├ CSVVitalsUploader.java
│   ├ MedicalRecord.java
│   ├ VitalSign.java
│   ├ VitalsDatabase.java
│   └ VitalsUploadReport.java
├── notifications
│   ├ EmailNotification.java
│   ├ ReminderService.java
│   └ SMSNotification.java
├── reporting
│   ├ DownloadHandler.java
│   ├ ReportFormat.java
│   └ ReportGenerator.java
├── ui
│   ├ controllers
│   │   ├ AddAdminDashboardController.java
│   │   ├ AdminDashboardController.java
│   │   ├ AssignDoctorController.java
│   │   ├ ChatWithDoctorDashboard.java
│   │   ├ ChatWithPatientDashboard.java
│   │   ├ DashboardController.java
│   │   ├ DoctorAppointmentsController.java
│   │   ├ DoctorDashboardController.java
│   │   ├ DoctorFeedbackController.java
│   │   ├ DoctorPatientDetailsController.java
│   │   ├ LoginViewController.java
│   │   ├ ManageUsersDashboardController.java
│   │   ├ MedicalRecordViewController.java
│   │   ├ PatientAppointmentsController.java
│   │   ├ PatientDashboardController.java
│   │   ├ PrescriptionViewController.java
│   │   ├ RegistrationController.java
│   │   ├ ReportGenerationController.java
│   │   ├ ScheduleAppointmentController.java
│   │   ├ SendEmailDashboardController.java
│   │   └ VitalSignsDashboard.java
│   └ resources
│       ├ views (FXML files)
│       └ styles.css
├── userManagement
│   ├ Administrator.java
│   ├ Doctor.java
│   ├ Patient.java
│   ├ User.java
│   └ UserManager.java
└── App.java
```

### DEMO VIDEO:
```
https://drive.google.com/file/d/1EGaJAOSugzxAt_U2s3aqVi_aTnek2AhX/view?usp=sharing
```

### GUI Screenshot:
<img width="1401" alt="image" src="https://github.com/user-attachments/assets/40fe5d47-9333-4e40-afc7-48ecd058d863" />



---

## 🚀 Getting Started

### Prerequisites

1. **Java JDK 11 or higher**
2. **Maven 3.6+**
3. **MySQL Server**
4. **Git**

### 1a. Clone the Repository

```bash
git clone https://github.com/Momin-Abdurrehman/Remote-Hospital-Mangement-System.git
cd Remote-Hospital-Mangement-System
```

### 1b. Adding Dependencies to Pom.xml:
To enable email notifications and database connectivity, add the following to your pom.xml inside the <dependencies> section:
```
<dependencies>
  <!-- Javax Mail (for EmailNotification) -->
  <dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
  </dependency>

  <!-- Javax WebSocket (if you use video/chat over WebSocket) -->
  <dependency>
    <groupId>javax.websocket</groupId>
    <artifactId>javax.websocket-api</artifactId>
    <version>1.1</version>
  </dependency>

  <!-- MySQL Connector/J (for JDBC) -->
  <dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
  </dependency>

  <!-- (Other existing dependencies…) -->
</dependencies>
```
### 2. Database Setup

1. **Start MySQL** (default port 3306) or note your custom host/port.

2. **Create Database**:

   ```sql
   CREATE DATABASE IF NOT EXISTS `hospital_db`;
   USE `hospital_db`;
   ```

3. **Create Tables**: Execute the full DDL below in your SQL console.

   ```sql
   -- Users table
   CREATE TABLE `Users` (
     `user_id` INT NOT NULL AUTO_INCREMENT,
     `username` VARCHAR(50) NOT NULL,
     `password_hash` VARCHAR(255) NOT NULL,
     `name` VARCHAR(100) NOT NULL,
     `email` VARCHAR(100) NOT NULL,
     `phone` VARCHAR(20) DEFAULT NULL,
     `address` TEXT,
     `user_type` ENUM('Patient','Doctor','Administrator') NOT NULL,
     `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
     `specialization` VARCHAR(100) DEFAULT 'General',
     `experience_years` INT DEFAULT '0',
     PRIMARY KEY (`user_id`),
     UNIQUE KEY `username` (`username`),
     UNIQUE KEY `email` (`email`)
   );

   -- Administrators
   CREATE TABLE `Administrators` (
     `admin_id` INT NOT NULL AUTO_INCREMENT,
     `user_id` INT NOT NULL,
     PRIMARY KEY (`admin_id`),
     UNIQUE KEY `user_id` (`user_id`),
     CONSTRAINT `administrators_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
   );

   -- Appointments
   CREATE TABLE `appointments` (
     `appointment_id` INT NOT NULL AUTO_INCREMENT,
     `appointment_date` DATETIME NOT NULL,
     `doctor_id` INT DEFAULT NULL,
     `patient_id` INT NOT NULL,
     `purpose` VARCHAR(255) DEFAULT NULL,
     `status` VARCHAR(50) DEFAULT 'Pending',
     `notes` VARCHAR(500) DEFAULT NULL,
     `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
     `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `notification_sent` TINYINT(1) DEFAULT '0',
     `accepted_by` INT DEFAULT NULL,
     PRIMARY KEY (`appointment_id`),
     KEY `fk_appointments_doctor` (`doctor_id`),
     KEY `fk_appointments_patient` (`patient_id`),
     CONSTRAINT `fk_appointments_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `Users` (`user_id`) ON DELETE SET NULL,
     CONSTRAINT `fk_appointments_patient` FOREIGN KEY (`patient_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
   );

   -- Chat Messages
   CREATE TABLE `Chat_Messages` (
     `message_id` INT NOT NULL AUTO_INCREMENT,
     `sender_id` INT NOT NULL,
     `receiver_id` INT NOT NULL,
     `message_text` TEXT NOT NULL,
     `sent_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
     `is_read` TINYINT(1) DEFAULT '0',
     PRIMARY KEY (`message_id`),
     KEY `idx_chat_messages_sender` (`sender_id`),
     KEY `idx_chat_messages_receiver` (`receiver_id`),
     CONSTRAINT `chat_messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE,
     CONSTRAINT `chat_messages_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
   );

   -- Doctor-Patient Assignments
   CREATE TABLE `doctor_patient_assignments` (
     `assignment_id` INT NOT NULL AUTO_INCREMENT,
     `doctor_id` INT NOT NULL,
     `patient_id` INT NOT NULL,
     `assigned_date` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`assignment_id`),
     UNIQUE KEY `unique_assignment` (`doctor_id`,`patient_id`),
     KEY `fk_assignment_patient` (`patient_id`),
     CONSTRAINT `fk_assignment_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE,
     CONSTRAINT `fk_assignment_patient` FOREIGN KEY (`patient_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
   );

   -- Doctor Requests
   CREATE TABLE `Doctor_Requests` (
     `request_id` INT NOT NULL AUTO_INCREMENT,
     `user_id` INT NOT NULL,
     `request_type` VARCHAR(50) NOT NULL,
     `doctor_specialization` VARCHAR(100) DEFAULT NULL,
     `additional_details` TEXT,
     `request_date` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
     `status` VARCHAR(20) DEFAULT 'Pending',
     PRIMARY KEY (`request_id`),
     KEY `user_id` (`user_id`),
     CONSTRAINT `doctor_requests_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`)
   );

   -- Doctors
   CREATE TABLE `Doctors` (
     `doctor_id` INT NOT NULL AUTO_INCREMENT,
     `user_id` INT NOT NULL,
     `specialization` VARCHAR(100) NOT NULL,
     `experience_years` INT NOT NULL,
     PRIMARY KEY (`doctor_id`),
     UNIQUE KEY `user_id` (`user_id`),
     CONSTRAINT `doctors_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
   );

   -- Patients
   CREATE TABLE `patients` (
     `patient_id` INT NOT NULL AUTO_INCREMENT,
     `user_id` INT NOT NULL,
     `emergency_contact` VARCHAR(100) DEFAULT NULL,
     PRIMARY KEY (`patient_id`),
     UNIQUE KEY `user_id` (`user_id`),
     CONSTRAINT `patients_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
   );

   -- Feedback by Doctor
   CREATE TABLE `feedback_by_doctor` (
     `feedback_id` INT NOT NULL AUTO_INCREMENT,
     `doctor_id` INT NOT NULL,
     `patient_id` INT NOT NULL,
     `comments` TEXT,
     `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`feedback_id`),
     KEY `doctor_id` (`doctor_id`),
     KEY `patient_id` (`patient_id`),
     CONSTRAINT `feedback_by_doctor_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `Doctors` (`doctor_id`),
     CONSTRAINT `feedback_by_doctor_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`)
   );

   -- Feedback by Patient
   CREATE TABLE `feedback_by_patient` (
     `feedback_id` INT NOT NULL AUTO_INCREMENT,
     `patient_id` INT NOT NULL,
     `doctor_id` INT NOT NULL,
     `feedback_text` TEXT NOT NULL,
     `rating` INT NOT NULL,
     `submitted_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`feedback_id`),
     KEY `patient_id` (`patient_id`),
     KEY `doctor_id` (`doctor_id`),
     CONSTRAINT `feedback_by_patient_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`) ON DELETE CASCADE,
     CONSTRAINT `feedback_by_patient_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `Doctors` (`doctor_id`) ON DELETE CASCADE,
     CONSTRAINT `feedback_by_patient_chk_1` CHECK ((`rating` BETWEEN 1 AND 5))
   );

   -- Patient Vitals
   CREATE TABLE `Patient_Vitals` (
     `vital_id` INT NOT NULL AUTO_INCREMENT,
     `user_id` INT NOT NULL,
     `heart_rate` DOUBLE DEFAULT NULL,
     `oxygen_level` DOUBLE DEFAULT NULL,
     `blood_pressure` DOUBLE DEFAULT NULL,
     `temperature` DOUBLE DEFAULT NULL,
     `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`vital_id`),
     KEY `fk_user_id_idx` (`user_id`),
     CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`)
   );

   -- Prescriptions
   CREATE TABLE `prescription` (
     `prescription_id` INT NOT NULL AUTO_INCREMENT,
     `feedback_id` INT NOT NULL,
     `medication_name` VARCHAR(255) NOT NULL,
     `dosage` VARCHAR(100) DEFAULT NULL,
     `schedule` VARCHAR(100) DEFAULT NULL,
     `duration` VARCHAR(100) DEFAULT NULL,
     `instructions` TEXT,
     PRIMARY KEY (`prescription_id`),
     KEY `fk_prescription_feedback` (`feedback_id`),
     CONSTRAINT `fk_prescription_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `feedback_by_doctor` (`feedback_id`)
   );
   ```

4. **Verify** tables:

   ```sql
   SHOW TABLES;
   ```

### 3. Configure Database Connection

Open `src/main/java/com/rhms/Database/DatabaseConnection.java` and update the constants:

```java
private static final String JDBC_URL = "jdbc:mysql://localhost:3306/hospital_db?useSSL=false&serverTimezone=UTC"; // <-- your DB URL
private static final String USERNAME = "root";        // <-- your MySQL username
private static final String PASSWORD = "Diamond4056!"; // <-- your MySQL password
```

Save the file.

### 4. Configure Email Notifications

Open `src/main/java/com/rhms/notifications/EmailNotification.java` and replace:

```java
private final String senderEmail = "remotehospitalsystem@gmail.com";  // <-- your Gmail
private final String senderPassword = "vcgs lcil ubcp cept";       // <-- your Gmail App Password
```

1. Enable 2-Step Verification on your Gmail.
2. Generate an App Password (Mail) and paste it as `senderPassword`.

### 5. (Optional) Externalize Credentials

Create `src/main/resources/dbconfig.properties`:

```properties
jdbc.url=jdbc:mysql://localhost:3306/hospital_db?useSSL=false&serverTimezone=UTC
jdbc.username=YOUR_DB_USERNAME
jdbc.password=YOUR_DB_PASSWORD

mail.sender=your.email@gmail.com
mail.app.password=YOUR_APP_PASSWORD
```

Load via `java.util.Properties` in your classes.

### 6. Build & Run

```bash
mvn clean install
java -cp target/rhms-1.0.jar com.rhms.App
```

Follow console prompts to log in as Patient, Doctor, or Admin.

---

## 🛠️ Troubleshooting

* **SMTP Errors**: Verify SMTP host `smtp.gmail.com` and port `587`, check network.
* **SQL Access Denied**: Confirm DB credentials and grant privileges:

  ```sql
  GRANT ALL ON hospital_db.* TO 'your_user'@'localhost';
  ```
* **Missing Tables**: Ensure DDL executed on `hospital_db` and JDBC URL is correct.

---

## 🤝 Contributing

1. Fork this repo
2. Create a feature branch:

   ```bash
   ```

git checkout -b feature/YourFeature

````
3. Commit & push:
   ```bash
git commit -m "Add feature"
git push origin feature/YourFeature
````

4. Open a Pull Request.

---

## 📞 Contact

Project: [RHMS on GitHub](https://github.com/Momin-Abdurrehman/Remote-Hospital-Mangement-System)

⭐ If this project helps you, please give it a star! ⭐
