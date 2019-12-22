# Katsuna SystemUI

Katsuna SystemUi is rom app based on aosp SystemUI app.

The basic change made on top of the aosp implementation was to replace the default quick settings functionality. Moreover Keyguard pattern lock screen was redesigned to conform to katsuna design pattern.



## Technical Info

- QuickStatusBarHeader
  - package com.android.systemui.qs
  - layout:   res/layout/quick_status_bar_expanded_header.xml
  - This view is used to display the minified version of quick settings. It contains the following features:
    - Wifi switch on/off
    - Bluetooth switch on/off
    - Cellular data switch on/off
    - Expand to full quick settings
    - Date and battery status
- QSFragment
  - package com.android.systemui.qs
  - layout:   res/layout/katsuna_quick_settings.xml
  - This fragment is presented after expanding the minified quick settings view. It contains the following functionalities:
    - Brightness adjustment
    - Volume adjustment
    - Wifi  switch on/off and link to navigate to Wifi settings activity
    - Cellular data switch on/off and link to navigate to Cellular data settings activity
    - Bluetooth switch on/off and link to navigate to Bluetooth  settings activity
    - Don't Disturb setting
    - Flight mode switch
    - Flash switch as torch
- SettingsController
  - package com.android.systemui.katsuna.utils
  - This is a utility class used to modify system settings. It uses the following android services:
    - AUDIO_SERVICE
    - WIFI_SERVICE
    - TELEPHONY_SERVICE
    - CONNECTIVITY_SERVICE
    - FlashlightController
- DrawQSUtils
  - package com.android.systemui.katsuna.utils
  - This class is used to apply katsuna design to various controls.
- KeyguardPatternView
  - package com.android.keyguard
  - layout res-keyguard/layout/keyguard_pattern_view.xml
  - Presentation changes were made to apply to katsuna design.



#### Dependencies (added for katsuna port)

- This project (as any other Katsuna app) depends on KatsunaCommon project (dev branch) which is an android library module that contains common classes and resources for all katsuna projects.
- Katsuna SystemUI requires KatsunaLauncher app because it contains the content provider that manages katsuna user profiles.



## License

This project is licensed under the Apache 2.0 License.