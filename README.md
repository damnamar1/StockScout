# StockScout

A senior-level Android take-home assignment demonstrating Clean Architecture, MVVM, Room, Retrofit, ML Kit barcode scanning, and offline-first sync via WorkManager.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                     │
│  SplashFragment → HomeFragment → ScannerFragment            │
│                              ↘ ItemDetailFragment           │
│  ViewModels (Hilt) observe LiveData<Resource<T>>            │
└──────────────────────┬──────────────────────────────────────┘
                       │ UseCases
┌──────────────────────▼──────────────────────────────────────┐
│                       Domain Layer                          │
│  GetItemsUseCase / ResolveAliasUseCase                      │
│  PickItemUseCase / SyncPendingPicksUseCase                  │
│  AliasResolver → BarcodeResolver → Gs1Parser               │
└──────────────────────┬──────────────────────────────────────┘
                       │ Repository interface
┌──────────────────────▼──────────────────────────────────────┐
│                        Data Layer                           │
│  ItemRepositoryImpl                                         │
│    ├── Remote: Retrofit ApiService (GET /items, POST /picks)│
│    └── Local:  Room (ItemDao, AliasDao, PendingPickDao)     │
└─────────────────────────────────────────────────────────────┘
```

**Room is the single source of truth.** The UI always reads from Room; the remote API is only called to populate Room.

---

## Tech Stack & Rationale

| Library | Why |
|---|---|
| **Hilt** | Compile-time DI, reduces boilerplate vs Dagger2, first-party Android support |
| **Room + KSP** | Type-safe local DB; KSP is faster than KAPT; supports coroutines natively |
| **Retrofit + OkHttp** | Industry standard; supports suspend functions via Kotlin adapter |
| **Navigation Component** | Single-Activity pattern; SafeArgs gives type-safe fragment arguments |
| **WorkManager** | Guaranteed background execution with retry and constraint support |
| **ML Kit Barcode Scanning** | On-device, no native code deps, supports UPC-A/EAN-13/GS1 out of the box, works offline |
| **CameraX** | Lifecycle-aware camera API, consistent across devices |
| **LiveData** | Lifecycle-safe UI observation; avoids leaks vs raw StateFlow observers in XML-based UI |

### Why ML Kit over ZXing?
- Zero native `.so` dependencies shipped with app
- On-device models — works fully offline once installed
- Officially maintained by Google, consistent with modern Android guidance
- First-class support for GS1 DataMatrix/QR in addition to 1D codes
- Simpler API (no `IntentIntegrator` ceremony)

---

## Offline Sync Flow

```
User taps PICK
    │
    ├─ 1. updateItemQuantity(code, qty-1)   ← immediate local write
    ├─ 2. insertPendingPick(PENDING)        ← persisted to Room
    └─ 3. enqueue OneTimeWorkRequest        ← runs when CONNECTED
              │
              └─ SyncWorker.doWork()
                    │
                    ├─ Fetch PENDING + FAILED picks from Room
                    ├─ POST /picks for each
                    ├─ Success → mark SYNCED
                    └─ Failure → increment retryCount
                                  retryCount > 5 → FAILED
                                  else → Result.retry() (exponential backoff)

Periodic safety net: PeriodicWorkRequest every 15 min with CONNECTED constraint.
NetworkMonitor: ConnectivityManager.NetworkCallback triggers sync when connection returns.
```

---

## GS1 Parsing

GS1 strings encode multiple data fields using Application Identifiers (AIs):

```
01  00123456789052  17  250630  10  LOT-A  21  SN-001
└── AI: GTIN-14    └── AI:Expiry └── AI:Lot  └── AI:Serial
```

Variable-length AIs are terminated by FNC1 (ASCII 29 / `\x1D`). `Gs1Parser` handles:
- Fixed AIs (e.g., `01` → 14 chars, `17` → 6 chars)
- Variable AIs (e.g., `10`, `21`) terminated by FNC1 or end-of-string
- The `]C1` prefix some scanners emit

`BarcodeResolver` then extracts the GTIN-14 and takes the last 13 digits as EAN-13 for alias lookup.

---

## Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11+
- Device/emulator API 24+

### Steps
1. Clone the repo
2. Configure mock API (see below)
3. Open in Android Studio → sync Gradle → Run

### Mock API Setup (mockapi.io)

1. Go to https://mockapi.io and create a free project
2. Create resource `items` with the schema below
3. Create resource `picks` (POST endpoint)
4. Copy your project base URL (e.g. `https://65f1234abc.mockapi.io/api/v1/`)
5. Replace `Constants.BASE_URL` in `utils/Constants.kt`

