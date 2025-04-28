# 🏥 Remote Healthcare Management System (RHMS)
![image](https://github.com/user-attachments/assets/fb411d4d-2d78-44ec-91ae-d7916baef692)


## 📋 Overview

The Remote Healthcare Management System (RHMS) is a comprehensive Java-based solution designed to bridge the gap between healthcare providers and patients through digital means. RHMS facilitates remote healthcare services including appointment scheduling, vital signs monitoring, emergency alerts, secure communication, and more.

## ✨ Features

### 👤 User Management
- Multi-user support (Patients, Doctors, Administrators)
- Secure registration and authentication
- Profile management

### 📅 Appointment Management
- Schedule, approve, and cancel appointments
- Automated reminders and notifications
- Appointment status tracking

### 📊 Health Data Management
- Upload and monitor vital signs
- Medical records management
- Automated alerts for abnormal vitals

### 🚨 Emergency Services
- Panic button functionality
- Automatic emergency alerts based on vital signs
- Immediate notification to healthcare providers

### 💬 Communication Tools
- Secure messaging between patients and doctors
- Video consultation capabilities
- Real-time chat functionality

### 📲 Notification System
- Email notifications
- SMS alerts
- Medication and appointment reminders

## 🛠️ Technology Stack

- **Language**: Java
- **Console-Based Interface**: Scanner for user input
- **Data Structure**: ArrayList, HashMap for data management
- **Organization**: Object-oriented design with package structure

## 📦 Package Structure

```
com.rhms
├── appointmentScheduling
│   ├── Appointment.java
│   └── AppointmentManager.java
├── doctorPatientInteraction
│   ├── ChatClient.java
│   ├── ChatServer.java
│   └── VideoCall.java
├── emergencyAlert
│   ├── EmergencyAlert.java
│   └── PanicButton.java
├── healthDataHandling
│   └── VitalSign.java
├── notifications
│   ├── EmailNotification.java
│   ├── ReminderService.java
│   └── SMSNotification.java
├── userManagement
│   ├── Doctor.java
│   └── Patient.java
└── App.java
```

## 🚀 Getting Started

### Prerequisites

- Java JDK 11 or higher
- Git (for cloning the repository)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/rhms.git
   cd rhms
   ```

2. Compile the project:
   ```bash
   javac -d bin src/com/rhms/App.java
   ```

3. Run the application:
   ```bash
   java -cp bin com.rhms.App
   ```

## 💻 Usage

After launching the application, you will be presented with a user type selection menu:

1. **Patient Interface**:
   - Schedule appointments
   - View personal vitals
   - Provide feedback to doctors
   - Trigger emergency alerts
   - Join video consultations
   - Chat with healthcare providers

2. **Doctor Interface**:
   - Approve or cancel appointments
   - Upload patient vital signs
   - View patient medical records
   - Provide feedback to patients
   - Start video consultations
   - Chat with patients

3. **Admin Interface**:
   - Register new patients and doctors
   - Schedule appointments
   - Upload vital signs
   - Send notifications
   - View all appointments

## 🔄 Workflow Example

1. Admin registers patients and doctors in the system
2. Patient schedules an appointment with a doctor
3. Doctor approves the appointment
4. System sends confirmation and reminder notifications
5. Patient uploads vital signs regularly
6. If vital signs are concerning, system triggers emergency alerts
7. Doctor and patient can communicate via chat or video consultation
8. Doctor provides feedback and updates medical records

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request


## 📞 Contact

Project Link: [(https://github.com/Momin-Abdurrehman/Remote-Hospital-Mangement-System)]

---

⭐️ If you find this project useful, please consider giving it a star on GitHub! ⭐️
