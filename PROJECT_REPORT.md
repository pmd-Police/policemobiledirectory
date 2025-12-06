# 📱 Police Mobile Directory - Project Report

## Executive Summary

**Police Mobile Directory** is a comprehensive Android application designed for managing police personnel information, facilitating communication, and streamlining administrative workflows for the Karnataka Police Department. The application leverages modern Android development practices with offline-first architecture, real-time synchronization, and robust security features.

---

## 1. Project Overview

### 1.1 Purpose
- **Digital Directory Management**: Centralized repository of police personnel information
- **Communication Platform**: Push notifications and messaging system
- **Administrative Tools**: Employee management, document distribution, and approval workflows
- **Offline Capability**: Full functionality even without internet connectivity

### 1.2 Target Users
- **Regular Officers**: Search contacts, view documents, receive notifications
- **Administrators**: Manage employees, approve registrations, send notifications, upload documents
- **Supervisors**: District/Station-level management and reporting

### 1.3 Key Metrics
- **Platform**: Android (Min SDK 26, Target SDK 34)
- **Architecture**: MVVM with Clean Architecture principles
- **Offline Support**: Room Database with Firestore sync
- **Real-time Updates**: Firebase Firestore listeners
- **Language**: Kotlin 100%

---

## 2. System Architecture

### 2.1 Architectural Pattern
**MVVM (Model-View-ViewModel) with Repository Pattern**

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  (Jetpack Compose Screens + Components)                     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    ViewModel Layer                          │
│  • EmployeeViewModel    • ConstantsViewModel               │
│  • DocumentsViewModel   • GalleryViewModel                 │
│  • AddEditEmployeeViewModel                                │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  Repository Layer                           │
│  • EmployeeRepository    • ConstantsRepository             │
│  • DocumentsRepository   • GalleryRepository               │
│  • ImageRepository       • PendingRegistrationRepository   │
└──────┬─────────────────────────────────────┬───────────────┘
       │                                     │
┌──────▼──────────────────┐    ┌────────────▼──────────────┐
│   Local Data Source     │    │   Remote Data Source      │
│  • Room Database        │    │  • Firebase Firestore     │
│  • SharedPreferences    │    │  • Firebase Storage       │
│  • DataStore           │    │  • Google Sheets (via API)│
│                        │    │  • Google Apps Script     │
└────────────────────────┘    └────────────────────────────┘
```

### 2.2 Dependency Injection
**Hilt (Dagger)** for compile-time dependency injection
- **Modules**:
  - `AppModule`: Repository and service bindings
  - `DatabaseModule`: Room database setup
  - `NetworkModule`: Retrofit API clients
  - `FirebaseModule`: Firebase service instances
  - `DispatchersModule`: Coroutine dispatchers

### 2.3 Data Layer Architecture

#### Local Database (Room)
- **EmployeeEntity**: Cached employee records
- **PendingRegistrationEntity**: Pending user registrations
- **AppIconEntity**: Application icons cache
- **FTS (Full-Text Search)**: EmployeeEntityFts for fast searching

#### Remote Data Sources
1. **Firebase Firestore**: Primary database
   - `employees` collection
   - `pending_registrations` collection
   - `notifications_queue` collection
   - `documents` collection
   - `gallery` collection
   - `useful_links` collection

2. **Firebase Storage**: File storage
   - Profile images
   - Document files (PDF, DOCX)
   - Gallery images

3. **Google Sheets**: Administrative data management
   - Employee profiles sync
   - Constants management (districts, stations, ranks)

4. **Google Apps Script**: Backend automation
   - Employee sync operations
   - Image upload processing
   - Document management
   - Notification queuing

### 2.4 UI Framework
**Jetpack Compose** with Material Design 3
- Declarative UI
- State-driven composition
- Navigation Compose for routing
- Theming with dark mode support

---

## 3. Core Features

### 3.1 Authentication & Authorization

#### Login Methods
1. **Email + PIN Authentication**
   - PIN hashed with bcrypt (offline-first)
   - Fallback to Firestore for online verification
   - Session persistence with DataStore

2. **Google Sign-In**
   - OAuth 2.0 authentication
   - Credentials Manager integration
   - Automatic account linking

#### Password Management
- **Forgot PIN**: OTP-based reset
- **Change PIN**: Secure PIN update with current PIN verification
- **Session Management**: Automatic session restoration

#### Role-Based Access Control
- **Admin Users**: Full system access
- **Regular Users**: Limited to personal data and search
- **Pending Users**: Registration approval workflow

### 3.2 Employee Management

#### Employee Directory
- **Search & Filter**:
  - By Name, KGID, Mobile, Station, Rank, Metal Number
  - Real-time search with FTS (Full-Text Search)
  - District/Station-based filtering
  
- **Employee Details**:
  - Personal Information (Name, Rank, KGID)
  - Contact Details (Mobile, Email)
  - Station & District Assignment
  - Profile Photo
  - Blood Group
  - Metal Number (for eligible ranks)

#### Employee CRUD Operations
- **Add Employee**: Admin-only, with validation
- **Edit Employee**: Admin or self-editing (limited fields)
- **Delete Employee**: Soft delete (isDeleted flag)
- **Bulk Import**: CSV upload for mass employee addition

#### Profile Management
- **My Profile**: View and edit own profile
- **Photo Upload**: Profile image with cropping (uCrop)
- **Profile Sync**: Automatic sync across devices

### 3.3 Registration Workflow

#### User Registration Process
```
1. New User Registration
   ├── Fill Registration Form
   ├── Upload Profile Photo (optional)
   ├── Submit to Pending Registrations
   └── Await Admin Approval

