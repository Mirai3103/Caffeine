# Caffeine v2 — Material 3 Expressive + tính năng mới

Ngày: 2026-11-17 · Trạng thái: chờ duyệt

## Mục đích

Caffeine giữ màn hình sáng trong một khoảng thời gian, bật/tắt từ app hoặc tile Quick Settings.
Bản v2 làm app nhanh dùng hơn (đặc biệt từ tile/notification, không cần mở app), an toàn hơn
(không giữ sáng ngoài ý muốn) và chuyển giao diện sang Material 3 Expressive.

## Mục tiêu

- Đổi thời lượng ngay trên tile, gia hạn ngay trên notification.
- Thêm chế độ không giới hạn (∞) có rào chắn pin.
- Tự dừng khi người dùng tắt màn hình / rút sạc (tuỳ chọn).
- Màn hình chính theo Material 3 Expressive, đạt các điểm checklist UI/UX (a11y, xoay ngang, cỡ chữ lớn).

## Không làm (non-goals)

- Không tự bật theo app đang mở (cần Accessibility/Usage Stats — nặng và nhạy cảm về quyền).
- Không tự **bật** khi cắm sạc: từ Android 8 broadcast `ACTION_POWER_CONNECTED` không tới được
  receiver khai báo trong manifest, và Android 12+ cấm start foreground service từ nền. Thay bằng
  tuỳ chọn "Dừng khi rút sạc" (kết hợp ∞ = "sáng suốt lúc sạc").
- Không widget màn hình chính, không nhiều màn hình / navigation.

## Anti-goals (coi là thất bại dù chạy được)

- App tốn pin khi không chạy, hoặc chạy công việc định kỳ nền (giữ nguyên nguyên tắc hiện tại:
  không tick nền, đếm ngược trong notification do hệ thống vẽ).
- Màn hình bị giữ sáng khi người dùng không mong đợi (sau khi bấm nút nguồn, khi pin cạn).
- Animation gây rối hoặc chạy khi người dùng bật "Giảm chuyển động".

## Ràng buộc

- minSdk 24, targetSdk 37, Kotlin 2.2.10, AGP 9.5.1.
- Compose BOM `2026.09.00`; ghim riêng `androidx.compose.material3:material3:1.5.0-rc01`
  (Material 3 Expressive). Khi 1.5.0 stable ra chỉ cần đổi version.
- Chuỗi hiển thị có cả `values/` (en) và `values-vi/`.
- Không thêm quyền mới ngoài các quyền đang có.

## Approaches đã cân nhắc

| | Cách | Đánh đổi |
|---|---|---|
| A | BOM 2026.09.00 với material3 1.4.0 stable + opt-in API Expressive thử nghiệm | Ổn định nhất nhưng component Expressive thiếu/chưa hoàn thiện |
| **B (chọn)** | BOM 2026.09.00 + ghim material3 1.5.0-rc01 | Nhánh Expressive gần hoàn chỉnh nhất; rủi ro RC thấp với app 1 màn hình |
| — | Giữ M3 thường, tự làm motion | Loại vì người dùng muốn Expressive |

Người dùng chọn B.

## Thiết kế

### 1. Mô hình phiên (session)

`CaffeineState` thay `endAt: Long` bằng `session: StateFlow<Session?>`:

```kotlin
data class Session(val startAt: Long, val endAt: Long?)   // endAt == null → ∞
```

`Duration` là một trong: `Minutes(n)` hoặc `Infinite`. `Prefs` lưu:
`selected` (10/15/20/∞/custom), `customMinutes` (mặc định 30, phạm vi 1–120),
`stopOnScreenOff` (mặc định **bật**), `stopOnUnplug` (mặc định tắt), `dimScreen` (mặc định tắt),
`tileAdded` (Boolean).

`CaffeineController` có: `start(duration)`, `stop()`, `extend(minutes = 5)`, `cycle()` (cho tile).
Logic thuần (tính `Session` mới, bước kế tiếp của vòng tile, cộng thời gian) tách ra hàm thuần để unit test.

### 2. Service

- Wake lock: `SCREEN_BRIGHT_WAKE_LOCK`, hoặc `SCREEN_DIM_WAKE_LOCK` nếu `dimScreen`.
  Có thời hạn: `acquire(remaining)` như hiện tại. ∞: `acquire()` không timeout (được rào bởi pin).
- Receiver đăng ký động, **chỉ trong lúc service chạy**:
  - `ACTION_SCREEN_OFF` → dừng nếu `stopOnScreenOff`.
  - `ACTION_POWER_DISCONNECTED` → dừng nếu `stopOnUnplug`.
  - `ACTION_BATTERY_LOW` (sticky check lúc start + broadcast) → dừng mọi phiên khi pin yếu và
    không sạc. Dùng ngưỡng hệ thống thay cho 15% cứng — không cần poll.
- Notification:
  - Có hạn: đếm ngược bằng chronometer như hiện tại; action **"+5 phút"** và **"Tắt"**.
  - ∞: tiêu đề "Giữ sáng không giới hạn", chronometer đếm **lên** từ `startAt`; chỉ action "Tắt".
