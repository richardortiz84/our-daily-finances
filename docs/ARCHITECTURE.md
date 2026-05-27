# Architecture

Our Daily Planner follows Clean Architecture with three layers — **data/domain**, **domain**, and **presentation** — sharing code across Android, iOS, and Desktop via Kotlin Multiplatform.

```
presentation/       UI — Composables, ViewModels, screen state, navigation
domain/             Business logic — use cases, repository interfaces, entities
data/               Data models, repository implementations (Firestore, DataStore)
```

---

## Navigation

Navigation uses Jetpack Compose Navigation with type-safe `@Serializable` routes organized in a three-level hierarchy.

### Route definitions

```
NavRoute.kt       Feature routes within the planner workspace
RootNav.kt        Top-level flow routes (Auth, Planners, Planner workspace)
```

**RootNav** — top-level flows:

| Route | Destination |
|---|---|
| `RootNav.Auth` | Login / registration flow |
| `RootNav.Planners` | Planner selection screen |
| `RootNav.Planner` | Main workspace (nested NavHost) |

**NavRoute** — workspace feature routes:

| Route | Description |
|---|---|
| `Notes` | Notes list |
| `ViewNote(noteId)` | Read-only note view |
| `EditNote(noteId)` | Edit existing note |
| `CreateNote` | New note |
| `Tasks` | Tasks list |
| `Journals` | Journals list |
| *(and more per feature)* | |

### NavHost nesting

```
RootNavigation (NavHost)
└── AuthNavigation          ← login/register screens
└── PlannersNavigation      ← planner picker
└── PlannerNavigation       ← nested NavHost, main workspace
    ├── Notes
    ├── Tasks
    └── Journals
```

`PlannerNavigation` passes a `NavigationType` enum to adapt the chrome to screen size:

| NavigationType | Trigger |
|---|---|
| `MODAL_NAVIGATION_DRAWER` | < 600 dp width |
| `NAVIGATION_RAIL` | 600–839 dp width |
| `PERMANENT_NAVIGATION_DRAWER` | ≥ 840 dp width |

`NavItem` data class links each section to an icon, label string, and route for use in drawers and rails.

---

## ViewModel State

All ViewModels use `MutableStateFlow` exposed as `StateFlow`, collected by Composables via `collectAsState()`.

### Pattern

```kotlin
// In ViewModel
private val _state = MutableStateFlow<NotesScreenState>(NotesScreenState.Loading)
val state = _state.asStateFlow()

// Composing multiple sources
viewModelScope.launch {
    combine(
        notesUseCase.GetNotes(activePlanner),
        preferencesUseCase.GetNotesViewListType(),
        preferencesUseCase.GetNotesViewListSortType(),
        tagsUseCase.GetAllTags(activePlanner),
    ) { notes, viewType, sortType, tags ->
        NotesScreenState.Notes(notes, viewType, sortType, tags)
    }.collect { _state.update { it } }
}
```

### Screen state sealed classes

Each feature uses a sealed class to represent mutually exclusive UI states:

```kotlin
sealed class NotesScreenState {
    data object Loading : NotesScreenState()
    data class Error(val message: String) : NotesScreenState()
    data class Notes(
        val notes: List<Note>,
        val viewType: NotesViewListType,
        val sortType: NotesViewListSortType,
        val tags: List<Tag>,
    ) : NotesScreenState()
}
```

This forces exhaustive `when` expressions in the UI and keeps display logic out of ViewModels.

### ViewModels

| ViewModel | Responsibility |
|---|---|
| `MainViewModel` | Auth state, active planner, app-level font size |
| `NotesViewModel` | Notes list with filtering, sorting, tag management |
| `NoteViewModel` | Single note display |
| `EditNoteViewModel` | Note editing with rich text state |
| `CreateNoteViewModel` | New note creation |
| `TasksViewModel` | Task and task-list aggregation, filtering |
| `JournalsViewModel` | Journal list with sorting |
| `LoginViewModel` | Login/registration flows, Google Sign-In |
| `PlannersViewModel` | Planner CRUD, invite handling |

---

## Dependency Injection

The app uses **Koin** (v4.0.4). All modules are declared in `commonMain` except `ViewModelModule`, which has platform-specific `actual` implementations.

### Module graph

```
initKoin()
├── activePlannerModule()         factory — resolves Planner? from PlannersRepository
├── currentNoteFontSizeModule()   factory — resolves Float from PreferencesRepository
├── platformModule()              platform-specific (DataStore path, HtmlFetcher, etc.)
├── repositoryModule()            singletons — all repository implementations
├── useCaseModule()               singletons — all use case classes
├── viewModelModule()             platform-specific — ViewModels via viewModelOf()
└── userModule()                  singleton — current User
```

### Repository module pattern

```kotlin
// RepositoryModule.kt
fun repositoryModule() = module {
    single { NotesRepositoryImpl(get(), get()) }.bind<NotesRepository>()
    single { TasksRepositoryImpl(get(), get()) }.bind<TasksRepository>()
    // ...
}
```

Implementations are bound to interfaces so use cases depend only on abstractions.

### ViewModel module (platform-specific)

```kotlin
// ViewModelModule.android.kt
actual fun viewModelModule() = module {
    viewModelOf(::NotesViewModel)
    viewModelOf(::TasksViewModel)
    // ...
}
```

On Android, `viewModelOf` integrates with `ViewModel` lifecycle. Desktop and iOS use equivalent Koin bindings.

---

## Repositories

Repositories abstract all data sources behind `Flow`-returning interfaces. Implementations live in `domain/` alongside the interfaces.

### Response wrapper

