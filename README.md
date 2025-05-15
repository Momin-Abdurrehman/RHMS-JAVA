# 🏥 Remote Healthcare Management System (RHMS)

![image](https://github.com/user-attachments/assets/fb411d4d-2d78-44ec-91ae-d7916baef692)

## 📋 Overview

The Remote Healthcare Management System (RHMS) is a Java-based console application built to help doctors, patients, and administrators connect and manage healthcare remotely. It supports appointment scheduling, health monitoring, emergency alerts, and secure communication. The system uses a MySQL database with JDBC for data storage and retrieval, and it is structured using Maven for dependency management. We also use Javax API for communication features.

## ✨ Features

### 👤 User Management

* Separate logins for Patients, Doctors, and Admins
* Secure registration with validation
* Profile updates

### 📅 Appointment Management

* Book, approve, and cancel appointments
* Get automatic reminders
* Track appointment status

### 📊 Health Data Management

* Patients can upload their vitals
* Doctors can view and manage medical records
* Automatic alerts for unusual vital readings

### 🚨 Emergency Services

* One-tap panic button
* Auto alerts when vitals cross safe limits
* Immediate messages to doctors

### 💬 Communication Tools

* Secure chat between patients and doctors
* Video consultations using Javax API
* Real-time communication

### 📲 Notification System

* Email alerts
* SMS reminders for medication and appointments

## 🛠️ Technology Stack

* **Language**: Java (JDK 11+)
* **Build Tool**: Maven
* **Database**: MySQL with JDBC integration
* **APIs**: Javax API (e.g., for video and messaging modules)
* **Interface**: Console-based using `Scanner`
* **Data Structures**: ArrayList, HashMap
* **Design**: Object-Oriented with a modular package structure

## 📦 Package Structure

```
com.rhms
├── appointmentScheduling         # Handles scheduling and management of appointments
├── Database                      # JDBC connection and database utilities
├── doctorPatientInteraction      # Chat, video calls, and messaging between users
├── emergencyAlert                # Panic button and emergency alert triggers
├── healthDataHandling            # Patient vitals and health data monitoring
├── notifications                 # Email, SMS, and reminder systems
├── reporting                     # Report generation and logs
├── ui                            # Graphical user interface (GUI) components
│   ├── controllers               # Logic controllers for handling UI actions
│   ├── resources                 # Static resources (e.g., icons, FXML files)
│   └── views                     # GUI views and screen logic
│       ├── LoginHandler.java
│       ├── MenuController.java
│       └── RhmsGuiApp.java       # Main GUI launcher
├── userManagement                # User classes and authentication logic
└── App.java                      # Console application entry point

```

## 🚀 Getting Started

### Prerequisites

* Java JDK 11 or higher
* Maven
* MySQL
* Git

### Installation
Database Setup (MySQL + JDBC)

**Create the database using this script:**
```
CREATE DATABASE IF NOT EXISTS `hospital_db`;
USE `hospital_db`;

CREATE TABLE `Users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` text,
  `user_type` enum('Patient','Doctor','Administrator') NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `specialization` varchar(100) DEFAULT 'General',
  `experience_years` int DEFAULT '0',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_username` (`username`),
  KEY `idx_users_email` (`email`)
);

