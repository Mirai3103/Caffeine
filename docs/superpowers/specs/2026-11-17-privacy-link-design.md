# Link Chính sách bảo mật trong app

Ngày: 2026-11-17 · Trạng thái: đã duyệt (thiết kế nhỏ)

## Mục đích

Google Play yêu cầu chính sách bảo mật phải truy cập được **từ trong app**, không chỉ từ store
listing. App hiện chưa có link nào. Trang policy đã có sẵn và ghi rõ nó được link từ trong app:
https://share.web.huuhoang.tech/privacy/caffeine.html

## Mục tiêu

- Người dùng mở được policy bằng một chạm từ màn hình chính.
- Không thêm quyền `INTERNET`, không thêm dependency, không thêm SDK.
- Nếu máy không có trình duyệt: hiện hộp thoại chứa URL có thể chọn/copy, không crash, không im lặng.

## Không làm (non-goals)

- Không nhúng WebView (phải thêm quyền mạng; trang hiện không cần mạng trong app).
- Không thêm Custom Tabs (thêm dependency androidx.browser).
- Không thêm top app bar / overflow menu / màn About.
- Không lưu bản policy offline.

## Ràng buộc

- Chuỗi hiển thị có cả `values/` (en) và `values-vi/`.
- Không đổi hành vi hiện có của hero, tile, service.
- Không đổi bố cục hero/controls; chỉ thêm một nút ở cuối cột.

## Approaches đã cân nhắc

| | Cách | Đánh đổi |
|---|---|---|
| **A (chọn)** | `TextButton` cuối cột nội dung, mở `ACTION_VIEW` | Rõ ràng, đúng chuẩn "link", không lẫn với tuỳ chọn bật/tắt |
| B | `ListItem` (icon + chevron) trong `OptionsSection` | Trông như toggle, a11y dễ nhầm |
| C | Top app bar + menu overflow | Thêm chrome cho cả màn hình chỉ để chứa 1 link |

Người dùng chọn A.

## Thiết kế

```
  Thời gian
  [10][15][20][ ∞ ][Khác…]

  Tuỳ chọn
  ...

   Thêm vào Cài đặt nhanh    ← vẫn ẩn khi đã thêm tile
   Chính sách bảo mật        ← mới, TextButton, luôn hiện
```

- File mới `Privacy.kt`: hằng `PRIVACY_POLICY_URL`, `fun openPrivacyPolicy(context)`:
  thử `startActivity(Intent(ACTION_VIEW, Uri.parse(URL)))`; nếu `ActivityNotFoundException`
  thì hiện `AlertDialog` (dựng bằng `AlertDialog.Builder`) chứa URL dạng text chọn được.
- `CaffeineScreen.kt`: `CaffeineContent` nhận thêm `onPrivacy: () -> Unit`; `CaffeineScreen` truyền
  `{ openPrivacyPolicy(context) }`; nút đặt dưới nút "Add to Quick Settings" (cùng `Column` của
  `ControlsSection`), `heightIn(min = 48.dp)` như các nút khác. Cập nhật các `@Preview`.
- Strings: `privacy` = "Privacy policy" / "Chính sách bảo mật".
  Chuỗi fallback: `privacy_unavailable_title` = "Privacy policy" / "Chính sách bảo mật".
- a11y: `TextButton` mặc định đã có role Button; thêm `semantics { contentDescription = ... }`?
  Không cần — nhãn đã đủ nghĩa; để nguyên text để TalkBack đọc tự nhiên.

## Quyết định của mình (người duyệt kiểm tra giúp)

1. Nút luôn hiện (không ẩn theo điều kiện nào), kể cả khi đã thêm tile.
2. Mở bằng trình duyệt ngoài, không WebView/Custom Tab.
3. Fallback là dialog chứa URL, không phải Toast.

## Kiểm thử

- `assembleDebug` + unit test hiện có vẫn xanh.
- Kiểm tra tay: bấm nút → trình duyệt mở đúng URL; xoay ngang vẫn thấy nút; TalkBack đọc nhãn.