Every repository method wraps its result in a sealed `Response<T>`:

```kotlin
sealed class Response<out T> {
    data class Loading<out T>(val data: T) : Response<T>()
    data class Success<out T>(val data: T) : Response<T>()
    data class Error<out T>(
        val data: T? = null,
        val message: String? = null,
    ) : Response<T>()
}
```

### Repository interfaces

| Repository | Data source | Key operations |
|---|---|---|
| `NotesRepository` | Firestore | `getNotes(planner): Flow<Response<List<Note>>>`, `saveNote`, `deleteNote` |
| `TasksRepository` | Firestore | `getTasks(planner): Flow<Response<List<Task>>>`, `saveTask`, `deleteTask` |
| `TaskListsRepository` | Firestore | `getTaskLists(planner): Flow<Response<List<TaskList>>>`, `saveTaskList`, `deleteTaskList` |
| `JournalsRepository` | Firestore | `getJournals(planner): Flow<Response<List<Journal>>>`, `saveJournal`, `deleteJournal` |
| `PlannersRepository` | Firestore + DataStore | `getPlanners(): Flow<Response<List<Planner>>>`, `setActivePlanner`, `acceptInvite`, `declineInvite` |
| `UserRepository` | Firebase Auth + Firestore | `getCurrentUser(): Flow<User?>`, `loginUser`, `loginWithGoogle`, `registerUser`, `signOut` |
| `PreferencesRepository` | DataStore | `getNoteFontSize`, `setNoteFontSize`, `getNotesViewListType`, … |
| `TagsRepository` | Firestore | `getAllTags(planner)`, `getRecentTags`, `saveRecentTags` |

### Firestore collection structure

```
planners/{plannerId}/
├── notes/{noteId}
├── tasks/{taskId}
├── taskLists/{taskListId}
├── journals/{journalId}
└── tags/{tagId}
```

Planners are filtered server-side by the requesting user's UID (`access` array field).

### Firestore streaming pattern

```kotlin
override fun getNotes(planner: Planner): Flow<Response<List<Note>>> = callbackFlow {
    val subscription = firestore
        .collection("planners/${planner.plannerId}/notes")
        .snapshots
        .collect { snapshot ->
            trySend(Response.Success(snapshot.documents.map { it.data<Note>() }))
        }
    awaitClose { subscription.cancel() }
}
```

All list reads use real-time Firestore snapshot listeners, so the UI stays in sync without manual refresh.

---

## Use Cases

Use cases implement single operations and are the only callers of repositories. ViewModels depend on use cases, never on repositories directly.

### Pattern

```kotlin
class GetNotes(private val notesRepository: NotesRepository) {
    suspend operator fun invoke(planner: Planner): Flow<Response<List<Note>>> =
        notesRepository.getNotes(planner)
}
```

`operator fun invoke` lets call sites read as `notesUseCase.GetNotes(planner)`.

### Composition via holder classes

Related use cases are grouped into a holder that ViewModels receive as a single dependency:

```kotlin
class NotesUseCase(
    var GetNotes: GetNotes,
    var SaveNote: SaveNote,
    var DeleteNote: DeleteNote,
)
```

Each individual class and the holder are all registered in Koin as `singleOf(::GetNotes)`.

### Use case families

| Holder | Use cases |
|---|---|
| `NotesUseCase` | `GetNotes`, `SaveNote`, `DeleteNote` |
| `TasksUseCase` | `GetTasks`, `SaveTask`, `DeleteTask` |
| `TaskListsUseCase` | `GetTaskLists`, `SaveTaskList`, `DeleteTaskList` |
| `JournalsUseCase` | `GetJournals`, `SaveJournal`, `DeleteJournal` |
| `PlannersUseCase` | `GetPlanners`, `GetActivePlanner`, `SetActivePlanner`, `SendInvite`, `AcceptInvite`, `DeclineInvite` |
| `UsersUseCase` | `GetCurrentUser`, `LoginUser`, `LoginUserWithGoogle`, `RegisterUser`, `SaveUser`, `SignOutUser` |
| `PreferencesUseCase` | `GetNoteFontSize`, `SetNoteFontSize`, `GetNotesViewListType`, `SetNotesViewListType`, … |
| `TagsUseCase` | `GetAllTags`, `GetRecentTags`, `SaveRecentTags` |

---

## Data Flow

A typical read from screen to Firestore:

```
Composable
  └─ collectAsState(viewModel.state)
       └─ ViewModel.combine(useCase.GetNotes(planner), ...)
            └─ GetNotes.invoke(planner)
                 └─ NotesRepository.getNotes(planner)
                      └─ Firestore.collection(...).snapshots (real-time Flow)
```

A typical write:

```
Composable (user action)
  └─ viewModel.saveNote(note)
       └─ viewModelScope.launch { notesUseCase.SaveNote(note, activePlanner) }
            └─ NotesRepository.saveNote(note, planner)
                 └─ Firestore.collection(...).document(id).set(note)
```

---

## Key Libraries

| Concern | Library |
|---|---|
| UI | Jetpack Compose Multiplatform + Material3 |
| Navigation | `androidx.navigation:navigation-compose` (type-safe) |
| DI | Koin 4.0.4 |
| Backend | Firebase via `dev.gitlive:firebase-kotlin-sdk` |
| Local prefs | AndroidX DataStore |
| Networking | Ktor 3.2.2 |
| Images | Coil 3.2.0 + Ktor client |
| Rich text | `richeditor-compose` |
| Drag-and-drop | `reorderable` |
| Serialization | `kotlinx.serialization` |
| Error tracking | Sentry |
