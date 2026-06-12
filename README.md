
## Overview

**Rent Management System** is an Android application designed for landlords and property managers who want a simpler way to calculate and manage monthly tenant charges.

The app helps with:

- Monthly **rent calculation**
- **Electricity bill** calculation using submeter readings
- **Water bill** calculation
- **AI-powered assistance** using the **Gemini API**

This project is useful for managing rental properties where utility usage and tenant-wise billing need to be handled in an organized and efficient way.

---

## Features

### Rent Calculation
- Calculate and manage monthly rent payments for tenants
- Keep billing structured and easy to review

### Electricity Bill Calculation
- Supports **submeter-based billing**
- Helps calculate charges for individual tenants or units based on actual electricity consumption
- Useful for shared buildings or multi-unit rental properties

### Water Bill Calculation
- Calculate water usage charges for tenants
- Helps include utility expenses along with rent records

### AI-Powered Assistance
- Integrated with the **Gemini API**
- Can be used to provide smart assistance, insights, or user support within the application

---

## Tech Stack

| Category | Technology |
|---------|------------|
| Platform | Android |
| Language | Kotlin |
| IDE | Android Studio |
| AI Integration | Gemini API |

---

## Project Purpose

This application was built to make rent and utility management more practical and less manual. Instead of calculating charges separately every month, the system brings rent, electricity, and water billing into one Android app.

It is especially helpful for:

- Landlords managing multiple tenants
- Property managers handling monthly rent collection
- Rental setups with shared utility billing

---

## Getting Started

### Prerequisites

Before running the project, make sure you have:

- **Android Studio** installed
- A working **Android emulator** or a physical Android device
- A valid **Gemini API key**

---

## Installation and Setup

### 1. Clone the repository

```bash
git clone https://github.com/Pratham-U-dev/rent-management-system-.git
cd rent-management-system-
```

### 2. Open the project in Android Studio

- Launch **Android Studio**
- Click **Open**
- Select the project folder
- Let Android Studio import the project
- Allow it to fix any compatibility issues if prompted

### 3. Configure environment variables

Create a `.env` file in the root project directory and add your Gemini API key:

```env
GEMINI_API_KEY=your_api_key_here
```

You can check `.env.example` for the expected format.

### 4. Update the Gradle configuration

Remove the following line from the app's `build.gradle.kts` file:

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

This step is required before successfully running the application in your environment.

### 5. Run the application

- Connect an Android phone or start an emulator
- Click **Run** in Android Studio
- Wait for the build to complete and launch the app

---

## How the App Helps

The app is designed to reduce manual work in rental management by combining multiple billing-related tasks into one place.

### Example usage
A landlord can:

- Add or manage tenant information
- Calculate monthly rent
- Enter electricity usage from submeters
- Calculate water charges
- Use AI assistance for smarter interaction or support

This makes monthly billing more organized and easier to handle.

---

## AI Integration

This project includes **Gemini API** integration for AI-powered assistance.

Depending on implementation, this can help with:

- Smart suggestions
- User assistance
- Billing-related guidance
- Improved app interaction

> Make sure your Gemini API key is properly configured in the `.env` file before running the app.

---

## Project Structure

A possible high-level structure of the project may look like this:

```bash
rent-management-system-/
├── app/
├── gradle/
├── .env.example
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── ...
```

> Update this section if you want to reflect the exact folder and file structure from your repository.

---

## Learning Outcomes

This project is a good example of combining:

- Android app development with **Kotlin**
- Real-world billing logic
- Utility consumption calculation
- API integration with **Gemini**
- Practical problem-solving for rental management

It demonstrates how mobile apps can be used to solve everyday management problems in a structured and scalable way.

---

## License

This project is intended for **educational and learning purposes** unless stated otherwise.

---

## Author

**Pratham**  