2. Admin Approval Process
   ├── View Pending Registrations
   ├── Review Details & Photo
   ├── Approve or Reject
   └── Notification Sent to User

3. Post-Approval
   ├── User receives approval notification
   ├── Account activated
   └── Access granted to full features
```

#### Registration Features
- **Duplicate Prevention**: Checks for existing KGID/Email
- **Photo Upload**: Optional profile photo during registration
- **Validation**: Comprehensive form validation
- **Status Tracking**: Pending → Approved/Rejected

### 3.4 Notification System

#### Notification Types
1. **Admin Notifications**: System alerts for admins
2. **User Notifications**: Personal notifications for users
3. **Push Notifications**: Real-time FCM notifications

#### Notification Targeting
- **ALL**: Broadcast to all users
- **SINGLE**: Target specific user by KGID
- **DISTRICT**: All users in a district
- **STATION**: All users in a specific station
- **ADMIN**: All admin users

#### Notification Features
- Real-time delivery via Firebase Cloud Messaging
- In-app notification history
- Read/unread status tracking
- Notification filtering by type
- Background notification handling

### 3.5 Documents Management

#### Document Upload
- **File Types**: PDF, DOCX, Images
- **Storage**: Firebase Storage
- **Metadata**: Title, Description, Category
- **Access Control**: Admin upload, public/restricted access

#### Document Features
- **Document List**: Categorized document listing
- **Download**: Offline caching
- **View**: In-app document viewer (PDF)
- **Search**: Search documents by title/description

#### Document Workflow
```
Admin Upload → Firebase Storage → Firestore Metadata → 
App Sync → Local Cache → User Access
```

### 3.6 Gallery

#### Gallery Features
- **Image Collection**: Department photo gallery
- **Categories**: Organized by events/occasions
- **View & Share**: Full-screen image viewing
- **Sync**: Automatic sync from Google Sheets

### 3.7 Useful Links

#### Features
- **Link Management**: Curated list of useful resources
- **App Links**: Direct links to other apps/websites
- **APK Distribution**: Download links for related apps
- **Icon Display**: Custom icons for each link

#### Link Types
- Play Store Links
- APK Downloads
- Web Links
- Internal App Navigation

### 3.8 Constants Management

#### Constants Types
- **Districts**: All Karnataka districts
- **Stations**: Stations mapped by district
- **Ranks**: Police ranks hierarchy
- **Blood Groups**: Available blood groups

#### Sync Mechanism
- **Google Sheets Integration**: Master data in Google Sheets
- **Caching**: Local cache with expiration (30 days)
- **Version Control**: Version-based updates
- **Fallback**: Hardcoded constants as backup
- **Auto-refresh**: Background sync on app launch

### 3.9 Statistics & Reporting

#### Employee Statistics (Admin)
- **Total Employees**: Count of all employees
- **Active Employees**: Non-deleted employees
- **By District**: Employee count per district
- **By Station**: Employee count per station
- **By Rank**: Distribution by rank
- **Deletion Stats**: Soft-deleted employee count

### 3.10 Additional Features

#### Nudi Converter
- **Language Conversion**: Kannada script conversion tool
- **Text Processing**: Utility for Kannada text handling

#### Terms & Conditions
- Legal terms and privacy policy display

#### About Screen
- App information and version details

---

## 4. Workflows

### 4.1 User Registration Workflow

```
┌──────────────┐
│ User Opens   │
│   App        │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Login Screen │
└──────┬───────┘
       │
       ▼