**Sample item schema for mockapi.io:**
```json
{
  "itemCode": "string",
  "name": "string",
  "unitOfMeasure": "string",
  "onHandQuantity": "number",
  "aliases": "array"
}
```

### Static fallback JSON (host on GitHub Gist or raw GitHub)

```json
[
  {
    "itemCode": "WGT-A",
    "name": "Widget Alpha",
    "unitOfMeasure": "EA",
    "onHandQuantity": 142,
    "aliases": [
      { "type": "UPC_A",  "value": "012345678901" },
      { "type": "EAN_13", "value": "0123456789012" },
      { "type": "TEXT",   "value": "SUPPLIER-SKU-ALPHA" }
    ]
  },
  {
    "itemCode": "BLT-B",
    "name": "Bolt Bravo",
    "unitOfMeasure": "BX",
    "onHandQuantity": 55,
    "aliases": [
      { "type": "EAN_13", "value": "5012345678900" },
      { "type": "GS1",    "value": "010050123456789017250630" }
    ]
  },
  {
    "itemCode": "NUT-C",
    "name": "Nut Charlie",
    "unitOfMeasure": "KG",
    "onHandQuantity": 200,
    "aliases": [
      { "type": "UPC_A",  "value": "098765432109" },
      { "type": "TEXT",   "value": "NUT-C-SUPPLIER" }
    ]
  },
  {
    "itemCode": "PLT-D",
    "name": "Plate Delta",
    "unitOfMeasure": "SHT",
    "onHandQuantity": 30,
    "aliases": [
      { "type": "EAN_13", "value": "4000123456789" }
    ]
  },
  {
    "itemCode": "CBL-E",
    "name": "Cable Echo",
    "unitOfMeasure": "M",
    "onHandQuantity": 500,
    "aliases": [
      { "type": "UPC_A",  "value": "745114000001" },
      { "type": "GS1",    "value": "0107451140000018172606301021BATCH02" },
      { "type": "TEXT",   "value": "CBL-ECHO-100M" }
    ]
  },
  {
    "itemCode": "GKT-F",
    "name": "Gasket Foxtrot",
    "unitOfMeasure": "EA",
    "onHandQuantity": 88,
    "aliases": [
      { "type": "EAN_13", "value": "8712345678901" },
      { "type": "TEXT",   "value": "GKT-F-EU" }
    ]
  }
]
```

---

## Assumptions & Trade-offs

- **Room is source of truth**: on-hand quantities updated locally on pick; remote sync is fire-and-forget
- **No authentication**: mock API is open; real implementation would add bearer token interceptor
- **GS1 subset**: supports most common AIs; edge cases like nested FNC1 in fixed-length AIs are not handled
- **Single activity**: Navigation Component handles all screen transitions
- **Quantity floor = 0**: pick on 0-quantity item records the pick but does not go negative
- **Periodic sync**: 15-minute minimum enforced by WorkManager (OS constraint)

---

## Bonus Features Included

- **Search debounce (300ms)**: `Flow.debounce` in `HomeViewModel` prevents query-per-keystroke
- **NetworkMonitor**: `ConnectivityManager.NetworkCallback` emits connectivity changes for auto-sync triggering
- **Dark mode**: `DayNight` theme with `values-night/colors.xml` overrides
- **Offline-first caching**: Room as single source of truth; UI always reads from DB

---

## Running Tests

```bash
./gradlew :app:test
```

Test coverage:
- `AliasResolverTest`: 7 cases covering all resolution paths
- `Gs1ParserTest`: 8 cases covering GTIN, lot, expiry, serial, FNC1
- `PickItemUseCaseTest`: 4 cases covering quantity decrement and pick creation
- `OfflineSyncTest`: 7 cases covering sync, retry, failure, and queue survival
