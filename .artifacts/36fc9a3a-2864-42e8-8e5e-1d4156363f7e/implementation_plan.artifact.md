# Implementation Plan - Vitamin App UI

This plan outlines the creation of XML layout files based on the provided prototype image. The app features a landing screen (Logo), a Home screen with 4 feature cards, and a multi-tab interface managed by a Bottom Navigation Bar.

## User Review Required

> [!IMPORTANT]
> - **Navigation Architecture**: I am assuming a Single-Activity architecture using Fragments for the main navigation (Home, Settings, Notification, User).
> - **Icons**: Since the project doesn't have custom icons yet, I will use standard Material Design icons or placeholders. You can replace them later with your specific assets.
> - **Logo**: The "ViTaMiN" logo will be represented by a `TextView` and `ImageView` placeholder.

## Proposed Changes

### 1. Resources & Theming

#### [MODIFY] [colors.xml](file:///home/rena/AndroidStudioProjects/vitamin/app/src/main/res/values/colors.xml)
Add accent colors used in the prototype:
- `accent_pink`: For Menstruasi and Logo.
- `accent_yellow`: For Kalori and Notifications.
- `accent_blue`: For BMI and Diagnose.
- `bg_gray`: For screen backgrounds.

#### [NEW] `res/menu/bottom_nav_menu.xml`
Define the four navigation items:
- Settings
- Home
- Notification
- You (User Account)

---

### 2. Core Layouts

#### [MODIFY] [activity_main.xml](file:///home/rena/AndroidStudioProjects/vitamin/app/src/main/res/layout/activity_main.xml)
Redesign to match the **Main Logo** screen:
- Central Logo container.
- "ViTaMiN" stylized text.
- "0% penggunaan Ai" caption.
- Bottom "Home" navigation button.

#### [NEW] `fragment_home.xml`
Implement the **Home** screen:
- Top bar with Logo and "ViTaMiN (Home)" title.
- A 2x2 Grid (using `GridLayout` or `ConstraintLayout`) for the cards:
    - **Kalori**: Yellow theme.
    - **Diagnose**: Blue theme.
    - **Menstruasi**: Pink theme.
    - **BMI**: Blue/Green theme.
- Integration area for Bottom Navigation.

---

### 3. Secondary Screens

#### [NEW] `fragment_user.xml`
- User profile header with circular image.
- "User123" and "nickname" fields.
- "History" list section.

#### [NEW] `fragment_notification.xml`
- Notification settings (Notify? YES/NO checkboxes).
- "New History" list with status indicators (NEW).

#### [NEW] `fragment_settings.xml`
- List-based layout for options: Ganti Nickname, Ganti foto profil, Hapus Akun, Logout.

---

### 4. Detail Screens

#### [NEW] `fragment_kalori.xml`, `fragment_diagnose.xml`, `fragment_menstruasi.xml`, `fragment_bmi.xml`
- Consistent header with icon and title.
- Placeholder content area as shown in the prototype (e.g., Manual/Kamera tabs for Kalori).

## Verification Plan

### Automated Tests
- I will use `render_compose_preview` (if applicable) or manual inspection of XML layouts in the IDE.
- Verify `ConstraintLayout` stability across different screen sizes.

### Manual Verification
- Deploy to an emulator to verify that all elements are visible and aligned according to the prototype.
- Verify that the Bottom Navigation items are correctly positioned.