- Gia hạn: intent `ACTION_EXTEND` → `endAt += 5 phút`, re-acquire wake lock, dời timeout, dựng lại notification.

### 3. Quick Settings tile

- Bấm: vòng **Tắt → 10 → 15 → 20 → ∞ → Tắt**. Mỗi bước bắt đầu lại từ "bây giờ". Nếu đang chạy
  phiên không thuộc vòng (vd. custom 35' bật từ app) thì bấm → Tắt.
- Subtitle: "Đến 14:32" (có hạn) / "Không giới hạn" (∞) / "Tắt".
- Nhấn giữ: `MainActivity` khai báo intent-filter `android.service.quicksettings.action.QS_TILE_PREFERENCES`.
- `onTileAdded`/`onTileRemoved` cập nhật `Prefs.tileAdded`.

### 4. App shortcuts

Static `res/xml/shortcuts.xml`: **10 phút, 20 phút, Không giới hạn, Tắt** (tối đa 4 hiển thị).
Mở một `ShortcutActivity` trong suốt, không UI: gọi controller rồi `finish()`.

### 5. Màn hình chính (Material 3 Expressive)

Bố cục dọc (portrait), cuộn được (`verticalScroll`):

```
        ╭─────────────╮
       ( ~ wavy ring ~ )      ← CircularWavyProgressIndicator (phần còn lại)
       (   ☕ shape    )      ← bấm để bật/tắt; tròn khi tắt, Cookie9Sided khi bật (morph, xoay chậm)
        ╰─────────────╯
            12:34             ← displayLargeEmphasized, tabular figures
   Màn hình sẽ luôn sáng      ← liveRegion polite
         [ +5 phút ]          ← chỉ hiện khi đang chạy phiên có hạn

  Thời gian
  [10][15][20][ ∞ ][Khác…]    ← ButtonGroup connected ToggleButton

  Tuỳ chọn
  ◐ Tắt khi tắt màn hình   [●]
  ⚡ Dừng khi rút sạc       [ ]
  ☾ Làm tối thay vì sáng   [ ]

   Thêm vào Cài đặt nhanh     ← ẩn khi tileAdded
```

- Xoay ngang (chiều rộng > chiều cao): 2 cột — hero trái, điều khiển phải.
- Hành động chính duy nhất là hero; bỏ nút "Bật/Tắt" lớn cũ.
- Đổi thời lượng khi đang chạy → khởi động lại phiên với thời lượng mới từ bây giờ.
  Bỏ dòng "Áp dụng cho lần bật tiếp theo".
- "Khác…" mở dialog chọn phút (slider 1–120, bước 5, có nút ±1); chip hiển thị giá trị đã chọn.
- ∞: vòng wavy chạy indeterminate; đồng hồ hiện thời gian đã chạy.
- Theme: `MaterialExpressiveTheme`, `MotionScheme.expressive()`; dynamic color trên Android 12+;
  bảng màu dự phòng tông cà phê (nâu/kem) thay cho tím mặc định của template, cả sáng và tối.
- Motion: chỉ hero morph/xoay và vòng progress chuyển động. Bỏ ý tưởng hơi nước để tránh quá tải chuyển động.
  Khi `ANIMATOR_DURATION_SCALE == 0` (Giảm chuyển động): không xoay, vòng progress tĩnh, morph tức thời.
- Accessibility: hero có `role = Switch`, `stateDescription` bật/tắt, nhãn "Caffeine";
  đồng hồ có `contentDescription` "còn N phút" (không đọc lại mỗi giây); dòng trạng thái là
  liveRegion polite; mọi vùng chạm ≥ 48dp.
- Haptic: `ToggleOn`/`ToggleOff` khi bật/tắt trong app.

### 6. Kiểm thử

- Unit test (JVM): vòng tile, tính `Session` khi start/extend, ∞, chọn custom.
- Build `assembleDebug` + lint không lỗi mới.
- Kiểm tra tay trên thiết bị/emulator: tile vòng + subtitle, +5' trên notification, tắt màn hình → dừng,
  shortcut, xoay ngang, cỡ chữ lớn nhất, TalkBack đọc hero, Giảm chuyển động, light/dark,
  API < 31 (bảng màu dự phòng).

## Quyết định của mình (người duyệt kiểm tra giúp)

1. Vòng tile cố định 10/15/20/∞, không gồm custom.
2. Rào pin dùng `ACTION_BATTERY_LOW` của hệ thống thay vì ngưỡng 15% tự đặt; áp dụng cho mọi phiên.
3. "Tắt khi tắt màn hình" mặc định bật.
4. "Tự bật khi cắm sạc" đổi thành "Dừng khi rút sạc" (lý do kỹ thuật ở mục Không làm).
5. Bỏ animation hơi nước.
6. Bỏ nút Bật/Tắt lớn; hero là hành động chính.
7. Shortcut: 10' / 20' / ∞ / Tắt.

## Để lại cho người làm

Cấu trúc file/package, tên hàm cụ thể, màu cụ thể của bảng màu cà phê, chi tiết dialog custom.
