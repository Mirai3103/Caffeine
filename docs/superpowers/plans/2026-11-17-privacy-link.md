# Privacy Policy Link Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm một nút "Chính sách bảo mật" trên màn hình chính, mở policy trong trình duyệt.

**Architecture:** Một object `Privacy` giữ URL và hàm mở policy (Intent `ACTION_VIEW`, fallback dialog). `CaffeineContent` nhận callback `onPrivacy` và vẽ thêm `TextButton` ở cuối cột điều khiển.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, minSdk 24, targetSdk 37.

**Spec:** `docs/superpowers/specs/2026-11-17-privacy-link-design.md`

## Global Constraints

- Không thêm quyền `INTERNET`, không thêm dependency.
- Mọi chuỗi mới có ở `values/strings.xml` và `values-vi/strings.xml`.
- Vùng chạm ≥ 48dp (`heightIn(min = 48.dp)`).
- URL chính xác: `https://share.web.huuhoang.tech/privacy/caffeine.html`

## Review Focus

- Máy không có trình duyệt (hoặc mọi activity `ACTION_VIEW` bị vô hiệu): phải hiện dialog chứa URL, không crash. — Kiểm bằng tay trên emulator không có browser (xem Task 2, Step 6).
- Người dùng bấm nhiều lần liên tiếp: dialog không được chồng lên nhau. — `openPrivacyPolicy` chỉ dựng dialog khi không mở được Intent; không có state nào giữ dialog nên không chồng.
- Xoay ngang / font scale 2.0: nút vẫn hiển thị và bấm được. — Preview `PreviewLandscape`, `PreviewFontScale`.

---

### Task 1: `Privacy.kt` + strings

**Files:**
- Create: `app/src/main/java/com/laffy/caffeine/Privacy.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-vi/strings.xml`

**Interfaces:**
- Produces: `const val PRIVACY_POLICY_URL: String`, `fun openPrivacyPolicy(context: Context)` trong `object Privacy`.

- [ ] **Step 1: Thêm strings**

`values/strings.xml`: `<string name="privacy">Privacy policy</string>` và
`<string name="privacy_unavailable">Couldn\'t open a browser. Copy the link:</string>`.
`values-vi/strings.xml`: `<string name="privacy">Chính sách bảo mật</string>` và
`<string name="privacy_unavailable">Không mở được trình duyệt. Sao chép liên kết:</string>`.

- [ ] **Step 2: Viết `Privacy.kt`**

```kotlin
package com.laffy.caffeine

object Privacy {
    const val PRIVACY_POLICY_URL = "https://share.web.huuhoang.tech/privacy/caffeine.html"
    fun openPrivacyPolicy(context: Context)
}
```
Body của `openPrivacyPolicy`:
1. `Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)`
   (context có thể là application context), `context.startActivity(...)`.
2. `catch (e: ActivityNotFoundException)` → dựng `android.app.AlertDialog.Builder(context)` (không có
   appcompat trong app) với title `R.string.privacy`, message là một `TextView` đã
   `Linkify.addLinks(text, Linkify.WEB_URLS)` chứa `R.string.privacy_unavailable` + URL, nút OK
   `android.R.string.ok`. Dialog cần `window` type dưới application context — dùng
   `context` truyền vào từ `LocalContext` (Activity) trong Task 2, nên không cần thêm flag.

- [ ] **Step 3: Biên dịch**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/laffy/caffeine/Privacy.kt app/src/main/res/values/strings.xml app/src/main/res/values-vi/strings.xml
git commit -m "feat: add privacy policy URL and opener"
```

### Task 2: Nút trong màn hình chính

**Files:**
- Modify: `app/src/main/java/com/laffy/caffeine/ui/CaffeineScreen.kt`

**Interfaces:**
- Consumes: `Privacy.openPrivacyPolicy(context)`, `R.string.privacy`.
- Produces: `CaffeineContent(..., onPrivacy: () -> Unit)` — tham số đặt sau `onAddTile`.

- [ ] **Step 1: Thêm tham số và nút**

`CaffeineScreen` truyền `onPrivacy = { Privacy.openPrivacyPolicy(context) }`.
`CaffeineContent` nhận `onPrivacy: () -> Unit`; `ControlsSection` nhận và vẽ, ngay sau khối
`if (showAddTile)`: `TextButton(onClick = onPrivacy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.privacy)) }`.

- [ ] **Step 2: Cập nhật preview**

`PreviewScreen` truyền `onPrivacy = {}` (6 preview dùng chung hàm này, chỉ sửa một chỗ).

- [ ] **Step 3: Build + unit test**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, toàn bộ test pass.

- [ ] **Step 4: Kiểm tra tay**

Cài debug APK, mở app → cuộn xuống cuối → bấm "Chính sách bảo mật": trình duyệt mở đúng URL.
Xoay ngang: nút nằm cuối cột phải. font scale 2.0: nhãn không bị cắt.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/laffy/caffeine/ui/CaffeineScreen.kt
git commit -m "feat: add privacy policy link to main screen"
```
