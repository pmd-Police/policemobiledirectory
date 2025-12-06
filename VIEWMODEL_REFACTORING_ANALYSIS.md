# EmployeeViewModel Refactoring Analysis

## Current State
- **File:** `app/src/main/java/com/example/policemobiledirectory/viewmodel/EmployeeViewModel.kt`
- **Lines of Code:** 1,195 lines
- **Status:** ⚠️ **TOO LARGE - Needs Refactoring**

## Problems Identified

### 1. **God Object Anti-Pattern**
The ViewModel is handling too many responsibilities:
- Authentication (Login, Logout, Google Sign-In, OTP/PIN)
- Employee CRUD operations
- Notifications (Admin & User)
- Pending Registrations
- Useful Links
- File Uploads
- UI State (Theme, Font Scale, Search)
- Session Management

### 2. **Maintainability Issues**
- Hard to navigate and understand
- Difficult to test individual features
- High risk of merge conflicts
- Violates Single Responsibility Principle

### 3. **Performance Concerns**
- Large initialization block with multiple coroutines
- Many StateFlows that may not all be needed simultaneously
- Potential memory overhead

## Recommended Refactoring Strategy

### Option 1: Split into Feature-Based ViewModels (Recommended)

#### **AuthViewModel**
- `loginWithPin()`
- `handleGoogleSignIn()`
- `logout()`
- `sendOtp()`
- `verifyOtp()`
- `updatePinAfterOtp()`
- `changePin()`
- `loadSession()`
- `ensureSignedInIfNeeded()`
- State: `currentUser`, `isLoggedIn`, `isAdmin`, `authStatus`, `googleSignInUiEvent`, `otpUiState`, `verifyOtpUiState`, `pinResetUiState`, `pinChangeState`, `remainingTime`

#### **EmployeeListViewModel**
- `refreshEmployees()`
- `addOrUpdateEmployee()`
- `deleteEmployee()`
- `updateSearchQuery()`
- `updateSearchFilter()`
- `updateSelectedDistrict()`
- `updateSelectedStation()`
- `Employee.matches()`
- State: `employees`, `employeeStatus`, `filteredEmployees`, `searchQuery`, `searchFilter`, `selectedDistrict`, `selectedStation`

#### **NotificationViewModel**
- `updateAdminNotificationListener()`
- `updateUserNotificationListener()`
- `markNotificationsRead()`
- `sendNotification()`
- `shouldDeliverNotification()`
- `Map<String, Any>.toAppNotification()`
- State: `adminNotifications`, `userNotifications`, `adminNotificationsLastSeen`, `userNotificationsLastSeen`

#### **PendingRegistrationViewModel**
- `refreshPendingRegistrations()`
- `approveRegistration()`
- `rejectRegistration()`
- `registerNewUser()`
- `updatePendingRegistration()`
- `resetPendingStatus()`
- State: `pendingRegistrations`, `pendingStatus`

#### **UsefulLinksViewModel**
- `fetchUsefulLinks()`
- State: `usefulLinks`

#### **SettingsViewModel** (or keep in MainActivity)
- `toggleTheme()`
- `adjustFontScale()`
- `setFontScale()`
- State: `isDarkTheme`, `fontScale`

#### **ImageUploadViewModel**
- `uploadPhoto()`
- `uploadGalleryImage()`
- State: `uploadStatus`

### Option 2: Use Composition Pattern

Keep `EmployeeViewModel` as a facade that composes smaller ViewModels:
```kotlin
@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val authViewModel: AuthViewModel,
    private val employeeListViewModel: EmployeeListViewModel,
    private val notificationViewModel: NotificationViewModel,
    // ... etc
) : ViewModel() {
    // Delegate to composed ViewModels
    val currentUser = authViewModel.currentUser
    val employees = employeeListViewModel.employees
    // ...
}
```

### Option 3: Extract Use Cases/Interactors

Create use case classes to move business logic out of ViewModel:
- `LoginUseCase`
- `SendNotificationUseCase`
- `ApproveRegistrationUseCase`
- etc.

## Implementation Priority

### Phase 1: High Priority (Immediate)
1. **Extract AuthViewModel** - Authentication is critical and self-contained
2. **Extract NotificationViewModel** - Complex real-time listeners

### Phase 2: Medium Priority
3. **Extract EmployeeListViewModel** - Core feature, frequently used
4. **Extract PendingRegistrationViewModel** - Admin-only feature

### Phase 3: Low Priority
5. **Extract UsefulLinksViewModel** - Simple feature
6. **Extract SettingsViewModel** - Simple UI state

## Benefits of Refactoring

1. **Better Testability** - Each ViewModel can be tested independently
2. **Improved Maintainability** - Smaller, focused files
3. **Reduced Merge Conflicts** - Multiple developers can work on different ViewModels
4. **Better Performance** - Only load what's needed
5. **Clearer Architecture** - Follows MVVM best practices
6. **Easier Onboarding** - New developers can understand smaller pieces

## Migration Strategy

1. Create new ViewModels alongside existing one
2. Gradually migrate screens to use new ViewModels
3. Update dependency injection
4. Remove old code once all screens migrated
5. Test thoroughly at each step

## Estimated Effort

- **Phase 1:** 4-6 hours
- **Phase 2:** 3-4 hours  
- **Phase 3:** 2-3 hours
- **Total:** ~10-13 hours

## Notes

- The current ViewModel works, but refactoring will improve long-term maintainability
- Can be done incrementally without breaking existing functionality
- Consider doing this before adding new features to avoid making it worse

