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

**Create the database:**

CREATE DATABASE rhms_db;

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


