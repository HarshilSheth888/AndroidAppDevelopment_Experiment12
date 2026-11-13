# HTTP Web Services

**Experiment 12: Consuming Web Services Using HTTP in Android**

**Student Name:** Harshil Sheth
**USN:** 25MCAR0225

---

## Table of Contents
1. [Experiment Overview](#experiment-overview)
2. [Concept and Technology](#concept-and-technology)
3. [Scenario](#scenario)
4. [Features](#features)
5. [Project Structure](#project-structure)
6. [Getting Started](#getting-started)
7. [Output](#output)
8. [Test Cases](#test-cases)
9. [Known Limitations](#known-limitations)
10. [Learning Outcomes](#learning-outcomes)

---

## Experiment Overview

This experiment shows how an Android app talks to a web service over HTTP. The app has two tabs. **Live REST Feed** loads posts from the public JSONPlaceholder API and lets the user create, edit, and delete posts with HTTP POST, PUT, and DELETE requests. **HTTP Playground** is a request builder where the user picks a method, URL, headers, and JSON body, sends the request, and inspects the status code, response time, size, headers, body, and an equivalent `curl` command. The user can switch between two HTTP clients, OkHttp and `HttpURLConnection`.

## Concept and Technology

- **HTTP and REST:** a client sends a request with a method (GET, POST, PUT, PATCH, DELETE, HEAD), headers, and an optional body, and the server answers with a status code, headers, and a body. REST maps these methods to read, create, update, and delete operations on resources such as `/posts`.
- **OkHttp 4** is a widely used HTTP client library. `HttpServiceEngine` builds a `Request`, sets timeouts, and runs it.
- **HttpURLConnection** is the HTTP client built into Android. The same engine can send the request this way, so the two approaches can be compared.
- **Gson** converts JSON to Kotlin objects (`PostItem`) and formats JSON for display.
- **Kotlin coroutines** run network calls on `Dispatchers.IO` so the UI thread is never blocked.
- **MVVM with `StateFlow`:** `MainViewModel` exposes the feed and request state (Loading, Success, Error), and the fragments render it. A repository (`WebServiceRepository`) separates network code from the UI.
- **Fragments and ViewPager2 with `TabLayout`** host the two tabs.
- **`ConnectivityManager`** with a network callback shows an Online or Offline indicator.
- **RecyclerView and SwipeRefreshLayout** display the posts with pull-to-refresh.

## Scenario

When the app opens, it fetches the list of posts from `https://jsonplaceholder.typicode.com/posts` and shows them with the status code, latency, and payload size. The user can:

- Search the posts by title, body, or ID
- Tap **POST Request** to create a post, or use the edit and delete buttons on a post for PUT and DELETE requests
- Inspect the JSON of any post
- Switch the HTTP engine between OkHttp and HttpURLConnection from the top bar
- Open the **HTTP Playground**, choose a preset or build a request, tap **Send HTTP Request**, and read the response in the Formatted JSON, Raw Body, Headers, or cURL view
- Open the guide from the top bar to read a summary of HTTP concepts

## Features

- GET, POST, PUT, PATCH, DELETE, and HEAD support
- Two HTTP engines that can be switched at runtime: OkHttp and HttpURLConnection
- Live feed with search, pull-to-refresh, loading, error, and empty states
- Stats card showing total items, status code, latency, and payload size
- Create, edit, and delete posts with confirmation messages
- Request builder with editable URL, headers, and JSON body, plus a Format JSON button
- Seven presets, including JSONPlaceholder requests and HTTPBin 200, 404, and 2-second-delay endpoints
- Response viewer with status color coding (2xx, 4xx, 5xx), headers, raw and formatted body, and a generated `curl` command
- Online and Offline network indicator
- Unit tests for the `curl` builder, JSON formatting, and status classification

## Project Structure

```
androidappdevelopment_experiment12/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/webservicedemo/
│   │   │   │   ├── MainActivity.kt                      # Tabs, engine toggle, network monitor
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── HttpMethod.kt                # GET, POST, PUT, PATCH, DELETE, HEAD
│   │   │   │   │   │   ├── HttpEngineType.kt            # OkHttp or HttpURLConnection
│   │   │   │   │   │   ├── HttpRequestConfig.kt         # Method, URL, headers, body, engine
│   │   │   │   │   │   ├── HttpResponseResult.kt        # Status, time, size, headers, body
│   │   │   │   │   │   └── PostItem.kt                  # Post data model
│   │   │   │   │   ├── network/HttpServiceEngine.kt     # Sends requests with either engine
│   │   │   │   │   └── repository/WebServiceRepository.kt  # Posts API calls
│   │   │   │   └── ui/
│   │   │   │       ├── viewmodel/MainViewModel.kt       # Feed and request state
│   │   │   │       ├── fragment/
│   │   │   │       │   ├── RestFeedFragment.kt          # Live REST Feed tab
│   │   │   │       │   └── HttpPlaygroundFragment.kt    # HTTP Playground tab
│   │   │   │       └── adapter/PostAdapter.kt           # RecyclerView adapter for posts
│   │   │   ├── res/
│   │   │   │   ├── layout/                              # Main, feed, playground, item, and dialog layouts
│   │   │   │   ├── drawable/                            # Icons
│   │   │   │   └── values/                              # strings.xml, colors.xml, themes.xml
│   │   │   └── AndroidManifest.xml                      # INTERNET permission
│   │   ├── test/java/com/example/webservicedemo/
│   │   │   └── HttpWebServiceTest.kt                    # Unit tests
│   │   └── androidTest/                                 # Instrumented test template
│   └── build.gradle.kts
├── gradle/libs.versions.toml                            # Dependency versions
├── build.gradle.kts
├── settings.gradle.kts
└── screenshots/                                         # Output and test case images
```

## Getting Started

**Requirements:** Android Studio (latest stable), Android SDK, an emulator or device running Android 7.0 (API 24) or later, and an internet connection.

1. Clone the repository:
   ```bash
   git clone https://github.com/HarshilSheth888/AndroidAppDevelopment_Experiment12.git
   ```
2. Open the project folder in Android Studio.
3. Let Gradle sync.
4. Press **Run** on an emulator or connected device.
5. To run the unit tests, use `./gradlew test`.

## Output

![Output screenshot](screenshots/output.png)

The app opens on the Live REST Feed tab, showing the engine and Online status in the header, the stats card for `GET /posts`, the search bar, and the list of posts.

## Test Cases

### Test Case 1: GET request loads the feed
- **Input:** Open the app with an internet connection.
- **Expected:** The feed loads 100 posts from JSONPlaceholder. The stats card shows `200 OK`, the latency in milliseconds, and the payload size.
- **Result:** ____________
- **Screenshot:** ![Test case 1](screenshots/test1.png)

### Test Case 2: POST request creates a post
- **Input:** Tap **POST Request**, enter a title and body, and tap **Send Request**.
- **Expected:** A message "HTTP POST Succeeded! Created item #…" appears, and the new post is added at the top of the list.
- **Result:** ____________
- **Screenshot:** ![Test case 2](screenshots/test2.png)

### Test Case 3: Playground request with an error status
- **Input:** Open **HTTP Playground**, tap the **HTTPBin 404 Error** preset, and tap **Send HTTP Request**.
- **Expected:** The response shows status 404 in the error color, along with the response time, size, headers, and the generated `curl` command.
- **Result:** ____________
- **Screenshot:** ![Test case 3](screenshots/test3.png)

## Known Limitations

- **JSONPlaceholder is a test API that does not save changes.** A POST returns a fake new ID, and PUT and DELETE succeed without changing the server's data. The app updates only its local list, so refreshing the feed brings back the original 100 posts.
- **PATCH with the HttpURLConnection engine is expected to fail,** because Android's `HttpURLConnection` does not support the PATCH method. Use OkHttp for PATCH requests.
- **Cleartext HTTP traffic is allowed** in the manifest (`usesCleartextTraffic="true"`) so that the playground can call `http://` URLs.
- **The app needs a working internet connection.** If it is offline, the feed shows an error and a retry button.

## Learning Outcomes

- Understanding HTTP methods, headers, status codes, and request bodies
- Calling a REST web service with OkHttp and with `HttpURLConnection`
- Parsing and formatting JSON with Gson
- Running network calls off the main thread with coroutines
- Structuring an app with a repository, ViewModel, and `StateFlow`
- Showing loading, success, error, and empty states in the UI
- Detecting network availability with `ConnectivityManager`
- Writing unit tests for network helper code

## Author

**Harshil Sheth**
USN: 25MCAR0225
GitHub: https://github.com/HarshilSheth888
