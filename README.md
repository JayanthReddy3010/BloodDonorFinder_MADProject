# Blood Donor Finder – Android Mobile Application

An Android application designed to connect people who need blood with registered blood donors based on blood group and location.

## About the Project

**Blood Donor Finder** is a mobile application developed as part of the Mobile Application Development Laboratory course. The application provides a platform where users can register as blood donors, search for available donors, create blood requests, and track request statuses.

The main objective is to simplify communication between blood donors and people searching for blood donors through an easy-to-use Android application.

## Objectives

- Provide a simple platform for donor registration.
- Maintain donor profiles with blood group, location, and contact information.
- Allow users to search for donors based on blood group and location.
- Enable authenticated users to create blood requests.
- Allow donors to respond to relevant blood requests.
- Provide request status tracking.

## Features

### 1. User Authentication
- User registration and login.
- Firebase Authentication integration.
- Authenticated access to application features.

### 2. Donor Registration
- Register as a blood donor.
- Provide personal and contact details.
- Select blood group.
- Enter location.
- Manage donor availability.

### 3. Find a Donor
- Search donors by blood group.
- Filter donors based on location.
- View available donor information.

### 4. Blood Request Management
- Create blood requests.
- Enter required blood group and units.
- Provide hospital and location details.
- Submit and view blood requests.

### 5. Request Status Tracking
- View submitted requests.
- Allow donors to accept or decline requests.
- Track request status.
- View updated request information.

*Feature availability depends on the implementation and testing status of the current project version.*

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Kotlin |
| UI Development | Jetpack Compose |
| UI Design | Material 3 |
| Development Environment | Android Studio |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Build System | Gradle |

## System Architecture

The application follows a simple Android client and Firebase cloud architecture.

1. **Presentation Layer:** Jetpack Compose screens for user interaction.
2. **Application Layer:** Kotlin classes handle validation, navigation, and application logic.
3. **Authentication Layer:** Firebase Authentication manages user authentication.
4. **Database Layer:** Cloud Firestore stores donor profiles and blood request information.

## Application Workflow

1. User opens the application.
2. User registers or logs in.
3. A donor can register a donor profile.
4. A requester can search for donors.
5. The requester can create a blood request.
6. Relevant donors can view and respond to requests.
7. The requester can track the request status.

## Project Structure

The project follows the standard Android application structure. The exact package organization may vary depending on the implementation.

```text
Blood-Donor-Finder/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── <package-name>/
│   │       │       ├── ui/
│   │       │       ├── navigation/
│   │       │       ├── model/
│   │       │       ├── repository/
│   │       │       └── ...
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   │
│   ├── google-services.json
│   └── build.gradle.kts
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Requirements

### Software Requirements

- Android Studio.
- Kotlin.
- Android SDK.
- Gradle.
- Firebase account.
- Firebase project with Authentication and Cloud Firestore configured.

### Hardware Requirements

- Android smartphone or Android emulator.
- Internet connection.
- Computer capable of running Android Studio.

## Setup and Installation

### Step 1: Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
```

### Step 2: Open the Project

1. Open Android Studio.
2. Select **Open**.
3. Select the cloned Blood Donor Finder project folder.
4. Wait for Gradle synchronization.

### Step 3: Configure Firebase

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Create a Firebase project.
3. Register an Android application using the exact package name of the project.
4. Download `google-services.json`.
5. Place the file inside the `app/` directory.
6. Enable Email/Password authentication.
7. Create a Cloud Firestore database.
8. Configure appropriate Firestore security rules.

**Important:** Do not commit Firebase service-account private keys, passwords, or other privileged credentials. Keep Firestore access restricted using appropriate security rules.

### Step 4: Build and Run

1. Allow Android Studio to finish Gradle synchronization.
2. Connect an Android device or start an emulator.
3. Enable USB debugging if using a physical device.
4. Click **Run** in Android Studio.

## Database

Cloud Firestore is used to store application data.

The planned collections include:

| Collection | Purpose |
|---|---|
| `users` | User account-related information, if used by the implementation |
| `donors` | Registered donor profiles |
| `bloodRequests` | Blood request details and status information |

The exact document structure and access permissions are determined by the implemented Firebase data model and security rules.

## Testing

The application should be tested for:

- User registration and login.
- Donor profile creation and retrieval.
- Donor search and filtering.
- Blood request creation.
- Donor accept and decline operations.
- Request status updates.
- Firestore data persistence.
- Invalid input handling.
- Unauthorized database access.
- Application behavior after restart.

Only verified test results should be reported as completed.

## Future Enhancements

- Push notifications for new blood requests.
- Improved donor availability management.
- Enhanced location-based donor search.
- Request history and reporting.
- Additional privacy controls for donor contact information.

## Limitations

- The application does not provide medical diagnosis or donor eligibility assessment.
- Blood availability is not guaranteed.
- Donor participation and responses depend on individual users.
- The application is intended as an academic software project and does not replace professional medical services.

## Academic Information

**Project Title:** Blood Donor Finder – Android Mobile Application

**Student Name:** G. Jayanth Reddy

**Roll Number:** 24881A05CW

**Course:** A8606 – Mobile Application Development Laboratory

**Course Facilitator:** Ms. P. Kaveri, Assistant Professor

## License

This project is developed for academic and educational purposes. A formal open-source license can be added if the project is intended for public reuse.

---

**Developed as part of the Mobile Application Development Laboratory.**