CREATE TABLE `Administrators` (
  `admin_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `administrators_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE `appointments` (
  `appointment_id` int NOT NULL AUTO_INCREMENT,
  `appointment_date` datetime NOT NULL,
  `doctor_id` int DEFAULT NULL,
  `patient_id` int NOT NULL,
  `purpose` varchar(255) DEFAULT NULL,
  `status` varchar(50) DEFAULT 'Pending',
  `notes` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `notification_sent` tinyint(1) DEFAULT '0',
  `accepted_by` int DEFAULT NULL,
  PRIMARY KEY (`appointment_id`),
  KEY `fk_appointments_doctor` (`doctor_id`),
  KEY `fk_appointments_patient` (`patient_id`),
  CONSTRAINT `fk_appointments_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_appointments_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE `Chat_Messages` (
  `message_id` int NOT NULL AUTO_INCREMENT,
  `sender_id` int NOT NULL,
  `receiver_id` int NOT NULL,
  `message_text` text NOT NULL,
  `sent_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`message_id`),
  KEY `idx_chat_messages_sender` (`sender_id`),
  KEY `idx_chat_messages_receiver` (`receiver_id`),
  CONSTRAINT `chat_messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `chat_messages_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE `doctor_patient_assignments` (
  `assignment_id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `assigned_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `unique_assignment` (`doctor_id`,`patient_id`),
  KEY `fk_assignment_patient` (`patient_id`),
  CONSTRAINT `fk_assignment_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assignment_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE `Doctor_Requests` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `request_type` varchar(50) NOT NULL,
  `doctor_specialization` varchar(100) DEFAULT NULL,
  `additional_details` text,
  `request_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) DEFAULT 'Pending',
  PRIMARY KEY (`request_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `doctor_requests_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`)
);

CREATE TABLE `Doctors` (
  `doctor_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `specialization` varchar(100) NOT NULL,
  `experience_years` int NOT NULL,
  PRIMARY KEY (`doctor_id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `doctors_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE `patients` (
  `patient_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `emergency_contact` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `patients_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE `feedback_by_doctor` (
  `feedback_id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `comments` text,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`),
  KEY `doctor_id` (`doctor_id`),
  KEY `patient_id` (`patient_id`),
  CONSTRAINT `feedback_by_doctor_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`),
  CONSTRAINT `feedback_by_doctor_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`)
);

CREATE TABLE `feedback_by_patient` (
  `feedback_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `feedback_text` text NOT NULL,
  `rating` int NOT NULL,
  `submitted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`),
  KEY `patient_id` (`patient_id`),
  KEY `doctor_id` (`doctor_id`),
  CONSTRAINT `feedback_by_patient_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `feedback_by_patient_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `Doctors` (`doctor_id`) ON DELETE CASCADE,
  CONSTRAINT `feedback_by_patient_chk_1` CHECK ((`rating` between 1 and 5))
);

CREATE TABLE `Patient_Vitals` (
  `vital_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `heart_rate` double DEFAULT NULL,
  `oxygen_level` double DEFAULT NULL,
  `blood_pressure` double DEFAULT NULL,
  `temperature` double DEFAULT NULL,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`vital_id`),
  KEY `fk_user_id_idx` (`user_id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `prescription` (
  `prescription_id` int NOT NULL AUTO_INCREMENT,
  `feedback_id` int NOT NULL,
  `medication_name` varchar(255) NOT NULL,
  `dosage` varchar(100) DEFAULT NULL,
  `schedule` varchar(100) DEFAULT NULL,
  `duration` varchar(100) DEFAULT NULL,
  `instructions` text,
  PRIMARY KEY (`prescription_id`),
  KEY `fk_prescription_feedback` (`feedback_id`),
  CONSTRAINT `fk_prescription_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `feedback_by_doctor` (`feedback_id`)
);
```
Create the required tables by running the provided SQL script (if available), or manually create tables like patients, doctors, appointments, etc.

Set up the DB credentials.

**Javax Mail Setup**
(For Email Notifications)

    Add the Javax Mail dependency in pom.xml:

<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>

Configure mail settings

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/rhms.git
   cd rhms
   ```

2. Add your MySQL database credentials in the configuration file (e.g., `dbconfig.properties`).

3. Build the project with Maven:

   ```bash
   mvn clean install
   ```

4. Run the application:

   ```bash
   java -cp target/rhms-1.0.jar com.rhms.App
   ```

## 💻 Usage

After launching, choose your user type:

### 1. Patient

* Book appointments
* Upload vital signs
* Chat with doctors
* Trigger emergencies
* Join video calls

### 2. Doctor

* Approve or cancel appointments
* Check patient vitals and records
* Send feedback
* Start consultations

### 3. Admin

* Register users
* Manage appointments and vitals
* Send notifications
* View all records

## 🔄 Workflow Example

1. Admin registers new users
2. Patient books an appointment
3. Doctor approves it
4. System sends notifications
5. Patient uploads vitals regularly
6. Alerts trigger if data is critical
7. Communication happens via chat or video
8. Doctor reviews and updates records

## 🤝 Contributing

We welcome contributions.

1. Fork the repo
2. Create a feature branch

   ```bash
   git checkout -b feature/new-feature
   ```
3. Commit and push your changes

   ```bash
   git commit -m "Add new feature"
   git push origin feature/new-feature
   ```
4. Open a Pull Request

## 📞 Contact

Project Repository: [RHMS on GitHub](https://github.com/Momin-Abdurrehman/Remote-Hospital-Mangement-System)

---

⭐ If this project helps you, give it a star on GitHub! ⭐