┌─────────────────┐      ┌──────────────────┐
│ "Register New"  │─────▶│ User Registration│
│    Button       │      │     Screen       │
└─────────────────┘      └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Fill Form +      │
                         │ Upload Photo     │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Submit to        │
                         │ Pending Queue    │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Admin Notification│
                         │ Sent (Firebase)  │
                         └──────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
                    ▼                           ▼
         ┌──────────────────┐       ┌──────────────────┐
         │  Admin Approves  │       │  Admin Rejects   │
         └────────┬─────────┘       └────────┬─────────┘
                  │                          │
                  ▼                          ▼
         ┌──────────────────┐       ┌──────────────────┐
         │ User Notified    │       │ User Notified    │
         │ Account Active   │       │ Registration     │
         │                  │       │ Rejected         │
         └──────────────────┘       └──────────────────┘
```

### 4.2 Employee Sync Workflow

```
┌──────────────────┐
│ Google Sheets    │
│ (Source of Truth)│
└────────┬─────────┘
         │
         │ Apps Script Sync
         ▼
┌──────────────────┐
│ Firebase         │
│ Firestore        │
└────────┬─────────┘
         │
         │ Real-time Sync
         ▼
┌──────────────────┐
│ Android App      │
│ (Room Database)  │
└────────┬─────────┘
         │
         │ Offline Cache
         ▼
┌──────────────────┐
│ UI Display       │
└──────────────────┘
```

**Sync Direction**:
- **Push**: Sheet → Firestore → App
- **Pull**: App → Firestore → Sheet (via Apps Script)

### 4.3 Image Upload Workflow

```
┌──────────────────┐
│ User Selects     │
│ Profile Image    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Crop & Compress  │
│ (uCrop)          │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Convert to       │
│ Base64           │
└────────┬─────────┘
         │
         │ POST /exec?action=uploadImage
         ▼
┌──────────────────┐
│ Google Apps      │
│ Script           │
└────────┬─────────┘
         │
         ├──────────────────┐
         │                  │
         ▼                  ▼
┌──────────────────┐ ┌──────────────┐
│ Upload to Google │ │ Update Sheet │
│ Drive            │ │ (photoUrl)   │
└────────┬─────────┘ └──────────────┘
         │
         │ Get Public URL
         ▼
┌──────────────────┐
│ Update Firestore │
│ (photoUrl)       │
└────────┬─────────┘
         │
         │ Sync to App
         ▼
┌──────────────────┐
│ Display Updated  │
│ Photo in App     │
└──────────────────┘
```

### 4.4 Notification Delivery Workflow

```
┌──────────────────┐
│ Admin Creates    │
│ Notification     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Write to         │
│ notifications_   │
│ queue (Firestore)│
└────────┬─────────┘
         │
         │ Firebase Function Trigger
         ▼
┌──────────────────┐
│ Cloud Function   │
│ Processes Queue  │
└────────┬─────────┘
         │
         ├──────────────────────────────┐
         │                              │
         ▼                              ▼
┌──────────────────┐          ┌──────────────────┐
│ Filter Recipients│          │ Get FCM Tokens   │
│ (Target Logic)   │          │ from Firestore   │
└────────┬─────────┘          └────────┬─────────┘
         │                              │
         └──────────────┬───────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │ Send via FCM API │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ Android Devices  │
              │ Receive Push     │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ In-App Display   │
              │ & History        │
              └──────────────────┘
