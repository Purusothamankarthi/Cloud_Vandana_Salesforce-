# Salesforce Validation Rule Manager

A full-stack application built with **Spring Boot 3.5.3** (Backend) and **React + Vite** (Frontend) to manage Salesforce Account Validation Rules. It authenticates users via Salesforce OAuth 2.0 and communicates with the Tooling and Metadata APIs to fetch, toggle, and deploy validation rule status changes.

## Features

- **Salesforce OAuth 2.0 Integration**: Authorize with a connected app directly from your Salesforce Org.
- **Rule Management Dashboard**: View, filter, and sort validation rules specifically for the `Account` object.
- **Toggle State**: Seamlessly enable or disable rules individually or in bulk.
- **Direct Deployment**: Deploys changes directly back to your Salesforce org via the Salesforce Metadata API in a transparent, reliable way.
- **Modern UI**: A beautiful, glassmorphism-inspired dark theme UI built in React.

## Prerequisites

1. **Java 17+**
2. **Node.js 18+**
3. **Maven**
4. A **Salesforce Developer Org** (https://developer.salesforce.com/signup)

## Salesforce Connected App Setup

To use this application, you must set up a Connected App in your Salesforce Org to obtain the OAuth credentials:

1. Log into your Salesforce Developer Org.
2. Go to **Setup** > **App Manager** > **New Connected App**.
3. Fill in the required basic information (Name, Email).
4. Check **Enable OAuth Settings**.
5. Set the **Callback URL** to `http://localhost:8080/api/auth/callback`.
6. Add the following **OAuth Scopes**:
   - Manage user data via APIs (api)
   - Perform requests at any time (refresh_token, offline_access)
   - Full access (full)
7. Save the Connected App. Wait 2-10 minutes for it to propagate.
8. Copy the **Consumer Key** (Client ID) and **Consumer Secret** (Client Secret).

## Configuration

1. In the `backend` folder, copy `.env.example` to `.env` and paste your Salesforce credentials:
   ```env
   SF_CLIENT_ID=your_consumer_key
   SF_CLIENT_SECRET=your_consumer_secret
   SF_REDIRECT_URI=http://localhost:8080/api/auth/callback
   ```
2. In the `frontend` folder, copy `.env.example` to `.env`:
   ```env
   VITE_API_BASE_URL=http://localhost:8080
   ```

## Running the Application

### Backend (Spring Boot)
Open a terminal in the `backend` directory:
```bash
cd backend
./mvnw clean spring-boot:run
```
The backend will run on http://localhost:8080.

### Frontend (React/Vite)
Open a new terminal in the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```
The frontend will be accessible at http://localhost:5173.

## Usage

1. Open http://localhost:5173 in your browser.
2. Click **Connect with Salesforce**.
3. Allow the requested permissions in the Salesforce OAuth prompt.
4. You will be redirected to the Dashboard where you can manage your Account validation rules.