```

### 4.5 Offline-First Data Flow

```
┌─────────────────────────────────────────────────┐
│            App Startup / Data Request           │
└────────────────────┬────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│ Check Room DB    │    │ Check Cache      │
│ (Local)          │    │ Age (30 days)    │
└────────┬─────────┘    └────────┬─────────┘
         │                       │
         │ Data Found?           │ Cache Valid?
         │                       │
         ▼                       ▼
    ┌─────────┐            ┌──────────┐
    │ Display │            │ Use Cache│
    │ Data    │            └────┬─────┘
    └─────────┘                 │
                                │
                                ▼
                       ┌──────────────────┐
                       │ Background Sync  │
                       │ (Firestore)      │
                       └────────┬─────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │ Update Room DB   │
                       │ & Cache          │
                       └──────────────────┘
```

---

## 5. Technical Stack

### 5.1 Core Android Technologies

| Technology | Purpose | Version |
|------------|---------|---------|
| **Kotlin** | Programming Language | Latest |
| **Jetpack Compose** | UI Framework | Latest |
| **Material Design 3** | Design System | Latest |
| **Navigation Compose** | Navigation | Latest |
| **Room** | Local Database | Latest |
| **DataStore** | Key-Value Storage | 1.1.1 |
| **Coroutines** | Async Operations | 1.8.1 |
| **Flow** | Reactive Streams | Built-in |

### 5.2 Dependency Injection & Architecture

| Technology | Purpose |
|------------|---------|
| **Hilt** | Dependency Injection |
| **ViewModel** | UI State Management |
| **LiveData/StateFlow** | Observable Data |
| **Repository Pattern** | Data Abstraction |

### 5.3 Backend & Cloud Services

| Service | Purpose |
|---------|---------|
| **Firebase Firestore** | NoSQL Database |
| **Firebase Storage** | File Storage |
| **Firebase Auth** | Authentication |
| **Firebase Cloud Messaging** | Push Notifications |
| **Firebase Functions** | Serverless Backend |
| **Google Sheets API** | Data Management |
| **Google Apps Script** | Automation & API |

### 5.4 Networking & APIs

| Technology | Purpose |
|------------|---------|
| **Retrofit** | REST API Client |
| **Gson** | JSON Serialization |
| **OkHttp** | HTTP Client |
| **Logging Interceptor** | Request/Response Logging |

### 5.5 Image Processing

| Technology | Purpose |
|------------|---------|
| **Coil** | Image Loading |
| **uCrop** | Image Cropping |
| **ImagesService** | Image Resizing (Apps Script) |

### 5.6 Document Processing

| Technology | Purpose |
|------------|---------|
| **PDFBox Android** | PDF Processing |
| **Apache POI** | DOCX Processing |
| **Aalto XML** | XML Parsing (Android) |

### 5.7 Utilities

| Technology | Purpose |
|------------|---------|
| **Commons IO** | File Operations |
| **Credential Manager** | Google Sign-In |
| **MultiDex** | Multi-DEX Support |

---

## 6. Data Models

### 6.1 Employee Model
```kotlin
data class Employee(
    val kgid: String,              // Primary Key
    val name: String,
    val email: String,
    val mobile1: String,
    val mobile2: String?,
    val rank: String,
    val station: String,
    val district: String,
    val metalNumber: String?,
    val bloodGroup: String?,
    val photoUrl: String?,
    val pin: String? (hashed),
    val fcmToken: String?,
    val isAdmin: Boolean,
    val isApproved: Boolean,
    val isDeleted: Boolean,
    val createdAt: Date,
    val updatedAt: Date
)
```

### 6.2 Notification Model
```kotlin
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val targetType: NotificationTarget,
    val targetKgid: String?,
    val targetDistrict: String?,
    val targetStation: String?,
    val timestamp: Date,
    val isRead: Boolean
)
```

### 6.3 Document Model
```kotlin
data class Document(
    val id: String,
    val title: String,
    val description: String?,
    val fileUrl: String,
    val fileType: String,
    val category: String?,
    val uploadedBy: String,
    val uploadedAt: Date,
    val isPublic: Boolean
)
```

---

## 7. Security Features

### 7.1 Authentication Security
- **PIN Hashing**: bcrypt hashing for PINs
- **Session Management**: Secure session tokens
- **OAuth 2.0**: Google Sign-In with secure credential storage
- **Token Management**: FCM token rotation and validation

### 7.2 Data Security
- **Firestore Rules**: Role-based access control
- **Encryption**: Data in transit (HTTPS)
- **Offline Security**: Local database encryption (Room)
- **Input Validation**: Client-side and server-side validation

### 7.3 Authorization
- **Role-Based Access**: Admin vs Regular User
- **Resource-Level Permissions**: Fine-grained access control
- **Audit Logging**: Operation tracking for sensitive actions

---

## 8. Performance Optimizations

### 8.1 Offline-First Architecture
- **Local Caching**: Room database for offline access
- **Background Sync**: Non-blocking data synchronization
- **Cache Management**: Smart cache invalidation (30-day expiration)
- **Incremental Updates**: Only sync changed data

### 8.2 Image Optimization
- **Compression**: Image compression before upload
- **Resizing**: Server-side image resizing (512x512)
- **Lazy Loading**: Coil for efficient image loading
- **Caching**: Image caching with Coil

### 8.3 Database Optimization
- **Indexing**: FTS indexes for fast searching
- **Pagination**: Paginated queries for large datasets
- **Query Optimization**: Efficient Firestore queries
- **Batch Operations**: Batch writes for bulk updates

### 8.4 UI Performance
- **Compose Optimization**: Efficient recomposition
- **Lazy Loading**: LazyColumn for large lists
- **State Management**: Minimized state updates
- **Background Processing**: Off-main-thread operations

---

## 9. Current Enhancements & Recommendations

### 9.1 Completed Enhancements ✅

1. **Constants Synchronization**
   - Google Sheets integration for master data
   - Version-based updates
   - Automatic cache refresh

2. **Image Upload Fix**
   - Fixed HTML response issue
   - Proper JSON error handling
   - Enhanced upload workflow

3. **Chikkaballapura Station Fix**
   - Case-insensitive district matching
   - Normalized map keys
   - Fallback mechanisms

4. **Offline-First Architecture**
   - Room database integration
   - Offline login capability
   - Local data caching

### 9.2 Recommended Enhancements 🔧

#### High Priority

1. **ViewModel Refactoring**
   - **Issue**: `EmployeeViewModel` is too large (1,195 lines)
   - **Solution**: Split into feature-based ViewModels
     - `AuthViewModel`: Authentication & session
     - `EmployeeListViewModel`: Employee CRUD
     - `NotificationViewModel`: Notifications
     - `ProfileViewModel`: User profile management
   - **Benefit**: Better maintainability, testability, and reduced complexity

2. **Error Handling Improvements**
   - Centralized error handling
   - User-friendly error messages
   - Retry mechanisms for network failures
   - Error logging and analytics

3. **Performance Monitoring**
   - Firebase Performance Monitoring
   - Crashlytics integration
   - Analytics for user behavior
   - Performance metrics tracking

4. **Search Enhancement**
   - Advanced search filters
   - Search history
   - Recent searches
   - Search suggestions

#### Medium Priority

5. **Export Functionality**
   - Export employee list to CSV/PDF
   - Share employee details
   - Print employee cards

6. **Multi-language Support**
   - Kannada language support
   - English/Kannada toggle
   - Localized content

7. **Biometric Authentication**
   - Fingerprint authentication
   - Face unlock (where supported)
   - Enhanced security

8. **Dark Mode Polish**
   - Complete dark theme implementation
   - Theme customization options
   - Automatic theme switching

9. **Offline Queue Management**
   - Queue failed operations
   - Automatic retry on connectivity
   - Sync status indicators

10. **Bulk Operations**
    - Bulk employee updates
    - Bulk notifications
    - Bulk data import/export

#### Low Priority

11. **Advanced Statistics**
    - Charts and graphs
    - Exportable reports
    - Custom date range filters

12. **Calendar Integration**
    - Important dates/events
    - Holiday calendar
    - Duty roster

13. **Voice Search**
    - Voice-based employee search
    - Accessibility improvement

14. **Widget Support**
    - Home screen widget
    - Quick search widget
    - Employee contact widget

15. **Backup & Restore**
    - Local backup functionality
    - Cloud backup option
    - Data recovery

### 9.3 Technical Debt

1. **Code Organization**
   - Split large ViewModels
   - Consolidate duplicate code
   - Improve naming conventions

2. **Testing**
   - Unit tests for ViewModels
   - Integration tests for Repositories
   - UI tests for critical flows

3. **Documentation**
   - Code documentation
   - API documentation
   - User guides

4. **Dependency Updates**
   - Regular dependency updates
   - Security patch management
   - Deprecation handling

---

## 10. Deployment & Distribution

### 10.1 Build Configuration
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Java Version**: 17

### 10.2 Release Process
1. **Development**: Feature development in dev branch
2. **Testing**: QA testing and bug fixes
3. **Staging**: Staging environment validation
4. **Production**: Release to Play Store/internal distribution

### 10.3 Distribution Channels
- **Google Play Store**: Public/internal distribution
- **APK Distribution**: Direct APK distribution via Useful Links
- **Firebase App Distribution**: Beta testing distribution

---

## 11. Future Roadmap

### Phase 1: Stability & Performance (Q1 2024)
- ViewModel refactoring
- Error handling improvements
- Performance monitoring
- Bug fixes and optimizations

### Phase 2: Feature Enhancements (Q2 2024)
- Advanced search
- Export functionality
- Multi-language support
- Biometric authentication

### Phase 3: Advanced Features (Q3-Q4 2024)
- Analytics dashboard
- Advanced reporting
- Calendar integration
- Voice search

### Phase 4: Scale & Expand (2025)
- Multi-department support
- Web admin portal
- API for third-party integration
- Mobile app for iOS

---

## 12. Conclusion

The **Police Mobile Directory** application is a robust, feature-rich solution for managing police personnel information with a strong focus on offline functionality, security, and user experience. The application successfully integrates multiple backend services (Firebase, Google Sheets, Apps Script) while maintaining offline-first architecture.

### Key Strengths
✅ **Offline-First Architecture**: Full functionality without internet  
✅ **Modern Tech Stack**: Latest Android technologies  
✅ **Scalable Design**: Modular architecture  
✅ **Comprehensive Features**: Employee management, notifications, documents  
✅ **Security**: Role-based access, encrypted storage  

### Areas for Improvement
🔧 **Code Organization**: Refactor large ViewModels  
🔧 **Testing**: Increase test coverage  
🔧 **Performance Monitoring**: Implement analytics  
🔧 **Documentation**: Enhance code documentation  

The application is production-ready and continues to evolve with regular updates and enhancements based on user feedback and requirements.

---

## Appendix

### A. File Structure
```
app/src/main/java/com/example/policemobiledirectory/
├── api/                    # API service interfaces
├── data/
│   ├── local/             # Room entities & DAOs
│   ├── mapper/            # Data mappers
│   └── remote/            # Remote data models
├── di/                     # Dependency injection modules
├── model/                  # Domain models
├── navigation/             # Navigation routing
├── repository/             # Repository implementations
├── ui/
│   ├── screens/           # Compose screens
│   └── theme/             # Theme & components
├── utils/                  # Utility classes
├── viewmodel/              # ViewModels
├── services/               # Background services
└── helper/                 # Helper classes
```

### B. Key APIs & Endpoints

#### Google Apps Script Endpoints
- `GET /exec?action=getEmployees` - Fetch employees
- `POST /exec?action=uploadImage` - Upload profile image
- `POST /exec?action=addEmployee` - Add employee
- `POST /exec?action=updateEmployee` - Update employee

#### Firebase Functions
- `sendNotification` - Send push notifications
- `notifyAdminOfNewRegistration` - Admin notification trigger

### C. Database Collections

#### Firestore Collections
- `employees` - Employee records
- `pending_registrations` - Registration queue
- `notifications_queue` - Notification queue
- `documents` - Document metadata
- `gallery` - Gallery images
- `useful_links` - Useful links
- `meta` - Metadata and counters

---

**Report Generated**: 2024  
**Version**: 1.0  
**Author**: Development Team  
**Status**: Active Development
